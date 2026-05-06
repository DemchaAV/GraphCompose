package com.demcha.compose.document.templates.decorations;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.style.DocumentColor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DividerTest {

    private static final DocumentColor GRAY = DocumentColor.GRAY;

    @Test
    void thinShouldEmitHalfPointShapeWithRequestedWidthAndColor() {
        DocumentNode node = Divider.thin(GRAY, 200.0);

        assertThat(node).isInstanceOf(ShapeNode.class);
        ShapeNode shape = (ShapeNode) node;
        assertThat(shape.width()).isEqualTo(200.0);
        assertThat(shape.height()).isEqualTo(Divider.THIN_THICKNESS).isEqualTo(0.5);
        assertThat(shape.fillColor()).isEqualTo(GRAY);
        assertThat(shape.name()).isEqualTo("Divider.thin");
    }

    @Test
    void thickShouldEmitOneAndAHalfPointShape() {
        ShapeNode shape = (ShapeNode) Divider.thick(GRAY, 100.0);
        assertThat(shape.height()).isEqualTo(Divider.THICK_THICKNESS).isEqualTo(1.5);
        assertThat(shape.name()).isEqualTo("Divider.thick");
    }

    @Test
    void dashedRendersIdenticallyToThinForNow() {
        ShapeNode shape = (ShapeNode) Divider.dashed(GRAY, 100.0);
        assertThat(shape.height()).isEqualTo(Divider.THIN_THICKNESS);
        assertThat(shape.name()).isEqualTo("Divider.dashed");
    }

    @Test
    void dottedAccentRendersIdenticallyToThinForNow() {
        ShapeNode shape = (ShapeNode) Divider.dottedAccent(GRAY, 100.0);
        assertThat(shape.height()).isEqualTo(Divider.THIN_THICKNESS);
        assertThat(shape.name()).isEqualTo("Divider.dottedAccent");
    }

    @Test
    void customAcceptsArbitraryThickness() {
        ShapeNode shape = (ShapeNode) Divider.custom(GRAY, 100.0, 3.0);
        assertThat(shape.height()).isEqualTo(3.0);
        assertThat(shape.name()).isEqualTo("Divider.custom");
    }

    @Test
    void colorMustNotBeNull() {
        assertThatThrownBy(() -> Divider.thin(null, 100.0))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("color");
    }

    @Test
    void widthMustBePositive() {
        assertThatThrownBy(() -> Divider.thin(GRAY, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Divider.thin(GRAY, -5.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void customThicknessMustBePositive() {
        assertThatThrownBy(() -> Divider.custom(GRAY, 100.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("thickness");
        assertThatThrownBy(() -> Divider.custom(GRAY, 100.0, Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Divider.custom(GRAY, 100.0, Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
