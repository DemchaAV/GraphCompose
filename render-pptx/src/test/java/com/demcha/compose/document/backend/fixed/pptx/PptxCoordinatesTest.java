package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.layout.PlacedFragment;
import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The y-flip contract: fragments arrive bottom-left-anchored with y growing
 * up; anchors leave top-left-anchored with y growing down.
 */
class PptxCoordinatesTest {

    @Test
    void flipsABottomLeftBoxIntoTopDownSlideSpace() {
        // A 100×50 box whose bottom edge sits 120pt above the page bottom on
        // a 300pt-tall page has its top edge 130pt below the slide top.
        assertThat(PptxCoordinates.topY(300, 120, 50)).isEqualTo(130.0);
        assertThat(PptxCoordinates.flipPoint(300, 120)).isEqualTo(180.0);
    }

    @Test
    void anchorPreservesXAndSizeAndFlipsY() {
        PlacedFragment fragment = new PlacedFragment(
                "root/shape", 0, 0, 40, 120, 100, 50, null, null, null);
        Rectangle2D.Double anchor = PptxCoordinates.anchorOf(300, fragment);
        assertThat(anchor.x).isEqualTo(40.0);
        assertThat(anchor.y).isEqualTo(130.0);
        assertThat(anchor.width).isEqualTo(100.0);
        assertThat(anchor.height).isEqualTo(50.0);
    }
}
