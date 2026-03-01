package net.z2six.toomanychests.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.minecraftforge.fml.loading.FMLPaths;
import net.z2six.toomanychests.Constants;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class TrackerConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve(Constants.MOD_ID + "-client.json");
    private static final long CHECK_INTERVAL_MS = 1000L;
    private static final Pattern COLOR_HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private static ConfigData data = ConfigData.defaults();
    private static long version = 0L;
    private static FileTime knownLastModified = FileTime.fromMillis(0L);
    private static long nextCheckAtMs = 0L;

    private TrackerConfigManager() {
    }

    public static synchronized void init() {
        loadOrCreate();
    }

    public static synchronized void hotReloadIfNeeded() {
        long now = System.currentTimeMillis();
        if (now < nextCheckAtMs) {
            return;
        }
        nextCheckAtMs = now + CHECK_INTERVAL_MS;
        try {
            if (!Files.exists(CONFIG_PATH)) {
                saveInternal();
                return;
            }
            FileTime current = Files.getLastModifiedTime(CONFIG_PATH);
            if (current.compareTo(knownLastModified) > 0) {
                loadFromDisk();
            }
        } catch (IOException exception) {
            Constants.LOG.warn("Failed to hot-reload config {}", CONFIG_PATH, exception);
        }
    }

    public static synchronized long version() {
        return version;
    }

    public static synchronized StackingMode stackingMode() {
        return data.stackingMode;
    }

    public static synchronized int highlightDurationSeconds() {
        return data.highlightDurationSeconds;
    }

    public static synchronized int highlightDurationTicks() {
        return data.highlightDurationSeconds * 20;
    }

    public static synchronized String indicatorCrosshairColorHex() {
        return data.indicatorCrosshairColorHex;
    }

    public static synchronized String indicatorLabelColorHex() {
        return data.indicatorLabelColorHex;
    }

    public static synchronized String chestOutlineColorHex() {
        return data.chestOutlineColorHex;
    }

    public static synchronized int indicatorCrosshairColorArgb(int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (clamped << 24) | Integer.parseInt(data.indicatorCrosshairColorHex.substring(1), 16);
    }

    public static synchronized int indicatorLabelColorArgb(int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (clamped << 24) | Integer.parseInt(data.indicatorLabelColorHex.substring(1), 16);
    }

    public static synchronized int chestOutlineColorArgb(int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (clamped << 24) | Integer.parseInt(data.chestOutlineColorHex.substring(1), 16);
    }

    public static synchronized Set<String> trackedBlocks() {
        return new HashSet<>(data.trackedBlocks);
    }

    public static synchronized List<String> trackedBlocksList() {
        return new ArrayList<>(data.trackedBlocks);
    }

    public static synchronized boolean isBlockTracked(String blockId) {
        return data.trackedBlocks.contains(normalizeId(blockId));
    }

    public static synchronized void updateFromScreen(
            StackingMode stackingMode,
            int highlightDurationSeconds,
            List<String> trackedBlocks,
            String indicatorCrosshairColorHex,
            String indicatorLabelColorHex,
            String chestOutlineColorHex
    ) {
        data.stackingMode = stackingMode;
        data.highlightDurationSeconds = clampHighlightDuration(highlightDurationSeconds);
        data.trackedBlocks = sanitizeBlockList(trackedBlocks);
        data.indicatorCrosshairColorHex = sanitizeColorHex(indicatorCrosshairColorHex);
        data.indicatorLabelColorHex = sanitizeColorHex(indicatorLabelColorHex);
        data.chestOutlineColorHex = sanitizeColorHex(chestOutlineColorHex);
        saveInternal();
    }

    private static void loadOrCreate() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                saveInternal();
                return;
            }
            loadFromDisk();
        } catch (IOException exception) {
            Constants.LOG.error("Failed to initialize config {}", CONFIG_PATH, exception);
            data = ConfigData.defaults();
            version++;
        }
    }

    private static void loadFromDisk() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
            if (loaded == null) {
                throw new JsonParseException("Parsed config is null");
            }
            data = sanitize(loaded);
            knownLastModified = Files.getLastModifiedTime(CONFIG_PATH);
            version++;
            Constants.LOG.info("Loaded client config from {}", CONFIG_PATH);
        } catch (JsonParseException exception) {
            Constants.LOG.warn("Invalid config JSON, rewriting defaults at {}", CONFIG_PATH, exception);
            data = ConfigData.defaults();
            saveInternal();
        }
    }

    private static void saveInternal() {
        data = sanitize(data);
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(data, writer);
            }
            knownLastModified = Files.getLastModifiedTime(CONFIG_PATH);
            version++;
        } catch (IOException exception) {
            Constants.LOG.error("Failed to save config {}", CONFIG_PATH, exception);
        }
    }

    private static ConfigData sanitize(ConfigData raw) {
        ConfigData sanitized = new ConfigData();
        sanitized.stackingMode = raw.stackingMode == null ? StackingMode.ALL_IDENTICAL : raw.stackingMode;
        sanitized.highlightDurationSeconds = clampHighlightDuration(raw.highlightDurationSeconds);
        sanitized.trackedBlocks = sanitizeBlockList(raw.trackedBlocks);
        String legacyIndicatorColor = sanitizeColorHex(raw.indicatorColorHex);
        sanitized.indicatorColorHex = legacyIndicatorColor;
        sanitized.indicatorCrosshairColorHex = sanitizeColorHexWithFallback(raw.indicatorCrosshairColorHex, legacyIndicatorColor);
        sanitized.indicatorLabelColorHex = sanitizeColorHexWithFallback(raw.indicatorLabelColorHex, legacyIndicatorColor);
        sanitized.chestOutlineColorHex = sanitizeColorHexWithFallback(raw.chestOutlineColorHex, "#ffffff");
        return sanitized;
    }

    private static int clampHighlightDuration(int seconds) {
        return Math.max(1, Math.min(300, seconds));
    }

    private static List<String> sanitizeBlockList(List<String> blockIds) {
        List<String> sanitized = new ArrayList<>();
        if (blockIds != null) {
            for (String id : blockIds) {
                String normalized = normalizeId(id);
                if (!normalized.isEmpty()) {
                    sanitized.add(normalized);
                }
            }
        }
        if (sanitized.isEmpty()) {
            sanitized.add("minecraft:chest");
            sanitized.add("minecraft:barrel");
            sanitized.add("minecraft:trapped_chest");
        }
        return sanitized;
    }

    private static String normalizeId(String blockId) {
        if (blockId == null) {
            return "";
        }
        return blockId.trim().toLowerCase(Locale.ROOT);
    }

    private static String sanitizeColorHex(String colorHex) {
        if (colorHex == null) {
            return "#fc0553";
        }
        String normalized = colorHex.trim();
        if (!COLOR_HEX.matcher(normalized).matches()) {
            return "#fc0553";
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static String sanitizeColorHexWithFallback(String colorHex, String fallback) {
        if (colorHex == null) {
            return fallback;
        }
        String normalized = colorHex.trim();
        if (!COLOR_HEX.matcher(normalized).matches()) {
            return fallback;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static final class ConfigData {
        private StackingMode stackingMode = StackingMode.ALL_IDENTICAL;
        private int highlightDurationSeconds = 12;
        // Legacy color kept for backward compatibility with older config files.
        private String indicatorColorHex = "#fc0553";
        private String indicatorCrosshairColorHex = "#fc0553";
        private String indicatorLabelColorHex = "#fc0553";
        private String chestOutlineColorHex = "#ffffff";
        private List<String> trackedBlocks = new ArrayList<>(List.of(
                "minecraft:chest",
                "minecraft:barrel",
                "minecraft:trapped_chest"
        ));

        private static ConfigData defaults() {
            return new ConfigData();
        }
    }
}
