package com.demcha.compose.engine.text.bidi;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a right-to-left run from logical order into the order a backend must emit it in.
 *
 * <p>A fixed-layout backend draws the characters of a string in the order it is given
 * them, left to right. Text is stored in logical order — the order it is read — so a
 * right-to-left run has to be reversed before it is drawn, or it appears backwards.</p>
 *
 * <p>Reversed by grapheme cluster, not by character: a Hebrew letter followed by a
 * vowel point, or any base-plus-combining-mark sequence, is one thing on the page. A
 * naive character reversal would move each mark in front of the letter it belongs to,
 * which changes what the reader sees rather than only where it sits.</p>
 *
 * <p>Ownership: shared engine foundation.</p>
 */
public final class BidiText {

    private BidiText() {
    }

    /**
     * Reverses a run for display, keeping each grapheme cluster intact.
     *
     * @param text run text in logical order
     * @return the same clusters in the opposite order
     */
    public static String reverseForDisplay(String text) {
        if (text == null || text.length() <= 1) {
            return text == null ? "" : text;
        }

        BreakIterator clusters = BreakIterator.getCharacterInstance();
        clusters.setText(text);

        List<String> parts = new ArrayList<>();
        int start = clusters.first();
        for (int end = clusters.next(); end != BreakIterator.DONE; start = end, end = clusters.next()) {
            parts.add(text.substring(start, end));
        }

        StringBuilder reversed = new StringBuilder(text.length());
        for (int index = parts.size() - 1; index >= 0; index--) {
            reversed.append(parts.get(index));
        }
        return reversed.toString();
    }
}
