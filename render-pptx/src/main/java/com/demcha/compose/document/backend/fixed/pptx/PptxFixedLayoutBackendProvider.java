package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.backend.fixed.FixedLayoutBackendProvider;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderer;
import com.demcha.compose.document.output.DocumentDebugOptions;
import com.demcha.compose.document.output.DocumentOutputOptions;

/**
 * POI-backed {@link FixedLayoutBackendProvider} for the {@code "pptx"} format,
 * registered through {@code META-INF/services} so the document API can resolve
 * the PPTX backend by format without importing it.
 *
 * <p>Document chrome (metadata, watermark, headers/footers) is not translated
 * yet: options a backend cannot honour are ignored per the
 * {@link DocumentOutputOptions} contract, and the chrome translation arrives
 * together with the corresponding PPTX capabilities — see
 * {@code docs/architecture/backend-capability-matrix.md}.</p>
 *
 * @since 2.1.0
 */
public final class PptxFixedLayoutBackendProvider implements FixedLayoutBackendProvider {

    /**
     * Creates the provider. Invoked reflectively by {@link java.util.ServiceLoader}.
     */
    public PptxFixedLayoutBackendProvider() {
    }

    @Override
    public String format() {
        return "pptx";
    }

    @Override
    public FixedLayoutRenderer create(DocumentOutputOptions chrome, DocumentDebugOptions debug) {
        return new PptxFixedLayoutBackend();
    }
}
