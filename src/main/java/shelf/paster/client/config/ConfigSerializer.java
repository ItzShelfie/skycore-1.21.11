package shelf.paster.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import shelf.paster.client.module.Module;
import shelf.paster.client.module.ModuleManager;
import shelf.paster.client.module.setting.BooleanSetting;
import shelf.paster.client.module.setting.ModeSetting;
import shelf.paster.client.module.setting.Setting;
import shelf.paster.client.module.setting.SliderSetting;

import java.util.Locale;

public final class ConfigSerializer {
    private ConfigSerializer() {
    }

    public static JsonObject saveModules(ModuleManager manager) {
        JsonObject modules = new JsonObject();
        for (Module module : manager.getModules()) {
            JsonObject data = new JsonObject();
            data.addProperty("key", module.getKeyBind());
            data.addProperty("enabled", module.isEnabled());
            for (Setting<?> setting : module.getSettings()) {
                saveSetting(data, setting);
            }
            modules.add(module.getName().toLowerCase(Locale.ROOT), data);
        }
        return modules;
    }

    public static void loadModules(ModuleManager manager, JsonObject modules) {
        if (modules == null) {
            return;
        }
        for (Module module : manager.getModules()) {
            JsonObject data = modules.getAsJsonObject(module.getName().toLowerCase(Locale.ROOT));
            if (data == null) {
                data = modules.getAsJsonObject(module.getName());
            }
            if (data == null) {
                continue;
            }
            module.setEnabled(false);
            if (data.has("key")) {
                module.setKeyBind(data.get("key").getAsInt());
            }
            for (Setting<?> setting : module.getSettings()) {
                loadSetting(data, setting);
            }
            if (data.has("enabled") && data.get("enabled").getAsBoolean()) {
                module.setEnabled(true);
            }
        }
    }

    private static void saveSetting(JsonObject data, Setting<?> setting) {
        String key = setting.getName().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (setting instanceof BooleanSetting bool) {
            data.addProperty(key, bool.enabled());
        } else if (setting instanceof SliderSetting slider) {
            data.addProperty(key, slider.get());
        } else if (setting instanceof ModeSetting mode) {
            data.addProperty(key, mode.get());
        }
    }

    private static void loadSetting(JsonObject data, Setting<?> setting) {
        String key = setting.getName().toLowerCase(Locale.ROOT).replace(' ', '_');
        JsonElement element = data.get(key);
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (setting instanceof BooleanSetting bool) {
            bool.set(element.getAsBoolean());
        } else if (setting instanceof SliderSetting slider) {
            slider.set(element.getAsDouble());
        } else if (setting instanceof ModeSetting mode) {
            mode.set(element.getAsString());
        }
    }
}
