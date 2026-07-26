package shelf.paster.client.module.impl;

import shelf.paster.client.module.Category;
import shelf.paster.client.module.Module;
import shelf.paster.client.module.ModuleGroup;
import shelf.paster.client.module.setting.BooleanSetting;
import shelf.paster.client.module.setting.ModeSetting;

public final class OtherAimModule extends Module {
    public static final OtherAimModule INSTANCE = new OtherAimModule();

    public final ModeSetting history = new ModeSetting("History", "High", "High", "Medium", "Low");
    public final BooleanSetting delayShot = new BooleanSetting("Delay Shot", true);
    public final BooleanSetting removeRecoil = new BooleanSetting("Remove Recoil", true);
    public final BooleanSetting removeSpread = new BooleanSetting("Remove Spread", true);
    public final BooleanSetting duckPeek = new BooleanSetting("Duck Peek Assist", false);
    public final BooleanSetting quickPeek = new BooleanSetting("Quick Peek Assist", false);
    public final BooleanSetting doubleTap = new BooleanSetting("Double Tap", true);

    private OtherAimModule() {
        super("Other", "Extra aim assists", Category.COMBAT, ModuleGroup.OTHER);
        addSettings(history, delayShot, removeRecoil, removeSpread, duckPeek, quickPeek, doubleTap);
    }
}
