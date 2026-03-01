package net.z2six.toomanychests.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.z2six.toomanychests.client.config.TrackerConfigManager;
import net.z2six.toomanychests.client.data.AggregatedItemEntry;
import net.z2six.toomanychests.client.data.ContainerRecord;
import net.z2six.toomanychests.client.data.ContainerStore;
import net.z2six.toomanychests.client.data.ItemAggregator;
import net.z2six.toomanychests.client.gui.widget.AccentButton;
import net.z2six.toomanychests.client.render.HighlightManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class TrackerScreen extends Screen {
    private static final int ACCENT = 0xFFFC0553;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int PANEL_FILL = 0xE6000000;
    private static final int PANEL_BORDER = 0xFF1A1A1A;
    private static final int GRID_SLOT_SIZE = 20;
    private static final int ICON_INSET = 2;
    private static final int CONTROL_HEIGHT = 20;
    private static final int CONTROL_GAP = 6;

    private final ContainerStore containerStore;
    private final HighlightManager highlightManager;

    private final List<AggregatedItemEntry> filteredEntries = new ArrayList<>();
    private long lastSeenStoreRevision = -1L;
    private long lastSeenConfigRevision = -1L;
    private FilterMode filterMode = FilterMode.NAME_OR_ID;
    private boolean enchantedOnly = false;
    private int firstVisibleGridRow = 0;
    private int inspectedIndex = -1;

    private EditBox searchBox;
    private EditBox minCountBox;

    private int gridPanelTop;
    private int gridPanelBottom;
    private int footerY;

    public TrackerScreen(ContainerStore containerStore, HighlightManager highlightManager) {
        super(Component.translatable("screen.toomanychests.title"));
        this.containerStore = containerStore;
        this.highlightManager = highlightManager;
    }

    @Override
    protected void init() {
        int left = listLeft();
        int contentWidth = listRight() - left;
        int row1Y = 22;
        int row2Y = row1Y + CONTROL_HEIGHT + CONTROL_GAP;

        int minCountWidth = 56;
        int filterWidth = 128;
        int searchWidth = Math.max(140, Math.min(300, contentWidth - (minCountWidth + filterWidth + CONTROL_GAP * 2)));
        if (searchWidth + minCountWidth + filterWidth + CONTROL_GAP * 2 > contentWidth) {
            searchWidth = Math.max(140, contentWidth - (minCountWidth + filterWidth + CONTROL_GAP * 2));
        }

        int searchX = left;
        int minCountX = searchX + searchWidth + CONTROL_GAP;
        int filterX = minCountX + minCountWidth + CONTROL_GAP;

        this.searchBox = makeEditBox(
                searchX,
                row1Y,
                searchWidth,
                Component.translatable("screen.toomanychests.search"),
                Component.translatable("screen.toomanychests.tooltip_search")
        );
        this.searchBox.setResponder(value -> rebuildEntries(true));
        this.searchBox.setHint(Component.translatable("screen.toomanychests.search_hint"));
        this.addRenderableWidget(this.searchBox);

        this.minCountBox = makeEditBox(
                minCountX,
                row1Y,
                minCountWidth,
                Component.translatable("screen.toomanychests.min_count"),
                Component.translatable("screen.toomanychests.tooltip_min_count")
        );
        this.minCountBox.setValue("1");
        this.minCountBox.setFilter(value -> value.isEmpty() || value.matches("\\d+"));
        this.minCountBox.setResponder(value -> rebuildEntries(true));
        this.addRenderableWidget(this.minCountBox);

        this.addRenderableWidget(
                accentButton(
                        filterButtonText(),
                        button -> {
                            filterMode = filterMode.next();
                            button.setMessage(filterButtonText());
                            rebuildEntries(true);
                        },
                        filterX,
                        row1Y,
                        filterWidth,
                        Component.translatable("screen.toomanychests.tooltip_filter_mode")
                )
        );

        int clearWidth = 90;
        int settingsWidth = 90;
        int helpWidth = 20;
        int fixed = clearWidth + settingsWidth + helpWidth + CONTROL_GAP * 3;
        int enchantedWidth = Math.max(120, Math.min(190, contentWidth - fixed));

        int enchantedX = left;
        int clearX = enchantedX + enchantedWidth + CONTROL_GAP;
        int settingsX = clearX + clearWidth + CONTROL_GAP;
        int helpX = settingsX + settingsWidth + CONTROL_GAP;

        this.addRenderableWidget(
                accentButton(
                        enchantedButtonText(),
                        button -> {
                            enchantedOnly = !enchantedOnly;
                            button.setMessage(enchantedButtonText());
                            rebuildEntries(true);
                        },
                        enchantedX,
                        row2Y,
                        enchantedWidth,
                        Component.translatable("screen.toomanychests.tooltip_enchanted")
                )
        );

        this.addRenderableWidget(
                accentButton(
                        Component.translatable("screen.toomanychests.clear"),
                        button -> {
                            containerStore.clear();
                            highlightManager.clear();
                            rebuildEntries(true);
                        },
                        clearX,
                        row2Y,
                        clearWidth,
                        Component.translatable("screen.toomanychests.tooltip_clear")
                )
        );

        this.addRenderableWidget(
                accentButton(
                        Component.translatable("screen.toomanychests.settings"),
                        button -> this.minecraft.setScreen(new TrackerSettingsScreen(this)),
                        settingsX,
                        row2Y,
                        settingsWidth,
                        Component.translatable("screen.toomanychests.tooltip_settings")
                )
        );

        this.addRenderableWidget(
                accentButton(
                        Component.literal("?"),
                        button -> {
                        },
                        helpX,
                        row2Y,
                        helpWidth,
                        Component.translatable("screen.toomanychests.tooltip_help")
                )
        );

        this.gridPanelTop = row2Y + CONTROL_HEIGHT + 10;
        this.gridPanelBottom = this.height - 16;
        this.footerY = this.gridPanelBottom - 10;

        rebuildEntries(true);
        setInitialFocus(this.searchBox);
    }

    private EditBox makeEditBox(int x, int y, int width, Component narration, Component tooltip) {
        EditBox box = new EditBox(this.font, x, y, width, CONTROL_HEIGHT, narration);
        box.setTextColor(0xFFFFFFFF);
        box.setTextColorUneditable(0xFF6A6A6A);
        box.setTooltip(Tooltip.create(tooltip));
        return box;
    }

    private Button accentButton(Component label, Button.OnPress onPress, int x, int y, int width, Component tooltip) {
        return Button.builder(label, onPress)
                .tooltip(Tooltip.create(tooltip))
                .bounds(x, y, width, CONTROL_HEIGHT)
                .build(AccentButton::new);
    }

    @Override
    public void tick() {
        if (containerStore.revision() != lastSeenStoreRevision || TrackerConfigManager.version() != lastSeenConfigRevision) {
            rebuildEntries(false);
        }
        if (inspectedIndex >= filteredEntries.size()) {
            inspectedIndex = -1;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!isRightMouseDown()) {
            inspectedIndex = -1;
        }

        for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        drawBrandTitle(guiGraphics, this.width / 2, 6, " Tracker");
        renderGrid(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int index = gridIndexAt(event.x(), event.y());
        if (index >= 0 && index < filteredEntries.size()) {
            if (event.button() == 0) {
                highlightEntry(filteredEntries.get(index));
                if (this.minecraft != null) {
                    this.minecraft.setScreen(null);
                }
                return true;
            }
            if (event.button() == 1) {
                inspectedIndex = index;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 1) {
            inspectedIndex = -1;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInsideGrid(mouseX, mouseY)) {
            int maxFirstRow = maxFirstVisibleGridRow();
            firstVisibleGridRow = Mth.clamp(firstVisibleGridRow - (int) Math.signum(scrollY), 0, maxFirstRow);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public void refreshFromSettings() {
        rebuildEntries(true);
    }

    private void renderGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int left = listLeft();
        int right = listRight();
        int top = listTop();
        int bottom = listBottom();

        guiGraphics.fill(left, top, right, bottom, PANEL_FILL);
        guiGraphics.fill(left - 1, top - 1, right + 1, top, PANEL_BORDER);
        guiGraphics.fill(left - 1, bottom, right + 1, bottom + 1, PANEL_BORDER);
        guiGraphics.fill(left - 1, top - 1, left, bottom + 1, PANEL_BORDER);
        guiGraphics.fill(right, top - 1, right + 1, bottom + 1, PANEL_BORDER);

        if (filteredEntries.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("screen.toomanychests.empty"), (left + right) / 2, top + 10, 0xFFAAAAAA);
            return;
        }

        int hoveredIndex = -1;
        int columns = gridColumns();
        int rows = gridRows();
        int startIndex = firstVisibleGridRow * columns;
        int gridLeft = gridLeft();
        int gridTop = gridTop();

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int index = startIndex + row * columns + column;
                int slotX = gridLeft + column * GRID_SLOT_SIZE;
                int slotY = gridTop + row * GRID_SLOT_SIZE;
                boolean hovered = mouseX >= slotX && mouseX < slotX + GRID_SLOT_SIZE && mouseY >= slotY && mouseY < slotY + GRID_SLOT_SIZE;

                guiGraphics.fill(slotX, slotY, slotX + GRID_SLOT_SIZE - 1, slotY + GRID_SLOT_SIZE - 1, 0xFF111111);
                if (hovered) {
                    guiGraphics.fill(slotX, slotY, slotX + GRID_SLOT_SIZE - 1, slotY + 1, ACCENT);
                    guiGraphics.fill(slotX, slotY + GRID_SLOT_SIZE - 2, slotX + GRID_SLOT_SIZE - 1, slotY + GRID_SLOT_SIZE - 1, ACCENT);
                    guiGraphics.fill(slotX, slotY, slotX + 1, slotY + GRID_SLOT_SIZE - 1, ACCENT);
                    guiGraphics.fill(slotX + GRID_SLOT_SIZE - 2, slotY, slotX + GRID_SLOT_SIZE - 1, slotY + GRID_SLOT_SIZE - 1, ACCENT);
                }

                if (index >= filteredEntries.size()) {
                    continue;
                }

                AggregatedItemEntry entry = filteredEntries.get(index);
                int itemX = slotX + ICON_INSET;
                int itemY = slotY + ICON_INSET;
                guiGraphics.renderItem(entry.displayStack(), itemX, itemY);
                guiGraphics.renderItemDecorations(this.font, entry.displayStack(), itemX, itemY, compactCount(entry.totalCount()));

                if (hovered) {
                    hoveredIndex = index;
                }
            }
        }

        int totalRows = totalGridRows();
        String footer = Component.translatable("screen.toomanychests.footer", filteredEntries.size(), firstVisibleGridRow + 1, Math.max(1, totalRows)).getString();
        guiGraphics.drawString(this.font, footer, left + 6, this.footerY - 2, 0xFFD0D0D0);

        if (hoveredIndex >= 0) {
            guiGraphics.setTooltipForNextFrame(this.font, filteredEntries.get(hoveredIndex).displayStack(), mouseX, mouseY);
        }

        if (inspectedIndex >= 0 && inspectedIndex < filteredEntries.size() && isRightMouseDown()) {
            renderInfoTooltip(guiGraphics, mouseX, mouseY, filteredEntries.get(inspectedIndex));
        }
    }

    private void rebuildEntries(boolean resetScroll) {
        List<ContainerRecord> records = containerStore.snapshot();
        List<AggregatedItemEntry> aggregated = ItemAggregator.aggregate(records, TrackerConfigManager.stackingMode());
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        int minCount = parseMinCount(minCountBox == null ? "1" : minCountBox.getValue());

        filteredEntries.clear();
        for (AggregatedItemEntry entry : aggregated) {
            if (entry.totalCount() < minCount) {
                continue;
            }
            if (enchantedOnly && !entry.displayStack().isEnchanted()) {
                continue;
            }
            if (!query.isEmpty() && !matchesQuery(entry, query)) {
                continue;
            }
            filteredEntries.add(entry);
        }

        lastSeenStoreRevision = containerStore.revision();
        lastSeenConfigRevision = TrackerConfigManager.version();
        if (resetScroll) {
            firstVisibleGridRow = 0;
        } else {
            firstVisibleGridRow = Mth.clamp(firstVisibleGridRow, 0, maxFirstVisibleGridRow());
        }
    }

    private boolean matchesQuery(AggregatedItemEntry entry, String query) {
        String name = entry.displayStack().getHoverName().getString().toLowerCase(Locale.ROOT);
        String itemId = BuiltInRegistries.ITEM.getKey(entry.displayStack().getItem()).toString().toLowerCase(Locale.ROOT);
        return switch (filterMode) {
            case NAME_ONLY -> name.contains(query);
            case ITEM_ID_ONLY -> itemId.contains(query);
            case NAME_OR_ID -> name.contains(query) || itemId.contains(query);
        };
    }

    private int parseMinCount(String value) {
        if (value == null || value.isBlank()) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private void highlightEntry(AggregatedItemEntry entry) {
        if (this.minecraft == null || this.minecraft.level == null || this.minecraft.player == null) {
            return;
        }
        highlightManager.clear();
        String dimensionId = this.minecraft.level.dimension().location().toString();
        Map<String, Integer> byDimension = containerStore.countContainersByDimension(entry.containerIds());
        String otherDimensions = buildOtherDimensionsSummary(byDimension, dimensionId);
        List<BlockPos> positions = containerStore.resolvePositions(entry.containerIds(), dimensionId);
        if (positions.isEmpty()) {
            if (!otherDimensions.isEmpty()) {
                this.minecraft.player.displayClientMessage(
                        Component.translatable("screen.toomanychests.only_other_dimensions", otherDimensions).withStyle(ChatFormatting.YELLOW),
                        true
                );
                return;
            }
            this.minecraft.player.displayClientMessage(Component.translatable("screen.toomanychests.no_positions").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        highlightManager.highlight(dimensionId, positions, this.minecraft.level.getGameTime(), TrackerConfigManager.highlightDurationTicks());
        Component message = Component.translatable("screen.toomanychests.highlighting", positions.size()).withStyle(ChatFormatting.RED);
        if (!otherDimensions.isEmpty()) {
            message = message.copy().append(Component.translatable("screen.toomanychests.other_dimensions", otherDimensions).withStyle(ChatFormatting.GRAY));
        }
        this.minecraft.player.displayClientMessage(message, true);
    }

    private String buildOtherDimensionsSummary(Map<String, Integer> byDimension, String currentDimensionId) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : byDimension.entrySet()) {
            String dimensionId = entry.getKey();
            int count = entry.getValue();
            if (dimensionId.equals(currentDimensionId) || count <= 0) {
                continue;
            }
            parts.add(formatDimensionName(dimensionId) + " x" + count);
        }
        if (parts.isEmpty()) {
            return "";
        }
        if (parts.size() <= 3) {
            return String.join(", ", parts);
        }
        return String.join(", ", parts.subList(0, 3)) + ", +" + (parts.size() - 3) + " more";
    }

    private String formatDimensionName(String dimensionId) {
        return switch (dimensionId) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "The End";
            default -> {
                int sep = dimensionId.indexOf(':');
                String raw = sep >= 0 && sep + 1 < dimensionId.length() ? dimensionId.substring(sep + 1) : dimensionId;
                String[] words = raw.replace('_', ' ').split(" ");
                StringBuilder builder = new StringBuilder();
                for (String word : words) {
                    if (word.isBlank()) {
                        continue;
                    }
                    if (!builder.isEmpty()) {
                        builder.append(' ');
                    }
                    builder.append(Character.toUpperCase(word.charAt(0)));
                    if (word.length() > 1) {
                        builder.append(word.substring(1));
                    }
                }
                yield builder.isEmpty() ? dimensionId : builder.toString();
            }
        };
    }

    private void renderInfoTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, AggregatedItemEntry entry) {
        ItemStack stack = entry.displayStack();
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        List<Component> lines = new ArrayList<>();
        lines.add(stack.getHoverName().copy().withStyle(style -> style.withColor(0xFC0553)));
        lines.add(Component.translatable("screen.toomanychests.info_total", compactCount(entry.totalCount())).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("screen.toomanychests.info_stacks", entry.stackCount()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("screen.toomanychests.info_containers", entry.containerCount()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("screen.toomanychests.info_item_id", itemId).withStyle(ChatFormatting.DARK_GRAY));
        guiGraphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX + 12, mouseY + 12);
    }

    private boolean isRightMouseDown() {
        return this.minecraft != null && this.minecraft.mouseHandler.isRightPressed();
    }

    private boolean isInsideGrid(double mouseX, double mouseY) {
        return mouseX >= gridLeft() && mouseX < gridRight() && mouseY >= gridTop() && mouseY < gridBottom();
    }

    private int gridIndexAt(double mouseX, double mouseY) {
        if (!isInsideGrid(mouseX, mouseY)) {
            return -1;
        }

        int column = (int) ((mouseX - gridLeft()) / GRID_SLOT_SIZE);
        int row = (int) ((mouseY - gridTop()) / GRID_SLOT_SIZE);
        int index = (firstVisibleGridRow + row) * gridColumns() + column;
        return index < filteredEntries.size() ? index : -1;
    }

    private int gridLeft() {
        return listLeft() + 4;
    }

    private int gridTop() {
        return listTop() + 4;
    }

    private int gridRight() {
        return listRight() - 4;
    }

    private int gridBottom() {
        return listBottom() - 4;
    }

    private int gridColumns() {
        return Math.max(1, (gridRight() - gridLeft()) / GRID_SLOT_SIZE);
    }

    private int gridRows() {
        return Math.max(1, (gridBottom() - gridTop()) / GRID_SLOT_SIZE);
    }

    private int totalGridRows() {
        return Math.max(1, Math.ceilDiv(filteredEntries.size(), gridColumns()));
    }

    private int maxFirstVisibleGridRow() {
        return Math.max(0, totalGridRows() - gridRows());
    }

    private String compactCount(long value) {
        if (value >= 1_000_000_000L) {
            return String.format(Locale.ROOT, "%.1fB", value / 1_000_000_000.0D);
        }
        if (value >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0D);
        }
        if (value >= 10_000L) {
            return String.format(Locale.ROOT, "%.1fk", value / 1_000.0D);
        }
        return Long.toString(value);
    }

    private int listLeft() {
        return 12;
    }

    private int listRight() {
        return this.width - 12;
    }

    private int listTop() {
        return this.gridPanelTop;
    }

    private int listBottom() {
        return this.gridPanelBottom;
    }

    private Component filterButtonText() {
        return Component.translatable("screen.toomanychests.filter_mode", filterMode.label);
    }

    private Component enchantedButtonText() {
        return Component.translatable("screen.toomanychests.only_enchanted", enchantedOnly ? "On" : "Off");
    }

    private void drawBrandTitle(GuiGraphics guiGraphics, int centerX, int y, String suffix) {
        String prefixLeft = "(";
        String prefixCore = "TMC";
        String prefixRight = ") ";
        String body = "TooManyChests" + suffix;
        String full = prefixLeft + prefixCore + prefixRight + body;

        int x = centerX - this.font.width(full) / 2;
        guiGraphics.drawString(this.font, prefixLeft, x, y, WHITE);
        x += this.font.width(prefixLeft);
        guiGraphics.drawString(this.font, prefixCore, x, y, ACCENT);
        x += this.font.width(prefixCore);
        guiGraphics.drawString(this.font, prefixRight, x, y, WHITE);
        x += this.font.width(prefixRight);
        guiGraphics.drawString(this.font, body, x, y, WHITE);
    }

    private enum FilterMode {
        NAME_ONLY("Name"),
        ITEM_ID_ONLY("Item ID"),
        NAME_OR_ID("Name + ID");

        private final String label;

        FilterMode(String label) {
            this.label = label;
        }

        private FilterMode next() {
            FilterMode[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }
}
