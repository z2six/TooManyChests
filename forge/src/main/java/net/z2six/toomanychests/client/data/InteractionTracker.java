package net.z2six.toomanychests.client.data;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class InteractionTracker {
    private TrackedTarget lastTarget;

    public void recordBlock(Level level, BlockPos blockPos, String sourceId) {
        this.lastTarget = TrackedTarget.forBlock(level, blockPos, sourceId, level.getGameTime());
    }

    public TrackedTarget consumeRecent(String dimensionId, long currentGameTime, long maxAgeTicks) {
        if (this.lastTarget == null) {
            return null;
        }
        if (!this.lastTarget.dimensionId().equals(dimensionId)) {
            return null;
        }
        if (currentGameTime - this.lastTarget.gameTime() > maxAgeTicks) {
            return null;
        }
        TrackedTarget target = this.lastTarget;
        this.lastTarget = null;
        return target;
    }
}
