package shelf.paster.client.ui.clickgui;

import java.awt.Color;

/** Figma ClickGUI tokens — (CSS / 2) × 1.3. */
public final class ClickGuiTheme {
    private static final float S = 1.3f;

    public static final float WINDOW_W = 312f * S;
    public static final float WINDOW_H = 239.5f * S;
    public static final float SIDEBAR_W = 66.5f * S;
    public static final float CONTENT_W = 245.5f * S;
    public static final float RADIUS = 6.25f * S;
    public static final float PANEL_RADIUS = 5.75f * S;
    public static final float TOGGLE_W = 12.5f * S;
    public static final float TOGGLE_H = 8f * S;
    public static final float KNOB = 6f * S;
    public static final float ROW_H = 15f * S;
    public static final float PANEL_PAD = 6f * S;
    public static final float COL_GAP = 4.5f * S;
    public static final float CONTENT_INSET = 4.5f * S;
    public static final float COL_W = (CONTENT_W - CONTENT_INSET * 2f - COL_GAP) * 0.5f;
    public static final float CONTENT_TOP = 24f * S;
    public static final float CONTENT_VIEW_H = WINDOW_H - 28f * S;
    public static final float CARD_H = ROW_H + 7f * S;
    public static final float CARD_GAP = 4.5f * S;
    public static final float NAV_ROW_H = 12f * S;
    public static final float NAV_STEP = 14.5f * S;
    public static final float SETTINGS_W = 168f * S;

    public static final float FONT_BODY = 5.5f * S;
    public static final float FONT_SMALL = 4f * S;
    public static final float FONT_LABEL = 5f * S;
    public static final float FONT_ICON = 5.75f * S;
    public static final float FONT_SUB = 5f * S;
    public static final float FONT_PROFILE = 5.75f * S;
    public static final float FONT_PROFILE_SUB = 5f * S;
    public static final float FONT_SECTION = 5.5f * S;
    /** Sidebar MAIN / COMMON / OTHER only. */
    public static final float FONT_SIDEBAR_SECTION = 4.25f * S;

    /** Frost blur tint — more transparent than the solid shell. */
    public static final Color WINDOW_BLUR = new Color(8, 12, 16, 110);
    /** Solid shell (was first rect alpha). */
    public static final Color WINDOW_BG = new Color(8, 12, 16, 230);
    /** Content column. */
    public static final Color CONTENT_BG = new Color(4, 6, 10, 230);
    /** rgba(21, 20, 29, 0.3) */
    public static final Color PANEL_BG = new Color(21, 20, 29, 77);
    public static final Color SETTINGS_BG = new Color(10, 12, 18, 245);
    public static final Color BORDER = new Color(255, 255, 255, 13);
    public static final Color STRIPE = new Color(255, 255, 255, 13);
    public static final Color ACCENT = new Color(0x4B, 0x7D, 0xFE);
    public static final Color TOGGLE_OFF = new Color(3, 9, 19);
    public static final Color KNOB_OFF = new Color(0x79, 0x88, 0x95);
    public static final Color TEXT = Color.WHITE;
    public static final Color MUTED = new Color(0x3C, 0x40, 0x46);
    public static final Color SIDE_MUTED = new Color(0x85, 0x8D, 0x92);
    public static final Color SECTION = new Color(0x9A, 0x9A, 0x9A);
    public static final Color SIDE_ACTIVE_BG = new Color(255, 255, 255, 26);
    public static final Color DIVIDER = new Color(0x16, 0x18, 0x1C);
    public static final Color CHIP_BG = new Color(28, 27, 38, 100);
    public static final Color CHIP_BORDER = new Color(255, 255, 255, 51);
    public static final Color SUBTITLE = new Color(0x43, 0x49, 0x51);
    public static final Color AVATAR = new Color(0x03, 0x0D, 0x1A);
    public static final Color SLIDER_TRACK = new Color(0x18, 0x20, 0x2A);

    private ClickGuiTheme() {
    }
}
