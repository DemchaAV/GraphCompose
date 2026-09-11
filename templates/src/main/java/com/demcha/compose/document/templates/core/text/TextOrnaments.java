package com.demcha.compose.document.templates.core.text;

import com.demcha.compose.document.style.DocumentLetterSpacing;

import java.util.Locale;

/**
 * Pure text-transform helpers used by the v2 components: uppercase
 * normalisation for the document headline and section banners, and the
 * pipe-joining used for contact lines.
 *
 * <p>The spaced-caps <em>look</em> is no longer <em>built</em> here. It
 * used to be: the text was rewritten with a space between every pair of
 * letters, which drew the right picture and wrecked the text layer —
 * a name came back out of the file as {@code "J A N E   D O E"} to
 * search, copy/paste, a screen reader and an applicant-tracking
 * parser. Spacing is typography, so it now lives on the style as
 * {@link #SPACED_CAPS}, and the text stays the text.</p>
 *
 * <p>{@link #spacedUpper(String)} is still here and still does exactly
 * what it always did, for callers compiled against 2.3.0 and earlier.
 * It is deprecated, and no GraphCompose component or preset calls it —
 * only the test that pins its output does.</p>
 */
public final class TextOrnaments {

    /**
     * The tracking the spaced-caps components apply.
     *
     * <p>Chosen against a measurement of what the old transform did.
     * That transform put one space glyph between adjacent letters,
     * which measured 0.232&nbsp;em (IBM Plex Serif) to 0.278&nbsp;em
     * (Helvetica) across the faces these presets use &mdash; far more
     * than editorial spaced caps normally carry, because a space glyph
     * is what it had to work with.</p>
     *
     * <p>Matching that per-gap figure would have made every heading
     * wider than it was, for two reasons: real tracking adds a unit
     * after the last glyph as well as between them, and it tracks the
     * real word space too. Matching the old <em>total</em> width
     * instead puts the value at 0.174&ndash;0.209&nbsp;em on the same
     * faces, so 0.18&nbsp;em sits inside that band, still reads
     * unmistakably as spaced caps, and keeps headings near the width
     * they already occupied rather than pushing them into new
     * wrapping.</p>
     *
     * <p>Expressed as a share of the font size, so one value serves a
     * 24pt name and an 8pt section label alike &mdash; which is what
     * the old transform did implicitly, the space glyph scaling with
     * the type.</p>
     */
    public static final DocumentLetterSpacing SPACED_CAPS = DocumentLetterSpacing.ofFontSize(0.18);

    private TextOrnaments() {
    }

    /**
     * Uppercase rendering, with {@code null} treated as empty.
     *
     * <p>The text and nothing but the text: pair it with
     * {@link #SPACED_CAPS} on the style when the spaced-caps look is
     * wanted.</p>
     *
     * @param value source text (null tolerated, returned as empty)
     * @return the value in upper case
     */
    public static String upper(String value) {
        if (value == null) {
            return "";
        }
        return value.toUpperCase(Locale.ROOT);
    }

    /**
     * Letter-spaced uppercase rendering (e.g.
     * {@code spacedUpper("Jane Doe") -> "J A N E   D O E"}).
     *
     * <p>Unchanged from 2.3.0, character for character, and kept so
     * that code written against it keeps compiling and keeps producing
     * the same strings. No built-in preset calls it any more.</p>
     *
     * @param value source text (null tolerated, returned as empty)
     * @return spaced-caps representation
     * @deprecated since 2.4.0; removed in 3.0. Use {@link #upper(String)}
     *             for the text and carry the spacing on the style instead
     *             &mdash; {@link #SPACED_CAPS}, or any
     *             {@link DocumentLetterSpacing} you prefer. The reason is
     *             not style: padding the string is what stored a name as
     *             {@code "J A N E   D O E"}, so the one field a CV is
     *             searched and parsed by came back out of the file
     *             unreadable. The replacement draws spaced caps and leaves
     *             the text alone. It does not reproduce this method's
     *             metrics exactly &mdash; a whole space glyph per gap is
     *             wider than editorial tracking &mdash; so expect the same
     *             look at a slightly different width.
     */
    @Deprecated(since = "2.4.0", forRemoval = true)
    public static String spacedUpper(String value) {
        if (value == null) {
            return "";
        }
        String upper = value.toUpperCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(upper.length() * 2);
        for (int i = 0; i < upper.length(); i++) {
            char current = upper.charAt(i);
            out.append(current);
            if (Character.isLetterOrDigit(current)
                && i + 1 < upper.length()
                && Character.isLetterOrDigit(upper.charAt(i + 1))) {
                out.append(' ');
            } else if (Character.isWhitespace(current)) {
                out.append("  ");
            }
        }
        return out.toString();
    }

    /**
     * Joins the non-blank parts with a {@code " | "} pipe separator
     * (e.g. {@code joinPipe("London", "", "+44") -> "London | +44"}).
     * Null / blank parts are skipped; each kept part is trimmed. Used to
     * build single-line contact/meta strings in headers.
     *
     * @param parts ordered parts (null / blank entries ignored)
     * @return pipe-joined string, empty when no non-blank parts
     */
    public static String joinPipe(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(part.trim());
        }
        return sb.toString();
    }
}
