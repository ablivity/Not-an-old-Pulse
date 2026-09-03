package ablivity.dev.not_an_old_pulse.mixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ablivity.dev.not_an_old_pulse.client.gui.screen.CustomMainMenuScreen;

@Mixin(LogoDrawer.class)
public class LogoDrawerMixin {
    @Inject(method = "draw(Lnet/minecraft/client/gui/DrawContext;IFI)V", at = @At("HEAD"), cancellable = true)
    private void onDraw(DrawContext context, int screenWidth, float alpha, int y, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof CustomMainMenuScreen) {
            ci.cancel();
        }
    }
}

