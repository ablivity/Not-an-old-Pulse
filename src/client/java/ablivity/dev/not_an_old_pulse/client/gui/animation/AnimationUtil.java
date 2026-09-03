package ablivity.dev.not_an_old_pulse.client.gui.animation;

public class AnimationUtil {
    public static float lerp(float start, float end, float delta) {
        return start + (end - start) * delta;
    }

    public static double lerp(double start, double end, double delta) {
        return start + (end - start) * delta;
    }

    public static int lerpColor(int start, int end, float delta) {
        int startA = (start >> 24) & 0xFF;
        int startR = (start >> 16) & 0xFF;
        int startG = (start >> 8) & 0xFF;
        int startB = start & 0xFF;

        int endA = (end >> 24) & 0xFF;
        int endR = (end >> 16) & 0xFF;
        int endG = (end >> 8) & 0xFF;
        int endB = end & 0xFF;

        int resultA = (int) lerp(startA, endA, delta);
        int resultR = (int) lerp(startR, endR, delta);
        int resultG = (int) lerp(startG, endG, delta);
        int resultB = (int) lerp(startB, endB, delta);

        return (resultA << 24) | (resultR << 16) | (resultG << 8) | resultB;
    }

    public static float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }

    public static float easeInOutCubic(float t) {
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }

    public static float easeOutQuart(float t) {
        return 1 - (float) Math.pow(1 - t, 4);
    }

    public static float easeOutExpo(float t) {
        return t == 1f ? 1f : 1 - (float) Math.pow(2, -10 * t);
    }
}
