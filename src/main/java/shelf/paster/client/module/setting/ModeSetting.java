package shelf.paster.client.module.setting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String defaultValue, String... modes) {
        super(name, defaultValue);
        this.modes = Collections.unmodifiableList(Arrays.asList(modes));
        if (!this.modes.contains(defaultValue) && !this.modes.isEmpty()) {
            set(this.modes.getFirst());
        }
    }

    public List<String> modes() {
        return modes;
    }

    public void cycle() {
        if (modes.isEmpty()) {
            return;
        }
        int index = modes.indexOf(get());
        set(modes.get((index + 1) % modes.size()));
    }
}
