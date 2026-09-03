package ablivity.dev.not_an_old_pulse.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ablivity.dev.not_an_old_pulse.config.ModConfig;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onDrawBlockOutline(MatrixStack matrices, VertexConsumer vertexConsumer, double cameraX, double cameraY, double cameraZ, net.minecraft.client.render.state.OutlineRenderState state, int color, float opacity, CallbackInfo ci) {
        if (ModConfig.getInstance().get("blockOverlay", true)) {
            ci.cancel();

            VoxelShape shape = state.shape();
            if (shape.isEmpty()) return;
            BlockPos pos = state.pos();

            matrices.push();
            matrices.translate((double)pos.getX() - cameraX, (double)pos.getY() - cameraY, (double)pos.getZ() - cameraZ);

            String hex = ModConfig.getInstance().getString("blockOverlay_Color", "#4A90E2");
            int colorInt = 0xFF4A90E2;
            try {
                if (hex.startsWith("#")) colorInt = 0xFF000000 | Integer.parseInt(hex.substring(1), 16);
            } catch (Exception ignored) {}
            
            float r = ((colorInt >> 16) & 0xFF) / 255f;
            float g = ((colorInt >> 8) & 0xFF) / 255f;
            float b = (colorInt & 0xFF) / 255f;
            float a = 0.8f;

            // Optional: You could use a thicker line here via GL, though vanilla renders lines directly via VertexConsumer
            // In 1.21, lineWidth needs to be set via RenderPhase if using custom RenderLayer.
            // But for simple drawBlockOutline hijack, we just write vertices.
            
            shape.forEachEdge((x1, y1, z1, x2, y2, z2) -> {
                vertexConsumer.vertex(matrices.peek().getPositionMatrix(), (float)x1, (float)y1, (float)z1)
                        .color(r, g, b, a).normal(matrices.peek(), 0, 0, 0);
                vertexConsumer.vertex(matrices.peek().getPositionMatrix(), (float)x2, (float)y2, (float)z2)
                        .color(r, g, b, a).normal(matrices.peek(), 0, 0, 0);
            });

            matrices.pop();
        }
    }
}


