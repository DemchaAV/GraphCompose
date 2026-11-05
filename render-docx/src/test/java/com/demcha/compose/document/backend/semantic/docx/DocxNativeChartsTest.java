package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.chart.AxisSpec;
import com.demcha.compose.document.chart.BarGrouping;
import com.demcha.compose.document.chart.ChartData;
import com.demcha.compose.document.chart.ChartSpec;
import com.demcha.compose.document.chart.ChartStyle;
import com.demcha.compose.document.chart.LegendPosition;
import com.demcha.compose.document.chart.LineInterpolation;
import com.demcha.compose.document.chart.NumberFormatSpec;
import com.demcha.compose.document.chart.PointMarker;
import com.demcha.compose.document.chart.SliceLabelMode;
import com.demcha.compose.document.chart.ValueLabelMode;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.ChartNode;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A chart is written as a Word chart: a chart part drawn inline at the size the page draws the
 * chart, built from what the page's chart is built from, its numbers in an embedded workbook, and
 * what Word's chart has no setting for named in the chart's note.
 */
class DocxNativeChartsTest {

    private static final String CHART_TYPE = "application/vnd.openxmlformats-officedocument.drawingml.chart+xml";
    private static final String SHAPES_TYPE = "application/vnd.openxmlformats-officedocument.drawingml.chartshapes+xml";

    private static final ChartData QUARTERS = ChartData.builder().categories("Q1", "Q2", "Q3", "Q4")
            .series("Sales", 120, 180, 150, 210).series("Costs", 90, 110, 130, 120).build();
    private static final ChartData SWING = ChartData.builder().categories("Jan", "Feb", "Mar", "Apr")
            .series("Visits", 10, 12, 40, 15).build();
    private static final ChartData SHARES = ChartData.builder().categories("North", "South", "East", "West")
            .series("Share", 45, 25, 20, 10).build();

    @Test
    void aBarChartCarriesThePagesScaleSeriesColoursLabelsAndLegend() throws Exception {
        ChartNode node = new ChartNode("", ChartSpec.bar().data(QUARTERS).legend(LegendPosition.BOTTOM)
                .valueLabels(ValueLabelMode.OUTSIDE).build(), null, DocumentInsets.zero(), DocumentInsets.of(10));
        Consumer<PageFlowBuilder> content = page -> page.add(node);
        String chart = chartXml(content);

        assertThat(chart).contains("<c:barDir val=\"col\"/>", "<c:grouping val=\"clustered\"/>",
                "<c:legendPos val=\"b\"/>", "<c:dLblPos val=\"outEnd\"/>", "<c:autoTitleDeleted val=\"1\"/>");
        assertThat(count(chart, "<c:ser>")).isEqualTo(2);
        // The page's scale, 0 to 300 by 100, which it draws its ticks at; an integer axis has no decimals.
        assertThat(chart).contains("<c:max val=\"300\"/>", "<c:min val=\"0\"/>", "<c:majorUnit val=\"100\"/>",
                "<c:numFmt formatCode=\"#,##0\" sourceLinked=\"0\"/>");
        assertThat(chart).as("the default palette's first two colours")
                .contains("<a:srgbClr val=\"4E79A7\"/>", "<a:srgbClr val=\"F28E2B\"/>");
        assertThat(chart).contains("<c:v>Q1</c:v>", "<c:v>Sales</c:v>", "<c:v>210</c:v>");

        // The drawing is the size the page draws the chart, its padding taken off the box.
        PlacedNode placed = placement(content, node);
        String body = bodyXml(content);
        assertThat(body).contains("<wp:extent cx=\"" + org.apache.poi.util.Units.toEMU(placed.placementWidth() - 20)
                                  + "\" cy=\"" + org.apache.poi.util.Units.toEMU(placed.placementHeight() - 20) + "\"/>");
        assertThat(note(content)).isEqualTo("written as a Word chart, its numbers in the workbook embedded with it; "
                + "Word lays the chart out in its box itself, so the plot area, the labels and the legend stand where "
                + "Word puts them rather than where the page does; its left padding is not in the file");
    }

