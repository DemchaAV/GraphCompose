package com.demcha.structure.interfaces.ui;

/**
 * Represents an object that exists on a specific layer.
 *
 * <p>In graphical systems, rendering engines, or UI frameworks,
 * a <b>layer</b> is often used to determine the drawing order or
 * stacking position of elements. Lower layer numbers are typically
 * drawn first (behind others), while higher numbers appear on top.</p>
 */
public interface Layered {

    /**
     * Returns the layer index of this object.
     *
     * @return the current layer index.
     */
    int getLayer();

    /**
     * Sets the layer index of this object.
     *
     * @param layer the new layer index; higher values are typically drawn above lower ones.
     */
    void setLayer(int layer);
}

