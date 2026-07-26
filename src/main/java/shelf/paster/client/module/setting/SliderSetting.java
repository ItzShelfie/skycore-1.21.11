package shelf.paster.client.module.setting;

public final class SliderSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;

    public SliderSetting(String name, double defaultValue, double min, double max, double step) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.step = Math.max(0.01, step);
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public double step() {
        return step;
    }

    @Override
    public void set(Double value) {
        if (value == null) {
            return;
        }
        double snapped = Math.round(value / step) * step;
        super.set(Math.max(min, Math.min(max, snapped)));
    }

    public float progress() {
        return (float) ((get() - min) / (max - min));
    }

    public void setProgress(float progress) {
        set(min + (max - min) * Math.max(0f, Math.min(1f, progress)));
    }
}
