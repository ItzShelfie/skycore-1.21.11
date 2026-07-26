package shelf.paster.client.module.impl;

import shelf.paster.client.module.Category;
import shelf.paster.client.module.Module;
import shelf.paster.client.module.ModuleGroup;
import shelf.paster.client.module.setting.ModeSetting;

public final class AntiAimModule extends Module {
    public static final AntiAimModule INSTANCE = new AntiAimModule();

    public final ModeSetting pitch = new ModeSetting("Pitch", "Down", "Down", "Up", "Zero", "Custom");
    public final ModeSetting yaw = new ModeSetting("Yaw", "Backward", "Backward", "Spin", "Jitter", "Static");
    public final ModeSetting freestanding = new ModeSetting("Freestanding", "Off", "Off", "Normal", "Aggressive");
    public final ModeSetting mouseOverride = new ModeSetting("Mouse Override", "Off", "Off", "On");

    private AntiAimModule() {
        super("AntiAim", "Anti-aim angles", Category.COMBAT, ModuleGroup.ANTI_AIM);
        addSettings(pitch, yaw, freestanding, mouseOverride);
    }
}
