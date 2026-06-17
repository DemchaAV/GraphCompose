package com.demcha.examples.flagships;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.chart.AxisSpec;
import com.demcha.compose.document.chart.BarGrouping;
import com.demcha.compose.document.chart.ChartData;
import com.demcha.compose.document.chart.ChartSize;
import com.demcha.compose.document.chart.ChartSpec;
import com.demcha.compose.document.chart.ChartStyle;
import com.demcha.compose.document.chart.LegendPosition;
import com.demcha.compose.document.chart.NumberFormatSpec;
import com.demcha.compose.document.chart.SliceLabelMode;
import com.demcha.compose.document.chart.ValueLabelMode;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * One-pager <em>Business Monthly Financial Report</em> dashboard, modelled
 * after the classic single-sheet infographic brief: a photo masthead, three
 * radial profit-margin gauges, a "Cash position" block (cash-balance KPI tile,
 * a monthly cash bar chart, a stacked OPEX chart, and a revenue-breakdown
 * donut), and a "Forecast Financial Analysis" block (a forecast revenue bar
 * chart and a forecast-vs-actual cost breakdown).
 *
 * <p>This is a tour of the v1.8 <strong>illustrative</strong> additions, every
 * visual drawn from deterministic geometry as native PDF curves — no raster:</p>
 * <ul>
 *   <li><b>Native vector charts</b> ({@code com.demcha.compose.document.chart}):
 *       vertical and horizontal bars, a stacked bar, grouped bars, and pie /
 *       donut slices, recoloured to the report's terracotta / teal / cream
 *       palette through the {@link ChartStyle} cascade.</li>
 *   <li><b>Donut gauges with a centre KPI</b> — each profit-margin ring is a
 *       two-slice {@link ChartSpec.Pie} (value + remainder track) with the
 *       percentage typeset in the donut hole via {@code centerText}.</li>
 *   <li><b>Inline sparklines</b> on the masthead text baseline
 *       ({@code RichText.sparkline} / {@code sparklineLine}).</li>
 *   <li><b>Free-form path clip</b> — the masthead photo is cut to a slanted
 *       parallelogram silhouette via {@link ShapeContainerBuilder#path} and
 *       {@link ClipPolicy#CLIP_PATH}.</li>
 *   <li><b>Block-level alignment</b> — the centred accent rule under the report
 *       title uses {@code addAligned(HorizontalAlign.CENTER, ...)}.</li>
 * </ul>
 *
 * <p>Combo charts (bars with an overlaid trend line) are not a native chart
 * kind, so the OPEX block reproduces the source's stacked bars and the trend
 * is shown instead as a masthead sparkline.</p>
 *
 * @author Artem Demchyshyn
 */
public final class FinancialReportExample {

    // ─────────────────── Palette (matches the reference) ──────────────────
    private static final DocumentColor PAPER       = DocumentColor.rgb(244, 239, 227);
    private static final DocumentColor BAND        = DocumentColor.rgb(224, 215, 191);
    private static final DocumentColor CARD        = DocumentColor.WHITE;
    private static final DocumentColor CARD_RING   = DocumentColor.rgb(223, 214, 192);
    private static final DocumentColor INK         = DocumentColor.rgb(38, 40, 46);
    private static final DocumentColor MUTED       = DocumentColor.rgb(122, 122, 128);
    private static final DocumentColor TEAL        = DocumentColor.rgb(52, 142, 140);
    private static final DocumentColor TEAL_DARK   = DocumentColor.rgb(34, 102, 100);
    private static final DocumentColor ORANGE      = DocumentColor.rgb(216, 90, 46);
    private static final DocumentColor ORANGE_DEEP = DocumentColor.rgb(118, 50, 32);
    private static final DocumentColor ORANGE_SOFT = DocumentColor.rgb(232, 150, 92);
    private static final DocumentColor GAUGE_TRACK = DocumentColor.rgb(228, 220, 194);

    private FinancialReportExample() {
    }

    /**
     * Renders the one-pager financial report dashboard.
     *
     * @return the generated PDF path
     * @throws Exception when rendering or resource IO fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("flagships", "financial-report.pdf");
        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(PAPER)
                .margin(18, 28, 14, 28)
                .create()) {
            compose(document);
            document.buildPdf();
        }
        return outputFile;
    }

    /**
     * Composes the one-pager into {@code document}. Shared by {@link #generate()}
     * and the layout snapshot test, so both lay out identical geometry — the test
     * creates its own session with the same page size and margin.
     *
     * @param document the open session to compose into
     * @throws Exception when the masthead photo resource cannot be read
     */
    static void compose(DocumentSession document) throws Exception {
        DocumentImageData photo = loadMastheadPhoto();
        PageFlowBuilder flow = document.pageFlow()
                .name("FinancialReport")
                .spacing(8);

        // ── Masthead: slanted photo + title + subtitle + sparklines ──
        flow.addRow("Masthead", row -> row
                .spacing(16)
                .weights(5, 13)
                .addSection("Photo", s -> s.add(mastheadPhoto(photo)))
                .addSection("Title", s -> s
                        .spacing(2)
                        .addParagraph(p -> p
                                .text("One Pager Business")
                                .textStyle(titleStyle())
                                .margin(DocumentInsets.zero()))
                        .addParagraph(p -> p
                                .text("Monthly Financial Report")
                                .textStyle(titleStyle())
                                .margin(DocumentInsets.zero()))
                        .addParagraph(p -> p
                                .text("This one pager covers the business monthly financial "
                                      + "report to assess performance — gross, operating, and "
                                      + "net profit margin, plus cash, OPEX, and forecast detail.")
                                .textStyle(subtitleStyle())
                                .lineSpacing(1.35)
                                .margin(new DocumentInsets(5, 0, 0, 0)))
                        .addRich(r -> r
                                .style("Revenue ", legendStyle())
                                .sparkline(46, 9, TEAL, 65, 70, 74, 81, 88)
                                .style("    Net margin ", legendStyle())
                                .sparklineLine(46, 9, 1.6, ORANGE, 9, 10, 11, 11.5, 12))));

        // ── Centred report-period band with a centred accent rule ──
        flow.addSection("Period", s -> s
                .spacing(4)
                .addParagraph(p -> p
                        .text("Financial Report : May 2023")
                        .textStyle(periodStyle())
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()))
                .addAligned(HorizontalAlign.CENTER, new ShapeBuilder()
                        .name("PeriodRule")
                        .size(120, 2.5)
                        .fillColor(ORANGE)
                        .margin(DocumentInsets.zero())
                        .build()));

        // ── Three profit-margin gauges ──
        flow.addRow("Gauges", row -> row
                .spacing(14)
                .evenWeights()
                .addSection("Gauge1", s -> gauge(s, 79, "Gross profit"))
                .addSection("Gauge2", s -> gauge(s, 30, "Operating profit margin"))
                .addSection("Gauge3", s -> gauge(s, 12, "Net profit margin")));

        // ── Cash position ──
        sectionBand(flow, "Cash position");

        flow.addRow("CashRow1", row -> row
                .spacing(14)
                .weights(8, 12)
                .addSection("CashBalance", FinancialReportExample::cashBalanceCard)
                .addSection("CashEnd", FinancialReportExample::cashAtEndCard));

        flow.addRow("CashRow2", row -> row
                .spacing(14)
                .weights(11, 9)
                .addSection("Opex", FinancialReportExample::opexCard)
                .addSection("Breakdown", FinancialReportExample::revenueBreakdownCard));

        // ── Forecast Financial Analysis ──
        sectionBand(flow, "Forecast Financial Analysis");

        flow.addRow("ForecastRow", row -> row
                .spacing(14)
                .evenWeights()
                .addSection("Revenue", FinancialReportExample::forecastRevenueCard)
                .addSection("Costs", FinancialReportExample::costBreakdownCard));

        flow.build();
    }

    /**
     * CLI entry point.
     *
     * @param args ignored
     * @throws Exception when rendering fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }

    // ─────────────────── Masthead photo (path clip) ──────────────────────

    private static DocumentNode mastheadPhoto(DocumentImageData photo) {
        double w = 140;
        double h = 96;
        // Slanted parallelogram silhouette — unit box, (0,0) bottom-left, y up.
        List<DocumentPathSegment> parallelogram = List.of(
                DocumentPathSegment.moveTo(0.12, 1.0),
                DocumentPathSegment.lineTo(1.0, 1.0),
                DocumentPathSegment.lineTo(0.88, 0.0),
                DocumentPathSegment.lineTo(0.0, 0.0),
                DocumentPathSegment.close());
        return new ShapeContainerBuilder()
                .name("MastheadPhoto")
                .path(w, h, parallelogram)
                .clipPolicy(ClipPolicy.CLIP_PATH)
                .stroke(DocumentStroke.of(TEAL, 1.5))
                .center(new ImageBuilder()
                        .source(photo)
                        .size(w, h)
                        .fitMode(DocumentImageFitMode.COVER)
                        .build())
                .build();
    }

    // ─────────────────── Gauges (donut + centre KPI) ─────────────────────

    private static void gauge(SectionBuilder section, double percent, String label) {
        ChartData data = ChartData.builder()
                .categories("value", "remainder")
                .series("margin", percent, 100 - percent)
                .build();
        ChartSpec spec = ChartSpec.pie()
                .data(data)
                .donutRatio(0.66)
                .startAngleDegrees(90)
                .clockwise(true)
                .centerText((int) percent + "%")
                .sliceLabels(SliceLabelMode.NONE)
                .legend(LegendPosition.NONE)
                .size(ChartSize.fixedHeight(84))
                .build();
        ChartStyle style = ChartStyle.builder()
                .palette(DocumentPaint.solid(ORANGE), DocumentPaint.solid(GAUGE_TRACK))
                .donutCenterTextStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA_BOLD)
                        .size(22)
                        .color(INK)
                        .build())
                .build();
        section.spacing(3)
                .chart(spec, style)
                .addParagraph(p -> p
                        .text(label)
                        .textStyle(gaugeLabelStyle())
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()));
    }

    // ─────────────────── Cash balance KPI tile ───────────────────────────

    private static void cashBalanceCard(SectionBuilder section) {
        // The card is a column of CashRow1, so it cannot host nested horizontal
        // rows — each KPI line is a single rich paragraph (token + amount).
        section.softPanel(CARD, 8, 10)
                .stroke(DocumentStroke.of(CARD_RING, 0.6))
                .accentTop(TEAL, 1.5)
                .spacing(13)
                .addParagraph(p -> p
                        .text("Cash Balance")
                        .textStyle(cardTitleStyle())
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()))
                .addRich(r -> r
                        .style("$  ", kpiTokenStyle(ORANGE, 15))
                        .style("$199.28 MM", kpiValueStyle()))
                .addRich(r -> r
                        .style("IN   ", kpiTokenStyle(MUTED, 11))
                        .style("$250.88 MM", kpiValueStyle()))
                .addRich(r -> r
                        .style("OUT  ", kpiTokenStyle(MUTED, 11))
                        .style("$150.66 MM", kpiValueStyle()));
    }

    // ─────────────────── Cash at end of month (bar) ──────────────────────

    private static void cashAtEndCard(SectionBuilder section) {
        ChartData data = ChartData.builder()
                .categories("Jan-22", "Feb-22", "Mar-22", "Apr-22", "May-22")
                .series("Cash", 13, 12, 15, 16, 17)
                .build();
        ChartSpec spec = ChartSpec.bar()
                .data(data)
                .valueAxis(AxisSpec.builder()
                        .baselineAtZero(true)
                        .max(20)
                        .format(NumberFormatSpec.pattern("#,##0").withSuffix("k"))
                        .build())
                .size(ChartSize.fixedHeight(96))
                .build();
        section.softPanel(CARD, 8, 10)
                .stroke(DocumentStroke.of(CARD_RING, 0.6))
                .accentTop(ORANGE, 1.5)
                .spacing(6)
                .addParagraph(p -> p
                        .text("Cash at end of Month")
                        .textStyle(cardTitleStyle())
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()))
                .chart(spec, tealBars());
    }

    // ─────────────────── OPEX (stacked bar) ──────────────────────────────

    private static void opexCard(SectionBuilder section) {
        ChartData data = ChartData.builder()
                .categories("M1", "M2", "M3", "M4", "M5")
                .series("OPEX", 42, 36, 46, 40, 43)
                .series("Marketing", 30, 20, 30, 28, 28)
                .build();
        ChartSpec spec = ChartSpec.bar()
                .data(data)
                .grouping(BarGrouping.STACKED)
                .valueAxis(AxisSpec.builder()
                        .baselineAtZero(true)
                        .max(100)
                        .format(NumberFormatSpec.pattern("#,##0").withSuffix("k"))
                        .build())
                .legend(LegendPosition.BOTTOM)
                .size(ChartSize.fixedHeight(110))
                .build();
        ChartStyle style = ChartStyle.builder()
                .palette(DocumentPaint.solid(ORANGE_DEEP), DocumentPaint.solid(ORANGE))
                .barCornerRadius(DocumentCornerRadius.top(2))
                .build();
        section.softPanel(CARD, 8, 10)
                .stroke(DocumentStroke.of(CARD_RING, 0.6))
                .accentTop(ORANGE, 1.5)
                .spacing(4)
                .addParagraph(p -> p
                        .text("OPEX")
                        .textStyle(cardTitleStyle())
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p
                        .text("Month to Month YTD")
                        .textStyle(captionStyle())
                        .margin(DocumentInsets.zero()))
                .chart(spec, style);
    }

    // ─────────────────── Revenue breakdown (donut) ───────────────────────

    private static void revenueBreakdownCard(SectionBuilder section) {
        ChartData data = ChartData.builder()
                .categories("Desktops", "Portables", "Accessories", "Wearables", "Other")
                .series("Share", 30, 26, 18, 14, 12)
                .build();
        ChartSpec spec = ChartSpec.pie()
                .data(data)
                .donutRatio(0.52)
                .sliceLabels(SliceLabelMode.PERCENT)
                .legend(LegendPosition.RIGHT)
                .size(ChartSize.fixedHeight(130))
                .build();
        ChartStyle style = ChartStyle.builder()
                .palette(
                        DocumentPaint.solid(TEAL),
                        DocumentPaint.solid(ORANGE),
                        DocumentPaint.solid(ORANGE_SOFT),
                        DocumentPaint.solid(TEAL_DARK),
                        DocumentPaint.solid(GAUGE_TRACK))
                .sliceGapDegrees(1.5)
                .build();
        section.softPanel(CARD, 8, 10)
                .stroke(DocumentStroke.of(CARD_RING, 0.6))
                .accentTop(TEAL, 1.5)
                .spacing(4)
                .addParagraph(p -> p
                        .text("Revenue Breakdown")
                        .textStyle(cardTitleStyle())
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()))
                .chart(spec, style);
    }

    // ─────────────────── Forecast revenue (bar) ──────────────────────────

    private static void forecastRevenueCard(SectionBuilder section) {
        ChartData data = ChartData.builder()
                .categories("Apr-22", "May-22", "Jun-22", "Jul-22", "Aug-22")
                .series("Revenue", 130, 135, 140, 138, 150)
                .build();
        ChartSpec spec = ChartSpec.bar()
                .data(data)
                .valueAxis(AxisSpec.builder()
                        .baselineAtZero(true)
                        .max(175)
                        .format(NumberFormatSpec.pattern("#,##0").withSuffix("k"))
                        .build())
                .valueLabels(ValueLabelMode.OUTSIDE)
                .size(ChartSize.fixedHeight(110))
                .build();
        section.softPanel(CARD, 8, 10)
                .stroke(DocumentStroke.of(CARD_RING, 0.6))
                .accentTop(TEAL, 1.5)
                .spacing(6)
                .addParagraph(p -> p
                        .text("Revenue")
                        .textStyle(cardTitleStyle())
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()))
                .chart(spec, tealBars());
    }

    // ─────────────────── Cost breakdown (horizontal grouped bars) ─────────

    private static void costBreakdownCard(SectionBuilder section) {
        ChartData data = ChartData.builder()
                .categories("Cogs", "Sales", "Marketing", "Gen & Admin", "Other", "Taxes")
                .series("Forecast", 300, 520, 180, 150, 60, 110)
                .series("Actual", 280, 500, 150, 170, 50, 90)
                .build();
        ChartSpec spec = ChartSpec.bar()
                .data(data)
                .horizontal(true)
                .grouping(BarGrouping.GROUPED)
                .valueAxis(AxisSpec.builder()
                        .baselineAtZero(true)
                        .max(600)
                        .build())
                .legend(LegendPosition.BOTTOM)
                .size(ChartSize.fixedHeight(130))
                .build();
        ChartStyle style = ChartStyle.builder()
                .seriesPaint(0, DocumentPaint.solid(TEAL))
                .seriesPaint(1, DocumentPaint.solid(ORANGE))
                .build();
        section.softPanel(CARD, 8, 10)
                .stroke(DocumentStroke.of(CARD_RING, 0.6))
                .accentTop(ORANGE, 1.5)
                .spacing(6)
                .addParagraph(p -> p
                        .text("Breakdown of Costs")
                        .textStyle(cardTitleStyle())
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()))
                .chart(spec, style);
    }

    // ─────────────────── Shared building blocks ──────────────────────────

    private static void sectionBand(PageFlowBuilder flow, String title) {
        flow.addSection("Band:" + title, s -> s
                .softPanel(BAND, 3, 7)
                .addParagraph(p -> p
                        .text(title)
                        .textStyle(bandTitleStyle())
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero())));
    }

    private static ChartStyle tealBars() {
        return ChartStyle.builder()
                .seriesPaint(0, DocumentPaint.solid(TEAL))
                .barCornerRadius(DocumentCornerRadius.top(2))
                .build();
    }

    // ─────────────────── Text styles ─────────────────────────────────────

    private static DocumentTextStyle titleStyle() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(19)
                .color(INK)
                .build();
    }

    private static DocumentTextStyle subtitleStyle() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(8.4)
                .color(MUTED)
                .build();
    }

    private static DocumentTextStyle periodStyle() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(15)
                .color(INK)
                .build();
    }

    private static DocumentTextStyle bandTitleStyle() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(12.5)
                .color(TEAL_DARK)
                .build();
    }

    private static DocumentTextStyle cardTitleStyle() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(11)
                .color(TEAL_DARK)
                .build();
    }

    private static DocumentTextStyle captionStyle() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(7.6)
                .color(MUTED)
                .build();
    }

    private static DocumentTextStyle gaugeLabelStyle() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(9.5)
                .color(INK)
                .build();
    }

    private static DocumentTextStyle kpiTokenStyle(DocumentColor color, double size) {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(size)
                .color(color)
                .build();
    }

    private static DocumentTextStyle kpiValueStyle() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(11)
                .color(INK)
                .build();
    }

    private static DocumentTextStyle legendStyle() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(8)
                .color(MUTED)
                .build();
    }

    // ─────────────────── Resource ────────────────────────────────────────

    private static DocumentImageData loadMastheadPhoto() throws Exception {
        try (InputStream in = Objects.requireNonNull(
                FinancialReportExample.class.getResourceAsStream("/engine-hero.jpg"),
                "engine-hero.jpg missing from examples/src/main/resources/")) {
            return DocumentImageData.fromBytes(in.readAllBytes());
        }
    }
}
