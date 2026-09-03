package ablivity.dev.not_an_old_pulse.mixin;

import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ablivity.dev.not_an_old_pulse.config.ModConfig;

@Mixin(SimpleOption.class)
public class SimpleOptionMixin<T> {
    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    private void onGetValue(CallbackInfoReturnable<T> cir) {
        if (ModConfig.getInstance().get("fullBright") && this.toString().contains("gamma")) {
            cir.setReturnValue((T) (Double) 100.0);
        }
    }
}
