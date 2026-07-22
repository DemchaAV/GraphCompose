package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.layout.payloads.ParagraphSvgSpan;
import com.demcha.compose.document.layout.payloads.ResolvedSvgLayer;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.style.DocumentDashPattern;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentLineJoin;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.engine.components.content.ImageData;
import com.demcha.compose.engine.components.content.shape.Stroke;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PptxInlineSvgRasterizerTest {

    @Test
    void keepsSimpleInBoundsLayersNative() {
        ParagraphSvgSpan span = span(layer(rectangle(0.1, 0.1, 0.9, 0.9), null,
                DocumentDashPattern.NONE, DocumentLineCap.BUTT, DocumentLineJoin.MITER));

        assertThat(PptxInlineSvgRasterizer.requiresRaster(span)).isFalse();
    }

    @Test
    void selectsFallbackForViewBoxBleedClipsAndExactStrokeStyles() {
        ParagraphSvgSpan offCanvas = span(layer(rectangle(-0.2, 0, 1.2, 1), null,
                DocumentDashPattern.NONE, DocumentLineCap.BUTT, DocumentLineJoin.MITER));
        ParagraphSvgSpan clipped = span(layer(rectangle(0, 0, 1, 1),
                rectangle(0, 0, 0.5, 1), DocumentDashPattern.NONE,
                DocumentLineCap.BUTT, DocumentLineJoin.MITER));
        ParagraphSvgSpan styledStroke = span(new ResolvedSvgLayer(
                List.of(DocumentPathSegment.moveTo(0.1, 0.5),
                        DocumentPathSegment.lineTo(0.9, 0.5)),
                null, null, new Stroke(Color.BLACK, 1), null,
                DocumentDashPattern.of(2, 1), DocumentLineCap.ROUND,
                DocumentLineJoin.BEVEL, null));

        assertThat(PptxInlineSvgRasterizer.requiresRaster(offCanvas)).isTrue();
        assertThat(PptxInlineSvgRasterizer.requiresRaster(clipped)).isTrue();
        assertThat(PptxInlineSvgRasterizer.requiresRaster(styledStroke)).isTrue();
    }

    @Test
    void rasterFallbackAppliesLayerClipInsideImplicitViewBox() throws Exception {
        ParagraphSvgSpan span = span(layer(rectangle(-0.2, 0, 1.2, 1),
                rectangle(0, 0, 0.5, 1), DocumentDashPattern.NONE,
                DocumentLineCap.BUTT, DocumentLineJoin.MITER));

        ImageData imageData = PptxInlineSvgRasterizer.rasterize(span);
        var image = ImageIO.read(new ByteArrayInputStream(imageData.getBytes()));

        assertThat(new Color(image.getRGB(image.getWidth() / 4, image.getHeight() / 2), true).getAlpha())
                .isGreaterThan(200);
        assertThat(new Color(image.getRGB(image.getWidth() * 3 / 4, image.getHeight() / 2), true).getAlpha())
                .isZero();
    }

    @Test
    void mapsExactDashCapAndJoinIntoRasterStroke() {
        ResolvedSvgLayer layer = new ResolvedSvgLayer(
                List.of(DocumentPathSegment.moveTo(0.1, 0.5),
                        DocumentPathSegment.lineTo(0.9, 0.5)),
                null, null, new Stroke(Color.BLACK, 1), null,
                DocumentDashPattern.of(2, 1), DocumentLineCap.ROUND,
                DocumentLineJoin.BEVEL, null);

        BasicStroke stroke = PptxInlineSvgRasterizer.awtStroke(1, layer);

        assertThat(stroke.getDashArray()).containsExactly(2.0f, 1.0f);
        assertThat(stroke.getEndCap()).isEqualTo(BasicStroke.CAP_ROUND);
        assertThat(stroke.getLineJoin()).isEqualTo(BasicStroke.JOIN_BEVEL);
    }

    private static ParagraphSvgSpan span(ResolvedSvgLayer layer) {
        return new ParagraphSvgSpan(List.of(layer), 20, 20,
                InlineImageAlignment.CENTER, 0, null);
    }

    private static ResolvedSvgLayer layer(List<DocumentPathSegment> segments,
                                          List<DocumentPathSegment> clip,
                                          DocumentDashPattern dash,
                                          DocumentLineCap cap,
                                          DocumentLineJoin join) {
        return new ResolvedSvgLayer(segments, Color.RED, null, null, null,
                dash, cap, join, clip);
    }

    private static List<DocumentPathSegment> rectangle(double left,
                                                       double bottom,
                                                       double right,
                                                       double top) {
        return List.of(
                DocumentPathSegment.moveTo(left, bottom),
                DocumentPathSegment.lineTo(right, bottom),
                DocumentPathSegment.lineTo(right, top),
                DocumentPathSegment.lineTo(left, top),
                DocumentPathSegment.close());
    }
}
