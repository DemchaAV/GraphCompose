package com.demcha.compose.document.backend.fixed.pdf.options;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Unit coverage for the {@link PdfDebugOptions} value semantics: factory
 * presets, wither transitions, and the enabled() aggregate.
 */
class PdfDebugOptionsTest {

    @Test
    void noneDisablesEveryOverlay() {
        PdfDebugOptions options = PdfDebugOptions.none();

        assertThat(options.showGuides()).isFalse();
        assertThat(options.showNodeLabels()).isFalse();
        assertThat(options.labelText()).isEqualTo(PdfDebugOptions.LabelText.NAME);
        assertThat(options.enabled()).isFalse();
    }

    @Test
    void factoriesEnableTheRequestedOverlays() {
        assertThat(PdfDebugOptions.guides().showGuides()).isTrue();
        assertThat(PdfDebugOptions.guides().showNodeLabels()).isFalse();
        assertThat(PdfDebugOptions.guides().enabled()).isTrue();

        assertThat(PdfDebugOptions.nodeLabels().showGuides()).isFalse();
        assertThat(PdfDebugOptions.nodeLabels().showNodeLabels()).isTrue();
        assertThat(PdfDebugOptions.nodeLabels().enabled()).isTrue();

        PdfDebugOptions both = PdfDebugOptions.guidesAndNodeLabels();
        assertThat(both.showGuides()).isTrue();
        assertThat(both.showNodeLabels()).isTrue();
        assertThat(both.enabled()).isTrue();
    }

    @Test
    void withersToggleSingleAspectsAndPreserveTheRest() {
        PdfDebugOptions labelsWithGuides = PdfDebugOptions.nodeLabels().withGuides(true);
        assertThat(labelsWithGuides.showGuides()).isTrue();
        assertThat(labelsWithGuides.showNodeLabels()).isTrue();

        PdfDebugOptions guidesOnlyAgain = labelsWithGuides.withNodeLabels(false);
        assertThat(guidesOnlyAgain.showGuides()).isTrue();
        assertThat(guidesOnlyAgain.showNodeLabels()).isFalse();

        PdfDebugOptions fullPath = PdfDebugOptions.nodeLabels()
                .withLabelText(PdfDebugOptions.LabelText.FULL_PATH);
        assertThat(fullPath.labelText()).isEqualTo(PdfDebugOptions.LabelText.FULL_PATH);
        assertThat(fullPath.showNodeLabels()).isTrue();
    }

    @Test
    void noOpWithersReturnTheSameInstance() {
        PdfDebugOptions options = PdfDebugOptions.guides();

        assertThat(options.withGuides(true)).isSameAs(options);
        assertThat(options.withNodeLabels(false)).isSameAs(options);
        assertThat(options.withLabelText(PdfDebugOptions.LabelText.NAME)).isSameAs(options);
    }

    @Test
    void nullLabelTextIsRejected() {
        assertThatNullPointerException()
                .isThrownBy(() -> new PdfDebugOptions(false, false, null));
        assertThatNullPointerException()
                .isThrownBy(() -> PdfDebugOptions.none().withLabelText(null));
    }
}
