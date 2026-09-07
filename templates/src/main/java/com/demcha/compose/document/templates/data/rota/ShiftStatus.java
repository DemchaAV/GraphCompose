package com.demcha.compose.document.templates.data.rota;

/**
 * What a cell on a rota says about the shift in it.
 *
 * <p>A status is a meaning, not a colour: the document says a day is holiday
 * and the preset decides what holiday looks like. That is why the statuses are
 * an enumeration rather than the colour strings a spreadsheet would carry — two
 * presets can render the same rota in two palettes, and a status a preset does
 * not style still says what it means to a reader of the legend.</p>
 *
 * @since 2.4.0
 */
public enum ShiftStatus {

    /** A worked shift, printed as the hours themselves and carrying no status. */
    NONE,

    /** Time the person has asked for and which is not yet granted. */
    REQUEST,

    /** A day not worked. */
    OFF,

    /** Booked leave. */
    HOLIDAY,

    /** A shift spent on stock rather than on the floor. */
    STOCK,

    /** On call: not rostered, but reachable. */
    STANDBY,

    /** A shift spent training or being trained. */
    TRAINING,

    /** Covering another team or another site. */
    SUPPORT
}
