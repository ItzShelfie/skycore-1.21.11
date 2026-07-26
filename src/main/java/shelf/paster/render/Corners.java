package shelf.paster.render;

import org.joml.Vector4f;

/** Uniform / per-corner radii without writing {@code new Vector4f(...)}. */
public final class Corners {
    private Corners() {
    }

    public static Vector4f of(float radius) {
        return new Vector4f(radius);
    }

    public static Vector4f of(double radius) {
        return new Vector4f((float) radius);
    }

    public static Vector4f of(int radius) {
        return new Vector4f(radius);
    }

    /** topLeft, topRight, bottomRight, bottomLeft */
    public static Vector4f of(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        return new Vector4f(topLeft, topRight, bottomRight, bottomLeft);
    }

    public static Vector4f of(double topLeft, double topRight, double bottomRight, double bottomLeft) {
        return new Vector4f((float) topLeft, (float) topRight, (float) bottomRight, (float) bottomLeft);
    }
}
