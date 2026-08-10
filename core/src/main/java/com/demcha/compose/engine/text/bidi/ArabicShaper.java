package com.demcha.compose.engine.text.bidi;

import com.demcha.compose.engine.text.TextControlSanitizer;

/**
 * Maps Arabic letters to their contextual presentation forms.
 *
 * <p>Arabic letters change shape by position — isolated, initial, medial, final — and
 * a font performs that substitution through OpenType {@code GSUB}, which a PDF content
 * stream never executes: {@code showText} walks the font's {@code cmap} and nothing
 * else. The one way to reach the joined forms there is to hand the font their code
 * points directly, from the Arabic Presentation Forms-B block that compatibility-era
 * fonts carry. That block is why the bundled Arabic family was chosen the way it was.</p>
 *
 * <p>Shaping is a pure text-to-text mapping and runs before measurement, because the
 * forms have their own advance widths and the engine's contract is that what is
 * measured is what is drawn. It is idempotent — the forms lie outside the base block,
 * so shaped text passes through unchanged — and a string with no Arabic in it is
 * returned as the same instance after one early-exiting scan.</p>
 *
 * <p>Vowel points and the bidirectional format characters are transparent: they sit
 * between letters without breaking the join, exactly as Unicode's joining rules say,
 * and stay in the output for the later seams that know what to do with them.</p>
 *
 * <p>Ownership: shared engine foundation.</p>
 *
 * @since 2.2.0
 */
public final class ArabicShaper {

    private ArabicShaper() {
    }

    /** First code point of the table: U+0621, hamza. */
    private static final int BASE_FIRST = 0x0621;
    /** Last code point of the table: U+064A, yeh. */
    private static final int BASE_LAST = 0x064A;

    /** Joining behaviour of a letter. */
    private static final int NON_JOINING = 0;
    /** Joins only to the letter before it (isolated and final forms exist). */
    private static final int RIGHT_JOINING = 1;
    /** Joins on both sides (all four forms exist). */
    private static final int DUAL_JOINING = 2;

    /**
     * Per-letter table, indexed by {@code codePoint - BASE_FIRST}:
     * {@code {joining, isolated, final, initial, medial}}; a zero form does not exist
     * for that letter. U+0640 tatweel is dual-joining and its own form in every
     * position. The gap U+063B..U+063F is unassigned in this block.
     */
    private static final int[][] LETTERS = new int[BASE_LAST - BASE_FIRST + 1][];

    private static void letter(int codePoint, int joining, int isolated, int fin, int ini, int med) {
        LETTERS[codePoint - BASE_FIRST] = new int[]{joining, isolated, fin, ini, med};
    }

