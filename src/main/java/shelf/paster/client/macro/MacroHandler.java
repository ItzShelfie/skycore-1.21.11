package shelf.paster.client.macro;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import shelf.paster.Skycore;
import shelf.paster.client.command.CommandManager;
import shelf.paster.client.ui.clickgui.ClickGuiScreen;

import java.util.HashMap;
import java.util.Map;

public final class MacroHandler {
    private final Map<Integer, Boolean> keyDown = new HashMap<>();

    public void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null || mc.player == null) {
            return;
        }
        Screen screen = mc.screen;
        if (screen instanceof ChatScreen || screen instanceof ClickGuiScreen) {
            keyDown.clear();
            return;
        }
        long handle = mc.getWindow().handle();
        for (MacroManager.MacroEntry macro : Skycore.get().macros().getMacros()) {
            boolean down = GLFW.glfwGetKey(handle, macro.keyCode()) == GLFW.GLFW_PRESS;
            boolean was = keyDown.getOrDefault(macro.keyCode(), false);
            if (down && !was) {
                run(macro.command());
            }
            keyDown.put(macro.keyCode(), down);
        }
    }

    private void run(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        String message = text.trim();
        CommandManager commands = CommandManager.get();
        if (commands != null && message.startsWith(commands.getPrefix())) {
            commands.tryExecute(message);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            if (message.startsWith("/")) {
                mc.getConnection().sendCommand(message.substring(1));
            } else {
                mc.getConnection().sendChat(message);
            }
        }
    }
}
