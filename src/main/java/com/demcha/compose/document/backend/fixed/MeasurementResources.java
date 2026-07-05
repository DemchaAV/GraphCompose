package com.demcha.compose.document.backend.fixed;

import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import com.demcha.compose.font.FontLibrary;

/**
 * Backend-neutral bundle of the font library and text-measurement service the
 * canonical layout pipeline needs to size text before rendering.
 *
 * <p>A {@link FontMetricsProvider} produces these resources for a session. Both
 * members are backend-neutral types ({@link FontLibrary}, {@link
 * TextMeasurementSystem}); the concrete font metrics live behind the provider,
 * so the document API can lay out a document without naming a rendering backend.
 * The instance is document-scoped and owns any caches it created, released by
 * {@link #close()}.</p>
 *
 * @since 2.0.0
 */
public interface MeasurementResources extends AutoCloseable {

    /**
     * Returns the resolved document-scoped font library.
     *
     * @return the font library bound to this session's fonts
     */
    FontLibrary fontLibrary();

    /**
     * Returns the text-measurement service backed by the provider's font metrics.
     *
     * @return the measurement service used during layout
     */
    TextMeasurementSystem textMeasurementSystem();

    /**
     * Releases any caches held by the measurement service.
     *
     * @throws Exception if the underlying resources fail to close
     */
    @Override
    void close() throws Exception;
}
