package com.demcha.structure.interfaces;

/**
 * Context passed to Layout during arrange pass.
 * Contains positioning information for arranging child elements.
 */
public class ArrangeCtx {
    private final double startX;
    private final double startY;
    private final double allocatedWidth;
    private final double allocatedHeight;

    public ArrangeCtx(double startX, double startY, double allocatedWidth, double allocatedHeight) {
        this.startX = startX;
        this.startY = startY;
        this.allocatedWidth = allocatedWidth;
        this.allocatedHeight = allocatedHeight;
    }

    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public double getAllocatedWidth() { return allocatedWidth; }
    public double getAllocatedHeight() { return allocatedHeight; }
}