    @Test
    void aHorizontalStackedBarRunsFromTheTopAndNamesTheTotalsItDoesNotWrite() throws Exception {
        Consumer<PageFlowBuilder> content = page -> page.add(new ChartNode(ChartSpec.bar().data(QUARTERS)
                .horizontal(true).grouping(BarGrouping.STACKED).valueLabels(ValueLabelMode.OUTSIDE).build()));
        String chart = chartXml(content);

        assertThat(chart).contains("<c:barDir val=\"bar\"/>", "<c:grouping val=\"stacked\"/>",
                "<c:overlap val=\"100\"/>", "<c:orientation val=\"maxMin\"/>", "<c:crosses val=\"max\"/>");
        assertThat(chart).as("a stack's sums, 330 at most").contains("<c:max val=\"400\"/>");
        assertThat(chart).as("no label on its parts").doesNotContain("<c:dLbls>");
        assertThat(note(content)).contains("its stack totals are not written");
    }

    @Test
    void aMonotoneLineIsWrittenStraightAndASmoothOneSmoothed() throws Exception {
        String monotone = chartXml(line(LineInterpolation.MONOTONE, false));
        assertThat(monotone).contains("<c:smooth val=\"0\"/>").doesNotContain("<c:smooth val=\"1\"/>");
        assertThat(note(line(LineInterpolation.MONOTONE, false)))
                .contains("its monotone curve is written as straight segments between its points");

        String smooth = chartXml(line(LineInterpolation.SMOOTH, false));
        assertThat(smooth).contains("<c:smooth val=\"1\"/>");
        assertThat(note(line(LineInterpolation.SMOOTH, false))).doesNotContain("monotone").doesNotContain("wedges");
        assertThat(chartXml(line(LineInterpolation.LINEAR, false))).contains("<c:smooth val=\"0\"/>");
    }

    @Test
    void aLineWithAnAreaIsAnAreaUnderItsLineListedOnceAndItsWedgesNamedWhereItIsSmoothed() throws Exception {
        String chart = chartXml(line(LineInterpolation.SMOOTH, true));
        assertThat(chart).contains("<c:areaChart>", "<c:lineChart>");
        assertThat(chart).as("the fill at the page's opacity, 35% to the nearest of 255 steps")
                .contains("<a:alpha val=\"34902\"/>");
        // Word will not open a chart whose series share an index across its plots.
        assertThat(count(chart, "<c:idx val=\"0\"/><c:order val=\"0\"/>")).as("the area's series").isEqualTo(1);
        assertThat(count(chart, "<c:idx val=\"1\"/><c:order val=\"1\"/>")).as("the line's, after it").isEqualTo(1);
        assertThat(chart).as("the area's entry taken out of the legend")
                .contains("<c:legendEntry><c:idx val=\"0\"/><c:delete val=\"1\"/></c:legendEntry>");
        // A translucent series colour: the page sets the area's alpha to the opacity, not a share of the colour's.
        ChartStyle halfSeen = ChartStyle.builder().palette(com.demcha.compose.document.style.DocumentPaint.solid(
                com.demcha.compose.document.style.DocumentColor.rgba(78, 121, 167, 128))).build();
        assertThat(chartXml(page -> page.add(new ChartNode("", ChartSpec.line().data(SWING).area(true).build(),
                halfSeen, null, null)))).as("the area's fill").contains("<a:alpha val=\"34902\"/>");
        assertThat(note(line(LineInterpolation.SMOOTH, true))).contains("the fill stands off the curve in wedges");
        assertThat(note(line(LineInterpolation.LINEAR, true))).doesNotContain("wedges");
    }

    @Test
    void aLinesMarkersAreRoundAndAnOvalOneIsNamed() throws Exception {
        Consumer<PageFlowBuilder> round = page -> page.add(new ChartNode("", ChartSpec.line().data(SWING).build(),
                ChartStyle.builder().pointMarker(PointMarker.circle(6)).build(), null, null));
        assertThat(chartXml(round)).contains("<c:symbol val=\"circle\"/><c:size val=\"6\"/>");
        Consumer<PageFlowBuilder> oval = page -> page.add(new ChartNode("", ChartSpec.line().data(SWING).build(),
                ChartStyle.builder().pointMarker(PointMarker.ellipse(8, 4)).build(), null, null));
        assertThat(note(oval)).contains("its point markers are written round");
        assertThat(chartXml(page -> page.add(new ChartNode(ChartSpec.line().data(SWING).build()))))
                .contains("<c:symbol val=\"none\"/>");
    }

