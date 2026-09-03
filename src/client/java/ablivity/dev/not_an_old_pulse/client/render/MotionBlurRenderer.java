package ablivity.dev.not_an_old_pulse.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.util.Pool;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ablivity.dev.not_an_old_pulse.config.ModConfig;
import ablivity.dev.not_an_old_pulse.mixin.MotionBlurPostEffectPassAccessor;
import ablivity.dev.not_an_old_pulse.mixin.MotionBlurPostEffectProcessorAccessor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;

public final class MotionBlurRenderer {

    public static final MotionBlurRenderer INSTANCE = new MotionBlurRenderer();

    private static final Identifier PROCESSOR_ID =
            Identifier.of("not_an_old_pulse", "motion_blur");
    private static final String UNIFORM_BLOCK = "MotionBlurUniforms";

    private static final int UNIFORM_BUFFER_SIZE = 288;
    private static final long STALE_FRAME_NANOS = 250_000_000L;
    private static final double TELEPORT_RESET_DISTANCE_SQUARED = 256.0;

    private final Pool framebufferPool = new Pool(2);
    private final ArrayDeque<GpuBuffer> ownedUniformBuffers = new ArrayDeque<>();

    private PostEffectProcessor processor;
    private PostEffectPass motionPass;
    private GpuBuffer dynamicUniformBuffer;

    private final Matrix4f previousProjection = new Matrix4f();
    private final Matrix4f previousModelView = new Matrix4f();
    private final Matrix4f currentProjectionInverse = new Matrix4f();
    private final Matrix4f currentModelViewInverse = new Matrix4f();
    private final ByteBuffer uniformUploadBuffer = ByteBuffer
            .allocateDirect(UNIFORM_BUFFER_SIZE)
            .order(ByteOrder.nativeOrder());
    private Vec3d previousCameraPos;
    private Object previousWorld;
    private long previousFrameNanos;
    private boolean previousStateReady;

    private boolean loadErrorPrinted;

    private MotionBlurRenderer() {
    }

    public void render(
            MinecraftClient client,
            Camera camera,
            Matrix4f modelView,
            Matrix4f projection
    ) {
        if (!shouldRender(client)) {
            resetPreviousState();
            return;
        }

        Framebuffer main = client.getFramebuffer();
        if (!valid(main)) {
            resetPreviousState();
            return;
        }

        float strength = configuredStrength();
        Vec3d cameraPos = camera.getCameraPos();
        long now = System.nanoTime();

        boolean staleFrame = previousFrameNanos != 0L && now - previousFrameNanos > STALE_FRAME_NANOS;
        boolean worldChanged = previousWorld != client.world;
        boolean teleported = previousCameraPos != null
                && cameraPos.squaredDistanceTo(previousCameraPos) > TELEPORT_RESET_DISTANCE_SQUARED;

        if (!previousStateReady || staleFrame || worldChanged || teleported || strength <= 0.0001f) {
            rememberCurrentState(client, cameraPos, modelView, projection, now);
            return;
        }

        PostEffectProcessor loadedProcessor = client.getShaderLoader()
                .loadPostEffect(PROCESSOR_ID, DefaultFramebufferSet.MAIN_ONLY);

        if (loadedProcessor == null) {
            printLoadErrorOnce();
            resetProcessor();
            rememberCurrentState(client, cameraPos, modelView, projection, now);
            return;
        }

        if (processor != loadedProcessor || motionPass == null || dynamicUniformBuffer == null || dynamicUniformBuffer.isClosed()) {
            if (!bindProcessor(loadedProcessor)) {
                printLoadErrorOnce();
                resetProcessor();
                rememberCurrentState(client, cameraPos, modelView, projection, now);
                return;
            }
        }

        float frameTimeSeconds = Math.max(1.0f / 240.0f, Math.min(1.0f / 20.0f, (now - previousFrameNanos) / 1_000_000_000.0f));

        currentProjectionInverse.set(projection).invert();
        currentModelViewInverse.set(modelView).invert();

        double cameraDeltaX = cameraPos.x - previousCameraPos.x;
        double cameraDeltaY = cameraPos.y - previousCameraPos.y;
        double cameraDeltaZ = cameraPos.z - previousCameraPos.z;

        if (!uploadUniforms(
                currentProjectionInverse,
                previousProjection,
                currentModelViewInverse,
                previousModelView,
                cameraDeltaX,
                cameraDeltaY,
                cameraDeltaZ,
                frameTimeSeconds,
                strength
        )) {
            printLoadErrorOnce();
            resetProcessor();
            rememberCurrentState(client, cameraPos, modelView, projection, now);
            return;
        }

        try {
            processor.render(main, framebufferPool);
            framebufferPool.decrementLifespan();
            loadErrorPrinted = false;
        } catch (RuntimeException exception) {
            if (!loadErrorPrinted) {
                System.err.println("[not_an_old_pulse] Motion Blur render failed: " + exception.getMessage());
                loadErrorPrinted = true;
            }
            resetProcessor();
        }

        rememberCurrentState(client, cameraPos, modelView, projection, now);
    }

