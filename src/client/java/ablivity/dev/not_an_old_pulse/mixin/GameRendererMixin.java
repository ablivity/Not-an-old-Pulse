package ablivity.dev.not_an_old_pulse.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ablivity.dev.not_an_old_pulse.config.ModConfig;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void disableHurtCam(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (ModConfig.getInstance().get("noHurtCam")) {
            ci.cancel();
        }
    }

    @Redirect(method = "getBasicProjectionMatrix", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;getFramebufferWidth()I"))
    private int onGetFramebufferWidth(Window window) {
        if (ModConfig.getInstance().get("aspectRatio")) {
            String ratio = ModConfig.getInstance().getString("aspectRatio_Ratio", ModConfig.getInstance().getString("aspectRatioValue", "4:3"));
            float factor = 1.3333334f;
            if ("4:3".equalsIgnoreCase(ratio)) factor = 1.3333334f;
            else if ("16:9".equalsIgnoreCase(ratio)) factor = 1.0f;
            else if ("21:9".equalsIgnoreCase(ratio)) factor = 0.75f;
            else if ("5:4".equalsIgnoreCase(ratio)) factor = 1.25f;
            else if ("1:1".equalsIgnoreCase(ratio)) factor = 1.7777778f;
            return (int) (window.getFramebufferWidth() / factor);
        }
        return window.getFramebufferWidth();
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void applyCustomFov(net.minecraft.client.render.Camera camera, float tickProgress, boolean changingFov, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
        if (ModConfig.getInstance().get("customFov")) {
            float fov = ModConfig.getInstance().getFloat("customFov_FOV", ModConfig.getInstance().getFloat("fovValue", 90.0f));
            try {
                net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
                if (mc.options != null && mc.options.getFov().getValue() != (int) fov) {
                    mc.options.getFov().setValue((int) fov);
                }
            } catch (Exception ignored) {}
            cir.setReturnValue(fov);
        }
    }
}