    static {
        letter(0x0621, NON_JOINING, 0xFE80, 0, 0, 0);
        letter(0x0622, RIGHT_JOINING, 0xFE81, 0xFE82, 0, 0);
        letter(0x0623, RIGHT_JOINING, 0xFE83, 0xFE84, 0, 0);
        letter(0x0624, RIGHT_JOINING, 0xFE85, 0xFE86, 0, 0);
        letter(0x0625, RIGHT_JOINING, 0xFE87, 0xFE88, 0, 0);
        letter(0x0626, DUAL_JOINING, 0xFE89, 0xFE8A, 0xFE8B, 0xFE8C);
        letter(0x0627, RIGHT_JOINING, 0xFE8D, 0xFE8E, 0, 0);
        letter(0x0628, DUAL_JOINING, 0xFE8F, 0xFE90, 0xFE91, 0xFE92);
        letter(0x0629, RIGHT_JOINING, 0xFE93, 0xFE94, 0, 0);
        letter(0x062A, DUAL_JOINING, 0xFE95, 0xFE96, 0xFE97, 0xFE98);
        letter(0x062B, DUAL_JOINING, 0xFE99, 0xFE9A, 0xFE9B, 0xFE9C);
        letter(0x062C, DUAL_JOINING, 0xFE9D, 0xFE9E, 0xFE9F, 0xFEA0);
        letter(0x062D, DUAL_JOINING, 0xFEA1, 0xFEA2, 0xFEA3, 0xFEA4);
        letter(0x062E, DUAL_JOINING, 0xFEA5, 0xFEA6, 0xFEA7, 0xFEA8);
        letter(0x062F, RIGHT_JOINING, 0xFEA9, 0xFEAA, 0, 0);
        letter(0x0630, RIGHT_JOINING, 0xFEAB, 0xFEAC, 0, 0);
        letter(0x0631, RIGHT_JOINING, 0xFEAD, 0xFEAE, 0, 0);
        letter(0x0632, RIGHT_JOINING, 0xFEAF, 0xFEB0, 0, 0);
        letter(0x0633, DUAL_JOINING, 0xFEB1, 0xFEB2, 0xFEB3, 0xFEB4);
        letter(0x0634, DUAL_JOINING, 0xFEB5, 0xFEB6, 0xFEB7, 0xFEB8);
        letter(0x0635, DUAL_JOINING, 0xFEB9, 0xFEBA, 0xFEBB, 0xFEBC);
        letter(0x0636, DUAL_JOINING, 0xFEBD, 0xFEBE, 0xFEBF, 0xFEC0);
        letter(0x0637, DUAL_JOINING, 0xFEC1, 0xFEC2, 0xFEC3, 0xFEC4);
        letter(0x0638, DUAL_JOINING, 0xFEC5, 0xFEC6, 0xFEC7, 0xFEC8);
        letter(0x0639, DUAL_JOINING, 0xFEC9, 0xFECA, 0xFECB, 0xFECC);
        letter(0x063A, DUAL_JOINING, 0xFECD, 0xFECE, 0xFECF, 0xFED0);
        letter(0x0640, DUAL_JOINING, 0x0640, 0x0640, 0x0640, 0x0640);
        letter(0x0641, DUAL_JOINING, 0xFED1, 0xFED2, 0xFED3, 0xFED4);
        letter(0x0642, DUAL_JOINING, 0xFED5, 0xFED6, 0xFED7, 0xFED8);
        letter(0x0643, DUAL_JOINING, 0xFED9, 0xFEDA, 0xFEDB, 0xFEDC);
        letter(0x0644, DUAL_JOINING, 0xFEDD, 0xFEDE, 0xFEDF, 0xFEE0);
        letter(0x0645, DUAL_JOINING, 0xFEE1, 0xFEE2, 0xFEE3, 0xFEE4);
        letter(0x0646, DUAL_JOINING, 0xFEE5, 0xFEE6, 0xFEE7, 0xFEE8);
        letter(0x0647, DUAL_JOINING, 0xFEE9, 0xFEEA, 0xFEEB, 0xFEEC);
        letter(0x0648, RIGHT_JOINING, 0xFEED, 0xFEEE, 0, 0);
        // Unicode classifies alef maksura as dual-joining (group YEH), but its
        // initial/medial forms live in Presentation Forms-A, outside the block this
        // table maps to — and the letter is word-final in standard Arabic. Kept
        // right-joining deliberately; do not "fix" it against ArabicShaping.txt.
        letter(0x0649, RIGHT_JOINING, 0xFEEF, 0xFEF0, 0, 0);
        letter(0x064A, DUAL_JOINING, 0xFEF1, 0xFEF2, 0xFEF3, 0xFEF4);
    }

    /**
     * Lam-alef ligatures: lam followed by an alef variant becomes one glyph,
     * {@code {alef, isolatedForm, finalForm}} per variant.
     */
    private static final int[][] LAM_ALEF = {
            {0x0622, 0xFEF5, 0xFEF6},
            {0x0623, 0xFEF7, 0xFEF8},
            {0x0625, 0xFEF9, 0xFEFA},
            {0x0627, 0xFEFB, 0xFEFC},
    };

    private static final int LAM = 0x0644;

