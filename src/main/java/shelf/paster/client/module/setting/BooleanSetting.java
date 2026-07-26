package shelf.paster.client.module.setting;

public final class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String name, boolean defaultValue) {
        super(name, defaultValue);
    }

    public boolean enabled() {
        return Boolean.TRUE.equals(get());
    }

    public void toggle() {
        set(!enabled());
    }
}
