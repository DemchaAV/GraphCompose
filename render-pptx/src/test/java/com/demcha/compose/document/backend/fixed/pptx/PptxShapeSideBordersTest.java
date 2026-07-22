package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxShapeFragmentRenderHandler;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.layout.payloads.SideBorders;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.engine.components.content.shape.Stroke;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Per-side borders draw as separate edge lines at the exact flipped edge
 * coordinates, with no rectangle shape when the payload carries neither fill
 * nor uniform stroke — the PPTX twin of the PDF handler's side-border pass.
 */
class PptxShapeSideBordersTest {

    @Test
    void topAndBottomBordersDrawAsEdgeLinesWithoutARectangle() throws Exception {
        try (XMLSlideShow show = new XMLSlideShow()) {
            PptxRenderSession session = new PptxRenderSession(show, 300, 200, 1);
            PptxRenderEnvironment environment = new PptxRenderEnvironment(show, session, 0, 200);

            ShapeFragmentPayload payload = new ShapeFragmentPayload(
                    null, null, DocumentCornerRadius.ZERO, null, null,
                    new SideBorders(new Stroke(Color.BLACK, 1), null, new Stroke(Color.BLACK, 2), null));
            // 120×40 box whose bottom edge sits 100pt above the page bottom on a
            // 200pt page: slide-space top edge = 60, bottom edge = 100.
            PlacedFragment fragment = new PlacedFragment(
                    "root/box", 0, 0, 50, 100, 120, 40, null, null, payload);

            new PptxShapeFragmentRenderHandler().render(fragment, payload, environment);

            List<XSLFShape> shapes = show.getSlides().get(0).getShapes();
            assertThat(shapes).hasSize(2);

            Rectangle2D top = shapes.get(0).getAnchor();
            assertThat(top.getY()).isEqualTo(60.0);
            assertThat(top.getX()).isEqualTo(50.0);
            assertThat(top.getWidth()).isEqualTo(120.0);
            assertThat(top.getHeight()).isEqualTo(0.0);

            Rectangle2D bottom = shapes.get(1).getAnchor();
            assertThat(bottom.getY()).isEqualTo(100.0);
            assertThat(bottom.getWidth()).isEqualTo(120.0);
        }
    }
}
