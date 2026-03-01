package net.z2six.toomanychests.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.util.Mth;

public class AccentButton extends Button {
    private static final int ACCENT = 0xFFFC0553;
    private static final int BG_NORMAL = 0xFF050505;
    private static final int BG_HOVERED = 0xFF0E0E0E;
    private static final int BG_DISABLED = 0xFF030303;
    private static final int BORDER_NORMAL = 0xFF1A1A1A;

    public AccentButton(Builder builder) {
        super(builder);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int left = this.getX();
        int top = this.getY();
        int right = left + this.getWidth();
        int bottom = top + this.getHeight();

        int background = !this.active ? BG_DISABLED : (this.isHoveredOrFocused() ? BG_HOVERED : BG_NORMAL);
        int border = this.isHoveredOrFocused() && this.active ? ACCENT : BORDER_NORMAL;

        guiGraphics.fill(left, top, right, bottom, background);
        guiGraphics.fill(left, top, right, top + 1, border);
        guiGraphics.fill(left, bottom - 1, right, bottom, border);
        guiGraphics.fill(left, top, left + 1, bottom, border);
        guiGraphics.fill(right - 1, top, right, bottom, border);

        int textColor = this.active ? 0xFFFFFFFF : 0xFF6A6A6A;
        int alpha = Mth.ceil(this.alpha * 255.0F) << 24;
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), left + this.getWidth() / 2, top + (this.getHeight() - 8) / 2, (textColor & 0x00FFFFFF) | alpha);
    }
}
