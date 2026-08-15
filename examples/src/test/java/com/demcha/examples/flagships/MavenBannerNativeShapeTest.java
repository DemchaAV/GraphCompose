package com.demcha.examples.flagships;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the Maven deck's editability promise at the PPTX structural level: the
 * panels, tags, code, diagram, connectors, table and charts land as native shapes
 * and text, and the only rasterised element in the whole deck is the SVG checkmark
 * in the "Maven Central" badge on slide 1. A clip-safety or SVG-handling regression
 * that flattened any of it into pictures would silently falsify that promise.
 *
 * <p>The charts matter most here. They are compiled to the same primitives as
 * everything else, so a deck claiming "native vector output" has to survive the
 * round trip as shapes rather than as an embedded image — which is exactly what a
 * chart-rendering shortcut would break, and nothing else would notice.</p>
 */
class MavenBannerNativeShapeTest {

    /** Banner, pipeline, measured, scaling, scripts. */
    private static final int SLIDES = 5;

    @Test
    void everySlideStaysNativeApartFromTheCheckIcon() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.SLIDE_16_9)
                .pageBackground(DocumentColor.rgb(13, 17, 33))
                .margin(DocumentInsets.zero())
                .create()) {
            MavenBannerPptxExample.compose(document);
            byte[] deck = document.toPptxBytes();
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(deck))) {
                assertThat(show.getSlides()).hasSize(SLIDES);

                long pictures = 0;
                for (int slide = 0; slide < SLIDES; slide++) {
                    List<XSLFShape> shapes = show.getSlides().get(slide).getShapes();
                    pictures += shapes.stream().filter(XSLFPictureShape.class::isInstance).count();
                    assertThat(shapes)
                            .as("slide %d carries native content", slide + 1)
                            .isNotEmpty();
                }

                assertThat(pictures)
                        .as("across the whole deck only the SVG checkmark rasterises — the table "
                                + "and both charts have to arrive as shapes, not as an image")
                        .isEqualTo(1);

                List<XSLFShape> banner = show.getSlides().get(0).getShapes();
                assertThat(banner.size() - 1)
                        .as("the banner itself lands as native shapes")
                        .isGreaterThan(100);
            }
        }
    }
}
