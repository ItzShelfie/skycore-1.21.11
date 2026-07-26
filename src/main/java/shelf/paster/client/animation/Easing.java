package shelf.paster.client.animation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/** Easing curves for {@link Anim}. */
public enum Easing {
    LINEAR(t -> t),
    EXPO_OUT(t -> t == 1f ? 1f : 1f - (float) Math.pow(2, -10 * t)),
    EXPO_IN(t -> t == 0f ? 0f : (float) Math.pow(2, 10 * (t - 1f))),
    CUBIC_OUT(t -> 1f - (float) Math.pow(1f - t, 3)),
    BACK_OUT(t -> {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        return 1f + c3 * (float) Math.pow(t - 1f, 3) + c1 * (float) Math.pow(t - 1f, 2);
    }),
    /** Soft overshoot then settle — good for GUI open. */
    SPRING_OUT(t -> {
        float c = (float) (1.0 - Math.cos(t * Math.PI * 0.5));
        float over = (float) Math.sin(t * Math.PI) * (1f - t) * 0.12f;
        return Math.min(1.08f, c + over);
    }),
    QUART_IN(t -> t * t * t * t),
    SMOOTH(t -> t * t * (3f - 2f * t));

    private final UnaryOperator<Float> fn;

    Easing(UnaryOperator<Float> fn) {
        this.fn = fn;
    }

    public float apply(float t) {
        return fn.apply(Math.max(0f, Math.min(1f, t)));
    }
}
