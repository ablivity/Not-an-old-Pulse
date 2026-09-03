package ablivity.dev.not_an_old_pulse.client.gui.widget;

import net.minecraft.client.gui.DrawContext;
import ablivity.dev.not_an_old_pulse.client.gui.GuiColors;
import ablivity.dev.not_an_old_pulse.client.gui.RoundedRect;
import ablivity.dev.not_an_old_pulse.client.gui.animation.AnimatedValue;
import ablivity.dev.not_an_old_pulse.client.gui.animation.AnimationUtil;

/** One of the four sidebar icon buttons (Settings, Visuals, Social, Calendar). */
public class SidebarButton {
    public enum Icon { SETTINGS, VISUALS, SOCIAL, CALENDAR }

    public int x, y, size;
    public final Icon icon;
    private boolean active;
    private final AnimatedValue activeAnim;
    private final AnimatedValue hover = new AnimatedValue(0f, 0.3f);

    public SidebarButton(Icon icon, boolean active) {
        this.icon = icon;
        this.active = active;
        this.activeAnim = new AnimatedValue(active ? 1f : 0f, 0.3f);
    }

    public void setBounds(int x, int y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
    }

    public void setActive(boolean active) {
        this.active = active;
        activeAnim.setTarget(active ? 1f : 0f);
    }

    public boolean isActive() {
        return active;
    }

    public void updateAnimation(float deltaSeconds, boolean hovered) {
        hover.setTarget(hovered ? 1f : 0f);
        hover.update(deltaSeconds);
        activeAnim.update(deltaSeconds);
    }

    public void render(DrawContext context) {
        float easedActive = AnimationUtil.easeOutCubic(activeAnim.get());
        float easedHover = AnimationUtil.easeOutCubic(hover.get());

        int bgColor = AnimationUtil.lerpColor(GuiColors.SIDEBAR_BG, GuiColors.ACCENT, easedActive * 0.9f);
        if (easedActive < 0.05f && easedHover > 0f) {
            bgColor = AnimationUtil.lerpColor(GuiColors.SIDEBAR_BG, GuiColors.MODULE_BG, easedHover);
        }
        RoundedRect.fill(context, x, y, size, size, 10, bgColor);

        int iconColor = AnimationUtil.lerpColor(GuiColors.TEXT_SECONDARY, GuiColors.TEXT_PRIMARY,
            Math.max(easedActive, easedHover));

        int cx = x + size / 2;
        int cy = y + size / 2;
        int r = size / 4;

        switch (icon) {
            case SETTINGS -> drawGear(context, cx, cy, r, iconColor, bgColor);
            case VISUALS -> drawEye(context, cx, cy, r, iconColor);
            case SOCIAL -> drawPeople(context, cx, cy, r, iconColor);
            case CALENDAR -> drawCalendar(context, cx, cy, r, iconColor, bgColor);
        }
    }

    private void drawGear(DrawContext context, int cx, int cy, int r, int color, int bgColor) {
        RoundedRect.fill(context, cx - r, cy - r, r * 2, r * 2, r / 2, color);
        int toothLen = Math.max(2, r / 2);
        context.fill(cx - 1, cy - r - toothLen, cx + 2, cy - r, color);
        context.fill(cx - 1, cy + r, cx + 2, cy + r + toothLen, color);
        context.fill(cx - r - toothLen, cy - 1, cx - r, cy + 2, color);
        context.fill(cx + r, cy - 1, cx + r + toothLen, cy + 2, color);
        int holeR = Math.max(1, r / 2);
        RoundedRect.fill(context, cx - holeR, cy - holeR, holeR * 2, holeR * 2, holeR, bgColor);
    }

    private void drawEye(DrawContext context, int cx, int cy, int r, int color) {
        context.fill(cx - r, cy - 1, cx + r, cy + 2, color);
        int pupilR = Math.max(2, r / 2);
        RoundedRect.fill(context, cx - pupilR, cy - pupilR, pupilR * 2, pupilR * 2, pupilR, color);
    }

    private void drawPeople(DrawContext context, int cx, int cy, int r, int color) {
        int headR = Math.max(2, r / 2);
        RoundedRect.fill(context, cx - headR, cy - r, headR * 2, headR * 2, headR, color);
        int bodyW = (int) (r * 1.4f);
        RoundedRect.fill(context, cx - bodyW / 2, cy, bodyW, r, 3, color);
    }

    private void drawCalendar(DrawContext context, int cx, int cy, int r, int color, int bgColor) {
        RoundedRect.fill(context, cx - r, cy - r, r * 2, r * 2, 3, color);
        int headerH = Math.max(1, r / 2);
        context.fill(cx - r, cy - r, cx + r, cy - r + headerH, bgColor);
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }
}

