package shelf.paster.client.waypoint;

public final class WaypointManager {
    private Waypoint point;
    private Waypoint playerPoint;

    public void set(Waypoint waypoint) {
        this.point = waypoint;
    }

    public void clear() {
        this.point = null;
    }

    public boolean isEmpty() {
        return point == null;
    }

    public Waypoint get() {
        return point;
    }

    public void setPlayerWaypoint(Waypoint waypoint) {
        this.playerPoint = waypoint;
    }

    public void clearPlayerWaypoint() {
        this.playerPoint = null;
    }

    public boolean isEmptyPlayerWaypoint() {
        return playerPoint == null;
    }

    public Waypoint getPlayerWaypoint() {
        return playerPoint;
    }
}
