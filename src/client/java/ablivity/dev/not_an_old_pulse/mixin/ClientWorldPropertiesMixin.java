package ablivity.dev.not_an_old_pulse.mixin;

import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ablivity.dev.not_an_old_pulse.config.ModConfig;

@Mixin(ClientWorld.Properties.class)
public abstract class ClientWorldPropertiesMixin {

    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void overrideTimeOfDay(CallbackInfoReturnable<Long> cir) {
        if (ModConfig.getInstance().get("timeChanger")) {
            long customTime = ModConfig.getInstance().getLong("timeChanger_Time", ModConfig.getInstance().getLong("timeValue", 6000L));
            cir.setReturnValue(customTime);
        }
    }
}
