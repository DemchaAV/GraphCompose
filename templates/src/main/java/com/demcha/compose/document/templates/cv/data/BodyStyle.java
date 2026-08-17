package com.demcha.compose.document.templates.cv.data;

/**
 * How one {@link CvItem}'s description lines render — the second,
 * smaller axis next to {@link CvKind}.
 *
 * <p>The kind decides the item's shape (a bullet, a dated entry, a
 * line in a list); this decides what happens to
 * {@link CvItem#body()} inside it. The same experience entry can list
 * its achievements as bullets or read as a paragraph without changing
 * the module's kind, which is the distinction authors actually make
 * when they say "this section is bulleted".</p>
 *
 * @since 2.3.0
 */
public enum BodyStyle {

    /**
     * Each body line is a paragraph of prose. The default: an item
     * built without a stated style reads as text.
     */
    PARAGRAPH,

    /**
     * Each body line carries a bullet glyph and a hanging indent.
     */
    BULLETS
}
