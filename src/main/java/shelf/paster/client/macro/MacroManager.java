package shelf.paster.client.macro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MacroManager {
    public record MacroEntry(String name, int keyCode, String command) {
    }

    private final List<MacroEntry> macros = new CopyOnWriteArrayList<>();

    public void addMacro(String name, int keyCode, String command) {
        removeMacro(name);
        macros.add(new MacroEntry(name, keyCode, command));
    }

    public void removeMacro(String name) {
        macros.removeIf(macro -> macro.name().equalsIgnoreCase(name));
    }

    public void clear() {
        macros.clear();
    }

    public List<MacroEntry> getMacros() {
        return Collections.unmodifiableList(new ArrayList<>(macros));
    }

    public MacroEntry find(String name) {
        for (MacroEntry macro : macros) {
            if (macro.name().equalsIgnoreCase(name)) {
                return macro;
            }
        }
        return null;
    }

    public List<String> names() {
        List<String> names = new ArrayList<>();
        for (MacroEntry macro : macros) {
            names.add(macro.name());
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }
}
