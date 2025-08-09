package com.demcha.structure.interfaces.ui;

/**
 * Represents a simple box model abstraction with configurable
 * margin and padding values.
 *
 * <p>In UI frameworks or layout engines, a <b>box model</b> defines
 * the spacing around content. This interface allows getting and
 * setting the margin and padding values.</p>
 *
 * <ul>
 *   <li><b>Margin</b> – The space outside the element's border.</li>
 *   <li><b>Padding</b> – The space between the content and the element's border.</li>
 * </ul>
 *
 * <p>Implementations may define units (e.g., pixels) and how these values
 * affect rendering or layout behavior.</p>
 */
public interface BoxModel {

    /**
     * Returns the margin value.
     *
     * @return the current margin, typically in pixels.
     */
    int getMargin();

    /**
     * Sets the margin value.
     *
     * @param margin the new margin value, typically in pixels.
     */
    void setMargin(int margin);

    /**
     * Returns the padding value.
     *
     * @return the current padding, typically in pixels.
     */
    int getPadding();

    /**
     * Sets the padding value.
     *
     * @param padding the new padding value, typically in pixels.
     */
    void setPadding(int padding);
}
