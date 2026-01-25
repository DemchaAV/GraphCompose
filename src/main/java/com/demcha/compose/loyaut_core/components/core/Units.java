package com.demcha.compose.loyaut_core.components.core;

/**
 * Represents supported measurement units for layout and rendering.
 */
public enum Units {

    /**
     * Pixels — device-dependent units.
     * OuterBoxSize changes with screen DPI.
     */
    PX,

    /**
     * Points — printing unit, 1 point = 1/72 inch.
     * Common in PDF and typography.
     */
    PT,

    /**
     * Millimeters — metric unit.
     * Often used in print layouts.
     */
    MM,

    /**
     * Centimeters — metric unit.
     */
    CM,

    /**
     * Inches — imperial unit.
     */
    IN
}

