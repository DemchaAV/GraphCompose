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

    private static BidiParagraphResolver.BaseDirection baseOf(boolean rightToLeft) {
        return rightToLeft
                ? BidiParagraphResolver.BaseDirection.RIGHT_TO_LEFT
                : BidiParagraphResolver.BaseDirection.LEFT_TO_RIGHT;
    }

    /**
     * Mirrors only the characters that resolve to a right-to-left level, keeping
     * logical order.
     *
     * <p>For a viewer that runs the bidirectional algorithm itself — PowerPoint — the
     * text must stay logical: reordering is a property of strong right-to-left
     * characters, not of a declared direction, so a pre-reordered string would have its
     * Hebrew re-reversed on display. What such a viewer was measured not to do is
     * UAX&nbsp;#9's L4, so the paired punctuation is swapped for it here — but only
     * where L4 itself would apply. A run at the left-to-right level keeps its brackets
     * and comparisons exactly as typed; mirroring those, as the whole-string swap did,
     * turns {@code a > b} into {@code a < b} in the only copy of the text the file
     * has.</p>
     *
     * <p>For a single-level right-to-left run this is {@link BidiMirroring#mirror},
     * unchanged.</p>
     *
     * @param text            run text in logical order
     * @param rightToLeftBase the run's own base direction — the level its neutrals
     *                        resolve against, which for a chip is the direction the
     *                        line gave it
     * @return the same order with paired punctuation at right-to-left levels swapped
     */
    public static String mirrorRightToLeftLevels(String text, boolean rightToLeftBase) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int[] levels = BidiParagraphResolver.levelsFor(text, baseOf(rightToLeftBase));
        if (levels.length == 0) {
            return text;
        }
        StringBuilder mirrored = new StringBuilder(text.length());
        int from = 0;
        while (from < text.length()) {
            int level = levels[from];
            int to = from + 1;
            while (to < text.length() && levels[to] == level) {
                to++;
            }
            String run = text.substring(from, to);
            mirrored.append(BidiParagraphResolver.isRightToLeftLevel(level)
                    ? BidiMirroring.mirror(run)
                    : run);
            from = to;
        }
        return mirrored.toString();
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
     * <p>The base direction is the run's own, not the paragraph's. A chip takes its
     * direction from its first character, and that character decides only where the
     * chip sits in the line: one opening on Latin is a left-to-right run that may
     * still hold Hebrew, and resolving it against the wrong base puts that Hebrew on
     * the wrong side of what surrounds it.</p>
     *
     * @param text            run text in logical order
     * @param rightToLeftBase the run's own base direction
     * @return the text as a left-to-right drawing order
     */
    public static String visualize(String text, boolean rightToLeftBase) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int[] levels = BidiParagraphResolver.levelsFor(text, baseOf(rightToLeftBase));
        if (levels.length == 0) {
            return text;
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
