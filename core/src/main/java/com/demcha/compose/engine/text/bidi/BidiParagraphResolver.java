package com.demcha.compose.engine.text.bidi;

import java.text.Bidi;
import java.util.List;

/**
 * Splits a logical line into directional runs and reorders laid-out items for display,
 * following the Unicode Bidirectional Algorithm (UAX #9).
 *
 * <p>Text is authored and stored in <em>logical</em> order — the order it is read in.
 * A page draws in <em>visual</em> order, left to right. For Latin the two coincide,
 * which is why the engine could ignore the distinction until right-to-left scripts
 * arrived; for Hebrew and Arabic they do not, and a mixed line reverses only its
 * right-to-left stretches while numbers and embedded Latin keep running forwards.</p>
 *
 * <p>Resolution runs on the <em>raw</em> line, before control characters are stripped.
 * The explicit direction marks (U+200E LRM, U+200F RLM, U+061C ALM) are Unicode
 * category Cf and carry no glyph, so the sanitizer removes them — but they exist
 * precisely to steer this algorithm, and a resolver that ran afterwards would never
 * see the instruction it was given. Each returned run is sanitized on its own; the
 * marks are zero-width, so dropping them inside a run cannot move a boundary.</p>
 *
 * <p>A line with no right-to-left character resolves to a single left-to-right run
 * without constructing a {@link Bidi} at all, so documents that were laid out before
 * direction existed take exactly the same path through the engine.</p>
 *
 * <p>Ownership: shared engine foundation. Callers translate their own public
 * direction option into a {@link BaseDirection}; this class knows nothing about the
 * document surface.</p>
 */
public final class BidiParagraphResolver {

    private BidiParagraphResolver() {
    }

    /** The paragraph direction a line is resolved against. */
    public enum BaseDirection {
        /** Left to right, regardless of content. */
        LEFT_TO_RIGHT,
        /** Right to left, regardless of content. */
        RIGHT_TO_LEFT,
        /**
         * Taken from the first strong character, falling back to left-to-right when the
         * line has none. Digits are not strong, so {@code "123 שלום"} resolves right to
         * left.
         */
        FIRST_STRONG_CHARACTER
    }

    /**
     * A stretch of a line that runs in one direction.
     *
     * @param text the run's text, in logical order
     * @param embeddingLevel the UAX #9 embedding level; even is left to right
     */
    public record DirectionalRun(String text, int embeddingLevel) {

        /** Returns whether this run is drawn right to left. */
        public boolean isRightToLeft() {
            return isRightToLeftLevel(embeddingLevel);
        }
    }

    /**
     * Splits a logical line into directional runs.
     *
     * @param logicalLine the raw line, before control-character sanitizing
     * @param baseDirection the paragraph direction to resolve against
     * @return the runs in logical order, never empty for a non-empty line
     */
    public static List<DirectionalRun> resolve(String logicalLine, BaseDirection baseDirection) {
        if (logicalLine == null || logicalLine.isEmpty()) {
            return List.of();
        }
        if (!needsResolution(logicalLine, baseDirection)) {
            return List.of(new DirectionalRun(logicalLine, 0));
        }

        Bidi bidi = new Bidi(logicalLine, flagsFor(baseDirection));
        int runCount = bidi.getRunCount();
        if (runCount <= 1) {
            return List.of(new DirectionalRun(logicalLine, bidi.getBaseLevel()));
        }

        DirectionalRun[] runs = new DirectionalRun[runCount];
        for (int run = 0; run < runCount; run++) {
            runs[run] = new DirectionalRun(
                    logicalLine.substring(bidi.getRunStart(run), bidi.getRunLimit(run)),
                    bidi.getRunLevel(run));
        }
        return List.of(runs);
    }

    /**
     * Returns the base embedding level a line resolves to — 0 for left to right, 1 for
     * right to left. Callers use it to decide the default alignment of a paragraph whose
     * direction is {@link BaseDirection#FIRST_STRONG_CHARACTER}.
     *
     * @param logicalLine the raw line, before control-character sanitizing
     * @param baseDirection the paragraph direction to resolve against
     * @return the base embedding level
     */
    public static int baseLevel(String logicalLine, BaseDirection baseDirection) {
        if (baseDirection == BaseDirection.RIGHT_TO_LEFT) {
            return 1;
        }
        if (logicalLine == null || logicalLine.isEmpty()
                || !needsResolution(logicalLine, baseDirection)) {
            return 0;
        }
        return new Bidi(logicalLine, flagsFor(baseDirection)).getBaseLevel();
    }

    /**
     * Maps the items of one laid-out line from logical order to visual order.
     *
     * <p>The returned array holds logical indices in the order they are drawn, so
     * {@code result[0]} is the item that appears leftmost. An empty array means the
     * two orders coincide and the caller should keep iterating as it always has —
     * which is what every left-to-right line produces.</p>
     *
     * @param embeddingLevels the level of each item, in logical order
     * @return the visual order, or an empty array when it is the identity
     */
    public static int[] visualOrder(int[] embeddingLevels) {
        if (embeddingLevels == null || embeddingLevels.length <= 1) {
            return EMPTY_ORDER;
        }

        byte[] levels = new byte[embeddingLevels.length];
        Integer[] logicalIndices = new Integer[embeddingLevels.length];
        boolean anyRightToLeft = false;
        for (int index = 0; index < embeddingLevels.length; index++) {
            levels[index] = (byte) embeddingLevels[index];
            logicalIndices[index] = index;
            anyRightToLeft |= isRightToLeftLevel(embeddingLevels[index]);
        }
        if (!anyRightToLeft) {
            return EMPTY_ORDER;
        }

        Bidi.reorderVisually(levels, 0, logicalIndices, 0, logicalIndices.length);

        int[] order = new int[logicalIndices.length];
        boolean identity = true;
        for (int index = 0; index < logicalIndices.length; index++) {
            order[index] = logicalIndices[index];
            identity &= order[index] == index;
        }
        return identity ? EMPTY_ORDER : order;
    }

    /** Returns whether an embedding level is drawn right to left. */
    public static boolean isRightToLeftLevel(int embeddingLevel) {
        return (embeddingLevel & 1) == 1;
    }

    private static final int[] EMPTY_ORDER = new int[0];

    /**
     * A left-to-right paragraph with no right-to-left character resolves to itself, and
     * skipping the algorithm keeps that path allocation-free.
     */
    private static boolean needsResolution(String logicalLine, BaseDirection baseDirection) {
        if (baseDirection == BaseDirection.RIGHT_TO_LEFT) {
            return true;
        }
        return Bidi.requiresBidi(logicalLine.toCharArray(), 0, logicalLine.length());
    }

    private static int flagsFor(BaseDirection baseDirection) {
        return switch (baseDirection) {
            case LEFT_TO_RIGHT -> Bidi.DIRECTION_LEFT_TO_RIGHT;
            case RIGHT_TO_LEFT -> Bidi.DIRECTION_RIGHT_TO_LEFT;
            case FIRST_STRONG_CHARACTER -> Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT;
        };
    }
}
