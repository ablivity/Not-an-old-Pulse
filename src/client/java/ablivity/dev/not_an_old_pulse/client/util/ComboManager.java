package ablivity.dev.not_an_old_pulse.client.util;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.util.ActionResult;

public class ComboManager {
    public static int combo = 0;
    private static long lastHitTime = 0;
    public static float animScale = 1.0f;

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) {
                combo++;
                lastHitTime = System.currentTimeMillis();
                animScale = 1.5f; // Pop animation
            }
            return ActionResult.PASS;
        });
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        if (combo > 0 && now - lastHitTime > 3000) {
            combo = 0;
        }
        if (animScale > 1.0f) {
            animScale += (1.0f - animScale) * 0.2f;
            if (animScale < 1.01f) animScale = 1.0f;
        }
    }
}


