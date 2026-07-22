package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.backend.fixed.BackendProviders;
import com.demcha.compose.document.exceptions.MissingBackendException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The lean {@code graph-compose-core} carries no render backend. Any operation that
 * needs one — measuring text to lay out, rendering, or rasterizing a document — must
 * fail with a {@link MissingBackendException} that names the artifact to add
 * ({@code graph-compose-render-pdf}). This is the defining contract of the lean core.
 *
 * <p>The test lives in core's own <em>backend-free</em> test scope on purpose: a
 * backend on the classpath (as in the qa module) would resolve the provider and hide
 * a regression here.</p>
 */
class MissingBackendContractTest {

    @Test
    void renderingWithoutABackendThrowsMissingBackendExceptionNamingRenderPdf() {
        assertThatThrownBy(() -> {
            try (DocumentSession session = GraphCompose.document()
                    .pageSize(200, 200)
                    .create()) {
                session.pageFlow(page -> page.module("m", module -> module.paragraph("hi")));
                session.toPdfBytes();
            }
        })
                .isInstanceOf(MissingBackendException.class)
                .hasMessageContaining("graph-compose-render-pdf");
    }

    @Test
    void missingKnownFormatNamesTheArtifactToAdd() {
        assertThatThrownBy(() -> BackendProviders.fixedLayout("pptx"))
                .isInstanceOf(MissingBackendException.class)
                .hasMessageContaining("\"pptx\"")
                .hasMessageContaining("io.github.demchaav:graph-compose-render-pptx");
    }

    @Test
    void missingUnknownFormatFailsWithTheFormatInTheMessage() {
        assertThatThrownBy(() -> BackendProviders.fixedLayout("xps"))
                .isInstanceOf(MissingBackendException.class)
                .hasMessageContaining("\"xps\"")
                .hasMessageContaining("FixedLayoutBackendProvider");
    }
}
