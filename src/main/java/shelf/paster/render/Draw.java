package shelf.paster.render;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.texture.OverlayTexture;
import shelf.paster.render.gpu.Sampler;
import shelf.paster.render.gpu.Ubo;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import static shelf.paster.render.gpu.Pipelines.*;
import static com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX;

public class Draw {
    private static TextureTarget gaussianBackgroundCopy;
    private static final ArrayDeque<Float> ALPHA_STACK = new ArrayDeque<>();
    private static final ArrayDeque<Matrix4f> TRANSFORM_STACK = new ArrayDeque<>();
    private static final Matrix4f IDENTITY_MATRIX = new Matrix4f();
    private static final Matrix4f RESOLVED_MATRIX = new Matrix4f();
    private static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);
    private static final Color BACKDROP_BLUR_COLOR = new Color(255, 255, 255, 255);
    private static final Vector4f ZERO_RADIUS = new Vector4f(0.0f);

    public static Vector4f corners(float radius) {
        return Corners.of(radius);
    }

    public static Vector4f corners(double radius) {
        return Corners.of(radius);
    }

    public static Vector4f corners(int radius) {
        return Corners.of(radius);
    }

    public static Vector4f corners(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        return Corners.of(topLeft, topRight, bottomRight, bottomLeft);
    }

    private static final int SHADER_QUAD_VERTEX_COUNT = 4;
    private static final int SHADER_QUAD_VERTEX_BYTES = POSITION_TEX.getVertexSize() * SHADER_QUAD_VERTEX_COUNT;
    private static final int PACKED_ROUND_VERTEX_COUNT = 6;
    private static final int PACKED_ROUND_VERTEX_BYTES = PACKED_RECTANGLE_VERTEX_FORMAT.getVertexSize();
    private static final int STREAM_BUFFER_COUNT = 8;
    private static final GpuBuffer[] SHADER_QUAD_BUFFERS = new GpuBuffer[STREAM_BUFFER_COUNT];
    private static final GpuBuffer[] MESH_VERTEX_BUFFERS = new GpuBuffer[STREAM_BUFFER_COUNT];
    private static final Map<Identifier, CachedSampler> TEXTURE_SAMPLERS = new HashMap<>();
    private static final ByteBuffer SHADER_QUAD_CPU_BUFFER = ByteBuffer
            .allocateDirect(SHADER_QUAD_VERTEX_BYTES)
            .order(ByteOrder.nativeOrder());
    private static ByteBuffer batchQuadCpuBuffer = ByteBuffer
            .allocateDirect(8192)
            .order(ByteOrder.nativeOrder());
    private static ByteBuffer batchPackedRoundCpuBuffer = ByteBuffer
            .allocateDirect(65536)
            .order(ByteOrder.nativeOrder());
    private static int shaderQuadBufferIndex;
    private static int meshVertexBufferIndex;
    private static float alphaMultiplier = 1f;
    private static final GpuSampler CLAMP_LINEAR_SAMPLER = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);

    private static void bindPassTexture(RenderPass pass, String name, GpuTextureView view) {
        pass.bindTexture(name, view, CLAMP_LINEAR_SAMPLER);
    }

    public enum Layer {
        LAYER,
        BACKGROUND
    }

    private static void applyScissor(RenderPass pass) {
        var scissorState = RenderSystem.getScissorStateForRenderTypeDraws();
        if (scissorState.enabled()) {
            pass.enableScissor(scissorState.x(), scissorState.y(), scissorState.width(), scissorState.height());
        } else {
            pass.disableScissor();
        }
    }

    private static void applyScissor(RenderPass pass, boolean enabled, int x, int y, int width, int height) {
        if (enabled && width > 0 && height > 0) {
            pass.enableScissor(x, y, width, height);
        } else {
            pass.disableScissor();
        }
    }

    private static void flushGuiIfNeeded() {
        if (!GuiFlush.needsFlush()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.gameRenderer != null) {
            try {
                var method = mc.gameRenderer.getClass().getMethod("flushGuiRenderState");
                method.invoke(mc.gameRenderer);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        GuiFlush.markFlushed();
    }

    private static GpuTextureView colorTargetView() {
        GpuTextureView override = RenderSystem.outputColorTextureOverride;
        return override != null ? override : Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
    }

    private static GpuTextureView depthTargetView() {
        GpuTextureView override = RenderSystem.outputDepthTextureOverride;
        return override != null ? override : Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();
    }

    public static void pushAlpha(float alpha) {
        ALPHA_STACK.push(alphaMultiplier);
        alphaMultiplier = clamp01(alphaMultiplier * alpha);
    }

    public static void popAlpha() {
        if (ALPHA_STACK.isEmpty()) {
            alphaMultiplier = 1f;
            return;
        }
        alphaMultiplier = ALPHA_STACK.pop();
    }

    public static void pushTransform(Matrix4f transform) {
        Matrix4f base = TRANSFORM_STACK.isEmpty() ? new Matrix4f() : new Matrix4f(TRANSFORM_STACK.peek());
        if (transform != null) {
            base.mul(transform);
        }
        TRANSFORM_STACK.push(base);
    }

    public static void popTransform() {
        if (!TRANSFORM_STACK.isEmpty()) {
            TRANSFORM_STACK.pop();
        }
    }

    public static Matrix4f getCurrentTransform() {
        return TRANSFORM_STACK.isEmpty() ? null : new Matrix4f(TRANSFORM_STACK.peek());
    }

    public static float getCurrentTransformScaleX() {
        Matrix4f transform = TRANSFORM_STACK.peek();
        return transform == null ? 1f : transform.m00();
    }

    public static float getCurrentTransformScaleY() {
        Matrix4f transform = TRANSFORM_STACK.peek();
        return transform == null ? 1f : transform.m11();
    }

    public static float getAlphaMultiplier() {
        return alphaMultiplier;
    }

    public static float applyAlpha(float alpha) {
        return clamp01(alpha * alphaMultiplier);
    }

    public static Color applyAlpha(Color color) {
        float multiplier = alphaMultiplier;
        if (multiplier >= 0.999f) {
            return color;
        }
        int newAlpha = Math.round(color.getAlpha() * multiplier);
        if (newAlpha == color.getAlpha()) {
            return color;
        }
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), newAlpha);
    }

    public static int applyAlpha(int argb) {
        float multiplier = alphaMultiplier;
        if (multiplier >= 0.999f) {
            return argb;
        }

        int alpha = (argb >>> 24) & 255;
        int newAlpha = Math.round(alpha * multiplier);
        if (newAlpha == alpha) {
            return argb;
        }
        return (argb & 0x00FFFFFF) | (newAlpha << 24);
    }

    private static float clamp01(float value) {
        if (value < 0f) {
            return 0f;
        }
        if (value > 1f) {
            return 1f;
        }
        return value;
    }

    public static void drawTexture(Matrix4f matrix4f, Identifier location, float x, float y, float width, float height, float smoothness, Vector4f roundness, Color color) {
        Sampler uniform = getTextureSampler(location);
        Color renderColor = applyAlpha(color);
        drawShader(matrix4f, getTexturePipeLine(), getTextureUniforms(x, y, width, height, roundness, renderColor, renderColor, renderColor, renderColor, smoothness), x, y, width, height, uniform);
    }

    public static void drawTexture(Identifier location, float x, float y, float width, float height, float smoothness, Vector4f roundness, Color color) {
        Sampler uniform = getTextureSampler(location);
        Color renderColor = applyAlpha(color);
        drawShader(null, getTexturePipeLine(), getTextureUniforms(x, y, width, height, roundness, renderColor, renderColor, renderColor, renderColor, smoothness), x, y, width, height, uniform);
    }

    public static void drawTexture(Identifier location, float x, float y, float width, float height, float smoothness, float roundness, Color color) {
        drawTexture(location, x, y, width, height, smoothness, corners(roundness), color);
    }

    public static void drawTexture(Identifier location, float x, float y, float width, float height, float smoothness, double roundness, Color color) {
        drawTexture(location, x, y, width, height, smoothness, corners(roundness), color);
    }

    public static void drawTexture(Identifier location, float x, float y, float width, float height, float smoothness, int roundness, Color color) {
        drawTexture(location, x, y, width, height, smoothness, corners(roundness), color);
    }

    public static void drawTexture(Identifier location, float x, float y, float width, float height, float smoothness, Vector4f roundness, Color color, Vector4f uvRect) {
        Sampler uniform = getTextureSampler(location);
        Color renderColor = applyAlpha(color);
        drawShader(null, getTexturePipeLine(), getTextureUniforms(x, y, width, height, roundness, uvRect, renderColor, renderColor, renderColor, renderColor, smoothness), x, y, width, height, uniform);
    }

    public static void drawTexture(Identifier location, float x, float y, float width, float height, float smoothness, Vector4f roundness, float r, float g, float b, float a) {
        Sampler uniform = getTextureSampler(location);
        Color color = new Color(r, g, b, applyAlpha(a));
        drawShader(null, getTexturePipeLine(), getTextureUniforms(x, y, width, height, roundness, color, color, color, color, smoothness), x, y, width, height, uniform);
    }

    public static void drawTextureView(GpuTextureView textureView, float x, float y, float width, float height, Color color) {
        if (textureView == null) return;
        Color renderColor = applyAlpha(color);
        if (renderColor.getAlpha() <= 0) return;
        drawShader(null, getTexturePipeLine(), getTextureUniforms(x, y, width, height, ZERO_RADIUS, renderColor, renderColor, renderColor, renderColor, 0), x, y, width, height, new Sampler("texSampler", textureView));
    }

    private static Sampler getTextureSampler(Identifier location) {
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(location);
        CachedSampler cached = TEXTURE_SAMPLERS.get(location);
        if (cached != null && cached.texture == texture && isSamplerAlive(cached.sampler)) {
            return cached.sampler;
        }

        Sampler sampler = new Sampler("texSampler", texture.getTextureView());
        TEXTURE_SAMPLERS.put(location, new CachedSampler(texture, sampler));
        return sampler;
    }

    public static void clearResourceCache() {
        for (CachedSampler cached : TEXTURE_SAMPLERS.values()) {
            clearSamplerReference(cached.sampler);
        }
        TEXTURE_SAMPLERS.clear();
    }

    private static boolean isSamplerAlive(Sampler sampler) {
        if (sampler == null || sampler.textureView() == null) {
            return false;
        }

        try {
            return !sampler.textureView().isClosed() && !sampler.textureView().texture().isClosed();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void clearSamplerReference(Sampler sampler) {
        if (sampler != null) {
            clearShaderTextureReference(sampler.textureView());
        }
    }

    public static void drawTextureBatch(Identifier location, QuadBatch batch, float width, float height, float smoothness, Vector4f roundness, Color color) {
        if (batch == null || batch.isEmpty() || width <= 0.0f || height <= 0.0f) {
            return;
        }

        Color renderColor = applyAlpha(color);
        if (renderColor.getAlpha() <= 0) {
            return;
        }

        Sampler sampler = getTextureSampler(location);
        ByteBuffer vertices = writeQuadBatch(batch, width, height);
        GpuBuffer uniforms = getTextureUniforms(0, 0, width, height, roundness, renderColor, renderColor, renderColor, renderColor, smoothness);
        Draw.drawVertexBuffer(
                vertices,
                batch.size() * 6,
                getTextureTrianglesPipeline(),
                List.of(new Ubo("params", uniforms)),
                sampler
        );
    }

    public static void drawTextureBatch(Identifier location, ClipQuadBatch batch, float width, float height, float smoothness, Vector4f roundness, Color color) {
        if (batch == null || batch.isEmpty() || width <= 0.0f || height <= 0.0f) {
            return;
        }

        Color renderColor = applyAlpha(color);
        if (renderColor.getAlpha() <= 0) {
            return;
        }

        Sampler sampler = getTextureSampler(location);
        ByteBuffer vertices = writeClipQuadBatch(batch, width, height);
        GpuBuffer uniforms = getTextureUniforms(0, 0, width, height, roundness, renderColor, renderColor, renderColor, renderColor, smoothness);
        Draw.drawVertexBuffer(
                vertices,
                batch.size() * 6,
                getTextureClippedTrianglesPipeline(),
                List.of(new Ubo("params", uniforms)),
                sampler
        );
    }

    public static void drawTextureAtlasBatch(Identifier location, TextureAtlasBatch batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }

        Sampler sampler = getTextureSampler(location);
        ByteBuffer vertices = writeTextureAtlasBatch(batch);
        Draw.drawVertexBuffer(
                vertices,
                batch.vertexCount(),
                getTextureAtlasClippedPipeline(),
                List.of(new Ubo("PackedGlobals", getPackedRectangleUniforms())),
                sampler
        );
    }

    public static void drawFullScreenQuad(BufferBuilder  builder) {
        builder.addVertex(-1, -1, 0).setUv(0, 0);
        builder.addVertex(-1, 1, 0).setUv(0, 1);
        builder.addVertex(1, 1, 0).setUv(1, 1);
        builder.addVertex(-1, -1, 0).setUv(0, 0);
        builder.addVertex(1, 1, 0).setUv(1, 1);
        builder.addVertex(1, -1, 0).setUv(1, 0);
    }
    public static void drawRound(float x, float y, float width, float height, Vector4f roundness, float r, float g, float b, float a) {
        Color color = new Color(r, g, b, a);
        drawRound(x, y, width, height, roundness, color, color, color, color, color, color, color, color, 0);
    }
    public static void drawRound(float x, float y, float width, float height, Vector4f roundness, Color color1, Color color2, Color color3, Color color4) {
        drawRound(x, y, width, height, roundness, color1, color2, color3, color4, TRANSPARENT_COLOR, TRANSPARENT_COLOR, TRANSPARENT_COLOR, TRANSPARENT_COLOR, 0);
    }
    public static void drawRound(float x, float y, float width, float height, Vector4f roundness, Color color) {
        drawRound(x, y, width, height, roundness, color, color, color, color, TRANSPARENT_COLOR, TRANSPARENT_COLOR, TRANSPARENT_COLOR, TRANSPARENT_COLOR, 0);
    }

    public static void drawRound(float x, float y, float width, float height, float roundness, Color color) {
        drawRound(x, y, width, height, corners(roundness), color);
    }

    public static void drawRound(float x, float y, float width, float height, double roundness, Color color) {
        drawRound(x, y, width, height, corners(roundness), color);
    }

    public static void drawRound(float x, float y, float width, float height, int roundness, Color color) {
        drawRound(x, y, width, height, corners(roundness), color);
    }

    public static void drawRoundBatch(QuadBatch batch, float width, float height, Vector4f roundness, Color color) {
        drawRoundBatch(batch, width, height, roundness, color, color, color, color, TRANSPARENT_COLOR, TRANSPARENT_COLOR, TRANSPARENT_COLOR, TRANSPARENT_COLOR, 0, true);
    }

    public static void drawRoundBatch(RoundBatch batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }
        drawPackedRoundVertices(batch.vertices(), batch.vertexCount());
    }

    public static void drawRound(float x, float y, float width, float height, Vector4f roundness, Color mainColor, Color strokeColor, float thickness) {
        drawRound(x, y, width, height, roundness, mainColor, mainColor, mainColor, mainColor, strokeColor, strokeColor, strokeColor, strokeColor, thickness, true);
    }

    public static void drawRound(float x, float y, float width, float height, float roundness, Color mainColor, Color strokeColor, float thickness) {
        drawRound(x, y, width, height, corners(roundness), mainColor, strokeColor, thickness);
    }

    public static void drawRound(float x, float y, float width, float height, double roundness, Color mainColor, Color strokeColor, float thickness) {
        drawRound(x, y, width, height, corners(roundness), mainColor, strokeColor, thickness);
    }

    public static void drawRound(float x, float y, float width, float height, int roundness, Color mainColor, Color strokeColor, float thickness) {
        drawRound(x, y, width, height, corners(roundness), mainColor, strokeColor, thickness);
    }

    public static void drawRound(float x, float y, float width, float height, Vector4f roundness, Color mainColor, Color strokeColor, float thickness, boolean overlay) {
        drawRound(x, y, width, height, roundness, mainColor, mainColor, mainColor, mainColor, strokeColor, strokeColor, strokeColor, strokeColor, thickness, overlay);
    }
    public static void drawRound(float x, float y, float width, float height, Vector4f roundness, Color color1, Color color2, Color color3, Color color4, Color stroke1, Color stroke2, Color stroke3, Color stroke4, float thickness) {
        drawRound(x, y, width, height, roundness, color1, color2, color3, color4, stroke1, stroke2, stroke3, stroke4, thickness, true);
    }
    public static void drawRect(float x, float y, float width, float height, int color) {
        drawRound(x, y, width, height, ZERO_RADIUS, new Color(color, true));
    }
    public static void drawRound(float x, float y, float width, float height, Vector4f roundness, int color) {
        drawRound(x, y, width, height, roundness, new Color(color, true));
    }
    public static void drawRound(float x, float y, float width, float height, float round, int color) {
        drawRound(x, y, width, height, corners(round), new Color(color, true));
    }

    public static void drawRound(float x, float y, float width, float height, double round, int color) {
        drawRound(x, y, width, height, corners(round), new Color(color, true));
    }

    public static void drawRound(float x, float y, float width, float height, int round, int color) {
        drawRound(x, y, width, height, corners(round), new Color(color, true));
    }
    public static void drawRound(float x, float y, float width, float height, Vector4f roundness, Color color1, Color color2, Color color3, Color color4, Color stroke1, Color stroke2, Color stroke3, Color stroke4, float thickness, boolean overlay) {
        Color c1 = applyAlpha(color1);
        Color c2 = applyAlpha(color2);
        Color c3 = applyAlpha(color3);
        Color c4 = applyAlpha(color4);
        Color s1 = applyAlpha(stroke1);
        Color s2 = applyAlpha(stroke2);
        Color s3 = applyAlpha(stroke3);
        Color s4 = applyAlpha(stroke4);
        drawShader(null, getRectanglePipeline(), getRectangleUniforms(width, height, roundness, c1, c2, c3, c4, s1, s2, s3, s4, thickness, overlay ? 1f : 0f), x, y, width, height);
    }

    public static void drawRoundBatch(QuadBatch batch, float width, float height, Vector4f roundness, Color color1, Color color2, Color color3, Color color4, Color stroke1, Color stroke2, Color stroke3, Color stroke4, float thickness, boolean overlay) {
        if (batch == null || batch.isEmpty() || width <= 0.0f || height <= 0.0f) {
            return;
        }

        Color c1 = applyAlpha(color1);
        Color c2 = applyAlpha(color2);
        Color c3 = applyAlpha(color3);
        Color c4 = applyAlpha(color4);
        Color s1 = applyAlpha(stroke1);
        Color s2 = applyAlpha(stroke2);
        Color s3 = applyAlpha(stroke3);
        Color s4 = applyAlpha(stroke4);
        if (c1.getAlpha() <= 0 && c2.getAlpha() <= 0 && c3.getAlpha() <= 0 && c4.getAlpha() <= 0
                && s1.getAlpha() <= 0 && s2.getAlpha() <= 0 && s3.getAlpha() <= 0 && s4.getAlpha() <= 0) {
            return;
        }

        ByteBuffer vertices = writeQuadBatch(batch, width, height);
        GpuBuffer uniforms = getRectangleUniforms(width, height, roundness, c1, c2, c3, c4, s1, s2, s3, s4, thickness, overlay ? 1f : 0f);
        Draw.drawVertexBuffer(
                vertices,
                batch.size() * 6,
                getRectangleTrianglesPipeline(),
                List.of(new Ubo("params", uniforms))
        );
    }

    public static void drawGaussian(float x, float y, float width, float height, Vector4f roundness, Color color) {
        drawGaussian(x, y, width, height, 12.0f, roundness, color, Layer.LAYER);
    }

    public static void drawGaussian(float x, float y, float width, float height, float roundness, Color color) {
        drawGaussian(x, y, width, height, 12.0f, corners(roundness), color, Layer.LAYER);
    }

    public static void drawGaussian(float x, float y, float width, float height, double roundness, Color color) {
        drawGaussian(x, y, width, height, 12.0f, corners(roundness), color, Layer.LAYER);
    }

    public static void drawGaussian(float x, float y, float width, float height, int roundness, Color color) {
        drawGaussian(x, y, width, height, 12.0f, corners(roundness), color, Layer.LAYER);
    }

    public static void drawGaussian(float x, float y, float width, float height, Vector4f roundness, int color) {
        drawGaussian(x, y, width, height, 12.0f, roundness, new Color(color, true), Layer.LAYER);
    }

    public static void drawGaussian(float x, float y, float width, float height, float roundness, int color) {
        drawGaussian(x, y, width, height, 12.0f, corners(roundness), new Color(color, true), Layer.LAYER);
    }

    public static void drawGaussian(float x, float y, float width, float height, double roundness, int color) {
        drawGaussian(x, y, width, height, 12.0f, corners(roundness), new Color(color, true), Layer.LAYER);
    }

    public static void drawGaussian(float x, float y, float width, float height, int roundness, int color) {
        drawGaussian(x, y, width, height, 12.0f, corners(roundness), new Color(color, true), Layer.LAYER);
    }

    public static void drawGaussian(float x, float y, float width, float height, Vector4f roundness, Color color, Layer layer) {
        drawGaussian(x, y, width, height, 12.0f, roundness, color, layer);
    }

    public static void drawGaussian(float x, float y, float width, float height, float roundness, Color color, Layer layer) {
        drawGaussian(x, y, width, height, 12.0f, corners(roundness), color, layer);
    }

    public static void drawGaussian(float x, float y, float width, float height, double roundness, Color color, Layer layer) {
        drawGaussian(x, y, width, height, 12.0f, corners(roundness), color, layer);
    }

    public static void drawGaussian(float x, float y, float width, float height, int roundness, Color color, Layer layer) {
        drawGaussian(x, y, width, height, 12.0f, corners(roundness), color, layer);
    }

    public static void drawGaussian(float x, float y, float width, float height, Vector4f roundness, int color, Layer layer) {
        drawGaussian(x, y, width, height, 12.0f, roundness, new Color(color, true), layer);
    }

    public static void drawGaussian(float x, float y, float width, float height, float blurRadius, float roundness, Color color) {
        drawGaussian(x, y, width, height, blurRadius, corners(roundness), color, Layer.LAYER);
    }

    public static void drawGaussian(float x, float y, float width, float height, float blurRadius, double roundness, Color color) {
        drawGaussian(x, y, width, height, blurRadius, corners(roundness), color, Layer.LAYER);
    }

    public static void drawGaussian(float x, float y, float width, float height, float blurRadius, int roundness, Color color) {
        drawGaussian(x, y, width, height, blurRadius, corners(roundness), color, Layer.LAYER);
    }

    public static void drawGaussian(float x, float y, float width, float height, float blurRadius, float roundness, int color) {
        drawGaussian(x, y, width, height, blurRadius, corners(roundness), new Color(color, true), Layer.LAYER);
    }

    public static void drawGaussian(float x, float y, float width, float height, float blurRadius, float roundness, Color color, Layer layer) {
        drawGaussian(x, y, width, height, blurRadius, corners(roundness), color, layer);
    }

    public static void drawGaussian(float x, float y, float width, float height, float blurRadius, float roundness, int color, Layer layer) {
        drawGaussian(x, y, width, height, blurRadius, corners(roundness), new Color(color, true), layer);
    }

    public static void drawGaussian(float x, float y, float width, float height, float blurRadius, Vector4f roundness, int color) {
        drawGaussian(x, y, width, height, blurRadius, roundness, new Color(color, true), Layer.LAYER);
    }

    public static void drawGaussian(float x, float y, float width, float height, float blurRadius, Vector4f roundness, int color, Layer layer) {
        drawGaussian(x, y, width, height, blurRadius, roundness, new Color(color, true), layer);
    }

    public static void drawGaussian(float x, float y, float width, float height, float blurRadius, Vector4f roundness, float r, float g, float b, float a) {
        drawGaussian(x, y, width, height, blurRadius, roundness, new Color(r, g, b, a), Layer.LAYER);
    }

    public static void drawGaussian(float x, float y, float width, float height, float blurRadius, Vector4f roundness, float r, float g, float b, float a, Layer layer) {
        drawGaussian(x, y, width, height, blurRadius, roundness, new Color(r, g, b, a), layer);
    }

    public static void drawGaussian(float x, float y, float width, float height, float blurRadius, Vector4f roundness, Color color) {
        drawGaussian(x, y, width, height, blurRadius, roundness, color, Layer.LAYER);
    }

    public static void drawGaussian(float x, float y, float width, float height, float blurRadius, Vector4f roundness, Color color, Layer layer) {
        if (width <= 0.0f || height <= 0.0f || color.getAlpha() <= 0) {
            return;
        }

        Color renderColor = applyAlpha(color);
        if (renderColor.getAlpha() <= 0) {
            return;
        }

        boolean background = layer == Layer.BACKGROUND;
        float blur = Math.max(0.0f, blurRadius);
        if (background) {
            drawGaussianBackground(x, y, width, height, blur, roundness, renderColor);
            return;
        }

        drawShader(
                null,
                getGaussianPipeline(),
                getGaussianUniforms(width, height, blur, roundness, renderColor),
                x - blur,
                y - blur,
                width + blur * 2.0f,
                height + blur * 2.0f
        );
    }

    public static void drawLayeredGaussian(float x, float y, float width, float height, float backdropBlurRadius, float foregroundBlurRadius, Vector4f roundness, Color color) {
        drawGaussian(x, y, width, height, backdropBlurRadius, roundness, BACKDROP_BLUR_COLOR, Layer.BACKGROUND);
        drawGaussian(x, y, width, height, foregroundBlurRadius, roundness, color, Layer.LAYER);
    }

    public static void drawLayeredGaussian(float x, float y, float width, float height, float backdropBlurRadius, float foregroundBlurRadius, float roundness, Color color) {
        drawLayeredGaussian(x, y, width, height, backdropBlurRadius, foregroundBlurRadius, corners(roundness), color);
    }

    public static void drawLayeredGaussian(float x, float y, float width, float height, float backdropBlurRadius, float foregroundBlurRadius, double roundness, Color color) {
        drawLayeredGaussian(x, y, width, height, backdropBlurRadius, foregroundBlurRadius, corners(roundness), color);
    }

    public static void drawLayeredGaussian(float x, float y, float width, float height, float backdropBlurRadius, float foregroundBlurRadius, int roundness, Color color) {
        drawLayeredGaussian(x, y, width, height, backdropBlurRadius, foregroundBlurRadius, corners(roundness), color);
    }

    /** Soft drop-shadow (LAYER gaussian halo), no screen sampling. */
    public static void drawShadow(float x, float y, float width, float height, Vector4f roundness, float blurRadius, float offsetX, float offsetY, Color shadowColor) {
        drawGaussian(x + offsetX, y + offsetY, width, height, blurRadius, roundness, shadowColor, Layer.LAYER);
    }

    public static void drawShadow(float x, float y, float width, float height, float roundness, float blurRadius, float offsetX, float offsetY, Color shadowColor) {
        drawShadow(x, y, width, height, corners(roundness), blurRadius, offsetX, offsetY, shadowColor);
    }

    public static void drawShadow(float x, float y, float width, float height, double roundness, float blurRadius, float offsetX, float offsetY, Color shadowColor) {
        drawShadow(x, y, width, height, corners(roundness), blurRadius, offsetX, offsetY, shadowColor);
    }

    public static void drawShadow(float x, float y, float width, float height, int roundness, float blurRadius, float offsetX, float offsetY, Color shadowColor) {
        drawShadow(x, y, width, height, corners(roundness), blurRadius, offsetX, offsetY, shadowColor);
    }

    public static void drawShadow(float x, float y, float width, float height, Vector4f roundness, Color shadowColor) {
        drawShadow(x, y, width, height, roundness, 10f, 0f, 3f, shadowColor);
    }

    public static void drawShadow(float x, float y, float width, float height, float roundness, Color shadowColor) {
        drawShadow(x, y, width, height, corners(roundness), shadowColor);
    }

    public static void drawShadow(float x, float y, float width, float height, double roundness, Color shadowColor) {
        drawShadow(x, y, width, height, corners(roundness), shadowColor);
    }

    public static void drawShadow(float x, float y, float width, float height, int roundness, Color shadowColor) {
        drawShadow(x, y, width, height, corners(roundness), shadowColor);
    }

    /**
     * Backdrop blur rect: samples the framebuffer inside a rounded mask.
     * Same as layered's background pass — no LAYER glow halo.
     * {@code tint} controls frost (dark + high alpha = stronger tint). Use white opaque for clear blur.
     */
    public static void drawBlur(float x, float y, float width, float height, float blurRadius, Vector4f roundness, Color tint) {
        Color color = tint == null ? BACKDROP_BLUR_COLOR : tint;
        // Near-white low-alpha is invisible over the same pixels — bump sample.
        // Dark frost tints may stay translucent (ClickGUI shell).
        if (color.getAlpha() < 200 && color.getRed() + color.getGreen() + color.getBlue() > 600) {
            color = BACKDROP_BLUR_COLOR;
        }
        drawGaussian(x, y, width, height, Math.max(1f, blurRadius), roundness, color, Layer.BACKGROUND);
    }

    public static void drawBlur(float x, float y, float width, float height, float blurRadius, float roundness, Color tint) {
        drawBlur(x, y, width, height, blurRadius, corners(roundness), tint);
    }

    public static void drawBlur(float x, float y, float width, float height, float blurRadius, double roundness, Color tint) {
        drawBlur(x, y, width, height, blurRadius, corners(roundness), tint);
    }

    public static void drawBlur(float x, float y, float width, float height, float blurRadius, int roundness, Color tint) {
        drawBlur(x, y, width, height, blurRadius, corners(roundness), tint);
    }

    public static void drawBlur(float x, float y, float width, float height, Vector4f roundness, Color tint) {
        drawBlur(x, y, width, height, 14f, roundness, tint);
    }

    public static void drawBlur(float x, float y, float width, float height, float roundness, Color tint) {
        drawBlur(x, y, width, height, 14f, corners(roundness), tint);
    }

    public static void drawBlur(float x, float y, float width, float height, double roundness, Color tint) {
        drawBlur(x, y, width, height, 14f, corners(roundness), tint);
    }

    public static void drawBlur(float x, float y, float width, float height, int roundness, Color tint) {
        drawBlur(x, y, width, height, 14f, corners(roundness), tint);
    }

    public static void drawBlur(float x, float y, float width, float height, float blurRadius, Vector4f roundness) {
        drawBlur(x, y, width, height, blurRadius, roundness, BACKDROP_BLUR_COLOR);
    }

    public static void drawBlur(float x, float y, float width, float height, float blurRadius, float roundness) {
        drawBlur(x, y, width, height, blurRadius, corners(roundness), BACKDROP_BLUR_COLOR);
    }

    public static void drawBlur(float x, float y, float width, float height, float blurRadius, double roundness) {
        drawBlur(x, y, width, height, blurRadius, corners(roundness), BACKDROP_BLUR_COLOR);
    }

    public static void drawBlur(float x, float y, float width, float height, float blurRadius, int roundness) {
        drawBlur(x, y, width, height, blurRadius, corners(roundness), BACKDROP_BLUR_COLOR);
    }

    public static void drawGaussianBatch(QuadBatch batch, float width, float height, float blurRadius, Vector4f roundness, Color color) {
        if (batch == null || batch.isEmpty() || width <= 0.0f || height <= 0.0f || color.getAlpha() <= 0) {
            return;
        }

        Color renderColor = applyAlpha(color);
        if (renderColor.getAlpha() <= 0) {
            return;
        }

        float blur = Math.max(0.0f, blurRadius);
        ByteBuffer vertices = writeQuadBatch(batch, width + blur * 2.0f, height + blur * 2.0f, -blur, -blur);
        GpuBuffer uniforms = getGaussianUniforms(width, height, blur, roundness, renderColor);
        Draw.drawVertexBuffer(
                vertices,
                batch.size() * 6,
                getGaussianTrianglesPipeline(),
                List.of(new Ubo("params", uniforms))
        );
    }

    private static void drawGaussianBackground(float x, float y, float width, float height, float blurRadius, Vector4f roundness, Color color) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer == null || RenderSystem.tryGetDevice() == null) {
            return;
        }

        flushGuiIfNeeded();

        RenderTarget mainTarget = mc.getMainRenderTarget();
        if (mainTarget == null || mainTarget.getColorTexture() == null || mainTarget.getColorTextureView() == null) {
            return;
        }

        try {
            ensureGaussianBackgroundCopy(mainTarget);
            if (gaussianBackgroundCopy == null || gaussianBackgroundCopy.getColorTexture() == null || gaussianBackgroundCopy.getColorTextureView() == null) {
                return;
            }

            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                    mainTarget.getColorTexture(),
                    gaussianBackgroundCopy.getColorTexture(),
                    0, 0, 0, 0, 0,
                    mainTarget.width,
                    mainTarget.height
            );

            drawShader(
                    null,
                    getGaussianBackgroundPipeline(),
                    getGaussianUniforms(width, height, blurRadius, roundness, color, 1),
                    x,
                    y,
                    width,
                    height,
                    new Sampler("Sampler0", gaussianBackgroundCopy.getColorTextureView())
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void ensureGaussianBackgroundCopy(RenderTarget mainTarget) {
        if (gaussianBackgroundCopy != null
                && gaussianBackgroundCopy.width == mainTarget.width
                && gaussianBackgroundCopy.height == mainTarget.height) {
            return;
        }

        if (gaussianBackgroundCopy != null) {
            clearShaderTextureReference(gaussianBackgroundCopy.getColorTextureView());
            gaussianBackgroundCopy.destroyBuffers();
        }

        gaussianBackgroundCopy = new TextureTarget("pasta-gaussian-background-copy", mainTarget.width, mainTarget.height, false);
    }

    public static void drawGlass(float x, float y, float width, float height, Vector4f roundness, float alpha, Color tint) {
        float combinedAlpha = clamp01(tint.getAlpha() / 255.0f * alpha);
        Color color = new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), Math.round(combinedAlpha * 255.0f));
        drawGaussian(x, y, width, height, 0.0f, roundness, color, Layer.BACKGROUND);
    }

    public static void drawGlass(float x, float y, float width, float height, float roundness, float alpha, Color tint) {
        drawGlass(x, y, width, height, corners(roundness), alpha, tint);
    }

    public static void drawGlass(float x, float y, float width, float height, double roundness, float alpha, Color tint) {
        drawGlass(x, y, width, height, corners(roundness), alpha, tint);
    }

    public static void drawGlass(float x, float y, float width, float height, int roundness, float alpha, Color tint) {
        drawGlass(x, y, width, height, corners(roundness), alpha, tint);
    }

    public static void drawGlass(GuiGraphics graphics, float x, float y, float width, float height, Vector4f roundness, float alpha, Color tint) {
        drawGlass(x, y, width, height, roundness, alpha, tint);
    }

    public static void drawGlass(GuiGraphics graphics, float x, float y, float width, float height, Vector4f roundness, Style style, float hover, float focus) {
        Style safeStyle = style == null ? Style.create() : style;
        int rgb = safeStyle.getTintColor();
        int alpha = Math.round(clamp01(safeStyle.getTintAlpha()) * 255.0f);
        Color color = new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, alpha);
        drawGaussian(x, y, width, height, 0.0f, roundness, color, Layer.BACKGROUND);
    }

    public static void drawShader(@Nullable Matrix4f matrix4f, RenderPipeline pipeline, GpuBuffer uniforms, float x, float y, float width, float height, Sampler... samplers) {
        if (RenderSystem.tryGetDevice() == null) return;
        if (hasInvalidSampler(samplers)) return;
        flushGuiIfNeeded();

        GpuBuffer vertexBuffer = updateShaderQuadBuffer(matrix4f, x, y, width, height);
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "pasta draw",
                colorTargetView(),
                OptionalInt.empty(),
                depthTargetView(),
                OptionalDouble.empty()
        )) {
            pass.setPipeline(pipeline);
            applyScissor(pass);
            pass.setUniform("params", uniforms);
            for (Sampler sampler : samplers) {
                bindPassTexture(pass, sampler.name(), sampler.textureView());
            }
            pass.setVertexBuffer(0, vertexBuffer);
            pass.draw(0, SHADER_QUAD_VERTEX_COUNT);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            clearShaderTextureReferences(samplers);
        }
    }
    public static void drawShader(@Nullable Matrix4f matrix4f, RenderPipeline pipeline, List<Ubo> uboUniforms, float x, float y, float width, float height, Sampler... samplers) {
        if (RenderSystem.tryGetDevice() == null) return;
        if (hasInvalidSampler(samplers)) return;
        flushGuiIfNeeded();

        GpuBuffer vertexBuffer = updateShaderQuadBuffer(matrix4f, x, y, width, height);
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "pasta draw",
                colorTargetView(),
                OptionalInt.empty(),
                depthTargetView(),
                OptionalDouble.empty()
        )) {
            pass.setPipeline(pipeline);
            applyScissor(pass);

            for (Ubo uboUniform : uboUniforms) {
                pass.setUniform(uboUniform.name(), uboUniform.uniformBuffer());
            }

            for (Sampler sampler : samplers) {
                bindPassTexture(pass, sampler.name(), sampler.textureView());
            }
            pass.setVertexBuffer(0, vertexBuffer);
            pass.draw(0, SHADER_QUAD_VERTEX_COUNT);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            clearShaderTextureReferences(samplers);
        }
    }
    public static void drawMesh(MeshData meshData, RenderPipeline pipeline, List<Ubo> uboUniforms, Sampler... samplers) {
        if (RenderSystem.tryGetDevice() == null) return;
        if (hasInvalidSampler(samplers)) return;
        flushGuiIfNeeded();
        GpuBuffer vertexBuffer = updateMeshVertexBuffer(meshData.vertexBuffer());
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Custom Shader Pass",
                colorTargetView(),
                OptionalInt.empty(),
                depthTargetView(),
                OptionalDouble.empty()
        )) {
            pass.setPipeline(pipeline);
            applyScissor(pass);

            for (Ubo uboUniform : uboUniforms) {
                pass.setUniform(uboUniform.name(), uboUniform.uniformBuffer());
            }

            for (Sampler sampler : samplers) {
                bindPassTexture(pass, sampler.name(), sampler.textureView());
            }
            pass.setVertexBuffer(0, vertexBuffer);
            
            if (meshData.drawState().indexCount() > 0) {
                 pass.draw(0, meshData.drawState().vertexCount());
            } else {
                 pass.draw(0, meshData.drawState().vertexCount());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            clearShaderTextureReferences(samplers);
        }
    }

    public static void drawVertexBuffer(ByteBuffer vertexData, int vertexCount, RenderPipeline pipeline, List<Ubo> uboUniforms, Sampler... samplers) {
        drawVertexBuffer(vertexData, vertexCount, pipeline, uboUniforms, false, 0, 0, 0, 0, samplers);
    }

    public static void drawVertexBuffer(ByteBuffer vertexData, int vertexCount, RenderPipeline pipeline, List<Ubo> uboUniforms, boolean useCapturedScissor, int scissorX, int scissorY, int scissorWidth, int scissorHeight, Sampler... samplers) {
        if (RenderSystem.tryGetDevice() == null || vertexCount <= 0 || vertexData == null || !vertexData.hasRemaining()) return;
        if (hasInvalidSampler(samplers)) return;
        flushGuiIfNeeded();
        GpuBuffer vertexBuffer = updateMeshVertexBuffer(vertexData);
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Custom Shader Vertex Pass",
                colorTargetView(),
                OptionalInt.empty(),
                depthTargetView(),
                OptionalDouble.empty()
        )) {
            pass.setPipeline(pipeline);
            if (useCapturedScissor) {
                applyScissor(pass, true, scissorX, scissorY, scissorWidth, scissorHeight);
            } else {
                applyScissor(pass);
            }

            for (Ubo uboUniform : uboUniforms) {
                pass.setUniform(uboUniform.name(), uboUniform.uniformBuffer());
            }

            for (Sampler sampler : samplers) {
                bindPassTexture(pass, sampler.name(), sampler.textureView());
            }
            pass.setVertexBuffer(0, vertexBuffer);
            pass.draw(0, vertexCount);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            clearShaderTextureReferences(samplers);
        }
    }

    private static void clearShaderTextureReferences(Sampler... samplers) {
        if (samplers == null || samplers.length == 0) {
            return;
        }

        for (Sampler sampler : samplers) {
            if (sampler != null) {
                clearShaderTextureReference(sampler.textureView());
            }
        }
    }

    private static boolean hasInvalidSampler(Sampler... samplers) {
        if (samplers == null) {
            return false;
        }

        for (Sampler sampler : samplers) {
            if (sampler == null || sampler.textureView() == null) {
                return true;
            }
            if (sampler.textureView().isClosed() || sampler.textureView().texture().isClosed()) {
                clearShaderTextureReference(sampler.textureView());
                return true;
            }
        }

        return false;
    }

    private static void clearShaderTextureReference(@Nullable GpuTextureView textureView) {
        // Shader slot texture tracking was removed in recent RenderSystem refactors.
    }

    private static GpuBuffer updateShaderQuadBuffer(@Nullable Matrix4f matrix4f, float x, float y, float width, float height) {
        Matrix4f matrix = resolveMatrixRef(matrix4f);
        ByteBuffer cpuBuffer = SHADER_QUAD_CPU_BUFFER;
        cpuBuffer.clear();

        if (matrix == IDENTITY_MATRIX) {
            putRawVertex(cpuBuffer, x, y, 0f, 0f);
            putRawVertex(cpuBuffer, x, y + height, 0f, 1f);
            putRawVertex(cpuBuffer, x + width, y, 1f, 0f);
            putRawVertex(cpuBuffer, x + width, y + height, 1f, 1f);
        } else {
            putTransformedVertex(cpuBuffer, matrix, x, y, 0f, 0f);
            putTransformedVertex(cpuBuffer, matrix, x, y + height, 0f, 1f);
            putTransformedVertex(cpuBuffer, matrix, x + width, y, 1f, 0f);
            putTransformedVertex(cpuBuffer, matrix, x + width, y + height, 1f, 1f);
        }

        cpuBuffer.flip();
        GpuBuffer vertexBuffer = nextShaderQuadBuffer();
        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(vertexBuffer.slice(0, cpuBuffer.remaining()), cpuBuffer);
        return vertexBuffer;
    }

    private static ByteBuffer writeQuadBatch(QuadBatch batch, float width, float height) {
        return writeQuadBatch(batch, width, height, 0.0f, 0.0f);
    }

    private static ByteBuffer writeQuadBatch(QuadBatch batch, float width, float height, float offsetX, float offsetY) {
        int requiredBytes = batch.size() * 6 * POSITION_TEX.getVertexSize();
        if (batchQuadCpuBuffer.capacity() < requiredBytes) {
            batchQuadCpuBuffer = ByteBuffer
                    .allocateDirect(nextPowerOfTwo(requiredBytes))
                    .order(ByteOrder.nativeOrder());
        }

        ByteBuffer buffer = batchQuadCpuBuffer;
        buffer.clear();
        Matrix4f matrix = resolveMatrixRef(null);
        boolean identity = matrix == IDENTITY_MATRIX;
        for (int i = 0; i < batch.size(); i++) {
            float x = batch.x(i) + offsetX;
            float y = batch.y(i) + offsetY;
            if (identity) {
                putRawVertex(buffer, x, y, 0f, 0f);
                putRawVertex(buffer, x, y + height, 0f, 1f);
                putRawVertex(buffer, x + width, y, 1f, 0f);
                putRawVertex(buffer, x + width, y, 1f, 0f);
                putRawVertex(buffer, x, y + height, 0f, 1f);
                putRawVertex(buffer, x + width, y + height, 1f, 1f);
            } else {
                putTransformedVertex(buffer, matrix, x, y, 0f, 0f);
                putTransformedVertex(buffer, matrix, x, y + height, 0f, 1f);
                putTransformedVertex(buffer, matrix, x + width, y, 1f, 0f);
                putTransformedVertex(buffer, matrix, x + width, y, 1f, 0f);
                putTransformedVertex(buffer, matrix, x, y + height, 0f, 1f);
                putTransformedVertex(buffer, matrix, x + width, y + height, 1f, 1f);
            }
        }
        buffer.flip();
        return buffer;
    }

    private static ByteBuffer writeClipQuadBatch(ClipQuadBatch batch, float width, float height) {
        int requiredBytes = batch.size() * 6 * CLIPPED_TEXTURE_VERTEX_FORMAT.getVertexSize();
        if (batchQuadCpuBuffer.capacity() < requiredBytes) {
            batchQuadCpuBuffer = ByteBuffer
                    .allocateDirect(nextPowerOfTwo(requiredBytes))
                    .order(ByteOrder.nativeOrder());
        }

        ByteBuffer buffer = batchQuadCpuBuffer;
        buffer.clear();
        Matrix4f matrix = resolveMatrixRef(null);
        boolean identity = matrix == IDENTITY_MATRIX;
        for (int i = 0; i < batch.size(); i++) {
            float x = batch.x(i);
            float y = batch.y(i);
            putClippedTextureVertex(buffer, matrix, identity, x, y, 0f, 0f, batch, i);
            putClippedTextureVertex(buffer, matrix, identity, x, y + height, 0f, 1f, batch, i);
            putClippedTextureVertex(buffer, matrix, identity, x + width, y, 1f, 0f, batch, i);
            putClippedTextureVertex(buffer, matrix, identity, x + width, y, 1f, 0f, batch, i);
            putClippedTextureVertex(buffer, matrix, identity, x, y + height, 0f, 1f, batch, i);
            putClippedTextureVertex(buffer, matrix, identity, x + width, y + height, 1f, 1f, batch, i);
        }
        buffer.flip();
        return buffer;
    }

    private static ByteBuffer writeTextureAtlasBatch(TextureAtlasBatch batch) {
        int requiredBytes = batch.vertexCount() * CLIPPED_TEXT_VERTEX_FORMAT.getVertexSize();
        if (batchQuadCpuBuffer.capacity() < requiredBytes) {
            batchQuadCpuBuffer = ByteBuffer
                    .allocateDirect(nextPowerOfTwo(requiredBytes))
                    .order(ByteOrder.nativeOrder());
        }

        ByteBuffer buffer = batchQuadCpuBuffer;
        buffer.clear();
        Matrix4f matrix = resolveMatrixRef(null);
        boolean identity = matrix == IDENTITY_MATRIX;
        for (int i = 0; i < batch.size(); i++) {
            float x = batch.x(i);
            float y = batch.y(i);
            float width = batch.width(i);
            float height = batch.height(i);
            putTextureAtlasVertex(buffer, matrix, identity, x, y, batch.u0(i), batch.v0(i), batch, i);
            putTextureAtlasVertex(buffer, matrix, identity, x, y + height, batch.u0(i), batch.v1(i), batch, i);
            putTextureAtlasVertex(buffer, matrix, identity, x + width, y, batch.u1(i), batch.v0(i), batch, i);
            putTextureAtlasVertex(buffer, matrix, identity, x + width, y, batch.u1(i), batch.v0(i), batch, i);
            putTextureAtlasVertex(buffer, matrix, identity, x, y + height, batch.u0(i), batch.v1(i), batch, i);
            putTextureAtlasVertex(buffer, matrix, identity, x + width, y + height, batch.u1(i), batch.v1(i), batch, i);
        }
        buffer.flip();
        return buffer;
    }

    private static void putTextureAtlasVertex(ByteBuffer buffer, Matrix4f matrix, boolean identity, float x, float y, float u, float v, TextureAtlasBatch batch, int index) {
        if (identity) {
            buffer.putFloat(x);
            buffer.putFloat(y);
            buffer.putFloat(0.0f);
        } else {
            buffer.putFloat(matrix.m00() * x + matrix.m10() * y + matrix.m30());
            buffer.putFloat(matrix.m01() * x + matrix.m11() * y + matrix.m31());
            buffer.putFloat(matrix.m02() * x + matrix.m12() * y + matrix.m32());
        }
        buffer.putFloat(u);
        buffer.putFloat(v);
        buffer.putFloat(batch.red(index));
        buffer.putFloat(batch.green(index));
        buffer.putFloat(batch.blue(index));
        buffer.putFloat(batch.alpha(index));
        buffer.putFloat(batch.clipX(index));
        buffer.putFloat(batch.clipY(index));
        buffer.putFloat(batch.clipWidth(index));
        buffer.putFloat(batch.clipHeight(index));
        buffer.putFloat(batch.clipRadiusX(index));
        buffer.putFloat(batch.clipRadiusY(index));
        buffer.putFloat(batch.clipRadiusZ(index));
        buffer.putFloat(batch.clipRadiusW(index));
    }

    private static void putClippedTextureVertex(ByteBuffer buffer, Matrix4f matrix, boolean identity, float x, float y, float u, float v, ClipQuadBatch batch, int index) {
        if (identity) {
            buffer.putFloat(x);
            buffer.putFloat(y);
            buffer.putFloat(0.0f);
        } else {
            buffer.putFloat(matrix.m00() * x + matrix.m10() * y + matrix.m30());
            buffer.putFloat(matrix.m01() * x + matrix.m11() * y + matrix.m31());
            buffer.putFloat(matrix.m02() * x + matrix.m12() * y + matrix.m32());
        }
        buffer.putFloat(u);
        buffer.putFloat(v);
        buffer.putFloat(batch.clipX(index));
        buffer.putFloat(batch.clipY(index));
        buffer.putFloat(batch.clipWidth(index));
        buffer.putFloat(batch.clipHeight(index));
        buffer.putFloat(batch.clipRadiusX(index));
        buffer.putFloat(batch.clipRadiusY(index));
        buffer.putFloat(batch.clipRadiusZ(index));
        buffer.putFloat(batch.clipRadiusW(index));
    }

    private static void drawPackedRoundVertices(ByteBuffer vertices, int vertexCount) {
        if (vertices == null || vertexCount <= 0 || !vertices.hasRemaining()) {
            return;
        }

        GpuBuffer uniforms = getPackedRectangleUniforms();
        drawVertexBuffer(
                vertices,
                vertexCount,
                getRectanglePackedPipeline(),
                List.of(new Ubo("PackedGlobals", uniforms))
        );
    }

    private static ByteBuffer writePackedRoundBatch(
            QuadBatch batch,
            float width,
            float height,
            Vector4f roundness,
            Color color1,
            Color color2,
            Color color3,
            Color color4,
            Color stroke1,
            Color stroke2,
            Color stroke3,
            Color stroke4,
            float thickness,
            boolean overlay
    ) {
        int requiredBytes = batch.size() * PACKED_ROUND_VERTEX_COUNT * PACKED_ROUND_VERTEX_BYTES;
        if (batchPackedRoundCpuBuffer.capacity() < requiredBytes) {
            batchPackedRoundCpuBuffer = ByteBuffer
                    .allocateDirect(nextPowerOfTwo(requiredBytes))
                    .order(ByteOrder.nativeOrder());
        }

        ByteBuffer buffer = batchPackedRoundCpuBuffer;
        buffer.clear();
        Matrix4f matrix = resolveMatrixRef(null);
        boolean identity = matrix == IDENTITY_MATRIX;
        Scissor scissor = Scissor.scissor();
        Vector4f clipRadius = scissor.getClipRadius();
        for (int i = 0; i < batch.size(); i++) {
            putPackedRoundQuad(
                    buffer,
                    matrix,
                    identity,
                    batch.x(i),
                    batch.y(i),
                    width,
                    height,
                    roundness,
                    color1,
                    color2,
                    color3,
                    color4,
                    stroke1,
                    stroke2,
                    stroke3,
                    stroke4,
                    thickness,
                    overlay,
                    scissor.getClipX(),
                    scissor.getClipY(),
                    scissor.getClipWidth(),
                    scissor.getClipHeight(),
                    clipRadius.x,
                    clipRadius.y,
                    clipRadius.z,
                    clipRadius.w
            );
        }
        buffer.flip();
        return buffer;
    }

    private static void putPackedRoundQuad(
            ByteBuffer buffer,
            Matrix4f matrix,
            boolean identity,
            float x,
            float y,
            float width,
            float height,
            Vector4f roundness,
            Color color1,
            Color color2,
            Color color3,
            Color color4,
            Color stroke1,
            Color stroke2,
            Color stroke3,
            Color stroke4,
            float thickness,
            boolean overlay,
            float clipX,
            float clipY,
            float clipWidth,
            float clipHeight,
            float clipRadiusX,
            float clipRadiusY,
            float clipRadiusZ,
            float clipRadiusW
    ) {
        float radiusX = roundness == null ? 0.0f : roundness.x;
        float radiusY = roundness == null ? 0.0f : roundness.y;
        float radiusZ = roundness == null ? 0.0f : roundness.z;
        float radiusW = roundness == null ? 0.0f : roundness.w;
        putPackedRoundVertex(buffer, matrix, identity, x, y, 0.0f, 0.0f, width, height, radiusX, radiusY, radiusZ, radiusW, color1, color2, color3, color4, stroke1, stroke2, stroke3, stroke4, thickness, overlay, clipX, clipY, clipWidth, clipHeight, clipRadiusX, clipRadiusY, clipRadiusZ, clipRadiusW);
        putPackedRoundVertex(buffer, matrix, identity, x, y + height, 0.0f, 1.0f, width, height, radiusX, radiusY, radiusZ, radiusW, color1, color2, color3, color4, stroke1, stroke2, stroke3, stroke4, thickness, overlay, clipX, clipY, clipWidth, clipHeight, clipRadiusX, clipRadiusY, clipRadiusZ, clipRadiusW);
        putPackedRoundVertex(buffer, matrix, identity, x + width, y, 1.0f, 0.0f, width, height, radiusX, radiusY, radiusZ, radiusW, color1, color2, color3, color4, stroke1, stroke2, stroke3, stroke4, thickness, overlay, clipX, clipY, clipWidth, clipHeight, clipRadiusX, clipRadiusY, clipRadiusZ, clipRadiusW);
        putPackedRoundVertex(buffer, matrix, identity, x + width, y, 1.0f, 0.0f, width, height, radiusX, radiusY, radiusZ, radiusW, color1, color2, color3, color4, stroke1, stroke2, stroke3, stroke4, thickness, overlay, clipX, clipY, clipWidth, clipHeight, clipRadiusX, clipRadiusY, clipRadiusZ, clipRadiusW);
        putPackedRoundVertex(buffer, matrix, identity, x, y + height, 0.0f, 1.0f, width, height, radiusX, radiusY, radiusZ, radiusW, color1, color2, color3, color4, stroke1, stroke2, stroke3, stroke4, thickness, overlay, clipX, clipY, clipWidth, clipHeight, clipRadiusX, clipRadiusY, clipRadiusZ, clipRadiusW);
        putPackedRoundVertex(buffer, matrix, identity, x + width, y + height, 1.0f, 1.0f, width, height, radiusX, radiusY, radiusZ, radiusW, color1, color2, color3, color4, stroke1, stroke2, stroke3, stroke4, thickness, overlay, clipX, clipY, clipWidth, clipHeight, clipRadiusX, clipRadiusY, clipRadiusZ, clipRadiusW);
    }

    private static void putPackedRoundVertex(
            ByteBuffer buffer,
            Matrix4f matrix,
            boolean identity,
            float x,
            float y,
            float u,
            float v,
            float width,
            float height,
            float radiusX,
            float radiusY,
            float radiusZ,
            float radiusW,
            Color color1,
            Color color2,
            Color color3,
            Color color4,
            Color stroke1,
            Color stroke2,
            Color stroke3,
            Color stroke4,
            float thickness,
            boolean overlay,
            float clipX,
            float clipY,
            float clipWidth,
            float clipHeight,
            float clipRadiusX,
            float clipRadiusY,
            float clipRadiusZ,
            float clipRadiusW
    ) {
        if (identity) {
            buffer.putFloat(x);
            buffer.putFloat(y);
            buffer.putFloat(0.0f);
        } else {
            buffer.putFloat(matrix.m00() * x + matrix.m10() * y + matrix.m30());
            buffer.putFloat(matrix.m01() * x + matrix.m11() * y + matrix.m31());
            buffer.putFloat(matrix.m02() * x + matrix.m12() * y + matrix.m32());
        }
        buffer.putFloat(u);
        buffer.putFloat(v);
        buffer.putFloat(width);
        buffer.putFloat(height);
        buffer.putFloat(radiusX);
        buffer.putFloat(radiusY);
        buffer.putFloat(radiusZ);
        buffer.putFloat(radiusW);
        putColor(buffer, color1);
        putColor(buffer, color2);
        putColor(buffer, color3);
        putColor(buffer, color4);
        putColor(buffer, stroke1);
        putColor(buffer, stroke2);
        putColor(buffer, stroke3);
        putColor(buffer, stroke4);
        buffer.putFloat(thickness);
        buffer.putFloat(overlay ? 1.0f : 0.0f);
        buffer.putFloat(clipX);
        buffer.putFloat(clipY);
        buffer.putFloat(clipWidth);
        buffer.putFloat(clipHeight);
        buffer.putFloat(clipRadiusX);
        buffer.putFloat(clipRadiusY);
        buffer.putFloat(clipRadiusZ);
        buffer.putFloat(clipRadiusW);
    }

    private static void putColor(ByteBuffer buffer, Color color) {
        buffer.putFloat(color.getRed() / 255.0f);
        buffer.putFloat(color.getGreen() / 255.0f);
        buffer.putFloat(color.getBlue() / 255.0f);
        buffer.putFloat(color.getAlpha() / 255.0f);
    }

    private static GpuBuffer nextShaderQuadBuffer() {
        int index = shaderQuadBufferIndex++ & (STREAM_BUFFER_COUNT - 1);
        GpuBuffer buffer = SHADER_QUAD_BUFFERS[index];
        if (buffer == null || buffer.isClosed()) {
            buffer = RenderSystem.getDevice().createBuffer(
                    () -> "pasta draw #" + index,
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                    SHADER_QUAD_VERTEX_BYTES
            );
            SHADER_QUAD_BUFFERS[index] = buffer;
        }
        return buffer;
    }

    private static GpuBuffer updateMeshVertexBuffer(ByteBuffer vertexData) {
        int bytes = vertexData.remaining();
        GpuBuffer vertexBuffer = nextMeshVertexBuffer(bytes);
        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(vertexBuffer.slice(0, bytes), vertexData);
        return vertexBuffer;
    }

    private static GpuBuffer nextMeshVertexBuffer(int requiredBytes) {
        int index = meshVertexBufferIndex++ & (STREAM_BUFFER_COUNT - 1);
        GpuBuffer buffer = MESH_VERTEX_BUFFERS[index];
        if (buffer != null && (buffer.isClosed() || buffer.size() < requiredBytes)) {
            buffer.close();
            buffer = null;
        }
        if (buffer == null) {
            int bufferSize = Math.max(1, nextPowerOfTwo(requiredBytes));
            buffer = RenderSystem.getDevice().createBuffer(
                    () -> "pasta mesh #" + index,
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                    bufferSize
            );
            MESH_VERTEX_BUFFERS[index] = buffer;
        }
        return buffer;
    }

    private static int nextPowerOfTwo(int value) {
        int result = 1;
        while (result < value) {
            result <<= 1;
        }
        return result;
    }

    private static void putRawVertex(ByteBuffer buffer, float x, float y, float u, float v) {
        buffer.putFloat(x);
        buffer.putFloat(y);
        buffer.putFloat(0.0f);
        buffer.putFloat(u);
        buffer.putFloat(v);
    }

    private static void putTransformedVertex(ByteBuffer buffer, Matrix4f matrix, float x, float y, float u, float v) {
        float transformedX = matrix.m00() * x + matrix.m10() * y + matrix.m30();
        float transformedY = matrix.m01() * x + matrix.m11() * y + matrix.m31();
        float transformedZ = matrix.m02() * x + matrix.m12() * y + matrix.m32();

        buffer.putFloat(transformedX);
        buffer.putFloat(transformedY);
        buffer.putFloat(transformedZ);
        buffer.putFloat(u);
        buffer.putFloat(v);
    }

    public static BufferBuilder _build(Matrix4f matrix4f, float x, float y, float width, float height){
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, POSITION_TEX);
        builder.addVertex(matrix4f, x, y, 0).setUv(0, 0);
        builder.addVertex(matrix4f, x, y + height, 0).setUv(0, 1);
        builder.addVertex(matrix4f,x + width, y, 0).setUv(1, 0);
        builder.addVertex(matrix4f, x + width, y + height, 0).setUv(1, 1);
        return builder;
    }

    private static Matrix4f resolveMatrix(@Nullable Matrix4f matrix4f) {
        Matrix4f transform = TRANSFORM_STACK.peek();
        if (transform == null) {
            return matrix4f == null ? new Matrix4f() : new Matrix4f(matrix4f);
        }

        return matrix4f == null ? new Matrix4f(transform) : new Matrix4f(transform).mul(matrix4f);
    }

    private static Matrix4f resolveMatrixRef(@Nullable Matrix4f matrix4f) {
        Matrix4f transform = TRANSFORM_STACK.peek();
        if (transform == null) {
            return matrix4f == null ? IDENTITY_MATRIX : matrix4f;
        }
        if (matrix4f == null) {
            return transform;
        }
        return RESOLVED_MATRIX.set(transform).mul(matrix4f);
    }

    public static void drawQuad3D(BufferBuilder builder, PoseStack.Pose pose, float half, int color, float z) {
        builder.addVertex(pose, half, -half, z).setColor(color).setUv(0f, 1f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, 0, 1);
        builder.addVertex(pose, -half, -half, z).setColor(color).setUv(1f, 1f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, 0, 1);
        builder.addVertex(pose, -half, half, z).setColor(color).setUv(1f, 0f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, 0, 1);
        builder.addVertex(pose, half, half, z).setColor(color).setUv(0f, 0f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, 0, 1);
    }

    private record CachedSampler(AbstractTexture texture, Sampler sampler) {
    }

    public static final class RoundBatch {
        private ByteBuffer vertices;
        private final Vector4f reusableRadius = new Vector4f();
        private int size;
        private int vertexCount;

        public RoundBatch(int initialCapacity) {
            int capacity = Math.max(1, initialCapacity) * PACKED_ROUND_VERTEX_COUNT * PACKED_ROUND_VERTEX_BYTES;
            vertices = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
        }

        public void clear() {
            size = 0;
            vertexCount = 0;
            vertices.clear();
        }

        public void add(float x, float y, float width, float height, float radius, Color color) {
            add(x, y, width, height, radius, radius, radius, radius, color);
        }

        public void add(float x, float y, float width, float height, Vector4f roundness, Color color) {
            add(x, y, width, height, roundness, color, color, color, color, TRANSPARENT_COLOR, TRANSPARENT_COLOR, TRANSPARENT_COLOR, TRANSPARENT_COLOR, 0.0f, true);
        }

        public void add(float x, float y, float width, float height, float radiusX, float radiusY, float radiusZ, float radiusW, Color color) {
            reusableRadius.set(radiusX, radiusY, radiusZ, radiusW);
            add(x, y, width, height, reusableRadius, color, color, color, color, TRANSPARENT_COLOR, TRANSPARENT_COLOR, TRANSPARENT_COLOR, TRANSPARENT_COLOR, 0.0f, true);
        }

        public void add(float x, float y, float width, float height, Vector4f roundness, Color color1, Color color2, Color color3, Color color4) {
            add(x, y, width, height, roundness, color1, color2, color3, color4, TRANSPARENT_COLOR, TRANSPARENT_COLOR, TRANSPARENT_COLOR, TRANSPARENT_COLOR, 0.0f, true);
        }

        public void add(float x, float y, float width, float height, Vector4f roundness, Color mainColor, Color strokeColor, float thickness, boolean overlay) {
            add(x, y, width, height, roundness, mainColor, mainColor, mainColor, mainColor, strokeColor, strokeColor, strokeColor, strokeColor, thickness, overlay);
        }

        public void add(
                float x,
                float y,
                float width,
                float height,
                Vector4f roundness,
                Color color1,
                Color color2,
                Color color3,
                Color color4,
                Color stroke1,
                Color stroke2,
                Color stroke3,
                Color stroke4,
                float thickness,
                boolean overlay
        ) {
            if (width <= 0.0f || height <= 0.0f) {
                return;
            }

            Color c1 = applyAlpha(color1);
            Color c2 = applyAlpha(color2);
            Color c3 = applyAlpha(color3);
            Color c4 = applyAlpha(color4);
            Color s1 = applyAlpha(stroke1);
            Color s2 = applyAlpha(stroke2);
            Color s3 = applyAlpha(stroke3);
            Color s4 = applyAlpha(stroke4);
            if (c1.getAlpha() <= 0 && c2.getAlpha() <= 0 && c3.getAlpha() <= 0 && c4.getAlpha() <= 0
                    && s1.getAlpha() <= 0 && s2.getAlpha() <= 0 && s3.getAlpha() <= 0 && s4.getAlpha() <= 0) {
                return;
            }

            ensure(size + 1);
            Matrix4f matrix = resolveMatrixRef(null);
            boolean identity = matrix == IDENTITY_MATRIX;
            Scissor scissor = Scissor.scissor();
            Vector4f clipRadius = scissor.getClipRadius();
            putPackedRoundQuad(
                    vertices,
                    matrix,
                    identity,
                    x,
                    y,
                    width,
                    height,
                    roundness,
                    c1,
                    c2,
                    c3,
                    c4,
                    s1,
                    s2,
                    s3,
                    s4,
                    thickness,
                    overlay,
                    scissor.getClipX(),
                    scissor.getClipY(),
                    scissor.getClipWidth(),
                    scissor.getClipHeight(),
                    clipRadius.x,
                    clipRadius.y,
                    clipRadius.z,
                    clipRadius.w
            );
            size++;
            vertexCount += PACKED_ROUND_VERTEX_COUNT;
        }

        public int size() {
            return size;
        }

        public int vertexCount() {
            return vertexCount;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        private ByteBuffer vertices() {
            ByteBuffer duplicate = vertices.duplicate().order(ByteOrder.nativeOrder());
            duplicate.position(0);
            duplicate.limit(vertexCount * PACKED_ROUND_VERTEX_BYTES);
            return duplicate;
        }

        private void ensure(int requiredSize) {
            int requiredBytes = requiredSize * PACKED_ROUND_VERTEX_COUNT * PACKED_ROUND_VERTEX_BYTES;
            if (vertices.capacity() >= requiredBytes) {
                return;
            }

            ByteBuffer next = ByteBuffer
                    .allocateDirect(nextPowerOfTwo(requiredBytes))
                    .order(ByteOrder.nativeOrder());
            vertices.flip();
            next.put(vertices);
            vertices = next;
        }
    }

    public static final class QuadBatch {
        private float[] positions;
        private int size;

        public QuadBatch(int initialCapacity) {
            positions = new float[Math.max(1, initialCapacity) * 2];
        }

        public void clear() {
            size = 0;
        }

        public void add(float x, float y) {
            ensure(size + 1);
            int index = size++ * 2;
            positions[index] = x;
            positions[index + 1] = y;
        }

        public int size() {
            return size;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        private float x(int index) {
            return positions[index * 2];
        }

        private float y(int index) {
            return positions[index * 2 + 1];
        }

        private void ensure(int requiredSize) {
            int required = requiredSize * 2;
            if (positions.length >= required) {
                return;
            }

            int capacity = positions.length;
            while (capacity < required) {
                capacity *= 2;
            }
            float[] next = new float[capacity];
            System.arraycopy(positions, 0, next, 0, size * 2);
            positions = next;
        }
    }

    public static final class ClipQuadBatch {
        private static final int STRIDE = 10;
        private float[] data;
        private int size;

        public ClipQuadBatch(int initialCapacity) {
            data = new float[Math.max(1, initialCapacity) * STRIDE];
        }

        public void clear() {
            size = 0;
        }

        public void add(float x, float y) {
            Scissor scissor = Scissor.scissor();
            Vector4f radius = scissor.getClipRadius();
            add(
                    x,
                    y,
                    scissor.getClipX(),
                    scissor.getClipY(),
                    scissor.getClipWidth(),
                    scissor.getClipHeight(),
                    radius.x,
                    radius.y,
                    radius.z,
                    radius.w
            );
        }

        public void add(float x, float y, float clipX, float clipY, float clipWidth, float clipHeight) {
            add(x, y, clipX, clipY, clipWidth, clipHeight, 0.0f, 0.0f, 0.0f, 0.0f);
        }

        public void add(
                float x,
                float y,
                float clipX,
                float clipY,
                float clipWidth,
                float clipHeight,
                float clipRadiusX,
                float clipRadiusY,
                float clipRadiusZ,
                float clipRadiusW
        ) {
            ensure(size + 1);
            int index = size++ * STRIDE;
            data[index] = x;
            data[index + 1] = y;
            data[index + 2] = clipX;
            data[index + 3] = clipY;
            data[index + 4] = clipWidth;
            data[index + 5] = clipHeight;
            data[index + 6] = clipRadiusX;
            data[index + 7] = clipRadiusY;
            data[index + 8] = clipRadiusZ;
            data[index + 9] = clipRadiusW;
        }

        public int size() {
            return size;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        private float x(int index) {
            return data[index * STRIDE];
        }

        private float y(int index) {
            return data[index * STRIDE + 1];
        }

        private float clipX(int index) {
            return data[index * STRIDE + 2];
        }

        private float clipY(int index) {
            return data[index * STRIDE + 3];
        }

        private float clipWidth(int index) {
            return data[index * STRIDE + 4];
        }

        private float clipHeight(int index) {
            return data[index * STRIDE + 5];
        }

        private float clipRadiusX(int index) {
            return data[index * STRIDE + 6];
        }

        private float clipRadiusY(int index) {
            return data[index * STRIDE + 7];
        }

        private float clipRadiusZ(int index) {
            return data[index * STRIDE + 8];
        }

        private float clipRadiusW(int index) {
            return data[index * STRIDE + 9];
        }

        private void ensure(int requiredSize) {
            int required = requiredSize * STRIDE;
            if (data.length >= required) {
                return;
            }

            int capacity = data.length;
            while (capacity < required) {
                capacity *= 2;
            }
            data = Arrays.copyOf(data, capacity);
        }
    }

    public static final class TextureAtlasBatch {
        private static final int STRIDE = 20;
        private float[] data;
        private int size;

        public TextureAtlasBatch(int initialCapacity) {
            data = new float[Math.max(1, initialCapacity) * STRIDE];
        }

        public void clear() {
            size = 0;
        }

        public void add(float x, float y, float width, float height, Vector4f uvRect, Color color) {
            if (uvRect == null || width <= 0.0f || height <= 0.0f) {
                return;
            }

            Color renderColor = applyAlpha(color);
            if (renderColor.getAlpha() <= 0) {
                return;
            }

            Scissor scissor = Scissor.scissor();
            Vector4f radius = scissor.getClipRadius();
            add(
                    x,
                    y,
                    width,
                    height,
                    uvRect.x,
                    uvRect.y,
                    uvRect.z,
                    uvRect.w,
                    renderColor.getRed() / 255.0f,
                    renderColor.getGreen() / 255.0f,
                    renderColor.getBlue() / 255.0f,
                    renderColor.getAlpha() / 255.0f,
                    scissor.getClipX(),
                    scissor.getClipY(),
                    scissor.getClipWidth(),
                    scissor.getClipHeight(),
                    radius.x,
                    radius.y,
                    radius.z,
                    radius.w
            );
        }

        public void add(
                float x,
                float y,
                float width,
                float height,
                float u0,
                float v0,
                float u1,
                float v1,
                float red,
                float green,
                float blue,
                float alpha,
                float clipX,
                float clipY,
                float clipWidth,
                float clipHeight,
                float clipRadiusX,
                float clipRadiusY,
                float clipRadiusZ,
                float clipRadiusW
        ) {
            ensure(size + 1);
            int index = size++ * STRIDE;
            data[index] = x;
            data[index + 1] = y;
            data[index + 2] = width;
            data[index + 3] = height;
            data[index + 4] = u0;
            data[index + 5] = v0;
            data[index + 6] = u1;
            data[index + 7] = v1;
            data[index + 8] = red;
            data[index + 9] = green;
            data[index + 10] = blue;
            data[index + 11] = alpha;
            data[index + 12] = clipX;
            data[index + 13] = clipY;
            data[index + 14] = clipWidth;
            data[index + 15] = clipHeight;
            data[index + 16] = clipRadiusX;
            data[index + 17] = clipRadiusY;
            data[index + 18] = clipRadiusZ;
            data[index + 19] = clipRadiusW;
        }

        public int size() {
            return size;
        }

        public int vertexCount() {
            return size * 6;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        private float value(int index, int offset) {
            return data[index * STRIDE + offset];
        }

        private float x(int index) { return value(index, 0); }
        private float y(int index) { return value(index, 1); }
        private float width(int index) { return value(index, 2); }
        private float height(int index) { return value(index, 3); }
        private float u0(int index) { return value(index, 4); }
        private float v0(int index) { return value(index, 5); }
        private float u1(int index) { return value(index, 6); }
        private float v1(int index) { return value(index, 7); }
        private float red(int index) { return value(index, 8); }
        private float green(int index) { return value(index, 9); }
        private float blue(int index) { return value(index, 10); }
        private float alpha(int index) { return value(index, 11); }
        private float clipX(int index) { return value(index, 12); }
        private float clipY(int index) { return value(index, 13); }
        private float clipWidth(int index) { return value(index, 14); }
        private float clipHeight(int index) { return value(index, 15); }
        private float clipRadiusX(int index) { return value(index, 16); }
        private float clipRadiusY(int index) { return value(index, 17); }
        private float clipRadiusZ(int index) { return value(index, 18); }
        private float clipRadiusW(int index) { return value(index, 19); }

        private void ensure(int requiredSize) {
            int required = requiredSize * STRIDE;
            if (data.length >= required) {
                return;
            }

            int capacity = data.length;
            while (capacity < required) {
                capacity *= 2;
            }
            data = Arrays.copyOf(data, capacity);
        }
    }
}
