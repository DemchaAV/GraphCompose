package com.demcha.compose.engine.components.content.header_footer;

import java.util.Locale;

/**
 * Engine-local page-number style. Owns the numeral formatting for header/footer
 * {@code {page}} / {@code {pages}} tokens; the canonical
 * {@code DocumentPageNumberStyle} is a thin selector mapped onto this at the
 * options-translation boundary (mirroring how the zone enums are mapped).
 *
 * @author Artem Demchyshyn
 */
public enum PageNumberStyle {
    /** Arabic numerals: 1, 2, 3. */
    DECIMAL,
    /** Lowercase Roman numerals: i, ii, iii. */
    LOWER_ROMAN,
    /** Uppercase Roman numerals: I, II, III. */
    UPPER_ROMAN,
    /** Lowercase letters: a, b, c. */
    LOWER_ALPHA,
    /** Uppercase letters: A, B, C. */
    UPPER_ALPHA;

    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] ROMAN_SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    /**
     * Formats a counted page number in this style. Values outside a style's
     * representable range (Roman 1–3999, alphabetic n &ge; 1) fall back to the
     * decimal string, so rendering never throws.
     *
     * @param n the counted page number
     * @return the formatted numeral
     */
    public String format(int n) {
        return switch (this) {
            case DECIMAL -> String.valueOf(n);
            case LOWER_ROMAN -> inRomanRange(n) ? roman(n).toLowerCase(Locale.ROOT) : String.valueOf(n);
            case UPPER_ROMAN -> inRomanRange(n) ? roman(n) : String.valueOf(n);
            case LOWER_ALPHA -> n >= 1 ? alpha(n).toLowerCase(Locale.ROOT) : String.valueOf(n);
            case UPPER_ALPHA -> n >= 1 ? alpha(n) : String.valueOf(n);
        };
    }

    private static boolean inRomanRange(int n) {
        return n >= 1 && n <= 3999;
    }

    private static String roman(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ROMAN_VALUES.length; i++) {
            while (n >= ROMAN_VALUES[i]) {
                sb.append(ROMAN_SYMBOLS[i]);
                n -= ROMAN_VALUES[i];
            }
        }
        return sb.toString();
    }

    /** Bijective base-26: 1 -&gt; A, 26 -&gt; Z, 27 -&gt; AA. */
    private static String alpha(int n) {
        StringBuilder sb = new StringBuilder();
        while (n > 0) {
            int rem = (n - 1) % 26;
            sb.append((char) ('A' + rem));
            n = (n - 1) / 26;
        }
        return sb.reverse().toString();
    }
}
