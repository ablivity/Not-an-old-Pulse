package ablivity.dev.not_an_old_pulse.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ablivity.dev.not_an_old_pulse.client.gui.screen.CustomPauseMenuScreen;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin {
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void onInit(CallbackInfo ci) {
        if (!((Object) this instanceof CustomPauseMenuScreen)) {
            GameMenuScreen screen = (GameMenuScreen) (Object) this;
            // Get the showMenu boolean from GameMenuScreen (we can just pass true as it's typically true for standard pause)
            MinecraftClient.getInstance().setScreen(new CustomPauseMenuScreen(true));
            ci.cancel();
        }
    }
}
