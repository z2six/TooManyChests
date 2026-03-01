package net.z2six.toomanychests.client.data;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ContainerRecord {
    private final String id;
    private final String dimensionId;
    private final BlockPos blockPos;
    private final String sourceId;
    private String title;
    private long updatedAtGameTick;
    private List<ItemStack> stacks;

    public ContainerRecord(String id, String dimensionId, BlockPos blockPos, String sourceId, String title, long updatedAtGameTick, List<ItemStack> stacks) {
        this.id = id;
        this.dimensionId = dimensionId;
        this.blockPos = blockPos;
        this.sourceId = sourceId;
        this.title = title;
        this.updatedAtGameTick = updatedAtGameTick;
        this.stacks = copyStacks(stacks);
    }

    public void update(String title, long updatedAtGameTick, List<ItemStack> stacks) {
        this.title = title;
        this.updatedAtGameTick = updatedAtGameTick;
        this.stacks = copyStacks(stacks);
    }

    public String id() {
        return id;
    }

    public String dimensionId() {
        return dimensionId;
    }

    public BlockPos blockPos() {
        return blockPos;
    }

    public String sourceId() {
        return sourceId;
    }

    public String title() {
        return title;
    }

    public long updatedAtGameTick() {
        return updatedAtGameTick;
    }

    public List<ItemStack> stacks() {
        return Collections.unmodifiableList(stacks);
    }

    private static List<ItemStack> copyStacks(List<ItemStack> input) {
        List<ItemStack> copied = new ArrayList<>(input.size());
        for (ItemStack stack : input) {
            copied.add(stack.copy());
        }
        return copied;
    }
}
