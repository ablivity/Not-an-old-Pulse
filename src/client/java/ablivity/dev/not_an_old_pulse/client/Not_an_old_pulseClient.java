package ablivity.dev.not_an_old_pulse.client;

import net.dimaskama.mcef.api.MCEFApi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import ablivity.dev.not_an_old_pulse.client.gui.browser.BrowserScreen;
import ablivity.dev.not_an_old_pulse.client.keybind.ModKeyBinds;
import ablivity.dev.not_an_old_pulse.client.render.ChinaHatRenderer;
import ablivity.dev.not_an_old_pulse.client.render.FullBrightRenderer;
import ablivity.dev.not_an_old_pulse.client.render.HudOverlayRenderer;
import ablivity.dev.not_an_old_pulse.config.ModConfig;
import ablivity.dev.not_an_old_pulse.client.rpc.SimpleDiscordIPC;
import org.lwjgl.glfw.GLFW;

public class Not_an_old_pulseClient implements ClientModInitializer {

    public static final String INTER_FONT = "not_an_old_pulse:inter";

    @Override
    public void onInitializeClient() {
        ModKeyBinds.register();
        ChinaHatRenderer.register();
        FullBrightRenderer.register();
        HudOverlayRenderer.register();
        ablivity.dev.not_an_old_pulse.client.render.WaypointRenderer.register();
        ablivity.dev.not_an_old_pulse.client.util.ComboManager.register();
        ablivity.dev.not_an_old_pulse.client.media.MediaBridge.INSTANCE.start();

        SimpleDiscordIPC.start();
        final boolean[] wasGPressed = new boolean[]{false};

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ablivity.dev.not_an_old_pulse.client.util.ComboManager.tick();
            
            if (client.getWindow() != null) {
                boolean isGPressed = GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_G) == GLFW.GLFW_PRESS;
                if (isGPressed && !wasGPressed[0]) {
                    boolean fullBright = ModConfig.getInstance().get("fullBright");
                    ModConfig.getInstance().set("fullBright", !fullBright);
                    if (client.player != null) {
                        client.player.sendMessage(net.minecraft.text.Text.literal("§a[Not an old Pulse] §7Fullbright: " + (!fullBright ? "§aВКЛ" : "§cВЫКЛ")), false);
                    }
                }
                wasGPressed[0] = isGPressed;
            }
            
            if (client.player != null) {
                if (ModConfig.getInstance().get("fullBright")) {
                    client.player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.NIGHT_VISION, 400, 0, false, false, false));
                } else if (client.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.NIGHT_VISION)) {
                    net.minecraft.entity.effect.StatusEffectInstance effect = client.player.getStatusEffect(net.minecraft.entity.effect.StatusEffects.NIGHT_VISION);
                    if (effect != null && !effect.shouldShowIcon()) {
                        client.player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.NIGHT_VISION);
                    }
                }
                
                // Clear vision mechanic from sn0w.visual
                if (client.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.NAUSEA)) client.player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.NAUSEA);
                if (client.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS)) client.player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS);
                if (client.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.DARKNESS)) client.player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.DARKNESS);
            }
            
            if (client.player != null && client.player.age % 100 == 0) {
                SimpleDiscordIPC.update("Играет в Not an old Pulse", client.getCurrentServerEntry() != null ? "Сетевая игра" : "Одиночная игра");
            }
            
            if (client.player != null) {
                // Auto Sprint
                if (ModConfig.getInstance().get("autoSprint")) {
                    if (client.player.input.hasForwardMovement() && !client.player.isSneaking() && !client.player.isSubmergedInWater() && client.player.getHungerManager().getFoodLevel() > 6) {
                        client.player.setSprinting(true);
                    }
                }
            }
        });

        // Asynchronously initialize MCEF and preload Web GUI so it is instant
        try {
            MCEFApi.Initialization init = MCEFApi.initialize();
            if (init.isDone()) {
                BrowserScreen.preload(MCEFApi.getInstance());
            } else {
                init.getFuture().thenAccept(BrowserScreen::preload);
            }
        } catch (Throwable t) {
            System.err.println("[not_an_old_pulse] Failed to start background MCEF initialization: " + t.getMessage());
        }
    }
}

