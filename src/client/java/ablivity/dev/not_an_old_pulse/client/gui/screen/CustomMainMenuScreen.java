package ablivity.dev.not_an_old_pulse.client.gui.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import ablivity.dev.not_an_old_pulse.client.font.FontUtil;
import ablivity.dev.not_an_old_pulse.client.util.RenderUtils;

import java.util.ArrayList;
import java.util.List;

public class CustomMainMenuScreen extends TitleScreen {

    public static boolean forceVanillaMenu = false;

    private final List<CustomBtn> buttons = new ArrayList<>();

    public CustomMainMenuScreen() {
        super(false);
    }

    @Override
    protected void init() {
        super.init();
        this.clearChildren();

        buttons.clear();

        int centerX = width / 2;
        int centerY = height / 2;

        int btnW = 200;
        int btnH = 24;

        buttons.add(new CustomBtn(centerX - btnW / 2, centerY - 16, btnW, btnH, "Одиночная игра", () -> {
            client.setScreen(new SelectWorldScreen(this));
        }));

        buttons.add(new CustomBtn(centerX - btnW / 2, centerY + 12, btnW, btnH, "Сетевая игра", () -> {
            client.setScreen(new MultiplayerScreen(this));
        }));

        int smallW = 24;
        int spacing = 8;
        int totalSmallW = smallW * 3 + spacing * 2;
        int startX = centerX - totalSmallW / 2;
        int smallY = centerY + 48;

        buttons.add(new CustomBtn(startX, smallY, smallW, smallW, "\u2699", () -> {
            client.setScreen(new OptionsScreen(this, client.options));
        }));
        
        buttons.add(new CustomBtn(startX + (smallW + spacing), smallY, smallW, smallW, "A", () -> {
            client.setScreen(new LanguageOptionsScreen(this, client.options, client.getLanguageManager()));
        }));
        
        buttons.add(new CustomBtn(startX + (smallW + spacing) * 2, smallY, smallW, smallW, "X", true, () -> {
            client.scheduleStop();
        }));

        buttons.add(new CustomBtn(width - 32, 8, 24, 24, "\u21BA", () -> {
            forceVanillaMenu = true;
            client.setScreen(new TitleScreen(false));
        }));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        
        context.fill(0, 0, width, height, 0xCC000000); // More darkened

        int centerX = width / 2;
        int centerY = height / 2;
        int logoY = centerY - 56;

        // Custom Title
        String title = "Not an old Pulse";
        int titleW = FontUtil.getWidth(client.textRenderer, title);
        int logoW = 10;
        int gap = 8;
        int startX = centerX - (logoW + gap + titleW) / 2;

        drawBluePulseLogo(context, startX, logoY - 2);
        FontUtil.drawTextWithShadow(context, client.textRenderer, title, startX + logoW + gap, logoY, 0xFFFFFFFF);

        // Player Name and Skin Top-Left
        String username = client.getSession().getUsername();
        int nameW = FontUtil.getWidth(client.textRenderer, username);
        RenderUtils.drawSmoothRoundedRect(context, 8, 8, 20 + nameW + 16, 28, 2, 0xAA111114);

        try {
            var tex = net.minecraft.client.util.DefaultSkinHelper.getSkinTextures(client.getSession().getUuidOrNull());
            net.minecraft.client.gui.PlayerSkinDrawer.draw(context, tex, 12, 12, 20);
        } catch (Exception e) {
            context.fill(12, 12, 32, 32, 0xFFAAAAAA);
        }
        
        FontUtil.drawTextWithShadow(context, client.textRenderer, username, 38, 18, 0xFFFFFFFF);

        FontUtil.drawTextWithShadow(context, client.textRenderer, "Not an old Pulse 1.21.11", 8, height - 16, 0xFFAAAAAA);

        for (CustomBtn btn : buttons) {
            btn.render(context, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.buttonInfo().button() == 0) {
            for (CustomBtn btn : buttons) {
                if (btn.isHovered((int) click.x(), (int) click.y())) {
                    btn.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private void drawBluePulseLogo(DrawContext context, int cx, int cy) {
        int color = 0xFF00A3FF;
        context.fill(cx + 2, cy, cx + 7, cy + 1, color);
        context.fill(cx + 1, cy + 1, cx + 2, cy + 2, color);
        context.fill(cx + 7, cy + 1, cx + 8, cy + 2, color);
        context.fill(cx, cy + 2, cx + 1, cy + 5, color);
        context.fill(cx + 8, cy + 2, cx + 9, cy + 5, color);
        context.fill(cx + 1, cy + 5, cx + 2, cy + 6, color);
        context.fill(cx + 7, cy + 5, cx + 8, cy + 6, color);
        context.fill(cx + 1, cy + 6, cx + 8, cy + 7, color);
        context.fill(cx + 2, cy + 7, cx + 3, cy + 8, color);
        context.fill(cx + 6, cy + 7, cx + 7, cy + 8, color);
        context.fill(cx + 3, cy + 8, cx + 4, cy + 9, color);
        context.fill(cx + 5, cy + 8, cx + 6, cy + 9, color);
        context.fill(cx + 4, cy + 9, cx + 5, cy + 10, color);
    }

    private class CustomBtn {
        int x, y, w, h;
        String text;
        Runnable action;
        float hoverAnim = 0f;
        boolean isRed;

        CustomBtn(int x, int y, int w, int h, String text, boolean isRed, Runnable action) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.text = text; this.isRed = isRed; this.action = action;
        }

        CustomBtn(int x, int y, int w, int h, String text, Runnable action) {
            this(x, y, w, h, text, false, action);
        }

        boolean isHovered(int mx, int my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }

        void render(DrawContext context, int mx, int my) {
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
            RenderUtils.drawSmoothRoundedRect(context, x, y, w, h, 4, color);

            int tw = FontUtil.getWidth(client.textRenderer, text);
            int textColor = hov ? 0xFFFFFFFF : 0xFFAAAAAA;
            FontUtil.drawTextWithShadow(context, client.textRenderer, text, x + (w - tw) / 2, y + (h - 9) / 2, textColor);
        }
    }
}




