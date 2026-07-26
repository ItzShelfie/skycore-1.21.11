package shelf.paster.render.font;

import shelf.paster.util.MinecraftInstance;
import shelf.paster.render.Scissor;
import shelf.paster.render.gpu.Sampler;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class TextUniforms implements MinecraftInstance {

    private static final int textUniformSize = new Std140SizeCalculator()
            .putVec2()
            .putFloat()
            .putFloat()
            .putVec4()
            .putVec4()
            .putVec4()
            .get();

    private static final Map<Font, GpuBuffer> staticUniforms = new HashMap<>();
    private static final Map<Font, Sampler> samplers = new HashMap<>();

    private static final Queue<GpuBuffer> dynamicPool = new java.util.ArrayDeque<>();

    public static Sampler getSampler(Font atlas) {
        return samplers.computeIfAbsent(atlas, font -> new Sampler("texSampler", font.getTextureView()));
    }

    public static void clearResourceCache() {
        samplers.clear();

        for (GpuBuffer buffer : staticUniforms.values()) {
            closeQuietly(buffer);
        }
        staticUniforms.clear();

        GpuBuffer buffer;
        while ((buffer = dynamicPool.poll()) != null) {
            closeQuietly(buffer);
        }
    }

    private static void closeQuietly(GpuBuffer buffer) {
        if (buffer == null) return;

        try {
            buffer.close();
        } catch (Exception ignored) {
        }
    }

    public static GpuBuffer getDynamicUniforms(Color color) {
        return getDynamicUniforms(color.getRGB());
    }

    public static GpuBuffer getDynamicUniforms(int argb) {
        Scissor scissor = Scissor.scissor();
        Vector4f clipRadius = scissor.getClipRadius();
        return getDynamicUniforms(
                argb,
                scissor.isClipActive(),
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

    public static GpuBuffer getDynamicUniforms(Color color, boolean clipActive, float clipX, float clipY, float clipWidth, float clipHeight, float radiusX, float radiusY, float radiusZ, float radiusW) {
        return getDynamicUniforms(color.getRGB(), clipActive, clipX, clipY, clipWidth, clipHeight, radiusX, radiusY, radiusZ, radiusW);
    }

    public static GpuBuffer getDynamicUniforms(int argb, boolean clipActive, float clipX, float clipY, float clipWidth, float clipHeight, float radiusX, float radiusY, float radiusZ, float radiusW) {
        float alpha = ((argb >>> 24) & 255) / 255f;
        float red = ((argb >>> 16) & 255) / 255f;
        float green = ((argb >>> 8) & 255) / 255f;
        float blue = (argb & 255) / 255f;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            var buffer = Std140Builder.onStack(stack, textUniformSize)
                    .putVec2(mc.getWindow().getWidth(), mc.getWindow().getHeight())
                    .putFloat(0).putFloat(0)
                    .putVec4(red, green, blue, alpha)
                    .putVec4(clipActive ? clipX : 0f, clipActive ? clipY : 0f, clipActive ? clipWidth : 0f, clipActive ? clipHeight : 0f)
                    .putVec4(clipActive ? radiusX : 0f, clipActive ? radiusY : 0f, clipActive ? radiusZ : 0f, clipActive ? radiusW : 0f)
                    .get();

            GpuBuffer ubo = dynamicPool.poll();
            if (ubo == null) {
                ubo = RenderSystem.getDevice().createBuffer(() -> "dynamicUBO_fonts_pooled", 136, textUniformSize);
            }

            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(ubo.slice(), buffer);
            return ubo;
        }
    }

    public static void freeDynamicUniforms(GpuBuffer buffer) {
        if (buffer != null) dynamicPool.offer(buffer);
    }

    private static final int staticUniformSize = new Std140SizeCalculator()
            .putFloat()
            .get();

    public static GpuBuffer getStaticUniforms(Font atlas) {
        GpuBuffer ubo;
        if ((ubo = staticUniforms.getOrDefault(atlas, null)) != null) {
            return ubo;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            var buffer = Std140Builder.onStack(stack, staticUniformSize)
                    .putFloat(atlas.getDistanceRange())
                    .get();

            ubo = RenderSystem.getDevice().createBuffer(() -> "staticUBO_font_" + Math.random(), 136, staticUniformSize);

            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(ubo.slice(), buffer);

            staticUniforms.put(atlas, ubo);

            return ubo;
        }
    }
}
