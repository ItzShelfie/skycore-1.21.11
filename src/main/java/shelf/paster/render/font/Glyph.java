package shelf.paster.render.font;

public class Glyph {
    private final float minU, maxU, minV, maxV;
    private final float advance, planeLeft, planeBottom, planeRight, planeTop;

    public Glyph(FontData.GlyphData data, float atlasWidth, float atlasHeight) {
        this.advance = data.getAdvance();

        FontData.BoundsData atlasBounds = data.getAtlasBounds();
        if (atlasBounds != null) {
            this.minU = atlasBounds.getLeft() / atlasWidth;
            this.maxU = atlasBounds.getRight() / atlasWidth;
            this.minV = 1.0f - atlasBounds.getTop() / atlasHeight;
            this.maxV = 1.0f - atlasBounds.getBottom() / atlasHeight;
        } else {
            this.minU = this.maxU = this.minV = this.maxV = 0.0f;
        }

        FontData.BoundsData planeBounds = data.getPlaneBounds();
        if (planeBounds != null) {
            this.planeLeft = planeBounds.getLeft();
            this.planeBottom = planeBounds.getBottom();
            this.planeRight = planeBounds.getRight();
            this.planeTop = planeBounds.getTop();
        } else {
            this.planeLeft = this.planeBottom = this.planeRight = this.planeTop = 0.0f;
        }
    }

    public float getMinU() {
        return minU;
    }

    public float getMaxU() {
        return maxU;
    }

    public float getMinV() {
        return minV;
    }

    public float getMaxV() {
        return maxV;
    }

    public float getAdvance() {
        return advance;
    }

    public float getPlaneLeft() {
        return planeLeft;
    }

    public float getPlaneBottom() {
        return planeBottom;
    }

    public float getPlaneRight() {
        return planeRight;
    }

    public float getPlaneTop() {
        return planeTop;
    }

    public float getWidth() {
        return planeRight - planeLeft;
    }

    public float getHeight() {
        return planeTop - planeBottom;
    }
}
