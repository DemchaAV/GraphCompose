package com.demcha.compose.engine.text.bidi;

import java.text.Bidi;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns one atomic run into the exact string a backend draws, level by level.
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
 * <p>Both entry points re-resolve the run's own levels against <em>the base the caller
 * passes</em> — the run's direction, which for a chip is its first character's and says
 * only where the chip sits in the line. A left-to-right base does not mean a
 * left-to-right run: Hebrew inside such a chip still resolves to level 1, and a neutral
 * standing between two Hebrew words takes their level too. The base decides how those
 * runs are ordered and where the neutrals fall, not whether any exist.</p>
 *
 * <p>{@link #visualize} applies UAX #9's L2 and L4 — reorder, reverse, mirror — for a
 * backend that draws characters in the order it is handed them. {@link
 * #mirrorRightToLeftLevels} applies L4 alone, for one that runs the algorithm itself
 * and only skips the mirroring. For a single-level run each is identical to the
 * whole-string treatment it replaced, so both are safe for every chip, not only the
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
        if (levels.length == 0 || !isUniformlyRightToLeft(levels)) {
            return text;
        }
        return BidiMirroring.mirror(text);
    }

    /**
     * Whether every character of the run resolves to a right-to-left level.
     *
     * <p>This is the whole condition, and it was measured rather than derived. A viewer
     * that orders the text itself is handed a run and does two things to it: it reorders
     * the levels and it mirrors what UAX&nbsp;#9 L4 mirrors. Which of the two it has
     * already done by the time a glyph is drawn is not something to reason about from the
     * outside — so it was read off a slide, one category of content at a time.</p>
     *
     * <p>A run that is uniformly right-to-left comes back needing the swap: {@code (}
     * Hebrew {@code )} handed over as typed was drawn {@code )}Hebrew{@code (}. A run that
     * mixes levels does not: the brackets of {@code (a > b)}, of {@code (2026)}, and of a
     * chip opening on Latin were all drawn correctly from the text exactly as typed, and
     * pre-swapping them is what produced {@code )a > b(} on a slide. Mirroring per level
     * run — the shape this replaces — is right for the first case and wrong for the rest,
     * because it applies half of a transform whose other half the viewer has already
     * decided not to need.</p>
     */
    private static boolean isUniformlyRightToLeft(int[] levels) {
        for (int level : levels) {
            if (!BidiParagraphResolver.isRightToLeftLevel(level)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Splits a run into its directional runs, ordered the way the page lays them out.
     *
     * <p>{@link BidiParagraphResolver#resolve} answers which runs there are and at what
     * level, in the order they are <em>read</em>. This answers the other half — the order
     * they are <em>placed</em> in, left to right across the page — which is UAX&nbsp;#9's
     * L2 and the one thing a caller that positions each run itself cannot work out from
     * levels alone.</p>
     *
     * <p>It exists for a backend that hands its runs to a consumer with a bidirectional
     * engine of its own. Such a consumer re-resolves whatever string it is given, and it
     * re-resolves it <em>without the line around it</em>: an atomic span handed over whole
     * is a fragment out of context, and the order it comes back in is the order that
     * fragment deserves rather than the one the line does. Placing each directional run in
     * its own box removes the question — a single-level run has nothing left to reorder —
     * and this is the order to place them in.</p>
     *
     * @param text            run text in logical order
     * @param rightToLeftBase the run's own base direction
     * @return the directional runs, first one leftmost; empty for empty text
     * @since 2.2.0
     */
    public static List<BidiParagraphResolver.DirectionalRun> visualRuns(String text,
                                                                       boolean rightToLeftBase) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        List<BidiParagraphResolver.DirectionalRun> logical =
                BidiParagraphResolver.resolve(text, baseOf(rightToLeftBase));
        if (logical.size() <= 1) {
            return logical;
        }
        byte[] levels = new byte[logical.size()];
        Integer[] order = new Integer[logical.size()];
        for (int index = 0; index < logical.size(); index++) {
            levels[index] = (byte) logical.get(index).embeddingLevel();
            order[index] = index;
        }
        Bidi.reorderVisually(levels, 0, order, 0, logical.size());
        List<BidiParagraphResolver.DirectionalRun> visual = new ArrayList<>(logical.size());
        for (Integer index : order) {
            visual.add(logical.get(index));
        }
        return List.copyOf(visual);
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
