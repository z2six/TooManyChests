package net.z2six.toomanychests.client.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ContainerStore {
    private final Map<String, ContainerRecord> containers = new LinkedHashMap<>();
    private long revision = 0L;

    public synchronized void putSnapshot(Level level, TrackedTarget target, String title, List<ItemStack> stacks, long gameTime) {
        NormalizedBlock normalizedBlock = normalizeBlockTarget(level, target);
        String id = buildId(target, normalizedBlock);
        String dimensionId = target == null ? "unknown" : target.dimensionId();
        BlockPos blockPos = normalizedBlock == null ? (target == null ? null : target.blockPos()) : normalizedBlock.primary();
        String sourceId = target == null ? "unknown" : target.sourceId();
        if (normalizedBlock != null && normalizedBlock.secondary() != null) {
            String secondaryId = buildBlockId(dimensionId, normalizedBlock.secondary());
            if (!secondaryId.equals(id)) {
                containers.remove(secondaryId);
            }
        }
        ContainerRecord existing = containers.get(id);
        if (existing == null) {
            containers.put(id, new ContainerRecord(id, dimensionId, blockPos, sourceId, title, gameTime, stacks));
        } else {
            existing.update(title, gameTime, stacks);
        }
        revision++;
    }

    public synchronized List<ContainerRecord> snapshot() {
        return coalesceDoubleChestRecords(new ArrayList<>(containers.values()));
    }

    public synchronized void replaceAll(List<ContainerRecord> records) {
        containers.clear();
        for (ContainerRecord record : records) {
            containers.put(record.id(), record);
        }
        revision++;
    }

    public synchronized void clear() {
        if (containers.isEmpty()) {
            return;
        }
        containers.clear();
        revision++;
    }

    public synchronized long revision() {
        return revision;
    }

    public synchronized List<BlockPos> resolvePositions(Collection<String> containerIds, String dimensionId) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        for (String id : containerIds) {
            ContainerRecord record = containers.get(id);
            if (record == null || record.blockPos() == null) {
                continue;
            }
            if (!record.dimensionId().equals(dimensionId)) {
                continue;
            }
            positions.add(record.blockPos());
        }
        return new ArrayList<>(positions);
    }

    public synchronized Map<String, Integer> countContainersByDimension(Collection<String> containerIds) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String id : containerIds) {
            ContainerRecord record = containers.get(id);
            if (record == null) {
                continue;
            }
            counts.merge(record.dimensionId(), 1, Integer::sum);
        }
        return counts;
    }

    private static String buildId(TrackedTarget target, NormalizedBlock normalizedBlock) {
        if (target == null) {
            return "unknown:" + UUID.randomUUID();
        }
        if (target.type() == TrackedTarget.Type.BLOCK) {
            BlockPos pos = normalizedBlock == null ? target.blockPos() : normalizedBlock.primary();
            return buildBlockId(target.dimensionId(), pos);
        }
        return "entity:" + target.dimensionId() + ":" + target.entityKey();
    }

    private static String buildBlockId(String dimensionId, BlockPos pos) {
        return "block:" + dimensionId + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
    }

    private static List<ContainerRecord> coalesceDoubleChestRecords(List<ContainerRecord> records) {
        Map<String, ContainerRecord> byChestPos = new HashMap<>();
        for (ContainerRecord record : records) {
            if (!isVanillaChestFamily(record)) {
                continue;
            }
            byChestPos.put(chestPosKey(record.dimensionId(), record.sourceId(), record.blockPos()), record);
        }

        Map<String, ContainerRecord> coalesced = new LinkedHashMap<>();
        for (ContainerRecord record : records) {
            String key = record.id();
            if (isVanillaChestFamily(record)) {
                BlockPos neighborPos = findAdjacentRecordedChest(record, byChestPos);
                if (neighborPos != null) {
                    BlockPos canonical = comparePos(record.blockPos(), neighborPos) <= 0 ? record.blockPos() : neighborPos;
                    key = "logical-double-chest:" + chestPosKey(record.dimensionId(), record.sourceId(), canonical);
                }
            }

            ContainerRecord existing = coalesced.get(key);
            if (existing == null || record.updatedAtGameTick() > existing.updatedAtGameTick()) {
                coalesced.put(key, record);
            }
        }
        return new ArrayList<>(coalesced.values());
    }

    private static boolean isVanillaChestFamily(ContainerRecord record) {
        if (record == null || record.blockPos() == null) {
            return false;
        }
        return "minecraft:chest".equals(record.sourceId()) || "minecraft:trapped_chest".equals(record.sourceId());
    }

    private static BlockPos findAdjacentRecordedChest(ContainerRecord record, Map<String, ContainerRecord> byChestPos) {
        BlockPos base = record.blockPos();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = base.relative(direction);
            String key = chestPosKey(record.dimensionId(), record.sourceId(), candidate);
            if (byChestPos.containsKey(key)) {
                return candidate;
            }
        }
        return null;
    }

    private static String chestPosKey(String dimensionId, String sourceId, BlockPos pos) {
        return dimensionId + "|" + sourceId + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ();
    }

    private static NormalizedBlock normalizeBlockTarget(Level level, TrackedTarget target) {
        if (level == null || target == null || target.type() != TrackedTarget.Type.BLOCK || target.blockPos() == null) {
            return null;
        }

        BlockPos primary = target.blockPos().immutable();
        BlockPos secondary = findOtherHalf(level, primary);
        if (secondary == null) {
            return new NormalizedBlock(primary, null);
        }
        BlockPos canonical = comparePos(primary, secondary) <= 0 ? primary : secondary;
        BlockPos other = canonical.equals(primary) ? secondary : primary;
        return new NormalizedBlock(canonical, other);
    }

    private static BlockPos findOtherHalf(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) {
            return null;
        }

        ChestType chestType = state.getValue(ChestBlock.TYPE);
        if (chestType == ChestType.SINGLE) {
            return null;
        }

        Direction facing = state.getValue(ChestBlock.FACING);
        Direction offset = chestType == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
        BlockPos otherPos = pos.relative(offset);
        BlockState other = level.getBlockState(otherPos);
        if (other.getBlock() != state.getBlock()) {
            return null;
        }
        if (other.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return null;
        }
        if (other.getValue(ChestBlock.FACING) != facing) {
            return null;
        }
        if (other.getValue(ChestBlock.TYPE) == chestType) {
            return null;
        }
        return otherPos.immutable();
    }

    private static int comparePos(BlockPos a, BlockPos b) {
        int byX = Integer.compare(a.getX(), b.getX());
        if (byX != 0) {
            return byX;
        }
        int byY = Integer.compare(a.getY(), b.getY());
        if (byY != 0) {
            return byY;
        }
        return Integer.compare(a.getZ(), b.getZ());
    }

    private record NormalizedBlock(BlockPos primary, BlockPos secondary) {
    }
}
