package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A container's content sits inside its margin and padding, and its panel reaches its edges.
 *
 * <p>The sides of a container were never written: a card's text ran flush with the page
 * margin, touching the card's edge, and the accent bar sat against the text. The paragraphs
 * are now indented by every enclosing margin and padding, and a panel's side borders are
 * spaced by its padding — Word draws a side border that far outside the text and shades out to
 * it — so the bar and the band are at the card's edge and the text inside it. Measured in
 * LibreOffice against the page's own render.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxPanelInsetTest {

    private static final DocumentColor SURFACE = DocumentColor.rgb(230, 240, 255);
    private static final DocumentColor ACCENT = DocumentColor.rgb(26, 86, 148);

    @Test
    void aPaddedPanelHoldsItsTextInAndItsEdgesOut() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 30, page -> page
                .addSection(s -> s.fillColor(SURFACE).padding(DocumentInsets.of(14)).accentLeft(ACCENT, 3)
                        .addParagraph(p -> p.text("Card title"))))) {
            CTPPr properties = paragraph(document, "Card title").getCTP().getPPr();

            assertThat(DocxTwips.of(properties.getInd().getLeft())).isEqualTo(14 * 20L);
            assertThat(DocxTwips.of(properties.getInd().getRight())).isEqualTo(14 * 20L);
            assertThat(DocxTwips.of(properties.getPBdr().getLeft().getSpace()))
                    .as("the accent is the padding outside the text, at the card's edge")
                    .isEqualTo(14L);
            assertThat(DocxTwips.of(properties.getPBdr().getRight().getSpace()))
                    .as("a hairline in the fill's colour carries the band out to the right edge too")
                    .isEqualTo(14L);
            assertThat(DocxTwips.of(properties.getPBdr().getRight().getSz())).isEqualTo(2L);
        }
    }

    @Test
    void anUnpaintedContainerStillHoldsItsTextIn() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 30, page -> page
                .addSection(s -> s.padding(DocumentInsets.of(10))
                        .addParagraph(p -> p.text("Inset"))))) {
            CTPPr properties = paragraph(document, "Inset").getCTP().getPPr();

            assertThat(DocxTwips.of(properties.getInd().getLeft())).isEqualTo(10 * 20L);
            assertThat(properties.isSetPBdr()).isFalse();
        }
    }

    @Test
    void nestedContainersAddUp() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 30, page -> page
                .addSection(outer -> outer.padding(DocumentInsets.of(10))
                        .addSection(inner -> inner.padding(DocumentInsets.of(6))
                                .addParagraph(p -> p.text("Deep")))))) {
            CTPPr properties = paragraph(document, "Deep").getCTP().getPPr();

            assertThat(DocxTwips.of(properties.getInd().getLeft())).isEqualTo(16 * 20L);
        }
    }

    @Test
    void aListInAPanelKeepsItsHangingIndentAndTheBarStaysAtTheEdge() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 30, page -> page
                .addSection(s -> s.fillColor(SURFACE).padding(DocumentInsets.of(14)).accentLeft(ACCENT, 3)
                        .addList(l -> l.bullet().items("Item"))))) {
            CTPPr properties = paragraph(document, "Item").getCTP().getPPr();

            assertThat(DocxTwips.of(properties.getInd().getLeft()))
                    .as("the panel's inset plus the level's own indent")
                    .isEqualTo(14 * 20L + 180L);
            assertThat(DocxTwips.of(properties.getInd().getHanging())).isEqualTo(180L);
            assertThat(DocxTwips.of(properties.getPBdr().getLeft().getSpace()))
                    .as("measured from where the marker starts, the first line, as for the text above")
                    .isEqualTo(14L);
        }
    }

    @Test
    void aParagraphOutsideAnyContainerIsWrittenAsBefore() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 30, page -> page
                .addParagraph(p -> p.text("Plain")))) {
            CTPPr properties = paragraph(document, "Plain").getCTP().getPPr();

            assertThat(properties == null || !properties.isSetInd()).isTrue();
        }
    }

    @Test
    void aCellInsideAPaddedPanelStartsFromItsOwnEdge() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 30, page -> page
                .addSection(s -> s.padding(DocumentInsets.of(12))
                        .addTable(t -> t.autoColumns(1).rowCells(
                                com.demcha.compose.document.table.DocumentTableCell.node(
                                        new com.demcha.compose.document.dsl.ParagraphBuilder().text("In a cell").build())))))) {
            XWPFParagraph inCell = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0);
            CTPPr properties = inCell.getCTP().getPPr();

            assertThat(properties == null || !properties.isSetInd())
                    .as("the cell's own margin keeps it clear; the panel around the table adds nothing")
                    .isTrue();
        }
    }

    @Test
    void aPaddingWiderThanWordAllowsIsHeldAtWordsLimit() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 30, page -> page
                .addSection(s -> s.fillColor(SURFACE).padding(DocumentInsets.of(40))
                        .addParagraph(p -> p.text("Wide"))))) {
            CTPPr properties = paragraph(document, "Wide").getCTP().getPPr();

            assertThat(DocxTwips.of(properties.getInd().getLeft())).isEqualTo(40 * 20L);
            assertThat(DocxTwips.of(properties.getPBdr().getLeft().getSpace()))
                    .as("w:space stops at 31pt")
                    .isEqualTo(31L);
        }
    }

    private static XWPFParagraph paragraph(XWPFDocument document, String text) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getText().equals(text))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no paragraph reads " + text));
    }
}
