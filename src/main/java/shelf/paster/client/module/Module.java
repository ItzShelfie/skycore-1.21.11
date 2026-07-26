package shelf.paster.client.module;

import shelf.paster.client.module.setting.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private final ModuleGroup group;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;
    private int keyBind = -1;
    private boolean expanded = true;

    protected Module(String name, String description, Category category) {
        this(name, description, category, ModuleGroup.MAIN);
    }

    protected Module(String name, String description, Category category, ModuleGroup group) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public ModuleGroup getGroup() {
        return group;
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    protected void addSettings(Setting<?>... toAdd) {
        for (Setting<?> setting : toAdd) {
            if (setting != null) {
                settings.add(setting);
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getKeyBind() {
        return keyBind;
    }

    public void setKeyBind(int keyBind) {
        this.keyBind = keyBind;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void toggleExpanded() {
        expanded = !expanded;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    public void onTick() {
    }
}
