package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A paragraph's lines are set in a measure as much wider or narrower than the page's as Word
 * sets its text.
 *
 * <p>Word states a type size in half points. {@code EngineeringResume}'s 7.8pt profile, set at
 * 8pt, took a line more than on the page, and its 6.9pt skills broke a word onto a line of its
 * own: each column stood 8 to 9pt low under them.</p>
 */
class DocxWordSizeMeasureTest {

    /** The body's width on a 400pt page with 20pt margins. */
    private static final double ROOM = 360;

    @Test
    void aSizeWordSetsLargerWidensTheMeasureByAsMuch() throws Exception {
        assertThat(rightIndent(paragraph(7.8, TextAlign.LEFT)))
                .as("the measure 8/7.8 of the page's")
                .isEqualTo(-Math.round(ROOM * (8 / 7.8 - 1) * 20));
    }

    @Test
    void aSizeWordSetsSmallerNarrowsIt() throws Exception {
        assertThat(rightIndent(paragraph(9.2, TextAlign.LEFT)))
                .isEqualTo(Math.round(ROOM * (1 - 9 / 9.2) * 20));
    }

    @Test
    void aSizeWordStatesAsItIsLeavesTheMeasureAlone() throws Exception {
        assertThat(rightIndent(paragraph(9.5, TextAlign.LEFT))).isZero();
    }

    @Test
    void aCentredLineIsLeftWhereThePageSetsIt() throws Exception {
        assertThat(rightIndent(paragraph(7.8, TextAlign.CENTER))).isZero();
    }

    @Test
    void runsOfTwoSizesShareTheMeasureByTheirLetters() throws Exception {
        // Ten letters at 7.8pt, set at 8, and ten at 9pt, set as they are.
        try (XWPFDocument document = DocxExports.withLayout(400, 400, 20, page -> page
                .addParagraph(p -> p.rich(rich -> rich
                        .size("abcdefghij", 7.8)
                        .size("klmnopqrst", 9))))) {
            double share = (10 * 8.0 + 10 * 9.0) / (10 * 7.8 + 10 * 9.0);

            assertThat(rightIndent(text(document, "abcdefghij")))
                    .isEqualTo(-Math.round(ROOM * (share - 1) * 20));
        }
    }

    @Test
    void aRunOfItsOwnSizeIsWrittenAtTheSizeTheMeasureIsTakenFor() throws Exception {
        // The body sets Normal at 12pt; the small print states its own size on its run.
        try (XWPFDocument document = DocxExports.withLayout(400, 400, 20, page -> page
                .addParagraph(p -> p.text("A body paragraph long enough to set the document's own size.")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(12)))
                .addParagraph(p -> p.text("Platform engineer with ten years of document pipelines.")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(7.8))))) {
            XWPFParagraph small = text(document, "Platform engineer");

            assertThat(DocxTwips.of(small.getRuns().get(0).getCTR().getRPr().getSzArray(0).getVal()))
                    .as("8pt, in half points").isEqualTo(16L);
            assertThat(rightIndent(small)).isEqualTo(-Math.round(ROOM * (8 / 7.8 - 1) * 20));
        }
    }

    @Test
    void aParagraphInACellIsGivenItsShareOfTheCellsMeasure() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 400, 20, page -> page
                .addRow("Page", row -> row
                        .columns(DocumentRowColumn.fixed(200), DocumentRowColumn.weight(1))
                        .addParagraph(p -> p.text("Platform engineer with ten years of document pipelines.")
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(7.8)))
                        .addParagraph("Main")))) {
            XWPFParagraph cell = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().stream()
                    .filter(p -> p.getText().startsWith("Platform engineer")).findFirst().orElseThrow();

            assertThat(-rightIndent(cell)).as("wider, by its share of a column no wider than 200pt")
                    .isPositive()
                    .isLessThanOrEqualTo(Math.round(200 * (8 / 7.8 - 1) * 20));
        }
    }

    private static long rightIndent(XWPFParagraph paragraph) {
        CTInd indent = paragraph.getCTP().getPPr() == null ? null : paragraph.getCTP().getPPr().getInd();
        return indent == null || !indent.isSetRight() ? 0 : DocxTwips.of(indent.getRight());
    }

    private static XWPFParagraph paragraph(double size, TextAlign align) throws Exception {
        XWPFDocument document = DocxExports.withLayout(400, 400, 20, page -> page
                .addParagraph(p -> p.text("Platform engineer with ten years of document pipelines.")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(size))
                        .align(align)));
        return text(document, "Platform engineer");
    }

    private static XWPFParagraph text(XWPFDocument document, String start) {
        return document.getParagraphs().stream()
                .filter(p -> p.getText().startsWith(start))
                .findFirst().orElseThrow();
    }
}
