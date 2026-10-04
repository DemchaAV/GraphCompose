package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.ListBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
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
    void inACellACoupleOfPointsOfTheSideTheTextDoesNotLeanOnStayTheEditors() throws Exception {
        // Off the side a line is set from, they moved every padded line in a cell 2pt off the
        // page's: ObsidianInvoice's amounts ended 1.8pt right of it, OrangeOps' certifications
        // started 2pt left. Padding, which a placed row's cell does not hold as it holds a left
        // margin.
        DocumentInsets sides = new DocumentInsets(0, 40, 0, 10);
        try (XWPFDocument document = DocxExports.withLayout(500, 600, 20, page -> page
                .addRow(row -> row
                        .addParagraph(p -> p.text("Left").padding(sides))
                        .addParagraph(p -> p.text("Right").padding(sides).align(TextAlign.RIGHT))
                        .addParagraph(p -> p.text("Centre").padding(sides).align(TextAlign.CENTER))
                        .addParagraph(p -> p.text("שלום").padding(sides)
                                .direction(TextDirection.RTL))))) {
            var cells = document.getTables().get(0).getRow(0).getTableCells();
            long[][] expected = {{10, 38}, {8, 40}, {8, 38}, {8, 38}};
            String[] what = {"a left-aligned line keeps its left", "a right-aligned line keeps its right",
                    "a centred line spares both sides", "a right-to-left line spares both sides"};
            for (int i = 0; i < expected.length; i++) {
                CTInd indent = indent(cells.get(i).getParagraphs().get(0));
                long[] written = {DocxTwips.of(indent.getLeft()), DocxTwips.of(indent.getRight())};
                // A right-to-left paragraph's indents are written mirrored, start and end.
                if (i == 3) {
                    assertThat(written).as(what[i]).containsExactlyInAnyOrder(expected[i][0] * 20, expected[i][1] * 20);
                } else {
                    assertThat(written).as(what[i]).containsExactly(expected[i][0] * 20, expected[i][1] * 20);
                }
            }
        }
    }

    @Test
    void aSideUnderTheEditorsCoupleOfPointsIsAllTheirsOnTheSideTheTextDoesNotLeanOn() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow(row -> row
                        .addParagraph(p -> p.text("Left").padding(new DocumentInsets(0, 1.5, 0, 1.5)))
                        .addParagraph("Right")))) {
            CTInd indent = indent(document.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0));

            assertThat(DocxTwips.of(indent.getLeft())).as("the side the line is set from").isEqualTo(30);
            assertThat(indent.isSetRight() && DocxTwips.of(indent.getRight()) > 0)
                    .as("a side under 2pt is all the editor's").isFalse();
        }
    }

    @Test
    void aParagraphInALayeredRowIsHeldInByItsOwnSides() throws Exception {
        // SubscriptionInvoice's metadata labels, padded past their bars in a row on a layer,
        // stood at the bars in Word: an overlay's paragraph took none of its own sides.
        DocumentNode row = new SectionBuilder().name("Holder")
                .addRow(line -> line.spacing(0)
                        .columns(DocumentRowColumn.fixed(4), DocumentRowColumn.weight(1))
                        .addShape(bar -> bar.size(4, 20).fillColor(DocumentColor.BLACK))
                        .addParagraph(p -> p.text("Invoice No").padding(new DocumentInsets(0, 0, 0, 9.6))))
                .build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addLayerStack(stack -> stack.layer(row, LayerAlign.TOP_LEFT)))) {
            XWPFParagraph label = document.getTables().stream()
                    .flatMap(table -> table.getRows().stream())
                    .flatMap(tableRow -> tableRow.getTableCells().stream())
                    .flatMap(cell -> cell.getParagraphs().stream())
                    .filter(paragraph -> paragraph.getText().equals("Invoice No")).findFirst().orElseThrow();

            assertThat(DocxTwips.of(indent(label).getLeft())).as("the label stands its padding past the bar")
                    .isEqualTo(192);
        }
    }

    @Test
    void aListIsHeldInByAllOfItsLeftSideInACellAndOnALayer() throws Exception {
        // An item's marker is set from its left edge: NavySidebar's lists, padded in their
        // column, stood 2pt left of the page's with the editor's points taken off that side.
        // Its right side gives the editor theirs.
        for (boolean layered : new boolean[] {false, true}) {
            DocumentNode node = new ListBuilder().name("Items").dash()
                    .padding(new DocumentInsets(0, 30, 0, 10)).items("Alpha").build();
            try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> {
                if (layered) {
                    page.addLayerStack(stack -> stack.layer(node, LayerAlign.TOP_LEFT));
                } else {
                    page.addRow(row -> row.addSection(column -> column.add(node)).addParagraph("Beside"));
                }
            })) {
                XWPFParagraph item = document.getParagraphs().stream()
                        .filter(p -> p.getText().endsWith("Alpha")).findFirst()
                        .orElseGet(() -> document.getTables().get(0).getRow(0).getCell(0).getParagraphs().stream()
                                .filter(p -> p.getText().endsWith("Alpha")).findFirst().orElseThrow());
                CTInd indent = indent(item);

                assertThat(indent).as("the item carries the padding past its level, layered %s", layered).isNotNull();
                assertThat(DocxTwips.of(indent.getLeft()))
                        .as("the level's column and all 10pt of the padding, layered %s", layered)
                        .isEqualTo(DocxListLevels.column(document) + 200);
                assertThat(DocxTwips.of(indent.getRight()))
                        .as("30pt, less the editor's 2pt in a cell, layered %s", layered)
                        .isEqualTo(layered ? 30 * 20L : 28 * 20L);
            }
        }
    }

    @Test
    void aParagraphThatIsALayerOfABandStandsItsOwnSidesPastWhereTheBandPlacesIt() throws Exception {
        // The band holds the layer in to its margin box; its padding is inside that, taken once.
        DocumentNode label = new ParagraphBuilder().name("Label").text("Status")
                .padding(new DocumentInsets(0, 0, 0, 10)).build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addLayerStack(stack -> stack
                        .back(new ShapeBuilder().name("Plate").size(300, 30).fillColor(DocumentColor.BLACK).build())
                        .position(label, 50, 0, LayerAlign.TOP_LEFT)))) {
            XWPFParagraph status = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().equals("Status")).findFirst().orElseThrow();

            assertThat(DocxTwips.of(indent(status).getLeft())).as("50pt along, and its 10pt padding past that")
                    .isEqualTo(60 * 20L);
        }
    }

    @Test
    void aPlacedRowsCellHoldsItsChildsLeftMarginOnce() throws Exception {
        // The layout starts the child past its left margin, and the cell's text starts where
        // the child does: the paragraph indenting by it as well stood it off twice as far.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow(row -> row
                        .addParagraph(p -> p.text(SUMMARY).margin(new DocumentInsets(0, 0, 0, 12)))
                        .addParagraph("Right")))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);
            CTInd indent = indent(cell.getParagraphs().get(0));

            assertThat(DocxTwips.of(cell.getCTTc().getTcPr().getTcMar().getLeft().getW()))
                    .as("the cell starts the text past the margin").isEqualTo(12 * 20L);
            assertThat(indent == null || !indent.isSetLeft() || DocxTwips.of(indent.getLeft()) == 0)
                    .as("and the paragraph does not indent by it again").isTrue();
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

            assertThat(indent == null || !indent.isSetRight() || DocxTwips.of(indent.getRight()) >= 0)
                    .as("its lines of several words break where the page breaks them").isTrue();
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
