package shelf.paster.client.module.setting;

public abstract class Setting<T> {
    private final String name;
    private T value;
    private final T defaultValue;

    protected Setting(String name, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        if (value == null) {
            return;
        }
        this.value = value;
        onChanged();
    }

    public T getDefault() {
        return defaultValue;
    }

    public void reset() {
        set(defaultValue);
    }

    protected void onChanged() {
    }
}
