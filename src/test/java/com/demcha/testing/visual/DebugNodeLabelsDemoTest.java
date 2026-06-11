package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfDebugOptions;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.testing.visual.ImageDiff;
import com.demcha.compose.testing.visual.PdfVisualRegression;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders the same two-module sheet with and without the debug overlay
 * (guide lines + semantic node labels) and writes a human-review PDF.
 * Asserts the overlay visibly paints on top of the plain render.
 */
class DebugNodeLabelsDemoTest {

    private static final PdfVisualRegression VISUAL = PdfVisualRegression.standard();

    @Test
    void debugOverlayPaintsGuidesAndNodeLabels() throws Exception {
        byte[] plain = sheet(PdfDebugOptions.none());
        byte[] debug = sheet(PdfDebugOptions.guidesAndNodeLabels());

        assertThat(debug).isNotEmpty();
        assertThat(new String(debug, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        BufferedImage plainPage = VISUAL.renderPages(plain).get(0);
        BufferedImage debugPage = VISUAL.renderPages(debug).get(0);
        ImageDiff.Result diff = ImageDiff.compare(plainPage, debugPage, 8);
        assertThat(diff.mismatchedPixelCount())
                .as("the debug overlay must visibly draw guides and labels (%s)", diff.summary())
                .isGreaterThan(500L);

        Path out = Path.of("target/visual-tests/debug-overlay/debug_node_labels.pdf");
        Files.createDirectories(out.getParent());
        Files.write(out, debug);
        javax.imageio.ImageIO.write(debugPage, "png",
                out.resolveSibling("debug_node_labels.png").toFile());
    }

    private static byte[] sheet(PdfDebugOptions options) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(360, 320)
                .margin(DocumentInsets.of(22))
                .debug(options)
                .create()) {
            document.pageFlow(page -> page
                    .module("InvoiceHeader", module -> module
                            .paragraph("ACME Corp — Invoice 2026-104")
                            .paragraph("Issued 2026-06-11, due in 14 days"))
                    .module("PriceSummary", module -> module
                            .paragraph("Subtotal 1,180.00")
                            .paragraph("Total 1,416.00")));
            return document.toPdfBytes();
        }
    }
}
