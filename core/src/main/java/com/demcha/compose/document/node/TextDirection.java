package com.demcha.compose.document.node;

/**
 * Writing direction of a paragraph.
 *
 * <p>Direction is not the same choice as {@link TextAlign}. Alignment says where a
 * line sits in the available width; direction says which way the text runs, and so
 * decides the order glyphs are drawn in and which edge a line starts from. A
 * right-to-left paragraph therefore aligns to the right unless the author asks for
 * something else.</p>
 *
 * <p>Direction applies to the paragraph as a whole. Within it, the Unicode
 * Bidirectional Algorithm still decides each stretch: Latin words and numbers
 * embedded in Hebrew or Arabic keep running left to right, and the paragraph
 * direction only sets what they are embedded in.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.2.0
 */
public enum TextDirection {
    /**
     * Left to right. The default, and the direction every document laid out before
     * this option existed is rendered with.
     */
    LTR,

    /**
     * Right to left, for Hebrew, Arabic and other right-to-left scripts. Lines align
     * to the right unless the paragraph sets its own alignment.
     */
    RTL,

    /**
     * Taken from the paragraph's first strong character, falling back to
     * {@link #LTR} when it has none.
     *
     * <p>Useful for text whose script is not known when the document is written — a
     * user-supplied name or address. Digits and punctuation are not strong, so
     * {@code "2026 שלום"} resolves right to left.</p>
     */
    AUTO
}
