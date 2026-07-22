package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.DocumentDsl;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Raster-slide mode is a pixel-exact copy of the PDF raster: one full-slide
 * picture per page whose bytes equal the independently encoded
 * {@code session.toImages} output at the same DPI. The default (no builder
 * flag) stays the editable vector mode.
 */
class PptxRasterSlidesTest {

    private static final int DPI = 144;

    @Test
    void everyPageBecomesOneFullSlidePictureWithThePdfRasterBytes() throws Exception {
        try (DocumentSession session = composeTwoPages()) {
            byte[] pptx = session.render(
                    PptxFixedLayoutBackend.builder().rasterSlides(DPI).build());
            List<BufferedImage> pdfPages = session.toImages(DPI);

            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                assertThat(show.getSlides()).hasSameSizeAs(pdfPages);
                for (int pageIndex = 0; pageIndex < pdfPages.size(); pageIndex++) {
                    XSLFSlide slide = show.getSlides().get(pageIndex);
                    List<XSLFShape> shapes = slide.getShapes();
                    assertThat(shapes).hasSize(1);
                    XSLFPictureShape picture = (XSLFPictureShape) shapes.get(0);

                    Rectangle2D anchor = picture.getAnchor();
                    assertThat(anchor.getX()).isZero();
                    assertThat(anchor.getY()).isZero();
                    assertThat(anchor.getWidth()).isEqualTo(400.0);
                    assertThat(anchor.getHeight()).isEqualTo(300.0);

                    try (ByteArrayOutputStream expected = new ByteArrayOutputStream()) {
                        ImageIO.write(pdfPages.get(pageIndex), "png", expected);
                        assertThat(picture.getPictureData().getData())
                                .as("slide %d picture must be the PDF raster", pageIndex)
                                .isEqualTo(expected.toByteArray());
                    }
                }
            }
        }
    }

    @Test
    void theDefaultModeStaysVector() throws Exception {
        try (DocumentSession session = composeTwoPages()) {
            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                assertThat(show.getSlides().get(0).getShapes())
                        .noneMatch(XSLFPictureShape.class::isInstance);
            }
        }
    }

    private static DocumentSession composeTwoPages() {
        DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create();
        DocumentDsl dsl = session.dsl();
        session.add(dsl.shape().name("Card").size(160, 60)
                .fillColor(DocumentColor.ROYAL_BLUE)
                .stroke(DocumentStroke.of(DocumentColor.BLACK, 2))
                .cornerRadius(8)
                .build());
        session.add(new PageBreakNode("Break", DocumentInsets.zero()));
        session.add(dsl.shape().name("Second").size(120, 40)
                .fillColor(DocumentColor.ORANGE)
                .build());
        return session;
    }
}
