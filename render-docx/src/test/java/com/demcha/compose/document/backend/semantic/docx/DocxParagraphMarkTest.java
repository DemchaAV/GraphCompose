package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A paragraph whose line height the editor sets sizes its mark as its text: the mark is a
 * character on the last line and counts towards its height.
 *
 * @author Artem Demchyshyn
 */
class DocxParagraphMarkTest {

    @Test
    void aParagraphsMarkCarriesWhatItsTextEndsInSoEnterContinuesIt() throws Exception {
        // Word gives the paragraph Enter opens its mark's formatting. With the mark left at the
        // document's own, the new paragraph's letters came out another size: 9pt after 10.5pt
        // in NavySidebar, 14pt after 11pt in ClassicInvoice. Lines written exact are included:
        // the mark does not grow them, but it is what typing continues.
        DocumentColor accent = DocumentColor.rgb(30, 90, 160);
        try (XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Body text sets the document's size"))
                .addParagraph(p -> p
                        .inlineText("Plain, then ")
                        .inlineText("bold and coloured", DocumentTextStyle.builder().size(11)
                                .decoration(com.demcha.compose.document.style.DocumentTextDecoration.BOLD)
                                .color(accent).build())))) {
            XWPFParagraph paragraph = document.getParagraphs().stream()
                    .filter(p -> p.getText().startsWith("Plain")).findFirst().orElseThrow();
            CTPPr properties = paragraph.getCTP().getPPr();

            assertThat(properties.getSpacing().getLineRule()).as("an exact line").hasToString("exact");
            var mark = properties.getRPr();
            assertThat(mark.getSzArray(0).getVal()).as("its last run's size, half points").hasToString("22");
            assertThat(mark.sizeOfBArray()).as("bold, as its last run").isEqualTo(1);
            assertThat(mark.getColorArray(0).getVal()).as("in its colour")
                    .isEqualTo(paragraph.getRuns().get(paragraph.getRuns().size() - 1).getCTR().getRPr().getColorArray(0).getVal());
        }
    }

    @Test
    void aHairlineOfTextInACellIsNotAsTallAsBodyText() throws Exception {
        // A heading rule drawn as a filled cell holding half-point text: with the mark at the
        // document's size, the cell came out a line of body text tall.
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(120))
                .rowCells(DocumentTableCell.node(new ParagraphBuilder().name("Rule").text(" ").lineSpacing(0)
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(0.5)).build())
                        .withStyle(DocumentTableStyle.builder().padding(DocumentInsets.zero())
                                .fillColor(DocumentColor.rgb(194, 96, 72)).build()))))) {
            XWPFParagraph rule = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0);
            CTPPr properties = rule.getCTP().getPPr();

            assertThat(properties.getRPr().getSzList()).singleElement()
                    .satisfies(size -> assertThat(((Number) size.getVal()).intValue()).isEqualTo(1));
        }
    }

    @Test
    void aLineWrittenAtAnExactHeightLeavesItsMarkAlone() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Body"))
                .addParagraph(p -> p.text("Small print").textStyle(DocumentTextStyle.DEFAULT.withSize(6))))) {
            CTPPr small = document.getParagraphs().get(1).getCTP().getPPr();

            assertThat(small.getSpacing().getLineRule()).hasToString("exact");
            assertThat(small.isSetRPr()).isFalse();
        }
    }

    @Test
    void aMarkAlreadyInTheDocumentsSizeAndFaceSaysNothing() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Body text sets the document's size"))
                .addTable(t -> t.columns(DocumentTableColumn.fixed(120))
                        .rowCells(DocumentTableCell.node(new ParagraphBuilder().text("Plain").build()))))) {
            CTPPr properties = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0)
                    .getCTP().getPPr();

            assertThat(properties == null || !properties.isSetRPr()).isTrue();
        }
    }

    @Test
    void aParagraphComposedInACellIsSetAtTheLineThePageMeasured() throws Exception {
        // Its lines are laid out among its table's fragments, not at a path of its own; without
        // them Word set the line at its face's own height.
        DocumentTextStyle heading = DocumentTextStyle.builder()
                .fontName(com.demcha.compose.font.FontName.TIMES_ROMAN).size(20).build();
        try (XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Body text sets the document's size"))
                .addTable(t -> t.columns(DocumentTableColumn.fixed(200))
                        .rowCells(DocumentTableCell.node(new ParagraphBuilder().text("Heading")
                                .textStyle(heading).build()))))) {
            CTPPr properties = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0)
                    .getCTP().getPPr();

            assertThat(properties.getSpacing().getLineRule()).hasToString("exact");
            assertThat(((Number) properties.getSpacing().getLine()).intValue()).as("the face's ascent and descent, no leading").isBetween(16 * 20, 24 * 20);
            // An exact line does not grow for its mark, but Enter at its end continues from it.
            assertThat(properties.getRPr().getSzArray(0).getVal())
                    .as("the mark in the text's size, half points").hasToString("40");
            assertThat(properties.getRPr().getRFontsArray(0).getAscii())
                    .as("and in its face, as its run names it")
                    .isEqualTo(document.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0)
                            .getRuns().get(0).getFontFamily());
        }
    }

    @Test
    void aMarkLargerThanTheDocumentsTextIsSetInTheTextsFace() throws Exception {
        // In the document's face, a 20pt mark grows the line by that face's height rather than
        // the text's own. Blank text lays out nothing to measure the line by.
        DocumentTextStyle heading = DocumentTextStyle.builder()
                .fontName(com.demcha.compose.font.FontName.TIMES_ROMAN).size(20).build();
        try (XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Body text sets the document's size"))
                .addTable(t -> t.columns(DocumentTableColumn.fixed(200))
                        .rowCells(DocumentTableCell.node(new ParagraphBuilder().text(" ")
                                .textStyle(heading).build()))))) {
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTParaRPr mark = document.getTables().get(0)
                    .getRow(0).getCell(0).getParagraphs().get(0).getCTP().getPPr().getRPr();

            assertThat(((Number) mark.getSzArray(0).getVal()).intValue()).isEqualTo(40);
            assertThat(((Number) mark.getSzCsArray(0).getVal()).intValue()).isEqualTo(40);
            assertThat(mark.getRFontsArray(0).getAscii()).isNotBlank();
        }
    }

    @Test
    void aLineGrownToAPictureSizesItsMarkToo() throws Exception {
        // Two lines of small print with an icon rising above them are written "at least" the
        // icon's height; a mark at the document's size would grow them further.
        try (XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Body text sets the document's size. ".repeat(12)))
                .addParagraph(p -> p.textStyle(DocumentTextStyle.DEFAULT.withSize(6))
                        .inlineText("Tiny print that runs on past the end of its line ".repeat(4))
                        .inlineImage(com.demcha.compose.document.image.DocumentImageData.fromBytes(png()), 14, 14)))) {
            CTPPr small = document.getParagraphs().get(1).getCTP().getPPr();

            assertThat(small.getSpacing().getLineRule()).hasToString("atLeast");
            assertThat(((Number) small.getRPr().getSzArray(0).getVal()).intValue()).isEqualTo(12);
        }
    }

    @Test
    void aMarkIsSizedAsTheTextThatEndsTheLineNotAsTheParagraphsUnusedStyle() throws Exception {
        // A contact block: an icon and runs styled on their own, the paragraph's style left at its
        // default, on two lines. A mark in that default grew every such line in both editors.
        try (XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Body text sets the document's size"))
                .addParagraph(p -> p
                        .inlineImage(com.demcha.compose.document.image.DocumentImageData.fromBytes(png()), 14, 14)
                        .inlineText(" billing@example.com".repeat(12), DocumentTextStyle.DEFAULT.withSize(6))))) {
            CTPPr line = document.getParagraphs().get(1).getCTP().getPPr();

            assertThat(line.getSpacing().getLineRule()).hasToString("atLeast");
            assertThat(((Number) line.getRPr().getSzArray(0).getVal()).intValue()).isEqualTo(12);
        }
    }

    @Test
    void aListItemInACellSizesItsMark() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Body text sets the document's size"))
                .addTable(t -> t.columns(DocumentTableColumn.fixed(200))
                        .rowCells(DocumentTableCell.node(new com.demcha.compose.document.dsl.ListBuilder()
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(7)).items("One", "Two").build()))))) {
            for (XWPFParagraph item : document.getTables().get(0).getRow(0).getCell(0).getParagraphs()) {
                if (item.getText().isBlank()) {
                    continue;
                }
                assertThat(((Number) item.getCTP().getPPr().getRPr().getSzArray(0).getVal()).intValue())
                        .as(item.getText()).isEqualTo(14);
            }
        }
    }

    private static byte[] png() {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(20, 20,
                    java.awt.image.BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static XWPFDocument export(java.util.function.Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content)
            throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400)
                .margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(content::accept);
            return new XWPFDocument(new ByteArrayInputStream(session.export(new DocxSemanticBackend())));
        }
    }
}
