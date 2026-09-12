package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Freezes what the semantic DOCX export of a list is today, before any
 * hanging-indent or marker-gap work exists.
 *
 * <p>The DOCX list path shares no geometry code with the PDF one — it walks the
 * authored node tree directly and never touches the layout engine — so its
 * contract has to be pinned separately, and pinned as codepoints. In particular
 * the two backends indent nesting with <em>different characters</em> that happen
 * to look the same, which is the kind of agreement that breaks silently the
 * first time someone "unifies" them.</p>
 */
class DocxListLegacyGeometryFreezeTest {

    /** What the DOCX writer uses per nesting level: two ASCII spaces. */
    private static final String DOCX_INDENT_UNIT = "  ";

    /** What the PDF flatten path uses per nesting level: two non-breaking spaces. */
    private static final String PDF_INDENT_UNIT = "  ";

    @Test
    void nestingIndentsWithTwoAsciiSpacesPerLevelAndNotWithTheNonBreakingSpacesThePdfPathUses() throws Exception {
        List<String> texts = exportTexts(flow -> flow
                .addList(list -> list
                        .name("Outline")
                        .addItem("alpha", l1 -> l1
                                .addItem("beta", l2 -> l2
                                        .addItem("gamma")))));

        assertThat(texts).contains(
                "• alpha",
                DOCX_INDENT_UNIT + "◦ beta",
                DOCX_INDENT_UNIT.repeat(2) + "▪ gamma");

        // Stated as the divergence it is: same visual width, different codepoints,
        // held together by convention rather than by shared code.
        assertThat(DOCX_INDENT_UNIT).isNotEqualTo(PDF_INDENT_UNIT);
        assertThat(texts.stream().anyMatch(t -> t.contains(" ")))
                .as("no non-breaking space reaches the DOCX run text")
                .isFalse();
    }

    @Test
    void theMarkerIsRunTextWithATrailingSpaceAndNotWordNumbering() throws Exception {
        try (XWPFDocument document = export(flow -> flow
                .addList(list -> list.name("Flat").bullet().items("Java", "SQL")))) {

            List<XWPFParagraph> paragraphs = document.getParagraphs().stream()
                    .filter(p -> !p.getText().isBlank())
                    .toList();
            assertThat(paragraphs).hasSize(2);
            assertThat(paragraphs.get(0).getText()).isEqualTo("• Java");
            assertThat(paragraphs.get(1).getText()).isEqualTo("• SQL");

            for (XWPFParagraph paragraph : paragraphs) {
                CTPPr properties = paragraph.getCTP().getPPr();
                boolean numbered = properties != null && properties.isSetNumPr();
                boolean indented = properties != null && properties.isSetInd();
                assertThat(numbered).as("no w:numPr — these are plain paragraphs").isFalse();
                assertThat(indented).as("no w:ind — hanging indent has no DOCX representation today").isFalse();
            }
        }
        assertThat(hasNumberingPart()).as("no numbering.xml is written").isFalse();
    }

    @Test
    void oneParagraphPerItemRegardlessOfLengthBecauseDocxDoesNotWrap() throws Exception {
        // The DOCX path never measures or wraps: a long item is one w:p and Word
        // does its own line breaking, so there is no continuation line to indent
        // and continuationIndent() has no effect here.
        List<String> texts = exportTexts(flow -> flow
                .addList(list -> list
                        .name("Long")
                        .bullet()
                        .continuationIndent("        ")
                        .items("Long item text that would wrap across several visual lines in the PDF "
                               + "backend but stays a single Word paragraph here.")));

        List<String> items = texts.stream().filter(t -> !t.isBlank()).toList();
        assertThat(items).hasSize(1);
        assertThat(items.get(0)).startsWith("• Long item text");
        assertThat(items.get(0)).doesNotContain("        ");
    }

    // ------------------------------------------------------------------

    private static boolean hasNumberingPart() throws Exception {
        try (XWPFDocument document = export(flow -> flow
                .addList(list -> list.name("Flat").bullet().items("Java")))) {
            return document.getNumbering() != null;
        }
    }

    private static List<String> exportTexts(
            Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> author) throws Exception {
        try (XWPFDocument document = export(author)) {
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
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
