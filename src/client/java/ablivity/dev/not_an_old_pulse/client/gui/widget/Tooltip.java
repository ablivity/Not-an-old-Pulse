package ablivity.dev.not_an_old_pulse.client.gui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import ablivity.dev.not_an_old_pulse.client.gui.GuiColors;
import ablivity.dev.not_an_old_pulse.client.gui.RoundedRect;
import ablivity.dev.not_an_old_pulse.client.gui.animation.AnimatedValue;

/**
 * The dynamic tooltip that appears centered above the main window. Its
 * background is fully opaque at rest ({@link GuiColors#TOOLTIP_BG}); alpha
 * is only animated during the appear/disappear transition, per the
 * requested hover behaviour, not as a persistent translucent surface.
 */
public class Tooltip {
    private static final int PADDING_X = 10;
    private static final int PADDING_Y = 6;
    private static final int RADIUS = 6;

    private final AnimatedValue alpha = new AnimatedValue(0f, 0.35f);
    private String text = "";
    private int anchorCenterX;
    private int anchorBottomY;

    public void setAnchor(int centerX, int bottomY) {
        this.anchorCenterX = centerX;
        this.anchorBottomY = bottomY;
    }

    public void show(String text) {
        this.text = text;
        alpha.setTarget(1f);
    }

    public void hide() {
        alpha.setTarget(0f);
    }

    public void updateAnimation(float deltaSeconds) {
        alpha.update(deltaSeconds);
    }

    public void render(DrawContext context, TextRenderer textRenderer) {
        if (alpha.get() < 0.01f || text.isEmpty()) return;

        int a = Math.round(255 * alpha.get());
        int textWidth = ablivity.dev.not_an_old_pulse.client.font.FontUtil.getWidth(textRenderer, text);
        int width = textWidth + PADDING_X * 2;
        int height = textRenderer.fontHeight + PADDING_Y * 2;
        int x = anchorCenterX - width / 2;
        int y = anchorBottomY - height;

        RoundedRect.fill(context, x, y, width, height, RADIUS, GuiColors.withAlpha(GuiColors.TOOLTIP_BG, a));
        ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, textRenderer, text, x + PADDING_X, y + PADDING_Y,
            GuiColors.withAlpha(GuiColors.TEXT_PRIMARY, a));
    }
}

