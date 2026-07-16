package com.demcha.compose.document.backend.fixed;

import com.demcha.compose.document.exceptions.MissingBackendException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Selection semantics of {@link BackendProviders} on a classpath with several
 * fixed-layout providers but no PDF backend (two test fakes, formats
 * {@code "aaa"} and {@code "zzz"}, registered in the reverse of lexicographic
 * order so classpath enumeration order cannot masquerade as the contract):
 * the no-arg default falls back to the lexicographically smallest format,
 * format lookup is case-insensitive and hands out one canonical instance per
 * format, and a missing format still fails with the artifact-naming
 * diagnostic.
 */
class BackendProvidersSelectionTest {

    @Test
    void defaultFallsBackToTheLexicographicallySmallestFormatWithoutPdf() {
        assertThat(BackendProviders.fixedLayout().format()).isEqualTo("aaa");
        assertThat(BackendProviders.fixedLayout())
                .isSameAs(BackendProviders.fixedLayout("aaa"));
    }

    @Test
    void formatLookupIsCaseInsensitiveAndInstanceCanonical() {
        assertThat(BackendProviders.fixedLayout("ZZZ"))
                .isSameAs(BackendProviders.fixedLayout("zzz"));
        assertThat(BackendProviders.fixedLayout("zzz").format()).isEqualTo("zzz");
    }

    @Test
    void pdfStaysMissingDespiteOtherRegisteredProviders() {
        assertThatThrownBy(() -> BackendProviders.fixedLayout("pdf"))
                .isInstanceOf(MissingBackendException.class)
                .hasMessageContaining("graph-compose-render-pdf")
                .hasMessageContaining("graph-compose-bundle");
    }
}
