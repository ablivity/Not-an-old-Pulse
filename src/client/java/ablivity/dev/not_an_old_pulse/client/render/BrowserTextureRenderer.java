package ablivity.dev.not_an_old_pulse.client.render;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;

public final class BrowserTextureRenderer {

    private static int program = 0;
    private static int vao = 0;
    private static int vbo = 0;
    private static int samplerLocation = -1;
    private static boolean failed = false;

    private static final FloatBuffer VERTICES = BufferUtils.createFloatBuffer(16);

    private BrowserTextureRenderer() {
    }

    public static void draw(
            int textureId,
            int screenWidth,
            int screenHeight,
            int x,
            int y,
            int width,
            int height
    ) {
        if (textureId <= 0 || screenWidth <= 0 || screenHeight <= 0 || width <= 0 || height <= 0 || failed) {
            return;
        }

        try {
            ensureInitialized();

            float left = x / (float) screenWidth * 2.0f - 1.0f;
            float right = (x + width) / (float) screenWidth * 2.0f - 1.0f;
            float top = 1.0f - y / (float) screenHeight * 2.0f;
            float bottom = 1.0f - (y + height) / (float) screenHeight * 2.0f;

            VERTICES.clear();
            VERTICES.put(left).put(bottom).put(0.0f).put(1.0f);
            VERTICES.put(right).put(bottom).put(1.0f).put(1.0f);
            VERTICES.put(left).put(top).put(0.0f).put(0.0f);
            VERTICES.put(right).put(top).put(1.0f).put(0.0f);
            VERTICES.flip();

            int previousProgram = GL33.glGetInteger(GL33.GL_CURRENT_PROGRAM);
            int previousVao = GL33.glGetInteger(GL33.GL_VERTEX_ARRAY_BINDING);
            int previousArrayBuffer = GL33.glGetInteger(GL33.GL_ARRAY_BUFFER_BINDING);
            int previousActiveTexture = GL33.glGetInteger(GL33.GL_ACTIVE_TEXTURE);

            GL33.glActiveTexture(GL33.GL_TEXTURE0);
            int previousTexture = GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);

            boolean depthEnabled = GL33.glIsEnabled(GL33.GL_DEPTH_TEST);
            boolean blendEnabled = GL33.glIsEnabled(GL33.GL_BLEND);
            boolean scissorEnabled = GL33.glIsEnabled(GL33.GL_SCISSOR_TEST);

            GL33.glDisable(GL33.GL_DEPTH_TEST);
            GL33.glDisable(GL33.GL_SCISSOR_TEST);
            GL33.glEnable(GL33.GL_BLEND);
            GL33.glBlendFunc(GL33.GL_SRC_ALPHA, GL33.GL_ONE_MINUS_SRC_ALPHA);

            GL33.glUseProgram(program);
            GL33.glUniform1i(samplerLocation, 0);

            GL33.glBindVertexArray(vao);
            GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vbo);
            GL33.glBufferSubData(GL33.GL_ARRAY_BUFFER, 0, VERTICES);

            GL33.glBindTexture(GL33.GL_TEXTURE_2D, textureId);
            GL33.glDrawArrays(GL33.GL_TRIANGLE_STRIP, 0, 4);

            GL33.glBindTexture(GL33.GL_TEXTURE_2D, previousTexture);
            GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, previousArrayBuffer);
            GL33.glBindVertexArray(previousVao);
            GL33.glUseProgram(previousProgram);
            GL33.glActiveTexture(previousActiveTexture);

            if (depthEnabled) {
                GL33.glEnable(GL33.GL_DEPTH_TEST);
            } else {
                GL33.glDisable(GL33.GL_DEPTH_TEST);
            }

            if (blendEnabled) {
                GL33.glEnable(GL33.GL_BLEND);
            } else {
                GL33.glDisable(GL33.GL_BLEND);
            }

            if (scissorEnabled) {
                GL33.glEnable(GL33.GL_SCISSOR_TEST);
            } else {
                GL33.glDisable(GL33.GL_SCISSOR_TEST);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            failed = true;
        }
    }

    private static void ensureInitialized() {
        if (program != 0) {
            return;
        }

        int vertex = compileShader(
                GL33.GL_VERTEX_SHADER,
                """
                #version 150
                in vec2 Position;
                in vec2 UV;
                out vec2 BrowserUV;

                void main() {
                    BrowserUV = UV;
                    gl_Position = vec4(Position, 0.0, 1.0);
                }
                """
        );

        int fragment = compileShader(
                GL33.GL_FRAGMENT_SHADER,
                """
                #version 150
                uniform sampler2D BrowserTexture;
                in vec2 BrowserUV;
                out vec4 FragmentColor;

                void main() {
                    FragmentColor = texture(BrowserTexture, BrowserUV);
                }
                """
        );

        program = GL33.glCreateProgram();
        GL33.glAttachShader(program, vertex);
        GL33.glAttachShader(program, fragment);

        GL33.glBindAttribLocation(program, 0, "Position");
        GL33.glBindAttribLocation(program, 1, "UV");

        GL33.glLinkProgram(program);

        if (GL33.glGetProgrami(program, GL33.GL_LINK_STATUS) == GL33.GL_FALSE) {
            throw new IllegalStateException(GL33.glGetProgramInfoLog(program));
        }

        GL33.glDetachShader(program, vertex);
        GL33.glDetachShader(program, fragment);
        GL33.glDeleteShader(vertex);
        GL33.glDeleteShader(fragment);

        samplerLocation = GL33.glGetUniformLocation(program, "BrowserTexture");

        vao = GL33.glGenVertexArrays();
        vbo = GL33.glGenBuffers();

        GL33.glBindVertexArray(vao);
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vbo);
        GL33.glBufferData(GL33.GL_ARRAY_BUFFER, 16L * Float.BYTES, GL33.GL_DYNAMIC_DRAW);

        GL33.glEnableVertexAttribArray(0);
        GL33.glVertexAttribPointer(0, 2, GL33.GL_FLOAT, false, 4 * Float.BYTES, 0L);

        GL33.glEnableVertexAttribArray(1);
        GL33.glVertexAttribPointer(1, 2, GL33.GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);

        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, 0);
        GL33.glBindVertexArray(0);
    }

    private static int compileShader(int type, String source) {
        int shader = GL33.glCreateShader(type);
        GL33.glShaderSource(shader, source);
        GL33.glCompileShader(shader);

        if (GL33.glGetShaderi(shader, GL33.GL_COMPILE_STATUS) == GL33.GL_FALSE) {
            throw new IllegalStateException(GL33.glGetShaderInfoLog(shader));
        }

        return shader;
    }
}
