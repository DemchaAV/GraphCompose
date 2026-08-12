package com.demcha.compose.engine.text.bidi;

/**
 * Swaps paired punctuation for text drawn right to left.
 *
 * <p>An opening bracket is the one that faces the text that follows it — which side of
 * the glyph that is depends on which way the line runs. In a right-to-left run the
 * characters keep their logical identity ({@code U+0028} is still "open parenthesis")
 * but the shape a reader sees must mirror, or every parenthesis in Hebrew text faces
 * away from what it encloses. UAX #9 calls this rule L4.</p>
 *
 * <p>The JDK says whether a character mirrors ({@link Character#isMirrored}) but not
 * what it mirrors to, so the pairs are ours. The set covers the pairs that occur in
 * documents — parentheses, brackets, braces, angle brackets and guillemets — rather
 * than the full Unicode BidiBrackets table; an unpaired mirrored character passes
 * through unchanged, which draws it un-mirrored rather than dropping it.</p>
 *
 * <p>Ownership: shared engine foundation.</p>
 *
 * @since 2.2.0
 */
public final class BidiMirroring {

    private BidiMirroring() {
    }

    /**
     * Mirrors the paired punctuation of one right-to-left run.
     *
     * @param text run text in logical order
     * @return the text with pairs swapped, or the same instance when nothing mirrors
     */
    public static String mirror(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }

        StringBuilder mirrored = null;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            char swapped = mirrorOf(character);
            if (swapped != character && mirrored == null) {
                mirrored = new StringBuilder(text.length());
                mirrored.append(text, 0, index);
            }
            if (mirrored != null) {
                mirrored.append(swapped);
            }
        }
        return mirrored == null ? text : mirrored.toString();
    }

    private static char mirrorOf(char character) {
        return switch (character) {
            case '(' -> ')';
            case ')' -> '(';
            case '[' -> ']';
            case ']' -> '[';
            case '{' -> '}';
            case '}' -> '{';
            case '<' -> '>';
            case '>' -> '<';
            case '«' -> '»';
            case '»' -> '«';
            case '‹' -> '›';
            case '›' -> '‹';
            default -> character;
        };
    }
}
