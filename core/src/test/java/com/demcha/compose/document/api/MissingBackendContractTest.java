package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.backend.fixed.BackendProviders;
import com.demcha.compose.document.exceptions.MissingBackendException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The lean {@code graph-compose-core} carries no render backend, and says so at the
 * first operation that needs one with a {@link MissingBackendException} naming the
 * artifact to add ({@code graph-compose-render-pdf}). This is the defining contract
 * of the lean core.
 *
 * <p>That first operation is {@code create()}, not a render call: opening a session
 * resolves the font-metrics provider, because layout measures text before anything is
 * drawn. The render-time site exists too, but it is unreachable from here — no session
 * opens on this classpath — so the format-specific cases below assert on the resolver
 * directly. The public path to it, {@code buildPptx()} without {@code render-pptx},
 * needs a classpath where one backend is present and another is not, and is covered by
 * {@code MissingPptxBackendContractTest} in {@code render-pdf}.</p>
 *
 * <p>The test lives in core's own <em>backend-free</em> test scope on purpose: a
 * backend on the classpath (as in the qa module) would resolve the provider and hide
 * a regression here.</p>
 */
class MissingBackendContractTest {

    /**
     * The throw is at {@code create()}, not at the render call.
     *
     * <p>This used to wrap {@code create()}, {@code pageFlow(...)} and
     * {@code toPdfBytes()} in one assertion. It passed, but only the first line ever
     * ran — and its name said "rendering", which is where three published documents
     * then placed the failure. Asserting on {@code create()} alone is what pins the
     * documented contract: opening a session resolves the font-metrics provider,
     * because layout measures text before anything is drawn.</p>
     */
    @Test
    void openingASessionWithoutABackendThrowsNamingRenderPdf() {
        assertThatThrownBy(() -> GraphCompose.document().pageSize(200, 200).create())
                .isInstanceOf(MissingBackendException.class)
                .hasMessageContaining("graph-compose-render-pdf");
    }

    /**
     * The other side of the same boundary: configuring a document is backend-free, so
     * a builder that resolved the provider eagerly — or one that deferred it past
     * {@code create()} to the render call — would fail here.
     */
    @Test
    void configuringADocumentDoesNotNeedABackendUntilTheSessionOpens() {
        GraphCompose.DocumentBuilder builder = GraphCompose.document().pageSize(200, 200);

        assertThat(builder).isNotNull();
        assertThatThrownBy(builder::create).isInstanceOf(MissingBackendException.class);
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
