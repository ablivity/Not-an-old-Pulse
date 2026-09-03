package ablivity.dev.not_an_old_pulse.client.util;

import net.minecraft.client.gui.DrawContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RenderUtils {

    private static final Map<Integer, CornerGeometry> CORNER_CACHE = new ConcurrentHashMap<>();
    private static final Map<Integer, int[]> CAPSULE_INSET_CACHE = new ConcurrentHashMap<>();
    private static final Map<Integer, int[]> CIRCLE_SPAN_CACHE = new ConcurrentHashMap<>();

    private RenderUtils() {
    }

    public static void drawSmoothRoundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0) return;
        if (radius <= 0) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }

        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + radius, y + height - radius, color);
        context.fill(x + width - radius, y + radius, x + width, y + height - radius, color);

        CornerGeometry geometry = CORNER_CACHE.computeIfAbsent(radius, RenderUtils::buildCornerGeometry);

        drawSmoothCorner(context, x + radius, y + radius, geometry, color, -1, -1);
        drawSmoothCorner(context, x + width - radius, y + radius, geometry, color, 1, -1);
        drawSmoothCorner(context, x + radius, y + height - radius, geometry, color, -1, 1);
        drawSmoothCorner(context, x + width - radius, y + height - radius, geometry, color, 1, 1);
    }

    public static void drawRoundedCapsule(DrawContext context, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) return;

        int[] insets = CAPSULE_INSET_CACHE.computeIfAbsent(height, RenderUtils::buildCapsuleInsets);

        for (int row = 0; row < height; row++) {
            int inset = insets[row];
            context.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
        }
    }

    public static void drawFilledCircle(DrawContext context, int x, int y, int size, int color) {
        if (size <= 0) return;

        int radius = size / 2;
        int centerX = x + radius;
        int centerY = y + radius;
        int[] spans = CIRCLE_SPAN_CACHE.computeIfAbsent(size, RenderUtils::buildCircleSpans);

        for (int row = 0; row < spans.length; row++) {
            int offsetY = row - radius;
            int halfWidth = spans[row];
            context.fill(centerX - halfWidth, centerY + offsetY, centerX + halfWidth + 1, centerY + offsetY + 1, color);
        }
    }

    public static void drawCircularTexture(DrawContext context, net.minecraft.util.Identifier texture, int x, int y, int size) {
        if (size <= 0) return;
        int[] insets = CAPSULE_INSET_CACHE.computeIfAbsent(size, RenderUtils::buildCapsuleInsets);
        for (int row = 0; row < size; row++) {
            int inset = insets[row];
            int rowW = size - inset * 2;
            if (rowW <= 0) continue;
            context.drawTexture(
                net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                texture,
                x + inset,
                y + row,
                (float) inset,
                (float) row,
                rowW,
                1,
                size,
                size
            );
        }
    }

    public static void drawRoundedTexture(DrawContext context, net.minecraft.util.Identifier texture, int x, int y, int width, int height, int radius) {
        if (width <= 0 || height <= 0) return;
        if (radius <= 0) {
            context.drawTexture(
                net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                texture,
                x,
                y,
                0.0f,
                0.0f,
                width,
                height,
                width,
                height
            );
            return;
        }

        radius = Math.min(radius, Math.min(width, height) / 2);
        int[] cornerInsets = new int[radius];
        for (int row = 0; row < radius; row++) {
            double distance = (radius - 1 - row) + 0.5;
            cornerInsets[row] = (int) Math.round(radius - Math.sqrt(Math.max(0.0, (double) radius * radius - distance * distance)));
        }

        // 1. Top corner rows
        for (int row = 0; row < radius; row++) {
            int inset = cornerInsets[row];
            int rowW = width - inset * 2;
            if (rowW <= 0) continue;
            context.drawTexture(
                net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                texture,
                x + inset,
                y + row,
                (float) inset,
                (float) row,
                rowW,
                1,
                width,
                height
            );
        }

        // 2. Middle quad
        int midHeight = height - radius * 2;
        if (midHeight > 0) {
            context.drawTexture(
                net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                texture,
                x,
                y + radius,
                0.0f,
                (float) radius,
                width,
                midHeight,
                width,
                height
            );
        }

        // 3. Bottom corner rows
        for (int i = 0; i < radius; i++) {
            int row = height - radius + i;
            int inset = cornerInsets[radius - 1 - i];
            int rowW = width - inset * 2;
            if (rowW <= 0) continue;
            context.drawTexture(
                net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                texture,
                x + inset,
                y + row,
                (float) inset,
                (float) row,
                rowW,
                1,
                width,
                height
            );
        }
    }

    private static CornerGeometry buildCornerGeometry(int radius) {
        CornerRow[] rows = new CornerRow[radius + 1];

        for (int j = 0; j <= radius; j++) {
            int fullEnd = -1;
            int edgeCount = 0;

            for (int i = 0; i <= radius; i++) {
                float distance = (float) Math.sqrt((double) i * i + (double) j * j);
                if (distance > radius) continue;

                if (distance <= radius - 1.0) fullEnd = i;
                else if (radius - distance > 0.0f) edgeCount++;
            }

            int[] edgeOffsets = new int[edgeCount];
            float[] edgeAlpha = new float[edgeCount];
            int edgeIndex = 0;

            for (int i = Math.max(0, fullEnd + 1); i <= radius; i++) {
                float distance = (float) Math.sqrt((double) i * i + (double) j * j);
                if (distance <= radius && distance > radius - 1.0f) {
                    float alpha = radius - distance;
                    if (alpha > 0.0f) {
                        edgeOffsets[edgeIndex] = i;
                        edgeAlpha[edgeIndex] = alpha;
                        edgeIndex++;
                    }
                }
            }

            rows[j] = new CornerRow(fullEnd, edgeOffsets, edgeAlpha);
        }

        return new CornerGeometry(rows);
    }

    private static int[] buildCapsuleInsets(int height) {
        int[] insets = new int[height];
        int radius = height / 2;

        for (int row = 0; row < height; row++) {
            double distance = row + 0.5 - radius;
            insets[row] = (int) Math.round(radius - Math.sqrt(Math.max(0.0, (double) radius * radius - distance * distance)));
        }

        return insets;
    }

    private static int[] buildCircleSpans(int size) {
        int radius = size / 2;
        int[] spans = new int[radius * 2 + 1];
        int radiusSquared = radius * radius;

        for (int row = 0; row < spans.length; row++) {
            int offsetY = row - radius;
            spans[row] = (int) Math.floor(Math.sqrt(Math.max(0, radiusSquared - offsetY * offsetY)));
        }

        return spans;
    }

    private static void drawSmoothCorner(DrawContext context, int centerX, int centerY, CornerGeometry geometry, int color, int directionX, int directionY) {
        int baseAlpha = (color >>> 24) & 0xFF;
        if (baseAlpha <= 0) return;

        int rgb = color & 0x00FFFFFF;
        CornerRow[] rows = geometry.rows();

        for (int j = 0; j < rows.length; j++) {
            CornerRow row = rows[j];
            int pixelY = directionY < 0 ? centerY - j - 1 : centerY + j;

            if (row.fullEnd() >= 0) {
                drawHorizontalRun(context, centerX, pixelY, 0, row.fullEnd(), directionX, color);
            }

            int[] offsets = row.edgeOffsets();
            float[] alphas = row.edgeAlpha();
            for (int index = 0; index < offsets.length; index++) {
                int finalAlpha = (int) (baseAlpha * alphas[index]);
                if (finalAlpha <= 0) continue;

                int pixelColor = (finalAlpha << 24) | rgb;
                drawHorizontalRun(context, centerX, pixelY, offsets[index], offsets[index], directionX, pixelColor);
            }
        }
    }

    private static void drawHorizontalRun(DrawContext context, int centerX, int y, int startOffset, int endOffset, int directionX, int color) {
        if (directionX < 0) {
            context.fill(centerX - endOffset - 1, y, centerX - startOffset, y + 1, color);
        } else {
            context.fill(centerX + startOffset, y, centerX + endOffset + 1, y + 1, color);
        }
    }

    private record CornerGeometry(CornerRow[] rows) {}
    private record CornerRow(int fullEnd, int[] edgeOffsets, float[] edgeAlpha) {}
}
