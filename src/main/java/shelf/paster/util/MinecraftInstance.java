package shelf.paster.util;

import net.minecraft.client.Minecraft;

/** Lightweight access to the running client instance (no mod shell dependencies). */
public interface MinecraftInstance {
    Minecraft mc = Minecraft.getInstance();

    static boolean playerReady() {
        return mc.player != null && mc.level != null;
    }
}
