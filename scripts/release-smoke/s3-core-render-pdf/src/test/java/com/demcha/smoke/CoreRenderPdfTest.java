package com.demcha.smoke;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario 3 — the explicit bring-your-own-backend combination
 * ({@code graph-compose-core} + {@code graph-compose-render-pdf}) renders a PDF.
 * The PDF backend registers its {@code FixedLayoutBackendProvider} /
 * {@code FontMetricsProvider} via {@code META-INF/services}, so the core
 * discovers it at runtime through the ServiceLoader SPI.
 */
class CoreRenderPdfTest {

    @Test
    void coreWithRenderPdfRendersPdf() throws Exception {
        Path out = Files.createTempFile("gc-smoke-core-render-pdf", ".pdf");
        try (DocumentSession document = GraphCompose.document(out)
                .pageSize(DocumentPageSize.A4)
                .margin(36f, 36f, 36f, 36f)
                .create()) {
            document.add(document.dsl().paragraph()
                    .text("graph-compose-core + graph-compose-render-pdf renders via the SPI.")
                    .build());
            document.buildPdf();
        }

        assertThat(Files.size(out)).isGreaterThan(0L);
        byte[] head = Arrays.copyOf(Files.readAllBytes(out), 5);
        assertThat(new String(head)).isEqualTo("%PDF-");
    }
}
