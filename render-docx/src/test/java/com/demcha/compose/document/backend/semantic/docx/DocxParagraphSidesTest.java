package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A paragraph's own sides hold its text in across, and a word the page sets past its box
 * stands out of it in Word too.
 *
 * <p>{@code SerifHeadline}'s summary stops at the column divider through its right margin;
 * written without it, it ran the page's width in Word, a line short, and everything under it
 * stood that line high. Its contact column sets "linkedin.com/in/alexmorgan" whole, 1.2pt past
 * the column's edge; Word broke the word between two letters and the column took a line more.</p>
 */
class DocxParagraphSidesTest {

    private static final String SUMMARY = "Software Engineer with years of experience designing and building "
            + "scalable web applications and backend services.";

    @Test
    void aBodyParagraphIsHeldInByItsOwnMargin() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text(SUMMARY).margin(new DocumentInsets(0, 120, 0, 10))))) {
            CTInd indent = indent(document.getParagraphs().get(0));

            assertThat(DocxTwips.of(indent.getRight())).isEqualTo(120 * 20L);
            assertThat(DocxTwips.of(indent.getLeft())).isEqualTo(10 * 20L);
        }
    }

    @Test
    void inACellACoupleOfPointsOfEachSideStayTheEditors() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow(row -> row
                        .addParagraph(p -> p.text("Left").margin(new DocumentInsets(0, 40, 0, 1.5)))
                        .addParagraph("Right")))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);
            CTInd indent = indent(cell.getParagraphs().get(0));

            assertThat(DocxTwips.of(indent.getRight())).as("40pt less the 2pt the editor keeps").isEqualTo(38 * 20L);
            assertThat(indent.isSetLeft() && DocxTwips.of(indent.getLeft()) > 0)
                    .as("a side under 2pt is all the editor's").isFalse();
        }
    }

    @Test
    void aWordWiderThanItsColumnStandsOutOfItInWord() throws Exception {
        // The page gives the word a box reaching past the column — here a negative right
        // margin — and sets it whole there; Word, given only the column, broke it.
        String link = "linkedin.com/in/alexmorgan";
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow(row -> row
                        .weights(4, 1)
                        .addParagraph("Name")
                        .addParagraph(p -> p.text(link).margin(new DocumentInsets(0, -60, 0, 0)))))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(1);
            XWPFParagraph contact = cell.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().equals(link)).findFirst().orElseThrow();
            CTInd indent = indent(contact);

            assertThat(indent != null && indent.isSetRight()).as("a right indent gives it room").isTrue();
            assertThat(DocxTwips.of(indent.getRight())).as("reaching past the column's edge").isNegative();
        }
    }

    @Test
    void wordsThatWrapKeepTheirColumn() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow(row -> row
                        .weights(3, 1)
                        .addParagraph("Name")
                        .addParagraph(SUMMARY)))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(1);
            CTInd indent = indent(cell.getParagraphs().get(0));

            assertThat(indent == null || !indent.isSetRight() || DocxTwips.of(indent.getRight()) >= 0)
                    .as("a line of several words breaks where it fits").isTrue();
        }
    }

    @Test
    void aLongWordAmongOtherWordsGivesTheParagraphNoMoreRoom() throws Exception {
        // The indent is the whole paragraph's: given to the word's line, it would let the
        // lines of several words take more of them than the page does.
        String text = "My profile page linkedin.com/in/alexmorgan";
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow(row -> row
                        .weights(4, 1)
                        .addParagraph("Name")
                        .addParagraph(p -> p.text(text).margin(new DocumentInsets(0, -60, 0, 0)))))) {
            CTInd indent = indent(document.getTables().get(0).getRow(0).getCell(1).getParagraphs().get(0));

            assertThat(indent == null || !indent.isSetRight() || DocxTwips.of(indent.getRight()) >= 0).isTrue();
        }
    }

    @Test
    void aCentredWordKeepsItsColumn() throws Exception {
        String link = "linkedin.com/in/alexmorgan";
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow(row -> row
                        .weights(4, 1)
                        .addParagraph("Name")
                        .addParagraph(p -> p.text(link).align(TextAlign.CENTER)
                                .margin(new DocumentInsets(0, -60, 0, 0)))))) {
            CTInd indent = indent(document.getTables().get(0).getRow(0).getCell(1).getParagraphs().get(0));

            assertThat(indent == null || !indent.isSetRight() || DocxTwips.of(indent.getRight()) >= 0)
                    .as("a right indent would move the centred line off the page's centre").isTrue();
        }
    }

    private static CTInd indent(XWPFParagraph paragraph) {
        var properties = paragraph.getCTP().getPPr();
        return properties == null || !properties.isSetInd() ? null : properties.getInd();
    }
}
