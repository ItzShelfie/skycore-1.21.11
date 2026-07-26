package shelf.paster.client.module.impl;

import shelf.paster.client.module.Category;
import shelf.paster.client.module.Module;
import shelf.paster.client.module.ModuleGroup;
import shelf.paster.client.module.setting.BooleanSetting;
import shelf.paster.client.module.setting.ModeSetting;
import shelf.paster.client.module.setting.SliderSetting;

public final class AuraModule extends Module {
    public static final AuraModule INSTANCE = new AuraModule();

    public final BooleanSetting silent = new BooleanSetting("Silent Aim", true);
    public final BooleanSetting autoFire = new BooleanSetting("Automatic Fire", true);
    public final BooleanSetting throughWalls = new BooleanSetting("Aim Through Walls", true);
    public final SliderSetting fov = new SliderSetting("Field of View", 180, 10, 180, 1);

    private AuraModule() {
        super("Aura", "Combat aiming assist", Category.COMBAT, ModuleGroup.MAIN);
        addSettings(silent, autoFire, throughWalls, fov);
    }
}
