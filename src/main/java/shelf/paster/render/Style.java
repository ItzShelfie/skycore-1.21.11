package shelf.paster.render;

public final class Style {
    private static final int DEFAULT_TINT_COLOR = 0x15141A;
    private static final float DEFAULT_TINT_ALPHA = 0.85f;

    private boolean hasTint;
    private int tintColor;
    private float tintAlpha;

    private boolean hasSmoothing;
    private float smoothingFactor;

    private boolean hasBlurRadius;
    private int blurRadius;

    private boolean hasShadow;
    private float shadowExpand;
    private float shadowFactor;
    private float shadowOffsetX;
    private float shadowOffsetY;
    private int shadowColor;
    private float shadowColorAlpha;

    private boolean hasRefraction;
    private float refThickness;
    private float refFactor;
    private float refDispersion;
    private float refFresnelRange;
    private float refFresnelHardness;
    private float refFresnelFactor;

    private boolean hasGlare;
    private float glareRange;
    private float glareHardness;
    private float glareConvergence;
    private float glareOppositeFactor;
    private float glareFactor;
    private float glareAngleRad;

    public static Style create() {
        return new Style();
    }

    public Style tint(int color, float alpha) {
        this.hasTint = true;
        this.tintColor = color;
        this.tintAlpha = alpha;
        return this;
    }

    public Style smoothing(float factor) {
        this.hasSmoothing = true;
        this.smoothingFactor = factor;
        return this;
    }

    public Style blurRadius(int radius) {
        this.hasBlurRadius = true;
        this.blurRadius = Math.max(0, radius);
        return this;
    }

    public Style shadow(float expand, float factor, float offsetX, float offsetY) {
        this.hasShadow = true;
        this.shadowExpand = expand;
        this.shadowFactor = factor;
        this.shadowOffsetX = offsetX;
        this.shadowOffsetY = offsetY;
        return this;
    }

    public Style shadowColor(int color, float alpha) {
        this.hasShadow = true;
        this.shadowColor = color;
        this.shadowColorAlpha = alpha;
        return this;
    }

    public Style refractionThickness(float value) {
        this.hasRefraction = true;
        this.refThickness = value;
        return this;
    }

    public Style refractionFactor(float value) {
        this.hasRefraction = true;
        this.refFactor = value;
        return this;
    }

    public Style refractionDispersion(float value) {
        this.hasRefraction = true;
        this.refDispersion = value;
        return this;
    }

    public Style fresnelRange(float value) {
        this.hasRefraction = true;
        this.refFresnelRange = value;
        return this;
    }

    public Style fresnelHardness(float value) {
        this.hasRefraction = true;
        this.refFresnelHardness = value;
        return this;
    }

    public Style fresnelFactor(float value) {
        this.hasRefraction = true;
        this.refFresnelFactor = value;
        return this;
    }

    public Style glareRange(float value) {
        this.hasGlare = true;
        this.glareRange = value;
        return this;
    }

    public Style glareHardness(float value) {
        this.hasGlare = true;
        this.glareHardness = value;
        return this;
    }

    public Style glareConvergence(float value) {
        this.hasGlare = true;
        this.glareConvergence = value;
        return this;
    }

    public Style glareOppositeFactor(float value) {
        this.hasGlare = true;
        this.glareOppositeFactor = value;
        return this;
    }

    public Style glareFactor(float value) {
        this.hasGlare = true;
        this.glareFactor = value;
        return this;
    }

    public Style glareAngleRad(float value) {
        this.hasGlare = true;
        this.glareAngleRad = value;
        return this;
    }

    public int getTintColor() {
        return hasTint ? tintColor : DEFAULT_TINT_COLOR;
    }

    public float getTintAlpha() {
        return hasTint ? tintAlpha : DEFAULT_TINT_ALPHA;
    }

    public float getSmoothing() {
        return hasSmoothing ? smoothingFactor : 1.0f;
    }

    public int getBlurRadius() {
        return hasBlurRadius ? blurRadius : 0;
    }

    public float getShadowExpand() {
        return hasShadow ? shadowExpand : 0.0f;
    }

    public float getShadowFactor() {
        return hasShadow ? shadowFactor : 0.0f;
    }

    public float getShadowOffsetX() {
        return hasShadow ? shadowOffsetX : 0.0f;
    }

    public float getShadowOffsetY() {
        return hasShadow ? shadowOffsetY : 0.0f;
    }

    public int getShadowColor() {
        return hasShadow ? shadowColor : 0x000000;
    }

    public float getShadowColorAlpha() {
        return hasShadow ? shadowColorAlpha : 0.0f;
    }

    public float getRefThickness() {
        return hasRefraction ? refThickness : 0.0f;
    }

    public float getRefFactor() {
        return hasRefraction ? refFactor : 1.0f;
    }

    public float getRefDispersion() {
        return hasRefraction ? refDispersion : 0.0f;
    }

    public float getRefFresnelRange() {
        return hasRefraction ? refFresnelRange : 0.0f;
    }

    public float getRefFresnelHardness() {
        return hasRefraction ? refFresnelHardness : 0.0f;
    }

    public float getRefFresnelFactor() {
        return hasRefraction ? refFresnelFactor : 0.0f;
    }

    public float getGlareRange() {
        return hasGlare ? glareRange : 0.0f;
    }

    public float getGlareHardness() {
        return hasGlare ? glareHardness : 0.0f;
    }

    public float getGlareConvergence() {
        return hasGlare ? glareConvergence : 0.0f;
    }

    public float getGlareOppositeFactor() {
        return hasGlare ? glareOppositeFactor : 0.0f;
    }

    public float getGlareFactor() {
        return hasGlare ? glareFactor : 0.0f;
    }

    public float getGlareAngleRad() {
        return hasGlare ? glareAngleRad : 0.0f;
    }
}
