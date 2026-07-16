package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.backend.fixed.BackendProviders;
import com.demcha.compose.document.backend.fixed.FixedLayoutBackendProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ServiceLoader wiring for the PPTX provider, exercised on the first real
 * two-backend classpath (render-pdf sits in this module's test scope): the
 * PPTX backend resolves by format while the no-arg default keeps preferring
 * PDF — the coexistence contract the format-keyed lookup exists for.
 */
class PptxProviderResolutionTest {

    @Test
    void resolvesThePptxProviderByFormat() {
        FixedLayoutBackendProvider provider = BackendProviders.fixedLayout("pptx");
        assertThat(provider).isInstanceOf(PptxFixedLayoutBackendProvider.class);
        assertThat(provider.format()).isEqualTo("pptx");
        assertThat(BackendProviders.fixedLayout("PPTX")).isSameAs(provider);
    }

    @Test
    void theDefaultProviderStaysPdfWithBothBackendsPresent() {
        assertThat(BackendProviders.fixedLayout().format()).isEqualTo("pdf");
    }

    @Test
    void theProviderCreatesAWorkingRenderer() {
        assertThat(BackendProviders.fixedLayout("pptx")
                .create(com.demcha.compose.document.output.DocumentOutputOptions.EMPTY,
                        com.demcha.compose.document.output.DocumentDebugOptions.none())
                .name())
                .isEqualTo("pptx-fixed-layout");
    }
}
