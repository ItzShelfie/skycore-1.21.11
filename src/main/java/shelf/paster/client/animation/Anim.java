package shelf.paster.client.animation;

/** Single animated float. Prefer creating via {@link Animations#of(String)}. */
public final class Anim {
    private float value;
    private float from;
    private float target;
    private float duration;
    private float elapsed;
    private Easing easing = Easing.EXPO_OUT;
    private boolean running;

    public Anim(float durationSeconds) {
        this.duration = Math.max(0.01f, durationSeconds);
    }

    public Anim ease(Easing easing) {
        this.easing = easing == null ? Easing.LINEAR : easing;
        return this;
    }

    public Anim duration(float seconds) {
        this.duration = Math.max(0.01f, seconds);
        return this;
    }

    /** One-shot retarget from current value. */
    public Anim to(float target) {
        if (Math.abs(this.target - target) < 0.0001f && Math.abs(this.value - target) < 0.0001f && !running) {
            this.value = target;
            return this;
        }
        this.from = this.value;
        this.target = target;
        this.elapsed = 0f;
        this.running = true;
        return this;
    }

    /** Retarget with duration/easing in one call. */
    public Anim to(float target, float seconds, Easing easing) {
        duration(seconds);
        ease(easing);
        return to(target);
    }

    public Anim snap(float value) {
        this.value = value;
        this.from = value;
        this.target = value;
        this.elapsed = 0f;
        this.running = false;
        return this;
    }

    public float get() {
        return value;
    }

    public float target() {
        return target;
    }

    public boolean done() {
        return !running;
    }

    public void update(float dt) {
        if (!running) {
            return;
        }
        elapsed += Math.max(0f, dt);
        float t = Math.min(1f, elapsed / duration);
        float e = easing.apply(t);
        value = from + (target - from) * e;
        if (t >= 1f) {
            value = target;
            running = false;
        }
    }
}
