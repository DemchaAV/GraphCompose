package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A paragraph's lines are written at a least height where they are at least as tall as their
 * faces' own line, so Word grows them when the text is made larger; and exact where they are
 * tighter, or where a least line would not stand the text where the page does.
 *
 * <p>The page's line is its face's ascent and descent; a face whose own line is no taller — Lato,
 * Spectral — is written at a least height, one whose own line holds a line gap the page's does
 * not — Poppins — or the editors' descent another depth — Alegreya Sans — exact (measured in Word
 * and in LibreOffice 24.2: a least line keeps the spare above its text and stands its baseline the
 * runs' descent above its foot).</p>
 *
 * @author Artem Demchyshyn
 */
class DocxGrownLinesExportTest {

    private static final String WRAPPED = "A paragraph long enough to wrap onto a second line and onto a third one "
                                          + "in a column a hundred and sixty points wide";

    @Test
    void linesWithAGapTakenFromTheSpaceAboveAreWrittenAtALeastHeightAndNotMoved() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(200, 600, 20, page -> page
                .addParagraph(p -> p.text("Lead").textStyle(face(FontName.GOTHIC_A1, 10)))
                .addParagraph(p -> p.text(WRAPPED).textStyle(face(FontName.LATO, 10)).lineSpacing(3)
                        .margin(DocumentInsets.top(10))))) {
            XWPFParagraph text = paragraph(document, WRAPPED);

            assertThat(spacing(text).getLineRule()).isEqualTo(STLineSpacingRule.AT_LEAST);
            assertThat(DocxTwips.of(spacing(text).getLine())).as("Lato's 12pt and the 3pt gap").isEqualTo(300);
            assertThat(DocxTwips.of(spacing(text).getBefore())).as("the gap off the 10pt above").isEqualTo(140);
            assertThat(positions(text)).as("the text stands on the line's foot where the page sets it")
                    .containsOnly(0L);
        }
    }

    @Test
    void aLineGrowsOnlyWhileWordWouldSetItUnderAQuarterPointOffThePagesBaseline() throws Exception {
        // Two lines right under the block above, which gives none of their gap: it is shared out
        // over both, and a least line's text, on its foot, stands a quarter of the gap low at the
        // middle of the two. A 0.8pt gap leaves it 0.2pt off and the lines grow; at 1.2pt it would
        // stand 0.3pt off, and they stay exact.
        for (double gap : new double[]{0.8, 1.2}) {
            try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                    .addParagraph(p -> p.text("Lead").textStyle(face(FontName.GOTHIC_A1, 10)))
                    .addParagraph(p -> p.text("First line\nSecond line").textStyle(face(FontName.LATO, 10))
                            .lineSpacing(gap)))) {
                XWPFParagraph text = paragraph(document, "First line");

                assertThat(DocxTwips.of(spacing(text).getLine())).as("the premise: Lato's 12pt and half the gap")
                        .isEqualTo(Math.round((12 + gap / 2) * 20));
                assertThat(spacing(text).getLineRule()).as("a %spt gap", gap)
                        .isEqualTo(gap < 1 ? STLineSpacingRule.AT_LEAST : STLineSpacingRule.EXACT);
            }
        }
    }

    @Test
    void aRowOfColumnsParagraphIsWrittenAtALeastHeightToo() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow(row -> row
                        .addParagraph(p -> p.text("Left").textStyle(face(FontName.LATO, 10)))
                        .addParagraph(p -> p.text("Right").textStyle(face(FontName.LATO, 10)))))) {
            assertThat(spacing(paragraph(document, "Left")).getLineRule()).isEqualTo(STLineSpacingRule.AT_LEAST);
            assertThat(spacing(paragraph(document, "Right")).getLineRule()).isEqualTo(STLineSpacingRule.AT_LEAST);
        }
    }

    @Test
    void linesTighterThanTheirFacesOwnAreWrittenExact() throws Exception {
        // Poppins' own line holds a line gap of a tenth of its size above its letters; the page's
        // line and half a point's gap are short of it, and a least line would grow to it.
        try (XWPFDocument document = DocxExports.withLayout(200, 600, 20, page -> page
                .addParagraph(p -> p.text(WRAPPED).textStyle(face(FontName.POPPINS, 10)).lineSpacing(0.5)))) {
            assertThat(spacing(paragraph(document, WRAPPED)).getLineRule()).isEqualTo(STLineSpacingRule.EXACT);
        }
    }

    @Test
    void runsOfDifferentDepthsAreWrittenExact() throws Exception {
        // Word stands each least line's text its own deepest run's descent above its foot: an 8pt
        // run's line and a 7.5pt one's would stand their text at two depths.
        DocumentTextStyle bold = DocumentTextStyle.builder().fontName(FontName.LATO).size(8)
                .decoration(DocumentTextDecoration.BOLD).build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.inlineText("LayoutLint ", bold).inlineText("flags fragile patterns",
                        face(FontName.LATO, 7.5)).lineSpacing(1)))) {
            assertThat(spacing(paragraph(document, "LayoutLint")).getLineRule()).isEqualTo(STLineSpacingRule.EXACT);
        }
    }

    @Test
    void aFaceTheEditorsSetOnAnotherDescentIsWrittenExact() throws Exception {
        // Alegreya Sans' typographic descent, which the editors set its line on, is two tenths of
        // its size, and its hhea descent, which the page measures, three: a least line would stand
        // its text 2pt low at 20pt, its position moving nothing, so the face has no line to grow to
        // and the line stays exact, where its position seats it.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Alegreya").textStyle(face(FontName.ALEGREYA_SANS, 20))))) {
            assertThat(spacing(paragraph(document, "Alegreya")).getLineRule()).isEqualTo(STLineSpacingRule.EXACT);
        }
    }

    @Test
    void aParagraphSeatedOffItsBaselineIsWrittenExact() throws Exception {
        // Its position seats it by its cap band, which a least line does not keep.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Seated").textStyle(face(FontName.LATO, 10))
                        .verticalAlign(TextVerticalAlign.TOP)))) {
            XWPFParagraph seated = paragraph(document, "Seated");

            assertThat(positions(seated)).as("the premise: seated by its position").doesNotContain(0L);
            assertThat(spacing(seated).getLineRule()).isEqualTo(STLineSpacingRule.EXACT);
        }
    }

    @Test
    void aListsItemGrowsWhereItsMarkerIsItsTextAndNotWhereWordDrawsIt() throws Exception {
        // An item of runs, under a hanging marker, has its marker written as its first run.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addList(list -> list.textStyle(face(FontName.LATO, 10)).marker("•").hangingIndent(true).markerGap(6)
                        .addItem(text -> text.style("Written", face(FontName.LATO, 10))))
                .addList(list -> list.textStyle(face(FontName.LATO, 10)).items("Drawn")))) {
            XWPFParagraph written = paragraph(document, "•");
            XWPFParagraph drawn = paragraph(document, "Drawn");

            assertThat(written.getText()).as("the premise: its marker its text").contains("Written");
            assertThat(spacing(written).getLineRule()).isEqualTo(STLineSpacingRule.AT_LEAST);
            assertThat(drawn.getCTP().getPPr().isSetNumPr()).as("the premise: a Word list's item").isTrue();
            assertThat(spacing(drawn).getLineRule()).as("its marker in the numbering's own face")
                    .isEqualTo(STLineSpacingRule.EXACT);
        }
    }

    private static DocumentTextStyle face(FontName name, double size) {
        return DocumentTextStyle.builder().fontName(name).size(size).build();
    }

    private static CTSpacing spacing(XWPFParagraph paragraph) {
        return paragraph.getCTP().getPPr().getSpacing();
    }

    /** The positions the paragraph's runs are raised by, in half points, 0 for none. */
    private static List<Long> positions(XWPFParagraph paragraph) {
        List<Long> raised = new ArrayList<>();
        for (var run : paragraph.getCTP().getRList()) {
            raised.add(run.isSetRPr() && run.getRPr().sizeOfPositionArray() > 0
                    ? ((Number) run.getRPr().getPositionArray(0).getVal()).longValue() : 0L);
        }
        return raised;
    }

    /** The paragraph, in the body or a table's cell, whose text starts so. */
    private static XWPFParagraph paragraph(XWPFDocument document, String start) {
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            if (paragraph.getText().startsWith(start)) {
                return paragraph;
            }
        }
        for (XWPFTable table : document.getTables()) {
            for (var row : table.getRows()) {
                for (var cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        if (paragraph.getText().startsWith(start)) {
                            return paragraph;
                        }
                    }
                }
            }
        }
        throw new AssertionError("no paragraph reads " + start);
    }
}
