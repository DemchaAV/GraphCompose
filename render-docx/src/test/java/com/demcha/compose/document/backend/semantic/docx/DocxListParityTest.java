package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PDF ↔ DOCX list parity: the semantic Word export must resolve markers and
 * item text through the same shared rules as fixed-layout rendering — the
 * {@link ListMarker#defaultForDepth(int)} cascade for nested fallbacks and
 * {@link ListMarker#normalizeItemText(String, boolean)} for flat items —
 * so both outputs of one session agree.
 */
class DocxListParityTest {

    @Test
    void nestedFallbackFollowsTheDepthCascade() throws Exception {
        List<String> texts = exportTexts(flow -> flow
                .addList(list -> list
                        .name("Outline")
                        .addItem("alpha", l1 -> l1
                                .addItem("beta", l2 -> l2
                                        .addItem("gamma")))));

        assertThat(texts).contains("alpha", "beta", "gamma");
        assertThat(markerPerDepth(flow -> flow
                .addList(list -> list
                        .name("Outline")
                        .addItem("alpha", l1 -> l1
                                .addItem("beta", l2 -> l2
                                        .addItem("gamma"))))))
                .containsExactly("•", "◦", "▪");
    }

    @Test
    void explicitMarkersStillBeatTheCascade() throws Exception {
        List<String> markers = markerPerDepth(flow -> flow
                .addList(list -> list
                        .name("Outline")
                        .markerFor(1, ListMarker.custom("→"))
                        .addItem("alpha", l1 -> l1.addItem("beta"))));

        assertThat(markers.get(1)).isEqualTo("→");
        assertThat(markers.get(1)).isNotEqualTo("◦");
    }

    @Test
    void flatItemsStripAuthorTypedMarkers() throws Exception {
        List<String> texts = exportTexts(flow -> flow
                .addList("- dashed", "• bulleted", "* starred", "+ plussed"));

        // Word draws the marker, so the item's text is the item: an author-typed marker
        // that survived normalization would show up here as a leading "- " or "• ".
        assertThat(texts).contains("dashed", "bulleted", "starred", "plussed");
        assertThat(texts).noneMatch(t -> t.startsWith("- ") || t.startsWith("• "));
    }

    @Test
    void boldLeadIsNotMistakenForAMarker() throws Exception {
        List<String> texts = exportTexts(flow -> flow
                .addList("**bold** lead stays intact"));

        assertThat(texts).contains("**bold** lead stays intact");
    }

    @Test
    void blankFlatItemsAreDropped() throws Exception {
        List<String> texts = exportTexts(flow -> flow
                .addList("kept", "", "   "));

        assertThat(texts).contains("kept");
        // No empty list items for the blank ones: a numbered empty paragraph would draw
        // a marker with nothing beside it, the same defect the old marker-only paragraph
        // was.
        assertThat(texts.stream().filter(t -> !t.isBlank()).toList()).containsExactly("kept");
    }

    @Test
    void normalizeMarkersFalsePreservesRawItems() throws Exception {
        List<String> texts = exportTexts(flow -> flow
                .addList(list -> list
                        .name("Raw")
                        .normalizeMarkers(false)
                        .items("- raw dash survives")));

        assertThat(texts).contains("- raw dash survives");
    }

    private static List<String> exportTexts(
            Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> author) throws Exception {
        try (XWPFDocument document = export(author)) {
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .toList();
        }
    }

    /**
     * The marker each nesting depth resolved to.
     *
     * <p>Markers are the list definition's business now rather than the run text's, so
     * the cascade is read where it lives.</p>
     */
    private static List<String> markerPerDepth(
            Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> author) throws Exception {
        try (XWPFDocument document = export(author)) {
            return document.getNumbering()
                    .getAbstractNum(java.math.BigInteger.ZERO).getAbstractNum().getLvlList()
                    .stream()
                    .map(level -> level.getLvlText().getVal())
                    .toList();
        }
    }

    private static XWPFDocument export(
            Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> author) throws Exception {
        byte[] docxBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {
            var flow = session.dsl().pageFlow().name("Flow");
            author.accept(flow);
            flow.build();
            docxBytes = session.export(new DocxSemanticBackend());
        }
        return new XWPFDocument(new ByteArrayInputStream(docxBytes));
    }
}
