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
 * A chart that is all a table cell draws is written there as a Word chart, as anywhere else, and
 * none of its drawing is drawn over it; one beside other drawings in its cell, or in a table
 * composed in a cell, is drawn among the cell's drawings, which the export cannot tell its own
 * from, and is written as its data table under them, and its note says so.
 */
class DocxChartInACellTest {

    private static final ChartSpec BARS = ChartSpec.bar().data(ChartData.builder().categories("Q1", "Q2", "Q3")
            .series("Sales", 3.0, 5.0, 4.0).series("Costs", 2.0, 2.5, 3.5).build()).build();
    private static final String WORD_CHART = "written as a Word chart, its numbers in the workbook embedded with it; "
                                             + "Word lays the chart out in its box itself, so the plot area, the "
                                             + "labels and the legend stand where Word puts them rather than where "
                                             + "the page does";
    private static final String DRAWN_TOO = "exported as its data table — a categories-by-series table in the "
                                            + "chart's own value format — and not as a Word chart: composed in a "
                                            + "table cell where the export does not find it drawing alone — beside "
                                            + "other drawings, or in a table composed in a cell — its bars, lines "
                                            + "and slices are drawn where the page draws them, over the table and "
                                            + "without its text, and a Word chart would stand over them";

    @Test
    void aChartACellIsBuiltFromIsAWordChartThereAndNothingIsDrawn() throws Exception {
        Consumer<PageFlowBuilder> content = beside(new ChartNode(BARS));
        try (XWPFDocument document = DocxExports.withLayout(500, 400, 30, content)) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(1);
            assertThat(cell.getTables()).as("no data table").isEmpty();
            assertThat(charts(cell.getCTTc().xmlText())).as("the Word chart, in its cell").isEqualTo(1);
            assertThat(drawings(document)).as("no bar or grid line drawn over it").isZero();
        }
        DocxExportReport report = laidOut(content);
        assertThat(details(report, "chart")).containsExactly(WORD_CHART);
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
                assertThat(charts(document)).as("%s", built).isEqualTo(1);
            }
            assertThat(details(laidOut(content), "chart")).containsExactly(WORD_CHART);
        }
    }

    @Test
    void aChartBesideAnotherDrawingInItsCellIsDrawnOverItsDataTableAndItsNoteSaysSo() throws Exception {
        Consumer<PageFlowBuilder> content = beside(new SectionBuilder()
                .add(new EllipseBuilder().circle(12).fillColor(DocumentColor.BLACK).build())
                .add(new ChartNode(BARS)).build());
        try (XWPFDocument document = DocxExports.withLayout(500, 400, 30, content)) {
            assertThat(drawings(document)).as("the disc, and its bars and grid lines").isGreaterThan(1);
            assertThat(charts(document)).as("no Word chart under the drawn one").isZero();
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(1);
            assertThat(cell.getTables()).as("its data table").hasSize(1);
            assertThat(cell.getTables().get(0).getRow(0).getCell(1).getText()).isEqualTo("Sales");
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
                assertThat(charts(document)).isEqualTo(1);
            }
            assertThat(details(laidOut(content), "chart")).containsExactly(WORD_CHART);
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
                assertThat(charts(document)).as("%s", built).isZero();
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
            assertThat(charts(document)).as("one Word chart, which Word repeats with its row").isEqualTo(1);
        }
        assertThat(laidOut(header).bySubject()).doesNotContainKey("cell drawing");
        Consumer<PageFlowBuilder> spanning = page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(150), DocumentTableColumn.fixed(150))
                .rowCells(DocumentTableCell.node(new ChartNode(BARS)).colSpan(2))
                .row("Left", "Right"));
        try (XWPFDocument document = DocxExports.withLayout(500, 400, 30, spanning)) {
            assertThat(drawings(document)).as("a chart across two columns").isZero();
            assertThat(charts(document)).isEqualTo(1);
        }
        assertThat(details(laidOut(spanning), "chart")).containsExactly(WORD_CHART);
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
    void aChartInTheFlowOrWithNoLayoutIsAWordChart() throws Exception {
        assertThat(details(laidOut(page -> page.add(new ChartNode(BARS))), "chart")).containsExactly(WORD_CHART);
        Consumer<PageFlowBuilder> besideADisc = beside(new SectionBuilder()
                .add(new EllipseBuilder().circle(12).fillColor(DocumentColor.BLACK).build())
                .add(new ChartNode(BARS)).build());
        try (XWPFDocument document = DocxExports.withoutLayout(500, 400, 30, besideADisc)) {
            assertThat(charts(document)).as("nothing is drawn without a layout, so nothing stands over it")
                    .isEqualTo(1);
            assertThat(document.getTables().get(0).getRow(0).getCell(1).getTables()).isEmpty();
        }
        assertThat(details(DocxExports.reportWithoutLayout(500, 400, 30, besideADisc), "chart"))
                .singleElement().asString().startsWith("written as a Word chart");
    }

    @Test
    void aChartNotDrawnInOneExportIsNotTakenForOneInTheNext() throws Exception {
        // One backend, two exports of one chart node: alone in a cell, then beside a disc.
        ChartNode chart = new ChartNode(BARS);
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        DocxSemanticBackend backend = new DocxSemanticBackend(report::set);
        export(backend, beside(chart));
        assertThat(details(report.get(), "chart")).containsExactly(WORD_CHART);
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

    /** The drawings in the document's body, its tables' cells included, floating or in a line, but its Word charts. */
    private static int drawings(XWPFDocument document) {
        String xml = document.getDocument().xmlText();
        return count(xml, "<wp:anchor") + count(xml, "<wp:inline") - charts(xml);
    }

    /** The Word charts the document's body holds. */
    private static int charts(XWPFDocument document) {
        return charts(document.getDocument().xmlText());
    }

    private static int charts(String xml) {
        return count(xml, "uri=\"http://schemas.openxmlformats.org/drawingml/2006/chart\"");
    }

    private static int count(String text, String of) {
        int count = 0;
        for (int at = text.indexOf(of); at >= 0; at = text.indexOf(of, at + 1)) {
            count++;
        }
        return count;
    }
}
