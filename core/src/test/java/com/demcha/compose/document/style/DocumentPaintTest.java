package com.demcha.compose.document.style;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Value-level contract of {@link DocumentPaint.Stop}: the offset bounds and the
 * opaque-colour requirement. Gradients render through PDF axial / radial
 * shadings, which carry no alpha channel, so a translucent stop must fail at
 * construction instead of silently rendering opaque — mirroring the SVG
 * reader, which already refuses {@code stop-opacity}.
 */
class DocumentPaintTest {

    @Test
    void opaqueStopIsAccepted() {
        assertThatCode(() -> new DocumentPaint.Stop(0.5, DocumentColor.rgb(20, 80, 95)))
                .doesNotThrowAnyException();
        // rgba with full alpha is still opaque.
        assertThatCode(() -> new DocumentPaint.Stop(0.0, DocumentColor.rgba(10, 20, 30, 255)))
                .doesNotThrowAnyException();
    }

    @Test
    void translucentStopIsRejected() {
        assertThatThrownBy(() -> new DocumentPaint.Stop(0.0, DocumentColor.rgb(20, 80, 95).withOpacity(0.5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("opaque");
        assertThatThrownBy(() -> new DocumentPaint.Stop(1.0, DocumentColor.rgba(10, 20, 30, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("opaque");
    }

    @Test
    void offsetOutsideUnitRangeIsRejected() {
        assertThatThrownBy(() -> new DocumentPaint.Stop(-0.1, DocumentColor.BLACK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offset");
        assertThatThrownBy(() -> new DocumentPaint.Stop(1.5, DocumentColor.BLACK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offset");
    }

    @Test
    void linearFactoryRejectsTranslucentEndpoints() {
        // The two-colour sugar builds stops, so the opaque guard reaches it too.
        assertThatThrownBy(() -> DocumentPaint.linear(
                DocumentColor.rgb(20, 80, 95).withOpacity(0.4), DocumentColor.WHITE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("opaque");
    }

    @Test
    void solidPaintExposesItsColor() {
        DocumentColor teal = DocumentColor.rgb(20, 80, 95);
        assertThat(DocumentPaint.solid(teal).primaryColor()).isEqualTo(teal);
    }
}
