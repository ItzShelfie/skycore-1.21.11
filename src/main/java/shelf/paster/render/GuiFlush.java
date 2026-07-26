package shelf.paster.render;

public final class GuiFlush {
    private static boolean active;
    private static boolean dirty;
    private static int frameId;

    private GuiFlush() {
    }

    public static void beginFrame() {
        active = true;
        dirty = false;
        frameId++;
    }

    public static void endFrame() {
        active = false;
        dirty = false;
    }

    public static void markSubmission() {
        if (active) {
            dirty = true;
        }
    }

    public static boolean needsFlush() {
        return active && dirty;
    }

    public static void markFlushed() {
        dirty = false;
    }

    public static boolean isActive() {
        return active;
    }

    public static int getFrameId() {
        return frameId;
    }
}
