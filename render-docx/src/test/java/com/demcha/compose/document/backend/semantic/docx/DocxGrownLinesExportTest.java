package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar;
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

    @Test
    void aTableTextCellAsTallAsItsFacesOwnLineIsWrittenAtALeastHeightAndItsRowNotMoved() throws Exception {
        // Lato's own line is the page's, its descent the page's: a least line stands the text
        // where the page does, whatever the cell's anchor, and the row's margins, which seat an
        // exact line, keep the padding.
        java.util.function.Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content = page -> page
                .addTable(t -> t.columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100),
                                DocumentTableColumn.fixed(100))
                        .rowCells(textCell("Middle", DocumentTableTextAnchor.CENTER),
                                textCell("Top", DocumentTableTextAnchor.TOP_LEFT),
                                textCell("Bottom", DocumentTableTextAnchor.BOTTOM_LEFT)));
        DocxExportReport report = DocxExports.report(400, 600, 20, content);
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, content)) {
            for (XWPFTableCell cell : document.getTables().get(0).getRow(0).getTableCells()) {
                XWPFParagraph text = cell.getParagraphs().get(0);

                assertThat(spacing(text).getLineRule()).as(text.getText()).isEqualTo(STLineSpacingRule.AT_LEAST);
                assertThat(DocxTwips.of(spacing(text).getLine())).as("Lato's own line at 8pt").isEqualTo(192);
                assertThat(margins(cell)).as("the padding, 4pt above and below").containsExactly(80L, 80L);
                assertThat(positions(text)).containsOnly(0L);
            }
        }
        assertThat(report.bySubject()).as("no line of text left exact").doesNotContainKey(DocxExports.EXACT_LINES);
    }

    @Test
    void aTableTextCellOfSeveralLinesGrowsEachOfThem() throws Exception {
        // A paragraph a line where the cell's lines stand apart, the space between them after
        // each; one paragraph, its lines broken, where they do not.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.lines("Apart", "Below").withStyle(cellStyle(face(FontName.LATO, 8),
                                DocumentTableTextAnchor.BOTTOM_LEFT, 2)),
                        DocumentTableCell.lines("Broken", "Under").withStyle(cellStyle(face(FontName.LATO, 8),
                                DocumentTableTextAnchor.CENTER, 0)))))) {
            XWPFTableCell apart = document.getTables().get(0).getRow(0).getCell(0);
            XWPFTableCell broken = document.getTables().get(0).getRow(0).getCell(1);

            assertThat(apart.getParagraphs()).as("the premise: a paragraph a line").hasSize(2)
                    .allSatisfy(line -> assertThat(spacing(line).getLineRule()).isEqualTo(STLineSpacingRule.AT_LEAST));
            assertThat(DocxTwips.of(spacing(apart.getParagraphs().get(0)).getAfter())).as("the space between them")
                    .isEqualTo(40);
            assertThat(broken.getParagraphs()).as("the premise: one paragraph, its lines broken").hasSize(1);
            assertThat(spacing(broken.getParagraphs().get(0)).getLineRule()).isEqualTo(STLineSpacingRule.AT_LEAST);
        }
    }

    @Test
    void aTableTextCellTighterThanItsFacesOwnLineStaysExact() throws Exception {
        // Poppins' own line holds a line gap the page's does not: a least line would grow to it,
        // and the row with it. Exact, the row's margins seat it.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.text("Poppins").withStyle(cellStyle(face(FontName.POPPINS, 8),
                        DocumentTableTextAnchor.CENTER, 0)))))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);

            assertThat(spacing(cell.getParagraphs().get(0)).getLineRule()).isEqualTo(STLineSpacingRule.EXACT);
            assertThat(margins(cell)[0]).as("its room moved up").isLessThan(80L);
        }
    }

    @Test
    void anExactCellBesideALeastOneIsLeftItsCallByTheRowsMargins() throws Exception {
        // The least cell calls for no move, so the row's margins, which move every cell's text at
        // once, stay its padding; the exact Ubuntu cell beside it, which Word sets a quarter point
        // high, is left that call, under the half point its position moves and with no space after
        // its line to trade.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100))
                .rowCells(textCell("Lato", DocumentTableTextAnchor.CENTER),
                        DocumentTableCell.text("Ubuntu").withStyle(cellStyle(face(FontName.UBUNTU, 8),
                                DocumentTableTextAnchor.CENTER, 0)))))) {
            var row = document.getTables().get(0).getRow(0);
            XWPFParagraph ubuntu = row.getCell(1).getParagraphs().get(0);

            assertThat(spacing(row.getCell(0).getParagraphs().get(0)).getLineRule()).isEqualTo(STLineSpacingRule.AT_LEAST);
            assertThat(spacing(ubuntu).getLineRule()).isEqualTo(STLineSpacingRule.EXACT);
            for (XWPFTableCell cell : row.getTableCells()) {
                assertThat(margins(cell)).containsExactly(80L, 80L);
            }
            assertThat(positions(ubuntu)).containsOnly(0L);
            assertThat(spacing(ubuntu).isSetAfter() ? DocxTwips.of(spacing(ubuntu).getAfter()) : 0L).isZero();
        }
    }

    @Test
    void aRightToLeftTextCellStaysExact() throws Exception {
        // Word sets a right-to-left run's line by its own measure, which a least line is not read by.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.text("Lato").withStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.of(4)).stroke(DocumentStroke.of(DocumentColor.BLACK, 0))
                        .textStyle(face(FontName.LATO, 8)).textAnchor(DocumentTableTextAnchor.CENTER)
                        .direction(TextDirection.RTL).build()))))) {
            XWPFParagraph text = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0);

            assertThat(text.getCTP().getRArray(0).getRPr().sizeOfRtlArray()).as("the premise: a right-to-left run")
                    .isPositive();
            assertThat(spacing(text).getLineRule()).isEqualTo(STLineSpacingRule.EXACT);
        }
    }

    @Test
    void aLineHoldingAPictureWithinItsTextIsWrittenAtALeastHeight() throws Exception {
        // A 6pt picture on the baseline of 10pt Lato stays under the text's top in both editors,
        // LibreOffice's tenth of a point above the baseline included: the line is the text's.
        byte[] png = png();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.inlineImage(com.demcha.compose.document.image.DocumentImageData.fromBytes(png),
                                6, 6, com.demcha.compose.document.node.InlineImageAlignment.BASELINE, 0, null)
                        .inlineText(" Small", face(FontName.LATO, 10))))) {
            XWPFParagraph small = paragraph(document, " Small");

            assertThat(small.getCTP().getRArray(0).sizeOfDrawingArray()).as("the premise: its picture first").isOne();
            assertThat(spacing(small).getLineRule()).isEqualTo(STLineSpacingRule.AT_LEAST);
            assertThat(DocxTwips.of(spacing(small).getLine())).as("Lato's own line").isEqualTo(240);
            assertThat(positions(small)).containsOnly(0L);
        }
    }

    @Test
    void aLineGrownToAPictureRisingPastItsTextIsGivenNoPosition() throws Exception {
        // An 11pt icon set at the middle of 10pt Poppins rises past the text the page measures, and
        // the line is written at least the icon's 14pt reach, shorter than Poppins' own 15pt line:
        // grown to its picture, it is not read as a line grown with its text. A least line's text
        // is not moved by a position in Word, and LibreOffice moves it and grows the line: none is
        // written.
        byte[] png = png();
        try (XWPFDocument document = DocxExports.withLayout(200, 600, 20, page -> page
                .addParagraph(p -> p.inlineImage(com.demcha.compose.document.image.DocumentImageData.fromBytes(png),
                                11, 11, com.demcha.compose.document.node.InlineImageAlignment.CENTER)
                        .inlineText(" Icon beside text long enough to wrap onto a second line", face(FontName.POPPINS, 10))))) {
            XWPFParagraph icon = paragraph(document, " Icon");

            var runs = icon.getCTP().getRList();
            assertThat(spacing(icon).getLineRule()).as("the premise: grown to its icon").isEqualTo(STLineSpacingRule.AT_LEAST);
            assertThat(DocxTwips.of(spacing(icon).getLine())).as("the premise: shorter than Poppins' own 15pt")
                    .isLessThan(300);
            assertThat(runs.get(0).sizeOfDrawingArray()).as("the premise: the icon first").isOne();
            assertThat(positions(icon).get(0)).as("the premise: lowered to the middle").isNegative();
            assertThat(positions(icon).subList(1, runs.size())).as("its text").containsOnly(0L);
        }
    }

    @Test
    void aLeastLineGrownToAPictureWithinItsFacesOwnLineIsGivenNoPosition() throws Exception {
        // An 11pt picture on the baseline of 10pt Poppins rises past the ascent the page measures
        // and stays under Poppins' own, its line gap above its letters; the 2pt between the lines
        // make the line taller than that own line. Seated, a least line is still given no position.
        byte[] png = png();
        try (XWPFDocument document = DocxExports.withLayout(200, 600, 20, page -> page
                .addParagraph(p -> p.inlineImage(com.demcha.compose.document.image.DocumentImageData.fromBytes(png),
                                11, 11, com.demcha.compose.document.node.InlineImageAlignment.BASELINE, 0, null)
                        .inlineText(" Gapped text long enough to wrap onto a second line", face(FontName.POPPINS, 10))
                        .lineSpacing(2)))) {
            XWPFParagraph gapped = paragraph(document, " Gapped");

            assertThat(spacing(gapped).getLineRule()).as("the premise: grown to its picture")
                    .isEqualTo(STLineSpacingRule.AT_LEAST);
            assertThat(DocxTwips.of(spacing(gapped).getLine())).as("the premise: as tall as Poppins' own line")
                    .isGreaterThanOrEqualTo(300);
            assertThat(positions(gapped).subList(1, gapped.getCTP().getRList().size())).as("its text")
                    .containsOnly(0L);
        }
    }

    @Test
    void aLineGrownToARaisedPictureIsGivenNoPosition() throws Exception {
        // A 9pt picture raised 2pt off the baseline of 10pt Lato reaches 11pt up in Word, past the
        // text's own line, and opening the page it has no space above to be held in: the line is
        // written at least that reach, taller than Lato's own 12pt line. Its text is given no
        // position, which Word would not move it by, and its picture keeps its own raise.
        byte[] png = png();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.inlineImage(com.demcha.compose.document.image.DocumentImageData.fromBytes(png),
                                9, 9, com.demcha.compose.document.node.InlineImageAlignment.BASELINE, 2, null)
                        .inlineText(" Raised", face(FontName.LATO, 10))))) {
            XWPFParagraph raised = paragraph(document, " Raised");
            List<Long> positions = positions(raised);

            assertThat(spacing(raised).getLineRule()).as("the premise: grown to its picture")
                    .isEqualTo(STLineSpacingRule.AT_LEAST);
            assertThat(DocxTwips.of(spacing(raised).getLine())).as("the premise: taller than Lato's own line")
                    .isGreaterThan(240);
            assertThat(positions.get(0)).as("the picture's own raise").isEqualTo(4L);
            assertThat(positions.subList(1, positions.size())).as("its text").containsOnly(0L);
        }
    }

    private static byte[] png() {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(8, 8,
                    java.awt.image.BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static DocumentTableCell textCell(String text, DocumentTableTextAnchor anchor) {
        return DocumentTableCell.text(text).withStyle(cellStyle(face(FontName.LATO, 8), anchor, 0));
    }

    private static DocumentTableStyle cellStyle(DocumentTextStyle face, DocumentTableTextAnchor anchor,
                                                double lineSpacing) {
        return DocumentTableStyle.builder().padding(DocumentInsets.of(4))
                .stroke(DocumentStroke.of(DocumentColor.BLACK, 0)).textStyle(face).textAnchor(anchor)
                .lineSpacing(lineSpacing).build();
    }

    /** A cell's top and bottom margins, in twips. */
    private static long[] margins(XWPFTableCell cell) {
        CTTcMar mar = cell.getCTTc().getTcPr().getTcMar();
        return new long[]{DocxTwips.of(mar.getTop().getW()), DocxTwips.of(mar.getBottom().getW())};
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
