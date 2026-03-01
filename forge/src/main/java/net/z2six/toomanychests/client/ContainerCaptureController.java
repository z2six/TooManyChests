package net.z2six.toomanychests.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.z2six.toomanychests.client.config.TrackerConfigManager;
import net.z2six.toomanychests.client.data.ContainerStore;
import net.z2six.toomanychests.client.data.InteractionTracker;
import net.z2six.toomanychests.client.data.TrackedTarget;

import java.util.ArrayList;
import java.util.List;

public final class ContainerCaptureController {
    private static final int CAPTURE_DELAY_TICKS = 3;
    private static final int INTERACTION_MAX_AGE_TICKS = 40;

    private final ContainerStore containerStore;
    private final InteractionTracker interactionTracker;

    private AbstractContainerMenu trackedMenu;
    private TrackedTarget targetForTrackedMenu;
    private int captureDelay;
    private long lastCapturedSignature = Long.MIN_VALUE;

    public ContainerCaptureController(ContainerStore containerStore, InteractionTracker interactionTracker) {
        this.containerStore = containerStore;
        this.interactionTracker = interactionTracker;
    }

    public void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            resetState();
            return;
        }

        AbstractContainerMenu menu = minecraft.player.containerMenu;
        if (menu == minecraft.player.inventoryMenu) {
            if (trackedMenu != null && captureDelay == 0) {
                captureMenuSnapshotIfChanged(minecraft, trackedMenu, targetForTrackedMenu, true);
            }
            resetState();
            return;
        }

        if (menu != trackedMenu) {
            trackedMenu = menu;
            targetForTrackedMenu = interactionTracker.consumeRecent(minecraft.level.dimension().location().toString(), minecraft.level.getGameTime(), INTERACTION_MAX_AGE_TICKS);
            captureDelay = CAPTURE_DELAY_TICKS;
            lastCapturedSignature = Long.MIN_VALUE;
        }

        if (captureDelay > 0) {
            captureDelay--;
            if (captureDelay == 0) {
                captureMenuSnapshotIfChanged(minecraft, menu, targetForTrackedMenu, true);
            }
            return;
        }

        captureMenuSnapshotIfChanged(minecraft, menu, targetForTrackedMenu, false);
    }

    private void resetState() {
        trackedMenu = null;
        targetForTrackedMenu = null;
        captureDelay = 0;
        lastCapturedSignature = Long.MIN_VALUE;
    }

    private void captureMenuSnapshotIfChanged(Minecraft minecraft, AbstractContainerMenu menu, TrackedTarget target, boolean force) {
        if (!isTrackableTarget(target)) {
            return;
        }

        long signature = computeMenuSignature(menu, minecraft.player.getInventory());
        if (!force && signature == lastCapturedSignature) {
            return;
        }
        lastCapturedSignature = signature;

        captureMenuSnapshot(minecraft, menu, target);
    }

    private static boolean isTrackableTarget(TrackedTarget target) {
        if (target == null) {
            return false;
        }

        if (target.type() != TrackedTarget.Type.BLOCK) {
            return false;
        }

        return TrackerConfigManager.isBlockTracked(target.sourceId());
    }

    private void captureMenuSnapshot(Minecraft minecraft, AbstractContainerMenu menu, TrackedTarget target) {
        List<ItemStack> stacks = collectMenuStacks(menu, minecraft.player.getInventory());
        String title = minecraft.screen == null ? menu.getClass().getSimpleName() : minecraft.screen.getTitle().getString();
        containerStore.putSnapshot(minecraft.level, target, title, stacks, minecraft.level.getGameTime());
    }

    private static long computeMenuSignature(AbstractContainerMenu menu, Inventory playerInventory) {
        long hash = 1469598103934665603L;
        int slotIndex = 0;
        for (Slot slot : menu.slots) {
            if (slot.container == playerInventory || slot.container instanceof Inventory) {
                slotIndex++;
                continue;
            }

            ItemStack stack = slot.getItem();
            hash = 31L * hash + slotIndex;
            hash = 31L * hash + stack.getItem().hashCode();
            hash = 31L * hash + (stack.getTag() == null ? 0 : stack.getTag().hashCode());
            hash = 31L * hash + stack.getCount();
            slotIndex++;
        }
        return hash;
    }

    private static List<ItemStack> collectMenuStacks(AbstractContainerMenu menu, Inventory playerInventory) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.container == playerInventory || slot.container instanceof Inventory) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
            }
        }
        return stacks;
    }

    public void recordRightClickBlock(Minecraft minecraft, net.minecraft.core.BlockPos blockPos) {
        if (minecraft.level == null) {
            return;
        }
        String blockId = ForgeRegistries.BLOCKS.getKey(minecraft.level.getBlockState(blockPos).getBlock()).toString();
        interactionTracker.recordBlock(minecraft.level, blockPos, blockId);
    }
}
