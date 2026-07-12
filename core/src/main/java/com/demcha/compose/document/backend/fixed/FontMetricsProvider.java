package com.demcha.compose.document.backend.fixed;

import com.demcha.compose.font.FontFamilyDefinition;

import java.util.Collection;

/**
 * Service-provider that supplies backend-specific font metrics to the layout
 * pipeline without exposing backend font types to the document API.
 *
 * <p>Implementations are discovered through {@link java.util.ServiceLoader};
 * each render backend artifact registers one (for example the PDF backend in
 * {@code io.github.demchaav:graph-compose-render-pdf}). The core resolves a
 * provider lazily via {@link BackendProviders#fontMetrics()} and fails with a
 * {@link com.demcha.compose.document.exceptions.MissingBackendException} naming
 * the artifact to add when none is registered.</p>
 *
 * @since 2.0.0
 */
public interface FontMetricsProvider {

    /**
     * Opens measurement resources for the given document-local font families.
     *
     * @param customFontFamilies document-local font families to register in
     *                            addition to the bundled catalog; never
     *                            {@code null} (may be empty)
     * @return owned, document-scoped measurement resources
     */
    MeasurementResources openMeasurement(Collection<FontFamilyDefinition> customFontFamilies);
}
