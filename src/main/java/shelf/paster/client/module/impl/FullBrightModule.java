package shelf.paster.client.module.impl;

import shelf.paster.client.module.Category;
import shelf.paster.client.module.Module;
import shelf.paster.client.module.ModuleGroup;
import shelf.paster.client.module.setting.BooleanSetting;
import shelf.paster.client.module.setting.SliderSetting;

public final class FullBrightModule extends Module {
    public static final FullBrightModule INSTANCE = new FullBrightModule();

    public final SliderSetting gamma = new SliderSetting("Gamma", 15, 1, 30, 0.5);
    public final BooleanSetting smooth = new BooleanSetting("Smooth", true);

    private FullBrightModule() {
        super("FullBright", "Brightens the world", Category.RENDER, ModuleGroup.MAIN);
        addSettings(gamma, smooth);
    }
}
