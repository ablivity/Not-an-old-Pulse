package ablivity.dev.not_an_old_pulse.client.gui.widget;

import net.minecraft.client.gui.DrawContext;
import ablivity.dev.not_an_old_pulse.client.gui.GuiColors;
import ablivity.dev.not_an_old_pulse.client.gui.RoundedRect;
import ablivity.dev.not_an_old_pulse.client.gui.animation.AnimatedValue;
import ablivity.dev.not_an_old_pulse.client.gui.animation.AnimationUtil;

/**
 * A single on/off switch. The thumb position and the track color are both
 * driven by one animated progress value (0 = off, 1 = on), interpolated
 * gray -> blue as requested.
 */
public class ToggleSwitch {
    public static final int WIDTH = 38;
    public static final int HEIGHT = 20;
    private static final int THUMB_PADDING = 3;
    private static final int THUMB_SIZE = HEIGHT - THUMB_PADDING * 2;

    private boolean value;
    private final AnimatedValue progress;

    public ToggleSwitch(boolean initialValue) {
        this.value = initialValue;
        this.progress = new AnimatedValue(initialValue ? 1f : 0f, 0.35f);
    }

    public boolean getValue() {
        return value;
    }

    public void toggle() {
        value = !value;
        progress.setTarget(value ? 1f : 0f);
    }

    public void updateAnimation(float deltaSeconds) {
        progress.update(deltaSeconds);
    }

    public void render(DrawContext context, int x, int y) {
        float eased = AnimationUtil.easeOutCubic(progress.get());

        int trackColor = AnimationUtil.lerpColor(GuiColors.TOGGLE_OFF, GuiColors.TOGGLE_ON, eased);
        RoundedRect.fill(context, x, y, WIDTH, HEIGHT, HEIGHT / 2, trackColor);

        int thumbX = x + THUMB_PADDING + Math.round(eased * (WIDTH - THUMB_SIZE - THUMB_PADDING * 2));
        int thumbY = y + THUMB_PADDING;
        RoundedRect.fill(context, thumbX, thumbY, THUMB_SIZE, THUMB_SIZE, THUMB_SIZE / 2, GuiColors.TOGGLE_THUMB);
    }

    public boolean isMouseOver(int x, int y, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
    }
}

