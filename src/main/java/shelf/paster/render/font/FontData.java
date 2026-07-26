package shelf.paster.render.font;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Parsed MSDF atlas JSON (bmfont / msdf-atlas-gen layout). */
public class FontData {
    private AtlasData atlas;
    private MetricsData metrics;
    private List<GlyphData> glyphs;
    @SerializedName("kerning")
    private List<KerningData> kernings;

    public AtlasData getAtlas() {
        return atlas;
    }

    public MetricsData getMetrics() {
        return metrics;
    }

    public List<GlyphData> getGlyphs() {
        return glyphs;
    }

    public List<KerningData> getKernings() {
        return kernings;
    }

    public static class AtlasData {
        private float distanceRange;
        private float size;
        private float width;
        private float height;
        private String yOrigin;

        public float getDistanceRange() {
            return distanceRange;
        }

        public float getSize() {
            return size;
        }

        public float getWidth() {
            return width;
        }

        public float getHeight() {
            return height;
        }

        public String getYOrigin() {
            return yOrigin;
        }
    }

    public static class MetricsData {
        private float emSize;
        private float lineHeight;
        private float ascender;
        private float descender;

        public float getEmSize() {
            return emSize;
        }

        public float getLineHeight() {
            return lineHeight;
        }

        public float getAscender() {
            return ascender;
        }

        public float getDescender() {
            return descender;
        }

        public static MetricsData defaultMetrics() {
            MetricsData m = new MetricsData();
            m.emSize = 1f;
            m.lineHeight = 1.2f;
            m.ascender = 0.8f;
            m.descender = -0.2f;
            return m;
        }
    }

    public static class GlyphData {
        private int unicode;
        private float advance;
        private BoundsData planeBounds;
        private BoundsData atlasBounds;

        public int getUnicode() {
            return unicode;
        }

        public float getAdvance() {
            return advance;
        }

        public BoundsData getPlaneBounds() {
            return planeBounds;
        }

        public BoundsData getAtlasBounds() {
            return atlasBounds;
        }
    }

    public static class BoundsData {
        private float left;
        private float bottom;
        private float right;
        private float top;

        public float getLeft() {
            return left;
        }

        public float getBottom() {
            return bottom;
        }

        public float getRight() {
            return right;
        }

        public float getTop() {
            return top;
        }
    }

    public static class KerningData {
        @SerializedName("unicode1")
        private int leftChar;
        @SerializedName("unicode2")
        private int rightChar;
        private float advance;

        public int getLeftChar() {
            return leftChar;
        }

        public int getRightChar() {
            return rightChar;
        }

        public float getAdvance() {
            return advance;
        }
    }
}
