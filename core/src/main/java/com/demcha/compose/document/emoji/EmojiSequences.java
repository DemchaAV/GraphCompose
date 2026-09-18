package com.demcha.compose.document.emoji;

import java.util.Arrays;

/**
 * Spells a glyph key of an emoji set as the emoji the glyph depicts.
 *
 * <p>A glyph key is the emoji's codepoints in hex, joined by {@code '-'} —
 * {@code 1f469-200d-1f4bb} — with U+FE0F dropped, because Noto names its files
 * without it and the key is a file name first. As text that loses something.
 * U+FE0F asks for the colour presentation of a character whose default is plain
 * text: {@code U+2764} alone is a heart in the text font, and a messenger can
 * paste it as a black symbol; {@code U+2764 U+FE0F} is the red heart.</p>
 *
 * <p>The form to restore is the one Unicode calls fully qualified (UTS #51):
 * every emoji character whose default presentation is text is followed by
 * U+FE0F, unless an emoji modifier (a skin tone) follows it, which qualifies it
 * already, or the key already carries a presentation selector. That needs one
 * fact per character — whether its default presentation is text — held in
 * {@link #TEXT_DEFAULT}. The table is Unicode data copied into source, so it is
 * checked against the JDK's own copy: a Unicode update that changes the set
 * fails {@code EmojiSequencesTest} when it runs on a JDK that ships it.</p>
 */
final class EmojiSequences {

    /** VARIATION SELECTOR-16: emoji presentation. */
    private static final int EMOJI_PRESENTATION = 0xFE0F;

    /** VARIATION SELECTOR-15: text presentation. */
    private static final int TEXT_PRESENTATION = 0xFE0E;

