package com.demcha.compose.document.templates.core.text;

import java.util.Locale;

/**
 * Pure text-transform helpers used by the v2 components. Currently
 * one entry: letter-spaced uppercase rendering for the document
 * headline and section banners.
 *
 * <p>These are <strong>algorithmic</strong>, not cosmetic — they do
 * not belong in {@code theme}. The visual effect of "space letters
 * apart" is structural: even if a theme picked a different banner
 * colour, the letters would still need the same spacing logic.</p>
 */
public final class TextOrnaments {

    /** Letter gap inside a word: a space the line wrapper cannot break on. */
    private static final char NON_BREAKING_SPACE = '\u00A0';

    private TextOrnaments() {
    }

    /**
     * Letter-spaced uppercase rendering (e.g.
     * {@code spacedUpper("Jane Doe") -> "J A N E   D O E"}).
     *
     * <p>The gap between two letters of one word is a non-breaking space.
     * Wrapping breaks on whitespace and every letter here is separated by
     * some, so an ordinary space lets a heading too wide for its column break
     * anywhere: "EDUCATION &amp; CERTIFICATIONS" came out as "EDUCATION &amp;
     * CERTI / FICATIONS" in a sidebar. {@code Character.isWhitespace} is false
     * for U+00A0, so a break falls between words wherever one exists — a word
     * wider than the column is still split, as it has to be. The glyph is the
     * same space, so a heading that already fitted does not move.</p>
     *
     * @param value source text (null tolerated, returned as empty)
     * @return spaced-caps representation
     */
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
                out.append(NON_BREAKING_SPACE);
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
