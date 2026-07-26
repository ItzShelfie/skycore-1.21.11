package shelf.paster.render;

import shelf.paster.util.MinecraftInstance;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

public class Scissor implements MinecraftInstance {
    private static final Scissor instance = new Scissor();

    public final Vector4f clipRadius = new Vector4f();
    public boolean clipActive;
    public float clipX;
    public float clipY;
    public float clipWidth;
    public float clipHeight;

    public static Scissor scissor() {
        return instance;
    }

    public Vector4f getClipRadius() {
        return clipRadius;
    }

    public boolean isClipActive() {
        return clipActive;
    }

    public float getClipX() {
        return clipX;
    }

    public float getClipY() {
        return clipY;
    }

    public float getClipWidth() {
        return clipWidth;
    }

    public float getClipHeight() {
        return clipHeight;
    }

    public void enableScissor(float x, float y, float width, float height) {
        enableScissor(x, y, width, height, (Vector4f) null);
    }

    public void enableScissor(float x, float y, float width, float height, float radius) {
        enableScissor(x, y, width, height, new Vector4f(Math.max(0f, radius)));
    }

    public void enableScissor(float x, float y, float width, float height, Vector4f radius) {
        if (width <= 0f || height <= 0f) {
            return;
        }

        float minX;
        float minY;
        float maxX;
        float maxY;
        Matrix4f transform = Draw.getCurrentTransform();
        if (transform == null) {
            minX = x;
            minY = y;
            maxX = x + width;
            maxY = y + height;
        } else {
            // Full matrix (scale + translate) — not just m00/m11
            float x0 = transform.m00() * x + transform.m10() * y + transform.m30();
            float y0 = transform.m01() * x + transform.m11() * y + transform.m31();
            float x1 = transform.m00() * (x + width) + transform.m10() * y + transform.m30();
            float y1 = transform.m01() * (x + width) + transform.m11() * y + transform.m31();
            float x2 = transform.m00() * x + transform.m10() * (y + height) + transform.m30();
            float y2 = transform.m01() * x + transform.m11() * (y + height) + transform.m31();
            float x3 = transform.m00() * (x + width) + transform.m10() * (y + height) + transform.m30();
            float y3 = transform.m01() * (x + width) + transform.m11() * (y + height) + transform.m31();
            minX = Math.min(Math.min(x0, x1), Math.min(x2, x3));
            maxX = Math.max(Math.max(x0, x1), Math.max(x2, x3));
            minY = Math.min(Math.min(y0, y1), Math.min(y2, y3));
            maxY = Math.max(Math.max(y0, y1), Math.max(y2, y3));
        }

        float scaledWidth = maxX - minX;
        float scaledHeight = maxY - minY;
        int glX = (int) Math.floor(minX);
        int glY = (int) Math.floor(mc.getWindow().getHeight() - maxY);
        int glWidth = Math.max(0, (int) Math.ceil(scaledWidth));
        int glHeight = Math.max(0, (int) Math.ceil(scaledHeight));

        this.clipActive = true;
        this.clipX = glX;
        this.clipY = glY;
        this.clipWidth = glWidth;
        this.clipHeight = glHeight;

        if (radius != null) {
            float sx = Draw.getCurrentTransformScaleX();
            float sy = Draw.getCurrentTransformScaleY();
            this.clipRadius.set(
                    Math.max(0f, radius.x * sx),
                    Math.max(0f, radius.y * sy),
                    Math.max(0f, radius.z * sx),
                    Math.max(0f, radius.w * sy)
            );
        } else {
            this.clipRadius.set(0f);
        }

        RenderSystem.enableScissorForRenderTypeDraws(glX, glY, glWidth, glHeight);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(glX, glY, glWidth, glHeight);
    }

    public void disableScissor() {
        this.clipActive = false;
        this.clipX = 0f;
        this.clipY = 0f;
        this.clipWidth = 0f;
        this.clipHeight = 0f;
        this.clipRadius.set(0f);

        RenderSystem.disableScissorForRenderTypeDraws();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
