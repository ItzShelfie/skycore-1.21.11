package shelf.paster.render;

import net.minecraft.resources.Identifier;
import shelf.paster.render.font.Font;

/** Lazily resolved MSDF faces (Mukta + SF + icon sets). */
public final class Fonts {
    private static Font mukta;
    private static Font muktaMedium;
    private static Font medium;
    private static Font regular;
    private static Font bold;
    private static Font icons;
    private static Font guiIcons;
    private static Font notificationIcons;
    private static Font watermarkIcons;

    private Fonts() {
    }

    /** Mukta Regular — ClickGUI body (CSS weight 400). */
    public static Font MUKTA() {
        if (mukta == null) {
            mukta = face("muktaregular");
        }
        return mukta;
    }

    /** Mukta Medium — ClickGUI labels (CSS weight 500). */
    public static Font MUKTA_MEDIUM() {
        if (muktaMedium == null) {
            muktaMedium = face("muktamedium");
        }
        return muktaMedium;
    }

    public static Font MEDIUM() {
        if (medium == null) {
            medium = face("sf_medium");
        }
        return medium;
    }

    public static Font REGULAR() {
        if (regular == null) {
            regular = face("sfregular");
        }
        return regular;
    }

    public static Font BOLD() {
        if (bold == null) {
            bold = face("sfbold");
        }
        return bold;
    }

    /** Legacy skycore icon atlas. */
    public static Font ICONS() {
        if (icons == null) {
            icons = face("skycore");
        }
        return icons;
    }

    /** ClickGUI sidebar / chrome icons (`a` logo … `o` filters). */
    public static Font GUI_ICONS() {
        if (guiIcons == null) {
            guiIcons = face("clickgui-icons");
        }
        return guiIcons;
    }

    public static Font NOTIFICATION_ICONS() {
        if (notificationIcons == null) {
            notificationIcons = face("notification-icons");
        }
        return notificationIcons;
    }

    public static Font WATERMARK_ICONS() {
        if (watermarkIcons == null) {
            watermarkIcons = face("watermark-icons");
        }
        return watermarkIcons;
    }

    public static void warmUp() {
        MUKTA().ensureLoaded();
        MUKTA_MEDIUM().ensureLoaded();
        MEDIUM().ensureLoaded();
        REGULAR().ensureLoaded();
        BOLD().ensureLoaded();
        ICONS().ensureLoaded();
        GUI_ICONS().ensureLoaded();
        NOTIFICATION_ICONS().ensureLoaded();
        WATERMARK_ICONS().ensureLoaded();
    }

    private static Font face(String baseName) {
        return new Font(
                Identifier.parse("pasta:fonts/" + baseName + ".json"),
                Identifier.parse("pasta:fonts/" + baseName + ".png")
        );
    }
}
