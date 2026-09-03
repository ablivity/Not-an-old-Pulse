package ablivity.dev.not_an_old_pulse.client.render;

import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import ablivity.dev.not_an_old_pulse.config.ModConfig;

public class ChinaHatFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    public ChinaHatFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        if (!ModConfig.getInstance().get("chinaHat")) {
            return;
        }

        float scale = ModConfig.getInstance().getFloat("chinaHat_Scale", ModConfig.getInstance().getFloat("chinaHatScale", 100.0f)) / 100.0f;
        String colorHex = ModConfig.getInstance().getString("chinaHat_color", ModConfig.getInstance().getString("chinaHat_Color", "#8A5AE2"));
        int color = 0xFF8A5AE2;
        try {
            if (colorHex.startsWith("#")) colorHex = colorHex.substring(1);
            color = 0xFF000000 | Integer.parseInt(colorHex, 16);
        } catch (Exception ignored) {}

        matrices.push();
        this.getContextModel().getHead().applyTransform(matrices);
        matrices.translate(0.0f, -0.45f, 0.0f);
        matrices.scale(scale, scale, scale);

        final int finalColor = color;
        queue.submitCustom(matrices, RenderLayers.lines(), (entry, consumer) -> {
            drawCone(entry.getPositionMatrix(), consumer, finalColor);
        });

        matrices.pop();
    }

    private static void drawCone(Matrix4f matrix, VertexConsumer consumer, int color) {
        int segments = 24;
        float radius = 0.55f;
        float height = 0.25f;

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = 220;

        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (i * 2 * Math.PI / segments);
            float angle2 = (float) ((i + 1) * 2 * Math.PI / segments);

            float x1 = (float) Math.cos(angle1) * radius;
            float z1 = (float) Math.sin(angle1) * radius;
            float x2 = (float) Math.cos(angle2) * radius;
            float z2 = (float) Math.sin(angle2) * radius;

            consumer.vertex(matrix, x1, 0, z1).color(r, g, b, a).normal(0, 1, 0).lineWidth(2.0f);
            consumer.vertex(matrix, x2, 0, z2).color(r, g, b, a).normal(0, 1, 0).lineWidth(2.0f);

            consumer.vertex(matrix, x1, 0, z1).color(r, g, b, a).normal(0, 1, 0).lineWidth(2.0f);
            consumer.vertex(matrix, 0, -height, 0).color(r, g, b, a).normal(0, 1, 0).lineWidth(2.0f);
        }
    }
}
