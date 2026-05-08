package net.z2six.toomanychests.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class HighlightManager {
    private final List<Marker> activeMarkers = new ArrayList<>();

    public synchronized void highlight(String dimensionId, Collection<BlockPos> positions, long nowGameTick, int durationTicks) {
        activeMarkers.clear();
        Set<BlockPos> uniquePositions = new LinkedHashSet<>(positions);
        for (BlockPos blockPos : uniquePositions) {
            activeMarkers.add(new Marker(dimensionId, blockPos.immutable(), nowGameTick + durationTicks));
        }
    }

    public synchronized void tick(long nowGameTick) {
        activeMarkers.removeIf(marker -> marker.expiresAtGameTick <= nowGameTick);
    }

    public synchronized void clear() {
        activeMarkers.clear();
    }

    public synchronized List<BlockPos> activeInDimension(String dimensionId) {
        List<BlockPos> positions = new ArrayList<>();
        for (Marker marker : activeMarkers) {
            if (marker.dimensionId.equals(dimensionId)) {
                positions.add(marker.blockPos);
            }
        }
        return positions;
    }

    public synchronized List<BlockPos> activeInDimensionWithinRange(String dimensionId, Vec3 center, int rangeBlocks) {
        double maxDistanceSqr = (double) rangeBlocks * rangeBlocks;
        List<BlockPos> positions = new ArrayList<>();
        for (Marker marker : activeMarkers) {
            if (marker.dimensionId.equals(dimensionId)
                    && marker.blockPos.distToCenterSqr(center.x, center.y, center.z) <= maxDistanceSqr) {
                positions.add(marker.blockPos);
            }
        }
        return positions;
    }

    private record Marker(String dimensionId, BlockPos blockPos, long expiresAtGameTick) {
    }
}
