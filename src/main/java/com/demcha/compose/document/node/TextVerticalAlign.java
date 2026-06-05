package com.demcha.compose.document.node;

/**
 * Vertical seating of a paragraph's text within the line box it is laid out in.
 *
 * <p>{@link #DEFAULT} keeps the engine's baseline seating — unchanged from
 * pre-1.7.0 behaviour. {@link #TOP}, {@link #CENTER} and {@link #BOTTOM} seat
 * the text by its cap band within the line box (cap top to the box top, cap band
 * centred, or baseline to the box bottom) using the font's cap height, ascent,
 * descent and leading — so a single line dropped into a taller
 * {@code ShapeContainer} / {@code LayerStack} layer sits where you ask instead
 * of always on the font baseline.</p>
 *
 * <p>These seat the glyph within its own line box; pair them with a
 * vertically-centred layer placement ({@code .center(...)} / {@code .centerLeft(...)})
 * to seat a label inside a rounded "pill". They do not change measurement,
 * pagination, or horizontal alignment.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.7.0
 */
public enum TextVerticalAlign {

    /** Engine default baseline seating — unchanged from pre-1.7.0 behaviour. */
    DEFAULT,

    /** Seat the cap top at the top of the line box (text hugs the top). */
    TOP,

    /** Optically centre the text by cap height within its line box. */
    CENTER,

    /**
     * Seat the baseline at the bottom of the line box (text hugs the bottom).
     * Descenders extend below the box, so this best suits cap-height labels.
     */
    BOTTOM
}
