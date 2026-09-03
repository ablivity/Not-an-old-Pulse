package ablivity.dev.not_an_old_pulse.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ablivity.dev.not_an_old_pulse.config.ModConfig;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {

    private boolean wasOnGround = true;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        boolean onGround = player.isOnGround();
        
        if (onGround && !wasOnGround) {
            if (ModConfig.getInstance().get("jumpCircles", true)) {
                float radius = ModConfig.getInstance().getFloat("jumpCircles_Radius", ModConfig.getInstance().getFloat("jumpCirclesValue", 2.0f));
                int points = (int) (radius * 12);
                for (int i = 0; i < points; i++) {
                    double angle = 2 * Math.PI * i / points;
                    double x = player.getX() + Math.cos(angle) * radius;
                    double z = player.getZ() + Math.sin(angle) * radius;
                    MinecraftClient.getInstance().world.addParticleClient(ParticleTypes.FLAME, x, player.getY(), z, 0, 0, 0);
                }
            }
        }
        wasOnGround = onGround;
    }
}

