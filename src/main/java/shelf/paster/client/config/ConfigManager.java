package shelf.paster.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import shelf.paster.client.module.Module;
import shelf.paster.client.module.ModuleManager;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Plain JSON configs under {@code .minecraft/skycore/configs/*.cfg}. */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final ModuleManager modules;
    private String lastLoadedConfig;

    public ConfigManager(ModuleManager modules) {
        this.modules = modules;
    }

    public void init() {
        getConfigDirectory().mkdirs();
    }

    public File getConfigDirectory() {
        File gameDir = Minecraft.getInstance().gameDirectory;
        return new File(gameDir, "skycore/configs");
    }

    public List<String> getConfigNames() {
        File[] files = getConfigDirectory().listFiles((dir, name) -> name.endsWith(".cfg"));
        if (files == null) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        for (File file : files) {
            names.add(file.getName().substring(0, file.getName().length() - 4));
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public String resolveConfigName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String trimmed = name.trim();
        for (String existing : getConfigNames()) {
            if (existing.equalsIgnoreCase(trimmed)) {
                return existing;
            }
        }
        return null;
    }

    public boolean configExists(String name) {
        return resolveConfigName(name) != null;
    }

    public ConfigInfo getConfigInfo(String name) {
        String resolved = resolveConfigName(name);
        if (resolved == null) {
            return null;
        }
        File file = new File(getConfigDirectory(), resolved + ".cfg");
        if (!file.exists()) {
            return null;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(file.toPath())).getAsJsonObject();
            long created = json.has("creationDate") ? json.get("creationDate").getAsLong() : file.lastModified();
            String creator = json.has("creator") ? json.get("creator").getAsString() : "Unknown";
            return new ConfigInfo(resolved, created, creator);
        } catch (Exception ignored) {
            return new ConfigInfo(resolved, file.lastModified(), "Unknown");
        }
    }

    public void saveConfig(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        String resolved = name.trim();
        getConfigDirectory().mkdirs();
        File file = new File(getConfigDirectory(), resolved + ".cfg");
        try {
            JsonObject root = new JsonObject();
            root.add("module", ConfigSerializer.saveModules(modules));
            root.addProperty("creationDate", System.currentTimeMillis());
            root.addProperty("creator", "Skycore");
            Files.writeString(file.toPath(), GSON.toJson(root));
            lastLoadedConfig = resolved;
        } catch (Exception exception) {
            System.err.println("[skycore] Failed to save config " + resolved + ": " + exception.getMessage());
        }
    }

    public boolean loadConfig(String name) {
        String resolved = resolveConfigName(name);
        if (resolved == null) {
            return false;
        }
        File file = new File(getConfigDirectory(), resolved + ".cfg");
        if (!file.exists()) {
            return false;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file.toPath())).getAsJsonObject();
            if (root.has("module") && root.get("module").isJsonObject()) {
                ConfigSerializer.loadModules(modules, root.getAsJsonObject("module"));
            } else if (root.has("modules") && root.get("modules").isJsonObject()) {
                ConfigSerializer.loadModules(modules, root.getAsJsonObject("modules"));
            }
            lastLoadedConfig = resolved;
            return true;
        } catch (Exception exception) {
            System.err.println("[skycore] Failed to load config " + resolved + ": " + exception.getMessage());
            return false;
        }
    }

    public boolean deleteConfig(String name) {
        String resolved = resolveConfigName(name);
        if (resolved == null) {
            return false;
        }
        boolean deleted = new File(getConfigDirectory(), resolved + ".cfg").delete();
        if (deleted && resolved.equals(lastLoadedConfig)) {
            lastLoadedConfig = null;
        }
        return deleted;
    }

    public void resetToDefaults() {
        for (Module module : modules.getModules()) {
            module.setEnabled(false);
            module.setKeyBind(-1);
        }
    }

    public String getLastLoadedConfig() {
        return lastLoadedConfig;
    }

    public record ConfigInfo(String name, long creationDate, String creator) {
    }
}
