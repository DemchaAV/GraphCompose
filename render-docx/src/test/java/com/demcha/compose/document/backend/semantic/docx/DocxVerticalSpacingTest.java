package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The space a block holds above and below itself reaches Word.
 *
 * <p>None of it used to: a paragraph's {@code margin} and {@code padding} were dropped, and
 * a container's were dropped twice over, since a container is not a Word object and its
 * children are written where it stood. Every exported document ran its blocks together and
 * leaned on whatever Word puts between paragraphs — which was invisible only while the line
 * height was Word's too, tall enough to stand in for the gaps.</p>
 *
 * <p>Vertical space is one of the few parts of a node's box Word holds natively, which is
 * why this is written where the horizontal half is not. A container hands its top edge to
 * the first paragraph written inside it and its bottom edge to the last, because that is
 * where a reader sees it either way.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxVerticalSpacingTest {

    private static final double TWIPS_PER_POINT = 20.0;

    @Test
    void aParagraphCarriesItsOwnMarginAndPadding() throws Exception {
        List<XWPFParagraph> paragraphs = bodyOf(page -> page
                .addParagraph(p -> p.text("Above"))
                .addParagraph(p -> p.text("Spaced")
                        .padding(DocumentInsets.top(16))
                        .margin(DocumentInsets.bottom(6)))
                .addParagraph(p -> p.text("Below")));

        assertThat(before(paragraphs.get(1))).isEqualTo(Math.round(16 * TWIPS_PER_POINT));
        assertThat(before(paragraphs.get(2)))
                .as("the 6pt below it, written once, above what follows")
                .isEqualTo(Math.round(6 * TWIPS_PER_POINT));
    }

    @Test
    void marginAndPaddingOnTheSameEdgeAddUp() throws Exception {
        // Different things to the engine — outside the box and inside it — but Word has one
        // gap above a paragraph, and the page shows their sum.
        List<XWPFParagraph> paragraphs = bodyOf(page -> page.addParagraph(p -> p.text("Both")
                .padding(DocumentInsets.top(10))
                .margin(DocumentInsets.top(4))));

        assertThat(before(paragraphs.get(0))).isEqualTo(Math.round(14 * TWIPS_PER_POINT));
    }

    @Test
    void aContainersEdgesGoToItsFirstAndLastParagraph() throws Exception {
        List<XWPFParagraph> paragraphs = bodyOf(page -> page.addSection("Card", card -> card
                .padding(DocumentInsets.symmetric(14, 0))
                .margin(DocumentInsets.symmetric(6, 0))
                .addParagraph(p -> p.text("First"))
                .addParagraph(p -> p.text("Middle"))
                .addParagraph(p -> p.text("Last")))
                .addParagraph(p -> p.text("Below")));

        assertThat(paragraphs).hasSize(4);
        assertThat(before(paragraphs.get(0)))
                .as("14pt of padding and 6pt of margin, on the paragraph that starts the card")
                .isEqualTo(Math.round(20 * TWIPS_PER_POINT));
        assertThat(before(paragraphs.get(1))).as("nothing in the middle").isZero();
        assertThat(after(paragraphs.get(1))).isZero();
        assertThat(before(paragraphs.get(3)))
                .as("and the same below the one that ends it, above what follows the card")
                .isEqualTo(Math.round(20 * TWIPS_PER_POINT));
    }

    @Test
    void theSpaceBelowTheLastBlockIsLeftOutAtTheEnd() throws Exception {
        // Nothing follows it: the page ends, and space written below the last line could only
        // push that line onto a page of its own.
        List<XWPFParagraph> paragraphs = bodyOf(page -> page
                .addParagraph(p -> p.text("Last").margin(DocumentInsets.bottom(30))));

        assertThat(after(paragraphs.get(0))).isZero();
    }

    @Test
    void theSpaceBelowTheLastLineOfEachCellOfAClosingTableIsLeftOut() throws Exception {
        // A two-column CV's columns end with their bottom padding; the table is the last thing
        // on the page, so that padding holds nothing up.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow(row -> row
                        .addSection("Left", left -> left.padding(DocumentInsets.bottom(30))
                                .addParagraph(p -> p.text("Sidebar")))
                        .addSection("Right", right -> right.padding(DocumentInsets.bottom(30))
                                .addParagraph(p -> p.text("Main")))))) {
            for (var cell : document.getTables().get(0).getRow(0).getTableCells()) {
                XWPFParagraph last = cell.getParagraphs().get(cell.getParagraphs().size() - 1);
                assertThat(after(last)).as("below '%s'", last.getText()).isZero();
            }
        }
    }

    @Test
    void nestingAddsUpOnTheSameParagraph() throws Exception {
        // An outer section and an inner card both start at the same paragraph, so it
        // carries both edges — which is what the page shows.
        List<XWPFParagraph> paragraphs = bodyOf(page -> page.addSection("Outer", outer -> outer
                .padding(DocumentInsets.top(8))
                .addSection("Inner", inner -> inner
                        .padding(DocumentInsets.top(12))
                        .addParagraph(p -> p.text("Deep").padding(DocumentInsets.top(3))))));

        assertThat(before(paragraphs.get(0))).isEqualTo(Math.round(23 * TWIPS_PER_POINT));
    }

    @Test
    void aContainerOfTablesHandsOnItsBottomEdgeAndDropsItsTop() throws Exception {
        // A container of tables has nowhere to put its top edge — Word has no space-before
        // on a table, and a top edge that waited would land below the table instead of
        // above it, which is somewhere the document never asked for. Its bottom edge has
        // somewhere to go: the gap under the table is the space above what follows, and
        // that is a paragraph. The two are told apart by asking for different numbers.
        List<XWPFParagraph> paragraphs = bodyOf(page -> page
                .addSection("TablesOnly", section -> section
                        .padding(new DocumentInsets(30, 0, 8, 0))
                        .addTable(t -> t.autoColumns(2).row("A", "B")))
                .addParagraph(p -> p.text("After")));

        assertThat(before(paragraphs.get(paragraphs.size() - 1)))
                .as("the bottom edge, and only it — the top one did not follow the table out")
                .isEqualTo(160L);
    }

    @Test
    void aContainerThatWritesNothingHandsBackTheEdgesAroundIt() throws Exception {
        // A portrait opening a padded sidebar: a layer stack holding only a path, which the
        // export drops. It stood above nothing, so the sidebar's top padding is still waiting
        // for the first paragraph — the stack drops only its own top edge, and its bottom
        // edge is owed below it as any container's is.
        List<XWPFParagraph> paragraphs = bodyOf(page -> page
                .addSection("Sidebar", sidebar -> sidebar
                        .padding(DocumentInsets.top(30))
                        .addLayerStack(stack -> stack
                                .name("Portrait")
                                .margin(new DocumentInsets(5, 0, 7, 0))
                                .layer(new com.demcha.compose.document.dsl.PathBuilder()
                                        .name("Silhouette").size(40, 40)
                                        .moveTo(0, 0).lineTo(1, 0).lineTo(0.5, 1).closePath()
                                        .build()))
                        .addParagraph(p -> p.text("Contact"))));

        assertThat(paragraphs).hasSize(1);
        assertThat(before(paragraphs.get(0)))
                .as("the sidebar's 30pt of padding and the portrait's 7pt below it, not its 5pt above")
                .isEqualTo(Math.round(37 * TWIPS_PER_POINT));
    }

    @Test
    void aContainerOfTablesStillDropsTheEdgesAroundIt() throws Exception {
        // The counterpart: the tables were written, and the outer section's top edge stood
        // above them. Handed back, it would land below them on the paragraph that follows.
        List<XWPFParagraph> paragraphs = bodyOf(page -> page
                .addSection("Outer", outer -> outer
                        .padding(DocumentInsets.top(30))
                        .addSection("TablesOnly", section -> section
                                .addTable(t -> t.autoColumns(2).row("A", "B")))
                        .addParagraph(p -> p.text("After"))));

        assertThat(before(paragraphs.get(paragraphs.size() - 1)))
                .as("the outer 30pt did not follow the table out")
                .isZero();
    }

    @Test
    void theEdgeATableStandsBelowIsHeldAboveIt() throws Exception {
        // A padded section opening with a row, after a paragraph: the page has the section's
        // top edge between the paragraph and the row. The paragraph above holds it below
        // itself; it neither lands under the row nor goes missing.
        List<XWPFParagraph> paragraphs = bodyOf(page -> page
                .addParagraph(p -> p.text("Before"))
                .addSection("Entry", entry -> entry
                        .padding(DocumentInsets.top(30))
                        .addTable(t -> t.autoColumns(2).row("Title", "2024"))
                        .addParagraph(p -> p.text("After"))));

        assertThat(after(paragraphs.get(0))).as("above the table").isEqualTo(Math.round(30 * TWIPS_PER_POINT));
        assertThat(before(paragraphs.get(paragraphs.size() - 1))).as("not under it").isZero();
    }

    @Test
    void spaceOwedAfterAPageBreakStaysOffThePageBeforeIt() throws Exception {
        // The break paragraph closes the page; space owed after it belongs to the next page,
        // not below the last paragraph of the one before.
        List<XWPFParagraph> paragraphs = bodyOf(page -> page
                .addParagraph(p -> p.text("Before"))
                .addPageBreak(b -> b.name("Break"))
                .addSection("Entry", entry -> entry
                        .padding(DocumentInsets.top(30))
                        .addTable(t -> t.autoColumns(2).row("Title", "2024"))));

        assertThat(after(paragraphs.get(0))).isZero();
    }

    @Test
    void aTableDropsTheEdgeItStoodBelow() throws Exception {
        // A section opening with a table: its top edge stood above the table, so it does not
        // wait for the paragraph under it — nor pass through a drawn-only divider on the way.
        List<XWPFParagraph> direct = bodyOf(page -> page
                .addSection("Outer", outer -> outer
                        .padding(DocumentInsets.top(30))
                        .addTable(t -> t.autoColumns(2).row("A", "B"))
                        .addParagraph(p -> p.text("After"))));
        List<XWPFParagraph> pastADivider = bodyOf(page -> page
                .addSection("Outer", outer -> outer
                        .padding(DocumentInsets.top(30))
                        .addTable(t -> t.autoColumns(2).row("A", "B"))
                        .addLayerStack(stack -> stack
                                .name("Divider")
                                .layer(new com.demcha.compose.document.dsl.PathBuilder()
                                        .name("Stroke").size(40, 4)
                                        .moveTo(0, 0).lineTo(1, 0).lineTo(1, 1).closePath()
                                        .build()))
                        .addParagraph(p -> p.text("After"))));

        assertThat(before(direct.get(direct.size() - 1)))
                .as("the outer 30pt did not land under the table")
                .isZero();
        assertThat(before(pastADivider.get(pastADivider.size() - 1)))
                .as("nor was it handed on by the divider the export drops")
                .isZero();
    }

    @Test
    void oneGapIsWrittenOnceRatherThanFromBothSides() throws Exception {
        // A gap between two blocks is one distance, and it used to be written as two: after
        // on the block above and before on the one below. That is the same thing only in an
        // editor that adds them — LibreOffice takes the larger, so 20 below and 16 above
        // rendered as 20 where the page shows 36, and everything under it sat 16pt high.
        List<XWPFParagraph> paragraphs = bodyOf(page -> page
                .addParagraph(p -> p.text("Above").margin(DocumentInsets.bottom(20)))
                .addParagraph(p -> p.text("Below").padding(DocumentInsets.top(16))));

        assertThat(before(paragraphs.get(1)))
                .as("the whole gap, on the side that can hold it")
                .isEqualTo(Math.round(36 * TWIPS_PER_POINT));
        assertThat(after(paragraphs.get(0)))
                .as("and nothing on the other, so adding and taking the maximum agree")
                .isZero();
    }

    @Test
    void aCardsBottomEdgeAndTheNextHeadingsTopAreOneGap() throws Exception {
        // The measured case: a section holding 20pt below itself, then a heading asking
        // for 16pt above it.
        List<XWPFParagraph> paragraphs = bodyOf(page -> page
                .addSection("Card", card -> card
                        .padding(DocumentInsets.of(14))
                        .margin(DocumentInsets.symmetric(6, 0))
                        .addParagraph(p -> p.text("Inside")))
                .addParagraph(p -> p.text("Heading").padding(DocumentInsets.top(16))));

        assertThat(after(paragraphs.get(0)))
                .as("the card's last paragraph hands its edge on")
                .isZero();
        assertThat(before(paragraphs.get(1)))
                .as("14 padding + 6 margin below the card, 16 above the heading")
                .isEqualTo(Math.round(36 * TWIPS_PER_POINT));
    }

    @Test
    void theGapAboveATableIsWrittenOnTheParagraphBeforeIt() throws Exception {
        // Word has no space above a table, so this is the one gap the block above has to
        // carry itself.
        List<XWPFParagraph> paragraphs = bodyOf(page -> page
                .addParagraph(p -> p.text("Lead").margin(DocumentInsets.bottom(12)))
                .addTable(t -> t.autoColumns(2).row("A", "B")));

        assertThat(after(paragraphs.get(0))).isEqualTo(Math.round(12 * TWIPS_PER_POINT));
    }

    @Test
    void aGapInsideACellStaysInsideIt() throws Exception {
        // A cell is its own stream of paragraphs. Its last gap must not travel out and land
        // on whatever the body writes next, which is a page away from where it belongs.
        List<XWPFParagraph> paragraphs = bodyOf(page -> page
                .addTable(t -> t.autoColumns(2)
                        .rowCells(DocumentTableCell.text("Plain"),
                                DocumentTableCell.node(new ParagraphBuilder()
                                        .name("Padded")
                                        .text("Inside")
                                        .margin(DocumentInsets.bottom(24))
                                        .build())))
                .addParagraph(p -> p.text("After the table")));

        assertThat(before(paragraphs.get(paragraphs.size() - 1)))
                .as("the cell's own 24pt did not follow it out")
                .isZero();
    }

    @Test
    void theGapBeforeAPageBreakIsWrittenOnTheParagraphThatHoldsIt() throws Exception {
        // A page break is a paragraph of its own, written outside the body-paragraph path,
        // so a gap waiting for "the next paragraph" would land after the break.
        List<XWPFParagraph> paragraphs = bodyOf(page -> page
                .addParagraph(p -> p.text("Last on the page").margin(DocumentInsets.bottom(10)))
                .addPageBreak(b -> b.name("toSecond"))
                .addParagraph(p -> p.text("First on the next")));

        assertThat(after(paragraphs.get(0))).isEqualTo(Math.round(10 * TWIPS_PER_POINT));
        assertThat(before(paragraphs.get(paragraphs.size() - 1))).isZero();
    }

    @Test
    void aTableHoldsItsOwnSpaceAboveAndBelow() throws Exception {
        // Word has no space above a table and none below one, so a table's own box used to
        // be dropped: measured on the probe corpus, a billing table lost the 6pt it holds
        // above itself. Neither edge needs an element of its own — the space above a table
        // is the space below the paragraph before it.
        List<XWPFParagraph> paragraphs = bodyOf(page -> page
                .addParagraph(p -> p.text("Lead"))
                .addTable(t -> t.autoColumns(2).row("A", "B")
                        .margin(new DocumentInsets(9, 0, 12, 0)))
                .addParagraph(p -> p.text("After")));

        assertThat(after(paragraphs.get(0)))
                .as("the table's top edge, on the paragraph above it")
                .isEqualTo(Math.round(9 * TWIPS_PER_POINT));
        assertThat(before(paragraphs.get(1)))
                .as("its bottom edge, on the paragraph below it")
                .isEqualTo(Math.round(12 * TWIPS_PER_POINT));
    }

    @Test
    void aRowHoldsItsOwnPaddingTheSameWay() throws Exception {
        // A row is exported as a one-row table, so its padding went the same way a table's
        // did — 14pt lost at each edge on the probe.
        List<XWPFParagraph> paragraphs = bodyOf(page -> page
                .addParagraph(p -> p.text("Lead"))
                .addRow("Pair", r -> r.padding(DocumentInsets.symmetric(14, 0))
                        .addParagraph(p -> p.text("Left"))
                        .addParagraph(p -> p.text("Right")))
                .addParagraph(p -> p.text("After")));

        assertThat(after(paragraphs.get(0))).isEqualTo(Math.round(14 * TWIPS_PER_POINT));
        assertThat(before(paragraphs.get(1))).isEqualTo(Math.round(14 * TWIPS_PER_POINT));
    }

    @Test
    void aTableWithNothingAboveItLosesThatEdgeAndKeepsTheOther() throws Exception {
        // The one edge Word genuinely cannot hold: there is no paragraph to carry it, and
        // an empty one would be a line the document never asked for.
        List<XWPFParagraph> paragraphs = bodyOf(page -> page
                .addTable(t -> t.autoColumns(2).row("A", "B")
                        .margin(new DocumentInsets(9, 0, 12, 0)))
                .addParagraph(p -> p.text("After")));

        assertThat(before(paragraphs.get(0)))
                .as("the bottom edge and only it — the top had nothing to land on")
                .isEqualTo(Math.round(12 * TWIPS_PER_POINT));
    }

    @Test
    void aPictureKeepsItsOwnMargin() throws Exception {
        // A picture is a block like any other, and its paragraph is its block. Measured on
        // the probe corpus, an image holding 12pt at each edge ran straight into the
        // heading under it, 24pt short of the page.
        List<XWPFParagraph> paragraphs = bodyOf(page -> page
                .addParagraph(p -> p.text("Above"))
                .addImage(image -> image
                        .name("Plate")
                        .source(DocumentImageData.fromBytes(pngBytes()))
                        .width(40)
                        .height(20)
                        .margin(DocumentInsets.symmetric(12, 0)))
                .addParagraph(p -> p.text("Below")));

        // The picture's own paragraph carries no text, so it is not in bodyOf's list: its
        // top edge is on it and its bottom edge lands on the paragraph after.
        assertThat(before(paragraphs.get(paragraphs.size() - 1)))
                .as("the picture's bottom edge reaches the block under it")
                .isEqualTo(Math.round(12 * TWIPS_PER_POINT));
    }

    @Test
    void aListKeepsItsOwnBoxAndTheSpaceBetweenItsItems() throws Exception {
        // A list is not a Word object either — its items are paragraphs written where it
        // stood — so its edges went the way a container's did, and itemSpacing with them.
        // Measured on the probe corpus, a four-item checklist ran 13pt short of the page.
        List<XWPFParagraph> paragraphs = bodyOf(page -> page
                .addParagraph(p -> p.text("Lead"))
                .addList(list -> list
                        .name("Checklist")
                        .itemSpacing(3)
                        .padding(DocumentInsets.top(4))
                        .margin(DocumentInsets.bottom(10))
                        .items("First", "Second", "Third"))
                .addParagraph(p -> p.text("After")));

        assertThat(before(paragraphs.get(1)))
                .as("the list's top edge, above its first item, and no item gap yet")
                .isEqualTo(Math.round(4 * TWIPS_PER_POINT));
        assertThat(before(paragraphs.get(2)))
                .as("the gap between two items")
                .isEqualTo(Math.round(3 * TWIPS_PER_POINT));
        assertThat(before(paragraphs.get(3))).isEqualTo(Math.round(3 * TWIPS_PER_POINT));
        assertThat(before(paragraphs.get(4)))
                .as("the list's bottom edge, on the paragraph after it")
                .isEqualTo(Math.round(10 * TWIPS_PER_POINT));
    }

    @Test
    void aBlockThatAsksForNothingCarriesNoSpacingAtAll() throws Exception {
        List<XWPFParagraph> paragraphs = bodyOf(page -> page.addParagraph(p -> p.text("Plain")));

        CTPPr properties = paragraphs.get(0).getCTP().getPPr();
        assertThat(properties == null || !properties.isSetSpacing()
                   || (!properties.getSpacing().isSetBefore() && !properties.getSpacing().isSetAfter()))
                .as("no w:before and no w:after, rather than zeros")
                .isTrue();
    }

    private static byte[] pngBytes() {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(
                    new java.awt.image.BufferedImage(40, 20,
                            java.awt.image.BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static long before(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetBefore()) {
            return 0;
        }
        return DocxTwips.of(properties.getSpacing().getBefore());
    }

    private static long after(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetAfter()) {
            return 0;
        }
        return DocxTwips.of(properties.getSpacing().getAfter());
    }

    private static List<XWPFParagraph> bodyOf(Consumer<PageFlowBuilder> content) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, content)) {
            return document.getParagraphs().stream()
                    .filter(paragraph -> !paragraph.getText().isBlank())
                    .toList();
        }
    }
}
