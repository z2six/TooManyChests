package net.z2six.toomanychests.client.data;

import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class AggregatedItemEntry {
    private final ItemStack displayStack;
    private long totalCount;
    private int stackCount;
    private final Set<String> containerIds = new LinkedHashSet<>();

    public AggregatedItemEntry(ItemStack initialStack, String containerId) {
        this.displayStack = initialStack.copy();
        this.totalCount = initialStack.getCount();
        this.stackCount = 1;
        this.containerIds.add(containerId);
    }

    public boolean canMerge(ItemStack other) {
        return ItemStack.isSameItemSameComponents(this.displayStack, other);
    }

    public void merge(ItemStack stack, String containerId) {
        this.totalCount += stack.getCount();
        this.stackCount++;
        this.containerIds.add(containerId);
    }

    public ItemStack displayStack() {
        return displayStack;
    }

    public long totalCount() {
        return totalCount;
    }

    public int stackCount() {
        return stackCount;
    }

    public int containerCount() {
        return containerIds.size();
    }

    public Set<String> containerIds() {
        return Collections.unmodifiableSet(containerIds);
    }
}
