package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.chart.ChartData;
import com.demcha.compose.document.chart.ChartSpec;
import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.ChartNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A chart that is all a table cell draws is written there as its data table, as anywhere else, and
 * none of its drawing is drawn over that table; one beside other drawings in its cell, or in a
 * table composed in a cell, is drawn among the cell's drawings, and its note says so.
 */
class DocxChartInACellTest {

    private static final ChartSpec BARS = ChartSpec.bar().data(ChartData.builder().categories("Q1", "Q2", "Q3")
            .series("Sales", 3.0, 5.0, 4.0).series("Costs", 2.0, 2.5, 3.5).build()).build();
    private static final String TABLE = "exported as its data table — a categories-by-series table in the chart's "
                                        + "own value format — because the drawn chart is layout geometry";
    private static final String DRAWN_TOO = TABLE + "; composed in a table cell where the export does not find it "
                                            + "drawing alone — beside other drawings, or in a table composed in a "
                                            + "cell — its bars, lines and slices are drawn as well, where the page "
                                            + "draws them, over the table and without its text";

    @Test
    void aChartACellIsBuiltFromIsItsDataTableThereAndNothingIsDrawn() throws Exception {
        Consumer<PageFlowBuilder> content = beside(new ChartNode(BARS));
        try (XWPFDocument document = DocxExports.withLayout(500, 400, 30, content)) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(1);
            assertThat(cell.getTables()).as("its data table").hasSize(1);
            assertThat(cell.getTables().get(0).getRow(0).getCell(1).getText()).isEqualTo("Sales");
            assertThat(drawings(document)).as("no bar or grid line drawn over it").isZero();
        }
        DocxExportReport report = laidOut(content);
        assertThat(details(report, "chart")).containsExactly(TABLE);
        assertThat(report.bySubject()).as("nothing drawn to name").doesNotContainKey("cell drawing");
    }

    @Test
    void aChartWithOnlyTextBesideItOrInAWrapperThatPaintsNothingIsNotDrawn() throws Exception {
        for (DocumentNode built : List.of(
                new SectionBuilder().add(new ChartNode(BARS)).build(),
                new SectionBuilder().addParagraph("By quarter").add(new ChartNode(BARS)).build())) {
            Consumer<PageFlowBuilder> content = beside(built);
            try (XWPFDocument document = DocxExports.withLayout(500, 400, 30, content)) {
                assertThat(drawings(document)).as("%s", built).isZero();
            }
            assertThat(details(laidOut(content), "chart")).containsExactly(TABLE);
        }
    }

    @Test
    void aChartBesideAnotherDrawingInItsCellIsDrawnTooAndItsNoteSaysSo() throws Exception {
        Consumer<PageFlowBuilder> content = beside(new SectionBuilder()
                .add(new EllipseBuilder().circle(12).fillColor(DocumentColor.BLACK).build())
                .add(new ChartNode(BARS)).build());
        try (XWPFDocument document = DocxExports.withLayout(500, 400, 30, content)) {
            assertThat(drawings(document)).as("the disc, and its bars and grid lines").isGreaterThan(1);
        }
        assertThat(details(laidOut(content), "chart")).containsExactly(DRAWN_TOO);
    }

    @Test
    void aCellsOwnDrawingBesideAChartsCellIsStillDrawnThoughItReachesIntoIt() throws Exception {
        // A tile in its own cell, and one its margin below zero widens from 20pt to 52pt in a 30pt
        // column, reaching into the chart's cell, which does not hold it.
        for (double right : new double[]{0, -30}) {
            Consumer<PageFlowBuilder> content = page -> page.addTable(t -> t
                    .columns(DocumentTableColumn.fixed(30), DocumentTableColumn.fixed(300))
                    .rowCells(DocumentTableCell.node(new com.demcha.compose.document.dsl.ShapeBuilder().size(20, 20)
                                    .fillColor(DocumentColor.BLACK).margin(new DocumentInsets(0, right, 0, 0)).build()),
                            DocumentTableCell.node(new ChartNode(BARS))));
            try (XWPFDocument document = DocxExports.withLayout(500, 400, 30, content)) {
                assertThat(drawings(document)).as("the tile with a right margin of %spt, and nothing of the chart",
                        right).isEqualTo(1);
            }
            assertThat(details(laidOut(content), "chart")).containsExactly(TABLE);
        }
    }

    @Test
    void twoChartsInACellOrAChartInAPaintedWrapperAreDrawnAndNamed() throws Exception {
        for (DocumentNode built : List.of(
                new SectionBuilder().add(new ChartNode(BARS)).add(new ChartNode(BARS)).build(),
                new SectionBuilder().fillColor(DocumentColor.rgb(240, 240, 240)).add(new ChartNode(BARS)).build())) {
            Consumer<PageFlowBuilder> content = beside(built);
            try (XWPFDocument document = DocxExports.withLayout(500, 400, 30, content)) {
                assertThat(drawings(document)).as("%s", built).isPositive();
            }
            assertThat(details(laidOut(content), "chart")).as("%s", built).isNotEmpty().allMatch(DRAWN_TOO::equals);
        }
    }

    @Test
    void aChartInARepeatedHeaderOrASpanningCellIsNotDrawnOnAnyPage() throws Exception {
        Consumer<PageFlowBuilder> header = page -> page.addTable(t -> {
            t.columns(DocumentTableColumn.fixed(300)).repeatHeader(1)
                    .rowCells(DocumentTableCell.node(new ChartNode(BARS)));
            for (int row = 0; row < 40; row++) {
                t.row("Row " + row);
            }
        });
        try (XWPFDocument document = DocxExports.withLayout(500, 400, 30, header)) {
            assertThat(document.getTables().get(0).getRows()).as("a table over pages").hasSizeGreaterThan(20);
            assertThat(drawings(document)).as("nothing of the chart on any page it repeats on").isZero();
        }
        assertThat(laidOut(header).bySubject()).doesNotContainKey("cell drawing");
        Consumer<PageFlowBuilder> spanning = page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(150), DocumentTableColumn.fixed(150))
                .rowCells(DocumentTableCell.node(new ChartNode(BARS)).colSpan(2))
                .row("Left", "Right"));
        try (XWPFDocument document = DocxExports.withLayout(500, 400, 30, spanning)) {
            assertThat(drawings(document)).as("a chart across two columns").isZero();
        }
        assertThat(details(laidOut(spanning), "chart")).containsExactly(TABLE);
    }

    @Test
    void aChartInATableComposedInACellIsDrawnTooAndItsNoteSaysSo() throws Exception {
        assertThat(details(laidOut(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(420))
                .rowCells(DocumentTableCell.node(new TableBuilder()
                        .columns(DocumentTableColumn.fixed(400))
                        .rowCells(DocumentTableCell.node(new ChartNode(BARS))).build())))), "chart"))
                .containsExactly(DRAWN_TOO);
    }

    @Test
    void aChartInTheFlowOrWithNoLayoutIsItsDataTableAlone() throws Exception {
        assertThat(details(laidOut(page -> page.add(new ChartNode(BARS))), "chart")).containsExactly(TABLE);
        DocxExportReport withoutLayout = DocxExports.reportWithoutLayout(500, 400, 30, beside(new SectionBuilder()
                .add(new EllipseBuilder().circle(12).fillColor(DocumentColor.BLACK).build())
                .add(new ChartNode(BARS)).build()));
        assertThat(details(withoutLayout, "chart")).as("nothing is drawn without a layout").containsExactly(TABLE);
    }

    @Test
    void aChartNotDrawnInOneExportIsNotTakenForOneInTheNext() throws Exception {
        // One backend, two exports of one chart node: alone in a cell, then beside a disc.
        ChartNode chart = new ChartNode(BARS);
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        DocxSemanticBackend backend = new DocxSemanticBackend(report::set);
        export(backend, beside(chart));
        assertThat(details(report.get(), "chart")).containsExactly(TABLE);
        export(backend, beside(new SectionBuilder()
                .add(new EllipseBuilder().circle(12).fillColor(DocumentColor.BLACK).build()).add(chart).build()));
        assertThat(details(report.get(), "chart")).containsExactly(DRAWN_TOO);
    }

    /** A table whose second cell is built from {@code content}, beside a cell of text. */
    private static Consumer<PageFlowBuilder> beside(DocumentNode content) {
        return page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(120), DocumentTableColumn.fixed(300))
                .rowCells(DocumentTableCell.text("Quarterly"), DocumentTableCell.node(content)));
    }

    private static void export(DocxSemanticBackend backend, Consumer<PageFlowBuilder> content) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(500, 400).margin(DocumentInsets.of(30))
                .create()) {
            session.pageFlow(content::accept);
            session.export(backend);
        }
    }

    /** The report of an export the layout stands behind. */
    private static DocxExportReport laidOut(Consumer<PageFlowBuilder> content) throws Exception {
        DocxExportReport report = DocxExports.report(500, 400, 30, content);
        assertThat(report.bySubject()).as("laid out").doesNotContainKey("measured geometry");
        return report;
    }

    private static List<String> details(DocxExportReport report, String subject) {
        return report.bySubject().getOrDefault(subject, List.of()).stream().map(DocxExportReport.Note::detail).toList();
    }

    /** The drawings in the document's body, its tables' cells included, floating or in a line. */
    private static int drawings(XWPFDocument document) {
        String xml = document.getDocument().xmlText();
        return count(xml, "<wp:anchor") + count(xml, "<wp:inline");
    }

    private static int count(String text, String of) {
        int count = 0;
        for (int at = text.indexOf(of); at >= 0; at = text.indexOf(of, at + 1)) {
            count++;
        }
        return count;
    }
}
