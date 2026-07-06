package com.demcha.compose.document.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.output.DocumentWatermark;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.testing.visual.ImageDiff;
import com.demcha.compose.testing.visual.PdfVisualRegression;

import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Covers the direct render-to-image API ({@code toImages}/{@code toImage}) that
 * rasterizes the in-memory document without the PDF byte round-trip.
 */
class DocumentSessionImageTest {

    private static DocumentSession singlePage() {
        DocumentSession session = GraphCompose.document()
                .pageSize(200, 140)
                .margin(DocumentInsets.of(12))
                .create();
        session.compose(dsl -> dsl.pageFlow(flow -> flow.addText("Rendered straight to an image.")));
        return session;
    }

    private static DocumentSession multiPage() {
        DocumentSession session = GraphCompose.document()
                .pageSize(200, 140)
                .margin(DocumentInsets.of(12))
                .create();
        session.compose(dsl -> dsl.pageFlow(flow -> {
            for (int i = 0; i < 30; i++) {
                flow.addText("Paragraph line number " + i + " of the multi-page render fixture.");
            }
        }));
        return session;
    }

    @Test
    void toImagesReturnsOneOpaqueImagePerPage() {
        try (DocumentSession session = multiPage()) {
            int pages = session.layoutGraph().totalPages();
            assertThat(pages).isGreaterThanOrEqualTo(2);

            List<BufferedImage> images = session.toImages(72);

            assertThat(images).hasSize(pages);
            for (BufferedImage image : images) {
                assertThat(image).isNotNull();
                assertThat(image.getType()).isEqualTo(BufferedImage.TYPE_INT_RGB);
                assertThat(image.getWidth()).isBetween(199, 201);   // 200pt @ 72dpi
                assertThat(image.getHeight()).isBetween(139, 141);  // 140pt @ 72dpi
            }
        }
    }

    @Test
    void imageDimensionsScaleWithDpi() {
        try (DocumentSession session = singlePage()) {
            BufferedImage at72 = session.toImage(0, 72);
            BufferedImage at144 = session.toImage(0, 144);

            assertThat(at72.getWidth()).isBetween(199, 201);
            assertThat(at144.getWidth()).isBetween(399, 401);
            assertThat(at144.getHeight()).isGreaterThan(at72.getHeight());
        }
    }

    @Test
    void renderedImageContainsPaintedContent() {
        try (DocumentSession session = singlePage()) {
            BufferedImage image = session.toImage(0, 144);
            assertThat(hasNonWhitePixel(image)).as("text should paint non-background pixels").isTrue();
        }
    }

    @Test
    void transparentRendersArgbWithTransparentBackground() {
        try (DocumentSession session = singlePage()) {
            BufferedImage opaque = session.toImage(0, 96, false);
            BufferedImage transparent = session.toImage(0, 96, true);

            assertThat(opaque.getType()).isEqualTo(BufferedImage.TYPE_INT_RGB);
            assertThat(transparent.getType()).isEqualTo(BufferedImage.TYPE_INT_ARGB);

            // The top-left corner sits inside the 12pt page margin, so no content
            // is painted there: opaque keeps a solid background, transparent stays
            // fully see-through.
            assertThat(alpha(opaque.getRGB(2, 2))).as("RGB background is opaque").isEqualTo(255);
            assertThat(alpha(transparent.getRGB(2, 2))).as("ARGB empty area is fully transparent").isZero();
        }
    }

    @Test
    void toImageReturnsTheRequestedPage() {
        try (DocumentSession session = multiPage()) {
            List<BufferedImage> all = session.toImages(96);
            assertThat(all).hasSizeGreaterThanOrEqualTo(2);

            BufferedImage page1 = session.toImage(1, 96);

            // The same page rendered both ways is pixel-identical...
            assertThat(ImageDiff.compare(all.get(1), page1, 0).withinBudget(0))
                    .as("toImage(1) matches toImages().get(1)").isTrue();
            // ...and a different page (distinct text) is not.
            assertThat(ImageDiff.compare(all.get(0), page1, 0).withinBudget(0))
                    .as("page 0 differs from page 1").isFalse();
        }
    }

    @Test
    void directRenderMatchesPdfByteRoundTrip() throws Exception {
        try (DocumentSession session = multiPage()) {
            PdfVisualRegression regression = PdfVisualRegression.standard();

            List<BufferedImage> direct = regression.renderPages(session);
            List<BufferedImage> roundTrip = regression.renderPages(session.toPdfBytes());

            assertThat(direct).hasSameSizeAs(roundTrip);
            for (int page = 0; page < direct.size(); page++) {
                ImageDiff.Result diff = ImageDiff.compare(roundTrip.get(page), direct.get(page), 0);
                assertThat(diff.withinBudget(0)).as("page %d is pixel-identical", page).isTrue();
            }
        }
    }

    @Test
    void postProcessingDecorationsLandInTheRaster() {
        // The watermark is applied by the post-processor inside the same build the
        // rasterizer reads, so it must show up in the image (parity with the PDF).
        try (DocumentSession plain = singlePage();
             DocumentSession watermarked = singlePage()) {
            watermarked.watermark(DocumentWatermark.builder().text("DRAFT").build());

            BufferedImage plainImage = plain.toImage(0, 96);
            BufferedImage watermarkedImage = watermarked.toImage(0, 96);

            assertThat(ImageDiff.compare(plainImage, watermarkedImage, 0).withinBudget(0))
                    .as("watermark changes the rendered pixels").isFalse();
        }
    }

    @Test
    void nonPositiveDpiIsRejected() {
        try (DocumentSession session = singlePage()) {
            assertThatThrownBy(() -> session.toImages(0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> session.toImages(-5)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> session.toImage(0, 0)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void pageIndexOutOfRangeIsRejected() {
        try (DocumentSession session = singlePage()) {
            assertThatThrownBy(() -> session.toImage(-1, 72)).isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> session.toImage(5, 72)).isInstanceOf(IndexOutOfBoundsException.class);
        }
    }

    @Test
    void renderingAnEmptyDocumentThrowsIllegalState() {
        try (DocumentSession session = GraphCompose.document().pageSize(200, 140).create()) {
            assertThatThrownBy(() -> session.toImages(72))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("empty document");
        }
    }

    private static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    private static boolean hasNonWhitePixel(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                    return true;
                }
            }
        }
        return false;
    }
}
