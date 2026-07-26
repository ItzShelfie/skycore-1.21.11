package shelf.paster.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import shelf.paster.client.module.impl.AntiAimModule;
import shelf.paster.client.module.impl.AuraModule;
import shelf.paster.client.module.impl.FullBrightModule;
import shelf.paster.client.module.impl.MiddleClickFriendModule;
import shelf.paster.client.module.impl.NoPushModule;
import shelf.paster.client.module.impl.OtherAimModule;
import shelf.paster.client.module.impl.SelectionModule;
import shelf.paster.client.module.impl.SprintModule;
import shelf.paster.client.module.impl.TestModule;
import shelf.paster.client.ui.clickgui.ClickGuiScreen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private final Map<Module, Boolean> keyDown = new HashMap<>();
    private boolean clickGuiKeyDown;

    public void init() {
        register(
                AuraModule.INSTANCE,
                OtherAimModule.INSTANCE,
                SelectionModule.INSTANCE,
                AntiAimModule.INSTANCE,
                SprintModule.INSTANCE,
                FullBrightModule.INSTANCE,
                NoPushModule.INSTANCE,
                MiddleClickFriendModule.INSTANCE
        );

        // Add 3 "Test" placeholder modules to every category+group that has modules.
        List<Module> snapshot = new ArrayList<>(modules);
        Set<String> seen = new LinkedHashSet<>();
        for (Module module : snapshot) {
            String key = module.getCategory().name() + "-" + module.getGroup().name();
            if (seen.add(key)) {
                for (int i = 1; i <= 3; i++) {
                    register(new TestModule("Test-" + key + "-" + i, module.getCategory(), module.getGroup()));
                }
            }
        }
    }

    public void register(Module... toRegister) {
        for (Module module : toRegister) {
            if (module != null && !modules.contains(module)) {
                modules.add(module);
            }
        }
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public List<Module> byCategory(Category category) {
        List<Module> list = new ArrayList<>();
        for (Module module : modules) {
            if (module.getCategory() == category) {
                list.add(module);
            }
        }
        return list;
    }

    public Module getModule(String name) {
        if (name == null) {
            return null;
        }
        String normalized = normalize(name);
        for (Module module : modules) {
            if (normalize(module.getName()).equalsIgnoreCase(normalized)) {
                return module;
            }
        }
        return null;
    }

    public void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return;
        }
        Screen screen = mc.screen;
        boolean blockKeys = screen instanceof ChatScreen || screen instanceof PauseScreen;
        long handle = mc.getWindow().handle();

        boolean guiKey = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (guiKey && !clickGuiKeyDown) {
            if (screen instanceof ClickGuiScreen) {
                mc.setScreen(null);
            } else if (screen == null) {
                mc.setScreen(new ClickGuiScreen());
            }
        }
        clickGuiKeyDown = guiKey;

        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onTick();
            }
            int bind = module.getKeyBind();
            if (blockKeys || bind <= 0 || screen instanceof ClickGuiScreen) {
                keyDown.put(module, false);
                continue;
            }
            boolean down = GLFW.glfwGetKey(handle, bind) == GLFW.GLFW_PRESS;
            boolean wasDown = keyDown.getOrDefault(module, false);
            if (down && !wasDown) {
                module.toggle();
            }
            keyDown.put(module, down);
        }
    }

    private static String normalize(String name) {
        return name.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
