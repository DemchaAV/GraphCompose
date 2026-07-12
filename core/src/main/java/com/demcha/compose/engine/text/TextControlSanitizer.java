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
