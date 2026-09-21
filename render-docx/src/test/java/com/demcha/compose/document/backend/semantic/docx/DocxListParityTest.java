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

        assertThat(markers.get(1))
                .as("the override wins over the cascade's ◦ at this depth")
                .isEqualTo("→");
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
        // Counted as list items, not as text: a numbered empty paragraph draws a marker
        // with nothing beside it — the same defect the marker-only paragraph used to be —
        // and filtering blank text before asserting would step right over it.
        assertThat(listItemCount(flow -> flow.addList("kept", "", "   ")))
                .as("one item survives; the blank ones leave no marker behind")
                .isEqualTo(1);
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

    /** How many paragraphs Word will draw a marker beside, whatever their text says. */
    private static long listItemCount(
            Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> author) throws Exception {
        try (XWPFDocument document = export(author)) {
            return document.getParagraphs().stream()
                    .filter(p -> p.getCTP().getPPr() != null && p.getCTP().getPPr().isSetNumPr())
                    .count();
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
