package com.demcha.compose.document.output;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Unit coverage for the {@link DocumentDebugOptions} value semantics: factory
 * presets, wither transitions, and the enabled() aggregate.
 */
class DocumentDebugOptionsTest {

    @Test
    void noneDisablesEveryOverlay() {
        DocumentDebugOptions options = DocumentDebugOptions.none();

        assertThat(options.showGuides()).isFalse();
        assertThat(options.showNodeLabels()).isFalse();
        assertThat(options.labelText()).isEqualTo(DocumentDebugOptions.LabelText.NAME);
        assertThat(options.enabled()).isFalse();
    }

    @Test
    void factoriesEnableTheRequestedOverlays() {
        assertThat(DocumentDebugOptions.guides().showGuides()).isTrue();
        assertThat(DocumentDebugOptions.guides().showNodeLabels()).isFalse();
        assertThat(DocumentDebugOptions.guides().enabled()).isTrue();

        assertThat(DocumentDebugOptions.nodeLabels().showGuides()).isFalse();
        assertThat(DocumentDebugOptions.nodeLabels().showNodeLabels()).isTrue();
        assertThat(DocumentDebugOptions.nodeLabels().enabled()).isTrue();

        DocumentDebugOptions both = DocumentDebugOptions.guidesAndNodeLabels();
        assertThat(both.showGuides()).isTrue();
        assertThat(both.showNodeLabels()).isTrue();
        assertThat(both.enabled()).isTrue();
    }

    @Test
    void withersToggleSingleAspectsAndPreserveTheRest() {
        DocumentDebugOptions labelsWithGuides = DocumentDebugOptions.nodeLabels().withGuides(true);
        assertThat(labelsWithGuides.showGuides()).isTrue();
        assertThat(labelsWithGuides.showNodeLabels()).isTrue();

        DocumentDebugOptions guidesOnlyAgain = labelsWithGuides.withNodeLabels(false);
        assertThat(guidesOnlyAgain.showGuides()).isTrue();
        assertThat(guidesOnlyAgain.showNodeLabels()).isFalse();

        DocumentDebugOptions fullPath = DocumentDebugOptions.nodeLabels()
                .withLabelText(DocumentDebugOptions.LabelText.FULL_PATH);
        assertThat(fullPath.labelText()).isEqualTo(DocumentDebugOptions.LabelText.FULL_PATH);
        assertThat(fullPath.showNodeLabels()).isTrue();
    }

    @Test
    void noOpWithersReturnTheSameInstance() {
        DocumentDebugOptions options = DocumentDebugOptions.guides();

        assertThat(options.withGuides(true)).isSameAs(options);
        assertThat(options.withNodeLabels(false)).isSameAs(options);
        assertThat(options.withLabelText(DocumentDebugOptions.LabelText.NAME)).isSameAs(options);
    }

    @Test
    void nullLabelTextIsRejected() {
        assertThatNullPointerException()
                .isThrownBy(() -> new DocumentDebugOptions(false, false, null));
        assertThatNullPointerException()
                .isThrownBy(() -> DocumentDebugOptions.none().withLabelText(null));
    }
}
