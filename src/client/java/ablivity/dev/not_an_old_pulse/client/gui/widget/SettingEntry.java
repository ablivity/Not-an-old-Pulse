package ablivity.dev.not_an_old_pulse.client.gui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import ablivity.dev.not_an_old_pulse.client.gui.GuiColors;
import ablivity.dev.not_an_old_pulse.client.gui.RoundedRect;
import ablivity.dev.not_an_old_pulse.client.gui.animation.AnimatedValue;
import ablivity.dev.not_an_old_pulse.client.gui.animation.AnimationUtil;

/**
 * One module row: white label on the left, a {@link ToggleSwitch} on the
 * right. Hovering draws a subtle accent-colored ring around the whole
 * block and is also what the screen uses to decide whether to show the
 * dynamic tooltip.
 */
public class SettingEntry {
    private static final int RADIUS = 8;

    public final String label;
    public final String configKey;
    private final ToggleSwitch toggle;
    private final AnimatedValue hover = new AnimatedValue(0f, 0.3f);

    public int x, y, width, height;

    public SettingEntry(String label, String configKey, boolean initialValue) {
        this.label = label;
        this.configKey = configKey;
        this.toggle = new ToggleSwitch(initialValue);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean getValue() {
        return toggle.getValue();
    }

    public void updateAnimation(float deltaSeconds, boolean hovered) {
        hover.setTarget(hovered ? 1f : 0f);
        hover.update(deltaSeconds);
        toggle.updateAnimation(deltaSeconds);
    }

    public void render(DrawContext context, TextRenderer textRenderer) {
        float easedHover = AnimationUtil.easeOutCubic(hover.get());
        // At rest both the ring and fill match the window background, so
        // the row is invisible until hovered — a flat list, not cards.
        int ringColor = AnimationUtil.lerpColor(GuiColors.WINDOW_BG, GuiColors.ACCENT, easedHover);
        int bgColor = AnimationUtil.lerpColor(GuiColors.WINDOW_BG, GuiColors.MODULE_BG_HOVER, easedHover);

        RoundedRect.glowRing(context, x, y, width, height, RADIUS, 2, ringColor, bgColor);

        ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, textRenderer, label, x + 12, y + (height - 8) / 2, GuiColors.TEXT_PRIMARY);

        int toggleX = x + width - ToggleSwitch.WIDTH - 12;
        int toggleY = y + (height - ToggleSwitch.HEIGHT) / 2;
        toggle.render(context, toggleX, toggleY);
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public boolean isMouseOverToggle(int mouseX, int mouseY) {
        int toggleX = x + width - ToggleSwitch.WIDTH - 12;
        int toggleY = y + (height - ToggleSwitch.HEIGHT) / 2;
        return toggle.isMouseOver(toggleX, toggleY, mouseX, mouseY);
    }

    public void toggle() {
        toggle.toggle();
    }
}

