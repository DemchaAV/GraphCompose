package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.node.RowArrangement;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The one-row table a {@code row(...)} is carried as.
 *
 * <p>Two things are pinned here. That the carrier's own grid is turned off <em>once</em>:
 * POI's {@code createTable} already writes a full set of single-line borders, and adding a
 * second element per edge on top of them leaves a {@code w:tblBorders} that
 * {@code CT_TblBorders} does not allow — Word reads the last one and draws nothing, so the
 * render looked correct while the part was invalid.</p>
 *
 * <p>And that the columns land where the fixed-layout render puts them. A row's slots are
 * arithmetic on the width it is offered, not a measurement, for every distribution but
 * two: weights, an even split and fixed columns are all shares of what is left after the
 * gaps, while an {@code auto} column and the flex path ask what a child's content
 * naturally measures. The first three are written; the other two stay Word's.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxRowLayoutTest {

    /** A5-ish page with a 20pt margin: 360pt of content width. */
    private static final double CONTENT_WIDTH = 360;
    private static final double TWIPS_PER_POINT = 20.0;

    @Test
    void theCarriersGridIsTurnedOffWithOneElementPerEdge() throws Exception {
        XWPFTable table = onlyTable(page -> page.addRow(r -> r
                .addParagraph(p -> p.text("Left"))
                .addParagraph(p -> p.text("Right"))));

        String borders = table.getCTTbl().getTblPr().getTblBorders().xmlText();
        for (String edge : new String[] {"top", "bottom", "left", "right", "insideH", "insideV"}) {
            assertThat(occurrences(borders, "<w:" + edge + " "))
                    .as("one w:%s, not POI's default plus ours", edge)
                    .isEqualTo(1);
        }
        assertThat(borders)
                .as("and the one that survives is the one that hides the grid")
                .doesNotContain("single");
    }

    @Test
    void theRowsVerticalAlignmentIsEveryCellsAndTheTopIsLeftToWord() throws Exception {
        // A table of contents aligns its entries to the bottom so the leader, a line a point
        // tall, sits on the text's baseline; in a cell left at Word's top it rode at the top.
        for (var align : com.demcha.compose.document.node.RowVerticalAlign.values()) {
            XWPFTable table = onlyTable(page -> page.addRow(r -> r
                    .verticalAlign(align)
                    .addParagraph(p -> p.text("Intro"))
                    .addLine(line -> line.horizontal(100))));

            for (var cell : table.getRow(0).getTableCells()) {
                assertThat(cell.getVerticalAlignment())
                        .as("%s", align)
                        .isEqualTo(switch (align) {
                            case TOP -> null;
                            case CENTER -> org.apache.poi.xwpf.usermodel.XWPFTableCell.XWPFVertAlign.CENTER;
                            case BOTTOM -> org.apache.poi.xwpf.usermodel.XWPFTableCell.XWPFVertAlign.BOTTOM;
                        });
            }
        }
    }

    @Test
    void twoChildrenWithNothingStatedSplitTheWidthEvenly() throws Exception {
        // measureRow gives each child slotsTotal / n when there are neither columns nor
        // weights, whatever the children contain. Word's autofit instead sizes them to
        // their text, which is how a short label beside a long one ended up with a third
        // of the width the PDF gives it.
        XWPFTable table = onlyTable(page -> page.addRow(r -> r
                .addParagraph(p -> p.text("Left"))
                .addParagraph(p -> p.text("A much longer right-hand column of text"))));

        assertThat(gridTwips(table)).containsExactly(3600L, 3600L);
        assertThat(layoutType(table))
                .as("without this Word re-fits the columns to their content and the grid "
                    + "is only a suggestion")
                .isEqualTo("fixed");
    }

    @Test
    void weightsAndTheGapDecideTheColumnsAndTheCellMargins() throws Exception {
        // 360 of content, less a 20pt gap, is 340 to share 3:2 — so 204 and 136. The gap
        // is not a column and Word has nowhere to put it, so it rides in the first
        // column's width and comes back out as that cell's right margin: the text box is
        // the slot, and the second column starts exactly where the second slot does.
        XWPFTable table = onlyTable(page -> page.addRow(r -> r
                .spacing(20)
                .weights(3, 2)
                .addParagraph(p -> p.text("Scope"))
                .addParagraph(p -> p.text("Period"))));

        assertThat(gridTwips(table)).containsExactly(4480L, 2720L);
        assertThat(sum(gridTwips(table)))
                .as("the columns still add up to the table")
                .isEqualTo(Math.round(CONTENT_WIDTH * TWIPS_PER_POINT));
        assertThat(marginTwips(table, 0)).containsExactly(0L, 400L);
        assertThat(marginTwips(table, 1))
                .as("written even though they are zero — Word's own default is not")
                .containsExactly(0L, 0L);
    }

    @Test
    void aFixedColumnTakesItsWidthAndAWeightTakesTheRest() throws Exception {
        XWPFTable table = onlyTable(page -> page.addRow(r -> r
                .columns(DocumentRowColumn.fixed(100), DocumentRowColumn.weight(1))
                .addParagraph(p -> p.text("Label"))
                .addParagraph(p -> p.text("Value"))));

        assertThat(gridTwips(table)).containsExactly(2000L, 5200L);
    }

    @Test
    void theRowsPaddingRidesInTheOuterColumnsAndComesBackOutAsAMargin() throws Exception {
        // 12pt each side leaves 336 to halve. The padding is part of the table, which
        // spans the content width, so it lives in the first and last columns and is taken
        // off again by their margins.
        XWPFTable table = onlyTable(page -> page.addRow(r -> r
                .padding(DocumentInsets.symmetric(0, 12))
                .addParagraph(p -> p.text("Left"))
                .addParagraph(p -> p.text("Right"))));

        assertThat(gridTwips(table)).containsExactly(3600L, 3600L);
        assertThat(marginTwips(table, 0)).containsExactly(240L, 0L);
        assertThat(marginTwips(table, 1)).containsExactly(0L, 240L);
    }

    @Test
    void anAutoColumnLeavesTheWholeSplitToWord() throws Exception {
        // An auto column is as wide as its content needs, and needing content widths is
        // exactly what this backend has no font runtime for.
        XWPFTable table = onlyTable(page -> page.addRow(r -> r
                .columns(DocumentRowColumn.fixed(100), DocumentRowColumn.auto())
                .addParagraph(p -> p.text("Label"))
                .addParagraph(p -> p.text("Value"))));

        assertThat(gridTwips(table)).as("no grid at all").isEmpty();
        assertThat(layoutType(table)).as("and Word keeps fitting the columns").isNull();
    }

    @Test
    void anArrangementThatJustifiesTheChildrenLeavesTheSplitToWord() throws Exception {
        // The flex path gives every child without a grow factor its natural width and
        // spreads the leftover — both halves of that are measurements.
        XWPFTable table = onlyTable(page -> page.addRow(r -> r
                .arrangement(RowArrangement.SPACE_BETWEEN)
                .addParagraph(p -> p.text("Left"))
                .addParagraph(p -> p.text("Right"))));

        assertThat(gridTwips(table)).isEmpty();
        assertThat(layoutType(table)).isNull();
    }

    @Test
    void theMeasuredSplitAgreesWithTheStatedOneWhereBothCanAnswer() throws Exception {
        // Weights are arithmetic either way, so the two paths have to arrive at the same
        // columns — if they ever part, one of them is reading the row wrong.
        Consumer<PageFlowBuilder> weighted = page -> page.addRow(r -> r
                .spacing(20)
                .weights(3, 2)
                .addParagraph(p -> p.text("Scope"))
                .addParagraph(p -> p.text("Period")));

        assertThat(gridTwips(measuredTable(weighted)))
                .isEqualTo(gridTwips(onlyTable(weighted)))
                .containsExactly(4480L, 2720L);
    }

    @Test
    void theLayoutAnswersTheSplitTheRowCannotStateItself() throws Exception {
        // The case the stated path declines: an auto column is as wide as its content, and
        // the layout is where that width was worked out.
        Consumer<PageFlowBuilder> mixed = page -> page.addRow(r -> r
                .columns(DocumentRowColumn.fixed(100), DocumentRowColumn.auto())
                .addParagraph(p -> p.text("Label"))
                .addParagraph(p -> p.text("Value")));

        assertThat(gridTwips(onlyTable(mixed))).as("nothing to write").isEmpty();
        List<Long> measured = gridTwips(measuredTable(mixed));
        assertThat(measured).hasSize(2);
        assertThat(measured.get(0)).as("the fixed column, exactly").isEqualTo(2000L);
        assertThat(sum(measured))
                .as("and the pair still tiles the row")
                .isEqualTo(Math.round(CONTENT_WIDTH * TWIPS_PER_POINT));
    }

    /** Grid column widths in twips, empty when the export wrote no grid. */
    private static List<Long> gridTwips(XWPFTable table) {
        CTTblGrid grid = table.getCTTbl().getTblGrid();
        if (grid == null) {
            return List.of();
        }
        return grid.getGridColList().stream().map(column -> twips(column.getW())).toList();
    }

    /** The left and right margin of one cell, in twips. */
    private static List<Long> marginTwips(XWPFTable table, int index) {
        CTTcMar margins = table.getRow(0).getCell(index).getCTTc().getTcPr().getTcMar();
        return List.of(twips(margins.getLeft().getW()), twips(margins.getRight().getW()));
    }

    private static String layoutType(XWPFTable table) {
        CTTblPr properties = table.getCTTbl().getTblPr();
        return properties == null || !properties.isSetTblLayout()
                ? null
                : properties.getTblLayout().getType().toString();
    }

    private static long sum(List<Long> values) {
        return values.stream().mapToLong(Long::longValue).sum();
    }

    /** {@code ST_TwipsMeasure} is an xmlbeans union, so the accessor is typed Object. */
    private static long twips(Object measure) {
        return DocxTwips.of(measure);
    }

    private static int occurrences(String haystack, String needle) {
        int count = 0;
        for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + 1)) {
            count++;
        }
        return count;
    }

    /** The carrier as written with nothing measured behind it. */
    private static XWPFTable onlyTable(Consumer<PageFlowBuilder> content) throws Exception {
        try (XWPFDocument document = DocxExports.withoutLayout(400, 600, 20, content)) {
            assertThat(document.getTables()).hasSize(1);
            return document.getTables().get(0);
        }
    }

    /** The carrier as written when the compiled layout is there to read. */
    private static XWPFTable measuredTable(Consumer<PageFlowBuilder> content) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, content)) {
            assertThat(document.getTables()).hasSize(1);
            return document.getTables().get(0);
        }
    }
}
