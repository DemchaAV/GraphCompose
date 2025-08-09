package com.demcha.structure;

public class MeasureCtx {
    private final double availableWidth;
    private final double availableHeight;
    private final Units units; // PX, PT, MM
    private final double dpi;

    public MeasureCtx(double availableWidth, double availableHeight, Units units, double dpi) {
        this.availableWidth = availableWidth;
        this.availableHeight = availableHeight;
        this.units = units;
        this.dpi = dpi;
    }

    public double getAvailableWidth() { return availableWidth; }
    public double getAvailableHeight() { return availableHeight; }
    public Units getUnits() { return units; }
    public double getDpi() { return dpi; }
}
