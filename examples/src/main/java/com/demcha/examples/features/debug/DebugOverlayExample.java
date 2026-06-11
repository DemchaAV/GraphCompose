package com.demcha.examples.features.debug;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.output.DocumentDebugOptions;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for the PDF debug overlay (v1.8): guide lines plus
 * semantic node labels.
 *
 * <p>One switch turns the rendered sheet into a self-describing layout
 * map:</p>
 *
 * <pre>{@code
 * GraphCompose.document(out)
 *         .debug(DocumentDebugOptions.guidesAndNodeLabels())
 *         .create()
 * }</pre>
 *
 * <p>Every rendered node then prints its stable semantic path — the same
 * path {@code DocumentSession.layoutSnapshot()} reports — once per node
 * and page as a corner badge straddling the top edge of the node's bounds
 * (right-aligned), next to the familiar fragment boxes and dashed
 * margin/padding guides. Spot a misplaced block on
 * paper, read its label, and grep that name straight in your builder
 * code. {@code DocumentDebugOptions.LabelText.FULL_PATH} switches the labels
 * from the compact own segment to the whole ancestor chain.</p>
 *
 * <p>Debug overlays draw strictly on top of regular content and never
 * affect measurement or pagination — disabling them returns the exact
 * production bytes.</p>
 *
 * @author Artem Demchyshyn
 */
public final class DebugOverlayExample {

    private DebugOverlayExample() {
    }

    /**
     * Renders a small annotated sheet with guides and node labels enabled.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering or file IO fails
     */
    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("features/debug", "debug-overlay.pdf");

        try (DocumentSession document = GraphCompose.document(pdfFile)
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(28))
                .debug(DocumentDebugOptions.guidesAndNodeLabels())
                .create()) {
            document.pageFlow(page -> page
                    .module("HowToReadThisSheet", module -> module
                            .paragraph("Every block carries its debug overlay: gray fragment boxes, "
                                       + "dashed margin (blue) and padding (orange) guides, and a small "
                                       + "purple label with the owning node's semantic path.")
                            .paragraph("Labels print the same stable path that layoutSnapshot() reports — "
                                       + "spot a misplaced block on paper, then search the label text "
                                       + "in your builder code."))
                    .module("InvoiceHeader", module -> module
                            .paragraph("ACME Corp — Invoice 2026-104")
                            .paragraph("Issued 2026-06-11, due in 14 days"))
                    .module("PriceSummary", module -> module
                            .paragraph("Subtotal 1,180.00 · VAT 236.00 · Total 1,416.00")));

            document.buildPdf();
        }

        return pdfFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
