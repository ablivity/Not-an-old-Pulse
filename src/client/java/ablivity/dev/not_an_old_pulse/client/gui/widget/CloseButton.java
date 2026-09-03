package ablivity.dev.not_an_old_pulse.client.gui.widget;

import net.minecraft.client.gui.DrawContext;
import ablivity.dev.not_an_old_pulse.client.gui.GuiColors;
import ablivity.dev.not_an_old_pulse.client.gui.RoundedRect;
import ablivity.dev.not_an_old_pulse.client.gui.animation.AnimatedValue;
import ablivity.dev.not_an_old_pulse.client.gui.animation.AnimationUtil;

/** A plain filled red dot, macOS-traffic-light style — no cross icon. */
public class CloseButton {
    public int x, y, size;
    private final AnimatedValue hover = new AnimatedValue(0f, 0.3f);

    public void setBounds(int x, int y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
    }

    public void updateAnimation(float deltaSeconds, boolean hovered) {
        hover.setTarget(hovered ? 1f : 0f);
        hover.update(deltaSeconds);
    }

    public void render(DrawContext context) {
        float eased = AnimationUtil.easeOutCubic(hover.get());
        int color = AnimationUtil.lerpColor(GuiColors.CLOSE_BUTTON, GuiColors.CLOSE_BUTTON_HOVER, eased);
        RoundedRect.fill(context, x, y, size, size, size / 2, color);
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }
}

