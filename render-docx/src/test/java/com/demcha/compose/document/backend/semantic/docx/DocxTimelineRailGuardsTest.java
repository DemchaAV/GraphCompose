package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTPosH;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.STRelFromH;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTOnOff;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.w3c.dom.Element;

import java.awt.Color;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static com.demcha.compose.document.backend.semantic.docx.DocxTimelineRailAlongCellsTest.barInFirstCell;
import static com.demcha.compose.document.backend.semantic.docx.DocxTimelineRailAlongCellsTest.entryTable;
import static com.demcha.compose.document.backend.semantic.docx.DocxTimelineRailAlongCellsTest.gapParagraph;
import static com.demcha.compose.document.backend.semantic.docx.DocxTimelineRailAlongCellsTest.plan;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A timeline's rail is written as its cells' border only where the entries were written as Word can
 * carry it: one-row tables of one grid and one set of properties, plain cells, nothing but exact
 * empty lines between them, and a bar placed from a cell of the first entry. Anything else leaves
 * the timeline exactly as it was written, its rail drawn.
 *
 * <p>Each document here is two entry tables, each a row of a 400- and a 4000-twip cell, an exact
 * empty line between them, and a bar 100 twips into the first entry's second cell, as
 * {@link DocxTimelineRailAlongCellsTest} builds them; one thing is changed per case.</p>
 */
class DocxTimelineRailGuardsTest {

    private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

