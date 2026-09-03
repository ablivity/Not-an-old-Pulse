package ablivity.dev.not_an_old_pulse.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.lwjgl.glfw.GLFW;
import ablivity.dev.not_an_old_pulse.client.util.RenderUtils;
import ablivity.dev.not_an_old_pulse.config.ModConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HudOverlayRenderer {

    private static final List<Long> leftClicks = new ArrayList<>();
    private static final List<Long> rightClicks = new ArrayList<>();

    // Dragging states for each widget
    private static int activeDragWidget = 0; // 0=none, 1=stats, 2=keystrokes, 3=playerInfo, 4=potions, 5=cooldowns
    private static int dragOffsetX = 0;
    private static int dragOffsetY = 0;
    private static boolean wasLmbHeld = false;

    public static void register() {
        HudRenderCallback.EVENT.register(HudOverlayRenderer::render);
    }

    public static void registerClick(int button) {
        long now = System.currentTimeMillis();
        if (button == 0) leftClicks.add(now);
        else if (button == 1) rightClicks.add(now);
    }

    private static int getCps(List<Long> clicks) {
        long now = System.currentTimeMillis();
        clicks.removeIf(t -> now - t > 1000L);
        return clicks.size();
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        // Hide ALL custom HUD overlays when Tab (player list) is open/held
        if (client.options.playerListKey.isPressed()) return;

        // 1. Dynamic Island
        DynamicIslandRenderer.render(context, tickCounter);

        // 2. Custom Crosshair
        CustomCrosshairRenderer.render(context);

        // 3. Waypoints 3D World / HUD Indicators
        WaypointRenderer.render(context);

        // 4. Target HUD (Player / Mob target card)
        TargetHudRenderer.render(context);

        // 5. Player Trail & Jump Circles tick
        PlayerTrailRenderer.tick(client);

        ModConfig config = ModConfig.getInstance();
        TextRenderer tr = client.textRenderer;
        boolean inChat = client.currentScreen instanceof ChatScreen;

        // Mouse Dragging in ChatScreen
        if (inChat) {
            long windowHandle = client.getWindow().getHandle();
            boolean lmbHeld = GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

            double mouseX = client.mouse.getX() * (double) client.getWindow().getScaledWidth() / (double) client.getWindow().getWidth();
            double mouseY = client.mouse.getY() * (double) client.getWindow().getScaledHeight() / (double) client.getWindow().getHeight();

            if (lmbHeld) {
                if (!wasLmbHeld) {
                    handleMouseClick(mouseX, mouseY);
                } else {
                    handleMouseDrag(mouseX, mouseY);
                }
            } else if (wasLmbHeld) {
                activeDragWidget = 0;
            }
            wasLmbHeld = lmbHeld;
        } else {
            activeDragWidget = 0;
            wasLmbHeld = false;
        }

        // 4. Cooldowns Card (Only show in-game when actual items are cooling down; in chat always show for positioning)
        if (inChat || config.get("cooldowns")) {
            renderCooldownsCard(context, client, tr, inChat);
        }

        // 5. Potions Card (Only show in-game when player has active effects; in chat always show for positioning)
        if (inChat || config.get("potions")) {
            renderPotionsCard(context, client, tr, inChat);
        }

        // 6. Stats Panel (FPS, CPS, Ping, Coords, Speed)
        renderStatsPanel(context, client, tr, config, inChat);

        // 7. Armor Status
        if (config.get("armorStatus")) {
            renderArmorStatus(context, client);
        }

        // Keystrokes removed by request

        // 9. Combo Counter
        if (inChat || config.get("comboCounter")) {
            renderComboCounter(context, client, tr, inChat);
        }

        // 10. Low Armor Warning
        renderArmorWarning(context, client, tr);
    }

    private static void renderPlayerInfoCard(DrawContext context, MinecraftClient client, TextRenderer tr, boolean inChat) {
        int x = ModConfig.getInstance().getInt("player_info_x", 8);
        int y = ModConfig.getInstance().getInt("player_info_y", 120);
        int w = 110;
        int h = 34;

        // Rounded Container
        RenderUtils.drawSmoothRoundedRect(context, x, y, w, h, 3, 0xEE111114);
        RenderUtils.drawSmoothRoundedRect(context, x + 1, y + 1, w - 2, h - 2, 2, 0xFB141418);

        // Avatar Head Box (Gold border)
        RenderUtils.drawSmoothRoundedRect(context, x + 5, y + 5, 22, 22, 3, 0xFFE2B23F);
        RenderUtils.drawSmoothRoundedRect(context, x + 6, y + 6, 20, 20, 2, 0xFF1E1E24);
        context.fill(x + 9, y + 9, x + 23, y + 23, 0xFFE2B23F);

        // Player Name
        String name = client.player != null ? client.player.getName().getString() : "Player";
        ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, name, x + 32, y + 5, 0xFFFFFFFF);

        // HP / Health Bar
        float health = client.player != null ? client.player.getHealth() : 20.0f;
        float maxHealth = client.player != null ? client.player.getMaxHealth() : 20.0f;
        String hpText = String.format("HP / %.1f", health);
        ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawText(context, tr, hpText, x + 32, y + 15, 0xFF8A8A96, false);

        // Purple Gradient Health Bar
        int barW = w - 38;
        int fillW = (int) (barW * Math.min(1.0f, health / Math.max(1.0f, maxHealth)));
        RenderUtils.drawSmoothRoundedRect(context, x + 32, y + 25, barW, 3, 1, 0xFF2A2A35);
        if (fillW > 0) {
            RenderUtils.drawSmoothRoundedRect(context, x + 32, y + 25, fillW, 3, 1, 0xFF8A5AE2);
        }

        if (inChat) drawOutline(context, x - 1, y - 1, w + 2, h + 2, 0xFF4A90E2);
    }

    private static void renderCooldownsCard(DrawContext context, MinecraftClient client, TextRenderer tr, boolean inChat) {
        ItemCooldownManager cd = client.player != null ? client.player.getItemCooldownManager() : null;
        boolean hasChorus = cd != null && cd.isCoolingDown(Items.CHORUS_FRUIT.getDefaultStack());
        boolean hasPearl = cd != null && cd.isCoolingDown(Items.ENDER_PEARL.getDefaultStack());
        boolean hasShield = cd != null && cd.isCoolingDown(Items.SHIELD.getDefaultStack());

        // In normal gameplay, only show if an item is actively cooling down!
        if (!inChat && !hasChorus && !hasPearl && !hasShield) {
            return;
        }

        int x = ModConfig.getInstance().getInt("cooldowns_x", 8);
        int y = ModConfig.getInstance().getInt("cooldowns_y", 160);
        int w = 110;
        int h = 36;

        RenderUtils.drawSmoothRoundedRect(context, x, y, w, h, 3, 0xEE111114);
        RenderUtils.drawSmoothRoundedRect(context, x + 1, y + 1, w - 2, h - 2, 2, 0xFB141418);

        // Header: ( ) Cooldowns
        ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, "\u23F1 Cooldowns", x + 8, y + 5, 0xFFB25AE2);

        // Row item
        String rowItem = hasPearl ? "Ender Pearl" : (hasChorus ? "Chorus Fruit" : (hasShield ? "Shield" : (inChat ? "Chorus Fruit" : "Ready")));
        String rowTime = (hasPearl || hasChorus || hasShield || inChat) ? "0:04" : "0:00";
        ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawText(context, tr, rowItem, x + 8, y + 19, 0xFFD0D0D8, false);
        int timeW = ablivity.dev.not_an_old_pulse.client.font.FontUtil.getWidth(tr, rowTime);
        ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawText(context, tr, rowTime, x + w - 8 - timeW, y + 19, 0xFF8A8A96, false);

        if (inChat) drawOutline(context, x - 1, y - 1, w + 2, h + 2, 0xFF4A90E2);
    }

    private static void renderPotionsCard(DrawContext context, MinecraftClient client, TextRenderer tr, boolean inChat) {
        Collection<StatusEffectInstance> effects = client.player != null ? client.player.getStatusEffects() : List.of();

        // In normal gameplay, only show if player has active status effects!
        if (!inChat && (effects == null || effects.isEmpty())) {
            return;
        }

        int screenW = client.getWindow().getScaledWidth();
        int x = ModConfig.getInstance().getInt("potions_x", screenW - 118);
        if (x > screenW - 10) x = screenW - 118;
        int y = ModConfig.getInstance().getInt("potions_y", 30);
        int w = 110;

        int rowCount = (effects != null && !effects.isEmpty()) ? effects.size() : (inChat ? 1 : 0);
        int h = 20 + Math.max(1, rowCount) * 14;

        RenderUtils.drawSmoothRoundedRect(context, x, y, w, h, 3, 0xEE111114);
        RenderUtils.drawSmoothRoundedRect(context, x + 1, y + 1, w - 2, h - 2, 2, 0xFB141418);

        // Header: Potions
        ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, "\uD83E\uDDEA Potions", x + 8, y + 5, 0xFF4A90E2);

        int effY = y + 18;
        if (effects == null || effects.isEmpty()) {
            if (inChat) {
                ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawText(context, tr, "Strength II", x + 8, effY, 0xFFE0E0E8, false);
                ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawText(context, tr, "1:30", x + w - 8 - ablivity.dev.not_an_old_pulse.client.font.FontUtil.getWidth(tr, "1:30"), effY, 0xFF8A8A96, false);
            }
        } else {
            for (StatusEffectInstance inst : effects) {
                String effName = inst.getEffectType().value().getName().getString();
                int durSec = inst.getDuration() / 20;
                String time = String.format("%d:%02d", durSec / 60, durSec % 60);

                ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawText(context, tr, effName, x + 8, effY, 0xFFE0E0E8, false);
                int tw = ablivity.dev.not_an_old_pulse.client.font.FontUtil.getWidth(tr, time);
                ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawText(context, tr, time, x + w - 8 - tw, effY, 0xFF8A8A96, false);
                effY += 14;
            }
        }

        if (inChat) drawOutline(context, x - 1, y - 1, w + 2, h + 2, 0xFF4A90E2);
    }

    private static void renderStatsPanel(DrawContext context, MinecraftClient client, TextRenderer tr, ModConfig config, boolean inChat) {
        boolean showFps = config.get("fpsDisplay");
        boolean showCps = config.get("cpsDisplay");
        boolean showPing = config.get("pingDisplay");
        boolean showCoords = config.get("coords");
        boolean showSpeed = config.get("speedometer");

        if (!inChat && !showFps && !showCps && !showPing && !showCoords && !showSpeed) return;

        int hudX = config.getInt("hud_x", 8);
        int hudY = config.getInt("hud_y", 8);

        List<String> lines = new ArrayList<>();
        if (showFps || inChat) lines.add(client.getCurrentFps() + " FPS");
        if (showCps) lines.add("CPS: " + getCps(leftClicks) + " | " + getCps(rightClicks));
        if (showPing && client.getNetworkHandler() != null) {
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            lines.add((entry != null ? entry.getLatency() : 0) + " ms");
        }
        if (showCoords && client.player != null) {
            PlayerEntity p = client.player;
            lines.add(String.format("XYZ: %.1f %.1f %.1f", p.getX(), p.getY(), p.getZ()));
        }
        if (showSpeed && client.player != null) {
            PlayerEntity p = client.player;
            double bps = p.getVelocity().horizontalLength() * 20.0;
            lines.add(String.format("%.2f BPS", bps));
        }

        if (lines.isEmpty() && inChat) {
            lines.add("144 FPS");
        }

        int maxWidth = 60;
        for (String l : lines) maxWidth = Math.max(maxWidth, ablivity.dev.not_an_old_pulse.client.font.FontUtil.getWidth(tr, l));
        int pad = 6;
        int panelW = maxWidth + pad * 2;
        int panelH = lines.size() * 13 + pad * 2 - 2;

        RenderUtils.drawSmoothRoundedRect(context, hudX, hudY, panelW, panelH, 3, 0xEE111114);
        RenderUtils.drawSmoothRoundedRect(context, hudX + 1, hudY + 1, panelW - 2, panelH - 2, 2, 0xFB141418);

        int textY = hudY + pad;
        for (String l : lines) {
            ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, l, hudX + pad, textY, 0xFFFFFFFF);
            textY += 13;
        }

        if (inChat) drawOutline(context, hudX - 1, hudY - 1, panelW + 2, panelH + 2, 0xFF4A90E2);
    }

    private static void renderArmorStatus(DrawContext context, MinecraftClient client) {
        if (client.player == null) return;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int startX = screenWidth / 2 + 96;
        int startY = screenHeight - 56;

        EquipmentSlot[] slots = new EquipmentSlot[] {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };

        for (EquipmentSlot slot : slots) {
            ItemStack stack = client.player.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                context.drawItem(stack, startX, startY);
                context.drawStackOverlay(client.textRenderer, stack, startX, startY);
                startY += 16;
            }
        }
    }

    private static void renderKeystrokes(DrawContext context, MinecraftClient client, TextRenderer tr, boolean inChat) {
        int screenWidth = client.getWindow().getScaledWidth();
        int defX = screenWidth - 76;
        int defY = 110;

        int x = ModConfig.getInstance().getInt("keystrokes_x", defX);
        if (x < 0) x = defX;
        int y = ModConfig.getInstance().getInt("keystrokes_y", defY);

        boolean w = client.options.forwardKey.isPressed();
        boolean a = client.options.leftKey.isPressed();
        boolean s = client.options.backKey.isPressed();
        boolean d = client.options.rightKey.isPressed();
        boolean lmb = client.options.attackKey.isPressed();
        boolean rmb = client.options.useKey.isPressed();

        drawKeyBox(context, tr, "W", x + 24, y, 22, 22, w);
        drawKeyBox(context, tr, "A", x, y + 24, 22, 22, a);
        drawKeyBox(context, tr, "S", x + 24, y + 24, 22, 22, s);
        drawKeyBox(context, tr, "D", x + 48, y + 24, 22, 22, d);
        drawKeyBox(context, tr, "LMB", x, y + 48, 34, 18, lmb);
        drawKeyBox(context, tr, "RMB", x + 36, y + 48, 34, 18, rmb);

        if (inChat) drawOutline(context, x - 2, y - 2, 74, 70, 0xFF4A90E2);
    }

    private static void drawKeyBox(DrawContext context, TextRenderer tr, String key, int x, int y, int w, int h, boolean pressed) {
        int borderColor = pressed ? 0xFF5C9DE8 : 0xEE111114;
        int bgColor = pressed ? 0xFF353540 : 0xFB141418;
        int fg = pressed ? 0xFF4A90E2 : 0xFFFFFFFF;

        RenderUtils.drawSmoothRoundedRect(context, x, y, w, h, 4, borderColor);
        RenderUtils.drawSmoothRoundedRect(context, x + 1, y + 1, w - 2, h - 2, 3, bgColor);
        int tw = ablivity.dev.not_an_old_pulse.client.font.FontUtil.getWidth(tr, key);
        ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawText(context, tr, key, x + (w - tw) / 2, y + (h - 8) / 2, fg, false);
    }

    private static void drawOutline(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static void renderComboCounter(DrawContext context, MinecraftClient client, TextRenderer tr, boolean inChat) {
        int combo = ablivity.dev.not_an_old_pulse.client.util.ComboManager.combo;
        if (combo == 0 && !inChat) return;

        int displayCombo = combo == 0 ? 12 : combo;
        String text = displayCombo + " Combo";
        
        float scale = ablivity.dev.not_an_old_pulse.client.util.ComboManager.animScale;
        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        
        int x = ModConfig.getInstance().getInt("combo_x", screenW / 2 + 100);
        int y = ModConfig.getInstance().getInt("combo_y", screenH / 2 - 20);

        // Color shifts based on combo
        int color = 0xFFFFFFFF;
        if (displayCombo > 10) color = 0xFFFF5555; // Red
        else if (displayCombo > 5) color = 0xFFFFAA00; // Gold
        else if (displayCombo > 2) color = 0xFF55FF55; // Green
        
        ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, text, x, y, color);
        
        if (inChat) drawOutline(context, x - 2, y - 2, 60, 14, 0xFF4A90E2);
    }

    private static void renderArmorWarning(DrawContext context, MinecraftClient client, TextRenderer tr) {
        if (client.player == null) return;
        boolean lowArmor = false;
        
        net.minecraft.entity.EquipmentSlot[] slots = new net.minecraft.entity.EquipmentSlot[] {
            net.minecraft.entity.EquipmentSlot.HEAD, net.minecraft.entity.EquipmentSlot.CHEST, 
            net.minecraft.entity.EquipmentSlot.LEGS, net.minecraft.entity.EquipmentSlot.FEET
        };
        for (net.minecraft.entity.EquipmentSlot slot : slots) {
            ItemStack stack = client.player.getEquippedStack(slot);
            if (stack.isDamageable() && stack.getDamage() > 0) {
                float durability = 1.0f - ((float) stack.getDamage() / stack.getMaxDamage());
                if (durability < 0.15f) {
                    lowArmor = true;
                    break;
                }
            }
        }
        
        if (lowArmor) {
            int screenW = client.getWindow().getScaledWidth();
            int screenH = client.getWindow().getScaledHeight();
            
            // Pulse alpha
            float pulse = (float) (Math.sin(System.currentTimeMillis() / 150.0) * 0.5 + 0.5);
            int alpha = (int) (100 + 155 * pulse);
            int color = (alpha << 24) | 0xFF5555;
            
            String text = "Ã¢Å¡Â  LOW ARMOR Ã¢Å¡Â ";
            int tw = ablivity.dev.not_an_old_pulse.client.font.FontUtil.getWidth(tr, text);
            ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, text, (screenW - tw) / 2, screenH / 2 + 30, color);
        }
    }

    private static void handleMouseClick(double mx, double my) {
        ModConfig cfg = ModConfig.getInstance();
        MinecraftClient client = MinecraftClient.getInstance();
        int screenW = client.getWindow().getScaledWidth();

        // 1. Stats
        int sx = cfg.getInt("hud_x", 8), sy = cfg.getInt("hud_y", 8);
        if (mx >= sx && mx <= sx + 100 && my >= sy && my <= sy + 80) {
            activeDragWidget = 1; dragOffsetX = (int) mx - sx; dragOffsetY = (int) my - sy; return;
        }
        // 4. Potions
        int potX = cfg.getInt("potions_x", screenW - 118), potY = cfg.getInt("potions_y", 30);
        if (mx >= potX && mx <= potX + 110 && my >= potY && my <= potY + 60) {
            activeDragWidget = 4; dragOffsetX = (int) mx - potX; dragOffsetY = (int) my - potY; return;
        }
        // 5. Cooldowns
        int cdx = cfg.getInt("cooldowns_x", 8), cdy = cfg.getInt("cooldowns_y", 160);
        if (mx >= cdx && mx <= cdx + 110 && my >= cdy && my <= cdy + 36) {
            activeDragWidget = 5; dragOffsetX = (int) mx - cdx; dragOffsetY = (int) my - cdy; return;
        }
        // 6. Target HUD
        int thx = cfg.getInt("target_hud_x", screenW / 2 + 10), thy = cfg.getInt("target_hud_y", client.getWindow().getScaledHeight() / 2 + 18);
        if (mx >= thx && mx <= thx + 150 && my >= thy && my <= thy + 42) {
            activeDragWidget = 6; dragOffsetX = (int) mx - thx; dragOffsetY = (int) my - thy; return;
        }
        // 7. Combo Counter
        int cmx = cfg.getInt("combo_x", screenW / 2 + 100), cmy = cfg.getInt("combo_y", client.getWindow().getScaledHeight() / 2 - 20);
        if (mx >= cmx && mx <= cmx + 60 && my >= cmy && my <= cmy + 14) {
            activeDragWidget = 7; dragOffsetX = (int) mx - cmx; dragOffsetY = (int) my - cmy; return;
        }

        DynamicIslandRenderer.mouseClicked(mx, my, 0);
    }

    private static void handleMouseDrag(double mx, double my) {
        ModConfig cfg = ModConfig.getInstance();
        int nx = Math.max(0, (int) mx - dragOffsetX);
        int ny = Math.max(0, (int) my - dragOffsetY);

        if (activeDragWidget == 1) { cfg.set("hud_x", nx); cfg.set("hud_y", ny); }
        else if (activeDragWidget == 4) { cfg.set("potions_x", nx); cfg.set("potions_y", ny); }
        else if (activeDragWidget == 5) { cfg.set("cooldowns_x", nx); cfg.set("cooldowns_y", ny); }
        else if (activeDragWidget == 6) { cfg.set("target_hud_x", nx); cfg.set("target_hud_y", ny); }
        else if (activeDragWidget == 7) { cfg.set("combo_x", nx); cfg.set("combo_y", ny); }
    }

}
