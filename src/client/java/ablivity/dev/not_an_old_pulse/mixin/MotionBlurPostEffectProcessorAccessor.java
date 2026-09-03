package ablivity.dev.not_an_old_pulse.mixin;

import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(PostEffectProcessor.class)
public interface MotionBlurPostEffectProcessorAccessor {

    @Accessor("passes")
    List<PostEffectPass> not_an_old_pulse$getPasses();
}