    @Test
    void aPieStartsWhereThePageStartsItAndListsOnlyTheSlicesItDraws() throws Exception {
        assertThat(chartXml(pie(SHARES, 90))).contains("<c:firstSliceAng val=\"0\"/>", "<c:pieChart>");
        assertThat(chartXml(pie(SHARES, 0))).contains("<c:firstSliceAng val=\"90\"/>");
        assertThat(chartXml(pie(SHARES, 180))).contains("<c:firstSliceAng val=\"270\"/>");

        ChartData withNone = ChartData.builder().categories("North", "South", "East")
                .series("Share", 45, 0, 20).build();
        String chart = chartXml(page -> page.add(new ChartNode(ChartSpec.pie().data(withNone)
                .legend(LegendPosition.RIGHT).sliceLabels(SliceLabelMode.PERCENT).build())));
        assertThat(chart).as("a slice a colour of its own").contains("<c:dPt><c:idx val=\"2\"/>");
        assertThat(chart).as("no slice for nothing").doesNotContain("<c:pt idx=\"1\"><c:v>0</c:v>");
        assertThat(chart).contains("<c:legendEntry><c:idx val=\"1\"/><c:delete val=\"1\"/></c:legendEntry>");
        // 45 and 20 of 65: 69.2% and 30.8%, neither whole, so the format's optional decimal as it is.
        assertThat(chart).contains("<c:numFmt formatCode=\"#,##0.#%\" sourceLinked=\"0\"/>",
                "<c:showPercent val=\"1\"/>", "<c:dLblPos val=\"outEnd\"/>");
    }

    @Test
    void aDonutsCentreTextIsATextBoxOfTheChartsOwnOverTheHoleWhereThePageDrawsTheDisc() throws Exception {
        Consumer<PageFlowBuilder> content = page -> page.add(new ChartNode(ChartSpec.pie().data(SHARES)
                .donutRatio(0.6).centerText("100%").sliceLabels(SliceLabelMode.CATEGORY_PERCENT)
                .legend(LegendPosition.BOTTOM).build()));
        String chart = chartXml(content);
        assertThat(chart).contains("<c:doughnutChart>", "<c:holeSize val=\"60\"/>", "<c:userShapes r:id=\"rId2\"/>");

        double[] disc = numbers(chart, "<c:x val=\"([0-9.]+)\"/><c:y val=\"([0-9.]+)\"/><c:w val=\"([0-9.]+)\"/>"
                                       + "<c:h val=\"([0-9.]+)\"/>");
        String shapes = part(content, SHAPES_TYPE);
        assertThat(shapes).contains("<a:t>100%</a:t>", "anchor=\"ctr\"", "<a:pPr algn=\"ctr\"/>",
                "sz=\"1300\" b=\"1\"");
        double[] from = numbers(shapes, "<cdr:from><cdr:x>([0-9.]+)</cdr:x><cdr:y>([0-9.]+)</cdr:y></cdr:from>");
        double[] to = numbers(shapes, "<cdr:to><cdr:x>([0-9.]+)</cdr:x><cdr:y>([0-9.]+)</cdr:y></cdr:to>");
        assertThat((from[0] + to[0]) / 2).as("centred on the disc").isCloseTo(disc[0] + disc[2] / 2, within(1e-4));
        assertThat((from[1] + to[1]) / 2).isCloseTo(disc[1] + disc[3] / 2, within(1e-4));
        assertThat(to[0] - from[0]).as("as wide as the hole").isCloseTo(0.6 * disc[2], within(1e-4));
        assertThat(note(content)).contains("round its disc, which stands where the page draws it")
                .contains("its slice labels are written on the ring").doesNotContain("hole is set in the middle");

        assertThat(DocxExports.reportWithoutLayout(500, 500, 30, content).bySubject().get("chart").get(0).detail())
                .contains("the layout gives no place of its disc here");
    }

