package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageMarginRule;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * The line holding a block's top edge on the page the layout moves it to leaves the space between
 * it and the block above as the flow has it, wherever the editor breaks the page instead.
 *
 * <p>The line stands at the top of that page only while the editor breaks where the layout does.
 * Where it breaks elsewhere, or an edit above moves the block up its page, the line stands in the
 * flow, and the space the export writes between the two blocks is what the editor shows: here it
 * is held to what the export writes for the same document on a page tall enough to need no break.
 * Measured in Word and LibreOffice, a document exported with the break and its page then made that
 * tall draws every shape below within 0.36pt of the one laid out for it, the space above them
 * dropped at the top of a page in both editors.</p>
 *
 * <p>A paragraph's first line, a row's cells' included, is set where the layout sets it once the
 * space above it is written: raised into that space where the block stays on its page, not where
 * its edge is held. That difference, a line's raise, is the one a block of text is compared within;
 * a table's is compared to the twip, and a line a twip tall holding the gap alone to that twip.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxPageTopHoldInTheFlowTest {

    private static final double EDGE = 12;
    private static final double GAP = 4;
    private static final double MOVED = 200;
    private static final double TALL = 1200;
    private static final double BORDER = 2;
    private static final int LINES_BEFORE_A_BORDERED_ROW = 7;

    @Test
    void aParagraphMovedToANewPageKeepsTheFlowsSpaceAboveIt() throws Exception {
        assertTheSpaceAboveIsTheFlows(page -> {
            lines(page);
            page.addParagraph(p -> p.text("Held").margin(new DocumentInsets(EDGE, 0, 0, 0)));
        }, "Line 7", "Held");
    }

    @Test
    void aRowMovedToANewPageKeepsTheFlowsSpaceAboveIt() throws Exception {
        assertTheSpaceAboveIsTheFlows(page -> {
            lines(page);
            closingRow(page);
        }, "Line 7", "Notes");
    }

    @Test
    void aTableMovedToANewPageKeepsTheFlowsSpaceAboveIt() throws Exception {
        assertTheSpaceAboveIsTheFlows(page -> {
            lines(page);
            page.addTable(t -> t.name("Totals").margin(new DocumentInsets(EDGE, 0, 0, 0))
                    .autoColumns(2).row("Subtotal", "8,500.00").row("VAT", "1,700.00"));
        }, "Line 7", "Subtotal", GAP + EDGE, 0);
    }

    @Test
    void aListMovedToANewPageKeepsTheFlowsSpaceAboveIt() throws Exception {
        assertTheSpaceAboveIsTheFlows(page -> {
            lines(page);
            page.addList(l -> l.margin(new DocumentInsets(EDGE, 0, 0, 0)).items("First", "Second"));
        }, "Line 7", "First");
    }

    @Test
    void aRowMovedToANewPageAfterATableKeepsTheGapBelowTheTable() throws Exception {
        // Seven rows fill the first page, and the row moves to the second. No paragraph above
        // carries the gap below the table: the line holding the row's edge carries it above
        // itself, where an editor drops it at the top of a page and keeps it anywhere else.
        Consumer<PageFlowBuilder> content = page -> {
            itemsTable(page);
            closingRow(page);
        };
        assertTheSpaceAboveIsTheFlows(content, "Item 6", "Notes");

        try (XWPFDocument document = DocxExports.withLayout(300, MOVED, 20, content)) {
            XWPFParagraph line = (XWPFParagraph) between(document, "Item 6", "Notes").get(0);
            CTSpacing spacing = line.getCTP().getPPr().getSpacing();
            assertThat(DocxTwips.of(spacing.getLine())).as("the line holds the row's edge on its page")
                    .isEqualTo(Math.round(EDGE * 20));
            assertThat(DocxTwips.of(spacing.getBefore())).as("and the gap below the table above itself")
                    .isEqualTo(Math.round(GAP * 20));
        }
    }

    @Test
    void aPaintedRowMovedToANewPageAfterATableKeepsTheGapBelowTheTable() throws Exception {
        // Written as a panel, through its own path to the line holding its edge.
        Consumer<PageFlowBuilder> content = page -> {
            itemsTable(page);
            page.addRow(row -> row.name("Closing").margin(new DocumentInsets(EDGE, 0, 0, 0))
                    .fillColor(DocumentColor.rgb(230, 230, 250)).addParagraph("Notes").addParagraph("Payment"));
        };
        assertTheSpaceAboveIsTheFlows(content, "Item 6", "Notes");

        try (XWPFDocument document = DocxExports.withLayout(300, MOVED, 20, content)) {
            CTSpacing spacing = spacing((XWPFParagraph) between(document, "Item 6", "Notes").get(0));
            assertThat(DocxTwips.of(spacing.getLine())).isEqualTo(Math.round(EDGE * 20));
            assertThat(DocxTwips.of(spacing.getBefore())).isEqualTo(Math.round(GAP * 20));
        }
    }

    @Test
    void aRowMovedToANewPageAfterABorderedRowKeepsWhatTheBorderLeavesOfTheGap() throws Exception {
        // The border below the row above reaches past its box, as Word draws it, and takes that
        // much of the gap in the flow; the line carries what it leaves.
        assertTheSpaceAboveIsTheFlows(page -> {
            page.spacing(GAP);
            for (int i = 0; i < LINES_BEFORE_A_BORDERED_ROW; i++) {
                page.addParagraph("Line " + i);
            }
            page.addRow(row -> row.name("Bordered").fillColor(DocumentColor.rgb(230, 230, 250))
                    .stroke(new DocumentStroke(DocumentColor.rgb(0, 0, 0), BORDER)).addParagraph("Bordered"));
            closingRow(page);
        }, "Bordered", "Notes", GAP + EDGE - BORDER, DocxExports.DEFAULT_LINE_RAISE);
    }

    @Test
    void aParagraphMovedToANewPageAfterATableKeepsTheGapBelowTheTable() throws Exception {
        // A paragraph's line takes the gap as its space above through the paragraph's own path,
        // as it did before; held here beside the blocks written as tables.
        assertTheSpaceAboveIsTheFlows(page -> {
            itemsTable(page);
            page.addParagraph(p -> p.text("Held").margin(new DocumentInsets(EDGE, 0, 0, 0)));
        }, "Item 6", "Held");
    }

    @Test
    void aRowWithNoEdgeMovedToANewPageAfterATableKeepsTheGapBelowTheTable() throws Exception {
        // Nothing of the row's own is carried to its page, and a line is written for the gap
        // alone, a twip tall, the gap above it; without it the gap was lost as before.
        Consumer<PageFlowBuilder> content = page -> {
            itemsTable(page);
            page.addTable(t -> t.name("Closing").autoColumns(2).row("Notes", "Payment").row("Due", "Now"));
        };
        assertTheSpaceAboveIsTheFlows(content, "Item 6", "Notes", GAP, 1);

        try (XWPFDocument document = DocxExports.withLayout(300, MOVED, 20, content)) {
            CTSpacing spacing = spacing((XWPFParagraph) between(document, "Item 6", "Notes").get(0));
            assertThat(DocxTwips.of(spacing.getLine())).as("a line for the gap alone").isEqualTo(1);
            assertThat(DocxTwips.of(spacing.getBefore())).as("the gap above it").isEqualTo(Math.round(GAP * 20));
        }
    }

    @Test
    void aRowMovedAfterATableToAPageWithItsOwnMarginsHoldsNoGapAboveItsLine() throws Exception {
        // The page's margins open a Word section with the line, and a section's first paragraph
        // keeps its space above: there the gap would stand the row that low. The section's break
        // fixes the page, so there is no flow for the gap to stand in.
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(300, MOVED)
                .margin(DocumentInsets.of(20)).create()) {
            session.pageMargins(List.of(PageMarginRule.from(2, new DocumentInsets(40, 20, 20, 20))));
            session.pageFlow(page -> {
                itemsTable(page);
                closingRow(page);
            });
            docx = session.export(new DocxSemanticBackend());
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            List<IBodyElement> between = between(document, "Item 6", "Notes");
            XWPFParagraph line = (XWPFParagraph) between.get(between.size() - 1);
            XWPFParagraph ending = (XWPFParagraph) between.get(between.size() - 2);
            assertThat(ending.getCTP().getPPr().isSetSectPr()).as("the section ends right above the line").isTrue();
            assertThat(isAHeldLine(line)).isTrue();
            assertThat(DocxTwips.of(spacing(line).getLine())).as("the row's edge, held")
                    .isEqualTo(Math.round(EDGE * 20));
            assertThat(DocxTwips.of(spacing(line).getBefore())).as("and no gap above it").isZero();
        }
    }

    @Test
    void aRowAfterATableOnItsPageIsKeptApartByTheSeparatorAsBefore() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(300, TALL, 20, page -> {
            itemsTable(page);
            closingRow(page);
        })) {
            List<IBodyElement> between = between(document, "Item 6", "Notes");
            assertThat(between).as("only the paragraph keeping the two tables apart").hasSize(1);
            XWPFParagraph separator = (XWPFParagraph) between.get(0);
            assertThat(separator.getCTP().getPPr().getRPr()).as("a separator, not a line holding an edge")
                    .isNull();
        }
    }

    /**
     * Exports the content on a page that moves its last block to a second page and on one tall
     * enough to hold it all, and holds the space written between the two named blocks in the first
     * to the second's.
     */
    private static void assertTheSpaceAboveIsTheFlows(Consumer<PageFlowBuilder> content, String above,
                                                      String block) throws Exception {
        assertTheSpaceAboveIsTheFlows(content, above, block, GAP + EDGE, DocxExports.DEFAULT_LINE_RAISE);
    }

    /**
     * @param flow  the space between the blocks on one page, in points, to within the raise the line
     *              above owes below itself
     * @param apart how many twips the two may differ by: a line's raise for a block of text, none for
     *              a table, a twip where the line holds no edge and is a twip tall
     */
    private static void assertTheSpaceAboveIsTheFlows(Consumer<PageFlowBuilder> content, String above,
                                                      String block, double flow, long apart) throws Exception {
        long moved;
        long flowing;
        try (XWPFDocument document = DocxExports.withLayout(300, MOVED, 20, content)) {
            assertThat(between(document, above, block)).as("a line holds the block's edge on its new page")
                    .anySatisfy(element -> assertThat(isAHeldLine(element)).isTrue());
            moved = spaceBetween(document, above, block);
        }
        try (XWPFDocument document = DocxExports.withLayout(300, TALL, 20, content)) {
            assertThat(between(document, above, block)).as("no line holds an edge where nothing moves")
                    .noneSatisfy(element -> assertThat(isAHeldLine(element)).isTrue());
            flowing = spaceBetween(document, above, block);
        }
        assertThat(flowing).as("the flow's space between the blocks, the gap and the block's own edge")
                .isCloseTo(Math.round(flow * 20), within(DocxExports.DEFAULT_LINE_RAISE));
        assertThat(moved).as("the space between them where the line holding the edge stands in the flow")
                .isCloseTo(flowing, within(apart));
    }

    /**
     * The space the export writes between two blocks, in twips: below the one above, every line
     * between them, and above the one below; a table has none of its own above or below it.
     */
    private static long spaceBetween(XWPFDocument document, String above, String block) {
        IBodyElement upper = elementWith(document, above);
        IBodyElement lower = elementWith(document, block);
        long space = upper instanceof XWPFParagraph paragraph ? after(paragraph) : 0;
        for (IBodyElement element : between(document, above, block)) {
            XWPFParagraph line = (XWPFParagraph) element;
            assertThat(line.getText()).as("nothing but space between the blocks").isEmpty();
            CTSpacing spacing = spacing(line);
            assertThat(spacing.getLineRule()).isEqualTo(STLineSpacingRule.EXACT);
            space += before(line) + DocxTwips.of(spacing.getLine()) + after(line);
        }
        return space + (lower instanceof XWPFParagraph paragraph ? before(paragraph) : 0);
    }

    private static List<IBodyElement> between(XWPFDocument document, String above, String block) {
        List<IBodyElement> body = document.getBodyElements();
        return body.subList(body.indexOf(elementWith(document, above)) + 1, body.indexOf(elementWith(document, block)));
    }

    /** The first body element whose text holds the given text. */
    private static IBodyElement elementWith(XWPFDocument document, String text) {
        return document.getBodyElements().stream()
                .filter(element -> textOf(element).contains(text))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no block holds " + text));
    }

    private static String textOf(IBodyElement element) {
        if (element instanceof XWPFParagraph paragraph) {
            return paragraph.getText();
        }
        // A panel's text is in the table nested in its cell, which the table's own text leaves out.
        return element instanceof XWPFTable table ? table.getCTTbl().xmlText() : "";
    }

    /** An empty line of an exact height kept with the next, its mark a point: a held edge. */
    private static boolean isAHeldLine(IBodyElement element) {
        return element instanceof XWPFParagraph paragraph && paragraph.getText().isEmpty()
               && paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetKeepNext()
               && paragraph.getCTP().getPPr().isSetRPr()
               && paragraph.getCTP().getPPr().getRPr().sizeOfSzArray() > 0;
    }

    private static void lines(PageFlowBuilder page) {
        page.spacing(GAP);
        for (int i = 0; i < 8; i++) {
            page.addParagraph("Line " + i);
        }
    }

    private static void itemsTable(PageFlowBuilder page) {
        page.spacing(GAP);
        page.addTable(t -> {
            t.name("Lines").autoColumns(2);
            for (int i = 0; i < 7; i++) {
                t.row("Item " + i, "100.00");
            }
        });
    }

    private static void closingRow(PageFlowBuilder page) {
        page.addRow(row -> row.name("Closing").margin(new DocumentInsets(EDGE, 0, 0, 0))
                .addParagraph("Notes").addParagraph("Payment"));
    }

    private static CTSpacing spacing(XWPFParagraph paragraph) {
        return paragraph.getCTP().getPPr() == null ? null : paragraph.getCTP().getPPr().getSpacing();
    }

    private static long before(XWPFParagraph paragraph) {
        CTSpacing spacing = spacing(paragraph);
        return spacing == null || !spacing.isSetBefore() ? 0 : DocxTwips.of(spacing.getBefore());
    }

    private static long after(XWPFParagraph paragraph) {
        CTSpacing spacing = spacing(paragraph);
        return spacing == null || !spacing.isSetAfter() ? 0 : DocxTwips.of(spacing.getAfter());
    }
}
