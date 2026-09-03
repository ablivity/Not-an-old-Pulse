package ablivity.dev.not_an_old_pulse.client.gui.animation;

/**
 * A single float that smoothly chases a target value over real time,
 * independent of frame rate. {@code speed} is a 0-1 "chase strength" at a
 * 60fps baseline; {@link #update(float)} expects {@code deltaTime} in
 * seconds, not ticks or frames — see {@code NotAnOldPulseScreen} for how
 * the screen's render delta is converted.
 */
public class AnimatedValue {
    private float current;
    private float target;
    private final float speed;

    public AnimatedValue(float initial, float speed) {
        this.current = initial;
        this.target = initial;
        this.speed = speed;
    }

    public void setTarget(float target) {
        this.target = target;
    }

    public void setImmediate(float value) {
        this.current = value;
        this.target = value;
    }

    public void update(float deltaTime) {
        if (Math.abs(current - target) < 0.001f) {
            current = target;
            return;
        }
        current = AnimationUtil.lerp(current, target, 1 - (float) Math.pow(1 - speed, deltaTime * 60));
    }

    public float get() {
        return current;
    }

    public float getTarget() {
        return target;
    }

    public boolean isAnimating() {
        return Math.abs(current - target) > 0.001f;
    }
}
