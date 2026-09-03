package ablivity.dev.not_an_old_pulse.client.gui.widget;

import net.minecraft.client.gui.DrawContext;
import ablivity.dev.not_an_old_pulse.client.gui.GuiColors;
import ablivity.dev.not_an_old_pulse.client.gui.RoundedRect;
import ablivity.dev.not_an_old_pulse.client.gui.animation.AnimatedValue;
import ablivity.dev.not_an_old_pulse.client.gui.animation.AnimationUtil;

/**
 * Top-right theme toggle icon. Draws a simple sun / moon glyph out of
 * solid fills — the "moon" is just a circle with a smaller opaque circle
 * (in the background color) cut out of one side, no alpha needed.
 */
public class ThemeToggleButton {
    public int x, y, size;
    private boolean dark = true;
    private final AnimatedValue hover = new AnimatedValue(0f, 0.3f);

    public void setBounds(int x, int y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
    }

    public boolean isDark() {
        return dark;
    }

    public void toggle() {
        dark = !dark;
    }

    public void updateAnimation(float deltaSeconds, boolean hovered) {
        hover.setTarget(hovered ? 1f : 0f);
        hover.update(deltaSeconds);
    }

    public void render(DrawContext context, int surroundingBgColor) {
        float eased = AnimationUtil.easeOutCubic(hover.get());
        int iconColor = AnimationUtil.lerpColor(GuiColors.THEME_ICON, GuiColors.THEME_ICON_HOVER, eased);

        int cx = x + size / 2;
        int cy = y + size / 2;
        int radius = size / 3;

        RoundedRect.fill(context, cx - radius, cy - radius, radius * 2, radius * 2, radius, iconColor);

        if (dark) {
            int cutRadius = (int) (radius * 0.85f);
            int cutOffset = (int) (radius * 0.55f);
            RoundedRect.fill(context, cx - cutRadius + cutOffset, cy - cutRadius, cutRadius * 2, cutRadius * 2,
                cutRadius, surroundingBgColor);
        } else {
            int rayLength = (int) (radius * 0.6f);
            int rayThickness = 2;
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * i / 4.0;
                int rx1 = cx + (int) (Math.cos(angle) * (radius + 2));
                int ry1 = cy + (int) (Math.sin(angle) * (radius + 2));
                int rx2 = cx + (int) (Math.cos(angle) * (radius + 2 + rayLength));
                int ry2 = cy + (int) (Math.sin(angle) * (radius + 2 + rayLength));
                context.fill(Math.min(rx1, rx2), Math.min(ry1, ry2),
                    Math.max(rx1, rx2) + rayThickness, Math.max(ry1, ry2) + rayThickness, iconColor);
            }
        }
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }
}

