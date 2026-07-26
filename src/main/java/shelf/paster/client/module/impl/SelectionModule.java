package shelf.paster.client.module.impl;

import shelf.paster.client.module.Category;
import shelf.paster.client.module.Module;
import shelf.paster.client.module.ModuleGroup;
import shelf.paster.client.module.setting.BooleanSetting;
import shelf.paster.client.module.setting.ModeSetting;
import shelf.paster.client.module.setting.SliderSetting;

public final class SelectionModule extends Module {
    public static final SelectionModule INSTANCE = new SelectionModule();

    public final ModeSetting target = new ModeSetting("Target", "Highest Damage", "Highest Damage", "Closest", "Lowest Health");
    public final ModeSetting hitboxes = new ModeSetting("Hitboxes", "Head, Chest, Stoma", "Head, Chest, Stoma", "Head", "Body");
    public final ModeSetting multipoint = new ModeSetting("Multipoint", "Head, Chest, Stoma", "Head, Chest, Stoma", "Head", "All");
    public final SliderSetting hitChance = new SliderSetting("Hit Chance", 50, 0, 100, 1);
    public final SliderSetting minDamage = new SliderSetting("Min Damage", 15, 0, 100, 1);
    public final BooleanSetting quickStop = new BooleanSetting("Quick Stop", true);
    public final BooleanSetting quickScope = new BooleanSetting("Quick Scope", true);

    private SelectionModule() {
        super("Selection", "Target selection rules", Category.COMBAT, ModuleGroup.SELECTION);
        addSettings(target, hitboxes, multipoint, hitChance, minDamage, quickStop, quickScope);
    }
}
