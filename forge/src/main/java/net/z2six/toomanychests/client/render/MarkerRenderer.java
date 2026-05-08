package net.z2six.toomanychests.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.z2six.toomanychests.client.config.TrackerConfigManager;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public final class MarkerRenderer {
    private MarkerRenderer() {
    }

    public static void render(RenderLevelStageEvent event, HighlightManager highlightManager) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        List<BlockPos> positions = highlightManager.activeInDimensionWithinRange(
                minecraft.level.dimension().location().toString(),
                minecraft.player.position(),
                TrackerConfigManager.trackingRangeBlocks()
        );
        if (positions.isEmpty()) {
            MarkerHudRenderer.clearProjectedMarkers();
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int markerColor = TrackerConfigManager.indicatorCrosshairColorArgb(255);
        int textColor = TrackerConfigManager.indicatorLabelColorArgb(255);
        List<MarkerHudRenderer.ProjectedMarker> projectedMarkers = new ArrayList<>();

        float animation = 0.5F + 0.5F * Mth.sin((minecraft.level.getGameTime() + event.getPartialTick()) * 0.2F);
        double expand = 0.04D + animation * 0.10D;
        float outerAlpha = 0.65F + animation * 0.30F;
        float innerAlpha = 0.35F + animation * 0.35F;
        int color = TrackerConfigManager.chestOutlineColorArgb(255);
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(2.0F);

        Matrix4f markerProjectionView = new Matrix4f(poseStack.last().pose());
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
        for (BlockPos blockPos : positions) {
            double blockDistance = Math.sqrt(blockPos.distToCenterSqr(cameraPos.x, cameraPos.y, cameraPos.z));
            double scaledExpand = Math.min(8.0D, Math.max(expand, blockDistance * 0.0025D));
            Vec3 markerCenter = drawMarker(minecraft.level, poseStack, lineConsumer, blockPos, scaledExpand, red, green, blue, outerAlpha, innerAlpha);
            double markerDistance = markerCenter.distanceTo(cameraPos);
            projectedMarkers.add(projectMarker(
                    markerCenter,
                    cameraPos,
                    markerProjectionView,
                    event.getProjectionMatrix(),
                    screenWidth,
                    screenHeight,
                    markerDistance,
                    markerColor,
                    textColor
            ));
        }
        poseStack.popPose();

        bufferSource.endBatch(RenderType.lines());
        RenderSystem.lineWidth(1.0F);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        MarkerHudRenderer.updateProjectedMarkers(projectedMarkers);
    }

    private static Vec3 drawMarker(
            Level level,
            PoseStack poseStack,
            VertexConsumer lineConsumer,
            BlockPos pos,
            double expand,
            float red,
            float green,
            float blue,
            float outerAlpha,
            float innerAlpha
    ) {
        Bounds bounds = resolveBounds(level, pos);
        double minX = bounds.minX() - expand;
        double minY = pos.getY() - expand;
        double minZ = bounds.minZ() - expand;
        double maxX = bounds.maxX() + expand;
        double maxY = pos.getY() + 1.0D + expand;
        double maxZ = bounds.maxZ() + expand;
        LevelRenderer.renderLineBox(poseStack, lineConsumer, minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, outerAlpha);

        double inset = 0.08D;
        LevelRenderer.renderLineBox(
                poseStack,
                lineConsumer,
                minX + inset,
                minY + inset,
                minZ + inset,
                maxX - inset,
                maxY - inset,
                maxZ - inset,
                red,
                green,
                blue,
                innerAlpha
        );

        Vec3 center = markerCenter(bounds, pos.getY());
        drawExactCenterMarker(poseStack, lineConsumer, center, red, green, blue, outerAlpha);
        return center;
    }

    private static void drawExactCenterMarker(
            PoseStack poseStack,
            VertexConsumer lineConsumer,
            Vec3 center,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        double halfSize = 0.30D;
        LevelRenderer.renderLineBox(
                poseStack,
                lineConsumer,
                center.x - halfSize,
                center.y - halfSize,
                center.z - halfSize,
                center.x + halfSize,
                center.y + halfSize,
                center.z + halfSize,
                red,
                green,
                blue,
                alpha
        );
    }

    private static Vec3 markerCenter(Bounds bounds, int blockY) {
        return new Vec3(
                (bounds.minX() + bounds.maxX()) * 0.5D,
                blockY + 0.5D,
                (bounds.minZ() + bounds.maxZ()) * 0.5D
        );
    }

    private static MarkerHudRenderer.ProjectedMarker projectMarker(
            Vec3 worldPos,
            Vec3 cameraPos,
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix,
            int screenWidth,
            int screenHeight,
            double distance,
            int markerColor,
            int textColor
    ) {
        Vector4f clip = new Vector4f(
                (float) (worldPos.x - cameraPos.x),
                (float) (worldPos.y - cameraPos.y),
                (float) (worldPos.z - cameraPos.z),
                1.0F
        );
        clip.mul(modelViewMatrix);
        clip.mul(projectionMatrix);

        boolean behind = clip.w() <= 0.0F;
        double ndcX = behind ? -clip.x() : clip.x() / clip.w();
        double ndcY = behind ? -clip.y() : clip.y() / clip.w();
        double screenX = (ndcX * 0.5D + 0.5D) * screenWidth;
        double screenY = (0.5D - ndcY * 0.5D) * screenHeight;
        double directionX = screenX - screenWidth / 2.0D;
        double directionY = screenY - screenHeight / 2.0D;
        return new MarkerHudRenderer.ProjectedMarker(screenX, screenY, directionX, directionY, distance, behind, markerColor, textColor);
    }

    private static Bounds resolveBounds(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) {
            return singleBlockBounds(pos);
        }

        ChestType chestType = state.getValue(ChestBlock.TYPE);
        if (chestType == ChestType.SINGLE) {
            return singleBlockBounds(pos);
        }

        Direction facing = state.getValue(ChestBlock.FACING);
        Direction offset = chestType == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
        BlockPos otherPos = pos.relative(offset);
        BlockState other = level.getBlockState(otherPos);
        if (other.getBlock() != state.getBlock()
                || other.getValue(ChestBlock.TYPE) == ChestType.SINGLE
                || other.getValue(ChestBlock.FACING) != facing
                || other.getValue(ChestBlock.TYPE) == chestType) {
            return singleBlockBounds(pos);
        }

        double minX = Math.min(pos.getX(), otherPos.getX());
        double minZ = Math.min(pos.getZ(), otherPos.getZ());
        double maxX = Math.max(pos.getX(), otherPos.getX()) + 1.0D;
        double maxZ = Math.max(pos.getZ(), otherPos.getZ()) + 1.0D;
        return new Bounds(minX, minZ, maxX, maxZ);
    }

    private static Bounds singleBlockBounds(BlockPos pos) {
        return new Bounds(pos.getX(), pos.getZ(), pos.getX() + 1.0D, pos.getZ() + 1.0D);
    }

    private record Bounds(double minX, double minZ, double maxX, double maxZ) {
    }
}
