package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.testing.visual.PdfVisualRegression;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives a consumer-style "compose a document, then pixel-test the rendered
 * PDF" workflow entirely through the public {@code
 * com.demcha.compose.testing.visual} surface — no package-private access.
 *
 * <p>Sibling to {@code LayoutSnapshotPublicApiDogfoodTest} on the semantic
 * layer; it proves the published harness is sufficient for downstream adoption
 * and guards against accidental private-to-public regressions in CI.</p>
 */
class PublicVisualApiDogfoodTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldSupportConsumerPixelRegressionThroughPublicApi() throws Exception {
        Path baselineRoot = tempDir.resolve("pdf-baselines");
        String baselineName = "consumer_simple_document";
        byte[] pdfBytes = renderConsumerDocument();

        PdfVisualRegression harness = PdfVisualRegression.standard().baselineRoot(baselineRoot);

        // 1. Consumer blesses a fresh baseline via the public approve flag.
        String previous = System.getProperty(PdfVisualRegression.APPROVE_PROPERTY);
        try {
            System.setProperty(PdfVisualRegression.APPROVE_PROPERTY, "true");
            harness.assertMatchesBaseline(baselineName, pdfBytes);
        } finally {
            restoreProperty(PdfVisualRegression.APPROVE_PROPERTY, previous);
        }

        assertThat(baselineRoot.resolve(baselineName + "-page-0.png"))
                .as("approve mode should write a page-0 baseline")
                .exists()
                .isRegularFile();

        // 2. A second render of the same document matches the just-written baseline.
        harness.assertMatchesBaseline(baselineName, pdfBytes);

        // 3. A matching run must not leave a mismatch sidecar.
        assertThat(baselineRoot.resolve(baselineName + "-page-0.actual.png"))
                .as("a matching run must not leave a mismatch sidecar")
                .doesNotExist();
    }

    private byte[] renderConsumerDocument() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(18, 18, 18, 18)
                .create()) {
            document.dsl()
                    .pageFlow()
                    .name("ConsumerRoot")
                    .spacing(8)
                    .addParagraph(paragraph -> paragraph
                            .name("Title")
                            .text("Consumer pixel baseline")
                            .textStyle(DocumentTextStyle.DEFAULT))
                    .addShape(shape -> shape
                            .name("AccentBox")
                            .size(120, 32))
                    .build();
            return document.toPdfBytes();
        }
    }

    private void restoreProperty(String key, String previous) {
        if (previous == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, previous);
        }
    }
}
