package shelf.paster.client.hud.elements;

import shelf.paster.render.Draw;
import shelf.paster.render.Fonts;
import shelf.paster.render.Text;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/** Stacked glass toasts for command feedback (replaces plain chat-only style). */
public final class CommandToast {
    private static final float X = 10f;
    private static final float START_Y = 48f;
    private static final float WIDTH = 220f;
    private static final float HEIGHT = 28f;
    private static final float GAP = 6f;
    private static final float RADIUS = 8f;
    private static final long LIFE_MS = 3200L;
    private static final Color PANEL = new Color(14, 18, 26, 210);
    private static final Color ACCENT = new Color(135, 206, 250);
    private static final Color TEXT = new Color(236, 238, 242);
    private static final Color MUTED = new Color(150, 158, 170);

    private static final Deque<Entry> ENTRIES = new ArrayDeque<>();

    private CommandToast() {
    }

    public static void push(String title, String body) {
        ENTRIES.addFirst(new Entry(title == null ? "cmd" : title, body == null ? "" : body, System.currentTimeMillis()));
        while (ENTRIES.size() > 4) {
            ENTRIES.removeLast();
        }
    }

    public static void render() {
        long now = System.currentTimeMillis();
        Iterator<Entry> it = ENTRIES.iterator();
        float y = START_Y;
        Text.beginBatch();
        while (it.hasNext()) {
            Entry entry = it.next();
            float age = now - entry.born;
            if (age > LIFE_MS) {
                it.remove();
                continue;
            }
            float alpha = age < 180f ? age / 180f : age > LIFE_MS - 320f ? (LIFE_MS - age) / 320f : 1f;
            alpha = Math.max(0f, Math.min(1f, alpha));

            Color panel = withAlpha(PANEL, alpha);
            Color accent = withAlpha(ACCENT, alpha);
            Color text = withAlpha(TEXT, alpha);
            Color muted = withAlpha(MUTED, alpha);

            Draw.drawLayeredGaussian(X, y, WIDTH, HEIGHT, 10f, 4f, RADIUS, panel);
            Draw.drawRound(X, y + 6f, 2.5f, HEIGHT - 12f, 1.2f, accent);

            float textX = X + 12f;
            float titleY = y + 5f;
            float bodyY = y + 15f;
            new Text(textX, titleY, Fonts.MEDIUM(), entry.title).size(8.5f).color(accent).render();
            new Text(textX, bodyY, Fonts.REGULAR(), entry.body).size(10f).color(text).render();
            y += HEIGHT + GAP;
        }
        Text.flushBatch();
    }

    private static Color withAlpha(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(color.getAlpha() * alpha));
    }

    private record Entry(String title, String body, long born) {
    }
}
