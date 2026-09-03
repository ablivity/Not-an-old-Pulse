package ablivity.dev.not_an_old_pulse.client.gui;

import net.minecraft.client.gui.DrawContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight rounded-rectangle drawing built entirely out of
 * {@link DrawContext#fill}. Every call is a solid, opaque fill — no
 * shaders, no alpha blending required for the shape itself, so it costs
 * almost nothing per frame and never produces blending artifacts.
 *
 * Corner geometry is now cached per radius to avoid per-frame sqrt calls.
 */
public final class RoundedRect {

    private static final Map<Integer, int[]> CORNER_DX_CACHE = new ConcurrentHashMap<>();

    private RoundedRect() {}

    public static void fill(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0) return;
        radius = Math.max(0, Math.min(radius, Math.min(width, height) / 2));

        if (radius == 0) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }

        // Center cross (covers everything except the four corners).
        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + radius, y + height - radius, color);
        context.fill(x + width - radius, y + radius, x + width, y + height - radius, color);

        // Corners, drawn as stacked horizontal scanlines of a quarter circle.
        int[] dxValues = CORNER_DX_CACHE.computeIfAbsent(radius, RoundedRect::buildCornerDx);
        fillCorner(context, x + radius, y + radius, radius, color, -1, -1, dxValues);
        fillCorner(context, x + width - radius, y + radius, radius, color, 1, -1, dxValues);
        fillCorner(context, x + radius, y + height - radius, radius, color, -1, 1, dxValues);
        fillCorner(context, x + width - radius, y + height - radius, radius, color, 1, 1, dxValues);
    }

    /**
     * Draws a rounded rect with an accent-colored ring around it by filling
     * a slightly larger rounded rect behind a normal one. Used for the
     * module hover "glow": pass {@code ringColor} already lerped between
     * the module background (invisible ring) and the accent color.
     */
    public static void glowRing(DrawContext context, int x, int y, int width, int height, int radius,
                                 int ringThickness, int ringColor, int innerColor) {
        fill(context, x - ringThickness, y - ringThickness, width + ringThickness * 2, height + ringThickness * 2,
            radius + ringThickness, ringColor);
        fill(context, x, y, width, height, radius, innerColor);
    }

    private static int[] buildCornerDx(int radius) {
        int[] dx = new int[radius + 1];
        for (int dy = 0; dy <= radius; dy++) {
            dx[dy] = (int) Math.round(Math.sqrt((double) radius * radius - (double) dy * dy));
        }
        return dx;
    }

    private static void fillCorner(DrawContext context, int cx, int cy, int radius, int color, int signX, int signY, int[] dxValues) {
        for (int dy = 0; dy <= radius; dy++) {
            int dx = dxValues[dy];
            int x0 = signX < 0 ? cx - dx : cx;
            int x1 = signX < 0 ? cx : cx + dx;
            int y0 = signY < 0 ? cy - dy : cy + dy;
            context.fill(x0, y0, x1, y0 + 1, color);
        }
    }
}

