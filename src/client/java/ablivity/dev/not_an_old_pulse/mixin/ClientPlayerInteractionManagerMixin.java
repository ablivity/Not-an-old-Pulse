package ablivity.dev.not_an_old_pulse.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ablivity.dev.not_an_old_pulse.client.render.HitEffectManager;
import ablivity.dev.not_an_old_pulse.client.social.FriendManager;
import ablivity.dev.not_an_old_pulse.config.ModConfig;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void onAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (target != null && target.getName() != null) {
            String name = target.getName().getString();
            if (FriendManager.isFriend(name) && ModConfig.getInstance().get("friendProtection", false)) {
                ci.cancel();
                return;
            }
        }
        HitEffectManager.onAttack(target);
    }
}
