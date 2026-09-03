package ablivity.dev.not_an_old_pulse.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import ablivity.dev.not_an_old_pulse.config.ModConfig;

public class CustomCrosshairRenderer {

    private static final int TARGET_COLOR = 0xFFFF454F;
    private static final int OUTLINE_COLOR = 0xD9000000;

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options.hudHidden) return;
        if (!ModConfig.getInstance().get("crosshair", true)) return;

        int centerX = context.getScaledWindowWidth() / 2;
        int centerY = context.getScaledWindowHeight() / 2;
        
        int size = Math.max(2, Math.min(10, ModConfig.getInstance().getInt("crosshair_size", 4)));
        int thickness = size >= 8 ? 2 : 1;
        int color = crosshairColor(client);

        String style = ModConfig.getInstance().getString("crosshair_Style", "Cross").toLowerCase();
        switch (style) {
            case "dot" -> drawDot(context, centerX, centerY, size, color);
            case "brackets" -> drawBrackets(context, centerX, centerY, size, thickness, color);
            case "diamond" -> drawDiamond(context, centerX, centerY, size, color);
            default -> drawCross(context, centerX, centerY, size, thickness, color);
        }
    }

    private static int crosshairColor(MinecraftClient client) {
        if (ModConfig.getInstance().get("crosshair_target_red", true)
                && client.targetedEntity instanceof PlayerEntity target
                && target != client.player
                && target.isAlive()
                && !target.isSpectator()) {
            return TARGET_COLOR;
        }

        String colorHex = ModConfig.getInstance().getString("crosshair_color", ModConfig.getInstance().getString("crosshair_Color", "#FFFFFF"));
        try {
            if (colorHex.startsWith("#")) colorHex = colorHex.substring(1);
            return 0xFF000000 | Integer.parseInt(colorHex, 16);
        } catch (Exception e) {
            return 0xFFF7F7FA;
        }
    }

    private static void drawCross(DrawContext context, int x, int y, int size, int thickness, int color) {
        int length = size + 2;
        int gap = size / 2 + 1;

        rect(context, x - gap - length, y, length, thickness, color);
        rect(context, x + gap + thickness, y, length, thickness, color);
        rect(context, x, y - gap - length, thickness, length, color);
        rect(context, x, y + gap + thickness, thickness, length, color);
    }

    private static void drawDot(DrawContext context, int x, int y, int size, int color) {
        int diameter = Math.max(2, (size + 1) / 2);
        rect(context, x - diameter / 2, y - diameter / 2, diameter, diameter, color);
    }

    private static void drawBrackets(DrawContext context, int x, int y, int size, int thickness, int color) {
        int halfHeight = size + 2;
        int side = size + 3;
        int cap = Math.max(3, size / 2 + 1);

        rect(context, x - side, y - halfHeight, thickness, halfHeight * 2 + 1, color);
        rect(context, x - side, y - halfHeight, cap, thickness, color);
        rect(context, x - side, y + halfHeight, cap, thickness, color);
        rect(context, x + side, y - halfHeight, thickness, halfHeight * 2 + 1, color);
        rect(context, x + side - cap + thickness, y - halfHeight, cap, thickness, color);
        rect(context, x + side - cap + thickness, y + halfHeight, cap, thickness, color);
    }

    private static void drawDiamond(DrawContext context, int x, int y, int size, int color) {
        int radius = Math.max(3, size);
        for (int offset = 0; offset <= radius; offset++) {
            int horizontal = radius - offset;
            rect(context, x - horizontal, y - offset, 1, 1, color);
            rect(context, x + horizontal, y - offset, 1, 1, color);
            rect(context, x - horizontal, y + offset, 1, 1, color);
            rect(context, x + horizontal, y + offset, 1, 1, color);
        }
    }

    private static void rect(DrawContext context, int x, int y, int width, int height, int color) {
        if (ModConfig.getInstance().get("crosshair_outline", true)) {
            context.fill(x - 1, y - 1, x + width + 1, y + height + 1, OUTLINE_COLOR);
        }
        context.fill(x, y, x + width, y + height, color);
    }
}

