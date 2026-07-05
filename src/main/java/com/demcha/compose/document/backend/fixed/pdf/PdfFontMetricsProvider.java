package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.backend.fixed.FontMetricsProvider;
import com.demcha.compose.document.backend.fixed.MeasurementResources;
import com.demcha.compose.font.FontFamilyDefinition;

import java.util.Collection;

/**
 * PDFBox-backed {@link FontMetricsProvider}, registered through
 * {@code META-INF/services} so the core layout pipeline can obtain text metrics
 * without importing the PDF backend.
 *
 * @since 2.0.0
 */
public final class PdfFontMetricsProvider implements FontMetricsProvider {

    /**
     * Creates the provider. Invoked reflectively by {@link java.util.ServiceLoader}.
     */
    public PdfFontMetricsProvider() {
    }

    @Override
    public MeasurementResources openMeasurement(Collection<FontFamilyDefinition> customFontFamilies) {
        return PdfMeasurementResources.open(customFontFamilies);
    }
}