    public void close() {
        framebufferPool.close();

        while (!ownedUniformBuffers.isEmpty()) {
            GpuBuffer buffer = ownedUniformBuffers.removeFirst();
            if (buffer != null && !buffer.isClosed()) {
                buffer.close();
            }
        }

        processor = null;
        motionPass = null;
        dynamicUniformBuffer = null;
        resetPreviousState();
        loadErrorPrinted = false;
    }

    private boolean bindProcessor(PostEffectProcessor loadedProcessor) {
        PostEffectPass pass = findMotionPass(loadedProcessor);
        if (pass == null) return false;

        Map<String, GpuBuffer> uniformBuffers =
                ((MotionBlurPostEffectPassAccessor) pass).not_an_old_pulse$getUniformBuffers();
        if (uniformBuffers == null) return false;

        GpuBuffer original = uniformBuffers.get(UNIFORM_BLOCK);
        if (original == null) return false;

        GpuBuffer replacement;
        try {
            replacement = RenderSystem.getDevice().createBuffer(
                    () -> "not_an_old_pulse MotionBlurUniforms",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    Math.max(UNIFORM_BUFFER_SIZE, original.size())
            );
        } catch (RuntimeException exception) {
            return false;
        }

        GpuBuffer replaced = uniformBuffers.put(UNIFORM_BLOCK, replacement);
        if (replaced != null && replaced != replacement && !ownedUniformBuffers.contains(replaced) && !replaced.isClosed()) {
            replaced.close();
        }

        ownedUniformBuffers.addLast(replacement);
        processor = loadedProcessor;
        motionPass = pass;
        dynamicUniformBuffer = replacement;

        return true;
    }

    private PostEffectPass findMotionPass(PostEffectProcessor effect) {
        List<PostEffectPass> passes =
                ((MotionBlurPostEffectProcessorAccessor) effect).not_an_old_pulse$getPasses();
        if (passes == null || passes.isEmpty()) return null;
        return passes.getFirst();
    }

    private boolean uploadUniforms(
            Matrix4f currentProjectionInverse,
            Matrix4f previousProjectionMatrix,
            Matrix4f currentModelViewInverse,
            Matrix4f previousModelViewMatrix,
            double cameraDeltaX,
            double cameraDeltaY,
            double cameraDeltaZ,
            float frameTimeSeconds,
            float strength
    ) {
        if (dynamicUniformBuffer == null || dynamicUniformBuffer.isClosed()) return false;

        try {
            uniformUploadBuffer.clear();
            Std140Builder builder = Std140Builder.intoBuffer(uniformUploadBuffer);
            builder.putMat4f(currentProjectionInverse);
            builder.putMat4f(previousProjectionMatrix);
            builder.putMat4f(currentModelViewInverse);
            builder.putMat4f(previousModelViewMatrix);
            builder.putVec4((float) cameraDeltaX, (float) cameraDeltaY, (float) cameraDeltaZ, frameTimeSeconds);
            builder.putVec4(strength, 3.0f, 0.003f, 0.0f);

            RenderSystem.getDevice()
                    .createCommandEncoder()
                    .writeToBuffer(dynamicUniformBuffer.slice(), builder.get());
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void rememberCurrentState(
            MinecraftClient client,
            Vec3d cameraPos,
            Matrix4f modelView,
            Matrix4f projection,
            long frameNanos
    ) {
        previousProjection.set(projection);
        previousModelView.set(modelView);
        previousCameraPos = cameraPos;
        previousWorld = client.world;
        previousFrameNanos = frameNanos;
        previousStateReady = true;
    }

    private float configuredStrength() {
        return ModConfig.getInstance().getFloat("motionBlur_Strength", 0.5f);
    }

    private boolean shouldRender(MinecraftClient client) {
        return ModConfig.getInstance().get("motionBlur", false)
                && client.player != null
                && client.world != null
                && client.currentScreen == null
                && !client.isPaused();
    }

    private boolean valid(Framebuffer framebuffer) {
        return framebuffer != null
                && framebuffer.textureWidth > 0
                && framebuffer.textureHeight > 0
                && framebuffer.getColorAttachment() != null
                && framebuffer.getColorAttachmentView() != null
                && framebuffer.getDepthAttachment() != null
                && framebuffer.getDepthAttachmentView() != null;
    }

    private void resetPreviousState() {
        previousCameraPos = null;
        previousWorld = null;
        previousFrameNanos = 0L;
        previousStateReady = false;
    }

    private void resetProcessor() {
        processor = null;
        motionPass = null;
        dynamicUniformBuffer = null;
    }

    private void printLoadErrorOnce() {
        if (loadErrorPrinted) return;
        System.err.println("[not_an_old_pulse] Motion blur post effect could not be loaded.");
        loadErrorPrinted = true;
    }
}