    /**
     * Every codepoint with {@code Emoji=Yes} and {@code Emoji_Presentation=No}
     * in the Unicode emoji data, ascending. Generated from
     * {@code Character.isEmoji} / {@code Character.isEmojiPresentation} (JDK 21+;
     * identical on JDK 23 and 24); {@code EmojiSequencesTest} compares it with
     * the running JDK whenever the JDK has those methods.
     */
    static final int[] TEXT_DEFAULT = {
            0x0023, 0x002A, 0x0030, 0x0031, 0x0032, 0x0033, 0x0034, 0x0035, 0x0036,
            0x0037, 0x0038, 0x0039, 0x00A9, 0x00AE, 0x203C, 0x2049, 0x2122, 0x2139,
            0x2194, 0x2195, 0x2196, 0x2197, 0x2198, 0x2199, 0x21A9, 0x21AA, 0x2328,
            0x23CF, 0x23ED, 0x23EE, 0x23EF, 0x23F1, 0x23F2, 0x23F8, 0x23F9, 0x23FA,
            0x24C2, 0x25AA, 0x25AB, 0x25B6, 0x25C0, 0x25FB, 0x25FC, 0x2600, 0x2601,
            0x2602, 0x2603, 0x2604, 0x260E, 0x2611, 0x2618, 0x261D, 0x2620, 0x2622,
            0x2623, 0x2626, 0x262A, 0x262E, 0x262F, 0x2638, 0x2639, 0x263A, 0x2640,
            0x2642, 0x265F, 0x2660, 0x2663, 0x2665, 0x2666, 0x2668, 0x267B, 0x267E,
            0x2692, 0x2694, 0x2695, 0x2696, 0x2697, 0x2699, 0x269B, 0x269C, 0x26A0,
            0x26A7, 0x26B0, 0x26B1, 0x26C8, 0x26CF, 0x26D1, 0x26D3, 0x26E9, 0x26F0,
            0x26F1, 0x26F4, 0x26F7, 0x26F8, 0x26F9, 0x2702, 0x2708, 0x2709, 0x270C,
            0x270D, 0x270F, 0x2712, 0x2714, 0x2716, 0x271D, 0x2721, 0x2733, 0x2734,
            0x2744, 0x2747, 0x2763, 0x2764, 0x27A1, 0x2934, 0x2935, 0x2B05, 0x2B06,
            0x2B07, 0x3030, 0x303D, 0x3297, 0x3299, 0x1F170, 0x1F171, 0x1F17E, 0x1F17F,
            0x1F202, 0x1F237, 0x1F321, 0x1F324, 0x1F325, 0x1F326, 0x1F327, 0x1F328, 0x1F329,
            0x1F32A, 0x1F32B, 0x1F32C, 0x1F336, 0x1F37D, 0x1F396, 0x1F397, 0x1F399, 0x1F39A,
            0x1F39B, 0x1F39E, 0x1F39F, 0x1F3CB, 0x1F3CC, 0x1F3CD, 0x1F3CE, 0x1F3D4, 0x1F3D5,
            0x1F3D6, 0x1F3D7, 0x1F3D8, 0x1F3D9, 0x1F3DA, 0x1F3DB, 0x1F3DC, 0x1F3DD, 0x1F3DE,
            0x1F3DF, 0x1F3F3, 0x1F3F5, 0x1F3F7, 0x1F43F, 0x1F441, 0x1F4FD, 0x1F549, 0x1F54A,
            0x1F56F, 0x1F570, 0x1F573, 0x1F574, 0x1F575, 0x1F576, 0x1F577, 0x1F578, 0x1F579,
            0x1F587, 0x1F58A, 0x1F58B, 0x1F58C, 0x1F58D, 0x1F590, 0x1F5A5, 0x1F5A8, 0x1F5B1,
            0x1F5B2, 0x1F5BC, 0x1F5C2, 0x1F5C3, 0x1F5C4, 0x1F5D1, 0x1F5D2, 0x1F5D3, 0x1F5DC,
            0x1F5DD, 0x1F5DE, 0x1F5E1, 0x1F5E3, 0x1F5E8, 0x1F5EF, 0x1F5F3, 0x1F5FA, 0x1F6CB,
            0x1F6CD, 0x1F6CE, 0x1F6CF, 0x1F6E0, 0x1F6E1, 0x1F6E2, 0x1F6E3, 0x1F6E4, 0x1F6E5,
            0x1F6E9, 0x1F6F0, 0x1F6F3
    };

    private EmojiSequences() {
    }

    /**
     * Spells a glyph key as fully-qualified emoji text.
     *
     * @param key the glyph key, e.g. {@code 2764} or {@code 1f469-200d-1f4bb}
     * @return the emoji as text, or {@code null} when the key is not a
     * sequence of hex codepoints
     */
    static String fullyQualified(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String[] parts = key.trim().split("-");
        int[] codepoints = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                codepoints[i] = Integer.parseInt(parts[i], 16);
            } catch (NumberFormatException e) {
                return null;
            }
            if (!Character.isValidCodePoint(codepoints[i])) {
                return null;
            }
        }
        StringBuilder text = new StringBuilder(codepoints.length * 3);
        for (int i = 0; i < codepoints.length; i++) {
            int codepoint = codepoints[i];
            text.appendCodePoint(codepoint);
            int next = i + 1 < codepoints.length ? codepoints[i + 1] : -1;
            if (isTextDefault(codepoint)
                    && next != EMOJI_PRESENTATION
                    && next != TEXT_PRESENTATION
                    && !isModifier(next)) {
                text.appendCodePoint(EMOJI_PRESENTATION);
            }
        }
        return text.toString();
    }

    static boolean isTextDefault(int codepoint) {
        return Arrays.binarySearch(TEXT_DEFAULT, codepoint) >= 0;
    }

    /** The five skin-tone modifiers, U+1F3FB..U+1F3FF. */
    private static boolean isModifier(int codepoint) {
        return codepoint >= 0x1F3FB && codepoint <= 0x1F3FF;
    }
}
