package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.DocumentDsl;
import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.BarcodeBuilder;
import com.demcha.compose.document.exceptions.UnsupportedNodeCapabilityException;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderContext;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentStroke;
import org.apache.poi.sl.usermodel.StrokeStyle;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFConnectorShape;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;

import java.awt.Color;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end geometry identity for the first vector payload slice: the same
 * resolved {@link LayoutGraph} renders to a .pptx whose shape anchors sit
 * exactly on the fragment boxes, across a page break, with fills, strokes,
 * corner radii, and line orientation surviving a POI round-trip.
 */
class PptxFixedLayoutBackendTest {

    private static DocumentSession composeVectorDocument() {
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
        session.add(dsl.shape().name("Plain").size(120, 40)
                .fillColor(DocumentColor.ORANGE)
                .build());
        session.add(new EllipseBuilder().name("Dot").circle(30)
                .fillColor(DocumentColor.DARK_GRAY)
                .build());
        session.add(new LineBuilder().name("Rule").horizontal(200)
                .color(DocumentColor.GRAY)
                .thickness(2)
                .build());
        session.add(new PageBreakNode("Break", DocumentInsets.zero()));
        session.add(dsl.shape().name("SecondPage").size(80, 80)
                .stroke(DocumentStroke.of(DocumentColor.DARK_GRAY, 1.5))
                .build());
        return session;
    }

    @Test
    void rendersVectorFragmentsAtTheExactGraphCoordinates() throws Exception {
        try (DocumentSession session = composeVectorDocument()) {
            LayoutGraph graph = session.render(new GraphCapturingBackend());
            byte[] pptx = session.render(new PptxFixedLayoutBackend());

            assertThat(graph.totalPages()).isEqualTo(2);
            PptxGeometryAssertions.assertGeometryMatches(graph, pptx);
        }
    }

    @Test
    void fillAndStrokeSurviveThePoiRoundTrip() throws Exception {
        try (DocumentSession session = composeVectorDocument()) {
            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                XSLFAutoShape card = (XSLFAutoShape) show.getSlides().get(0).getShapes().get(0);
                assertThat(card.getFillColor())
                        .isEqualTo(DocumentColor.ROYAL_BLUE.color());
                assertThat(card.getLineColor()).isEqualTo(Color.BLACK);
                assertThat(card.getLineWidth()).isEqualTo(2.0);

                XSLFAutoShape outlineOnly = (XSLFAutoShape) show.getSlides().get(1).getShapes().get(0);
                assertThat(outlineOnly.getFillColor()).isNull();
                assertThat(outlineOnly.getLineWidth()).isEqualTo(1.5);

                // Card is 160×60 with radius 8: the roundRect adjust value is the
                // radius as a fraction of the smaller side — 8/60 in 1/100000 units.
                CTShape cardXml = (CTShape) card.getXmlObject();
                assertThat(cardXml.getSpPr().getPrstGeom().getAvLst().getGdArray(0).getFmla())
                        .isEqualTo("val 13333");
            }
        }
    }

    @Test
    void diagonalLinesCarryOppositeVerticalFlips() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(new LineBuilder().name("Descending").diagonal(120, 60)
                    .color(DocumentColor.BLACK).thickness(2)
                    .build());
            session.add(new LineBuilder().name("Ascending").size(120, 60).from(0, 60).to(120, 0)
                    .color(DocumentColor.BLACK).thickness(2)
                    .dashed(4, 2).lineCap(DocumentLineCap.ROUND)
                    .build());
            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                XSLFConnectorShape first = (XSLFConnectorShape) show.getSlides().get(0).getShapes().get(0);
                XSLFConnectorShape second = (XSLFConnectorShape) show.getSlides().get(0).getShapes().get(1);
                assertThat(first.getFlipVertical())
                        .as("opposite slopes must carry opposite flips")
                        .isNotEqualTo(second.getFlipVertical());
                assertThat(second.getLineDash()).isEqualTo(StrokeStyle.LineDash.DASH);
                assertThat(second.getLineCap()).isEqualTo(StrokeStyle.LineCap.ROUND);
            }
        }
    }

    @Test
    void writeStreamsAValidDocumentAndLeavesTheStreamOpen() throws Exception {
        try (DocumentSession session = composeVectorDocument()) {
            LayoutGraph graph = session.render(new GraphCapturingBackend());
            class ProbeStream extends java.io.ByteArrayOutputStream {
                boolean closed;

                @Override
                public void close() {
                    closed = true;
                }
            }
            ProbeStream output = new ProbeStream();
            new PptxFixedLayoutBackend().write(graph,
                    new FixedLayoutRenderContext(graph.canvas(), java.util.List.of(), null, output));
            assertThat(output.closed).as("caller-owned stream must stay open").isFalse();
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(output.toByteArray()))) {
                assertThat(show.getSlides()).hasSize(2);
            }
        }
    }

    @Test
    void unsupportedPayloadFailsWithACapabilityError() {
        // Every engine payload now has a handler, so the diagnostic is pinned
        // with a payload type the registry can never know, rendered through
        // the public render(graph, context) path.
        record UnknownPayload() {
        }
        com.demcha.compose.document.layout.LayoutCanvas canvas =
                new com.demcha.compose.document.layout.LayoutCanvas(
                        300, 200, 280, 180,
                        com.demcha.compose.engine.components.style.Margin.of(10));
        com.demcha.compose.document.layout.LayoutGraph graph =
                new com.demcha.compose.document.layout.LayoutGraph(
                        canvas, 1, java.util.List.of(),
                        java.util.List.of(new com.demcha.compose.document.layout.PlacedFragment(
                                "root/unknown", 0, 0, 10, 10, 50, 50, null, null,
                                new UnknownPayload())));
        assertThatThrownBy(() -> new PptxFixedLayoutBackend().render(graph,
                new com.demcha.compose.document.backend.fixed.FixedLayoutRenderContext(
                        canvas, java.util.List.of(), null, null)))
                .isInstanceOf(UnsupportedNodeCapabilityException.class)
                .hasMessageContaining("UnknownPayload");
    }
}
