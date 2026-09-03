package ablivity.dev.not_an_old_pulse.client.render;

/**
 * DISABLED for the 1.21.11 port.
 *
 * This used to hook net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents,
 * but that whole event suite was removed from Fabric API starting with
 * 1.21.9 — Fabric's own release notes say a replacement isn't ready yet and
 * point mod authors at writing a mixin into WorldRenderer instead:
 * https://fabricmc.net/2025/09/23/1219.html
 *
 * Re-implementing this needs a real WorldRenderer mixin (render-after-entities
 * hook), not a drop-in API swap, so it's parked here rather than guessed at.
 * The old body (camera-relative cone render via a VertexConsumer) is kept
 * below, commented out, as a reference for whoever picks this back up —
 * note it will still need Camera.getPos() -> Camera.getCameraPos() and a
 * replacement for RenderLayer.getEntitySolid(Identifier), which also
 * changed in this version range.
 */
public class ChinaHatRenderer {
    public static void register() {
        // no-op until rewritten as a WorldRenderer mixin
    }

    /*
    private static void drawCone(Matrix4f matrix, VertexConsumer consumer, int light, int color) {
        int segments = 8;
        float radius = 0.3f;
        float height = 0.5f;

        float r = (color >> 16 & 0xFF) / 255.0f;
        float g = (color >> 8 & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = (color >> 24 & 0xFF) / 255.0f;

        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (i * 2 * Math.PI / segments);
            float angle2 = (float) ((i + 1) * 2 * Math.PI / segments);

            float x1 = (float) Math.cos(angle1) * radius;
            float z1 = (float) Math.sin(angle1) * radius;
            float x2 = (float) Math.cos(angle2) * radius;
            float z2 = (float) Math.sin(angle2) * radius;

            consumer.vertex(matrix, x1, 0, z1).color(r, g, b, a).texture(0, 0).light(light);
            consumer.vertex(matrix, 0, height, 0).color(r, g, b, a).texture(0.5f, 1).light(light);
            consumer.vertex(matrix, x2, 0, z2).color(r, g, b, a).texture(1, 0).light(light);
        }

        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (i * 2 * Math.PI / segments);
            float angle2 = (float) ((i + 1) * 2 * Math.PI / segments);

            float x1 = (float) Math.cos(angle1) * radius;
            float z1 = (float) Math.sin(angle1) * radius;
            float x2 = (float) Math.cos(angle2) * radius;
            float z2 = (float) Math.sin(angle2) * radius;

            consumer.vertex(matrix, 0, 0, 0).color(r, g, b, a).texture(0.5f, 0.5f).light(light);
            consumer.vertex(matrix, x2, 0, z2).color(r, g, b, a).texture(0, 1).light(light);
            consumer.vertex(matrix, x1, 0, z1).color(r, g, b, a).texture(1, 1).light(light);
        }
    }
    */
}

