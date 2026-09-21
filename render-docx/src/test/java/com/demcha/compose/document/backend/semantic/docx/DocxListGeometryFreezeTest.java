package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Freezes the geometry of the semantic DOCX export of a list.
 *
 * <p>The DOCX list path shares no geometry code with the PDF one — it walks the authored
 * node tree directly and never touches the layout engine — so its contract has to be
 * pinned separately.</p>
 *
 * <p>What is pinned changed when a list became a list Word owns. The nesting indent is no
 * longer padding characters in the run text but a level in the list definition, so the
 * hazard this test used to name — the two backends indenting with different codepoints
 * that look alike — is gone: the DOCX side writes no indent characters at all. In its
 * place are two indent constants that Word reads and nobody measures, worth pinning for
 * the same reason the codepoints were.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxListGeometryFreezeTest {

    /** The marker column the export writes, in twips. A convention, not a measurement. */
    private static final int HANGING_TWIPS = 180;

    /** Added per nesting level, approximating the two spaces the old text path used. */
    private static final int NESTING_STEP_TWIPS = 120;

    private static final Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> OUTLINE =
            flow -> flow.addList(list -> list
                    .name("Outline")
                    .addItem("alpha", l1 -> l1
                            .addItem("beta", l2 -> l2
                                    .addItem("gamma"))));

    @Test
    void nestingIsAListLevelWithAPinnedIndentAndNoPaddingCharacters() throws Exception {
        // No indent characters of any kind reach the text — neither the ASCII spaces the
        // DOCX path used to write nor the non-breaking ones the PDF flatten path uses.
        List<String> texts = exportTexts(OUTLINE);
        assertThat(texts).contains("alpha", "beta", "gamma");
        assertThat(texts.stream().anyMatch(DocxListGeometryFreezeTest::startsWithPadding))
                .as("nesting is a list level, so nothing pads the run text")
                .isFalse();

        try (XWPFDocument document = export(OUTLINE)) {
            var levels = document.getNumbering()
                    .getAbstractNum(BigInteger.ZERO).getAbstractNum().getLvlList();
            assertThat(levels).hasSize(3);
            for (int depth = 0; depth < levels.size(); depth++) {
                var indent = levels.get(depth).getPPr().getInd();
                assertThat(twips(indent.getHanging()))
                        .as("the marker column at depth %d", depth)
                        .isEqualTo(HANGING_TWIPS);
                assertThat(twips(indent.getLeft()))
                        .as("the content origin at depth %d", depth)
                        .isEqualTo(HANGING_TWIPS + NESTING_STEP_TWIPS * depth);
            }
        }
    }

    @Test
    void theMarkerIsWordNumberingAndNotRunText() throws Exception {
        try (XWPFDocument document = export(flow -> flow
                .addList(list -> list.name("Flat").bullet().items("Java", "SQL")))) {

            List<XWPFParagraph> paragraphs = document.getParagraphs().stream()
                    .filter(p -> !p.getText().isBlank())
                    .toList();
            assertThat(paragraphs).hasSize(2);
            // The marker belongs to the list definition, so the text is the item alone.
            assertThat(paragraphs.get(0).getText()).isEqualTo("Java");
            assertThat(paragraphs.get(1).getText()).isEqualTo("SQL");

            for (XWPFParagraph paragraph : paragraphs) {
                CTPPr properties = paragraph.getCTP().getPPr();
                assertThat(properties).isNotNull();
                assertThat(properties.isSetNumPr())
                        .as("w:numPr — these are list items, not paragraphs that look like some")
                        .isTrue();
                assertThat(properties.isSetInd())
                        .as("the indent is the level's, not repeated on every paragraph")
                        .isFalse();
            }
            assertThat(document.getNumbering()).as("numbering.xml is written").isNotNull();
        }
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
        assertThat(items.get(0)).startsWith("Long item text");
        assertThat(items.get(0)).doesNotContain("        ");
    }

    // ------------------------------------------------------------------

    /**
     * Reads a twip measure back as a number.
     *
     * <p>{@code ST_SignedTwipsMeasure} is an xmlbeans union, so the accessor is typed
     * {@code Object} and hands back whichever member matched.</p>
     */
    private static int twips(Object measure) {
        return Integer.parseInt(String.valueOf(measure));
    }

    /** True when a line begins with an ASCII space or carries a non-breaking one. */
    private static boolean startsWithPadding(String text) {
        return text.startsWith(" ") || text.indexOf(' ') >= 0;
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
