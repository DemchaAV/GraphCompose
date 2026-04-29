package com.demcha.compose.document.style;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class DocumentTransformTest {

    private static final double EPS = 1e-9;

    @Test
    void noneIsIdentity() {
        assertThat(DocumentTransform.NONE.isIdentity()).isTrue();
        assertThat(DocumentTransform.NONE.rotationDegrees()).isEqualTo(0.0, within(EPS));
        assertThat(DocumentTransform.NONE.scaleX()).isEqualTo(1.0, within(EPS));
        assertThat(DocumentTransform.NONE.scaleY()).isEqualTo(1.0, within(EPS));
        assertThat(DocumentTransform.none()).isSameAs(DocumentTransform.NONE);
    }

    @Test
    void rotateFactoryKeepsScalesAtIdentity() {
        DocumentTransform t = DocumentTransform.rotate(45);
        assertThat(t.rotationDegrees()).isEqualTo(45.0, within(EPS));
        assertThat(t.scaleX()).isEqualTo(1.0, within(EPS));
        assertThat(t.scaleY()).isEqualTo(1.0, within(EPS));
        assertThat(t.isIdentity()).isFalse();
    }

    @Test
    void uniformScaleFactoryPropagatesToBothAxes() {
        DocumentTransform t = DocumentTransform.scale(0.5);
        assertThat(t.scaleX()).isEqualTo(0.5, within(EPS));
        assertThat(t.scaleY()).isEqualTo(0.5, within(EPS));
        assertThat(t.rotationDegrees()).isEqualTo(0.0, within(EPS));
    }

    @Test
    void nonUniformScaleFactoryHonoursAxes() {
        DocumentTransform t = DocumentTransform.scale(2.0, 0.5);
        assertThat(t.scaleX()).isEqualTo(2.0, within(EPS));
        assertThat(t.scaleY()).isEqualTo(0.5, within(EPS));
    }

    @Test
    void withRotationReplacesOnlyRotation() {
        DocumentTransform original = DocumentTransform.scale(2.0);
        DocumentTransform rotated = original.withRotation(15.0);
        assertThat(rotated.rotationDegrees()).isEqualTo(15.0, within(EPS));
        assertThat(rotated.scaleX()).isEqualTo(2.0, within(EPS));
        assertThat(rotated.scaleY()).isEqualTo(2.0, within(EPS));
    }

    @Test
    void withScaleReplacesOnlyScale() {
        DocumentTransform original = DocumentTransform.rotate(15.0);
        DocumentTransform scaled = original.withScale(0.5, 1.5);
        assertThat(scaled.rotationDegrees()).isEqualTo(15.0, within(EPS));
        assertThat(scaled.scaleX()).isEqualTo(0.5, within(EPS));
        assertThat(scaled.scaleY()).isEqualTo(1.5, within(EPS));
    }

    @Test
    void zeroScaleIsRejected() {
        // A zero scale would collapse the geometry to a point and is
        // almost always a caller mistake — reject at construction time.
        assertThatThrownBy(() -> new DocumentTransform(0.0, 0.0, 1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scaleX");
        assertThatThrownBy(() -> DocumentTransform.scale(0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scaleX");
        assertThatThrownBy(() -> DocumentTransform.scale(1.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scaleY");
    }

    @Test
    void nonFiniteValuesAreRejected() {
        assertThatThrownBy(() -> new DocumentTransform(Double.NaN, 1.0, 1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rotationDegrees");
        assertThatThrownBy(() -> new DocumentTransform(0.0, Double.POSITIVE_INFINITY, 1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scaleX");
    }

    @Test
    void negativeScaleIsAllowedForMirror() {
        // Negative scale flips the axis — useful for mirror effects.
        DocumentTransform mirrored = DocumentTransform.scale(-1.0, 1.0);
        assertThat(mirrored.scaleX()).isEqualTo(-1.0, within(EPS));
        assertThat(mirrored.isIdentity()).isFalse();
    }
}
