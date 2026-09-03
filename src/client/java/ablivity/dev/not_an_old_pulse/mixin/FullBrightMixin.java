package ablivity.dev.not_an_old_pulse.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ablivity.dev.not_an_old_pulse.config.ModConfig;

@Mixin(GameRenderer.class)
public class FullBrightMixin {

    @Inject(method = "getNightVisionStrength", at = @At("HEAD"), cancellable = true)
    private static void onGetNightVisionStrength(LivingEntity entity, float tickDelta, CallbackInfoReturnable<Float> cir) {
        if (ModConfig.getInstance().get("fullBright")) {
            int brightness = ModConfig.getInstance().getInt("fullBright_Brightness", ModConfig.getInstance().getInt("brightnessValue", 100));
            cir.setReturnValue(Math.max(0.0f, Math.min(1.0f, brightness / 100.0f)));
        }
    }
}
