package ablivity.dev.not_an_old_pulse.client.gui.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;
import ablivity.dev.not_an_old_pulse.client.font.FontUtil;
import ablivity.dev.not_an_old_pulse.client.util.RenderUtils;

import java.util.ArrayList;
import java.util.List;

public class CustomPauseMenuScreen extends GameMenuScreen {

    private final List<CustomBtn> buttons = new ArrayList<>();

    public CustomPauseMenuScreen(boolean showMenu) {
        super(showMenu);
    }

    @Override
    protected void init() {
        super.init(); // Let GameMenuScreen initialize
        this.clearChildren(); // Then we clear its widgets
        buttons.clear();

        int centerX = width / 2;
        int centerY = height / 2;

        int btnW = 200;
        int btnH = 24;

        // Ãâ€™ÃÂµÃ‘â‚¬ÃÂ½Ã‘Æ’Ã‘â€šÃ‘Å’Ã‘ÂÃ‘Â ÃÂº ÃÂ¸ÃÂ³Ã‘â‚¬ÃÂµ
        buttons.add(new CustomBtn(centerX - btnW / 2, centerY - 28, btnW, btnH, "Вернуться в игру", () -> {
            client.setScreen(null);
            client.mouse.lockCursor();
        }));

        // ÃÂÃÂ°Ã‘ÂÃ‘â€šÃ‘â‚¬ÃÂ¾ÃÂ¹ÃÂºÃÂ¸
        buttons.add(new CustomBtn(centerX - btnW / 2, centerY, btnW, btnH, "Настройки", () -> {
            client.setScreen(new OptionsScreen(this, client.options));
        }));

        // ÃÅ¾Ã‘â€šÃÂºÃÂ»Ã‘Å½Ã‘â€¡ÃÂ¸Ã‘â€šÃ‘Å’Ã‘ÂÃ‘Â
        buttons.add(new CustomBtn(centerX - btnW / 2, centerY + 28, btnW, btnH, "Отключиться", true, () -> {
            boolean bl = this.client.isInSingleplayer();
            boolean bl2 = this.client.isConnectedToLocalServer();
            this.client.world.disconnect(Text.translatable("menu.savingLevel"));
            this.client.disconnect(Text.translatable("menu.savingLevel"));
            
            TitleScreen titleScreen = new TitleScreen();
            if (bl) {
                this.client.setScreen(titleScreen);
            } else if (bl2) {
                this.client.setScreen(new MultiplayerScreen(titleScreen));
            } else {
                this.client.setScreen(new MultiplayerScreen(titleScreen));
            }
        }));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xAA000000);

        int centerX = width / 2;
        int centerY = height / 2;
        int logoY = centerY - 56;

        String title = "Not an old Pulse";
        int titleW = FontUtil.getWidth(client.textRenderer, title);
        int logoW = 10;
        int gap = 8;
        int startX = centerX - (logoW + gap + titleW) / 2;

        drawBluePulseLogo(context, startX, logoY - 2);
        FontUtil.drawTextWithShadow(context, client.textRenderer, title, startX + logoW + gap, logoY, 0xFFFFFFFF);

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
}



