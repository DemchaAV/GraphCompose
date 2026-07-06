package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end coverage for the canonical barcode DSL ({@code addBarcode}): a QR
 * code and a linear CODE128 both lay out on a single page and render through the
 * PDF backend without error. Guards barcode rendering on the canonical path.
 */
class BarcodeRenderTest {

    @Test
    void barcodesLayOutOnOnePageAndRenderToPdf() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(240, 200)
                .margin(DocumentInsets.of(12))
                .create()) {
            session.pageFlow()
                    .addBarcode(b -> b.qrCode().data("GC-2026-001").size(80, 80))
                    .addBarcode(b -> b.code128().data("GC-2026-001").size(160, 40))
                    .build();

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isEqualTo(1);
            assertThat(graph.nodes()).isNotEmpty();

            byte[] pdf = session.toPdfBytes();
            assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        }
    }
}
