package shelf.paster.client.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import shelf.paster.Skycore;
import shelf.paster.client.waypoint.Waypoint;
import shelf.paster.render.Draw;
import shelf.paster.render.Fonts;
import shelf.paster.render.Text;

import java.awt.Color;

/** Simple 2D GPS arrow + distance. */
public final class GpsHud {
    private static final Color ACCENT = new Color(135, 206, 250);
    private static final Color TEXT = new Color(236, 238, 242);

    private GpsHud() {
    }

    public static void render() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.options.hideGui) {
            return;
        }
        Waypoint point = Skycore.get().waypoints().get();
        if (point == null) {
            point = Skycore.get().waypoints().getPlayerWaypoint();
        }
        if (point == null) {
            return;
        }

        double dx = point.x() + 0.5 - mc.player.getX();
        double dz = point.z() + 0.5 - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float relative = Mth.wrapDegrees(yaw - mc.player.getYRot());

        float cx = mc.getWindow().getGuiScaledWidth() * 0.5f;
        float cy = 36f;
        float rad = (float) Math.toRadians(relative);
        float ax = cx + Mth.sin(rad) * 28f;
        float ay = cy - Mth.cos(rad) * 18f;

        Draw.drawRound(ax - 3f, ay - 3f, 6f, 6f, 3f, ACCENT);
        Text.beginBatch();
        new Text(cx - 20f, cy + 22f, Fonts.REGULAR(), point.name() + " · " + (int) dist + "m")
                .size(10f)
                .color(TEXT)
                .render();
        new Text(ax - 4f, ay - 5f, Fonts.WATERMARK_ICONS(), ".")
                .size(10f)
                .color(ACCENT)
                .render();
        Text.flushBatch();
    }
}
