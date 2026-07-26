package shelf.paster.render.gpu;
import shelf.paster.render.font.Font;
import shelf.paster.render.font.Glyph;
import shelf.paster.render.Scissor;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.util.function.Function;

public final class Pipelines {
    public static GpuBuffer uniformBuffer;
    public static GpuBuffer textureUniformBuffer;
    public static GpuBuffer textUniformBuffer;
    public static GpuBuffer radialMenuUniformBuffer;
    public static GpuBuffer crosshairArcUniformBuffer;
    public static GpuBuffer gaussianUniformBuffer;
    public static GpuBuffer packedRectangleUniformBuffer;
    private static final VertexFormatElement PACKED_RECT_SIZE_ELEMENT = createGenericFloatElement(2);
    private static final VertexFormatElement PACKED_RECT_RADIUS_ELEMENT = createGenericFloatElement(4);
    private static final VertexFormatElement PACKED_RECT_COLOR1_ELEMENT = createGenericFloatElement(4);
    private static final VertexFormatElement PACKED_RECT_COLOR2_ELEMENT = createGenericFloatElement(4);
    private static final VertexFormatElement PACKED_RECT_COLOR3_ELEMENT = createGenericFloatElement(4);
    private static final VertexFormatElement PACKED_RECT_COLOR4_ELEMENT = createGenericFloatElement(4);
    private static final VertexFormatElement PACKED_RECT_STROKE1_ELEMENT = createGenericFloatElement(4);
    private static final VertexFormatElement PACKED_RECT_STROKE2_ELEMENT = createGenericFloatElement(4);
    private static final VertexFormatElement PACKED_RECT_STROKE3_ELEMENT = createGenericFloatElement(4);
    private static final VertexFormatElement PACKED_RECT_STROKE4_ELEMENT = createGenericFloatElement(4);
    private static final VertexFormatElement PACKED_RECT_PARAMS_ELEMENT = createGenericFloatElement(2);
    private static final VertexFormatElement PACKED_RECT_CLIP_ELEMENT = createGenericFloatElement(4);
    private static final VertexFormatElement PACKED_RECT_CLIP_RADIUS_ELEMENT = createGenericFloatElement(4);
    public static final VertexFormat PACKED_RECTANGLE_VERTEX_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.UV0)
            .add("RectSize", PACKED_RECT_SIZE_ELEMENT)
            .add("Radius", PACKED_RECT_RADIUS_ELEMENT)
            .add("Color1", PACKED_RECT_COLOR1_ELEMENT)
            .add("Color2", PACKED_RECT_COLOR2_ELEMENT)
            .add("Color3", PACKED_RECT_COLOR3_ELEMENT)
            .add("Color4", PACKED_RECT_COLOR4_ELEMENT)
            .add("Stroke1", PACKED_RECT_STROKE1_ELEMENT)
            .add("Stroke2", PACKED_RECT_STROKE2_ELEMENT)
            .add("Stroke3", PACKED_RECT_STROKE3_ELEMENT)
            .add("Stroke4", PACKED_RECT_STROKE4_ELEMENT)
            .add("RoundParams", PACKED_RECT_PARAMS_ELEMENT)
            .add("ClipRect", PACKED_RECT_CLIP_ELEMENT)
            .add("ClipRadius", PACKED_RECT_CLIP_RADIUS_ELEMENT)
            .build();
    public static final VertexFormat CLIPPED_TEXTURE_VERTEX_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.UV0)
            .add("ClipRect", PACKED_RECT_CLIP_ELEMENT)
            .add("ClipRadius", PACKED_RECT_CLIP_RADIUS_ELEMENT)
            .build();
    public static final VertexFormat CLIPPED_TEXT_VERTEX_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.UV0)
            .add("TextColor", PACKED_RECT_COLOR1_ELEMENT)
            .add("ClipRect", PACKED_RECT_CLIP_ELEMENT)
            .add("ClipRadius", PACKED_RECT_CLIP_RADIUS_ELEMENT)
            .build();
    public static int rectSize = new Std140SizeCalculator()
            .putVec2()
            .putVec2()
            .putVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .putFloat()
            .putFloat()
            .putVec4()
            .putVec4()
            .get();
    public static int textureSize = new Std140SizeCalculator()
            .putVec2()
            .putVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .putFloat()
            .putVec4()
            .putVec4()
            .get();
    public static int textUnifoSize = new Std140SizeCalculator()
            .putVec2()
            .putVec4()
            .putVec4()
            .putFloat()
            .get();
    public static int radialMenuSize = new Std140SizeCalculator()
            .putVec2()
            .putVec2()
            .putFloat()
            .putFloat()
            .putInt()
            .putInt()
            .putInt()
            .putVec4()
            .putVec4()
            .putVec4()
            .putFloat()
            .putFloat()
            .get();
    public static int crosshairArcSize = new Std140SizeCalculator()
            .putVec2()
            .putVec2()
            .putVec4()
            .putVec4()
            .putFloat()
            .putFloat()
            .putFloat()
            .putFloat()
            .get();
    public static int gaussianSize = new Std140SizeCalculator()
            .putVec2()
            .putVec2()
            .putVec2()
            .putVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .get();
    public static int packedRectangleGlobalsSize = new Std140SizeCalculator()
            .putVec2()
            .get();

    private static VertexFormatElement createGenericFloatElement(int count) {
        int id = nextVertexFormatElementId();
        if (id < 0) {
            throw new IllegalStateException("No free VertexFormatElement slots left for packed rectangle attributes");
        }
        return VertexFormatElement.register(id, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, count);
    }

    private static int nextVertexFormatElementId() {
        for (int i = 0; i < VertexFormatElement.MAX_COUNT; i++) {
            if (VertexFormatElement.byId(i) == null) {
                return i;
            }
        }
        return -1;
    }

    public static GpuBuffer getTextureUniforms(float x, float y, float width, float height, Vector4f radius, Color color, Color color2, Color color3, Color color4, float smoothness){
        return getTextureUniforms(x, y, width, height, radius, new Vector4f(0, 0, 1, 1), color, color2, color3, color4, smoothness);
    }

    public static GpuBuffer getTextureUniforms(float x, float y, float width, float height, Vector4f radius, Vector4f uvRect, Color color, Color color2, Color color3, Color color4, float smoothness){
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var window = Minecraft.getInstance().getWindow();
            Vector4f clipRadius = Scissor.scissor().getClipRadius();
            var byteBuffer = Std140Builder.onStack(
                            stack,
                            textureSize
                    )
                    .putVec2(window.getWidth(), window.getHeight())
                    .putVec4(x, y, width, height)
                    .putVec4(uvRect)
                    .putVec4(radius)
                    .putVec4(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f)
                    .putVec4(color2.getRed() / 255f, color2.getGreen() / 255f, color2.getBlue() / 255f, color2.getAlpha() / 255f)
                    .putVec4(color3.getRed() / 255f, color3.getGreen() / 255f, color3.getBlue() / 255f, color3.getAlpha() / 255f)
                    .putVec4(color4.getRed() / 255f, color4.getGreen() / 255f, color4.getBlue() / 255f, color4.getAlpha() / 255f)
                    .putFloat(smoothness)
                    .putVec4(Scissor.scissor().getClipX(), Scissor.scissor().getClipY(), Scissor.scissor().getClipWidth(), Scissor.scissor().getClipHeight())
                    .putVec4(clipRadius.x, clipRadius.y, clipRadius.z, clipRadius.w)
                    .get();
            if (textureUniformBuffer == null) {
                textureUniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Custom UBO", 136, textureSize);
            }
            if (textureUniformBuffer.size() >= byteBuffer.remaining()) {
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(textureUniformBuffer.slice(), byteBuffer);
            }
            return textureUniformBuffer;
        }
    }
    public static GpuBuffer getRectangleUniforms(float w, float h, Vector4f vector4f, Color color1, Color color2, Color color3, Color color4, Color stroke1, Color stroke2, Color stroke3, Color stroke4, float thickness, float overlay) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var window = Minecraft.getInstance().getWindow();
            Vector4f clipRadius = Scissor.scissor().getClipRadius();
            var byteBuffer = Std140Builder.onStack(
                            stack,
                            rectSize
                    )
                    .putVec2(window.getWidth(), window.getHeight())
                    .putVec2(w, h)
                    .putVec4(color1.getRed() / 255f, color1.getGreen() / 255f, color1.getBlue() / 255f, color1.getAlpha() / 255f)
                    .putVec4(color2.getRed() / 255f, color2.getGreen() / 255f, color2.getBlue() / 255f, color2.getAlpha() / 255f)
                    .putVec4(color3.getRed() / 255f, color3.getGreen() / 255f, color3.getBlue() / 255f, color3.getAlpha() / 255f)
                    .putVec4(color4.getRed() / 255f, color4.getGreen() / 255f, color4.getBlue() / 255f, color4.getAlpha() / 255f)
                    .putVec4(vector4f)
                    .putVec4(stroke1.getRed() / 255f, stroke1.getGreen() / 255f, stroke1.getBlue() / 255f, stroke1.getAlpha() / 255f)
                    .putVec4(stroke2.getRed() / 255f, stroke2.getGreen() / 255f, stroke2.getBlue() / 255f, stroke2.getAlpha() / 255f)
                    .putVec4(stroke3.getRed() / 255f, stroke3.getGreen() / 255f, stroke3.getBlue() / 255f, stroke3.getAlpha() / 255f)
                    .putVec4(stroke4.getRed() / 255f, stroke4.getGreen() / 255f, stroke4.getBlue() / 255f, stroke4.getAlpha() / 255f)
                    .putFloat(thickness)
                    .putFloat(overlay)
                    .putVec4(Scissor.scissor().getClipX(), Scissor.scissor().getClipY(), Scissor.scissor().getClipWidth(), Scissor.scissor().getClipHeight())
                    .putVec4(clipRadius.x, clipRadius.y, clipRadius.z, clipRadius.w)
                    .get();
            if (uniformBuffer == null) {
                uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Custom UBO", 136, rectSize);
            }
            if (uniformBuffer.size() >= byteBuffer.remaining()) {
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(uniformBuffer.slice(), byteBuffer);
            }
            return uniformBuffer;
        }
    }

    public static GpuBuffer getPackedRectangleUniforms() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var window = Minecraft.getInstance().getWindow();
            var byteBuffer = Std140Builder.onStack(stack, packedRectangleGlobalsSize)
                    .putVec2(window.getWidth(), window.getHeight())
                    .get();
            if (packedRectangleUniformBuffer == null) {
                packedRectangleUniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Packed Rectangle UBO", 136, packedRectangleGlobalsSize);
            }
            if (packedRectangleUniformBuffer.size() >= byteBuffer.remaining()) {
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(packedRectangleUniformBuffer.slice(), byteBuffer);
            }
            return packedRectangleUniformBuffer;
        }
    }

    public static GpuBuffer getTextUniforms(Glyph glyph, Color color, Font atlas) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var window = Minecraft.getInstance().getWindow();
            var byteBuffer = Std140Builder.onStack(stack, textUnifoSize)
                    .putVec2(window.getWidth(), window.getHeight())
                    .putVec4(glyph.getMinU(), glyph.getMinV(), glyph.getMaxU(), glyph.getMaxV())
                    .putVec4(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f)
                    .putFloat(atlas.getDistanceRange())
                    .get();
            if (textUniformBuffer == null) {
                textUniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Text UBO", 136, textUnifoSize);
            }
            if (textUniformBuffer.size() >= byteBuffer.remaining()) {
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(textUniformBuffer.slice(), byteBuffer);
            }
            return textUniformBuffer;
        }
    }

    public static GpuBuffer getRadialMenuUniforms(float centerX, float centerY, float innerRadius, float outerRadius,
                                                   int itemCount, int hoveredIndex, int selectedIndex,
                                                   Color normalColor, Color hoverColor, Color selectColor,
                                                   float gapAngle, float time) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var window = Minecraft.getInstance().getWindow();
            var byteBuffer = Std140Builder.onStack(stack, radialMenuSize)
                    .putVec2(window.getWidth(), window.getHeight())
                    .putVec2(centerX, centerY)
                    .putFloat(innerRadius)
                    .putFloat(outerRadius)
                    .putInt(itemCount)
                    .putInt(hoveredIndex)
                    .putInt(selectedIndex)
                    .putVec4(normalColor.getRed() / 255f, normalColor.getGreen() / 255f, normalColor.getBlue() / 255f, normalColor.getAlpha() / 255f)
                    .putVec4(hoverColor.getRed() / 255f, hoverColor.getGreen() / 255f, hoverColor.getBlue() / 255f, hoverColor.getAlpha() / 255f)
                    .putVec4(selectColor.getRed() / 255f, selectColor.getGreen() / 255f, selectColor.getBlue() / 255f, selectColor.getAlpha() / 255f)
                    .putFloat(gapAngle)
                    .putFloat(time)
                    .get();
            if (radialMenuUniformBuffer == null) {
                radialMenuUniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Radial Menu UBO", 136, radialMenuSize);
            }
            if (radialMenuUniformBuffer.size() >= byteBuffer.remaining()) {
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(radialMenuUniformBuffer.slice(), byteBuffer);
            }
            return radialMenuUniformBuffer;
        }
    }

    public static GpuBuffer getCrosshairArcUniforms(
            float x, float y, float width, float height,
            float innerRadius, float outerRadius,
            float startAngle, float endAngle,
            Color color1, Color color2
    ) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var window = Minecraft.getInstance().getWindow();
            var buf = Std140Builder.onStack(stack, crosshairArcSize)
                    .putVec2(window.getWidth(), window.getHeight())
                    .putVec2(width, height)
                    .putVec4(color1.getRed() / 255f, color1.getGreen() / 255f, color1.getBlue() / 255f, color1.getAlpha() / 255f)
                    .putVec4(color2.getRed() / 255f, color2.getGreen() / 255f, color2.getBlue() / 255f, color2.getAlpha() / 255f)
                    .putFloat(innerRadius)
                    .putFloat(outerRadius)
                    .putFloat(startAngle)
                    .putFloat(endAngle)
                    .get();
            if (crosshairArcUniformBuffer == null)
                crosshairArcUniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Crosshair Arc UBO", 136, crosshairArcSize);
            if (crosshairArcUniformBuffer.size() >= buf.remaining())
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(crosshairArcUniformBuffer.slice(), buf);
            return crosshairArcUniformBuffer;
        }
    }

    public static GpuBuffer getGaussianUniforms(float width, float height, float blurRadius, Vector4f radius, Color color) {
        return getGaussianUniforms(width, height, blurRadius, radius, color, 0);
    }

    public static GpuBuffer getGaussianUniforms(float width, float height, float blurRadius, Vector4f radius, Color color, int layer) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var window = Minecraft.getInstance().getWindow();
            Vector4f clipRadius = Scissor.scissor().getClipRadius();
            float safeBlur = Math.max(0.0f, blurRadius);
            boolean background = layer == 1;
            var byteBuffer = Std140Builder.onStack(stack, gaussianSize)
                    .putVec2(window.getWidth(), window.getHeight())
                    .putVec2(background ? width : width + safeBlur * 2.0f, background ? height : height + safeBlur * 2.0f)
                    .putVec2(width, height)
                    .putVec4(radius)
                    .putVec4(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f)
                    .putVec4(safeBlur, layer, 0.0f, 0.0f)
                    .putVec4(Scissor.scissor().getClipX(), Scissor.scissor().getClipY(), Scissor.scissor().getClipWidth(), Scissor.scissor().getClipHeight())
                    .putVec4(clipRadius.x, clipRadius.y, clipRadius.z, clipRadius.w)
                    .get();
            if (gaussianUniformBuffer == null) {
                gaussianUniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Gaussian UBO", 136, gaussianSize);
            }
            if (gaussianUniformBuffer.size() >= byteBuffer.remaining()) {
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gaussianUniformBuffer.slice(), byteBuffer);
            }
            return gaussianUniformBuffer;
        }
    }

    @ApiStatus.Internal
    public static RenderPipeline RECTANGLE_PIPELINE;
    @ApiStatus.Internal
    public static RenderPipeline RECTANGLE_TRIANGLES_PIPELINE;
    @ApiStatus.Internal
    public static RenderPipeline RECTANGLE_PACKED_PIPELINE;
    @ApiStatus.Internal
    public static RenderPipeline TEXTURE_PIPELINE;
    @ApiStatus.Internal
    public static RenderPipeline TEXTURE_TRIANGLES_PIPELINE;
    @ApiStatus.Internal
    public static RenderPipeline TEXTURE_CLIPPED_TRIANGLES_PIPELINE;
    @ApiStatus.Internal
    public static RenderPipeline TEXTURE_ATLAS_CLIPPED_PIPELINE;
    @ApiStatus.Internal
    public static RenderPipeline TEXT_PIPELINE;
    @ApiStatus.Internal
    public static RenderPipeline TEXT_CLIPPED_PIPELINE;
    @ApiStatus.Internal
    public static RenderPipeline LINES_NO_DEPTH_PIPELINE;
    @ApiStatus.Internal
    public static RenderPipeline LINES_WITH_DEPTH_PIPELINE;
    @ApiStatus.Internal
    public static RenderPipeline LIGHT_LINES_NO_DEPTH_PIPELINE;
    @ApiStatus.Internal
    public static RenderPipeline LIGHT_LINES_WITH_DEPTH_PIPELINE;
    @ApiStatus.Internal
    public static RenderPipeline RADIAL_MENU_PIPELINE;

    public static Function<Boolean, RenderPipeline> PIPELAIN;
    @ApiStatus.Internal
    public static RenderPipeline COLORED_QUADS_NO_DEPTH_PIPELINE;
    @ApiStatus.Internal
    public static RenderPipeline LIGHT_COLORED_QUADS_NO_DEPTH_PIPELINE;
    @ApiStatus.Internal
    public static RenderPipeline LIGHT_COLORED_QUADS_WITH_DEPTH_PIPELINE;
    @ApiStatus.Internal
    public static RenderPipeline CROSSHAIR_ARC_PIPELINE;
    @ApiStatus.Internal
    public static RenderPipeline GAUSSIAN_PIPELINE;
    @ApiStatus.Internal
    public static RenderPipeline GAUSSIAN_TRIANGLES_PIPELINE;
    @ApiStatus.Internal
    public static RenderPipeline GAUSSIAN_BACKGROUND_PIPELINE;

    public static void init() {
        RECTANGLE_PIPELINE = Shaders.pasta("rectangle").strip().uniform("params").get();
        RECTANGLE_TRIANGLES_PIPELINE = Shaders.pasta("rectangle").id("rectangle_triangles").tris().uniform("params").get();
        RECTANGLE_PACKED_PIPELINE = Shaders.pasta("rectangle_packed").format(PACKED_RECTANGLE_VERTEX_FORMAT).tris().uniform("PackedGlobals").get();
        TEXTURE_PIPELINE = Shaders.pasta("texture").strip().uniform("params").sampler("texSampler").get();
        TEXTURE_TRIANGLES_PIPELINE = Shaders.pasta("texture").id("texture_triangles").tris().uniform("params").sampler("texSampler").get();
        TEXTURE_CLIPPED_TRIANGLES_PIPELINE = Shaders.pasta("texture_clipped").id("texture_clipped_triangles").format(CLIPPED_TEXTURE_VERTEX_FORMAT).tris().uniform("params").sampler("texSampler").get();
        TEXTURE_ATLAS_CLIPPED_PIPELINE = Shaders.pasta("texture_atlas_clipped").format(CLIPPED_TEXT_VERTEX_FORMAT).tris().uniform("PackedGlobals").sampler("texSampler").get();
        TEXT_PIPELINE = Shaders.pasta("text").id("text_optimized").tris().uniform("dynamicUniforms", "staticUniforms").sampler("texSampler").get();
        TEXT_CLIPPED_PIPELINE = Shaders.pasta("text_clipped").format(CLIPPED_TEXT_VERTEX_FORMAT).tris().uniform("dynamicUniforms", "staticUniforms").sampler("texSampler").get();

        LINES_NO_DEPTH_PIPELINE = Shaders.mc("rendertype_lines").id("pipeline/lines_nodepth").format(DefaultVertexFormat.POSITION_COLOR_NORMAL).lines()
                .uniform("DynamicTransforms", "Projection", "Fog", "Globals").get();
        LINES_WITH_DEPTH_PIPELINE = Shaders.mc("rendertype_lines").id("pipeline/lines_depth").format(DefaultVertexFormat.POSITION_COLOR_NORMAL).lines()
                .uniform("DynamicTransforms", "Projection", "Fog", "Globals").depth(DepthTestFunction.LEQUAL_DEPTH_TEST).get();
        LIGHT_LINES_NO_DEPTH_PIPELINE = Shaders.mc("rendertype_lines").id("pipeline/light_lines_nodepth").format(DefaultVertexFormat.POSITION_COLOR_NORMAL).lines()
                .blend(BlendFunction.ADDITIVE).uniform("DynamicTransforms", "Projection", "Fog", "Globals").get();
        LIGHT_LINES_WITH_DEPTH_PIPELINE = Shaders.mc("rendertype_lines").id("pipeline/light_lines_depth").format(DefaultVertexFormat.POSITION_COLOR_NORMAL).lines()
                .blend(BlendFunction.ADDITIVE).uniform("DynamicTransforms", "Projection", "Fog", "Globals").depth(DepthTestFunction.LEQUAL_DEPTH_TEST).get();

        PIPELAIN = Util.memoize(depth -> Shaders.mc("position_tex_color").id("pipeline/particle_additive")
                .format(DefaultVertexFormat.POSITION_TEX_COLOR).quads()
                .blend(BlendFunction.LIGHTNING).sampler("Sampler0")
                .uniform("DynamicTransforms", "Projection", "Fog", "Globals")
                .depth(depth ? DepthTestFunction.LEQUAL_DEPTH_TEST : DepthTestFunction.NO_DEPTH_TEST)
                .get());

        COLORED_QUADS_NO_DEPTH_PIPELINE = Shaders.mc("position_color").id("pipeline/colored_quads_nodepth")
                .format(DefaultVertexFormat.POSITION_COLOR).quads()
                .uniform("DynamicTransforms", "Projection", "Fog", "Globals").get();
        LIGHT_COLORED_QUADS_NO_DEPTH_PIPELINE = Shaders.mc("position_color").id("pipeline/light_colored_quads_nodepth")
                .format(DefaultVertexFormat.POSITION_COLOR).quads().blend(BlendFunction.ADDITIVE)
                .uniform("DynamicTransforms", "Projection", "Fog", "Globals").get();
        LIGHT_COLORED_QUADS_WITH_DEPTH_PIPELINE = Shaders.mc("position_color").id("pipeline/light_colored_quads_depth")
                .format(DefaultVertexFormat.POSITION_COLOR).quads().blend(BlendFunction.ADDITIVE)
                .uniform("DynamicTransforms", "Projection", "Fog", "Globals").depth(DepthTestFunction.LEQUAL_DEPTH_TEST).get();

        GAUSSIAN_PIPELINE = Shaders.pasta("gaussian").strip().uniform("params").get();
        GAUSSIAN_TRIANGLES_PIPELINE = Shaders.pasta("gaussian").id("gaussian_triangles").tris().uniform("params").get();
        GAUSSIAN_BACKGROUND_PIPELINE = Shaders.pasta("gaussian").fragment("gaussian_background").id("gaussian_background")
                .strip().uniform("params").sampler("Sampler0").get();
    }
    public static RenderPipeline getTexturePipeLine(){
        if(TEXTURE_PIPELINE == null){
            init();
        }
        return TEXTURE_PIPELINE;
    }
    public static RenderPipeline getRectanglePipeline(){
        if(RECTANGLE_PIPELINE == null){
            init();
        }
        return RECTANGLE_PIPELINE;
    }
    public static RenderPipeline getRectangleTrianglesPipeline() {
        if (RECTANGLE_TRIANGLES_PIPELINE == null) {
            init();
        }
        return RECTANGLE_TRIANGLES_PIPELINE;
    }
    public static RenderPipeline getRectanglePackedPipeline() {
        if (RECTANGLE_PACKED_PIPELINE == null) {
            init();
        }
        return RECTANGLE_PACKED_PIPELINE;
    }
    public static RenderPipeline getTextureTrianglesPipeline() {
        if (TEXTURE_TRIANGLES_PIPELINE == null) {
            init();
        }
        return TEXTURE_TRIANGLES_PIPELINE;
    }
    public static RenderPipeline getTextureClippedTrianglesPipeline() {
        if (TEXTURE_CLIPPED_TRIANGLES_PIPELINE == null) {
            init();
        }
        return TEXTURE_CLIPPED_TRIANGLES_PIPELINE;
    }
    public static RenderPipeline getTextureAtlasClippedPipeline() {
        if (TEXTURE_ATLAS_CLIPPED_PIPELINE == null) {
            init();
        }
        return TEXTURE_ATLAS_CLIPPED_PIPELINE;
    }
    public static RenderPipeline getTextPipeline() {
        if (TEXT_PIPELINE == null) {
            init();
        }
        return TEXT_PIPELINE;
    }
    public static RenderPipeline getTextClippedPipeline() {
        if (TEXT_CLIPPED_PIPELINE == null) {
            init();
        }
        return TEXT_CLIPPED_PIPELINE;
    }

    public static RenderPipeline getLinesPipeline(boolean throughWalls) {
        if (throughWalls) {
            if (LINES_NO_DEPTH_PIPELINE == null) {
                init();
            }
            return LINES_NO_DEPTH_PIPELINE;
        } else {
            if (LINES_WITH_DEPTH_PIPELINE == null) {
                init();
            }
            return LINES_WITH_DEPTH_PIPELINE;
        }
    }

    public static RenderPipeline getLightLinesPipeline(boolean throughWalls) {
        if (throughWalls) {
            if (LIGHT_LINES_NO_DEPTH_PIPELINE == null) {
                init();
            }
            return LIGHT_LINES_NO_DEPTH_PIPELINE;
        } else {
            if (LIGHT_LINES_WITH_DEPTH_PIPELINE == null) {
                init();
            }
            return LIGHT_LINES_WITH_DEPTH_PIPELINE;
        }
    }

    public static RenderPipeline getRadialMenuPipeline() {
        if (RADIAL_MENU_PIPELINE == null) {
            init();
        }
        return RADIAL_MENU_PIPELINE;
    }
    public static RenderPipeline getColoredQuadsPipeline() {
        if (COLORED_QUADS_NO_DEPTH_PIPELINE == null) init();
        return COLORED_QUADS_NO_DEPTH_PIPELINE;
    }
    public static RenderPipeline getLightColoredQuadsPipeline() {
        if (LIGHT_COLORED_QUADS_NO_DEPTH_PIPELINE == null) init();
        return LIGHT_COLORED_QUADS_NO_DEPTH_PIPELINE;
    }
    public static RenderPipeline getLightColoredQuadsPipeline(boolean throughWalls) {
        if (throughWalls) {
            if (LIGHT_COLORED_QUADS_NO_DEPTH_PIPELINE == null) init();
            return LIGHT_COLORED_QUADS_NO_DEPTH_PIPELINE;
        } else {
            if (LIGHT_COLORED_QUADS_WITH_DEPTH_PIPELINE == null) init();
            return LIGHT_COLORED_QUADS_WITH_DEPTH_PIPELINE;
        }
    }
    public static RenderPipeline getCrosshairArcPipeline() {
        if (CROSSHAIR_ARC_PIPELINE == null) init();
        return CROSSHAIR_ARC_PIPELINE;
    }
    public static RenderPipeline getGaussianPipeline() {
        if (GAUSSIAN_PIPELINE == null) init();
        return GAUSSIAN_PIPELINE;
    }
    public static RenderPipeline getGaussianTrianglesPipeline() {
        if (GAUSSIAN_TRIANGLES_PIPELINE == null) init();
        return GAUSSIAN_TRIANGLES_PIPELINE;
    }
    public static RenderPipeline getGaussianBackgroundPipeline() {
        if (GAUSSIAN_BACKGROUND_PIPELINE == null) init();
        return GAUSSIAN_BACKGROUND_PIPELINE;
    }
}