    /**
     * Shapes every Arabic letter in the text to its contextual presentation form.
     *
     * @param text text in logical order
     * @return the shaped text, or the same instance when there is nothing to shape
     */
    public static String shape(String text) {
        if (text == null || text.isEmpty() || !containsArabicLetter(text)) {
            return text == null ? "" : text;
        }

        StringBuilder shaped = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            int[] letter = letterAt(character);
            if (letter == null) {
                shaped.append(character);
                continue;
            }

            if (character == LAM && index + 1 < text.length()) {
                // The ligature is formed only for an adjacent pair. A vowel point
                // between lam and alef belongs to the lam; folding the pair into one
                // glyph would strand the mark after it, and the inverse mapping the
                // slide backend relies on could no longer put it back where the
                // author wrote it. A vocalized lam-alef renders as two joined
                // letters instead — unligated, and exactly restorable.
                int[] ligature = lamAlefFor(text.charAt(index + 1));
                if (ligature != null) {
                    shaped.append((char) (joinsPrevious(text, index) ? ligature[2] : ligature[1]));
                    index++;
                    continue;
                }
            }

            boolean joinsPrevious = joinsPrevious(text, index);
            boolean joinsNext = joinsNext(text, index);
            int form;
            if (joinsPrevious && joinsNext && letter[4] != 0) {
                form = letter[4];
            } else if (joinsPrevious && letter[2] != 0) {
                form = letter[2];
            } else if (joinsNext && letter[3] != 0) {
                form = letter[3];
            } else {
                form = letter[1];
            }
            shaped.append((char) form);
        }
        return shaped.toString();
    }

    /**
     * Returns the base letters a presentation form stands for, or {@code null} when the
     * code point is not one this shaper produces.
     *
     * <p>This is the degradation path: a font without the form can still draw the base
     * letter — unjoined but readable — where substituting a {@code '?'} would lose the
     * text entirely. A lam-alef ligature decomposes back into its two letters.</p>
     *
     * @param codePoint a presentation-form code point
     * @return the base letters in logical order, or {@code null}
     */
    public static String baseLettersOf(int codePoint) {
        for (int[] ligature : LAM_ALEF) {
            if (codePoint == ligature[1] || codePoint == ligature[2]) {
                return new String(new char[]{(char) LAM, (char) ligature[0]});
            }
        }
        for (int base = BASE_FIRST; base <= BASE_LAST; base++) {
            int[] letter = LETTERS[base - BASE_FIRST];
            if (letter == null) {
                continue;
            }
            if (codePoint == letter[1] || codePoint == letter[2]
                    || codePoint == letter[3] || codePoint == letter[4]) {
                return String.valueOf((char) base);
            }
        }
        return null;
    }

    /**
     * Maps every presentation form in the text back to its base letters.
     *
     * <p>For the backend whose display engine shapes Arabic itself: handing it the
     * forms would freeze the letters into this shaper's choices and put compatibility
     * code points into a file a user may search or copy from. Text without a form
     * passes through as the same instance.</p>
     *
     * @param text shaped text
     * @return the text with base letters restored
     */
    public static String toBaseLetters(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        StringBuilder restored = null;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            String base = character >= 0xFE80 && character <= 0xFEFC
                    ? baseLettersOf(character)
                    : null;
            if (base != null && restored == null) {
                restored = new StringBuilder(text.length() + 4);
                restored.append(text, 0, index);
            }
            if (restored != null) {
                if (base != null) {
                    restored.append(base);
                } else {
                    restored.append(character);
                }
            }
        }
        return restored == null ? text : restored.toString();
    }

    private static boolean containsArabicLetter(String text) {
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character >= BASE_FIRST && character <= BASE_LAST && letterAt(character) != null) {
                return true;
            }
        }
        return false;
    }

    private static int[] letterAt(char character) {
        if (character < BASE_FIRST || character > BASE_LAST) {
            return null;
        }
        return LETTERS[character - BASE_FIRST];
    }

    /**
     * Transparent characters sit between letters without affecting the join: the
     * vowel points and marks of the Arabic block, and the bidirectional format
     * characters that survive sanitizing until the layout has read them.
     */
    private static boolean isTransparent(char character) {
        return (character >= 0x064B && character <= 0x065F)
                || character == 0x0670
                || (character >= 0x06D6 && character <= 0x06ED)
                || TextControlSanitizer.isBidiControl(character);
    }

    /** The nearest non-transparent character before {@code index} joins forward. */
    private static boolean joinsPrevious(String text, int index) {
        for (int before = index - 1; before >= 0; before--) {
            char character = text.charAt(before);
            if (isTransparent(character)) {
                continue;
            }
            int[] letter = letterAt(character);
            return letter != null && letter[0] == DUAL_JOINING;
        }
        return false;
    }

    /** This letter joins forward and the nearest non-transparent character can receive it. */
    private static boolean joinsNext(String text, int index) {
        int[] self = letterAt(text.charAt(index));
        if (self == null || self[0] != DUAL_JOINING) {
            return false;
        }
        int after = nextOpaqueIndex(text, index);
        if (after < 0) {
            return false;
        }
        int[] next = letterAt(text.charAt(after));
        return next != null && next[0] != NON_JOINING;
    }

    /** Index of the nearest non-transparent character after {@code index}, or -1. */
    private static int nextOpaqueIndex(String text, int index) {
        for (int after = index + 1; after < text.length(); after++) {
            if (!isTransparent(text.charAt(after))) {
                return after;
            }
        }
        return -1;
    }

    private static int[] lamAlefFor(char character) {
        for (int[] ligature : LAM_ALEF) {
            if (character == ligature[0]) {
                return ligature;
            }
        }
        return null;
    }
}
