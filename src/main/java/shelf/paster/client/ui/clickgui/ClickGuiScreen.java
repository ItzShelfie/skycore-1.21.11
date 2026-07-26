package shelf.paster.client.ui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import shelf.paster.Skycore;
import shelf.paster.client.animation.Animations;
import shelf.paster.client.animation.Easing;
import shelf.paster.client.module.Category;
import shelf.paster.client.module.Module;
import shelf.paster.client.module.ModuleGroup;
import shelf.paster.client.module.SidebarSection;
import shelf.paster.client.module.setting.BooleanSetting;
import shelf.paster.client.module.setting.ModeSetting;
import shelf.paster.client.module.setting.Setting;
import shelf.paster.client.module.setting.SliderSetting;
import shelf.paster.render.Draw;
import shelf.paster.render.Fonts;
import shelf.paster.render.Scissor;
import shelf.paster.render.Text;
import shelf.paster.render.font.Font;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static shelf.paster.client.ui.clickgui.ClickGuiTheme.*;

/**
 * ClickGUI: category label → one panel with module rows (name | gear | toggle).
 * Gear opens a floating CSS-style settings window.
 */
public final class ClickGuiScreen extends Screen {
    private enum Page {
        MODULES,
        THEMES,
        CONFIGS
    }

    private final List<SidebarHit> sidebarHits = new ArrayList<>();
    private final Searcher searcher = new Searcher();
    private Page page = Page.MODULES;
    private Category category = Category.COMBAT;
    private float scrollTarget;
    private SliderSetting dragging;
    private float dragSliderX;
    private float dragSliderW;
    private final HashMap<String, float[]> sliderPhysics = new HashMap<>();
    private float winX;
    private float winY;
    private String configName = "Rage";
    private String configScope = "Global";
    private boolean closing;
    private long lastFrameNanos = Util.getNanos();
    private Module settingsModule;
    private float settingsScrollTarget;

    public ClickGuiScreen() {
        super(Component.literal("ClickGUI"));
        Animations.clear("gui.open");
        Animations.clear("gui.close");
        Animations.clear("gui.scroll");
        Animations.clear("gui.content");
        Animations.clear("gui.settings");
        Animations.clear("gui.settings.scroll");
        Animations.of("gui.open").snap(0f).to(1f, 0.28f, Easing.CUBIC_OUT);
        Animations.of("gui.scroll").snap(0f);
        Animations.of("gui.content").snap(1f);
        Animations.of("gui.settings").snap(0f);
        Animations.of("gui.settings.scroll").snap(0f);
    }

    @Override
    protected void init() {
        centerWindow();
    }

    private void centerWindow() {
        winX = Math.round((width - WINDOW_W) * 0.5f);
        winY = Math.round((height - WINDOW_H) * 0.5f);
    }

    @Override
    public void tick() {
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected void renderBlurredBackground(GuiGraphics graphics) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (closing) {
            return;
        }
        closing = true;
        Animations.of("gui.close").snap(0f).to(1f, 0.22f, Easing.QUART_IN);
    }

