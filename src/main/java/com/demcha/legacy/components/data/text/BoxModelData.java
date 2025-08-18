package com.demcha.legacy.components.data.text;

import com.demcha.components.core.Component;

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
public record BoxModelData(int margin, int padding) implements Component {}

