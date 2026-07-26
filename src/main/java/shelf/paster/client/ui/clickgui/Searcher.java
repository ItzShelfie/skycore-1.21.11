package shelf.paster.client.ui.clickgui;

import shelf.paster.client.animation.Animations;
import shelf.paster.client.animation.Easing;
import shelf.paster.client.module.Module;
import shelf.paster.render.Draw;
import shelf.paster.render.Text;
import shelf.paster.render.font.Font;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public final class Searcher {
    private static final Color PANEL = new Color(12, 14, 18, 240);
    private static final Color BORDER = new Color(255, 255, 255, 51);
    private static final Color DOT = new Color(255, 255, 255, 77);
    private static final Color CARET = new Color(255, 255, 255, 200);
    private static final float W = 120f;
    private static final float H = 16f;
    private static final float FONT = 6f;

    private boolean open;
    private final StringBuilder query = new StringBuilder();
    private float x;
    private float y;
    private long lastTypeNanos;
    private int typePulse;

    public boolean isOpen() {
        return open;
    }

    public float presence() {
        return Animations.of("search.open").get();
    }

    public void layoutBelowGui(float guiLocalX, float guiBottomY) {
        this.x = guiLocalX + (ClickGuiTheme.WINDOW_W - W) * 0.5f;
        this.y = guiBottomY + 8f;
    }

    public void open() {
        open = true;
        Animations.of("search.open").to(1f, 0.28f, Easing.CUBIC_OUT);
    }

    public void close() {
        open = false;
        query.setLength(0);
        typePulse = 0;
        Animations.of("search.open").to(0f, 0.18f, Easing.QUART_IN);
    }

    public void toggle() {
        if (open || presence() > 0.5f) {
            close();
        } else {
            open();
        }
    }

    public String query() {
        return query.toString();
    }

    public boolean hasQuery() {
        return !query.isEmpty();
    }

    public boolean matches(Module module) {
        if (query.isEmpty()) {
            return true;
        }
        String q = query.toString().toLowerCase(Locale.ROOT).replace(" ", "");
        String name = module.getName().toLowerCase(Locale.ROOT).replace(" ", "");
        return name.contains(q);
    }

    public List<Module> filter(List<Module> modules) {
        if (query.isEmpty()) {
            return modules;
        }
        List<Module> out = new ArrayList<>();
        for (Module module : modules) {
            if (matches(module)) {
                out.add(module);
            }
        }
        return out;
    }

    public boolean hit(float mx, float my) {
        if (presence() < 0.4f) {
            return false;
        }
        float[] box = animatedBox();
        return mx >= box[0] && my >= box[1] && mx <= box[0] + box[2] && my <= box[1] + box[3];
    }

    public boolean charTyped(int codepoint) {
        if (!open) {
            return false;
        }
        if (Character.isISOControl(codepoint)) {
            return false;
        }
        if (query.length() >= 32) {
            return true;
        }
        query.appendCodePoint(codepoint);
        lastTypeNanos = System.nanoTime();
        typePulse++;
        Animations.of("search.type").snap(0f).to(1f, 0.28f, Easing.CUBIC_OUT);
        return true;
    }

    public boolean backspace() {
        if (!open || query.isEmpty()) {
            return open;
        }
        query.deleteCharAt(query.length() - 1);
        lastTypeNanos = System.nanoTime();
        Animations.of("search.type").snap(0f).to(1f, 0.2f, Easing.CUBIC_OUT);
        return true;
    }

    private float[] animatedBox() {
        float p = presence();
        float slide = (1f - p) * 14f;
        float scale = 0.92f + 0.08f * p;
        float cx = x + W * 0.5f;
        float cy = y + H * 0.5f + slide;
        float aw = W * scale;
        float ah = H * scale;
        return new float[]{cx - aw * 0.5f, cy - ah * 0.5f, aw, ah};
    }

    public void render(Font font) {
        float p = presence();
        if (p <= 0.01f) {
            return;
        }
        float[] box = animatedBox();
        float bx = box[0];
        float by = box[1];
        float bw = box[2];
        float bh = box[3];

        Draw.pushAlpha(p);
        Draw.drawRound(bx, by, bw, bh, 3f, PANEL);
        Draw.drawRound(bx, by, bw, bh, 3f, new Color(0, 0, 0, 0), BORDER, 1.25f);

        if (query.isEmpty()) {
            float dy = by + (bh - 2f) * 0.5f;
            float dx = bx + 8f;
            float bob = (float) Math.sin(System.nanoTime() * 1e-9 * 4.0) * 0.6f;
            Draw.drawRound(dx, dy + bob, 2f, 2f, 1f, DOT);
            Draw.drawRound(dx + 5f, dy - bob * 0.5f, 2f, 2f, 1f, DOT);
            Draw.drawRound(dx + 10f, dy + bob, 2f, 2f, 1f, DOT);
        } else {
            renderTypedText(font, bx, by, bw, bh);
        }
        Draw.popAlpha();
    }

    private void renderTypedText(Font font, float bx, float by, float bw, float bh) {
        String full = query.toString();
        float type = clamp01(Animations.of("search.type").get());
        float age = (System.nanoTime() - lastTypeNanos) / 1_000_000_000f;
        float cursor = bx + 6f;
        float baseY = by + (bh - font.getLineHeight(FONT)) * 0.5f;
        float max = bw - 14f;

        int start = 0;
        while (start < full.length() && font.getWidth(full.substring(start), FONT) > max) {
            start++;
        }
        String visible = full.substring(start);

        for (int i = 0; i < visible.length(); i++) {
            String ch = String.valueOf(visible.charAt(i));
            boolean isLast = (start + i) == full.length() - 1;
            float pop = isLast ? (1f - type) : 0f;
            float yOff = -pop * 4f;
            float scale = 1f + pop * 0.35f;
            float wave = (float) Math.sin((age * 10f) + i * 0.55f) * (isLast ? 0.4f : 0f);
            float alpha = isLast ? (0.55f + 0.45f * type) : 1f;
            Color color = rgba(255, 255, 255, alpha);

            float size = FONT * (isLast ? scale : 1f);
            float gy = baseY + yOff + wave;
            new Text(cursor, gy, font, ch).size(size).color(color).render();
            cursor += font.getWidth(ch, size) + (isLast ? 0.4f : 0f);
        }

        if (((System.currentTimeMillis() / 380L) & 1L) == 0L) {
            float caretPulse = clamp01(0.7f + 0.3f * type);
            Draw.drawRound(cursor + 1f, by + 3.5f, 0.9f, bh - 7f, 0.45f,
                    rgba(255, 255, 255, 0.78f * caretPulse));
        }
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static Color rgba(int r, int g, int b, float a) {
        return new Color(r, g, b, Math.max(0, Math.min(255, Math.round(255f * a))));
    }
}
