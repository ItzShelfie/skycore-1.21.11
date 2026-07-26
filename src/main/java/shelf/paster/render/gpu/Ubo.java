package shelf.paster.render.gpu;

import com.mojang.blaze3d.buffers.GpuBuffer;

public record Ubo(String name, GpuBuffer uniformBuffer) { }