    @Test
    void optionalDecimalsAreWrittenAsTheyAreUnlessAWholeNumberWouldKeepItsPoint() throws Exception {
        // Ticks 0, 0.5 … 2: "#,##0.##" in Excel would show 1 as "1.", so one fixed decimal, named.
        ChartData small = ChartData.builder().categories("A", "B").series("S", 0.5, 2.0).build();
        Consumer<PageFlowBuilder> mixed = page -> page.add(new ChartNode(ChartSpec.bar().data(small).build()));
        assertThat(chartXml(mixed)).contains("<c:numFmt formatCode=\"#,##0.0\" sourceLinked=\"0\"/>");
        assertThat(note(mixed)).contains("its numbers are shown with 1 decimal each, where the page shows a whole "
                                         + "number without one");
        // Whole ticks: no decimals and nothing to name.
        assertThat(note(page -> page.add(new ChartNode(ChartSpec.bar().data(QUARTERS).build()))))
                .doesNotContain("decimal");
        // A decimal that must show keeps its point, so the pattern is written as it is.
        AxisSpec fixed = AxisSpec.builder().format(NumberFormatSpec.pattern("0.0#")).build();
        Consumer<PageFlowBuilder> kept = page -> page.add(new ChartNode(ChartSpec.bar().data(small).valueAxis(fixed).build()));
        assertThat(chartXml(kept)).contains("<c:numFmt formatCode=\"0.0#\" sourceLinked=\"0\"/>");
        assertThat(note(kept)).doesNotContain("decimal");
        // Labels of fractional values only: the pattern as it is.
        ChartData fractions = ChartData.builder().categories("A", "B").series("S", 1.5, 2.25).build();
        assertThat(chartXml(page -> page.add(new ChartNode(ChartSpec.bar().data(fractions)
                .valueLabels(ValueLabelMode.OUTSIDE).build()))))
                .as("the labels'").contains("<c:dLbls><c:numFmt formatCode=\"#,##0.##\" sourceLinked=\"0\"/>");
        AxisSpec dollars = AxisSpec.builder().format(NumberFormatSpec.pattern("#,##0").withPrefix("$")).build();
        assertThat(chartXml(page -> page.add(new ChartNode(ChartSpec.bar().data(QUARTERS).valueAxis(dollars).build()))))
                .contains("formatCode=\"&quot;$&quot;#,##0\"");
    }

    @Test
    void aNumberFormatExcelCannotHoldIsNamedOnceForTheAxisAndEveryLabelledSeries() throws Exception {
        Map<NumberFormatSpec, String> phrases = Map.of(
                NumberFormatSpec.pattern("0.##E0"), "its number format \"0.##E0\" is not written",
                NumberFormatSpec.defaults().scaledBy(1000), "its numbers are shown unscaled",
                NumberFormatSpec.defaults().withLocale(Locale.GERMANY), "its number format's separators are not written",
                NumberFormatSpec.pattern("#,#0"), "its digits are grouped in thousands");
        for (Map.Entry<NumberFormatSpec, String> format : phrases.entrySet()) {
            String note = note(page -> page.add(new ChartNode(ChartSpec.bar().data(QUARTERS)
                    .valueLabels(ValueLabelMode.OUTSIDE).valueAxis(AxisSpec.builder().format(format.getKey()).build())
                    .build())));
            assertThat(count(note, format.getValue())).as("%s", format.getKey()).isEqualTo(1);
        }
        assertThat(note(page -> page.add(new ChartNode(ChartSpec.bar().data(QUARTERS)
                .valueAxis(AxisSpec.builder().format(NumberFormatSpec.defaults().withLocale(Locale.US)).build()).build()))))
                .as("the root's separators").doesNotContain("separators");
    }

    @Test
    void barsTooManyForWordsWidestGapAreNamed() throws Exception {
        ChartData.Builder twelve = ChartData.builder().categories("A", "B");
        for (int s = 0; s < 12; s++) {
            twelve.series("S" + s, 1 + s, 2 + s);
        }
        Consumer<PageFlowBuilder> content = page -> page.add(new ChartNode(ChartSpec.bar().data(twelve.build()).build()));
        assertThat(chartXml(content)).contains("<c:gapWidth val=\"500\"/>");
        assertThat(note(content)).contains("its bars are drawn wider than the page's");
        assertThat(note(page -> page.add(new ChartNode(ChartSpec.bar().data(QUARTERS).build()))))
                .doesNotContain("wider");
    }

