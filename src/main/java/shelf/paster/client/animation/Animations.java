package shelf.paster.client.animation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global animation registry — one line to use:
 * {@code float v = Animations.of("gui.open").to(1f).get();}
 */
public final class Animations {
    private static final Map<String, Anim> ANIMS = new ConcurrentHashMap<>();

    private Animations() {
    }

    /** Get or create named anim (default 0.32s expo-out). */
    public static Anim of(String id) {
        return ANIMS.computeIfAbsent(id, ignored -> new Anim(0.32f).ease(Easing.EXPO_OUT));
    }

    /** Get or create with duration + easing. */
    public static Anim of(String id, float duration, Easing easing) {
        return ANIMS.computeIfAbsent(id, ignored -> new Anim(duration).ease(easing));
    }

    /** Convenience: set target and read current in one expression after tick. */
    public static float get(String id) {
        return of(id).get();
    }

    public static void tick(float deltaSeconds) {
        for (Anim anim : ANIMS.values()) {
            anim.update(deltaSeconds);
        }
    }

    public static void clear(String id) {
        ANIMS.remove(id);
    }

    public static void clearAll() {
        ANIMS.clear();
    }
}
