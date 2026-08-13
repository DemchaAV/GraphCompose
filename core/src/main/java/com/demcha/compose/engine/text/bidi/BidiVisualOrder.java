package com.demcha.compose.engine.text.bidi;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns one atomic right-to-left run into the exact string a backend draws, level by
 * level.
 *
 * <p>A plain span never needs this: the wrapper splits it wherever its characters
 * resolve to a different embedding level, so each piece is single-level and reversing
 * it whole is correct. An inline chip cannot be split — it is one rounded fill — so it
 * reaches the backend carrying its first character's level and whatever its interior
 * holds. Reversing <em>that</em> whole is wrong the moment the interior sits at the
 * opposite level: a chip reading {@code (a > b)} after a Hebrew word was drawn as
 * {@code (b < a)}, the comparison inverted, because the left-to-right interior was
 * reversed and mirrored along with the brackets that enclose it.</p>
 *
 * <p>This class re-resolves the run's own levels (right-to-left base, matching the
 * flag the caller holds), reorders the level runs visually, and reverses and mirrors
 * only the right-to-left ones — UAX #9's L2 and L4 applied to the one span the wrapper
 * could not split. For a single-level run the result is identical to reversing and
 * mirroring the whole string, so the transform is safe for every chip, not only the
 * mixed ones.</p>
 *
 * <p>Ownership: shared engine foundation.</p>
 *
 * @since 2.2.0
 */
public final class BidiVisualOrder {

    private BidiVisualOrder() {
    }

    /**
     * Returns whether a right-to-left run holds characters at both directions' levels.
     *
     * <p>A backend with a bidirectional engine of its own — PowerPoint — handles a
     * single-level run correctly when handed logical text and the run's direction. A
     * mixed run is what it cannot be trusted with: it places the levels itself but does
     * not mirror the neutrals it moves, so the caller must hand it {@link #visualize
     * settled text} instead.</p>
     *
     * @param text run text in logical order
     * @return {@code true} when the text resolves to more than one embedding level
     */
    public static boolean mixesDirections(String text) {
        int[] levels = BidiParagraphResolver.levelsFor(
                text, BidiParagraphResolver.BaseDirection.RIGHT_TO_LEFT);
        if (levels.length == 0) {
            return false;
        }
        for (int index = 1; index < levels.length; index++) {
            if (levels[index] != levels[0]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves one right-to-left run into visual order, mirroring what moves.
     *
     * <p>Level runs are reordered as UAX #9 orders them; a right-to-left run is
     * reversed by grapheme cluster and its paired punctuation mirrored, a
     * left-to-right run passes through as written. Text that resolves to no
     * right-to-left level at all is returned unchanged — drawing it as written is
     * already correct.</p>
     *
     * @param text run text in logical order
     * @return the text as a left-to-right drawing order
     */
    public static String visualize(String text) {
        int[] levels = BidiParagraphResolver.levelsFor(
                text, BidiParagraphResolver.BaseDirection.RIGHT_TO_LEFT);
        if (levels.length == 0) {
            return text == null ? "" : text;
        }

        List<String> runs = new ArrayList<>();
        List<Integer> runLevels = new ArrayList<>();
        int from = 0;
        while (from < text.length()) {
            int level = levels[from];
            int to = from + 1;
            while (to < text.length() && levels[to] == level) {
                to++;
            }
            runs.add(text.substring(from, to));
            runLevels.add(level);
            from = to;
        }

        int[] levelArray = new int[runLevels.size()];
        for (int index = 0; index < levelArray.length; index++) {
            levelArray[index] = runLevels.get(index);
        }
        int[] order = BidiParagraphResolver.visualOrder(levelArray);

        StringBuilder visual = new StringBuilder(text.length());
        for (int position = 0; position < runs.size(); position++) {
            int logical = order.length == 0 ? position : order[position];
            String run = runs.get(logical);
            visual.append(BidiParagraphResolver.isRightToLeftLevel(levelArray[logical])
                    ? BidiMirroring.mirror(BidiText.reverseForDisplay(run))
                    : run);
        }
        return visual.toString();
    }
}
