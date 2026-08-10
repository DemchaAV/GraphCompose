package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.TextAlign;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the one expression three backends share.
 *
 * <p>It had been copied into each of them, which is what makes it worth a test of its
 * own: the failure of a drifted copy is not a crash but a link that no longer covers its
 * text, or a slide whose words sit a few points from where the page put them.</p>
 */
class ParagraphLineGeometryTest {

    private static final double INNER_X = 40.0;
    private static final double INNER_WIDTH = 200.0;
    private static final double LINE_WIDTH = 50.0;

    @Test
    void leftStartsAtTheContentEdge() {
        assertThat(ParagraphLineGeometry.lineStartX(TextAlign.LEFT, INNER_X, INNER_WIDTH, LINE_WIDTH))
                .isEqualTo(40.0);
    }

    @Test
    void rightEndsAtTheContentEdge() {
        assertThat(ParagraphLineGeometry.lineStartX(TextAlign.RIGHT, INNER_X, INNER_WIDTH, LINE_WIDTH))
                .describedAs("the line's right edge lands on the content box's right edge")
                .isEqualTo(190.0);
    }

    @Test
    void centreLeavesEqualSpaceOnBothSides() {
        double x = ParagraphLineGeometry.lineStartX(TextAlign.CENTER, INNER_X, INNER_WIDTH, LINE_WIDTH);

        assertThat(x - INNER_X)
                .isEqualTo(INNER_X + INNER_WIDTH - (x + LINE_WIDTH));
    }

    @Test
    void aLineWiderThanTheBoxOverflowsInTheDirectionItIsAlignedTo() {
        assertThat(ParagraphLineGeometry.lineStartX(TextAlign.RIGHT, INNER_X, 100.0, 160.0))
                .describedAs("clamping here would silently disagree with the width the layout "
                        + "already committed to, so the overflow is left visible")
                .isEqualTo(-20.0);
    }

    @Test
    void aMissingAlignmentBehavesAsLeft() {
        assertThat(ParagraphLineGeometry.lineStartX(null, INNER_X, INNER_WIDTH, LINE_WIDTH))
                .isEqualTo(ParagraphLineGeometry.lineStartX(TextAlign.LEFT, INNER_X, INNER_WIDTH, LINE_WIDTH));
    }
}
