package net.z2six.toomanychests.client.data;

import net.minecraft.world.item.ItemStack;
import net.z2six.toomanychests.client.config.StackingMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ItemAggregator {
    private ItemAggregator() {
    }

    public static List<AggregatedItemEntry> aggregate(List<ContainerRecord> records, StackingMode stackingMode) {
        List<AggregatedItemEntry> aggregated = new ArrayList<>();
        for (ContainerRecord record : records) {
            for (ItemStack stack : record.stacks()) {
                if (stack.isEmpty()) {
                    continue;
                }
                if (stackingMode == StackingMode.NONE) {
                    aggregated.add(new AggregatedItemEntry(stack, record.id()));
                    continue;
                }
                if (stackingMode == StackingMode.STACKABLE_ONLY && stack.getMaxStackSize() <= 1) {
                    aggregated.add(new AggregatedItemEntry(stack, record.id()));
                    continue;
                }
                mergeIntoExisting(aggregated, stack, record.id());
            }
        }

        aggregated.sort(Comparator
                .comparingLong(AggregatedItemEntry::totalCount).reversed()
                .thenComparing(entry -> entry.displayStack().getHoverName().getString(), String.CASE_INSENSITIVE_ORDER));
        return aggregated;
    }

    private static void mergeIntoExisting(List<AggregatedItemEntry> aggregated, ItemStack stack, String containerId) {
        for (AggregatedItemEntry entry : aggregated) {
            if (!entry.canMerge(stack)) {
                continue;
            }
            entry.merge(stack, containerId);
            return;
        }
        aggregated.add(new AggregatedItemEntry(stack, containerId));
    }
}
