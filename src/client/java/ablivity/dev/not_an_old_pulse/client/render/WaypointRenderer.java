package ablivity.dev.not_an_old_pulse.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;
import ablivity.dev.not_an_old_pulse.client.util.RenderUtils;
import ablivity.dev.not_an_old_pulse.client.waypoint.WaypointManager;

public class WaypointRenderer {

    public static void register() {
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) return;

        TextRenderer tr = client.textRenderer;
        Vec3d cameraPos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
        if (client.gameRenderer != null && client.gameRenderer.getCamera() != null) {
            cameraPos = client.gameRenderer.getCamera().getCameraPos();
        }

        int activeIndex = 0;
        int screenW = client.getWindow().getScaledWidth();

        for (WaypointManager.Waypoint wp : WaypointManager.getWaypoints()) {
            if (!wp.visible) continue;

            double dx = wp.x - cameraPos.x;
            double dy = wp.y - cameraPos.y;
            double dz = wp.z - cameraPos.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            String label = String.format("%s - %.0fm", wp.name, dist);
            int textW = ablivity.dev.not_an_old_pulse.client.font.FontUtil.getWidth(tr, label);
            int boxW = textW + 20;
            int boxH = 14;

            int boxX = 8;
            int boxY = 86 + activeIndex * 18;

            int colorInt = 0xFF4A90E2;
            try {
                if (wp.color != null && wp.color.startsWith("#")) {
                    colorInt = 0xFF000000 | Integer.parseInt(wp.color.substring(1), 16);
                }
            } catch (Exception ignored) {}

            RenderUtils.drawSmoothRoundedRect(context, boxX, boxY, boxW, boxH, 4, 0xEE111114);
            RenderUtils.drawSmoothRoundedRect(context, boxX + 1, boxY + 1, boxW - 2, boxH - 2, 3, 0xFA17171C);

            RenderUtils.drawSmoothRoundedRect(context, boxX + 4, boxY + 4, 6, 6, 3, colorInt);
            ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, label, boxX + 14, boxY + 3, 0xFFFFFFFF);

            activeIndex++;
        }
    }
}


