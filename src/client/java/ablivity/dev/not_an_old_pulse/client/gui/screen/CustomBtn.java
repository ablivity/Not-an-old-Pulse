package ablivity.dev.not_an_old_pulse.client.gui.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import ablivity.dev.not_an_old_pulse.client.font.FontUtil;
import ablivity.dev.not_an_old_pulse.client.util.RenderUtils;

public class CustomBtn {
    int x, y, w, h;
    String text;
    Runnable action;
    float hoverAnim = 0f;
    boolean isRed;

    public CustomBtn(int x, int y, int w, int h, String text, boolean isRed, Runnable action) {
        this.x = x; this.y = y; this.w = w; this.h = h; this.text = text; this.isRed = isRed; this.action = action;
    }

    public CustomBtn(int x, int y, int w, int h, String text, Runnable action) {
        this(x, y, w, h, text, false, action);
    }

    public boolean isHovered(int mx, int my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public void render(DrawContext context, int mx, int my) {
        boolean hov = isHovered(mx, my);
        hoverAnim += (hov ? 1f : -1f) * 0.15f;
        if (hoverAnim < 0) hoverAnim = 0;
        if (hoverAnim > 1) hoverAnim = 1;

        int alpha = (int) (170 + 85 * hoverAnim);
        int rgb = isRed ? 0x800000 : 0x111114;
        if (isRed && hov) {
            rgb = 0xAA0000;
        }
        int color = (alpha << 24) | rgb;
        RenderUtils.drawSmoothRoundedRect(context, x, y, w, h, 2, color);

        MinecraftClient client = MinecraftClient.getInstance();
        int tw = FontUtil.getWidth(client.textRenderer, text);
        int textColor = hov ? 0xFFFFFFFF : 0xFFAAAAAA;
        FontUtil.drawTextWithShadow(context, client.textRenderer, text, x + (w - tw) / 2, y + (h - 9) / 2, textColor);
    }
}


