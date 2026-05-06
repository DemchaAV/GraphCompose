package com.demcha.compose.document.templates.decorations;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.style.DocumentColor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccentStripTest {

    private static final DocumentColor ROYAL = DocumentColor.ROYAL_BLUE;

    @Test
    void leftEmitsShapeWithRequestedDimensionsAndColor() {
        DocumentNode node = AccentStrip.left(ROYAL, 4.0, 200.0);

        assertThat(node).isInstanceOf(ShapeNode.class);
        ShapeNode shape = (ShapeNode) node;
        assertThat(shape.width()).isEqualTo(4.0);
        assertThat(shape.height()).isEqualTo(200.0);
        assertThat(shape.fillColor()).isEqualTo(ROYAL);
        assertThat(shape.name()).isEqualTo("AccentStrip.left");
    }

    @Test
    void rightCarriesItsOwnSemanticName() {
        ShapeNode shape = (ShapeNode) AccentStrip.right(ROYAL, 4.0, 200.0);
        assertThat(shape.name()).isEqualTo("AccentStrip.right");
    }

    @Test
    void topCarriesItsOwnSemanticName() {
        ShapeNode shape = (ShapeNode) AccentStrip.top(ROYAL, 200.0, 3.0);
        assertThat(shape.name()).isEqualTo("AccentStrip.top");
    }

    @Test
    void bottomCarriesItsOwnSemanticName() {
        ShapeNode shape = (ShapeNode) AccentStrip.bottom(ROYAL, 200.0, 3.0);
        assertThat(shape.name()).isEqualTo("AccentStrip.bottom");
    }

    @Test
    void rectAcceptsArbitraryRatio() {
        ShapeNode shape = (ShapeNode) AccentStrip.rect(ROYAL, 50.0, 30.0);
        assertThat(shape.width()).isEqualTo(50.0);
        assertThat(shape.height()).isEqualTo(30.0);
    }

    @Test
    void colorMustNotBeNull() {
        assertThatThrownBy(() -> AccentStrip.left(null, 4.0, 200.0))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("color");
    }

    @Test
    void zeroWidthIsRejectedByShapeNode() {
        assertThatThrownBy(() -> AccentStrip.left(ROYAL, 0.0, 200.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroHeightIsRejectedByShapeNode() {
        assertThatThrownBy(() -> AccentStrip.top(ROYAL, 200.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
