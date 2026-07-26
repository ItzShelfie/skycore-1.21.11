package shelf.paster.render.font;

import com.google.gson.Gson;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import shelf.paster.util.MinecraftInstance;

import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

/** MSDF font atlas loaded from pasta:fonts JSON + PNG pairs. */
public class Font {
    private static final int WIDTH_CACHE_LIMIT = 2048;
    private static final Set<Font> INSTANCES = Collections.newSetFromMap(new WeakHashMap<>());

    private final Identifier fontDataJson;
    private final Identifier fontAtlasTexture;
    private AbstractTexture texture;
    private Map<Integer, Glyph> glyphs;
    private Map<Integer, Map<Integer, Float>> kernings;
    private FontData.MetricsData metrics;
    private float distanceRange;
    private volatile boolean loaded;
    private final Map<WidthKey, Float> widthCache = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<WidthKey, Float> eldest) {
            return size() > WIDTH_CACHE_LIMIT;
        }
    };

    public Font(Identifier fontDataJson, Identifier fontAtlasTexture) {
        this.fontDataJson = fontDataJson;
        this.fontAtlasTexture = fontAtlasTexture;
        this.glyphs = Map.of();
        this.kernings = Map.of();
        this.metrics = FontData.MetricsData.defaultMetrics();
        this.distanceRange = 4f;
    }

    /** Loads glyph data when the resource manager is ready (safe to call repeatedly). */
    public synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        ResourceManager resourceManager = MinecraftInstance.mc.getResourceManager();
        if (resourceManager == null) {
            return;
        }
        try {
            Resource resource = resourceManager.getResource(fontDataJson).orElseThrow();
            FontData data = new Gson().fromJson(new InputStreamReader(resource.open()), FontData.class);
            if (data.getAtlas() == null || data.getMetrics() == null || data.getGlyphs() == null) {
                return;
            }

            refreshTexture();

            float atlasWidth = data.getAtlas().getWidth();
            float atlasHeight = data.getAtlas().getHeight();

            this.glyphs = data.getGlyphs().stream()
                    .filter(glyphData -> glyphData.getUnicode() != 0 || glyphData.getAdvance() > 0)
                    .collect(Collectors.toMap(
                            FontData.GlyphData::getUnicode,
                            glyphData -> new Glyph(glyphData, atlasWidth, atlasHeight),
                            (left, right) -> left
                    ));
            // Drop the null/control slot if it collided with missing unicode fields.
            this.glyphs.remove(0);

            this.kernings = new HashMap<>();
            if (data.getKernings() != null) {
                data.getKernings().forEach(kerning -> kernings
                        .computeIfAbsent(kerning.getLeftChar(), k -> new HashMap<>())
                        .put(kerning.getRightChar(), kerning.getAdvance()));
            }

            this.metrics = data.getMetrics();
            this.distanceRange = data.getAtlas().getDistanceRange();
            synchronized (INSTANCES) {
                INSTANCES.add(this);
            }
            loaded = true;
        } catch (Exception e) {
            // Keep stub metrics until assets exist (e.g. missing PNG).
        }
    }

    public static void refreshAll() {
        synchronized (INSTANCES) {
            for (Font atlas : new java.util.ArrayList<>(INSTANCES)) {
                atlas.refreshTexture();
            }
        }
    }

    public void refreshTexture() {
        AbstractTexture currentTexture = MinecraftInstance.mc.getTextureManager().getTexture(fontAtlasTexture);
        this.texture = currentTexture;
    }

    public Glyph getGlyph(int character) {
        ensureLoaded();
        return glyphs.get(character);
    }

    public float getKerning(int left, int right, float scale) {
        ensureLoaded();
        return kernings.getOrDefault(left, Map.of()).getOrDefault(right, 0.0f) * scale;
    }

    public float getWidth(String text, float size) {
        ensureLoaded();
        if (text == null || text.isEmpty()) {
            return 0.0f;
        }

        WidthKey key = new WidthKey(text, Float.floatToIntBits(size));
        Float cached = widthCache.get(key);
        if (cached != null) {
            return cached;
        }

        float width = 0.0f;
        int prevChar = -1;
        float scale = size / metrics.getEmSize();

        for (int i = 0; i < text.length(); i++) {
            int charCode = text.charAt(i);
            Glyph glyph = getGlyph(charCode);
            if (glyph == null) {
                continue;
            }

            if (prevChar != -1) {
                width += getKerning(prevChar, charCode, scale);
            }

            width += glyph.getAdvance() * scale;
            prevChar = charCode;
        }
        widthCache.put(key, width);
        return width;
    }

    public float getLineHeight(float size) {
        ensureLoaded();
        return (metrics.getLineHeight() / metrics.getEmSize()) * size;
    }

    public FontData.MetricsData getMetrics() {
        ensureLoaded();
        return metrics;
    }

    public float getDistanceRange() {
        ensureLoaded();
        return distanceRange;
    }

    public Identifier getFontAtlasTexture() {
        return fontAtlasTexture;
    }

    public GpuTextureView getTextureView() {
        ensureLoaded();
        GpuTextureView view = texture == null ? null : texture.getTextureView();
        if (view == null || view.isClosed() || view.texture().isClosed()) {
            refreshTexture();
            if (texture == null) {
                return null;
            }
            view = texture.getTextureView();
        }
        return view;
    }

    public boolean isLoaded() {
        return loaded;
    }

    private record WidthKey(String text, int sizeBits) {
    }
}
