package shelf.paster.client.module.impl;

import shelf.paster.client.module.Category;
import shelf.paster.client.module.Module;
import shelf.paster.client.module.ModuleGroup;
import shelf.paster.client.module.setting.BooleanSetting;
import shelf.paster.client.module.setting.ModeSetting;

public final class NoPushModule extends Module {
    public static final NoPushModule INSTANCE = new NoPushModule();

    public final BooleanSetting players = new BooleanSetting("Players", true);
    public final BooleanSetting blocks = new BooleanSetting("Blocks", true);
    public final ModeSetting mode = new ModeSetting("Mode", "Cancel", "Cancel", "Reduce");

    private NoPushModule() {
        super("NoPush", "Reduces entity / block push", Category.PLAYER, ModuleGroup.MAIN);
        addSettings(players, blocks, mode);
    }
}
