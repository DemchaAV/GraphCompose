package com.demcha.compose.document.templates.cv.v2.components;

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

    private TextOrnaments() {
    }

    /**
     * Letter-spaced uppercase rendering (e.g.
     * {@code spacedUpper("Jane Doe") -> "J A N E   D O E"}).
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
                out.append(' ');
            } else if (Character.isWhitespace(current)) {
                out.append("  ");
            }
        }
        return out.toString();
    }
}