    @Test
    void anAreaWithAMissingValueNamesLibreOfficesFillAndAStacksFloorIsTheFramesOwn() throws Exception {
        ChartData gap = new ChartData(List.of("Jan", "Feb", "Mar"),
                List.of(new ChartData.Series("Visits", java.util.Arrays.asList(10.0, null, 40.0))));
        assertThat(note(page -> page.add(new ChartNode(ChartSpec.line().data(gap).area(true).build()))))
                .contains("LibreOffice fills its area down to the axis at a missing value");
        assertThat(note(line(LineInterpolation.LINEAR, true))).doesNotContain("missing value");

        // The page holds a vertical stack's floor at zero, and the horizontal frame does not.
        AxisSpec from150 = AxisSpec.builder().min(150).build();
        assertThat(chartXml(page -> page.add(new ChartNode(ChartSpec.bar().data(QUARTERS)
                .grouping(BarGrouping.STACKED).valueAxis(from150).build())))).contains("<c:min val=\"0\"/>");
        assertThat(chartXml(page -> page.add(new ChartNode(ChartSpec.bar().data(QUARTERS).horizontal(true)
                .grouping(BarGrouping.STACKED).valueAxis(from150).build())))).contains("<c:min val=\"150\"/>");
    }

    @Test
    void aPiesDiscIsPinnedToTheBoxItsSlicesAreDrawnIn() throws Exception {
        // Padding unequal top and bottom, a legend below: the disc stands off the box's middle.
        ChartNode node = new ChartNode("", ChartSpec.pie().data(SHARES).legend(LegendPosition.BOTTOM)
                .size(com.demcha.compose.document.chart.ChartSize.aspectRatio(16, 10)).build(), null,
                DocumentInsets.zero(), new DocumentInsets(30, 10, 5, 20));
        Consumer<PageFlowBuilder> content = page -> page.add(node);
        double[] pinned = numbers(chartXml(content), "<c:x val=\"([0-9.]+)\"/><c:y val=\"([0-9.]+)\"/>"
                                                     + "<c:w val=\"([0-9.]+)\"/><c:h val=\"([0-9.]+)\"/>");
        try (DocumentSession session = GraphCompose.document().pageSize(500, 500).margin(DocumentInsets.of(30))
                .create()) {
            session.pageFlow(content::accept);
            var graph = session.layoutGraph();
            PlacedNode chart = graph.nodes().stream().filter(n -> n.path().contains("Chart")).findFirst().orElseThrow();
            List<com.demcha.compose.document.layout.PlacedFragment> slices = graph.fragments().stream()
                    .filter(f -> f.path().equals(chart.path()))
                    .filter(f -> f.payload() instanceof com.demcha.compose.document.layout.payloads.PolygonFragmentPayload)
                    .toList();
            assertThat(slices).hasSize(4);
            double width = chart.placementWidth() - 30;
            double height = chart.placementHeight() - 35;
            // The page counts up from its foot; the box's top is its top less the padding above.
            double boxLeft = chart.placementX() + 20;
            double boxTop = chart.placementY() + chart.placementHeight() - 30;
            com.demcha.compose.document.layout.PlacedFragment disc = slices.get(0);
            assertThat(pinned[0]).isCloseTo((disc.x() - boxLeft) / width, within(1e-4));
            assertThat(pinned[1]).isCloseTo((boxTop - (disc.y() + disc.height())) / height, within(1e-4));
            assertThat(pinned[2]).isCloseTo(disc.width() / width, within(1e-4));
            assertThat(pinned[3]).isCloseTo(disc.height() / height, within(1e-4));
        }
    }

