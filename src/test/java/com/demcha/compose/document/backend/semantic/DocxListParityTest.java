package com.demcha.compose.document.backend.semantic;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

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
@DisabledIfSystemProperty(named = "no.poi", matches = "true",
        disabledReason = "DocxSemanticBackend requires poi-ooxml; the no-poi profile validates the rest of the suite without it")
class DocxListParityTest {

    @Test
    void nestedFallbackFollowsTheDepthCascade() throws Exception {
        List<String> texts = exportTexts(flow -> flow
                .addList(list -> list
                        .name("Outline")
                        .addItem("alpha", l1 -> l1
                                .addItem("beta", l2 -> l2
                                        .addItem("gamma")))));

        assertThat(texts).contains("• alpha", "  ◦ beta", "    ▪ gamma");
    }

    @Test
    void explicitMarkersStillBeatTheCascade() throws Exception {
        List<String> texts = exportTexts(flow -> flow
                .addList(list -> list
                        .name("Outline")
                        .markerFor(1, ListMarker.custom("→"))
                        .addItem("alpha", l1 -> l1.addItem("beta"))));

        assertThat(texts).contains("  → beta");
        assertThat(texts).doesNotContain("  ◦ beta");
    }

    @Test
    void flatItemsStripAuthorTypedMarkers() throws Exception {
        List<String> texts = exportTexts(flow -> flow
                .addList("- dashed", "• bulleted", "* starred", "+ plussed"));

        assertThat(texts).contains("• dashed", "• bulleted", "• starred", "• plussed");
        assertThat(texts).noneMatch(t -> t.startsWith("• - ") || t.startsWith("• • "));
    }

    @Test
    void boldLeadIsNotMistakenForAMarker() throws Exception {
        List<String> texts = exportTexts(flow -> flow
                .addList("**bold** lead stays intact"));

        assertThat(texts).contains("• **bold** lead stays intact");
    }

    @Test
    void blankFlatItemsAreDropped() throws Exception {
        List<String> texts = exportTexts(flow -> flow
                .addList("kept", "", "   "));

        assertThat(texts).contains("• kept");
        // No marker-only paragraphs for the blank items.
        assertThat(texts).noneMatch(t -> t.trim().equals("•"));
    }

    @Test
    void normalizeMarkersFalsePreservesRawItems() throws Exception {
        List<String> texts = exportTexts(flow -> flow
                .addList(list -> list
                        .name("Raw")
                        .normalizeMarkers(false)
                        .items("- raw dash survives")));

        assertThat(texts).contains("• - raw dash survives");
    }

    private static List<String> exportTexts(
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
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .toList();
        }
    }
}
