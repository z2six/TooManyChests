package net.z2six.toomanychests.client.data;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public record TrackedTarget(Type type, String dimensionId, BlockPos blockPos, String sourceId, String entityKey, long gameTime) {
    public enum Type {
        BLOCK,
        ENTITY
    }

    public static TrackedTarget forBlock(Level level, BlockPos blockPos, String sourceId, long gameTime) {
        return new TrackedTarget(Type.BLOCK, level.dimension().location().toString(), blockPos.immutable(), sourceId, null, gameTime);
    }

    public static TrackedTarget forEntity(Level level, Entity entity, long gameTime) {
        String source = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        return new TrackedTarget(Type.ENTITY, level.dimension().location().toString(), entity.blockPosition().immutable(), source, entity.getStringUUID(), gameTime);
    }
}
