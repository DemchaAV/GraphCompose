package com.demcha.compose.engine.text;

/**
 * Internal text cleanup helper for layout and render hot paths.
 *
 * <p>This class only removes or replaces Unicode category C code points. It
 * intentionally does not normalize punctuation or replace visible symbols such
 * as bullets, because those glyph decisions belong to the font/PDF safety
 * sanitizer.</p>
 */
public final class TextControlSanitizer {
    private TextControlSanitizer() {
    }

    /**
     * Removes Unicode category C code points from a text run.
     *
     * @param text source text
     * @return sanitized text, never {@code null}
     */
    public static String remove(String text) {
        return sanitize(text, "");
    }

    /**
     * Replaces Unicode category C code points with a caller-supplied value.
     *
     * @param text source text
     * @param replacement replacement for each removed code point
     * @return sanitized text, never {@code null}
     */
    public static String replace(String text, String replacement) {
        return sanitize(text, replacement == null ? "" : replacement);
    }

    /**
     * Removes Unicode category C code points except the bidirectional formatting
     * characters, which are kept for the layout pipeline to read.
     *
     * <p>The direction marks and isolates (U+061C, U+200E, U+200F, U+202A..U+202E,
     * U+2066..U+2069) draw nothing — they exist to steer the Unicode Bidirectional
     * Algorithm. Removing them with the rest of category C would delete the author's
     * only way to say which direction a neutral stretch of text belongs to, before
     * anything had a chance to act on it.</p>
     *
     * @param text source text
     * @return sanitized text keeping the bidirectional formatting characters, never {@code null}
     * @since 2.2.0
     */
    public static String removeExceptDirectionMarks(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder sanitized = null;
        for (int inputIndex = 0; inputIndex < text.length(); ) {
            int codePoint = text.codePointAt(inputIndex);
            int charCount = Character.charCount(codePoint);
            if (isCategoryC(codePoint) && !isBidiControl(codePoint)) {
                if (sanitized == null) {
                    sanitized = new StringBuilder(text.length());
                    sanitized.append(text, 0, inputIndex);
                }
            } else if (sanitized != null) {
                sanitized.appendCodePoint(codePoint);
            }
            inputIndex += charCount;
        }

        return sanitized == null ? text : sanitized.toString();
    }

    /**
     * Removes only the bidirectional formatting characters, leaving everything else.
     *
     * <p>Used once the algorithm has read them, so the text handed to a backend
     * carries no zero-width steering characters.</p>
     *
     * @param text source text
     * @return text without bidirectional formatting characters, never {@code null}
     * @since 2.2.0
     */
    public static String removeDirectionMarks(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder sanitized = null;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (isBidiControl(character)) {
                if (sanitized == null) {
                    sanitized = new StringBuilder(text.length());
                    sanitized.append(text, 0, index);
                }
            } else if (sanitized != null) {
                sanitized.append(character);
            }
        }

        return sanitized == null ? text : sanitized.toString();
    }

    /**
     * Returns whether a code point is a bidirectional formatting character — a
     * direction mark, an embedding, an override, or an isolate.
     *
     * @param codePoint code point to test
     * @return {@code true} for a bidirectional formatting character
     * @since 2.2.0
     */
    public static boolean isBidiControl(int codePoint) {
        return codePoint == 0x061C
                || codePoint == 0x200E
                || codePoint == 0x200F
                || (codePoint >= 0x202A && codePoint <= 0x202E)
                || (codePoint >= 0x2066 && codePoint <= 0x2069);
    }

    private static String sanitize(String text, String replacement) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder sanitized = null;
        for (int inputIndex = 0; inputIndex < text.length(); ) {
            int codePoint = text.codePointAt(inputIndex);
            int charCount = Character.charCount(codePoint);
            if (isCategoryC(codePoint)) {
                if (sanitized == null) {
                    sanitized = new StringBuilder(text.length());
                    sanitized.append(text, 0, inputIndex);
                }
                sanitized.append(replacement);
            } else if (sanitized != null) {
                sanitized.appendCodePoint(codePoint);
            }
            inputIndex += charCount;
        }

        return sanitized == null ? text : sanitized.toString();
    }

    private static boolean isCategoryC(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.CONTROL
                || type == Character.FORMAT
                || type == Character.PRIVATE_USE
                || type == Character.SURROGATE
                || type == Character.UNASSIGNED;
    }
}
