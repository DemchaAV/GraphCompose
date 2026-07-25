package com.demcha.examples.flagships;

import com.demcha.compose.document.api.DocumentSession;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards that the flagship PDF documents re-emitted as PowerPoint decks land as
 * native, editable shapes — not a page-sized screenshot. Each twin shares its
 * source example's {@code compose(...)} and {@code document(...)} session
 * config, so the same {@code LayoutGraph} that prints the PDF is what the deck
 * is built from. The test opens the resulting {@code .pptx} with Apache POI and
 * asserts native shapes dominate, with only the genuinely un-vectorizable
 * regions (a clip-masked composite, a photo, a barcode raster) landing as
 * pictures. A clip-safety or backend regression that rasterised whole slides
 * would silently falsify the "editable in PowerPoint" claim — this catches it.
 */
class FlagshipPptxTwinNativeShapeTest {

    @Test
    void businessReportDeckStaysNative() throws Exception {
        byte[] deck;
        try (DocumentSession document =
                     BusinessReportExample.document(Path.of("target", "business-report-native.pptx")).create()) {
            BusinessReportExample.compose(document);
            deck = document.toPptxBytes();
        }
        assertMostlyNative(deck, 1, 2, 60);
    }

    @Test
    void financialReportDeckStaysNative() throws Exception {
        byte[] deck;
        try (DocumentSession document =
                     FinancialReportExample.document(Path.of("target", "financial-report-native.pptx")).create()) {
            FinancialReportExample.compose(document);
            deck = document.toPptxBytes();
        }
        // The masthead photo is a genuine bitmap; everything else stays native.
        assertMostlyNative(deck, 1, 2, 60);
    }

    @Test
    void masterShowcaseDeckStaysNative() throws Exception {
        byte[] deck;
        try (DocumentSession document =
                     MasterShowcaseExample.document(Path.of("target", "master-showcase-native.pptx")).create()) {
            MasterShowcaseExample.compose(document);
            deck = document.toPptxBytes();
        }
        // Multi-page report → one slide per page; the rotated clip-masked seal
        // and the two ZXing barcodes are the only expected pictures.
        assertMostlyNative(deck, 2, 4, 120);
    }

    /**
     * Opens the deck and asserts it is built from native shapes.
     *
     * @param deck           the rendered {@code .pptx} bytes
     * @param minSlides      one slide per resolved page — at least this many
     * @param maxPictures    the un-vectorizable regions that may rasterise
     * @param minNativeShapes native (non-picture) shapes summed over every slide
     */
    private static void assertMostlyNative(byte[] deck, int minSlides, int maxPictures,
                                           int minNativeShapes) throws Exception {
        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(deck))) {
            List<XSLFSlide> slides = show.getSlides();
            assertThat(slides.size())
                    .as("one slide per resolved page")
                    .isGreaterThanOrEqualTo(minSlides);

            long pictures = 0;
            long nativeShapes = 0;
            for (XSLFSlide slide : slides) {
                List<XSLFShape> shapes = slide.getShapes();
                long pics = shapes.stream().filter(XSLFPictureShape.class::isInstance).count();
                pictures += pics;
                nativeShapes += shapes.size() - pics;
            }
            assertThat(pictures)
                    .as("only genuinely un-vectorizable regions rasterise")
                    .isLessThanOrEqualTo(maxPictures);
            assertThat(nativeShapes)
                    .as("the document lands as native, editable shapes, not a screenshot")
                    .isGreaterThan(minNativeShapes);
        }
    }
}
