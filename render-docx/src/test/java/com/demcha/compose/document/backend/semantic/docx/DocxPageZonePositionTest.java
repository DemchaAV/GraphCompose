package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.SemanticBackend;
import com.demcha.compose.document.backend.semantic.SemanticExportContext;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentOutputOptions;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A header or footer sits as far from its page edge as the page puts it.
 *
 * <p>Nothing was written, so Word used its own distance — 36pt — and the probe's footer sat
 * 14.5pt higher than the page draws it, on every page. The engine does not state the
 * distance either: a zone is a band of a given height against the edge, with its content
 * laid out inside it from the top. So the distance is read from where the content landed
 * in the resolved layout.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxPageZonePositionTest {

    private static final double TWIPS_PER_POINT = 20.0;

    @Test
    void aFootersDistanceFollowsItsBandRatherThanWordsDefault() throws Exception {
        // The one relation that holds whatever the font measures: content is laid from the
        // band's top, so a band 30pt taller lifts its content 30pt further from the edge.
        long shallow = footerDistance(zone(DocumentHeaderFooterZone.FOOTER, 30, DocumentInsets.zero()));
        long deep = footerDistance(zone(DocumentHeaderFooterZone.FOOTER, 60, DocumentInsets.zero()));

        assertThat(deep - shallow).isEqualTo(Math.round(30 * TWIPS_PER_POINT));
        assertThat(shallow)
                .as("inside the band, and not Word's 720-twip default")
                .isBetween(0L, Math.round(30 * TWIPS_PER_POINT))
                .isNotEqualTo(720L);
    }

    @Test
    void aHeadersDistanceIsItsContentsTopFromThePageTop() throws Exception {
        // A header's content starts at the band's top, so its padding is exactly the gap
        // between the page's top edge and the content.
        long distance = headerDistance(zone(DocumentHeaderFooterZone.HEADER, 40, new DocumentInsets(9, 0, 0, 0)));

        assertThat(distance).isEqualTo(Math.round(9 * TWIPS_PER_POINT));
    }

    @Test
    void withoutALayoutAFooterFallsBackToItsOwnPadding() throws Exception {
        // A document that cannot be laid out still exports, and a zone's own padding on
        // that edge is the nearest thing to the distance it states.
        long distance = footerDistanceWithoutLayout(
                zone(DocumentHeaderFooterZone.FOOTER, 30, new DocumentInsets(0, 0, 7, 0)));

        assertThat(distance).isEqualTo(Math.round(7 * TWIPS_PER_POINT));
    }

    private static DocumentPageZone zone(DocumentHeaderFooterZone kind, double height, DocumentInsets padding) {
        return DocumentPageZone.builder()
                .zone(kind)
                .height(height)
                .padding(padding)
                .content(page -> new ParagraphBuilder().name("ZoneLine").text("Chrome").build())
                .build();
    }

    private static long footerDistance(DocumentPageZone zone) throws Exception {
        CTPageMar margin = marginOf(exportWithLayout(zone));
        return margin.getFooter() == null ? -1 : Long.parseLong(String.valueOf(margin.getFooter()));
    }

    private static long headerDistance(DocumentPageZone zone) throws Exception {
        CTPageMar margin = marginOf(exportWithLayout(zone));
        return margin.getHeader() == null ? -1 : Long.parseLong(String.valueOf(margin.getHeader()));
    }

    private static long footerDistanceWithoutLayout(DocumentPageZone zone) throws Exception {
        CTPageMar margin = marginOf(exportWithoutLayout(zone));
        return margin.getFooter() == null ? -1 : Long.parseLong(String.valueOf(margin.getFooter()));
    }

    private static CTPageMar marginOf(byte[] docx) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            return (CTPageMar) document.getDocument().getBody().getSectPr().getPgMar().copy();
        }
    }

    private static byte[] exportWithLayout(DocumentPageZone zone) throws Exception {
        try (DocumentSession session = session(zone)) {
            return session.export(new DocxSemanticBackend());
        }
    }

    /** The same document handed to the backend with no layout, the way a bare caller does. */
    private static byte[] exportWithoutLayout(DocumentPageZone zone) throws Exception {
        Captured captured = new Captured();
        try (DocumentSession session = session(zone)) {
            session.export(captured);
            return new DocxSemanticBackend().export(captured.graph,
                    new SemanticExportContext(captured.canvas, java.util.List.of(), null,
                            captured.options));
        }
    }

    private static DocumentSession session(DocumentPageZone zone) {
        DocumentSession session = GraphCompose.document()
                .pageSize(400, 600)
                .margin(DocumentInsets.of(40))
                .create();
        session.chrome().zone(zone);
        session.pageFlow(page -> page.addParagraph(p -> p.text("Body")));
        return session;
    }

    private static final class Captured implements SemanticBackend<byte[]> {

        private DocumentGraph graph;
        private LayoutCanvas canvas;
        private DocumentOutputOptions options;

        @Override
        public String name() {
            return "capture";
        }

        @Override
        public byte[] export(DocumentGraph documentGraph, SemanticExportContext context) {
            this.graph = documentGraph;
            this.canvas = context.canvas();
            this.options = context.outputOptions();
            return new byte[0];
        }
    }
}
