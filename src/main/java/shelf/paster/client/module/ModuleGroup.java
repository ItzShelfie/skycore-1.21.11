package shelf.paster.client.module;

/** Content panel group labels above module lists. */
public enum ModuleGroup {
    MAIN("Aura"),
    OTHER("Tweaks"),
    SELECTION("Selection"),
    ANTI_AIM("Anti-Aim");

    private final String label;

    ModuleGroup(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Left column in two-col content layout. */
    public boolean leftColumn() {
        return this == MAIN || this == SELECTION;
    }
}
