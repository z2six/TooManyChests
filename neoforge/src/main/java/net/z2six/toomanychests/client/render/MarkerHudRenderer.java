package net.z2six.toomanychests.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MarkerHudRenderer {
    private static final int EDGE_MARGIN = 20;
    private static final int LABEL_PADDING = 3;
    private static final int SCREEN_PADDING = 2;
    private static final int LABEL_STEP = 14;
    private static List<ProjectedMarker> projectedMarkers = List.of();

    private MarkerHudRenderer() {
    }

    public static void updateProjectedMarkers(List<ProjectedMarker> markers) {
        projectedMarkers = List.copyOf(markers);
    }

    public static void clearProjectedMarkers() {
        projectedMarkers = List.of();
    }

    public static void render(RenderGuiEvent.Post event) {
        if (projectedMarkers.isEmpty()) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();
        List<Rect> occupiedLabels = new ArrayList<>();

        for (ProjectedMarker marker : projectedMarkers) {
            String distanceText = formatDistance(marker.distance());
            if (marker.onScreen(screenWidth, screenHeight)) {
                drawOnScreenMarker(
                        guiGraphics,
                        (int) Math.round(marker.screenX()),
                        (int) Math.round(marker.screenY()),
                        distanceText,
                        marker.markerColor(),
                        marker.textColor(),
                        screenWidth,
                        screenHeight,
                        marker.screenX() - screenWidth / 2.0D,
                        marker.screenY() - screenHeight / 2.0D,
                        occupiedLabels
                );
            } else {
                drawOffScreenIndicator(
                        guiGraphics,
                        marker.directionX(),
                        marker.directionY(),
                        screenWidth,
                        screenHeight,
                        distanceText,
                        marker.behind(),
                        marker.textColor(),
                        occupiedLabels
                );
            }
        }
    }

    private static void drawOnScreenMarker(
            GuiGraphics guiGraphics,
            int x,
            int y,
            String distanceText,
            int markerColor,
            int textColor,
            int screenWidth,
            int screenHeight,
            double dirX,
            double dirY,
            List<Rect> occupiedLabels
    ) {
        guiGraphics.fill(x - 4, y - 1, x + 5, y + 2, markerColor);
        guiGraphics.fill(x - 1, y - 4, x + 2, y + 5, markerColor);

        Font font = Minecraft.getInstance().font;
        drawClampedLabel(
                guiGraphics,
                font,
                distanceText,
                x - font.width(distanceText) / 2,
                y - 15,
                textColor,
                screenWidth,
                screenHeight,
                dirX,
                dirY,
                occupiedLabels
        );
    }

    private static void drawOffScreenIndicator(
            GuiGraphics guiGraphics,
            double directionX,
            double directionY,
            int screenWidth,
            int screenHeight,
            String distanceText,
            boolean behind,
            int textColor,
            List<Rect> occupiedLabels
    ) {
        if (Math.abs(directionX) < 0.001D && Math.abs(directionY) < 0.001D) {
            directionY = -1.0D;
        }

        double length = Math.sqrt(directionX * directionX + directionY * directionY);
        double normX = directionX / length;
        double normY = directionY / length;
        double ringRadius = Math.max(40.0D, Math.min(screenWidth, screenHeight) * 0.5D - EDGE_MARGIN);
        int x = (int) Math.round(screenWidth / 2.0D + normX * ringRadius);
        int y = (int) Math.round(screenHeight / 2.0D + normY * ringRadius);
        String arrow = directionalArrow(normX, normY);
        String label = behind ? arrow + " " + distanceText + " (behind)" : arrow + " " + distanceText;

        Font font = Minecraft.getInstance().font;
        drawClampedLabel(
                guiGraphics,
                font,
                label,
                x - font.width(label) / 2,
                y - 4,
                textColor,
                screenWidth,
                screenHeight,
                normX,
                normY,
                occupiedLabels
        );
    }

    private static String directionalArrow(double normX, double normY) {
        double angle = Math.atan2(normY, normX);
        int bucket = (int) Math.round(angle / (Math.PI / 4.0D));
        return switch (Math.floorMod(bucket, 8)) {
            case 0 -> "\u2192";
            case 1 -> "\u2198";
            case 2 -> "\u2193";
            case 3 -> "\u2199";
            case 4 -> "\u2190";
            case 5 -> "\u2196";
            case 6 -> "\u2191";
            default -> "\u2197";
        };
    }

    private static String formatDistance(double distance) {
        if (distance >= 1000.0D) {
            return String.format(Locale.ROOT, "%.1fk blocks", distance / 1000.0D);
        }
        return Integer.toString((int) Math.round(distance)) + " blocks";
    }

    private static void drawClampedLabel(
            GuiGraphics guiGraphics,
            Font font,
            String text,
            int idealTextX,
            int idealTextY,
            int textColor,
            int screenWidth,
            int screenHeight,
            double directionX,
            double directionY,
            List<Rect> occupiedLabels
    ) {
        int maxTextWidth = Math.max(8, screenWidth - 2 * (SCREEN_PADDING + LABEL_PADDING));
        String finalText = text;
        if (font.width(finalText) > maxTextWidth) {
            String ellipsis = "...";
            int ellipsisWidth = font.width(ellipsis);
            finalText = ellipsisWidth >= maxTextWidth
                    ? font.plainSubstrByWidth(finalText, maxTextWidth)
                    : font.plainSubstrByWidth(finalText, maxTextWidth - ellipsisWidth) + ellipsis;
        }

        int textWidth = font.width(finalText);
        int minTextX = SCREEN_PADDING + LABEL_PADDING;
        int maxTextX = Math.max(minTextX, screenWidth - SCREEN_PADDING - LABEL_PADDING - textWidth);
        int textX = Mth.clamp(idealTextX, minTextX, maxTextX);

        int minTextY = SCREEN_PADDING + LABEL_PADDING;
        int maxTextY = Math.max(minTextY, screenHeight - SCREEN_PADDING - LABEL_PADDING - font.lineHeight);
        int baseY = Mth.clamp(idealTextY, minTextY, maxTextY);

        double length = Math.sqrt(directionX * directionX + directionY * directionY);
        double normX = length < 0.001D ? 1.0D : directionX / length;
        double normY = length < 0.001D ? 0.0D : directionY / length;
        double tangentX = -normY;
        double tangentY = normX;

        int chosenX = textX;
        int chosenY = baseY;
        Rect chosenRect = buildRect(textX, baseY, textWidth, font.lineHeight);
        boolean found = false;
        for (int attempt = 0; attempt < 13; attempt++) {
            int offsetStep = signedStep(attempt);
            int candidateX = Mth.clamp(textX + (int) Math.round(tangentX * offsetStep * LABEL_STEP), minTextX, maxTextX);
            int candidateY = Mth.clamp(baseY + (int) Math.round(tangentY * offsetStep * LABEL_STEP), minTextY, maxTextY);
            Rect candidateRect = buildRect(candidateX, candidateY, textWidth, font.lineHeight);
            if (!intersectsAny(candidateRect, occupiedLabels)) {
                chosenX = candidateX;
                chosenY = candidateY;
                chosenRect = candidateRect;
                found = true;
                break;
            }
        }

        if (!found) {
            for (int attempt = 1; attempt < 7; attempt++) {
                int candidateY = Mth.clamp(baseY + attempt * LABEL_STEP, minTextY, maxTextY);
                Rect candidateRect = buildRect(textX, candidateY, textWidth, font.lineHeight);
                if (!intersectsAny(candidateRect, occupiedLabels)) {
                    chosenY = candidateY;
                    chosenRect = candidateRect;
                    break;
                }
            }
        }

        if (intersectsAny(chosenRect, occupiedLabels)) {
            return;
        }

        occupiedLabels.add(chosenRect);
        guiGraphics.fill(chosenRect.left, chosenRect.top, chosenRect.right, chosenRect.bottom, 0xB0000000);
        guiGraphics.drawString(font, finalText, chosenX, chosenY, textColor);
    }

    private static boolean intersectsAny(Rect rect, List<Rect> occupied) {
        for (Rect other : occupied) {
            if (rect.left < other.right && rect.right > other.left && rect.top < other.bottom && rect.bottom > other.top) {
                return true;
            }
        }
        return false;
    }

    private static int signedStep(int attempt) {
        if (attempt == 0) {
            return 0;
        }
        int step = (attempt + 1) / 2;
        return attempt % 2 == 0 ? -step : step;
    }

    private static Rect buildRect(int textX, int textY, int textWidth, int lineHeight) {
        return new Rect(textX - LABEL_PADDING, textY - 2, textX + textWidth + LABEL_PADDING, textY + lineHeight + 1);
    }

    public record ProjectedMarker(
            double screenX,
            double screenY,
            double directionX,
            double directionY,
            double distance,
            boolean behind,
            int markerColor,
            int textColor
    ) {
        private boolean onScreen(int screenWidth, int screenHeight) {
            return !behind
                    && screenX >= EDGE_MARGIN
                    && screenX <= screenWidth - EDGE_MARGIN
                    && screenY >= EDGE_MARGIN
                    && screenY <= screenHeight - EDGE_MARGIN;
        }
    }

    private record Rect(int left, int top, int right, int bottom) {
    }
}
