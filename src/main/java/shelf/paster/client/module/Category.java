package shelf.paster.client.module;

/** Module categories for ClickGUI / module registry. */
public enum Category {
    COMBAT("Combat", 'c', SidebarSection.MAIN),
    MOVEMENT("Movement", 'd', SidebarSection.MAIN),
    PLAYER("Player", 'e', SidebarSection.COMMON),
    RENDER("Render", 'f', SidebarSection.COMMON),
    MISC("Misc", 'g', SidebarSection.COMMON);

    private final String displayName;
    private final char icon;
    private final SidebarSection section;

    Category(String displayName, char icon, SidebarSection section) {
        this.displayName = displayName;
        this.icon = icon;
        this.section = section;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Glyph in {@code clickgui-icons} MSDF font. */
    public char getIcon() {
        return icon;
    }

    public SidebarSection getSection() {
        return section;
    }
}
