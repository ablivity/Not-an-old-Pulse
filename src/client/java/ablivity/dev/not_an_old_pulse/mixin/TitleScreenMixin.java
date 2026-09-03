package ablivity.dev.not_an_old_pulse.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ablivity.dev.not_an_old_pulse.client.gui.screen.CustomMainMenuScreen;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void onInit(CallbackInfo ci) {
        if (!CustomMainMenuScreen.forceVanillaMenu && !((Object) this instanceof CustomMainMenuScreen)) {
            MinecraftClient.getInstance().setScreen(new CustomMainMenuScreen());
            ci.cancel();
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInitTail(CallbackInfo ci) {
        if (CustomMainMenuScreen.forceVanillaMenu && !((Object) this instanceof CustomMainMenuScreen)) {
            TitleScreen screen = (TitleScreen) (Object) this;
            ButtonWidget btn = ButtonWidget.builder(Text.literal("Меню Pulse"), button -> {
                CustomMainMenuScreen.forceVanillaMenu = false;
                MinecraftClient.getInstance().setScreen(new CustomMainMenuScreen());
            }).dimensions(4, 4, 100, 20).build();
            
            Screens.getButtons(screen).add(btn);
        }
    }
}


