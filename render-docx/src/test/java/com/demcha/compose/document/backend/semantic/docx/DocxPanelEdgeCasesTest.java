package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBrType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a card written as a one-cell table has to keep that the paragraphs it replaced kept
 * by being in the body: its blocks' anchors and keeps, a page break among them, the space
 * above it, and a size where nothing states one.
 *
 * @author Artem Demchyshyn
 */
class DocxPanelEdgeCasesTest {

    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final DocumentColor ACCENT = DocumentColor.rgb(26, 86, 148);

    @Test
    void anAnchorOnABlockInsideACardIsABookmarkInItsCell() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .addParagraph(p -> p.text("Intro"))
                .addSection("Results", results -> results.anchor("results")
                        .addParagraph(p -> p.text("Results"))
                        .addParagraph(p -> p.text("All green")))))) {
            XWPFTableCell cell = card(document);

            assertThat(paragraphIn(cell, "Results").getCTP().getBookmarkStartList())
                    .extracting(CTBookmark::getName)
                    .containsExactly("results");
            assertThat(paragraphIn(cell, "All green").getCTP().getBookmarkEndList()).hasSize(1);
        }
    }

    @Test
    void aBlockInsideACardThatTakesOverATablesCloserIsBookmarkedFromIt() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .addTable(t -> t.autoColumns(1).row("Above"))
                .addSection("Note", note -> note.anchor("note")
                        .addParagraph(p -> p.text("Note"))
                        .addParagraph(p -> p.text("Ends here")))))) {
            XWPFParagraph note = paragraphIn(card(document), "Note");

            assertThat(note.getCTP().getBookmarkStartList())
                    .extracting(CTBookmark::getName)
                    .containsExactly("note");
        }
    }

    @Test
    void keepingABlockInsideACardTogetherKeepsItsParagraphs() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .addSection("Kept", kept -> kept.keepTogether()
                        .addParagraph(p -> p.text("First kept"))
                        .addParagraph(p -> p.text("Second kept")))))) {
            XWPFTableCell cell = card(document);

            assertThat(paragraphIn(cell, "First kept").getCTP().getPPr().isSetKeepNext()).isTrue();
            assertThat(paragraphIn(cell, "Second kept").getCTP().getPPr().isSetKeepLines()).isTrue();
        }
    }

    @Test
    void aCardOpeningWithATableIsBookmarkedFromInsideThatTable() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .anchor("card")
                .fillColor(SURFACE)
                .addTable(t -> t.autoColumns(1).row("First row"))
                .addParagraph(p -> p.text("After"))))) {
            XWPFTable nested = card(document).getTables().get(0);

            assertThat(nested.getRow(0).getCell(0).getParagraphs().get(0).getCTP().getBookmarkStartList())
                    .extracting(CTBookmark::getName)
                    .containsExactly("card");
        }
    }

    @Test
    void aPageBreakInsideACardClosesItAndOpensItAgainAfterTheBreak() throws Exception {
        // Word breaks no page inside a cell, and the break used to land after the whole card.
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .addParagraph(p -> p.text("Before the break"))
                .addPageBreak(b -> { })
                .addParagraph(p -> p.text("After the break"))))) {
            List<IBodyElement> body = document.getBodyElements();

            assertThat(body).as("the second card, then the paragraph closing the document").hasSize(4);
            assertThat(((XWPFTable) body.get(0)).getRow(0).getCell(0).getText()).isEqualTo("Before the break");
            assertThat(((XWPFParagraph) body.get(1)).getRuns().get(0).getCTR().getBrArray(0).getType())
                    .isEqualTo(STBrType.PAGE);
            XWPFTableCell second = ((XWPFTable) body.get(2)).getRow(0).getCell(0);
            assertThat(second.getText()).isEqualTo("After the break");
            assertThat(second.getCTTc().getTcPr().getShd()).as("still the card").isNotNull();
        }
    }

    @Test
    void theSpaceAboveACardWithNothingAboveItIsHeldByAHairline() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .margin(DocumentInsets.top(12))
                .addParagraph(p -> p.text("Inside"))))) {
            XWPFParagraph holder = (XWPFParagraph) document.getBodyElements().get(0);

            assertThat(holder.getText()).isEmpty();
            assertThat(DocxTwips.of(holder.getCTP().getPPr().getSpacing().getBefore())).isEqualTo(12 * 20L);
            assertThat(DocxTwips.of(holder.getCTP().getPPr().getSpacing().getLine())).isEqualTo(2L);
            assertThat(document.getBodyElements().get(1)).isInstanceOf(XWPFTable.class);
        }
    }

    @Test
    void aCardWithNothingAboveItAndNoSpaceOwedGetsNoHolder() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .addParagraph(p -> p.text("Inside"))))) {
            assertThat(document.getBodyElements().get(0)).isInstanceOf(XWPFTable.class);
        }
    }

    @Test
    void anEmptyCardIsItsPaddingTallNotALineTaller() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .padding(DocumentInsets.of(6))))) {
            XWPFParagraph only = card(document).getParagraphs().get(0);

            assertThat(DocxTwips.of(only.getCTP().getPPr().getSpacing().getLine())).isEqualTo(2L);
        }
    }

    @Test
    void aNestedCardWithNoMarginStartsHalfItsBorderLeftOfTheCell() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Outer", outer -> outer
                .fillColor(SURFACE)
                .addSection("Inner", inner -> inner
                        .accentLeft(ACCENT, 2)
                        .addParagraph(p -> p.text("Inner")))))) {
            XWPFTable nested = card(document).getTables().get(0);

            assertThat(DocxTwips.of(nested.getCTTbl().getTblPr().getTblInd().getW())).isEqualTo(-20L);
        }
    }

    @Test
    void aCardWithNoCanvasLeavesItsWidthToTheEditor() throws Exception {
        try (XWPFDocument document = DocxExports.withoutCanvas(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .addParagraph(p -> p.text("Inside"))))) {
            var properties = card(document).getTableRow().getTable().getCTTbl().getTblPr();

            assertThat(properties.isSetTblW() && properties.getTblW().getType() == STTblWidth.DXA)
                    .as("no width rather than one computed from an unbounded page")
                    .isFalse();
        }
    }

    @Test
    void aChipOverAStripeInsideACardIsFlattenedAgainstTheStripe() throws Exception {
        DocumentColor stripe = DocumentColor.rgb(200, 200, 200);
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .addTable(t -> t.columns(DocumentTableColumn.auto())
                        .rowCells(DocumentTableCell.node(new com.demcha.compose.document.dsl.RowBuilder()
                                        .addParagraph(p -> p.inlineText("Call ").inlineCode("render()"))
                                        .addParagraph(p -> p.text("Beside"))
                                        .build())
                                .withStyle(DocumentTableStyle.builder().fillColor(stripe).build())))))) {
            XWPFTableCell striped = card(document).getTables().get(0).getRow(0).getCell(0);
            XWPFRun chip = striped.getTables().get(0).getRow(0).getCell(0).getParagraphs().stream()
                    .flatMap(p -> p.getRuns().stream())
                    .filter(run -> "render()".equals(run.text()))
                    .findFirst()
                    .orElseThrow();

            // 175/184/193 at 20% over 200/200/200 is 195/197/199.
            assertThat(hex(chip.getCTR().getRPr().getShdArray(0).getFill())).isEqualTo("C3C5C7");
        }
    }

    private static XWPFTableCell card(XWPFDocument document) {
        XWPFTable table = document.getTables().get(0);
        return table.getRow(0).getCell(0);
    }

    private static XWPFParagraph paragraphIn(XWPFTableCell cell, String text) {
        return cell.getParagraphs().stream()
                .filter(paragraph -> paragraph.getText().equals(text))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no paragraph in the card reads " + text));
    }

    private static String hex(Object value) {
        if (value instanceof byte[] bytes) {
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                out.append(String.format("%02X", b & 0xFF));
            }
            return out.toString();
        }
        return String.valueOf(value);
    }

    private static XWPFDocument export(
            java.util.function.Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content) throws Exception {
        return DocxExports.withLayout(595, 842, 36, content);
    }
}
