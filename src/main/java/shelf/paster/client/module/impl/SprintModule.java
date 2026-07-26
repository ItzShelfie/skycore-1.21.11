package shelf.paster.client.module.impl;

import shelf.paster.client.module.Category;
import shelf.paster.client.module.Module;
import shelf.paster.client.module.ModuleGroup;
import shelf.paster.client.module.setting.BooleanSetting;
import shelf.paster.client.module.setting.SliderSetting;

public final class SprintModule extends Module {
    public static final SprintModule INSTANCE = new SprintModule();

    public final BooleanSetting omni = new BooleanSetting("Omni", true);
    public final SliderSetting keep = new SliderSetting("Keep Ticks", 1, 0, 10, 1);

    private SprintModule() {
        super("Sprint", "Keeps sprint while enabled", Category.MOVEMENT, ModuleGroup.MAIN);
        addSettings(omni, keep);
    }
}
