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
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.z2six.toomanychests.client.config.TrackerConfigManager;

import java.util.List;

public final class MarkerRenderer {
    private MarkerRenderer() {
    }

    public static void render(RenderLevelStageEvent event, HighlightManager highlightManager) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        List<BlockPos> positions = highlightManager.activeInDimension(minecraft.level.dimension().location().toString());
        if (positions.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        float animation = 0.5F + 0.5F * Mth.sin((minecraft.level.getGameTime() + event.getPartialTick().getGameTimeDeltaTicks()) * 0.2F);
        double expand = 0.04D + animation * 0.10D;
        float outerAlpha = 0.65F + animation * 0.30F;
        float innerAlpha = 0.35F + animation * 0.35F;
        int color = TrackerConfigManager.indicatorColorRgb();
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(2.0F);

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
        for (BlockPos blockPos : positions) {
            double distance = Math.sqrt(blockPos.distToCenterSqr(cameraPos.x, cameraPos.y, cameraPos.z));
            double scaledExpand = Math.min(8.0D, Math.max(expand, distance * 0.0025D));
            drawMarker(minecraft.level, poseStack, lineConsumer, blockPos, scaledExpand, red, green, blue, outerAlpha, innerAlpha);
        }
        poseStack.popPose();

        bufferSource.endBatch(RenderType.lines());
        RenderSystem.lineWidth(1.0F);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void drawMarker(
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
