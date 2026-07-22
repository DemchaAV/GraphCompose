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
 * Pins the hook page's headline promise at the PPTX structural level: the
 * slide is built from native shapes, and the only picture is the clip-masked
 * logo composite. A clip-safety regression that rasters more composites would
 * silently falsify the README's editability claim — this guard catches it.
 */
class TwinOutputNativeShapeTest {

    @Test
    void theSlideStaysNativeApartFromTheClippedLogo() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.SLIDE_16_9)
                .pageBackground(DocumentColor.rgb(9, 12, 27))
                .margin(DocumentInsets.zero())
                .create()) {
            TwinOutputExample.compose(document);
            byte[] deck = document.toPptxBytes();
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(deck))) {
                assertThat(show.getSlides()).hasSize(1);
                List<XSLFShape> shapes = show.getSlides().get(0).getShapes();
                long pictures = shapes.stream().filter(XSLFPictureShape.class::isInstance).count();
                assertThat(pictures)
                        .as("only the clip-masked logo lockup rasterizes")
                        .isEqualTo(1);
                assertThat(shapes.size() - pictures)
                        .as("the rest of the page lands as native shapes")
                        .isGreaterThan(60);
            }
        }
    }
}
