package net.z2six.toomanychests.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.z2six.toomanychests.client.config.StackingMode;
import net.z2six.toomanychests.client.config.TrackerConfigManager;
import net.z2six.toomanychests.client.gui.widget.AccentButton;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class TrackerSettingsScreen extends Screen {
    private static final int ACCENT = 0xFFFC0553;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int PANEL_FILL = 0xE6000000;
    private static final int PANEL_BORDER = 0xFF1A1A1A;
    private static final int CONTROL_HEIGHT = 20;

    private final TrackerScreen parent;
    private StackingMode stackingMode;

    private EditBox highlightDurationBox;
    private EditBox trackedBlocksBox;
    private EditBox indicatorCrosshairColorBox;
    private EditBox indicatorLabelColorBox;
    private EditBox chestOutlineColorBox;

    private int panelLeft;
    private int panelRight;
    private int panelTop;
    private int panelBottom;

    public TrackerSettingsScreen(TrackerScreen parent) {
        super(Component.translatable("screen.toomanychests.settings_title"));
        this.parent = parent;
        this.stackingMode = TrackerConfigManager.stackingMode();
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(560, this.width - 40);
        panelLeft = (this.width - panelWidth) / 2;
        panelRight = panelLeft + panelWidth;
        panelTop = 28;

        int innerLeft = panelLeft + 10;
        int innerWidth = panelWidth - 20;
        int y = panelTop + 18;

        this.addRenderableWidget(
                Button.builder(stackingButtonText(), button -> {
                            stackingMode = stackingMode.next();
                            button.setMessage(stackingButtonText());
                        })
                        .tooltip(Tooltip.create(Component.translatable("screen.toomanychests.tooltip_stacking_mode")))
                        .bounds(innerLeft, y, innerWidth, CONTROL_HEIGHT)
                        .build(AccentButton::new)
        );
        y += 28;

        this.highlightDurationBox = makeEditBox(
                innerLeft,
                y + 10,
                innerWidth,
                Component.translatable("screen.toomanychests.highlight_seconds"),
                Component.translatable("screen.toomanychests.tooltip_highlight_seconds")
        );
        this.highlightDurationBox.setFilter(value -> value.isEmpty() || value.matches("\\d+"));
        this.highlightDurationBox.setValue(Integer.toString(TrackerConfigManager.highlightDurationSeconds()));
        this.addRenderableWidget(this.highlightDurationBox);
        y += 40;

        this.trackedBlocksBox = makeEditBox(
                innerLeft,
                y + 10,
                innerWidth,
                Component.translatable("screen.toomanychests.tracked_blocks"),
                Component.translatable("screen.toomanychests.tooltip_tracked_blocks")
        );
        this.trackedBlocksBox.setValue(String.join(", ", TrackerConfigManager.trackedBlocksList()));
        this.addRenderableWidget(this.trackedBlocksBox);
        y += 40;

        this.indicatorCrosshairColorBox = makeEditBox(
                innerLeft,
                y + 10,
                innerWidth,
                Component.translatable("screen.toomanychests.indicator_crosshair_color"),
                Component.translatable("screen.toomanychests.tooltip_indicator_crosshair_color")
        );
        this.indicatorCrosshairColorBox.setFilter(value -> value.isEmpty() || value.matches("#?[0-9a-fA-F]{0,6}"));
        this.indicatorCrosshairColorBox.setValue(TrackerConfigManager.indicatorCrosshairColorHex());
        this.addRenderableWidget(this.indicatorCrosshairColorBox);
        y += 40;

        this.indicatorLabelColorBox = makeEditBox(
                innerLeft,
                y + 10,
                innerWidth,
                Component.translatable("screen.toomanychests.indicator_label_color"),
                Component.translatable("screen.toomanychests.tooltip_indicator_label_color")
        );
        this.indicatorLabelColorBox.setFilter(value -> value.isEmpty() || value.matches("#?[0-9a-fA-F]{0,6}"));
        this.indicatorLabelColorBox.setValue(TrackerConfigManager.indicatorLabelColorHex());
        this.addRenderableWidget(this.indicatorLabelColorBox);
        y += 40;

        this.chestOutlineColorBox = makeEditBox(
                innerLeft,
                y + 10,
                innerWidth,
                Component.translatable("screen.toomanychests.chest_outline_color"),
                Component.translatable("screen.toomanychests.tooltip_chest_outline_color")
        );
        this.chestOutlineColorBox.setFilter(value -> value.isEmpty() || value.matches("#?[0-9a-fA-F]{0,6}"));
        this.chestOutlineColorBox.setValue(TrackerConfigManager.chestOutlineColorHex());
        this.addRenderableWidget(this.chestOutlineColorBox);
        y += 50;

        int buttonWidth = (innerWidth - 8) / 2;
        this.addRenderableWidget(
                Button.builder(Component.translatable("gui.done"), button -> {
                            applyChanges();
                            this.parent.refreshFromSettings();
                            this.minecraft.setScreen(parent);
                        })
                        .tooltip(Tooltip.create(Component.translatable("screen.toomanychests.tooltip_done")))
                        .bounds(innerLeft, y, buttonWidth, CONTROL_HEIGHT)
                        .build(AccentButton::new)
        );

        this.addRenderableWidget(
                Button.builder(Component.translatable("gui.cancel"), button -> this.minecraft.setScreen(parent))
                        .tooltip(Tooltip.create(Component.translatable("screen.toomanychests.tooltip_cancel")))
                        .bounds(innerLeft + buttonWidth + 8, y, buttonWidth, CONTROL_HEIGHT)
                        .build(AccentButton::new)
        );

        panelBottom = y + CONTROL_HEIGHT + 12;
    }

    private EditBox makeEditBox(int x, int y, int width, Component narration, Component tooltip) {
        EditBox box = new EditBox(this.font, x, y, width, CONTROL_HEIGHT, narration);
        box.setTextColor(0xFFFFFFFF);
        box.setTextColorUneditable(0xFF6A6A6A);
        box.setTooltip(Tooltip.create(tooltip));
        return box;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, PANEL_FILL);
        guiGraphics.fill(panelLeft - 1, panelTop - 1, panelRight + 1, panelTop, PANEL_BORDER);
        guiGraphics.fill(panelLeft - 1, panelBottom, panelRight + 1, panelBottom + 1, PANEL_BORDER);
        guiGraphics.fill(panelLeft - 1, panelTop - 1, panelLeft, panelBottom + 1, PANEL_BORDER);
        guiGraphics.fill(panelRight, panelTop - 1, panelRight + 1, panelBottom + 1, PANEL_BORDER);

        for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        int innerLeft = panelLeft + 10;
        int y = panelTop + 6;
        drawBrandTitle(guiGraphics, (panelLeft + panelRight) / 2, y, " Settings");
        y += 38;
        guiGraphics.drawString(this.font, Component.translatable("screen.toomanychests.highlight_seconds"), innerLeft, y, 0xFFD7D7D7);
        y += 40;
        guiGraphics.drawString(this.font, Component.translatable("screen.toomanychests.tracked_blocks"), innerLeft, y, 0xFFD7D7D7);
        y += 40;
        guiGraphics.drawString(this.font, Component.translatable("screen.toomanychests.indicator_crosshair_color"), innerLeft, y, 0xFFD7D7D7);
        y += 40;
        guiGraphics.drawString(this.font, Component.translatable("screen.toomanychests.indicator_label_color"), innerLeft, y, 0xFFD7D7D7);
        y += 40;
        guiGraphics.drawString(this.font, Component.translatable("screen.toomanychests.chest_outline_color"), innerLeft, y, 0xFFD7D7D7);
    }

    private void applyChanges() {
        int highlightSeconds = parseIntOrDefault(highlightDurationBox.getValue(), TrackerConfigManager.highlightDurationSeconds());
        List<String> trackedBlocks = Arrays.stream(trackedBlocksBox.getValue().split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
        String crosshairColor = normalizeHexColor(indicatorCrosshairColorBox.getValue());
        String labelColor = normalizeHexColor(indicatorLabelColorBox.getValue());
        String chestOutlineColor = normalizeHexColor(chestOutlineColorBox.getValue());
        TrackerConfigManager.updateFromScreen(stackingMode, highlightSeconds, trackedBlocks, crosshairColor, labelColor, chestOutlineColor);
    }

    private int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private Component stackingButtonText() {
        return Component.translatable("screen.toomanychests.stacking_mode", stackingMode.label());
    }

    private String normalizeHexColor(String value) {
        String color = value == null ? "" : value.trim();
        if (!color.isEmpty() && !color.startsWith("#")) {
            color = "#" + color;
        }
        return color;
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
}
