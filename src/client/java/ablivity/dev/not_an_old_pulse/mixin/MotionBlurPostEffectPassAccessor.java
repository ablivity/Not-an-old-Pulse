package ablivity.dev.not_an_old_pulse.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import net.minecraft.client.gl.PostEffectPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(PostEffectPass.class)
public interface MotionBlurPostEffectPassAccessor {

    @Accessor("samplers")
    List<PostEffectPass.Sampler> not_an_old_pulse$getSamplers();

    @Accessor("uniformBuffers")
    Map<String, GpuBuffer> not_an_old_pulse$getUniformBuffers();
}
