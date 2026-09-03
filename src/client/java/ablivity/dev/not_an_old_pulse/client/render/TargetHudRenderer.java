package ablivity.dev.not_an_old_pulse.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import ablivity.dev.not_an_old_pulse.client.util.RenderUtils;
import ablivity.dev.not_an_old_pulse.config.ModConfig;

public final class TargetHudRenderer {

    private static LivingEntity currentTarget = null;
    private static long lastTargetTime = 0L;
    private static float animAlpha = 0.0f;
    private static float displayedHp = 20.0f;
    private static float trailingHp = 20.0f;

    public static void setTarget(LivingEntity entity) {
        if (entity != null && entity.isAlive()) {
            currentTarget = entity;
            lastTargetTime = System.currentTimeMillis();
        }
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (!ModConfig.getInstance().get("targetHud", true)) return;

        boolean inChat = client.currentScreen instanceof ChatScreen;
        long now = System.currentTimeMillis();

        // Target decay after 4 seconds of no combat
        if (currentTarget != null && (!currentTarget.isAlive() || now - lastTargetTime > 4000L)) {
            if (!inChat) {
                currentTarget = null;
            }
        }

        // Target target or dummy target in chat
        boolean shouldShow = (currentTarget != null) || inChat;
        float targetAnim = shouldShow ? 1.0f : 0.0f;
        animAlpha += (targetAnim - animAlpha) * 0.18f;
        if (animAlpha <= 0.02f) return;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        int defaultX = screenW / 2 + 10;
        int defaultY = screenH / 2 + 18;
        int x = ModConfig.getInstance().getInt("target_hud_x", defaultX);
        int y = ModConfig.getInstance().getInt("target_hud_y", defaultY);

        int w = 150;
        int h = 42;

        String name = currentTarget != null ? currentTarget.getName().getString() : "Preview Target";
        float health = currentTarget != null ? currentTarget.getHealth() : 18.5f;
        float maxHealth = currentTarget != null ? currentTarget.getMaxHealth() : 20.0f;
        double dist = currentTarget != null ? Math.sqrt(client.player.squaredDistanceTo(currentTarget)) : 3.4;

        displayedHp += (health - displayedHp) * 0.35f;
        if (trailingHp > health) {
            trailingHp += (health - trailingHp) * 0.03f;
        } else {
            trailingHp = health;
        }

        TextRenderer tr = client.textRenderer;

        // Container Background (Dark glass)
        RenderUtils.drawSmoothRoundedRect(context, x, y, w, h, 3, 0xEE111114);
        RenderUtils.drawSmoothRoundedRect(context, x + 1, y + 1, w - 2, h - 2, 2, 0xFA141418);

        // Target Avatar Head Box (Left)
        RenderUtils.drawSmoothRoundedRect(context, x + 5, y + 5, 26, 26, 3, 0xFFE0473F);
        RenderUtils.drawSmoothRoundedRect(context, x + 6, y + 6, 24, 24, 2, 0xFF1E1E24);
        context.fill(x + 9, y + 9, x + 27, y + 27, 0xFFE0473F);

        // Name & Distance
        String distStr = String.format("%.1fm", dist);
        int distW = ablivity.dev.not_an_old_pulse.client.font.FontUtil.getWidth(tr, distStr);
        int maxNameW = w - 40 - distW - 6;
        String trimmedName = ablivity.dev.not_an_old_pulse.client.font.FontUtil.trimToWidth(tr, name, Math.max(20, maxNameW));
        ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, trimmedName, x + 36, y + 6, 0xFFFFFFFF);
        ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawText(context, tr, distStr, x + w - 8 - distW, y + 6, 0xFF8A8A96, false);

        // HP Text
        String hpStr = String.format("%.1f HP", health);
        ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawText(context, tr, hpStr, x + 36, y + 18, 0xFFD0D0D8, false);

        // Health Bar (Purple gradient with White trail)
        int barX = x + 36;
        int barY = y + 29;
        int barW = w - 44;
        int fillW = (int) (barW * Math.min(1.0f, Math.max(0.0f, displayedHp / Math.max(1.0f, maxHealth))));
        int trailW = (int) (barW * Math.min(1.0f, Math.max(0.0f, trailingHp / Math.max(1.0f, maxHealth))));

        RenderUtils.drawSmoothRoundedRect(context, barX, barY, barW, 4, 2, 0xFF2A2A35);
        if (trailW > 0) {
            RenderUtils.drawSmoothRoundedRect(context, barX, barY, trailW, 4, 2, 0xFFFFFFFF);
        }
        if (fillW > 0) {
            RenderUtils.drawSmoothRoundedRect(context, barX, barY, fillW, 4, 2, 0xFF8A5AE2);
        }

        if (inChat) {
            context.fill(x - 1, y - 1, x + w + 1, y, 0xFF4A90E2);
            context.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xFF4A90E2);
            context.fill(x - 1, y, x, y + h, 0xFF4A90E2);
            context.fill(x + w, y, x + w + 1, y + h, 0xFF4A90E2);
        }
    }
}

