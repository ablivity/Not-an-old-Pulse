package ablivity.dev.not_an_old_pulse.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import ablivity.dev.not_an_old_pulse.config.ModConfig;

import java.util.concurrent.ThreadLocalRandom;

public final class PlayerTrailRenderer {

    private static boolean wasOnGround = true;
    private static int trailTick = 0;

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        ClientPlayerEntity player = client.player;

        // 1. Jump Circles
        if (ModConfig.getInstance().get("jumpCircles")) {
            boolean onGround = player.isOnGround();
            if (onGround && !wasOnGround && player.fallDistance > 0.4f) {
                spawnJumpCircle(client, player);
            }
            wasOnGround = onGround;
        }

        // 2. Player Movement Trail
        if (ModConfig.getInstance().get("playerTrail", true)) {
            Vec3d vel = player.getVelocity();
            double speedSq = vel.x * vel.x + vel.z * vel.z;

            if (speedSq > 0.003 && player.isAlive() && !player.isSpectator()) {
                trailTick++;
                int delay = player.isSprinting() || player.isGliding() ? 1 : 2;
                if (trailTick >= delay) {
                    trailTick = 0;
                    spawnTrailParticle(client, player, vel);
                }
            }
        }
    }

    private static void spawnJumpCircle(MinecraftClient client, ClientPlayerEntity player) {
        int points = 24;
        float radius = ModConfig.getInstance().getFloat("jumpCircles_Radius", ModConfig.getInstance().getFloat("jumpCircleRadius", 2.0f));
        double px = player.getX();
        double py = player.getY() + 0.05;
        double pz = player.getZ();

        for (int i = 0; i < points; i++) {
            double angle = (i * 2.0 * Math.PI) / points;
            double vx = Math.cos(angle) * 0.15;
            double vz = Math.sin(angle) * 0.15;
            double sx = px + Math.cos(angle) * (radius * 0.5);
            double sz = pz + Math.sin(angle) * (radius * 0.5);

            client.world.addParticleClient(ParticleTypes.SOUL_FIRE_FLAME, sx, py, sz, vx, 0.01, vz);
        }
    }

    private static void spawnTrailParticle(MinecraftClient client, ClientPlayerEntity player, Vec3d vel) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double px = player.getX() + (rnd.nextDouble() - 0.5) * 0.3;
        double py = player.getY() + 0.1 + rnd.nextDouble() * 0.4;
        double pz = player.getZ() + (rnd.nextDouble() - 0.5) * 0.3;

        double vx = -vel.x * 0.2 + (rnd.nextDouble() - 0.5) * 0.02;
        double vy = 0.02 + rnd.nextDouble() * 0.02;
        double vz = -vel.z * 0.2 + (rnd.nextDouble() - 0.5) * 0.02;

        client.world.addParticleClient(ParticleTypes.GLOW, px, py, pz, vx, vy, vz);
    }
}
