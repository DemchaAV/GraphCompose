package com.demcha.compose.document.backend.fixed;

import com.demcha.compose.document.backend.fixed.pdf.PdfFixedLayoutBackendProvider;
import com.demcha.compose.document.backend.fixed.pdf.PdfFontMetricsProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the ServiceLoader wiring of the backend seam: in a single-jar build the
 * bundled PDF providers must be discoverable through {@link BackendProviders},
 * and each must produce usable output. This is the explicit check behind the
 * transitive coverage the render/measurement suites give the seam.
 */
class BackendProvidersTest {

    @Test
    void resolvesThePdfFontMetricsProviderAndOpensMeasurementResources() throws Exception {
        FontMetricsProvider provider = BackendProviders.fontMetrics();
        assertThat(provider).isInstanceOf(PdfFontMetricsProvider.class);
        try (MeasurementResources resources = provider.openMeasurement(List.of())) {
            assertThat(resources.fontLibrary()).isNotNull();
            assertThat(resources.textMeasurementSystem()).isNotNull();
        }
    }

    @Test
    void resolvesThePdfFixedLayoutBackendProvider() {
        FixedLayoutBackendProvider provider = BackendProviders.fixedLayout();
        assertThat(provider).isInstanceOf(PdfFixedLayoutBackendProvider.class);
        assertThat(provider.format()).isEqualTo("pdf");
    }
}
