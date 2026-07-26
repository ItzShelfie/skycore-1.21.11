package shelf.paster.client.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import shelf.paster.render.Text;
import shelf.paster.render.Draw;
import shelf.paster.render.Fonts;
import shelf.paster.render.font.Font;

import java.awt.Color;
import java.util.UUID;

public final class Watermark {
    private static final float X = 10f;
    private static final float Y = 10f;
    private static final float PAD_X = 10f;
    private static final float GAP = 9f;
    private static final float ICON_GAP = 4f;
    private static final float DOT_SIZE = 2.5f;
    private static final float BODY_SIZE = 12f;
    private static final float LABEL_SIZE = 10f;
    private static final float ICON_SIZE = 12f;
    private static final float LOGO_SIZE = 12f;
    private static final float RADIUS = 12f;
    private static final float DOT_RADIUS = DOT_SIZE * 0.5f;
    private static final Color TEXT = new Color(236, 238, 242);
    private static final Color MUTED = new Color(150, 158, 170);
    private static final Color DOT_COLOR = new Color(170, 176, 188, 200);
    private static final Color PANEL = new Color(18, 22, 30, 200);
    private static final Color ICON = new Color(135, 206, 250);

    private Watermark() {
    }

    public static void render() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options.hideGui || mc.getWindow() == null) {
            return;
        }

        Fonts.warmUp();

        Font icons = Fonts.ICONS();
        String nick = mc.getUser() != null ? mc.getUser().getName() : "Player";
        String fpsValue = String.valueOf(mc.getFps());
        String pingValue = String.valueOf(resolvePing(mc));
        String server = resolveServer(mc);

        float logoW = icons.getWidth("a", LOGO_SIZE);
        float iconUser = icons.getWidth("b", ICON_SIZE);
        float iconFps = icons.getWidth("c", ICON_SIZE);
        float iconPing = icons.getWidth("g", ICON_SIZE);
        float iconServer = icons.getWidth("h", ICON_SIZE);

        float nickW = Fonts.REGULAR().getWidth(nick, BODY_SIZE);
        float fpsValueW = Fonts.REGULAR().getWidth(fpsValue, BODY_SIZE);
        float fpsLabelW = Fonts.REGULAR().getWidth("FPS", LABEL_SIZE);
        float pingValueW = Fonts.REGULAR().getWidth(pingValue, BODY_SIZE);
        float pingLabelW = Fonts.REGULAR().getWidth("ms", LABEL_SIZE);
        float serverW = Fonts.REGULAR().getWidth(server, BODY_SIZE);

        float nickBlock = iconUser + ICON_GAP + nickW;
        float fpsBlock = iconFps + ICON_GAP + fpsValueW + 1f + fpsLabelW;
        float pingBlock = iconPing + ICON_GAP + pingValueW + 1f + pingLabelW;
        float serverBlock = iconServer + ICON_GAP + serverW;

        float contentW = logoW + GAP + DOT_SIZE + GAP
                + nickBlock + GAP + DOT_SIZE + GAP
                + fpsBlock + GAP + DOT_SIZE + GAP
                + pingBlock + GAP + DOT_SIZE + GAP
                + serverBlock;
        float panelW = contentW + PAD_X * 2f - 4;
        float panelH = 30f;

        Draw.drawLayeredGaussian(X, Y, panelW, panelH, 14f, 5f, RADIUS, PANEL);

        float cursor = X + PAD_X;
        float bodyY = Y + (panelH - Fonts.REGULAR().getLineHeight(BODY_SIZE)) * 0.5f;
        float iconY = Y + (panelH - icons.getLineHeight(ICON_SIZE)) * 0.5f;
        float logoY = Y + (panelH - icons.getLineHeight(LOGO_SIZE)) * 0.5f;
        float labelY = bodyY
                + (Fonts.REGULAR().getLineHeight(BODY_SIZE) - Fonts.REGULAR().getLineHeight(LABEL_SIZE)) * 0.5f
                + 0.5f;
        float dotY = Y + (panelH - DOT_SIZE) * 0.5f;

        Text.beginBatch();

        new Text(cursor, logoY, icons, "a").size(LOGO_SIZE).color(ICON).render();
        cursor += logoW + GAP;
        cursor = drawDotSep(cursor, dotY);
        cursor = drawIconText(cursor, iconY, bodyY, icons, "b", nick, iconUser, nickW);
        cursor = drawDotSep(cursor, dotY);
        cursor = drawIconValueLabel(cursor, iconY, bodyY, labelY, icons, "c", fpsValue, "fps", iconFps, fpsValueW);
        cursor = drawDotSep(cursor, dotY);
        cursor = drawIconValueLabel(cursor, iconY, bodyY, labelY, icons, "g", pingValue, "ms", iconPing, pingValueW);
        cursor = drawDotSep(cursor, dotY);
        drawIconText(cursor, iconY, bodyY, icons, "h", server, iconServer, serverW);

        Text.flushBatch();
    }

    private static float drawIconText(float cursor, float iconY, float bodyY, Font icons, String icon,
                                      String value, float iconW, float valueW) {
        new Text(cursor, iconY, icons, icon).size(ICON_SIZE).color(ICON).render();
        cursor += iconW + ICON_GAP;
        new Text(cursor, bodyY, Fonts.REGULAR(), value).size(BODY_SIZE).color(TEXT).render();
        return cursor + valueW + GAP;
    }

    private static float drawIconValueLabel(float cursor, float iconY, float bodyY, float labelY, Font icons,
                                            String icon, String value, String label, float iconW, float valueW) {
        new Text(cursor, iconY, icons, icon).size(ICON_SIZE).color(ICON).render();
        cursor += iconW + ICON_GAP;
        new Text(cursor, bodyY, Fonts.REGULAR(), value).size(BODY_SIZE).color(TEXT).render();
        cursor += valueW + 1f;
        new Text(cursor, labelY, Fonts.REGULAR(), label).size(LABEL_SIZE).color(MUTED).render();
        return cursor + Fonts.REGULAR().getWidth(label, LABEL_SIZE) + GAP;
    }

    private static float drawDotSep(float cursor, float dotY) {
        drawDot(cursor, dotY);
        return cursor + DOT_SIZE + GAP;
    }

    private static void drawDot(float x, float y) {
        Draw.drawRound(x, y, DOT_SIZE, DOT_SIZE, DOT_RADIUS, DOT_COLOR);
    }

    private static int resolvePing(Minecraft mc) {
        ClientPacketListener connection = mc.getConnection();
        if (connection == null || mc.player == null) {
            return 0;
        }
        UUID uuid = mc.player.getUUID();
        PlayerInfo info = connection.getPlayerInfo(uuid);
        return info != null ? Math.max(0, info.getLatency()) : 0;
    }

    private static String resolveServer(Minecraft mc) {
        if (mc.isLocalServer() || mc.hasSingleplayerServer()) {
            return "localhost";
        }
        ServerData data = mc.getCurrentServer();
        if (data == null) {
            return "localhost";
        }
        if (data.name != null && !data.name.isBlank() && !"Minecraft Server".equalsIgnoreCase(data.name)) {
            return data.name;
        }
        return data.ip != null && !data.ip.isBlank() ? data.ip : "localhost";
    }
}
