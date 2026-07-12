package com.demcha.compose.document.backend.fixed;

import com.demcha.compose.document.output.DocumentDebugOptions;
import com.demcha.compose.document.output.DocumentOutputOptions;

/**
 * Service-provider that supplies a fixed-layout render backend to the document
 * API's convenience output methods without exposing a concrete backend type.
 *
 * <p>Implementations are discovered through {@link java.util.ServiceLoader};
 * each render backend artifact registers one (for example the PDF backend in
 * {@code io.github.demchaav:graph-compose-render-pdf}). The core resolves a
 * provider lazily via {@link BackendProviders#fixedLayout()} and fails with a
 * {@link com.demcha.compose.document.exceptions.MissingBackendException} naming
 * the artifact to add when none is registered.</p>
 *
 * @since 2.0.0
 */
public interface FixedLayoutBackendProvider {

    /**
     * Returns the output format this provider renders, used for diagnostics and
     * manifests.
     *
     * @return a stable format identifier such as {@code "pdf"}
     */
    String format();

    /**
     * Creates a backend configured with the session's chrome and debug overlay.
     *
     * @param chrome document-level chrome (metadata, watermark, protection,
     *               repeating headers and footers); never {@code null}
     * @param debug  debug overlay options; never {@code null}
     * @return a configured, ready-to-use renderer
     */
    FixedLayoutRenderer create(DocumentOutputOptions chrome, DocumentDebugOptions debug);
}
