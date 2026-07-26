package shelf.paster.render.gpu;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * One-line pasta pipeline registration.
 * <pre>
 * Shaders.pasta("rectangle").strip().uniform("params").get();
 * Shaders.pasta("gaussian").fragment("gaussian_background").sampler("Sampler0").uniform("params").get();
 * </pre>
 */
public final class Shaders {
    private Shaders() {
    }

    public static Builder pasta(String shader) {
        return new Builder("pasta", shader, shader, "pipeline/2d/" + shader);
    }

    public static Builder mc(String shader) {
        return new Builder("minecraft", shader, shader, "pipeline/" + shader.replace('/', '_'));
    }

    public static final class Builder {
        private final String namespace;
        private final String vertex;
        private String fragment;
        private String locationPath;
        private VertexFormat format = DefaultVertexFormat.POSITION_TEX;
        private VertexFormat.Mode mode = VertexFormat.Mode.TRIANGLE_STRIP;
        private BlendFunction blend = BlendFunction.TRANSLUCENT;
        private DepthTestFunction depthTest = DepthTestFunction.NO_DEPTH_TEST;
        private boolean depthWrite;
        private boolean cull;
        private String[] uniforms = new String[0];
        private String[] samplers = new String[0];

        private Builder(String namespace, String vertex, String fragment, String locationPath) {
            this.namespace = namespace;
            this.vertex = vertex;
            this.fragment = fragment;
            this.locationPath = locationPath;
        }

        public Builder id(String pipelineId) {
            this.locationPath = pipelineId.contains("/") ? pipelineId : "pipeline/2d/" + pipelineId;
            return this;
        }

        public Builder fragment(String fragmentShader) {
            this.fragment = fragmentShader;
            return this;
        }

        public Builder format(VertexFormat format) {
            this.format = format;
            return this;
        }

        public Builder strip() {
            this.mode = VertexFormat.Mode.TRIANGLE_STRIP;
            return this;
        }

        public Builder tris() {
            this.mode = VertexFormat.Mode.TRIANGLES;
            return this;
        }

        public Builder quads() {
            this.mode = VertexFormat.Mode.QUADS;
            return this;
        }

        public Builder lines() {
            this.mode = VertexFormat.Mode.LINES;
            return this;
        }

        public Builder blend(BlendFunction blend) {
            this.blend = blend;
            return this;
        }

        public Builder depth(DepthTestFunction depthTest) {
            this.depthTest = depthTest;
            return this;
        }

        public Builder depthWrite(boolean depthWrite) {
            this.depthWrite = depthWrite;
            return this;
        }

        public Builder cull(boolean cull) {
            this.cull = cull;
            return this;
        }

        public Builder uniform(String... names) {
            this.uniforms = names == null ? new String[0] : names;
            return this;
        }

        public Builder sampler(String... names) {
            this.samplers = names == null ? new String[0] : names;
            return this;
        }

        public RenderPipeline get() {
            var builder = RenderPipeline.builder()
                    .withBlend(blend)
                    .withVertexFormat(format, mode)
                    .withCull(cull)
                    .withDepthTestFunction(depthTest)
                    .withDepthWrite(depthWrite)
                    .withVertexShader(shaderId(namespace, vertex))
                    .withFragmentShader(shaderId(namespace, fragment))
                    .withLocation(Identifier.parse("pasta:" + locationPath));

            for (String uniform : uniforms) {
                builder.withUniform(uniform, UniformType.UNIFORM_BUFFER);
            }
            for (String sampler : samplers) {
                builder.withSampler(sampler);
            }
            return RenderPipelines.register(builder.build());
        }

        private static Identifier shaderId(String namespace, String name) {
            if (name.contains(":")) {
                return Identifier.parse(name);
            }
            String path = name.startsWith("core/") ? name : "core/" + name;
            return Identifier.fromNamespaceAndPath(namespace, path);
        }
    }
}
