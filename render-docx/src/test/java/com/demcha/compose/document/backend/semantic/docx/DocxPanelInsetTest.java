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
    void aPaddedSectionInsideAPanelKeepsThePanelsEdgeAtThePanelsEdge() throws Exception {
        // The inner section indents its text further; the panel's bar and band still belong
        // at the panel's edge, so the border is spaced by the whole distance, not by the
        // panel's own padding.
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 30, page -> page
                .addSection(s -> s.fillColor(SURFACE).padding(DocumentInsets.of(14)).accentLeft(ACCENT, 3)
                        .addSection(inner -> inner.padding(DocumentInsets.of(10))
                                .addParagraph(p -> p.text("Nested")))))) {
            CTPPr properties = paragraph(document, "Nested").getCTP().getPPr();

            assertThat(DocxTwips.of(properties.getInd().getLeft())).isEqualTo(24 * 20L);
            assertThat(DocxTwips.of(properties.getPBdr().getLeft().getSpace())).isEqualTo(24L);
            assertThat(DocxTwips.of(properties.getPBdr().getRight().getSpace())).isEqualTo(24L);
        }
    }

    @Test
    void aRowAndATableInAPaddedSectionStartWhereItsTextStarts() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 30, page -> page
                .addSection(s -> s.padding(DocumentInsets.of(14))
                        .addRow(r -> r.addParagraph(p -> p.text("Engineer")).addParagraph(p -> p.text("2019")))
                        .addParagraph(p -> p.text("Between"))
                        .addTable(t -> t.autoColumns(2).row("Item", "Amount"))))) {
            var row = document.getTables().get(0);
            var table = document.getTables().get(1);

            // The editor measures w:tblInd to the first cell's text, so the first cell's own
            // margin is added and the table's edge lands at the section's text.
            assertThat(DocxTwips.of(row.getCTTbl().getTblPr().getTblInd().getW()))
                    .isEqualTo(14 * 20L + firstCellMarginTwips(row));
            assertThat(DocxTwips.of(table.getCTTbl().getTblPr().getTblInd().getW()))
                    .isEqualTo(14 * 20L + firstCellMarginTwips(table));
            assertThat(firstCellMarginTwips(table)).as("the engine's 4pt cell padding").isEqualTo(80L);
        }
    }

    @Test
    void aTableOutsideAnyContainerIsNotIndented() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 30, page -> page
                .addTable(t -> t.autoColumns(1).row("Plain")))) {
            var properties = document.getTables().get(0).getCTTbl().getTblPr();

            assertThat(properties.isSetTblInd()).isFalse();
        }
    }

    @Test
    void aWideImageInAPaddedSectionFitsBetweenItsInsets() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 30, page -> page
                .addSection(s -> s.padding(DocumentInsets.of(14))
                        .addImage(i -> i.source(com.demcha.compose.document.image.DocumentImageData.fromBytes(png(800, 100)))
                                .width(800).height(100))))) {
            var picture = document.getParagraphs().stream()
                    .flatMap(p -> p.getRuns().stream())
                    .flatMap(r -> r.getEmbeddedPictures().stream())
                    .findFirst()
                    .orElseThrow();
            double widthPoints = picture.getWidth();

            assertThat(widthPoints)
                    .as("the page's 340pt less 14pt either side, not the page's full width")
                    .isLessThanOrEqualTo(312.0 + 0.5);
        }
    }

    @Test
    void aNestedListItemsBarStaysAtThePanelsEdge() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 30, page -> page
                .addSection(s -> s.fillColor(SURFACE).padding(DocumentInsets.of(14)).accentLeft(ACCENT, 3)
                        .addList(l -> l.bullet().addItem("Parent", child -> child.bullet().items("Child")))))) {
            CTPPr child = paragraph(document, "Child").getCTP().getPPr();

            assertThat(DocxTwips.of(child.getInd().getLeft())).isEqualTo(14 * 20L + 180L + 120L);
            assertThat(DocxTwips.of(child.getPBdr().getLeft().getSpace()))
                    .as("the panel's inset plus one nesting step, where the child's marker starts")
                    .isEqualTo(14L + 6L);
        }
    }

    private static long firstCellMarginTwips(org.apache.poi.xwpf.usermodel.XWPFTable table) {
        var cell = table.getRow(0).getCell(0).getCTTc().getTcPr();
        return DocxTwips.of(cell.getTcMar().getLeft().getW());
    }

    private static byte[] png(int width, int height) {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(width, height,
                    java.awt.image.BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
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
