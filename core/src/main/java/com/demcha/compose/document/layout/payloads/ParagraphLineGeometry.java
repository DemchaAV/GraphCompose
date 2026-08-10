package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.TextAlign;

/**
 * Where a laid-out paragraph line starts along the x axis.
 *
 * <p>Every fixed-layout backend has to answer this, and every one of them has to answer
 * it the same way — the PDF handler draws the glyphs, the PDF backend places the
 * clickable rectangles over them, and the PPTX handler anchors its text boxes. Three
 * copies of the same expression is three chances for them to drift, and a disagreement
 * shows up as a link that no longer covers its text, or a slide whose words sit a few
 * points off where the page had them.</p>
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
}
