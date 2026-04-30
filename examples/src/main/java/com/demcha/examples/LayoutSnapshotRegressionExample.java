package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.snapshot.LayoutSnapshot;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.builtins.InvoiceTemplateV2;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.testing.layout.LayoutSnapshotJson;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Phase E.4 — runnable showcase for GraphCompose's deterministic
 * post-layout snapshot mechanism.
 *
 * <p>Adopters use snapshots as a fast, renderer-agnostic regression
 * signal: a snapshot captures the resolved page count, canvas, and
 * the depth-first list of every node's resolved bounds and metadata,
 * but it deliberately leaves out renderer-specific bytes (font
 * embedding, PDFBox object IDs, timestamps). When a layout change
 * lands, the JSON diff makes it instantly visible whether the new
 * code re-flowed something it shouldn't have.</p>
 *
 * <p>This example walks through the full workflow:</p>
 *
 * <ol>
 *   <li>Compose a document via the canonical {@link DocumentSession}.</li>
 *   <li>Call {@link DocumentSession#layoutSnapshot()} to extract the
 *       deterministic post-layout snapshot.</li>
 *   <li>Serialize it to JSON via {@link LayoutSnapshotJson#toJson}.</li>
 *   <li>On first run, write a baseline next to the generated PDF.
 *       On subsequent runs, compare against the baseline and report
 *       drift.</li>
 * </ol>
 *
 * <p>In real test code you would skip step 4 and use
 * {@code com.demcha.compose.testing.layout.LayoutSnapshotAssertions
 * .assertMatches(document, "templates/invoice/invoice_baseline")}
 * — that helper is what every {@code *LayoutSnapshotTest} in the
 * GraphCompose source tree calls.</p>
 *
 * <p>See {@code docs/recipes/streaming.md} and the existing
 * {@code BuiltInTemplateLayoutSnapshotTest} for the production
 * pattern.</p>
 *
 * @author Artem Demchyshyn
 */
public final class LayoutSnapshotRegressionExample {

    private LayoutSnapshotRegressionExample() {
    }

    /**
     * Composes the sample invoice through {@link InvoiceTemplateV2},
     * extracts the layout snapshot, writes (or verifies) a JSON
     * baseline alongside the PDF, and renders the PDF for visual
     * inspection.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering, snapshot extraction, or baseline IO fails
     */
    public static Path generate() throws Exception {
        BusinessTheme theme = BusinessTheme.modern();
        Path pdfFile = ExampleOutputPaths.prepare("invoice-snapshot-regression.pdf");
        Path baselineFile = ExampleOutputPaths.prepare("invoice-snapshot-regression.layout.json");

        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .pageBackground(theme.pageBackground())
                .margin(DocumentInsets.of(28))
                .create()) {
            new InvoiceTemplateV2(theme).compose(document, ExampleDataFactory.sampleInvoice());

            // Step 1: extract the deterministic post-layout snapshot.
            LayoutSnapshot snapshot = document.layoutSnapshot();
            String actualJson = LayoutSnapshotJson.toJson(snapshot);

            // Step 2: write or verify the JSON baseline. In real test
            // code, this whole block is replaced with one call to
            // LayoutSnapshotAssertions.assertMatches(...).
            verifyOrWriteBaseline(baselineFile, actualJson);

            // Step 3: render the same session to PDF for human review.
            Files.write(pdfFile, document.toPdfBytes());
        }

        return pdfFile;
    }

    /**
     * Compares {@code actualJson} against the baseline at
     * {@code baselineFile}. On first run the baseline does not exist
     * and the method writes it; on subsequent runs the method reads
     * the baseline and prints either a "match" or a "drift" message.
     *
     * <p>Real production tests would throw an {@link AssertionError}
     * on drift — for the runnable example we just print so adopters
     * can see what the diagnostic looks like without forcing a
     * non-zero exit.</p>
     */
    private static void verifyOrWriteBaseline(Path baselineFile, String actualJson) throws Exception {
        if (Files.notExists(baselineFile)) {
            Files.writeString(baselineFile, actualJson);
            System.out.println("[snapshot] wrote new baseline at " + baselineFile);
            return;
        }

        String expectedJson = LayoutSnapshotJson.normalizeLineEndings(Files.readString(baselineFile));
        if (!expectedJson.endsWith("\n")) {
            expectedJson = expectedJson + "\n";
        }

        if (expectedJson.equals(actualJson)) {
            System.out.println("[snapshot] layout matches baseline at " + baselineFile);
        } else {
            // In a real test this would be an AssertionError. The
            // example just reports so the runnable smoke does not
            // fail when adopters intentionally tweak the template.
            Path actualPath = baselineFile.resolveSibling(
                    baselineFile.getFileName().toString().replace(".layout.json", ".layout.actual.json"));
            Files.writeString(actualPath, actualJson);
            System.out.println("[snapshot] LAYOUT DRIFT detected. Expected: " + baselineFile
                    + " Actual: " + actualPath
                    + " Re-run after deleting the baseline if the change is intentional.");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
