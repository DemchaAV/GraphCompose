package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRegularTextRun;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Translucent colors travel natively into DrawingML: POI writes an
 * {@code <a:alpha>} child for any color whose alpha is below 255, so the
 * capability matrix's "native alpha on every surface" claim is pinned here
 * for shape fills and text runs rather than trusted implicitly.
 */
class PptxAlphaTest {

    @Test
    void translucentFillAndTextCarryNativeAlpha() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            // Table cell fills travel through the identical XSLFAutoShape
            // setFillColor path this shape pins, so the matrix's "table
            // paint" claim rides on the same mechanism.
            session.add(session.dsl().shape().name("Veil").size(120, 40)
                    .fillColor(DocumentColor.rgba(20, 80, 95, 128))
                    .stroke(com.demcha.compose.document.style.DocumentStroke.of(
                            DocumentColor.rgba(10, 10, 10, 64), 2))
                    .build());
            session.add(new ParagraphBuilder().name("Ghost")
                    .text("watermark-ish")
                    .textStyle(DocumentTextStyle.builder()
                            .color(DocumentColor.rgba(0, 0, 0, 128))
                            .build())
                    .build());
            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                XSLFAutoShape veil = show.getSlides().get(0).getShapes().stream()
                        .filter(XSLFAutoShape.class::isInstance)
                        .map(XSLFAutoShape.class::cast)
                        .findFirst().orElseThrow();
                assertThat(veil.getFillColor().getAlpha())
                        .as("shape fill alpha survives the POI round-trip")
                        .isEqualTo(128);
                // POI's getLineColor ignores the alpha child, so the stroke
                // claim is pinned at the XML level PowerPoint actually reads.
                var strokeColor = ((org.openxmlformats.schemas.presentationml.x2006.main.CTShape)
                        veil.getXmlObject()).getSpPr().getLn().getSolidFill().getSrgbClr();
                assertThat(strokeColor.sizeOfAlphaArray())
                        .as("stroke color must carry an explicit alpha child")
                        .isEqualTo(1);
                assertThat(((Number) strokeColor.getAlphaArray(0).getVal()).intValue())
                        .isBetween(24_000, 26_000);

                XSLFTextBox ghost = show.getSlides().get(0).getShapes().stream()
                        .filter(XSLFTextBox.class::isInstance)
                        .map(XSLFTextBox.class::cast)
                        .findFirst().orElseThrow();
                CTRegularTextRun run = (CTRegularTextRun) ghost.getTextParagraphs().get(0)
                        .getTextRuns().get(0).getXmlObject();
                var color = run.getRPr().getSolidFill().getSrgbClr();
                assertThat(color.sizeOfAlphaArray())
                        .as("text run color must carry an explicit alpha child")
                        .isEqualTo(1);
                // 128/255 in DrawingML thousandths-of-a-percent, rounding-tolerant.
                assertThat(((Number) color.getAlphaArray(0).getVal()).intValue())
                        .isBetween(49_000, 51_000);
            }
        }
    }
}
