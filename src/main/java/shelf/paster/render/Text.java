package shelf.paster.render;

import shelf.paster.render.gpu.Pipelines;
import shelf.paster.render.gpu.Ubo;
import shelf.paster.render.gpu.Sampler;
import shelf.paster.render.font.*;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.Color;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Text {
    private static final int LAYOUT_FLOATS_PER_VERTEX = 4;
    private static final int TEXT_VERTEX_BYTES = DefaultVertexFormat.POSITION_TEX.getVertexSize();
    private static final int CLIPPED_TEXT_FLOATS_PER_VERTEX = 17;
    private static final int CLIPPED_TEXT_VERTEX_BYTES = Pipelines.CLIPPED_TEXT_VERTEX_FORMAT.getVertexSize();
    private static final int MAX_LAYOUT_CACHE_SIZE = 2048;
    private static final Map<TextBatchKey, TextBatch> TEXT_BATCHES = new LinkedHashMap<>(16);
    private static final Map<LayoutKey, TextLayout> LAYOUT_CACHE = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<LayoutKey, TextLayout> eldest) {
            return size() > MAX_LAYOUT_CACHE_SIZE;
        }
    };
    private static java.nio.ByteBuffer textVertexBuffer = java.nio.ByteBuffer
            .allocateDirect(8192)
            .order(java.nio.ByteOrder.nativeOrder());
    private static boolean batchMode;

    private float x, y;
    private Font font;
    private String text;
    private float size = 16;
    private int color = Color.WHITE.getRGB();
    private TextAlign align = TextAlign.LEFT;
    private Matrix4f matrix;

    public Text(float x, float y, Font font, String text) {
        this.x = x;
        this.y = y;
        this.font = font;
        this.text = text;
    }

    public Text size(float size) {
        this.size = size;
        return this;
    }

    public Text color(Color color) {
        this.color = color == null ? Color.WHITE.getRGB() : color.getRGB();
        return this;
    }

    public Text color(int color) {
        this.color = color;
        return this;
    }

    public Text align(TextAlign align) {
        this.align = align;
        return this;
    }

    public Text matrix(Matrix4f matrix) {
        this.matrix = matrix;
        return this;
    }

    /** Italic shear leaning right (positive amount). */
    public Text italic(float amount) {
        float shear = Math.abs(amount);
        // Y grows downward: negate so tops shift right (/).
        Matrix4f matrix = new Matrix4f().identity();
        matrix.m10(-shear);
        matrix.m30(shear * this.y);
        this.matrix = matrix;
        return this;
    }

    public Text italic() {
        return italic(0.28f);
    }

    public static void beginBatch() {
        flushBatch();
        batchMode = true;
    }

    public static void flushBatch() {
        if (TEXT_BATCHES.isEmpty()) {
            batchMode = false;
            return;
        }

        try {
            for (TextBatch batch : TEXT_BATCHES.values()) {
                batch.draw();
            }
        } finally {
            TEXT_BATCHES.clear();
            batchMode = false;
        }
    }

    public static void clearCaches() {
        TEXT_BATCHES.clear();
        LAYOUT_CACHE.clear();
        batchMode = false;
    }

    public void render() {
        if (font == null || text == null || text.isEmpty()) return;

        TextLayout layout = getLayout(font, text, size, align);
        if (layout.vertexCount == 0) return;

        try {
            int renderColor = Draw.applyAlpha(color);
            if (((renderColor >>> 24) & 255) <= 0) return;

            if (batchMode) {
                appendBatch(layout, x, y, matrix, renderColor);
                return;
            }

            java.nio.ByteBuffer vertices = writeVertices(layout, x, y, matrix);
            GpuBuffer dynBuf = TextUniforms.getDynamicUniforms(renderColor);
            var colorUniform = new Ubo("dynamicUniforms", dynBuf);
            
            GpuBuffer staticBuf = TextUniforms.getStaticUniforms(font);
            var staticUniform = new Ubo("staticUniforms", staticBuf);
            
            try {
                Draw.drawVertexBuffer(
                        vertices,
                        layout.vertexCount,
                        Pipelines.getTextPipeline(),
                        List.of(staticUniform, colorUniform),
                        TextUniforms.getSampler(font)
                );
            } finally {
                TextUniforms.freeDynamicUniforms(dynBuf);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void appendBatch(TextLayout layout, float x, float y, Matrix4f explicitMatrix, int color) {
        TextBatchKey key = new TextBatchKey(font);
        TextBatch batch = TEXT_BATCHES.computeIfAbsent(key, TextBatch::new);
        batch.append(layout, x, y, explicitMatrix, color);
    }

    private static TextLayout getLayout(Font font, String text, float size, TextAlign align) {
        LayoutKey key = new LayoutKey(font, text, Float.floatToIntBits(size), align);
        TextLayout cached = LAYOUT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        TextLayout layout = buildLayout(font, text, size, align);
        LAYOUT_CACHE.put(key, layout);
        return layout;
    }

    private static TextLayout buildLayout(Font font, String text, float size, TextAlign align) {
        float scale = size / font.getMetrics().getEmSize();
        float lineHeight = font.getMetrics().getLineHeight() * scale;
        float ascender = font.getMetrics().getAscender() * scale;
        float currentY = 0.0f;
        float[] vertices = new float[Math.max(1, text.length() * 6 * LAYOUT_FLOATS_PER_VERTEX)];
        int offset = 0;

        List<String> lines = Arrays.asList(text.split("\n"));
        for (String line : lines) {
            float lineWidth = font.getWidth(line, size);
            float lineStartX = switch (align) {
                case CENTER -> -lineWidth / 2.0f;
                case RIGHT -> -lineWidth;
                default -> 0.0f;
            };

            offset = buildLine(font, vertices, offset, line, lineStartX, currentY + ascender, scale);
            currentY += lineHeight;
        }

        return new TextLayout(Arrays.copyOf(vertices, offset), offset / LAYOUT_FLOATS_PER_VERTEX);
    }

    private static int buildLine(Font font, float[] vertices, int offset, String text, float x, float y, float scale) {
        float currentX = x;
        int prevCharCode = -1;

        for (int i = 0; i < text.length(); i++) {
            char charCode = text.charAt(i);
            Glyph glyph = font.getGlyph(charCode);
            if (glyph == null) continue;

            if (prevCharCode != -1) {
                currentX += font.getKerning(prevCharCode, charCode, scale);
            }

            float glyphX = currentX + (glyph.getPlaneLeft() * scale);
            float glyphY = y - (glyph.getPlaneTop() * scale);
            float glyphW = glyph.getWidth() * scale;
            float glyphH = glyph.getHeight() * scale;

            if (glyphW > 0 && glyphH > 0) {
                float minU = glyph.getMinU();
                float maxU = glyph.getMaxU();
                float minV = glyph.getMinV();
                float maxV = glyph.getMaxV();

                offset = putLayoutVertex(vertices, offset, glyphX, glyphY, minU, minV);
                offset = putLayoutVertex(vertices, offset, glyphX, glyphY + glyphH, minU, maxV);
                offset = putLayoutVertex(vertices, offset, glyphX + glyphW, glyphY, maxU, minV);
                offset = putLayoutVertex(vertices, offset, glyphX, glyphY + glyphH, minU, maxV);
                offset = putLayoutVertex(vertices, offset, glyphX + glyphW, glyphY + glyphH, maxU, maxV);
                offset = putLayoutVertex(vertices, offset, glyphX + glyphW, glyphY, maxU, minV);
            }

            currentX += glyph.getAdvance() * scale;
            prevCharCode = charCode;
        }
        return offset;
    }

    private static int putLayoutVertex(float[] vertices, int offset, float x, float y, float u, float v) {
        vertices[offset++] = x;
        vertices[offset++] = y;
        vertices[offset++] = u;
        vertices[offset++] = v;
        return offset;
    }

    private static java.nio.ByteBuffer writeVertices(TextLayout layout, float x, float y, Matrix4f explicitMatrix) {
        int requiredBytes = layout.vertexCount * TEXT_VERTEX_BYTES;
        if (textVertexBuffer.capacity() < requiredBytes) {
            textVertexBuffer = java.nio.ByteBuffer
                    .allocateDirect(nextPowerOfTwo(requiredBytes))
                    .order(java.nio.ByteOrder.nativeOrder());
        }

        java.nio.ByteBuffer buffer = textVertexBuffer;
        buffer.clear();
        Matrix4f resolvedMatrix = explicitMatrix != null ? explicitMatrix : Draw.getCurrentTransform();
        float[] vertices = layout.vertices;

        if (resolvedMatrix == null) {
            for (int i = 0; i < vertices.length; i += LAYOUT_FLOATS_PER_VERTEX) {
                putRawVertex(buffer, x + vertices[i], y + vertices[i + 1], vertices[i + 2], vertices[i + 3]);
            }
        } else {
            for (int i = 0; i < vertices.length; i += LAYOUT_FLOATS_PER_VERTEX) {
                putTransformedVertex(buffer, resolvedMatrix, x + vertices[i], y + vertices[i + 1], vertices[i + 2], vertices[i + 3]);
            }
        }

        buffer.flip();
        return buffer;
    }

    private static void putRawVertex(java.nio.ByteBuffer buffer, float x, float y, float u, float v) {
        buffer.putFloat(x);
        buffer.putFloat(y);
        buffer.putFloat(0.0f);
        buffer.putFloat(u);
        buffer.putFloat(v);
    }

    private static void putTransformedVertex(java.nio.ByteBuffer buffer, Matrix4f matrix, float x, float y, float u, float v) {
        float transformedX = matrix.m00() * x + matrix.m10() * y + matrix.m30();
        float transformedY = matrix.m01() * x + matrix.m11() * y + matrix.m31();
        float transformedZ = matrix.m02() * x + matrix.m12() * y + matrix.m32();

        buffer.putFloat(transformedX);
        buffer.putFloat(transformedY);
        buffer.putFloat(transformedZ);
        buffer.putFloat(u);
        buffer.putFloat(v);
    }

    private static int nextPowerOfTwo(int value) {
        int result = 1;
        while (result < value) {
            result <<= 1;
        }
        return result;
    }

    private record LayoutKey(Font font, String text, int sizeBits, TextAlign align) {
    }

    private record TextLayout(float[] vertices, int vertexCount) {
    }

    private record TextBatchKey(Font font) {
    }

    private static final class TextBatch {
        private final TextBatchKey key;
        private float[] data = new float[2048];
        private int size;
        private int vertexCount;

        private TextBatch(TextBatchKey key) {
            this.key = key;
        }

        private void append(TextLayout layout, float x, float y, Matrix4f explicitMatrix, int color) {
            ensure(size + layout.vertices.length / LAYOUT_FLOATS_PER_VERTEX * CLIPPED_TEXT_FLOATS_PER_VERTEX);
            Matrix4f resolvedMatrix = explicitMatrix != null ? explicitMatrix : Draw.getCurrentTransform();
            float[] vertices = layout.vertices;
            float red = ((color >>> 16) & 255) / 255.0f;
            float green = ((color >>> 8) & 255) / 255.0f;
            float blue = (color & 255) / 255.0f;
            float alpha = ((color >>> 24) & 255) / 255.0f;
            Scissor scissor = Scissor.scissor();
            Vector4f radius = scissor.getClipRadius();
            boolean clipActive = scissor.isClipActive();
            float clipX = clipActive ? scissor.getClipX() : 0.0f;
            float clipY = clipActive ? scissor.getClipY() : 0.0f;
            float clipWidth = clipActive ? scissor.getClipWidth() : 0.0f;
            float clipHeight = clipActive ? scissor.getClipHeight() : 0.0f;
            float radiusX = clipActive ? radius.x : 0.0f;
            float radiusY = clipActive ? radius.y : 0.0f;
            float radiusZ = clipActive ? radius.z : 0.0f;
            float radiusW = clipActive ? radius.w : 0.0f;

            if (resolvedMatrix == null) {
                for (int i = 0; i < vertices.length; i += LAYOUT_FLOATS_PER_VERTEX) {
                    putRaw(x + vertices[i], y + vertices[i + 1], vertices[i + 2], vertices[i + 3], red, green, blue, alpha, clipX, clipY, clipWidth, clipHeight, radiusX, radiusY, radiusZ, radiusW);
                }
            } else {
                for (int i = 0; i < vertices.length; i += LAYOUT_FLOATS_PER_VERTEX) {
                    putTransformed(resolvedMatrix, x + vertices[i], y + vertices[i + 1], vertices[i + 2], vertices[i + 3], red, green, blue, alpha, clipX, clipY, clipWidth, clipHeight, radiusX, radiusY, radiusZ, radiusW);
                }
            }
        }

        private void putRaw(float x, float y, float u, float v, float red, float green, float blue, float alpha, float clipX, float clipY, float clipWidth, float clipHeight, float radiusX, float radiusY, float radiusZ, float radiusW) {
            data[size++] = x;
            data[size++] = y;
            data[size++] = 0.0f;
            data[size++] = u;
            data[size++] = v;
            putShared(red, green, blue, alpha, clipX, clipY, clipWidth, clipHeight, radiusX, radiusY, radiusZ, radiusW);
            vertexCount++;
        }

        private void putTransformed(Matrix4f matrix, float x, float y, float u, float v, float red, float green, float blue, float alpha, float clipX, float clipY, float clipWidth, float clipHeight, float radiusX, float radiusY, float radiusZ, float radiusW) {
            data[size++] = matrix.m00() * x + matrix.m10() * y + matrix.m30();
            data[size++] = matrix.m01() * x + matrix.m11() * y + matrix.m31();
            data[size++] = matrix.m02() * x + matrix.m12() * y + matrix.m32();
            data[size++] = u;
            data[size++] = v;
            putShared(red, green, blue, alpha, clipX, clipY, clipWidth, clipHeight, radiusX, radiusY, radiusZ, radiusW);
            vertexCount++;
        }

        private void putShared(float red, float green, float blue, float alpha, float clipX, float clipY, float clipWidth, float clipHeight, float radiusX, float radiusY, float radiusZ, float radiusW) {
            data[size++] = red;
            data[size++] = green;
            data[size++] = blue;
            data[size++] = alpha;
            data[size++] = clipX;
            data[size++] = clipY;
            data[size++] = clipWidth;
            data[size++] = clipHeight;
            data[size++] = radiusX;
            data[size++] = radiusY;
            data[size++] = radiusZ;
            data[size++] = radiusW;
        }

        private void ensure(int required) {
            if (data.length >= required) {
                return;
            }

            int capacity = data.length;
            while (capacity < required) {
                capacity *= 2;
            }
            data = Arrays.copyOf(data, capacity);
        }

        private void draw() {
            if (vertexCount <= 0) {
                return;
            }

            int requiredBytes = vertexCount * CLIPPED_TEXT_VERTEX_BYTES;
            if (textVertexBuffer.capacity() < requiredBytes) {
                textVertexBuffer = java.nio.ByteBuffer
                        .allocateDirect(nextPowerOfTwo(requiredBytes))
                        .order(java.nio.ByteOrder.nativeOrder());
            }

            java.nio.ByteBuffer buffer = textVertexBuffer;
            buffer.clear();
            for (int i = 0; i < size; i++) {
                buffer.putFloat(data[i]);
            }
            buffer.flip();

            GpuBuffer dynBuf = TextUniforms.getDynamicUniforms(
                    0xFFFFFFFF,
                    false,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f
            );
            var colorUniform = new Ubo("dynamicUniforms", dynBuf);
            GpuBuffer staticBuf = TextUniforms.getStaticUniforms(key.font);
            var staticUniform = new Ubo("staticUniforms", staticBuf);

            try {
                Draw.drawVertexBuffer(
                        buffer,
                        vertexCount,
                        Pipelines.getTextClippedPipeline(),
                        List.of(staticUniform, colorUniform),
                        TextUniforms.getSampler(key.font)
                );
            } finally {
                TextUniforms.freeDynamicUniforms(dynBuf);
            }
        }
    }
}