    private void finishClose() {
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    private float openT() {
        return Animations.get("gui.open");
    }

    private float closeT() {
        return closing ? Animations.get("gui.close") : 0f;
    }

    private float presence() {
        return Math.max(0f, openT() * (1f - closeT()));
    }

    private float windowScale() {
        return 0.96f + 0.04f * openT() - 0.05f * closeT();
    }

    private float windowYSettle() {
        return (1f - openT()) * 5f + closeT() * 6f;
    }

    private float scroll() {
        return Animations.get("gui.scroll");
    }

    private float settingsPresence() {
        return Animations.get("gui.settings");
    }

    private float settingsScroll() {
        return Animations.get("gui.settings.scroll");
    }

    private void setScrollTarget(float value) {
        scrollTarget = clamp(value, 0f, maxScroll());
        Animations.of("gui.scroll").to(scrollTarget, 0.2f, Easing.CUBIC_OUT);
    }

    private void setSettingsScrollTarget(float value) {
        settingsScrollTarget = clamp(value, 0f, maxSettingsScroll());
        Animations.of("gui.settings.scroll").to(settingsScrollTarget, 0.18f, Easing.CUBIC_OUT);
    }

    private float maxScroll() {
        if (page != Page.MODULES) {
            return 0f;
        }
        return Math.max(0f, measureContentHeight() - CONTENT_VIEW_H);
    }

    private float maxSettingsScroll() {
        if (settingsModule == null) {
            return 0f;
        }
        // Enabled + settings
        int rows = 1 + settingsModule.getSettings().size();
        float body = rows * ROW_H + PANEL_PAD;
        float view = settingsPopupBox()[3] - ROW_H * 1.6f - PANEL_PAD;
        return Math.max(0f, body - view);
    }

    private void clampScroll() {
        float max = maxScroll();
        if (scrollTarget > max) {
            setScrollTarget(max);
        }
        if (Animations.get("gui.scroll") > max + 0.5f) {
            Animations.of("gui.scroll").to(max, 0.15f, Easing.CUBIC_OUT);
        }
        if (settingsModule != null) {
            float sMax = maxSettingsScroll();
            if (settingsScrollTarget > sMax) {
                setSettingsScrollTarget(sMax);
            }
        }
    }

    private void switchNav(Category cat, Page nextPage) {
        settingsModule = null;
        Animations.of("gui.settings").snap(0f);
        page = nextPage;
        if (cat != null) {
            category = cat;
        }
        scrollTarget = 0f;
        Animations.of("gui.scroll").snap(0f);
        Animations.of("gui.content").snap(0f).to(1f, 0.22f, Easing.CUBIC_OUT);
    }

    private void openSettings(Module module) {
        settingsModule = module;
        settingsScrollTarget = 0f;
        Animations.of("gui.settings.scroll").snap(0f);
        Animations.of("gui.settings").to(1f, 0.22f, Easing.CUBIC_OUT);
    }

    private void closeSettings() {
        dragging = null;
        Animations.of("gui.settings").to(0f, 0.16f, Easing.QUART_IN);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        centerWindow();

        long now = Util.getNanos();
        float dt = Math.min(0.05f, (now - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = now;
        Animations.tick(dt);

        if (closing && Animations.of("gui.close").done()) {
            finishClose();
            return;
        }

        clampScroll();

        float alpha = Math.max(0f, Math.min(1f, presence()));
        float scaleAnim = windowScale();
        float ySettle = windowYSettle();
        float guiScale = (float) minecraft.getWindow().getGuiScale();

        Draw.pushAlpha(alpha);
        Draw.pushTransform(new Matrix4f().scaling(guiScale, guiScale, 1f));
        try {
            Draw.drawRound(0, 0, width, height, 0f, new Color(0, 0, 0, (int) (110 * alpha)));

            float cx = winX + WINDOW_W * 0.5f;
            float cy = winY + WINDOW_H * 0.5f + ySettle;
            Matrix4f local = new Matrix4f()
                    .translation(cx, cy, 0f)
                    .scale(scaleAnim, scaleAnim, 1f)
                    .translate(-WINDOW_W * 0.5f, -WINDOW_H * 0.5f, 0f);
            Draw.pushTransform(local);
            try {
                float x = 0f;
                float y = 0f;

                Draw.drawBlur(x, y, WINDOW_W, WINDOW_H, 10f, RADIUS, WINDOW_BLUR);
                Draw.drawRound(x, y, WINDOW_W, WINDOW_H, RADIUS, WINDOW_BG);
                Draw.drawRound(x, y, WINDOW_W, WINDOW_H, RADIUS, new Color(0, 0, 0, 0), BORDER, 1f);

                renderSidebar(x, y);
                renderContent(x + SIDEBAR_W, y);

                searcher.layoutBelowGui(x, y + WINDOW_H);
                searcher.render(Fonts.MUKTA());

                renderSettingsPopup(Fonts.MUKTA());
            } finally {
                Draw.popTransform();
            }
        } finally {
            Draw.popTransform();
            Draw.popAlpha();
        }
    }

    private void renderSidebar(float x, float y) {
        Font icons = Fonts.GUI_ICONS();
        Font mukta = Fonts.MUKTA();
        Font muktaMed = Fonts.MUKTA_MEDIUM();
        Text.beginBatch();
        sidebarHits.clear();

        float logoSize = 13.5f * 1.3f;
        drawIcon(x + 7f * 1.3f, y + 4f * 1.3f, logoSize, icons, "a", FONT_ICON + 3f, TEXT);
        drawText(x + 20f * 1.3f, y + 4.5f * 1.3f, 7.5f * 1.3f, muktaMed, "Skycore", FONT_BODY + 1f, TEXT);
        drawText(x + 19.5f * 1.3f, y + 10.5f * 1.3f, 7f * 1.3f, muktaMed, "1.21.11", FONT_SUB, SUBTITLE);
        Draw.drawRound(x + 4f * 1.3f, y + 21.5f * 1.3f, SIDEBAR_W - 8f, 1f, 0.5f, DIVIDER);

        float cursor = y + 26f * 1.3f;
        cursor = renderSidebarSection(x, cursor, "MAIN", SidebarSection.MAIN, icons, mukta);
        cursor = renderSidebarSection(x, cursor + 3f * 1.3f, "COMMON", SidebarSection.COMMON, icons, mukta);
        renderSidebarOther(x, cursor + 3f * 1.3f, icons, mukta);

        float profileY = y + WINDOW_H - 23.5f * 1.3f;
        Draw.drawRound(x + 3.5f * 1.3f, profileY - 7f * 1.3f, SIDEBAR_W - 6.5f, 1f, 0.5f, DIVIDER);
        float av = 13.5f * 1.3f;
        Draw.drawRound(x + 6.5f * 1.3f, profileY + 3f * 1.3f, av, av, av * 0.5f, new Color(0x2A, 0x30, 0x38));
        drawText(x + 23.5f * 1.3f, profileY + 4f * 1.3f, 6.5f * 1.3f, mukta, "Shelf", FONT_PROFILE, TEXT);
        drawText(x + 23.5f * 1.3f, profileY + 10f * 1.3f, 6f * 1.3f, mukta, "LifeTime", FONT_PROFILE_SUB, MUTED);

        Text.flushBatch();
    }

    private float renderSidebarSection(float x, float startY, String title, SidebarSection section, Font icons, Font mukta) {
        drawText(x + 8f * 1.3f, startY, 7f * 1.3f, mukta, title, FONT_SIDEBAR_SECTION, SECTION);
        float itemY = startY + 8f * 1.3f;
        for (Category cat : Category.values()) {
            if (cat.getSection() != section) {
                continue;
            }
            boolean active = page == Page.MODULES && cat == category;
            drawNavRow(x, itemY, String.valueOf(cat.getIcon()), cat.getDisplayName(), active, "nav." + cat.name(), icons, mukta);
            sidebarHits.add(SidebarHit.category(itemY, cat));
            itemY += NAV_STEP;
        }
        return itemY;
    }

    private void renderSidebarOther(float x, float startY, Font icons, Font mukta) {
        drawText(x + 8f * 1.3f, startY, 7f * 1.3f, mukta, "OTHER", FONT_SIDEBAR_SECTION, SECTION);
        float itemY = startY + 8f * 1.3f;
        drawNavRow(x, itemY, "h", "Themes", page == Page.THEMES, "nav.themes", icons, mukta);
        sidebarHits.add(SidebarHit.page(itemY, Page.THEMES));
        itemY += NAV_STEP;
        drawNavRow(x, itemY, "i", "Configs", page == Page.CONFIGS, "nav.configs", icons, mukta);
        sidebarHits.add(SidebarHit.page(itemY, Page.CONFIGS));
    }

    private void drawNavRow(float x, float itemY, String icon, String label, boolean active, String animId, Font icons, Font mukta) {
        float highlight = Animations.of(animId).to(active ? 1f : 0f, 0.18f, Easing.CUBIC_OUT).get();
        float ix = x + 4f * 1.3f;
        float rowW = SIDEBAR_W - 8f;
        if (highlight > 0.01f) {
            Color bg = new Color(255, 255, 255, (int) (26 * highlight));
            Draw.drawRound(ix, itemY, rowW, NAV_ROW_H, 2.5f * 1.3f, bg);
        }
        Color iconColor = lerp(SIDE_MUTED, ACCENT, highlight);
        drawIcon(ix + 4f * 1.3f, itemY, NAV_ROW_H, icons, icon, FONT_ICON, iconColor);
        drawText(ix + 14f * 1.3f, itemY, NAV_ROW_H, mukta, label, FONT_BODY, TEXT);
    }

    private void renderContent(float x, float y) {
        Draw.drawRound(x, y, CONTENT_W, WINDOW_H, RADIUS, CONTENT_BG);
        Draw.drawRound(x, y, CONTENT_W, WINDOW_H, RADIUS, new Color(0, 0, 0, 0), BORDER, 1f);

        Text.beginBatch();
        Font mukta = Fonts.MUKTA();
        Font icons = Fonts.GUI_ICONS();

        float chipH = 11.5f * 1.3f;
        drawConfigChip(x + 4.5f * 1.3f, y + 5f * 1.3f, 43.5f * 1.3f, configName, true, mukta, icons);
        drawConfigChip(x + 54f * 1.3f, y + 5f * 1.3f, 33.5f * 1.3f, configScope, false, mukta, icons);
        float searchIconX = x + CONTENT_W - 15f * 1.3f;
        float searchIconY = y + 5f * 1.3f;
        drawIcon(searchIconX, searchIconY, chipH, icons, "b", FONT_ICON, TEXT);
        Draw.drawRound(x + 4.5f * 1.3f, y + 21.5f * 1.3f, CONTENT_W - 9f * 1.3f, 1f, 0.5f, DIVIDER);

        float contentAnim = Animations.get("gui.content");
        float slide = (1f - contentAnim) * 10f;
        Draw.pushAlpha(0.35f + 0.65f * contentAnim);
        Draw.pushTransform(new Matrix4f().translation(slide, 0f, 0f));
        try {
            Scissor.scissor().enableScissor(x + 2f, y + CONTENT_TOP, CONTENT_W - 4f, CONTENT_VIEW_H);

            if (page == Page.THEMES) {
                renderPlaceholder(x, y, "Themes", "Accent / palette presets", mukta);
            } else if (page == Page.CONFIGS) {
                renderPlaceholder(x, y, "Configs", "Save / load profiles", mukta);
            } else {
                renderModuleGroups(x, y, mukta);
            }

            Scissor.scissor().disableScissor();
        } finally {
            Draw.popTransform();
            Draw.popAlpha();
        }
        Text.flushBatch();
    }

    private void renderPlaceholder(float x, float y, String title, String sub, Font mukta) {
        float s = scroll();
        float top = y + 33.5f * 1.3f - s;
        Draw.drawRound(x + CONTENT_INSET, top, COL_W, 40f * 1.3f, PANEL_RADIUS, PANEL_BG);
        Draw.drawRound(x + CONTENT_INSET, top, COL_W, 40f * 1.3f, PANEL_RADIUS, new Color(0, 0, 0, 0), BORDER, 1f);
        drawText(x + CONTENT_INSET + 6f, top + 7.5f * 1.3f, 9f * 1.3f, mukta, title, FONT_BODY, TEXT);
        drawText(x + CONTENT_INSET + 6f, top + 18.5f * 1.3f, 8f * 1.3f, mukta, sub, FONT_LABEL, SIDE_MUTED);
    }

    private List<Module> visibleModules() {
        List<Module> source = searcher.hasQuery()
                ? Skycore.get().modules().getModules()
                : Skycore.get().modules().byCategory(category);
        return searcher.filter(source);
    }

    private Map<ModuleGroup, List<Module>> groupModules(List<Module> modules) {
        Map<ModuleGroup, List<Module>> grouped = new EnumMap<>(ModuleGroup.class);
        for (ModuleGroup g : ModuleGroup.values()) {
            grouped.put(g, new ArrayList<>());
        }
        for (Module module : modules) {
            grouped.get(module.getGroup()).add(module);
        }
        return grouped;
    }

    private boolean useCombatGroups() {
        return !searcher.hasQuery() && category == Category.COMBAT;
    }

    private float groupPanelHeight(int moduleCount) {
        return Math.max(CARD_H, moduleCount * ROW_H + PANEL_PAD);
    }

    private float measureContentHeight() {
        List<Module> modules = visibleModules();
        if (modules.isEmpty()) {
            return 40f;
        }
        if (useCombatGroups()) {
            Map<ModuleGroup, List<Module>> grouped = groupModules(modules);
            float leftY = CONTENT_TOP + 3f;
            float rightY = leftY;
            for (ModuleGroup group : ModuleGroup.values()) {
                List<Module> list = grouped.get(group);
                if (list.isEmpty()) {
                    continue;
                }
                float block = 8f * 1.3f + groupPanelHeight(list.size()) + CARD_GAP;
                if (group.leftColumn()) {
                    leftY += block;
                } else {
                    rightY += block;
                }
            }
            return Math.max(leftY, rightY) - CONTENT_TOP + 8f;
        }

        float h = 8f * 1.3f + groupPanelHeight(modules.size()) + CARD_GAP;
        return h + 8f;
    }

    private void renderModuleGroups(float x, float y, Font mukta) {
        List<Module> modules = visibleModules();
        float s = scroll();

        if (useCombatGroups()) {
            Map<ModuleGroup, List<Module>> grouped = groupModules(modules);
            float leftX = x + CONTENT_INSET;
            float rightX = leftX + COL_W + COL_GAP;
            float leftY = y + CONTENT_TOP + 3f - s;
            float rightY = leftY;

            for (ModuleGroup group : ModuleGroup.values()) {
                List<Module> list = grouped.get(group);
                if (list.isEmpty()) {
                    continue;
                }
                boolean left = group.leftColumn();
                float colX = left ? leftX : rightX;
                float colY = left ? leftY : rightY;

                drawText(colX + 5f, colY, 7f * 1.3f, mukta, group.label(), FONT_SECTION, SECTION);
                colY += 8f * 1.3f;
                float ph = groupPanelHeight(list.size());
                renderModuleListPanel(colX, colY, COL_W, ph, list, mukta);
                colY += ph + CARD_GAP;

                if (left) {
                    leftY = colY;
                } else {
                    rightY = colY;
                }
            }
            return;
        }

        // Single category panel (Render / Movement / search results)
        float colX = x + CONTENT_INSET;
        float colY = y + CONTENT_TOP + 3f - s;
        String title = searcher.hasQuery() ? "Results" : category.getDisplayName();
        drawText(colX + 5f, colY, 7f * 1.3f, mukta, title, FONT_SECTION, SECTION);
        colY += 8f * 1.3f;
        float ph = groupPanelHeight(modules.size());
        // Wider panel when not combat two-col
        float panelW = CONTENT_W - CONTENT_INSET * 2f;
        renderModuleListPanel(colX, colY, panelW, ph, modules, mukta);
    }

    /** One CSS panel containing module rows: name | gear | toggle. */
    private void renderModuleListPanel(float x, float y, float w, float h, List<Module> modules, Font mukta) {
        Draw.drawRound(x, y, w, h, PANEL_RADIUS, PANEL_BG);
        Draw.drawRound(x, y, w, h, PANEL_RADIUS, new Color(0, 0, 0, 0), BORDER, 1f);

        float rowY = y + PANEL_PAD * 0.45f;
        for (Module module : modules) {
            drawText(x + PANEL_PAD, rowY, ROW_H, mukta, module.getName(), FONT_BODY, TEXT);

            float toggleX = x + w - PANEL_PAD - TOGGLE_W;
            float toggleY = rowY + (ROW_H - TOGGLE_H) * 0.5f;
            float gearSize = 7f * 1.3f;
            float gearX = toggleX - gearSize - 5f * 1.3f;
            float gearY = rowY + (ROW_H - gearSize) * 0.5f;
            drawGear(gearX, gearY, gearSize, settingsModule == module ? ACCENT : SIDE_MUTED);
            drawToggle(toggleX, toggleY, module.isEnabled(), "tog." + module.getName() + ".enabled");
            rowY += ROW_H;
        }
    }

    private float[] panelOriginForGroup(ModuleGroup group, List<Module> modules) {
        Map<ModuleGroup, List<Module>> grouped = groupModules(modules);
        float leftX = CONTENT_INSET;
        float rightX = leftX + COL_W + COL_GAP;
        float leftY = CONTENT_TOP + 3f;
        float rightY = leftY;
        for (ModuleGroup g : ModuleGroup.values()) {
            List<Module> list = grouped.get(g);
            if (list.isEmpty()) {
                continue;
            }
            boolean left = g.leftColumn();
            float colX = left ? leftX : rightX;
            float colY = left ? leftY : rightY;
            colY += 8f * 1.3f;
            float ph = groupPanelHeight(list.size());
            if (g == group) {
                return new float[]{colX, colY, COL_W, ph};
            }
            colY += ph + CARD_GAP;
            if (left) {
                leftY = colY;
            } else {
                rightY = colY;
            }
        }
        return new float[]{leftX, leftY, COL_W, CARD_H};
    }

    private float[] singlePanelOrigin(List<Module> modules) {
        float colX = CONTENT_INSET;
        float colY = CONTENT_TOP + 3f + 8f * 1.3f;
        float panelW = CONTENT_W - CONTENT_INSET * 2f;
        float ph = groupPanelHeight(modules.size());
        return new float[]{colX, colY, panelW, ph};
    }

    private void renderSettingsPopup(Font mukta) {
        float p = settingsPresence();
        if (settingsModule == null) {
            return;
        }
        if (p <= 0.01f) {
            if (Animations.of("gui.settings").done()) {
                settingsModule = null;
            }
            return;
        }
        Module module = settingsModule;

        Draw.pushAlpha(p);
        try {
            Draw.drawRound(0, 0, WINDOW_W, WINDOW_H, RADIUS, new Color(0, 0, 0, (int) (120 * p)));

            float[] box = settingsPopupBox();
            float bx = box[0];
            float by = box[1];
            float bw = box[2];
            float bh = box[3];
            float scale = 0.94f + 0.06f * p;
            float cx = bx + bw * 0.5f;
            float cy = by + bh * 0.5f;

            Draw.pushTransform(new Matrix4f()
                    .translation(cx, cy, 0f)
                    .scale(scale, scale, 1f)
                    .translate(-bw * 0.5f, -bh * 0.5f, 0f));
            try {
                Draw.drawRound(0, 0, bw, bh, PANEL_RADIUS, SETTINGS_BG);
                Draw.drawRound(0, 0, bw, bh, PANEL_RADIUS, new Color(0, 0, 0, 0), CHIP_BORDER, 1.15f);

                float headerH = ROW_H * 1.35f;
                drawText(PANEL_PAD, 0, headerH, mukta, module.getName(), FONT_BODY + 0.5f, TEXT);
                drawText(bw - PANEL_PAD - mukta.getWidth("x", FONT_BODY + 1f), 0, headerH, mukta, "x", FONT_BODY + 1f, SIDE_MUTED);
                Draw.drawRound(PANEL_PAD, headerH, bw - PANEL_PAD * 2f, 1f, 0.5f, DIVIDER);

                float viewTop = headerH + 2f;
                float viewH = bh - viewTop - PANEL_PAD * 0.5f;
                Scissor.scissor().enableScissor(2f, viewTop, bw - 4f, viewH);
                float rowY = viewTop + 2f - settingsScroll();
                boolean first = true;

                // Enabled (module master toggle) — CSS first row
                drawSettingRow(0, rowY, bw, "Enabled", mukta, first);
                drawDots(bw - PANEL_PAD - TOGGLE_W - 10f * 1.3f, rowY + (ROW_H - 1f) * 0.5f);
                drawToggle(bw - PANEL_PAD - TOGGLE_W, rowY + (ROW_H - TOGGLE_H) * 0.5f,
                        module.isEnabled(), "tog." + module.getName() + ".enabled");
                rowY += ROW_H;
                first = false;

                String modKey = module.getName();
                for (Setting<?> setting : module.getSettings()) {
                    renderSettingRow(0, rowY, bw, setting, modKey, mukta, first);
                    rowY += ROW_H;
                    first = false;
                }
                Scissor.scissor().disableScissor();
            } finally {
                Draw.popTransform();
            }
        } finally {
            Draw.popAlpha();
        }
    }

    private float[] settingsPopupBox() {
        int rows = settingsModule == null ? 2 : 1 + Math.max(1, settingsModule.getSettings().size());
        float header = ROW_H * 1.35f;
        float body = Math.min(rows, 9) * ROW_H + PANEL_PAD;
        float bh = header + body;
        float bw = Math.min(SETTINGS_W, CONTENT_W - 20f);
        float bx = (WINDOW_W - bw) * 0.5f;
        float by = (WINDOW_H - bh) * 0.5f;
        return new float[]{bx, by, bw, bh};
    }

    private void renderSettingRow(float x, float rowY, float w, Setting<?> setting, String modKey, Font mukta, boolean first) {
        drawSettingRow(x, rowY, w, setting.getName(), mukta, first);
        String key = modKey + "." + setting.getName();

        if (setting instanceof BooleanSetting bool) {
            drawDots(x + w - PANEL_PAD - TOGGLE_W - 10f * 1.3f, rowY + (ROW_H - 1f) * 0.5f);
            drawToggle(x + w - PANEL_PAD - TOGGLE_W, rowY + (ROW_H - TOGGLE_H) * 0.5f, bool.enabled(), "tog." + key);
        } else if (setting instanceof SliderSetting slider) {
            float sliderW = 31f * 1.3f;
            float visual = visualSlider(slider, key);
            String label = formatSlider(slider, visual);
            float boxW = Math.max(9.5f * 1.3f, Fonts.REGULAR().getWidth(label, FONT_SMALL) + 5f * 1.3f);
            float boxX = x + w - PANEL_PAD - boxW;
            float boxY = rowY + (ROW_H - 8.5f * 1.3f) * 0.5f;
            float sliderX = boxX - sliderW - 6f * 1.3f;
            drawDots(sliderX - 10f * 1.3f, rowY + (ROW_H - 1f) * 0.5f);
            float[] phys = sliderPhysics.get(key);
            float vel = phys == null ? 0f : phys[1];
            drawSlider(sliderX, rowY + ROW_H * 0.5f - 1f * 1.3f, sliderW, visual, vel);
            Draw.drawRound(boxX, boxY, boxW, 8.5f * 1.3f, 2.5f * 1.3f, CHIP_BG);
            Draw.drawRound(boxX, boxY, boxW, 8.5f * 1.3f, 2.5f * 1.3f, new Color(0, 0, 0, 0), BORDER, 1f);
            drawText(boxX + 2f, boxY, 8.5f * 1.3f, Fonts.REGULAR(), label, FONT_SMALL, TEXT);
        } else if (setting instanceof ModeSetting mode) {
            float modeFlash = Animations.get("mode." + key);
            float modeW = 53.5f * 1.3f;
            float modeX = x + w - modeW - PANEL_PAD;
            float modeY = rowY + (ROW_H - 9.5f * 1.3f) * 0.5f;
            Draw.drawRound(modeX, modeY, modeW, 9.5f * 1.3f, 2.5f * 1.3f,
                    new Color(28, 27, 38, Math.min(220, 160 + (int) (60 * modeFlash))));
            Draw.drawRound(modeX, modeY, modeW, 9.5f * 1.3f, 2.5f * 1.3f, new Color(0, 0, 0, 0), BORDER, 1f);
            float textSlide = (1f - modeFlash) * 4f;
            String value = ellipsize(mukta, mode.get(), FONT_LABEL, modeW - 9f * 1.3f);
            drawText(modeX + 2.5f * 1.3f + textSlide, modeY, 9.5f * 1.3f, mukta, value, FONT_LABEL, TEXT);
            drawChevron(modeX + modeW - 6f * 1.3f, modeY + 4f * 1.3f);
        }
    }

    private float visualSlider(SliderSetting slider, String key) {
        float target = slider.progress();
        float[] state = sliderPhysics.computeIfAbsent(key, ignored -> new float[]{target, 0f});
        float err = target - state[0];
        float catchUp = dragging == slider ? 0.11f : 0.16f;
        state[1] = err * catchUp;
        if (dragging != slider) {
            state[1] = state[1] * 0.55f + err * 0.12f;
        }
        state[0] = clamp(state[0] + state[1], 0f, 1f);
        if (dragging != slider && Math.abs(state[1]) < 0.0004f && Math.abs(target - state[0]) < 0.0015f) {
            state[0] = target;
            state[1] = 0f;
        }
        return state[0];
    }

    private void drawSettingRow(float x, float rowY, float w, String label, Font mukta, boolean first) {
        if (!first) {
            Draw.drawRound(x + PANEL_PAD, rowY - 0.5f, w - PANEL_PAD * 2f, 1f, 0.5f, STRIPE);
        }
        drawText(x + PANEL_PAD, rowY, ROW_H, mukta, label, FONT_BODY, TEXT);
    }

    private void drawConfigChip(float x, float y, float w, String label, boolean withIcon, Font mukta, Font icons) {
        float h = 11.5f * 1.3f;
        Draw.drawRound(x, y, w, h, 2.5f * 1.3f, CHIP_BG);
        Draw.drawRound(x, y, w, h, 2.5f * 1.3f, new Color(0, 0, 0, 0), CHIP_BORDER, 1.15f);
        float textX = x + 4f * 1.3f;
        if (withIcon) {
            drawIcon(x + 3.5f * 1.3f, y, h, icons, "k", 5.5f * 1.3f, TEXT);
            textX = x + 12f * 1.3f;
        }
        drawText(textX, y, h, mukta, label, FONT_BODY, TEXT);
        drawChevron(x + w - 7f * 1.3f, y + 5f * 1.3f);
    }

    private void drawToggle(float x, float y, boolean on, String animId) {
        float t = Animations.of(animId).to(on ? 1f : 0f, 0.16f, Easing.CUBIC_OUT).get();
        float trackR = safeRoundRadius(TOGGLE_W, TOGGLE_H, TOGGLE_H * 0.5f);
        float knobR = safeRoundRadius(KNOB, KNOB, KNOB * 0.5f);
        Color track = lerp(TOGGLE_OFF, ACCENT, t);
        float trackH = lerp(TOGGLE_H * 0.94f, TOGGLE_H, t);
        Draw.drawRound(x, y + (TOGGLE_H - trackH) * 0.5f, TOGGLE_W, trackH, trackR, track);
        float knob = lerp(KNOB * 0.92f, KNOB, t);
        float knobX = x + lerp(0.5f * 1.3f, TOGGLE_W - knob - 0.5f * 1.3f, t);
        float knobY = y + (TOGGLE_H - knob) * 0.5f;
        Draw.drawRound(knobX, knobY, knob, knob, knobR, lerp(KNOB_OFF, Color.WHITE, t));
    }

    private void drawSlider(float x, float y, float w, float progress, float velocity) {
        final float h = 2f * 1.3f;
        float trackR = safeRoundRadius(w, h, h * 0.5f);
        Draw.drawRound(x, y, w, h, trackR, SLIDER_TRACK);
        float fill = w * clamp(progress, 0f, 1f);
        if (fill > 0.35f) {
            Draw.drawRound(x, y, fill, h, safeRoundRadius(fill, h, trackR), ACCENT);
        }
        float stretch = Math.min(2.4f * 1.3f, Math.abs(velocity) * 48f);
        float knobW = 4.5f * 1.3f + stretch;
        float knobH = Math.max(2.6f * 1.3f, 4.5f * 1.3f - stretch * 0.45f);
        Draw.drawRound(x + fill - knobW * 0.5f, y + h * 0.5f - knobH * 0.5f, knobW, knobH,
                safeRoundRadius(knobW, knobH, Math.min(knobW, knobH) * 0.5f), Color.WHITE);
    }

    private static float safeRoundRadius(float w, float h, float desired) {
        return Math.max(0f, Math.min(desired, Math.min(w, h) * 0.5f - 0.51f));
    }

    private static void drawGear(float x, float y, float size, Color color) {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float outer = size * 0.42f;
        float tooth = size * 0.18f;
        float hole = size * 0.16f;
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3.0;
            float tx = cx + (float) Math.cos(a) * outer - tooth * 0.5f;
            float ty = cy + (float) Math.sin(a) * outer - tooth * 0.5f;
            Draw.drawRound(tx, ty, tooth, tooth, tooth * 0.25f, color);
        }
        Draw.drawRound(cx - outer * 0.72f, cy - outer * 0.72f, outer * 1.44f, outer * 1.44f, outer * 0.72f, color);
        Draw.drawRound(cx - hole, cy - hole, hole * 2f, hole * 2f, hole, PANEL_BG);
    }

    private void drawDots(float x, float y) {
        Draw.drawRound(x, y, 1f * 1.3f, 1f * 1.3f, 0.5f * 1.3f, TEXT);
        Draw.drawRound(x + 1.5f * 1.3f, y, 1f * 1.3f, 1f * 1.3f, 0.5f * 1.3f, TEXT);
        Draw.drawRound(x + 3f * 1.3f, y, 1f * 1.3f, 1f * 1.3f, 0.5f * 1.3f, TEXT);
    }

    private void drawChevron(float x, float y) {
        Draw.drawRound(x, y, 3f * 1.3f, 0.5f * 1.3f, 0.25f * 1.3f, TEXT);
        Draw.drawRound(x + 0.75f * 1.3f, y + 1f * 1.3f, 1.5f * 1.3f, 0.5f * 1.3f, 0.25f * 1.3f, TEXT);
    }

    private static void drawText(float x, float rowTop, float rowH, Font font, String text, float size, Color color) {
        float ty = rowTop + (rowH - font.getLineHeight(size)) * 0.5f;
        new Text(x, ty, font, text).size(size).color(color).render();
    }

    private static void drawIcon(float x, float rowTop, float rowH, Font icons, String glyph, float size, Color color) {
        float ty = rowTop + (rowH - icons.getLineHeight(size)) * 0.5f;
        new Text(x, ty, icons, glyph).size(size).color(color).render();
    }

    private static String ellipsize(Font font, String text, float size, float maxW) {
        if (font.getWidth(text, size) <= maxW) {
            return text;
        }
        String cut = text;
        while (cut.length() > 1 && font.getWidth(cut + "..", size) > maxW) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "..";
    }

    private static String formatSlider(SliderSetting slider, float progress) {
        double v = slider.min() + clamp(progress, 0f, 1f) * (slider.max() - slider.min());
        String name = slider.getName().toLowerCase();
        if (name.contains("fov") || name.contains("view")) {
            return String.format("%.0f°", v);
        }
        if (name.contains("chance")) {
            return ((int) Math.rint(v)) + "%";
        }
        if (Math.abs(v - Math.rint(v)) < 0.001) {
            return String.valueOf((int) Math.rint(v));
        }
        return String.format("%.1f", v);
    }

    private float[] toLocal(double mx, double my) {
        float scaleAnim = windowScale();
        float cx = winX + WINDOW_W * 0.5f;
        float cy = winY + WINDOW_H * 0.5f + windowYSettle();
        return new float[]{
                ((float) mx - cx) / scaleAnim + WINDOW_W * 0.5f,
                ((float) my - cy) / scaleAnim + WINDOW_H * 0.5f
        };
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (closing) {
            return true;
        }
        centerWindow();
        float[] local = toLocal(event.x(), event.y());
        float localX = local[0];
        float localY = local[1];

        if (settingsPresence() > 0.4f && settingsModule != null) {
            return handleSettingsClick(localX, localY);
        }

        float maxY = WINDOW_H + 40f;
        if (localX < -20f || localY < -10f || localX > WINDOW_W + 20f || localY > maxY) {
            return false;
        }

        if (searcher.presence() > 0.2f && searcher.hit(localX, localY)) {
            if (!searcher.isOpen()) {
                searcher.open();
            }
            return true;
        }

        if (localX < 0 || localY < 0 || localX > WINDOW_W || localY > WINDOW_H) {
            if (hit(localX, localY, SIDEBAR_W + CONTENT_W - 18f * 1.3f, 4f * 1.3f, 14f * 1.3f, 14f * 1.3f)) {
                searcher.toggle();
                return true;
            }
            return searcher.presence() > 0.01f;
        }

        if (sidebarHits.isEmpty()) {
            rebuildSidebarHits();
        }

        float navLeft = 4f * 1.3f;
        float navRight = SIDEBAR_W - 4f * 1.3f;
        for (SidebarHit hit : sidebarHits) {
            if (localX >= navLeft && localX <= navRight && localY >= hit.y && localY <= hit.y + NAV_ROW_H) {
                if (hit.category != null) {
                    switchNav(hit.category, Page.MODULES);
                } else {
                    switchNav(null, hit.page);
                }
                return true;
            }
        }

        if (page != Page.MODULES) {
            return true;
        }

        if (hit(localX, localY, SIDEBAR_W + 4.5f * 1.3f, 5f * 1.3f, 43.5f * 1.3f, 11.5f * 1.3f)) {
            configName = configName.equals("Rage") ? "Legit" : "Rage";
            return true;
        }
        if (hit(localX, localY, SIDEBAR_W + 54f * 1.3f, 5f * 1.3f, 33.5f * 1.3f, 11.5f * 1.3f)) {
            configScope = configScope.equals("Global") ? "Local" : "Global";
            return true;
        }
        if (hit(localX, localY, SIDEBAR_W + CONTENT_W - 18f * 1.3f, 4f * 1.3f, 14f * 1.3f, 14f * 1.3f)) {
            searcher.toggle();
            return true;
        }

        return handleModuleListClick(localX, localY);
    }

    private boolean handleModuleListClick(float mx, float my) {
        List<Module> modules = visibleModules();
        float s = scroll();

        if (useCombatGroups()) {
            Map<ModuleGroup, List<Module>> grouped = groupModules(modules);
            for (ModuleGroup group : ModuleGroup.values()) {
                List<Module> list = grouped.get(group);
                if (list.isEmpty()) {
                    continue;
                }
                float[] origin = panelOriginForGroup(group, modules);
                float px = SIDEBAR_W + origin[0];
                float py = origin[1] - s;
                float pw = origin[2];
                if (hitModuleRows(mx, my, px, py, pw, list)) {
                    return true;
                }
            }
            return true;
        }

        float[] origin = singlePanelOrigin(modules);
        return hitModuleRows(mx, my, SIDEBAR_W + origin[0], origin[1] - s, origin[2], modules);
    }

    private boolean hitModuleRows(float mx, float my, float px, float py, float pw, List<Module> list) {
        float rowY = py + PANEL_PAD * 0.45f;
        for (Module module : list) {
            float toggleX = px + pw - PANEL_PAD - TOGGLE_W;
            float toggleY = rowY + (ROW_H - TOGGLE_H) * 0.5f;
            if (hit(mx, my, toggleX, toggleY, TOGGLE_W, TOGGLE_H)) {
                module.toggle();
                return true;
            }
            float gearSize = 7f * 1.3f;
            float gearX = toggleX - gearSize - 5f * 1.3f;
            float gearY = rowY + (ROW_H - gearSize) * 0.5f;
            if (hit(mx, my, gearX - 2f, gearY - 2f, gearSize + 4f, gearSize + 4f)) {
                openSettings(module);
                return true;
            }
            rowY += ROW_H;
        }
        return false;
    }

    private boolean handleSettingsClick(float mx, float my) {
        float[] box = settingsPopupBox();
        float bx = box[0];
        float by = box[1];
        float bw = box[2];
        float bh = box[3];

        if (!hit(mx, my, bx, by, bw, bh)) {
            closeSettings();
            return true;
        }

        float headerH = ROW_H * 1.35f;
        float closeW = Math.max(10f, Fonts.MUKTA().getWidth("x", FONT_BODY + 1f) + 4f);
        if (hit(mx, my, bx + bw - PANEL_PAD - closeW, by, closeW + PANEL_PAD, headerH)) {
            closeSettings();
            return true;
        }

        Module module = settingsModule;
        float viewTop = by + headerH + 2f;
        float rowY = viewTop + 2f - settingsScroll();

        // Enabled
        float toggleY = rowY + (ROW_H - TOGGLE_H) * 0.5f;
        if (hit(mx, my, bx + bw - PANEL_PAD - TOGGLE_W, toggleY, TOGGLE_W, TOGGLE_H)) {
            module.toggle();
            return true;
        }
        rowY += ROW_H;

        String modKey = module.getName();
        for (Setting<?> setting : module.getSettings()) {
            String key = modKey + "." + setting.getName();
            if (setting instanceof BooleanSetting bool) {
                if (hit(mx, my, bx + bw - PANEL_PAD - TOGGLE_W, rowY + (ROW_H - TOGGLE_H) * 0.5f, TOGGLE_W, TOGGLE_H)) {
                    bool.toggle();
                    return true;
                }
            } else if (setting instanceof ModeSetting mode) {
                float modeW = 53.5f * 1.3f;
                float modeX = bx + bw - modeW - PANEL_PAD;
                float modeY = rowY + (ROW_H - 9.5f * 1.3f) * 0.5f;
                if (hit(mx, my, modeX, modeY, modeW, 9.5f * 1.3f)) {
                    mode.cycle();
                    Animations.of("mode." + key).snap(0f).to(1f, 0.22f, Easing.CUBIC_OUT);
                    return true;
                }
            } else if (setting instanceof SliderSetting slider) {
                float sliderW = 31f * 1.3f;
                String label = formatSlider(slider, slider.progress());
                float boxW = Math.max(9.5f * 1.3f, Fonts.REGULAR().getWidth(label, FONT_SMALL) + 5f * 1.3f);
                float valueBoxX = bx + bw - PANEL_PAD - boxW;
                float sliderX = valueBoxX - sliderW - 6f * 1.3f;
                if (hit(mx, my, sliderX - 2f, rowY, sliderW + boxW + 6f, ROW_H)) {
                    dragging = slider;
                    dragSliderX = sliderX;
                    dragSliderW = sliderW;
                    sliderPhysics.computeIfAbsent(key, ignored -> new float[]{slider.progress(), 0f})[1] = 0f;
                    updateSlider(mx, sliderX, sliderW, slider);
                    return true;
                }
            }
            rowY += ROW_H;
        }
        return true;
    }

    private void rebuildSidebarHits() {
        sidebarHits.clear();
        float cursor = 26f * 1.3f;
        cursor = collectSectionHits(cursor, SidebarSection.MAIN);
        cursor = collectSectionHits(cursor + 3f * 1.3f, SidebarSection.COMMON);
        float otherY = cursor + 3f * 1.3f + 8f * 1.3f;
        sidebarHits.add(SidebarHit.page(otherY, Page.THEMES));
        sidebarHits.add(SidebarHit.page(otherY + NAV_STEP, Page.CONFIGS));
    }

    private float collectSectionHits(float startY, SidebarSection section) {
        float itemY = startY + 8f * 1.3f;
        for (Category cat : Category.values()) {
            if (cat.getSection() != section) {
                continue;
            }
            sidebarHits.add(SidebarHit.category(itemY, cat));
            itemY += NAV_STEP;
        }
        return itemY;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null) {
            updateSlider(toLocal(event.x(), event.y())[0], dragSliderX, dragSliderW, dragging);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = null;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (settingsPresence() > 0.4f && settingsModule != null) {
            setSettingsScrollTarget(settingsScrollTarget - (float) scrollY * 14f);
            return true;
        }
        setScrollTarget(scrollTarget - (float) scrollY * 14f);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (settingsPresence() > 0.4f && settingsModule != null && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            closeSettings();
            return true;
        }
        if (searcher.isOpen()) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                searcher.close();
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                searcher.backspace();
                setScrollTarget(0f);
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                return true;
            }
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            if (searcher.isOpen()) {
                searcher.close();
                return true;
            }
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searcher.isOpen() && searcher.charTyped(event.codepoint())) {
            setScrollTarget(0f);
            return true;
        }
        return super.charTyped(event);
    }

    private void updateSlider(float mouseX, float sliderX, float sliderW, SliderSetting slider) {
        slider.setProgress((mouseX - sliderX) / sliderW);
    }

    private static boolean hit(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx <= x + w && my <= y + h;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * clamp(t, 0f, 1f);
    }

    private static Color lerp(Color a, Color b, float t) {
        t = clamp(t, 0f, 1f);
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t)
        );
    }

    private record SidebarHit(float y, Category category, Page page) {
        static SidebarHit category(float y, Category category) {
            return new SidebarHit(y, category, Page.MODULES);
        }

        static SidebarHit page(float y, Page page) {
            return new SidebarHit(y, null, page);
        }
    }
}
