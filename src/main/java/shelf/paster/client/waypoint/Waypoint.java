package shelf.paster.client.waypoint;

public final class Waypoint {
    private final String name;
    private final int x;
    private final int z;

    public Waypoint(int x, int z) {
        this("GPS", x, z);
    }

    public Waypoint(String name, int x, int z) {
        this.name = name == null ? "GPS" : name;
        this.x = x;
        this.z = z;
    }

    public String name() {
        return name;
    }

    public int x() {
        return x;
    }

    public int z() {
        return z;
    }
}
