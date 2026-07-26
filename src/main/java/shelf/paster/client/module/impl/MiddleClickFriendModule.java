package shelf.paster.client.module.impl;

import shelf.paster.client.module.Category;
import shelf.paster.client.module.Module;
import shelf.paster.client.module.ModuleGroup;
import shelf.paster.client.module.setting.BooleanSetting;

public final class MiddleClickFriendModule extends Module {
    public static final MiddleClickFriendModule INSTANCE = new MiddleClickFriendModule();

    public final BooleanSetting notify = new BooleanSetting("Notify", true);

    private MiddleClickFriendModule() {
        super("MiddleClickFriend", "Middle-click to friend players", Category.MISC, ModuleGroup.OTHER);
        addSettings(notify);
    }
}
