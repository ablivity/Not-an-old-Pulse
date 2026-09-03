package ablivity.dev.not_an_old_pulse.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import ablivity.dev.not_an_old_pulse.config.ModConfig;

import java.util.concurrent.ThreadLocalRandom;

public final class HitEffectManager {

    public static void onAttack(Entity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        if (!(target instanceof LivingEntity living)) return;

        // 1. Update Target HUD
        TargetHudRenderer.setTarget(living);

        // 2. Custom Hit Sound
        if (ModConfig.getInstance().get("hitSounds", true)) {
            String soundStyle = ModConfig.getInstance().getString("hitSounds_Style", ModConfig.getInstance().getString("hitSoundStyle", "Bell"));
            float pitch = 1.0f + (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.2f;

            if ("Bell".equalsIgnoreCase(soundStyle)) {
                client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), pitch, 1.0f));
            } else if ("Ding".equalsIgnoreCase(soundStyle) || "Osu".equalsIgnoreCase(soundStyle)) {
                client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, pitch * 1.3f, 0.9f));
            } else if ("Pop".equalsIgnoreCase(soundStyle)) {
                client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_ITEM_PICKUP, pitch, 1.0f));
            } else {
                client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_ARROW_HIT_PLAYER, pitch, 0.8f));
            }
        }

        // 3. Custom Hit Particles
        if (ModConfig.getInstance().get("particles")) {
            int density = ModConfig.getInstance().getInt("particles_Density", 2);
            int count = density * 4;

            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            for (int i = 0; i < count; i++) {
                double px = living.getX() + (rnd.nextDouble() - 0.5) * living.getWidth();
                double py = living.getY() + rnd.nextDouble() * living.getHeight();
                double pz = living.getZ() + (rnd.nextDouble() - 0.5) * living.getWidth();
                double vx = (rnd.nextDouble() - 0.5) * 0.3;
                double vy = rnd.nextDouble() * 0.2 + 0.1;
                double vz = (rnd.nextDouble() - 0.5) * 0.3;

                client.world.addParticleClient(ParticleTypes.CRIT, px, py, pz, vx, vy, vz);
                client.world.addParticleClient(ParticleTypes.ENCHANTED_HIT, px, py, pz, vx, vy, vz);
            }
        }
    }
}