    @Test
    void theFixtureAsBuiltIsWrittenAlongItsCells() throws Exception {
        // The control: each case below differs from this in the one thing it names.
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable upper = entryTable(document);
            gapParagraph(document);
            XWPFTable lower = entryTable(document);

            assertThat(DocxTimelineRails.apply(plan(upper, lower, 0, 0, barInFirstCell(upper)))).isTrue();
            assertThat(document.getDocument().getBody().sizeOfTblArray()).as("one table").isEqualTo(1);
            assertThat(upper.getCTTbl().sizeOfTrArray()).as("two entries and the gap").isEqualTo(3);
        }
    }

    @Test
    void aRailWithNoBarOrANullOneLeavesTheTimelineAsItWas() throws Exception {
        assertLeftAsItWas((upper, lower, bar) -> withBars(plan(upper, lower, 0, 0, bar), List.of()));
        assertLeftAsItWas((upper, lower, bar) -> withBars(plan(upper, lower, 0, 0, bar),
                Arrays.asList((CTDrawing) null)));
    }

    @Test
    void aGridColumnOfNoWidthLeavesTheTimelineAsItWas() throws Exception {
        assertLeftAsItWas(tables -> tables.forEach(table ->
                table.getCTTbl().getTblGrid().getGridColArray(0).setW(BigInteger.ZERO)));
    }

    @Test
    void anEntryOfTwoRowsLeavesTheTimelineAsItWas() throws Exception {
        assertLeftAsItWas(tables -> tables.get(1).createRow());
    }

    @Test
    void entriesOfDifferentGridsLeaveTheTimelineAsItWas() throws Exception {
        assertLeftAsItWas(tables -> {
            tables.get(1).getCTTbl().getTblGrid().getGridColArray(0).setW(BigInteger.valueOf(500));
            tables.get(1).getCTTbl().getTblGrid().getGridColArray(1).setW(BigInteger.valueOf(3900));
        });
    }

    @Test
    void aCellThatIsNotOneColumnWideInTwipsLeavesTheTimelineAsItWas() throws Exception {
        // Marked as spanning (a span of one: a wider one would leave the row a cell short of its
        // grid first), merged down or across, measured in fiftieths of a percent, of no width, or
        // one cell too many for the grid: the rows would not take the rail's column as written.
        assertLeftAsItWas(tables -> cellProperties(tables.get(1), 0).addNewGridSpan().setVal(BigInteger.ONE));
        assertLeftAsItWas(tables -> cellProperties(tables.get(1), 0).addNewVMerge());
        assertLeftAsItWas(tables -> cellProperties(tables.get(1), 0).addNewHMerge());
        assertLeftAsItWas(tables -> cellProperties(tables.get(1), 0).getTcW().setType(STTblWidth.PCT));
        assertLeftAsItWas(tables -> cellProperties(tables.get(1), 0).getTcW().setW(BigInteger.ZERO));
        assertLeftAsItWas(tables -> {
            // A cell of its own width in twips: only the count tells it from the grid.
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth width =
                    tables.get(1).getRow(0).addNewTableCell().getCTTc().addNewTcPr().addNewTcW();
            width.setType(STTblWidth.DXA);
            width.setW(BigInteger.valueOf(100));
        });
    }

    @Test
    void aRailStartingAboveItsFirstEntryOrEndingBelowItsLastLeavesTheTimelineAsItWas() throws Exception {
        assertLeftAsItWas((upper, lower, bar) -> withEnds(plan(upper, lower, 0, 0, bar), 510, 450));
        assertLeftAsItWas((upper, lower, bar) -> withEnds(plan(upper, lower, 0, 0, bar), 500, 440));
    }

    @Test
    void aRailInsideBothEndsOfItsOnlyEntryLeavesTheTimelineAsItWas() throws Exception {
        // One row cannot be cut at both ends; one end cut, as the rail from its marker, would be.
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable only = entryTable(document);
            CTDrawing bar = barInFirstCell(only);
            String before = document.getDocument().xmlText();
            DocxLayoutMetrics.RailedEntry entry = new DocxLayoutMetrics.RailedEntry(null, 0, 100, 500, 480);

            assertThat(DocxTimelineRails.apply(new DocxTimelineRails.Plan(List.of(only), List.of(entry),
                    495, 0, 485, 0, Color.RED, 1, List.of(bar)))).isFalse();
            assertThat(document.getDocument().xmlText()).isEqualTo(before);
        }
    }

    @Test
    void aBarPlacedPastTheEntrysLastColumnLeavesTheTimelineAsItWas() throws Exception {
        assertLeftAsItWas((upper, lower, bar) -> {
            bar.getAnchorArray(0).getPositionH().setPosOffset(5000 * 635);
            return plan(upper, lower, 0, 0, bar);
        });
    }

    @Test
    void aBarNotAnchoredAtAnOffsetFromItsColumnLeavesTheTimelineAsItWas() throws Exception {
        // Where such a bar stands across the entry is not read: in line, with no position across,
        // aligned rather than offset, or offset from the page.
        assertLeftAsItWasWithBar(cell -> cell.addNewDrawing().addNewInline());
        assertLeftAsItWasWithBar(cell -> cell.addNewDrawing().addNewAnchor());
        assertLeftAsItWasWithBar(cell -> cell.addNewDrawing().addNewAnchor().addNewPositionH()
                .setRelativeFrom(STRelFromH.COLUMN));
        assertLeftAsItWasWithBar(cell -> {
            CTPosH across = cell.addNewDrawing().addNewAnchor().addNewPositionH();
            across.setRelativeFrom(STRelFromH.PAGE);
            across.setPosOffset(100 * 635);
        });
    }

    @Test
    void aRailOnACellsLeftEdgeBordersThatCellInEveryRow() throws Exception {
        // Centred on the second cell's left edge, the rail is that cell's border, and the cell is
        // cut nowhere: the bar's own run is all it holds.
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable upper = entryTable(document);
            gapParagraph(document);
            XWPFTable lower = entryTable(document);
            CTDrawing bar = barOnTheSecondCellsEdge(upper);

            assertThat(DocxTimelineRails.apply(plan(upper, lower, 0, 0, bar))).isTrue();
            assertThat(document.getDocument().getBody().sizeOfTblArray()).isEqualTo(1);
            assertThat(gridOf(upper)).as("no column cut").containsExactly(400, 4000);
            assertThat(upper.getCTTbl().sizeOfTrArray()).isEqualTo(3);
            for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow row : upper.getCTTbl().getTrArray()) {
                CTBorder left = row.getTcArray(1).getTcPr().getTcBorders().getLeft();
                assertThat(left.xgetColor().getStringValue()).isEqualTo("FF0000");
            }
        }
    }

    @Test
    void aCellOnTheRailHoldingAnythingButTheBarLeavesTheTimelineAsItWas() throws Exception {
        // Its border would stand through what it holds: a word, a link or a table.
        assertLeftAsItWasOnTheEdge(upper -> upper.getRow(0).getCell(1).getParagraphs().get(0).createRun()
                .setText("2020"));
        assertLeftAsItWasOnTheEdge(upper -> upper.getRow(0).getCell(1).getParagraphs().get(0).getCTP()
                .addNewHyperlink());
        assertLeftAsItWasOnTheEdge(upper -> upper.getRow(0).getCell(1).getCTTc().addNewTbl());
    }

    @Test
    void entriesWithNothingBetweenThemOrSomethingButALineLeaveTheTimelineAsItWas() throws Exception {
        assertLeftAsItWasBetween(document -> { });
        assertLeftAsItWasBetween(document -> document.getDocument().getBody().addNewBookmarkStart());
        assertLeftAsItWasBetween(document -> {
            gapParagraph(document);
            lastParagraph(document).getCTP().getPPr().addNewPBdr();
        });
        assertLeftAsItWasBetween(document -> {
            gapParagraph(document);
            lastParagraph(document).getCTP().getPPr().addNewShd();
        });
        assertLeftAsItWasBetween(document -> {
            gapParagraph(document);
            lastParagraph(document).getCTP().getPPr().getSpacing().setLineRule(
                    org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.AUTO);
        });
        assertLeftAsItWasBetween(document -> document.createParagraph());
    }

    @Test
    void theGapRowKeepsWithTheNextEntryOnlyWhereTheGapsLineIsKeptOn() throws Exception {
        // An on/off value is on with no value, and off when it says false, 0 or off.
        assertThat(gapRowKept(null)).isFalse();
        assertThat(gapRowKept("")).as("keepNext with no value").isTrue();
        assertThat(gapRowKept("1")).isTrue();
        assertThat(gapRowKept("true")).isTrue();
        assertThat(gapRowKept("false")).isFalse();
        assertThat(gapRowKept("0")).isFalse();
        assertThat(gapRowKept("off")).isFalse();
    }

    // ---- fixtures

    @FunctionalInterface
    private interface PlanOf {
        DocxTimelineRails.Plan of(XWPFTable upper, XWPFTable lower, CTDrawing bar);
    }

    /** The fixture with its tables changed, then its rail asked for: nothing written, nothing changed. */
    private static void assertLeftAsItWas(Consumer<List<XWPFTable>> change) throws Exception {
        assertLeftAsItWas((upper, lower, bar) -> {
            change.accept(List.of(upper, lower));
            return plan(upper, lower, 0, 0, bar);
        });
    }

    private static void assertLeftAsItWas(PlanOf planned) throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable upper = entryTable(document);
            gapParagraph(document);
            XWPFTable lower = entryTable(document);
            DocxTimelineRails.Plan plan = planned.of(upper, lower, barInFirstCell(upper));
            String before = document.getDocument().xmlText();

            assertThat(DocxTimelineRails.apply(plan)).isFalse();
            assertThat(document.getDocument().xmlText()).isEqualTo(before);
        }
    }

    private static void assertLeftAsItWasWithBar(Consumer<org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR> bar)
            throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable upper = entryTable(document);
            gapParagraph(document);
            XWPFTable lower = entryTable(document);
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR run =
                    upper.getRow(0).getCell(1).getParagraphs().get(0).createRun().getCTR();
            bar.accept(run);
            String before = document.getDocument().xmlText();

            assertThat(DocxTimelineRails.apply(plan(upper, lower, 0, 0, run.getDrawingArray(0)))).isFalse();
            assertThat(document.getDocument().xmlText()).isEqualTo(before);
        }
    }

    private static void assertLeftAsItWasOnTheEdge(Consumer<XWPFTable> change) throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable upper = entryTable(document);
            gapParagraph(document);
            XWPFTable lower = entryTable(document);
            CTDrawing bar = barOnTheSecondCellsEdge(upper);
            change.accept(upper);
            String before = document.getDocument().xmlText();

            assertThat(DocxTimelineRails.apply(plan(upper, lower, 0, 0, bar))).isFalse();
            assertThat(document.getDocument().xmlText()).isEqualTo(before);
        }
    }

    private static void assertLeftAsItWasBetween(Consumer<XWPFDocument> between) throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable upper = entryTable(document);
            between.accept(document);
            XWPFTable lower = entryTable(document);
            CTDrawing bar = barInFirstCell(upper);
            String before = document.getDocument().xmlText();

            assertThat(DocxTimelineRails.apply(plan(upper, lower, 0, 0, bar))).isFalse();
            assertThat(document.getDocument().xmlText()).isEqualTo(before);
        }
    }

    /** Whether the row written for the gap keeps with the next, the gap's line kept with a value. */
    private static boolean gapRowKept(String keepNext) throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable upper = entryTable(document);
            gapParagraph(document);
            if (keepNext != null) {
                CTOnOff kept = lastParagraph(document).getCTP().getPPr().addNewKeepNext();
                if (!keepNext.isEmpty()) {
                    ((Element) kept.getDomNode()).setAttributeNS(W, "w:val", keepNext);
                }
            }
            XWPFTable lower = entryTable(document);

            assertThat(DocxTimelineRails.apply(plan(upper, lower, 0, 0, barInFirstCell(upper)))).isTrue();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow gap = upper.getCTTbl().getTrArray(1);
            return Arrays.stream(gap.getTcArray()).flatMap(cell -> Arrays.stream(cell.getPArray()))
                    .allMatch(paragraph -> paragraph.isSetPPr() && paragraph.getPPr().isSetKeepNext());
        }
    }

    /** A 1pt bar centred on the left edge of the first entry's second cell, 400 twips in. */
    private static CTDrawing barOnTheSecondCellsEdge(XWPFTable entry) {
        CTDrawing bar = entry.getRow(0).getCell(1).getParagraphs().get(0).createRun().getCTR().addNewDrawing();
        CTPosH across = bar.addNewAnchor().addNewPositionH();
        across.setRelativeFrom(STRelFromH.COLUMN);
        across.setPosOffset(-10 * 635);
        return bar;
    }

    private static DocxTimelineRails.Plan withBars(DocxTimelineRails.Plan plan, List<CTDrawing> bars) {
        return new DocxTimelineRails.Plan(plan.tables(), plan.entries(), plan.railTop(), plan.topPage(),
                plan.railBottom(), plan.bottomPage(), plan.colour(), plan.width(), bars);
    }

    private static DocxTimelineRails.Plan withEnds(DocxTimelineRails.Plan plan, double top, double bottom) {
        return new DocxTimelineRails.Plan(plan.tables(), plan.entries(), top, plan.topPage(),
                bottom, plan.bottomPage(), plan.colour(), plan.width(), plan.bars());
    }

    private static CTTcPr cellProperties(XWPFTable table, int cell) {
        return table.getRow(0).getCell(cell).getCTTc().getTcPr();
    }

    private static XWPFParagraph lastParagraph(XWPFDocument document) {
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        return paragraphs.get(paragraphs.size() - 1);
    }

    private static List<Integer> gridOf(XWPFTable table) {
        List<Integer> grid = new ArrayList<>();
        table.getCTTbl().getTblGrid().getGridColList()
                .forEach(column -> grid.add(((BigInteger) column.getW()).intValue()));
        return grid;
    }
}
