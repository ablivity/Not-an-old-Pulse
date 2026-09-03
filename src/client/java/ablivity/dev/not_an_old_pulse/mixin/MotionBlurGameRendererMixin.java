package ablivity.dev.not_an_old_pulse.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ablivity.dev.not_an_old_pulse.client.render.MotionBlurRenderer;

@Mixin(GameRenderer.class)
public abstract class MotionBlurGameRendererMixin {

    @Redirect(
            method = "renderWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V"
            )
    )
    private void renderWorldThenMotionBlur(
            WorldRenderer worldRenderer,
            ObjectAllocator allocator,
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            Matrix4f modelView,
            Matrix4f projection,
            Matrix4f cullingProjection,
            GpuBufferSlice fogBuffer,
            Vector4f fogColor,
            boolean renderSky
    ) {
        worldRenderer.render(
                allocator,
                tickCounter,
                renderBlockOutline,
                camera,
                modelView,
                projection,
                cullingProjection,
                fogBuffer,
                fogColor,
                renderSky
        );

        MotionBlurRenderer.INSTANCE.render(
                MinecraftClient.getInstance(),
                camera,
                modelView,
                projection
        );
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void closeMotionBlur(CallbackInfo ci) {
        MotionBlurRenderer.INSTANCE.close();
    }
}
