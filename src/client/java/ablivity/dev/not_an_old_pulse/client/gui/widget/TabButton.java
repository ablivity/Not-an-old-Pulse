package ablivity.dev.not_an_old_pulse.client.gui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import ablivity.dev.not_an_old_pulse.client.gui.GuiColors;
import ablivity.dev.not_an_old_pulse.client.gui.animation.AnimatedValue;
import ablivity.dev.not_an_old_pulse.client.gui.animation.AnimationUtil;

/**
 * A single tab label. The active/inactive background rectangle that slides
 * between tabs is owned and animated by the screen (one shared
 * {@link AnimatedValue} for its X position) — this class only renders its
 * own text and tracks its own hover fade.
 */
public class TabButton {
    public int x, y, width, height;
    public final String label;
    private boolean active;
    private final AnimatedValue hover = new AnimatedValue(0f, 0.3f);

    public TabButton(String label) {
        this.label = label;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public void updateAnimation(float deltaSeconds, boolean hovered) {
        hover.setTarget(hovered ? 1f : 0f);
        hover.update(deltaSeconds);
    }

    public void render(DrawContext context, TextRenderer textRenderer) {
        int textColor = active
            ? GuiColors.TEXT_TAB_ACTIVE
            : AnimationUtil.lerpColor(GuiColors.TEXT_TAB_INACTIVE, GuiColors.TEXT_TAB_ACTIVE,
                AnimationUtil.easeOutCubic(hover.get()) * 0.6f);

        ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawCenteredTextWithShadow(context, textRenderer, label, x + width / 2, y + (height - 8) / 2, textColor);
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}

