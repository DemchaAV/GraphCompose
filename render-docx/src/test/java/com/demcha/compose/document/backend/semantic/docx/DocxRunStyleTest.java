package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Per-run styling in the DOCX semantic backend.
 *
 * <p>An {@code InlineTextRun} carries its own style and falls back to the paragraph's
 * only when it has none. Applying the paragraph style to every run instead produced a
 * valid {@code .docx} in which a bold segment, an accent-coloured segment and plain text
 * were indistinguishable — no error, no warning, just a document missing the emphasis it
 * was told to carry. These pin the run's own style reaching the file, in a paragraph and
 * inside a table cell.</p>
 */
class DocxRunStyleTest {

    private static final DocumentTextStyle BASE = DocumentTextStyle.builder().size(11).build();
    private static final DocumentTextStyle BOLD = DocumentTextStyle.builder()
            .size(11).decoration(DocumentTextDecoration.BOLD).build();
    private static final DocumentTextStyle ACCENT = DocumentTextStyle.builder()
            .size(11).color(DocumentColor.rgb(192, 57, 43)).build();

    @Test
    void eachRunKeepsItsOwnStyleRatherThanTheParagraphFallback() throws Exception {
        List<XWPFRun> runs = paragraphRuns(flow -> flow.addParagraph(paragraph -> paragraph
                .textStyle(BASE)
                .inlineText("plain ")
                .inlineText("bold ", BOLD)
                .inlineText("accent", ACCENT)));

        assertThat(runs).hasSize(3);
        assertThat(runs.get(0).getText(0)).isEqualTo("plain ");
        assertThat(runs.get(0).isBold()).isFalse();

        assertThat(runs.get(1).getText(0)).isEqualTo("bold ");
        assertThat(runs.get(1).isBold()).isTrue();

        assertThat(runs.get(2).getText(0)).isEqualTo("accent");
        assertThat(runs.get(2).isBold()).isFalse();
        assertThat(runs.get(2).getColor()).isEqualToIgnoringCase("C0392B");
    }

    @Test
    void aRunWithoutItsOwnStyleTakesTheParagraphStyle() throws Exception {
        // The fallback InlineTextRun documents — it must survive the fix that stopped
        // applying it unconditionally.
        List<XWPFRun> runs = paragraphRuns(flow -> flow.addParagraph(paragraph -> paragraph
                .textStyle(BOLD)
                .inlineText("inherits")));

        assertThat(runs).hasSize(1);
        assertThat(runs.get(0).isBold()).isTrue();
    }

    @Test
    void strikethroughReachesTheDocument() throws Exception {
        List<XWPFRun> runs = paragraphRuns(flow -> flow.addParagraph(paragraph -> paragraph
                .textStyle(BASE)
                .inlineText("struck", DocumentTextStyle.builder()
                        .size(11).decoration(DocumentTextDecoration.STRIKETHROUGH).build())));

        assertThat(runs).hasSize(1);
        assertThat(runs.get(0).isStrikeThrough()).isTrue();
    }

    @Test
    void theRemainingDecorationsAlsoTravelPerRun() throws Exception {
        List<XWPFRun> runs = paragraphRuns(flow -> flow.addParagraph(paragraph -> paragraph
                .textStyle(BASE)
                .inlineText("i", decorated(DocumentTextDecoration.ITALIC))
                .inlineText("u", decorated(DocumentTextDecoration.UNDERLINE))
                .inlineText("bi", decorated(DocumentTextDecoration.BOLD_ITALIC))));

        assertThat(runs).hasSize(3);
        assertThat(runs.get(0).isItalic()).isTrue();
        assertThat(runs.get(0).isBold()).isFalse();
        assertThat(runs.get(1).getUnderline()).isEqualTo(UnderlinePatterns.SINGLE);
        assertThat(runs.get(2).isBold()).isTrue();
        assertThat(runs.get(2).isItalic()).isTrue();
    }

    @Test
    void aCodeChipCarriesItsOwnGlyphStyleRatherThanTheParagraphs() throws Exception {
        // A chip is lowered to a text run holding the chip's style, so per-run styling
        // means the chip's monospace face now reaches Word. Its background does not.
        List<XWPFRun> runs = paragraphRuns(flow -> flow.addParagraph(paragraph -> paragraph
                .textStyle(BASE)
                .inlineText("call ")
                .inlineCode("run()")));

        assertThat(runs).hasSize(2);
        assertThat(runs.get(0).getFontFamily()).isEqualTo("Helvetica");
        assertThat(runs.get(1).getText(0)).isEqualTo("run()");
        assertThat(runs.get(1).getFontFamily()).isEqualTo("Courier");
    }

    @Test
    void aTableCellKeepsPerRunStylingAndLosesNoText() throws Exception {
        byte[] docx = export(flow -> flow.addRow(row -> row
                .addParagraph(paragraph -> paragraph
                        .textStyle(BASE)
                        .inlineText("plain ")
                        .inlineText("bold", BOLD))));

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(document.getTables()).hasSize(1);
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);
            List<XWPFRun> runs = cell.getParagraphs().get(0).getRuns();

            assertThat(runs).hasSize(2);
            assertThat(runs.get(0).isBold()).isFalse();
            assertThat(runs.get(1).isBold()).isTrue();
            // The cell used to be written from the concatenated text in one style.
            // Splitting it into runs must not change what the cell says.
            assertThat(cell.getText()).isEqualTo("plain bold");
        }
    }

    private static DocumentTextStyle decorated(DocumentTextDecoration decoration) {
        return DocumentTextStyle.builder().size(11).decoration(decoration).build();
    }

    private static List<XWPFRun> paragraphRuns(Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> spec)
            throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(export(spec)))) {
            List<XWPFParagraph> paragraphs = document.getParagraphs().stream()
                    .filter(paragraph -> !paragraph.getRuns().isEmpty())
                    .toList();
            assertThat(paragraphs).hasSize(1);
            return paragraphs.get(0).getRuns();
        }
    }

    private static byte[] export(Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> spec)
            throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {
            com.demcha.compose.document.dsl.PageFlowBuilder flow = session.dsl().pageFlow().name("Flow");
            spec.accept(flow);
            flow.build();
            return session.export(new DocxSemanticBackend());
        }
    }
}
