package com.demcha.compose.document.output;

/**
 * How a page number is rendered in a header / footer {@code {page}} or
 * {@code {pages}} token. A backend-neutral selector; the engine performs the
 * actual numeral formatting.
 *
 * @author Artem Demchyshyn
 * @see DocumentPageNumbering
 * @since 1.9.0
 */
public enum DocumentPageNumberStyle {
    /** Arabic numerals: {@code 1, 2, 3} (the default). */
    DECIMAL,
    /** Lowercase Roman numerals: {@code i, ii, iii} — common for front matter. */
    LOWER_ROMAN,
    /** Uppercase Roman numerals: {@code I, II, III}. */
    UPPER_ROMAN,
    /** Lowercase letters: {@code a, b, c, … z, aa}. */
    LOWER_ALPHA,
    /** Uppercase letters: {@code A, B, C, … Z, AA}. */
    UPPER_ALPHA
}
