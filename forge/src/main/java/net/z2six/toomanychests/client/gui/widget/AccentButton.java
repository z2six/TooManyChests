package net.z2six.toomanychests.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.util.Mth;

public class AccentButton extends Button {
    private static final int ACCENT = 0xFFFC0553;
    private static final int BG_NORMAL = 0xFF050505;
    private static final int BG_HOVERED = 0xFF0E0E0E;
    private static final int BG_DISABLED = 0xFF030303;
    private static final int BORDER_NORMAL = 0xFF1A1A1A;

    public AccentButton(int x, int y, int width, int height, net.minecraft.network.chat.Component message, OnPress onPress, OnTooltip onTooltip) {
        super(x, y, width, height, message, onPress, onTooltip);
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        int left = this.x;
        int top = this.y;
        int right = left + this.width;
        int bottom = top + this.height;

        int background = !this.active ? BG_DISABLED : (this.isHoveredOrFocused() ? BG_HOVERED : BG_NORMAL);
        int border = this.isHoveredOrFocused() && this.active ? ACCENT : BORDER_NORMAL;

        fill(poseStack, left, top, right, bottom, background);
        fill(poseStack, left, top, right, top + 1, border);
        fill(poseStack, left, bottom - 1, right, bottom, border);
        fill(poseStack, left, top, left + 1, bottom, border);
        fill(poseStack, right - 1, top, right, bottom, border);

        int textColor = this.active ? 0xFFFFFFFF : 0xFF6A6A6A;
        int alpha = Mth.ceil(this.alpha * 255.0F) << 24;
        drawCenteredString(poseStack, Minecraft.getInstance().font, this.getMessage(), left + this.width / 2, top + (this.height - 8) / 2, (textColor & 0x00FFFFFF) | alpha);
    }
}
