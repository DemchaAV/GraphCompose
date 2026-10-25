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
            assertThat(topMargin(second)).as("the page goes on with the card at its top, padding none").isZero();
        }
    }

    @Test
    void aPageBreakInASectionInsideACardCutsTheCardThere() throws Exception {
        // The page goes on with the card on the next page, its section with it: Word, which breaks
        // no page inside a cell, set both parts in the card on the first page, the break after it.
        try (XWPFDocument document = export(page -> page
                .addSection("Card", card -> card.fillColor(SURFACE).padding(DocumentInsets.of(8))
                        .addSection(in -> in.addParagraph("Part A").addPageBreak(b -> { }).addParagraph("Part B")))
                .addParagraph("After the card"))) {
            List<IBodyElement> body = document.getBodyElements();

            assertThat(body).as("two tables of the card, the break between them, and what follows").hasSize(4);
            XWPFTableCell first = ((XWPFTable) body.get(0)).getRow(0).getCell(0);
            XWPFTableCell second = ((XWPFTable) body.get(2)).getRow(0).getCell(0);
            assertThat(first.getText()).isEqualTo("Part A");
            assertThat(((XWPFParagraph) body.get(1)).getRuns().get(0).getCTR().getBrArray(0).getType())
                    .isEqualTo(STBrType.PAGE);
            assertThat(second.getText()).isEqualTo("Part B");
            assertThat(((XWPFParagraph) body.get(3)).getText()).isEqualTo("After the card");
            assertThat(topMargin(first)).as("the card's padding where it starts").isEqualTo(8 * 20);
            assertThat(topMargin(second)).as("none where the page goes on with it").isZero();
        }
    }

    @Test
    void aPageBreakInACardInsideACardCutsBoth() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addSection("Card", card -> card.fillColor(SURFACE).padding(DocumentInsets.of(8))
                        .addSection(in -> in.fillColor(DocumentColor.rgb(220, 230, 240)).padding(DocumentInsets.of(6))
                                .addParagraph("Part A").addPageBreak(b -> { }).addParagraph("Part B"))
                        .addParagraph("Card tail"))
                .addParagraph("After the card"))) {
            List<IBodyElement> body = document.getBodyElements();

            assertThat(body).hasSize(4);
            XWPFTableCell first = ((XWPFTable) body.get(0)).getRow(0).getCell(0);
            XWPFTableCell second = ((XWPFTable) body.get(2)).getRow(0).getCell(0);
            assertThat(first.getTables()).as("the inner card's first part").hasSize(1);
            assertThat(first.getTables().get(0).getRow(0).getCell(0).getText()).isEqualTo("Part A");
            assertThat(second.getTables()).as("its second, in the card's second table").hasSize(1);
            XWPFTableCell innerAfter = second.getTables().get(0).getRow(0).getCell(0);
            assertThat(innerAfter.getText()).isEqualTo("Part B");
            assertThat(topMargin(innerAfter)).as("none where the page goes on with it").isZero();
            assertThat(second.getParagraphs()).extracting(XWPFParagraph::getText).contains("Card tail");
            String xml = document.getDocument().xmlText();
            assertThat(xml.split("Part A", -1)).as("written once").hasSize(2);
            assertThat(xml.split("Part B", -1)).as("written once").hasSize(2);
            assertThat(xml.split("Card tail", -1)).as("written once").hasSize(2);
        }
    }

    @Test
    void whatFollowsABreakInASectionStandsItsSpacingBelowThePagesTop() throws Exception {
        // The section's top edge stands above its first part and its bottom edge below its last,
        // not at the break.
        try (XWPFDocument document = export(page -> page
                .addSection("Card", card -> card.fillColor(SURFACE)
                        .addSection(in -> in.spacing(4).padding(DocumentInsets.of(6)).addParagraph("Part A")
                                .addPageBreak(b -> { }).addParagraph("Part B"))))) {
            XWPFTableCell first = ((XWPFTable) document.getBodyElements().get(0)).getRow(0).getCell(0);
            XWPFTableCell second = ((XWPFTable) document.getBodyElements().get(2)).getRow(0).getCell(0);
            XWPFParagraph before = first.getParagraphs().get(first.getParagraphs().size() - 1);
            XWPFParagraph after = second.getParagraphs().get(0);

            assertThat(after.getText()).isEqualTo("Part B");
            assertThat(DocxTwips.of(after.getCTP().getPPr().getSpacing().getBefore()))
                    .as("the section's spacing, as the page sets it after the break, less what the line takes "
                        + "of it to stand on the page's baseline")
                    .isBetween(4 * 20 - 10L, 4 * 20L);
            assertThat(before.getText()).isEqualTo("Part A");
            var spacing = before.getCTP().getPPr().getSpacing();
            assertThat(spacing.isSetAfter() ? DocxTwips.of(spacing.getAfter()) : 0)
                    .as("no bottom edge of the section at the break").isLessThan(6 * 20);
        }
    }

    @Test
    void aCutSectionsAnchorMarksWhereItStartsAndItsNotesAreSaidOnce() throws Exception {
        java.util.function.Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content = page -> page
                .addSection("Card", card -> card.fillColor(SURFACE)
                        .addSection(in -> in.anchor("terms").fillColor(DocumentColor.rgb(220, 230, 240)).cornerRadius(6)
                                .addParagraph("Part A").addPageBreak(b -> { }).addParagraph("Part B")));
        try (XWPFDocument document = export(content)) {
            String xml = document.getDocument().xmlText();
            XWPFTableCell first = ((XWPFTable) document.getBodyElements().get(0)).getRow(0).getCell(0);

            assertThat(xml.split("w:name=\"terms\"", -1)).as("one bookmark").hasSize(2);
            assertThat(paragraphIn(first.getTables().get(0).getRow(0).getCell(0), "Part A").getCTP()
                    .getBookmarkStartList()).extracting(CTBookmark::getName).containsExactly("terms");
        }
        DocxExportReport report = DocxExports.report(595, 842, 36, content);
        assertThat(report.notes()).filteredOn(note -> note.subject().equals("corner radius"))
                .as("said once, of the panel the break cuts").hasSize(1);
    }

    @Test
    void aBreakOpeningASectionInACardLeavesNothingWrittenTwice() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addSection("Card", card -> card.fillColor(SURFACE).addParagraph("Head")
                        .addSection(in -> in.addPageBreak(b -> { }).addParagraph("Part B")
                                .addPageBreak(b -> { }).addPageBreak(b -> { }).addParagraph("Part D")))
                .addParagraph("After the card"))) {
            String xml = document.getDocument().xmlText();

            for (String text : List.of("Head", "Part B", "Part D", "After the card")) {
                assertThat(xml.split(text, -1)).as(text + ", written once").hasSize(2);
            }
            assertThat(((XWPFTable) document.getBodyElements().get(0)).getRow(0).getCell(0).getText()).isEqualTo("Head");
        }
    }

    @Test
    void aBreakInACardInARowsColumnIsNotCutThere() throws Exception {
        // A row's columns are a table of their own, laid out as one piece: a card there is written
        // whole, as it was.
        try (XWPFDocument document = export(page -> page.addRow(row -> row
                .addSection(card -> card.fillColor(SURFACE)
                        .addSection(in -> in.addParagraph("Part A").addPageBreak(b -> { }).addParagraph("Part B")))
                .addParagraph("Beside")))) {
            XWPFTable rowTable = (XWPFTable) document.getBodyElements().get(0);
            XWPFTableCell column = rowTable.getRow(0).getCell(0);

            assertThat(column.getTables()).as("one card").hasSize(1);
            assertThat(column.getTables().get(0).getRow(0).getCell(0).getText()).contains("Part A").contains("Part B");
        }
    }

    private static long topMargin(XWPFTableCell cell) {
        var margins = cell.getCTTc().getTcPr().getTcMar();
        return margins == null || !margins.isSetTop() ? 0 : DocxTwips.of(margins.getTop().getW());
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
    void aNestedCardWithTooThinAMarginGivesUpNoMoreThanItHolds() throws Exception {
        // 1pt of padding against a 1.5pt border: the margin can give back 0.25pt, not the 3.5
        // the border and its clearance would need. The text keeps its width; the border moves in
        // only by that quarter point.
        try (XWPFDocument document = export(page -> page.addSection("Outer", outer -> outer
                .fillColor(SURFACE)
                .addSection("Inner", inner -> inner
                        .stroke(com.demcha.compose.document.style.DocumentStroke.of(ACCENT, 1.5))
                        .padding(DocumentInsets.of(1))
                        .addParagraph(p -> p.text("Inner")))))) {
            XWPFTableCell host = card(document);
            var hostProperties = host.getCTTc().getTcPr();
            long room = DocxTwips.of(hostProperties.getTcW().getW())
                        - DocxTwips.of(hostProperties.getTcMar().getLeft().getW())
                        - DocxTwips.of(hostProperties.getTcMar().getRight().getW());
            XWPFTable nested = host.getTables().get(0);
            long width = DocxTwips.of(nested.getCTTbl().getTblPr().getTblW().getW());
            var margins = nested.getRow(0).getCell(0).getCTTc().getTcPr().getTcMar();

            assertThat(DocxTwips.of(margins.getRight().getW())).as("all its right margin given up").isZero();
            // Every border inside its margins: the room less 1pt of padding each side, and the four
            // half borders the margins give back, less what its left side gave its text — the half
            // border the table is wider by and the 0.75pt its margin gave back — which the table
            // gives up, as Word starts it half that border in from the cell's text and it ran past
            // the cell. Its text is still wider than the page's.
            assertThat(width - DocxTwips.of(margins.getLeft().getW()) - DocxTwips.of(margins.getRight().getW()))
                    .as("its text 1.5pt wider than on the page")
                    .isCloseTo(room - 40 + 60 - 30, org.assertj.core.data.Offset.offset(3L));
        }
    }

    @Test
    void aNestedCardAsWideAsItsCellEndsItsRightBorderInsideItAndKeepsItsTextsWidth() throws Exception {
        // Word starts a nested table no further left than its cell's text, draws its right border
        // outside its right edge and, on screen, cuts off what passes the cell's edge and draws
        // the cell's gridline there: a card as wide as its cell lost its right border. The points
        // it gives up come off its right margin.
        try (XWPFDocument document = export(page -> page.addSection("Outer", outer -> outer
                .fillColor(SURFACE)
                .addSection("Inner", inner -> inner
                        .stroke(com.demcha.compose.document.style.DocumentStroke.of(ACCENT, 1.5))
                        .padding(DocumentInsets.of(10))
                        .addParagraph(p -> p.text("Inner")))))) {
            XWPFTableCell host = card(document);
            XWPFTable nested = host.getTables().get(0);
            var hostProperties = host.getCTTc().getTcPr();
            long room = DocxTwips.of(hostProperties.getTcW().getW())
                        - DocxTwips.of(hostProperties.getTcMar().getLeft().getW())
                        - DocxTwips.of(hostProperties.getTcMar().getRight().getW());
            var tableProperties = nested.getCTTbl().getTblPr();
            long start = Math.max(0, tableProperties.isSetTblInd() ? DocxTwips.of(tableProperties.getTblInd().getW()) : 0);
            long width = DocxTwips.of(tableProperties.getTblW().getW());
            var margins = nested.getRow(0).getCell(0).getCTTc().getTcPr().getTcMar();
            long text = width - DocxTwips.of(margins.getLeft().getW()) - DocxTwips.of(margins.getRight().getW());

            // Word draws the right border, 1.5pt or 30 twips, outside the table's edge, and the
            // cell's gridline half a point further. The text is as wide as the card written as
            // wide as its cell, every border inside the cell's margins: its room less its 10pt
            // padding each side, and the four half borders the margins give back.
            assertThat(start + width + 30 + 10).as("its right border inside the cell, clear of its gridline")
                    .isLessThanOrEqualTo(room);
            assertThat(text).as("its text as wide as on the page")
                    .isCloseTo(room - 400 + 60, org.assertj.core.data.Offset.offset(3L));
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
                    // The chip's last letter is a run of its own, spaced by its padding.
                    .filter(run -> "render(".equals(run.text()))
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