    @Test
    void theChartsNumbersAreInTheWorkbookWithItAndADeterministicExportIsTheSameTwice() throws Exception {
        Consumer<PageFlowBuilder> content = page -> page.add(new ChartNode(ChartSpec.pie().data(SHARES)
                .donutRatio(0.5).centerText("KPI").build()));
        java.time.Instant pinned = java.time.Instant.parse("2020-02-02T10:20:30Z");
        byte[] first = export(content, DocxSemanticBackend.builder().deterministic(pinned).build());
        byte[] second = export(content, DocxSemanticBackend.builder().deterministic(pinned).build());
        assertThat(first).isEqualTo(second);
        // Two exports a second apart can match by chance; the workbook's own clocks are the proof.
        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(first))) {
            PackagePart embedded = document.getPackage().getPartsByContentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").get(0);
            try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(embedded.getInputStream())) {
                int entries = 0;
                for (java.util.zip.ZipEntry entry; (entry = zip.getNextEntry()) != null; entries++) {
                    assertThat(entry.getTimeLocal()).as(entry.getName())
                            .isEqualTo(java.time.LocalDateTime.of(2020, 2, 2, 10, 20, 30));
                    if (entry.getName().equals("docProps/core.xml")) {
                        assertThat(new String(zip.readAllBytes(), StandardCharsets.UTF_8))
                                .contains("2020-02-02T10:20:30Z");
                    }
                }
                assertThat(entries).isPositive();
            }
        }
        // Without deterministic output the workbook is pinned all the same, to the backend's default instant.
        try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(
                new java.io.ByteArrayInputStream(workbookBytes(export(content, new DocxSemanticBackend()))))) {
            java.util.zip.ZipEntry entry = zip.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getTimeLocal()).isEqualTo(java.time.LocalDateTime.of(2000, 1, 1, 0, 0));
        }
        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(first));
             org.apache.poi.xssf.usermodel.XSSFWorkbook book = document.getCharts().get(0).getWorkbook()) {
            assertThat(book.getSheetAt(0).getRow(4).getCell(0).getStringCellValue()).isEqualTo("West");
            assertThat(book.getSheetAt(0).getRow(4).getCell(1).getNumericCellValue()).isEqualTo(10.0);
        }
    }

    private static Consumer<PageFlowBuilder> line(LineInterpolation interpolation, boolean area) {
        return page -> page.add(new ChartNode(ChartSpec.line().data(SWING).interpolation(interpolation).area(area)
                .legend(LegendPosition.BOTTOM).build()));
    }

    private static Consumer<PageFlowBuilder> pie(ChartData data, double start) {
        return page -> page.add(new ChartNode(ChartSpec.pie().data(data).startAngleDegrees(start).build()));
    }

    private static String note(Consumer<PageFlowBuilder> content) throws Exception {
        List<DocxExportReport.Note> notes = DocxExports.report(500, 500, 30, content).bySubject().get("chart");
        assertThat(notes).hasSize(1);
        return notes.get(0).detail();
    }

    private static String chartXml(Consumer<PageFlowBuilder> content) throws Exception {
        return part(content, CHART_TYPE);
    }

    private static String part(Consumer<PageFlowBuilder> content, String type) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(500, 500, 30, content)) {
            List<PackagePart> parts = document.getPackage().getPartsByContentType(type);
            assertThat(parts).hasSize(1);
            try (InputStream in = parts.get(0).getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    private static String bodyXml(Consumer<PageFlowBuilder> content) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(500, 500, 30, content)) {
            return document.getDocument().xmlText();
        }
    }

    private static PlacedNode placement(Consumer<PageFlowBuilder> content, ChartNode node) {
        try (DocumentSession session = GraphCompose.document().pageSize(500, 500).margin(DocumentInsets.of(30))
                .create()) {
            session.pageFlow(content::accept);
            return session.layoutGraph().nodes().stream().filter(n -> n.path().contains("Chart")).findFirst()
                    .orElseThrow();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static byte[] export(Consumer<PageFlowBuilder> content, DocxSemanticBackend backend) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(500, 500).margin(DocumentInsets.of(30))
                .create()) {
            session.pageFlow(content::accept);
            return session.export(backend);
        }
    }

    private static byte[] workbookBytes(byte[] docx) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(docx));
             InputStream in = document.getPackage().getPartsByContentType(
                     "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").get(0).getInputStream()) {
            return in.readAllBytes();
        }
    }

    private static double[] numbers(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        assertThat(matcher.find()).as("%s in the part", regex).isTrue();
        double[] numbers = new double[matcher.groupCount()];
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = Double.parseDouble(matcher.group(i + 1));
        }
        return numbers;
    }

    private static int count(String text, String of) {
        int count = 0;
        for (int at = text.indexOf(of); at >= 0; at = text.indexOf(of, at + 1)) {
            count++;
        }
        return count;
    }
}
