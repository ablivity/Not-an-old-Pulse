package ablivity.dev.not_an_old_pulse.client.font;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class FontUtil {

    public static final Identifier INTER_FONT = Identifier.of("not_an_old_pulse", "inter");
    public static final StyleSpriteSource INTER_SPRITE_SOURCE = new StyleSpriteSource.Font(INTER_FONT);
    public static final Style INTER_STYLE = Style.EMPTY.withFont(INTER_SPRITE_SOURCE);

    private FontUtil() {
    }

    public static MutableText text(String text) {
        if (text == null) {
            return Text.empty();
        }
        return Text.literal(text).setStyle(INTER_STYLE);
    }

    public static MutableText styleText(Text text) {
        if (text == null) {
            return Text.empty();
        }
        return text.copy().setStyle(text.getStyle().withFont(INTER_SPRITE_SOURCE));
    }

    public static void drawText(DrawContext context, TextRenderer tr, String text, int x, int y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) return;
        context.drawText(tr, text(text), x, y, color, shadow);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0.3f, 0.0f);
        context.drawText(tr, text(text), x, y, color, shadow);
        context.getMatrices().popMatrix();
    }

    public static void drawText(DrawContext context, TextRenderer tr, Text text, int x, int y, int color, boolean shadow) {
        if (text == null) return;
        context.drawText(tr, styleText(text), x, y, color, shadow);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0.3f, 0.0f);
        context.drawText(tr, styleText(text), x, y, color, shadow);
        context.getMatrices().popMatrix();
    }

    public static void drawTextWithShadow(DrawContext context, TextRenderer tr, String text, int x, int y, int color) {
        if (text == null || text.isEmpty()) return;
        context.drawTextWithShadow(tr, text(text), x, y, color);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0.3f, 0.0f);
        context.drawTextWithShadow(tr, text(text), x, y, color);
        context.getMatrices().popMatrix();
    }

    public static void drawTextWithShadow(DrawContext context, TextRenderer tr, Text text, int x, int y, int color) {
        if (text == null) return;
        context.drawTextWithShadow(tr, styleText(text), x, y, color);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0.3f, 0.0f);
        context.drawTextWithShadow(tr, styleText(text), x, y, color);
        context.getMatrices().popMatrix();
    }

    public static void drawCenteredTextWithShadow(DrawContext context, TextRenderer tr, String text, int centerX, int y, int color) {
        if (text == null || text.isEmpty()) return;
        context.drawCenteredTextWithShadow(tr, text(text), centerX, y, color);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0.3f, 0.0f);
        context.drawCenteredTextWithShadow(tr, text(text), centerX, y, color);
        context.getMatrices().popMatrix();
    }

    public static void drawCenteredTextWithShadow(DrawContext context, TextRenderer tr, Text text, int centerX, int y, int color) {
        if (text == null) return;
        context.drawCenteredTextWithShadow(tr, styleText(text), centerX, y, color);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0.3f, 0.0f);
        context.drawCenteredTextWithShadow(tr, styleText(text), centerX, y, color);
        context.getMatrices().popMatrix();
    }

    public static int getWidth(TextRenderer tr, String text) {
        if (text == null || text.isEmpty()) return 0;
        return tr.getWidth(text(text));
    }

    public static int getWidth(TextRenderer tr, Text text) {
        if (text == null) return 0;
        return tr.getWidth(styleText(text));
    }

    public static String trimToWidth(TextRenderer tr, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        // Use Minecraft's native trim to handle surrogates, formatting, and custom fonts correctly
        return tr.getTextHandler().trimToWidth(text, maxWidth, INTER_STYLE);
    }
}



