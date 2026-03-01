package net.z2six.toomanychests.client.data;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import net.z2six.toomanychests.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ContainerStorePersistence {
    private static final Path DATA_PATH = FMLPaths.CONFIGDIR.get().resolve(Constants.MOD_ID + "-containers.nbt");
    private static final long SAVE_INTERVAL_MS = 500L;

    private boolean loadedForSession = false;
    private long lastSavedRevision = -1L;
    private long nextSaveAtMs = 0L;

    public void tick(Minecraft minecraft, ContainerStore store) {
        if (minecraft.level == null) {
            return;
        }

        if (!loadedForSession) {
            load(minecraft, store);
            loadedForSession = true;
            lastSavedRevision = store.revision();
        }

        long currentRevision = store.revision();
        long now = System.currentTimeMillis();
        if (currentRevision != lastSavedRevision && now >= nextSaveAtMs) {
            save(minecraft, store);
            lastSavedRevision = store.revision();
            nextSaveAtMs = now + SAVE_INTERVAL_MS;
        }
    }

    public void flush(Minecraft minecraft, ContainerStore store) {
        if (minecraft.level == null) {
            return;
        }
        if (store.revision() != lastSavedRevision) {
            save(minecraft, store);
            lastSavedRevision = store.revision();
        }
    }

    public void resetSession() {
        loadedForSession = false;
        nextSaveAtMs = 0L;
    }

    private void load(Minecraft minecraft, ContainerStore store) {
        if (!Files.exists(DATA_PATH)) {
            return;
        }

        try {
            CompoundTag root = NbtIo.readCompressed(DATA_PATH.toFile());
            ListTag list = root.getList("containers", Tag.TAG_COMPOUND);
            List<ContainerRecord> records = new ArrayList<>(list.size());
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                String id = entry.getString("id");
                String dimensionId = entry.getString("dimension");
                int x = entry.getInt("x");
                int y = entry.getInt("y");
                int z = entry.getInt("z");
                String sourceId = entry.getString("source");
                String title = entry.getString("title");
                long updated = entry.getLong("updated");

                ListTag stacksTag = entry.getList("stacks", Tag.TAG_COMPOUND);
                List<ItemStack> stacks = new ArrayList<>(stacksTag.size());
                for (int stackIndex = 0; stackIndex < stacksTag.size(); stackIndex++) {
                    CompoundTag stackTag = stacksTag.getCompound(stackIndex);
                    ItemStack stack = ItemStack.of(stackTag);
                    if (!stack.isEmpty()) {
                        stacks.add(stack);
                    }
                }

                records.add(new ContainerRecord(id, dimensionId, new net.minecraft.core.BlockPos(x, y, z), sourceId, title, updated, stacks));
            }
            store.replaceAll(records);
            Constants.LOG.info("Loaded {} tracked containers from {}", records.size(), DATA_PATH);
        } catch (IOException exception) {
            Constants.LOG.warn("Failed to load tracked containers from {}", DATA_PATH, exception);
        }
    }

    private void save(Minecraft minecraft, ContainerStore store) {
        List<ContainerRecord> records = store.snapshot();

        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (ContainerRecord record : records) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", record.id());
            entry.putString("dimension", record.dimensionId());
            entry.putInt("x", record.blockPos().getX());
            entry.putInt("y", record.blockPos().getY());
            entry.putInt("z", record.blockPos().getZ());
            entry.putString("source", record.sourceId());
            entry.putString("title", record.title());
            entry.putLong("updated", record.updatedAtGameTick());

            ListTag stacksTag = new ListTag();
            for (ItemStack stack : record.stacks()) {
                Tag encoded = stack.save(new CompoundTag());
                if (encoded instanceof CompoundTag encodedTag && !encodedTag.isEmpty()) {
                    stacksTag.add(encodedTag);
                }
            }
            entry.put("stacks", stacksTag);
            list.add(entry);
        }

        root.put("containers", list);
        root.putLong("savedAt", System.currentTimeMillis());

        try {
            Files.createDirectories(DATA_PATH.getParent());
            NbtIo.writeCompressed(root, DATA_PATH.toFile());
        } catch (IOException exception) {
            Constants.LOG.warn("Failed to save tracked containers to {}", DATA_PATH, exception);
        }
    }
}
