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
 * Guards the Maven-banner slide's editability promise at the PPTX structural
 * level: the panels, tags, code, diagram and connectors land as native shapes
 * and text, and the only rasterised element is the SVG checkmark in the "Maven
 * Central" badge. A clip-safety or SVG-handling regression that flattened the
 * panels into pictures would silently falsify that promise — this catches it.
 */
class MavenBannerNativeShapeTest {

    @Test
    void theSlideStaysNativeApartFromTheCheckIcon() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.SLIDE_16_9)
                .pageBackground(DocumentColor.rgb(13, 17, 33))
                .margin(DocumentInsets.zero())
                .create()) {
            MavenBannerPptxExample.compose(document);
            byte[] deck = document.toPptxBytes();
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(deck))) {
                assertThat(show.getSlides()).hasSize(1);
                List<XSLFShape> shapes = show.getSlides().get(0).getShapes();
                long pictures = shapes.stream().filter(XSLFPictureShape.class::isInstance).count();
                assertThat(pictures)
                        .as("only the SVG checkmark rasterises")
                        .isEqualTo(1);
                assertThat(shapes.size() - pictures)
                        .as("the rest of the banner lands as native shapes")
                        .isGreaterThan(100);
            }
        }
    }
}
