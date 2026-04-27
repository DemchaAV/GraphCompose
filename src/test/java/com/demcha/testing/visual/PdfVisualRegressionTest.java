package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfVisualRegressionTest {

    @Test
    void identicalImagesProduceZeroMismatch() {
        BufferedImage left = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        BufferedImage right = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < 20; x++) {
                int rgb = ((x * 12) << 16) | ((y * 12) << 8);
                left.setRGB(x, y, rgb);
                right.setRGB(x, y, rgb);
            }
        }

        ImageDiff.Result diff = ImageDiff.compare(left, right, 0);
        assertThat(diff.differs()).isFalse();
        assertThat(diff.mismatchedPixelCount()).isEqualTo(0);
        assertThat(diff.maxChannelDelta()).isEqualTo(0);
        assertThat(diff.withinBudget(0)).isTrue();
    }

    @Test
    void differentImagesReportMismatchAndProduceDiffImage() {
        BufferedImage left = solidColor(10, 10, 0x202020);
        BufferedImage right = solidColor(10, 10, 0x404040);

        ImageDiff.Result diff = ImageDiff.compare(left, right, 0);
        assertThat(diff.differs()).isTrue();
        assertThat(diff.mismatchedPixelCount()).isEqualTo(100);
        assertThat(diff.maxChannelDelta()).isEqualTo(0x20);
        assertThat(diff.diffImage()).isNotNull();
    }

    @Test
    void perPixelToleranceSwallowsTinyDifferences() {
        BufferedImage left = solidColor(8, 8, 0x404040);
        BufferedImage right = solidColor(8, 8, 0x434343);

        ImageDiff.Result diff = ImageDiff.compare(left, right, 5);
        assertThat(diff.mismatchedPixelCount()).isEqualTo(0);
        assertThat(diff.maxChannelDelta()).isEqualTo(3);
        assertThat(diff.differs()).isFalse();
    }

    @Test
    void differentSizesAreReportedAsMaxDelta() {
        BufferedImage left = solidColor(8, 8, 0xFFFFFF);
        BufferedImage right = solidColor(10, 8, 0xFFFFFF);

        ImageDiff.Result diff = ImageDiff.compare(left, right, 0);
        assertThat(diff.differs()).isTrue();
        assertThat(diff.mismatchedPixelCount()).isEqualTo(Long.MAX_VALUE);
        assertThat(diff.maxChannelDelta()).isEqualTo(255);
        assertThat(diff.diffImage()).isNull();
    }

    @Test
    void renderingTheSameDocumentTwiceProducesPixelIdenticalPages() throws Exception {
        byte[] first = renderSampleDocument();
        byte[] second = renderSampleDocument();

        PdfVisualRegression regression = PdfVisualRegression.standard();
        List<BufferedImage> firstPages = regression.renderPages(first);
        List<BufferedImage> secondPages = regression.renderPages(second);

        assertThat(firstPages).hasSize(secondPages.size());
        for (int page = 0; page < firstPages.size(); page++) {
            ImageDiff.Result diff = ImageDiff.compare(firstPages.get(page), secondPages.get(page), 0);
            assertThat(diff.differs())
                    .as("Deterministic render mismatch on page %d: %s", page, diff.summary())
                    .isFalse();
        }
    }

    @Test
    void approveModeWritesBaselineEvenWhenItDoesNotExist(@TempDir Path tmp) throws Exception {
        System.setProperty("graphcompose.visual.approve", "true");
        try {
            PdfVisualRegression regression = PdfVisualRegression.standard().baselineRoot(tmp);
            byte[] pdfBytes = renderSampleDocument();
            regression.assertMatchesBaseline("approve-sample", pdfBytes);

            assertThat(tmp.resolve("approve-sample-page-0.png")).exists();
        } finally {
            System.clearProperty("graphcompose.visual.approve");
        }
    }

    @Test
    void compareModeFailsWhenBaselineIsMissing(@TempDir Path tmp) throws Exception {
        System.clearProperty("graphcompose.visual.approve");
        PdfVisualRegression regression = PdfVisualRegression.standard().baselineRoot(tmp);
        byte[] pdfBytes = renderSampleDocument();

        assertThatThrownBy(() -> regression.assertMatchesBaseline("missing-baseline", pdfBytes))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Missing baseline")
                .hasMessageContaining("Re-run with");

        assertThat(tmp.resolve("missing-baseline-page-0.actual.png")).exists();
    }

    @Test
    void compareModePassesWhenBaselineMatches(@TempDir Path tmp) throws Exception {
        PdfVisualRegression regression = PdfVisualRegression.standard().baselineRoot(tmp);
        byte[] pdfBytes = renderSampleDocument();

        // Approve once
        System.setProperty("graphcompose.visual.approve", "true");
        try {
            regression.assertMatchesBaseline("baseline-roundtrip", pdfBytes);
        } finally {
            System.clearProperty("graphcompose.visual.approve");
        }

        // Then compare against the freshly approved baseline
        regression.assertMatchesBaseline("baseline-roundtrip", pdfBytes);
        assertThat(tmp.resolve("baseline-roundtrip-page-0.png")).exists();
    }

    @Test
    void compareModeFailsAndWritesDiffWhenRenderChanged(@TempDir Path tmp) throws Exception {
        PdfVisualRegression regression = PdfVisualRegression.standard().baselineRoot(tmp);

        // Bless one document as the baseline
        System.setProperty("graphcompose.visual.approve", "true");
        try {
            regression.assertMatchesBaseline("changed-render", renderSampleDocument());
        } finally {
            System.clearProperty("graphcompose.visual.approve");
        }

        // A different document (extra paragraph) must produce a diff
        byte[] altered = renderAlteredDocument();
        assertThatThrownBy(() -> regression.assertMatchesBaseline("changed-render", altered))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Visual diff over budget");

        assertThat(tmp.resolve("changed-render-page-0.diff.png")).exists();
        assertThat(tmp.resolve("changed-render-page-0.actual.png")).exists();
    }

    private static BufferedImage solidColor(int width, int height, int rgb) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, rgb);
            }
        }
        return image;
    }

    private static byte[] renderSampleDocument() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 320)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.compose(dsl -> dsl.pageFlow(page -> page
                    .addParagraph("GraphCompose visual regression smoke test")
                    .spacer(0, 12)
                    .addParagraph("Stable input then stable PDF then stable PNG.")));
            return session.toPdfBytes();
        }
    }

    private static byte[] renderAlteredDocument() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 320)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.compose(dsl -> dsl.pageFlow(page -> page
                    .addParagraph("GraphCompose visual regression smoke test")
                    .spacer(0, 12)
                    .addParagraph("Stable input then stable PDF then stable PNG.")
                    .spacer(0, 12)
                    .addParagraph("EXTRA LINE that should change the rendered pixels.")));
            return session.toPdfBytes();
        }
    }
}
