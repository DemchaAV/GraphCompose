package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.TextAlign;

/**
 * Where a laid-out paragraph line sits.
 *
 * <p>Every fixed-layout backend has to answer this, and every one of them has to answer
 * it the same way — the PDF handler draws the glyphs, the PDF backend places the
 * clickable rectangles over them, and the PPTX handler anchors its text boxes. Three
 * copies of the same expression is three chances for them to drift, and a disagreement
 * shows up as a link that no longer covers its text, or a slide whose words sit a few
 * points off where the page had them.</p>
 *
 * <p>The vertical walk down a fragment's lines joined it in 2.2.2, for the same reason
 * and one more: the layout snapshot reports where each line ended up, and a diagnostic
 * that recomputed the stack itself would be free to disagree with the page it claims to
 * describe.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.2.0
 */
public final class ParagraphLineGeometry {

    private ParagraphLineGeometry() {
    }

    /**
     * Returns the left edge of a line within the content box.
     *
     * @param align     the paragraph's horizontal alignment
     * @param innerX    left edge of the content box
     * @param innerWidth width of the content box
     * @param lineWidth measured width of the line
     * @return the x coordinate the line starts at
     */
    public static double lineStartX(TextAlign align, double innerX, double innerWidth, double lineWidth) {
        return switch (align == null ? TextAlign.LEFT : align) {
            case RIGHT -> innerX + innerWidth - lineWidth;
            case CENTER -> innerX + (innerWidth - lineWidth) / 2.0;
            case LEFT -> innerX;
        };
    }

    /**
     * Returns the top edge of the content box a fragment's lines stack down from.
     *
     * @param fragmentY      bottom edge of the fragment
     * @param fragmentHeight height of the fragment
     * @param paddingTop     the paragraph's top padding
     * @return the y coordinate the first line's top sits at
     * @since 2.2.2
     */
    public static double contentTop(double fragmentY, double fragmentHeight, double paddingTop) {
        return fragmentY + fragmentHeight - paddingTop;
    }

    /**
     * Returns the top of the line following one of height {@code lineHeight}.
     *
     * <p>Lines stack downward, so this decreases.</p>
     *
     * @param lineTop    top of the current line
     * @param lineHeight resolved height of the current line
     * @param lineGap    extra spacing configured between lines
     * @return the top of the next line
     * @since 2.2.2
     */
    public static double nextLineTop(double lineTop, double lineHeight, double lineGap) {
        return lineTop - lineHeight - lineGap;
    }

    /**
     * Returns the baseline of a line whose box top is {@code lineTop}.
     *
     * <p>This is the baseline <em>before</em> any vertical-seating correction. A
     * paragraph using a non-default {@code TextVerticalAlign} shifts it by an amount
     * derived from the backend font's cap height, which only a backend can supply — so a
     * renderer-neutral caller gets the unseated baseline and has to say so.</p>
     *
     * @param lineTop                  top of the line box
     * @param lineHeight               resolved height of the line
     * @param baselineOffsetFromBottom distance from the line bottom to the baseline
     * @return the y coordinate of the text baseline
     * @since 2.2.2
     */
    public static double baselineY(double lineTop, double lineHeight, double baselineOffsetFromBottom) {
        return lineTop - lineHeight + baselineOffsetFromBottom;
    }
}
