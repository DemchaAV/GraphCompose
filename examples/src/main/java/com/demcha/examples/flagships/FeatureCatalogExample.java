package com.demcha.examples.flagships;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.chart.AxisSpec;
import com.demcha.compose.document.chart.ChartData;
import com.demcha.compose.document.chart.ChartSize;
import com.demcha.compose.document.chart.ChartSpec;
import com.demcha.compose.document.chart.ChartStyle;
import com.demcha.compose.document.chart.LegendPosition;
import com.demcha.compose.document.chart.PointMarker;
import com.demcha.compose.document.chart.SliceLabelMode;
import com.demcha.compose.document.chart.ValueLabelMode;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.PolygonNode;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.style.DocumentTransform;
import com.demcha.compose.document.style.ShapePoint;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * The browsable feature catalog: one PDF where every shipped capability is a
 * self-documenting block — a heading (which also lands in the PDF outline, so
 * the bookmarks panel works as a clickable table of contents), the exact API
 * call that produced it, and the live rendered result right below. Open the
 * PDF, spot "nested lists" or "donut chart", and the snippet above the block
 * is how it's done; this source file contains the literal same calls.
 *
 * <p>Every block opts into {@code keepTogether()}, so a snippet is never
 * orphaned from its rendered result at a page break — itself one of the
 * catalogued features.</p>
 */
public final class FeatureCatalogExample {

    private static final BusinessTheme THEME = BusinessTheme.modern();
    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(112, 116, 128);
    private static final DocumentColor TEAL = DocumentColor.rgb(20, 80, 95);
    private static final DocumentColor GOLD = DocumentColor.rgb(196, 153, 76);
    private static final DocumentColor CODE_BG = DocumentColor.rgb(244, 245, 248);
    private static final DocumentColor CODE_INK = DocumentColor.rgb(52, 74, 94);

    private FeatureCatalogExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("flagships", "feature-catalog.pdf");

        ChartData revenue = ChartData.builder()
                .categories("Q1", "Q2", "Q3", "Q4")
                .series("2024", 12.4, 15.1, 9.8, 14.2)
                .series("2025", 14.0, 18.2, 11.3, 16.9)
                .build();
        ChartData regions = ChartData.builder()
                .categories("EMEA", "Americas", "APAC", "Other")
                .series("Share", 38.0, 31.0, 22.0, 9.0)
                .build();

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(THEME.pageBackground())
                .margin(34, 34, 34, 34)
                .create()) {

            document.metadata(DocumentMetadata.builder()
                    .title("GraphCompose Feature Catalog")
                    .author("DemchaAV")
                    .subject("Every shipped capability with its API call and rendered result")
                    .build());
            document.header(DocumentHeaderFooter.builder()
                    .zone(DocumentHeaderFooterZone.HEADER)
                    .leftText("GraphCompose · Feature catalog")
                    .rightText("{date}")
                    .fontSize(8f).textColor(MUTED)
                    .showSeparator(true)
                    .separatorColor(THEME.palette().rule())
                    .separatorThickness(0.5f)
                    .build());
            document.footer(DocumentHeaderFooter.builder()
                    .zone(DocumentHeaderFooterZone.FOOTER)
                    .centerText("Page {page} of {pages}")
                    .fontSize(8f).textColor(MUTED)
                    .build());
            var flow = document.pageFlow().name("FeatureCatalog").spacing(12);

            flow.addSection("Intro", s -> s
                    .softPanel(THEME.palette().surfaceMuted(), 10, 16)
                    .spacing(6)
                    .addParagraph(p -> p
                            .text("GraphCompose feature catalog")
                            .textStyle(THEME.text().h1())
                            .bookmark(new DocumentBookmarkOptions("Feature catalog", 0))
                            .margin(DocumentInsets.zero()))
                    .addParagraph(p -> p
                            .text("Each block below is self-documenting: the heading lands in the "
                                    + "PDF outline (use your viewer's bookmarks panel as a clickable "
                                    + "index), the grey panel shows the exact API call, and the result "
                                    + "renders right under it. Blocks never split across pages — "
                                    + "that's keepTogether(), also catalogued here.")
                            .textStyle(THEME.text().body())
                            .lineSpacing(1.35)
                            .margin(DocumentInsets.zero())));

            feature(flow, "Rich text — mixed runs in one paragraph", """
                    section.addRich(r -> r
                        .plain("Status: ").bold("Approved ").checkbox(9, true, TEAL)
                        .plain("  budget ").accent("$1.2M", GOLD)
                        .plain("  details: ").link("docs", "https://github.com/DemchaAV/GraphCompose"))""",
                    demo -> demo.addRich(r -> r
                            .plain("Status: ").bold("Approved ").checkbox(9, true, TEAL)
                            .plain("  budget ").accent("$1.2M", GOLD)
                            .plain("  details: ").link("docs", "https://github.com/DemchaAV/GraphCompose")));

            feature(flow, "Inline sparklines — mini-charts on the text baseline", """
                    section.addRich(r -> r
                        .plain("Revenue trend ").sparkline(42, 9, TEAL, 65.2, 69.8, 74.1, 81.3, 88.2)
                        .plain("   profit ").sparklineLine(42, 9, 1.6, GOLD, 28.1, 30.7, 32.9, 36.4, 39.5))""",
                    demo -> demo.addRich(r -> r
                            .plain("Revenue trend ").sparkline(42, 9, TEAL, 65.2, 69.8, 74.1, 81.3, 88.2)
                            .plain("   profit ").sparklineLine(42, 9, 1.6, GOLD, 28.1, 30.7, 32.9, 36.4, 39.5)));

            feature(flow, "Nested lists — per-depth markers", """
                    section.addList(l -> l
                        .markerFor(1, ListMarker.custom("–")).markerFor(2, ListMarker.custom("·"))
                        .addItem("Platform", nested -> nested
                            .addItem("Layout engine")
                            .addItem("Backends", deep -> deep.addItem("PDF").addItem("DOCX")))
                        .addItem("Tooling"))""",
                    demo -> demo.addList(l -> l
                            .markerFor(1, com.demcha.compose.document.node.ListMarker.custom("–"))
                            .markerFor(2, com.demcha.compose.document.node.ListMarker.custom("·"))
                            .addItem("Platform", nested -> nested
                                    .addItem("Layout engine")
                                    .addItem("Backends", deep -> deep.addItem("PDF").addItem("DOCX")))
                            .addItem("Tooling")));

            feature(flow, "Timeline — markers on a connector rail", """
                    section.addTimeline(t -> t.keepEntriesTogether()
                        .entry(TimelineMarker.numbered(1, 14, TEAL, WHITE),
                               e -> e.title("Kickoff").meta("Jan 2026").body("Scope agreed."))
                        .entry(TimelineMarker.dot(8, GOLD),
                               e -> e.title("Beta").meta("Mar 2026").body("First external users.")))""",
                    demo -> demo.addTimeline(t -> t.keepEntriesTogether()
                            .entry(com.demcha.compose.document.dsl.TimelineMarker.numbered(1, 14, TEAL, DocumentColor.WHITE),
                                    e -> e.title("Kickoff").meta("Jan 2026").body("Scope agreed."))
                            .entry(com.demcha.compose.document.dsl.TimelineMarker.dot(8, GOLD),
                                    e -> e.title("Beta").meta("Mar 2026").body("First external users."))));

            feature(flow, "Tables — zebra rows and a totals row", """
                    section.addTable(t -> t
                        .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto(), DocumentTableColumn.auto())
                        .headerRow("Region", "Q1", "Q2")
                        .row("EMEA", "38", "41").row("APAC", "22", "25")
                        .zebra(WHITE, rgb(248, 246, 240))
                        .totalRow(totalStyle, "Total", "60", "66"))""",
                    demo -> demo.addTable(t -> t
                            .columns(com.demcha.compose.document.table.DocumentTableColumn.auto(),
                                    com.demcha.compose.document.table.DocumentTableColumn.auto(),
                                    com.demcha.compose.document.table.DocumentTableColumn.auto())
                            .headerRow("Region", "Q1", "Q2")
                            .row("EMEA", "38", "41").row("APAC", "22", "25")
                            .zebra(DocumentColor.WHITE, DocumentColor.rgb(248, 246, 240))
                            .totalRow(com.demcha.compose.document.table.DocumentTableStyle.builder()
                                            .textStyle(DocumentTextStyle.builder()
                                                    .fontName(FontName.HELVETICA_BOLD).size(9.5).color(INK).build())
                                            .fillColor(DocumentColor.rgb(240, 237, 228))
                                            .padding(DocumentInsets.of(6)).build(),
                                    "Total", "60", "66")));

            feature(flow, "Bar chart — grouped, value labels, legend", """
                    section.chart(ChartSpec.bar().data(revenue)
                        .valueLabels(ValueLabelMode.OUTSIDE)
                        .legend(LegendPosition.BOTTOM)
                        .size(ChartSize.fixedHeight(150)).build())""",
                    demo -> demo.chart(ChartSpec.bar().data(revenue)
                            .valueLabels(ValueLabelMode.OUTSIDE)
                            .legend(LegendPosition.BOTTOM)
                            .size(ChartSize.fixedHeight(150)).build()));

            feature(flow, "Line chart — smooth, area fill, markers, label halos", """
                    section.chart(ChartSpec.line().data(revenue)
                            .smooth(true).area(true).valueLabels(ValueLabelMode.OUTSIDE)
                            .size(ChartSize.fixedHeight(150)).build(),
                        ChartStyle.builder().lineWidth(1.8)
                            .pointMarker(PointMarker.circle(5).withStroke(DocumentStroke.of(WHITE, 1.2)))
                            .build())""",
                    demo -> demo.chart(ChartSpec.line().data(revenue)
                                    .smooth(true).area(true).valueLabels(ValueLabelMode.OUTSIDE)
                                    .size(ChartSize.fixedHeight(150)).build(),
                            ChartStyle.builder().lineWidth(1.8)
                                    .pointMarker(PointMarker.circle(5)
                                            .withStroke(DocumentStroke.of(DocumentColor.WHITE, 1.2)))
                                    .build()));

            feature(flow, "Donut chart — centre KPI, slice gaps, percent labels", """
                    section.chart(ChartSpec.pie().data(regions)
                            .donutRatio(0.58).centerText("58.4k")
                            .sliceLabels(SliceLabelMode.PERCENT)
                            .legend(LegendPosition.RIGHT)
                            .size(ChartSize.fixedHeight(170)).build(),
                        ChartStyle.builder().sliceGapDegrees(2).build())""",
                    demo -> demo.chart(ChartSpec.pie().data(regions)
                                    .donutRatio(0.58).centerText("58.4k")
                                    .sliceLabels(SliceLabelMode.PERCENT)
                                    .legend(LegendPosition.RIGHT)
                                    .size(ChartSize.fixedHeight(170)).build(),
                            ChartStyle.builder().sliceGapDegrees(2).build()));

            feature(flow, "Horizontal bars — categories on Y, stacked totals", """
                    section.chart(ChartSpec.bar().data(revenue)
                        .horizontal(true).grouping(BarGrouping.STACKED)
                        .valueLabels(ValueLabelMode.OUTSIDE)
                        .size(ChartSize.fixedHeight(140)).build())""",
                    demo -> demo.chart(ChartSpec.bar().data(revenue)
                            .horizontal(true)
                            .grouping(com.demcha.compose.document.chart.BarGrouping.STACKED)
                            .valueLabels(ValueLabelMode.OUTSIDE)
                            .size(ChartSize.fixedHeight(140)).build()));

            feature(flow, "Images — classpath bytes, explicit size, fit modes", """
                    DocumentImageData photo = DocumentImageData.fromBytes(
                        getClass().getResourceAsStream("/engine-hero.png").readAllBytes());
                    section.addImage(i -> i.source(photo).size(150, 84)
                        .fitMode(DocumentImageFitMode.COVER))   // vs CONTAIN on the right""",
                    demo -> demo.addRow(r -> r.spacing(14).weights(1, 1)
                            .addSection("Cover", a -> a.addImage(i -> i
                                    .source(catalogImage()).size(150, 84)
                                    .fitMode(com.demcha.compose.document.image.DocumentImageFitMode.COVER)))
                            .addSection("Contain", b -> b.addImage(i -> i
                                    .source(catalogImage()).size(150, 84)
                                    .fitMode(com.demcha.compose.document.image.DocumentImageFitMode.CONTAIN)))));

            feature(flow, "Shapes — dividers, ellipses, soft cards", """
                    section.addDivider(d -> d.width(420).thickness(2).color(GOLD));
                    section.addEllipse(96, 44, TEAL);
                    section.addShape(s -> s.size(150, 40).cornerRadius(10)
                        .fillColor(rgb(248, 246, 240)).stroke(DocumentStroke.of(GOLD, 0.8)))""",
                    demo -> demo
                            .addDivider(d -> d.width(420).thickness(2).color(GOLD))
                            .addRow(r -> r.spacing(14).weights(1, 1)
                                    .addSection("El", a -> a.addEllipse(96, 44, TEAL))
                                    .addSection("Card", b -> b.addShape(s -> s
                                            .size(150, 40).cornerRadius(10)
                                            .fillColor(DocumentColor.rgb(248, 246, 240))
                                            .stroke(DocumentStroke.of(GOLD, 0.8))))));

            feature(flow, "Gradient fills — native linear and radial shadings", """
                    section.addShape(s -> s.size(420, 40).cornerRadius(8)
                        .fill(DocumentPaint.linear(TEAL, GOLD)));            // 0 deg = left -> right
                    section.addShape(s -> s.size(420, 40).cornerRadius(8)
                        .fill(new DocumentPaint.Radial(stops(GOLD, NAVY), 0.5, 0.5)))""",
                    demo -> demo
                            .addShape(s -> s.size(420, 40).cornerRadius(8)
                                    .fill(DocumentPaint.linear(TEAL, GOLD)))
                            .addShape(s -> s.size(420, 40).cornerRadius(8)
                                    .fill(new DocumentPaint.Radial(List.of(
                                            new DocumentPaint.Stop(0.0, GOLD),
                                            new DocumentPaint.Stop(1.0, DocumentColor.rgb(28, 38, 70))),
                                            0.5, 0.5))));

            feature(flow, "Translucency — rgba colours blend on overlap", """
                    section.addShape(s -> s.size(420, 34).cornerRadius(8)
                        .fillColor(TEAL.withOpacity(0.35)));
                    // alpha reaches the PDF as a graphics-state constant;
                    // opaque colours stay byte-identical""",
                    demo -> demo
                            .addShape(s -> s.size(420, 34).cornerRadius(8)
                                    .fillColor(TEAL.withOpacity(0.35)))
                            .addShape(s -> s.size(420, 34).cornerRadius(8)
                                    .margin(new DocumentInsets(-20, 0, 0, 60))
                                    .fillColor(GOLD.withOpacity(0.45))));

            feature(flow, "Custom polygons — arbitrary closed vector geometry", """
                    section.add(new PolygonNode("Gem", 80, 60,
                        List.of(new ShapePoint(0.5, 1), new ShapePoint(1, 0.6),
                                new ShapePoint(0.78, 0), new ShapePoint(0.22, 0),
                                new ShapePoint(0, 0.6)),
                        TEAL, DocumentStroke.of(GOLD, 1.2), zero(), zero()))""",
                    demo -> demo.add(new PolygonNode("Gem", 80, 60,
                            List.of(new ShapePoint(0.5, 1.0), new ShapePoint(1.0, 0.6),
                                    new ShapePoint(0.78, 0.0), new ShapePoint(0.22, 0.0),
                                    new ShapePoint(0.0, 0.6)),
                            TEAL, DocumentStroke.of(GOLD, 1.2),
                            DocumentInsets.zero(), DocumentInsets.zero())));

            feature(flow, "Shape as container — children clipped to the outline", """
                    section.addCircle(72, TEAL, c -> c
                        .stroke(DocumentStroke.of(GOLD, 1.2))
                        .center(label("GC", 20, WHITE)))""",
                    demo -> demo.addCircle(72, TEAL, c -> c
                            .stroke(DocumentStroke.of(GOLD, 1.2))
                            .center(new com.demcha.compose.document.dsl.ParagraphBuilder()
                                    .text("GC")
                                    .textStyle(DocumentTextStyle.builder()
                                            .fontName(FontName.HELVETICA_BOLD).size(20)
                                            .color(DocumentColor.WHITE).build())
                                    .align(com.demcha.compose.document.node.TextAlign.CENTER)
                                    .build())));

            feature(flow, "Canvas — absolute (x, y) placement", """
                    section.addCanvas(220, 70, canvas -> canvas
                        .position(badge("v1.8"), 8, 8)
                        .position(badge("charts"), 84, 26)
                        .position(badge("paint"), 160, 8))""",
                    demo -> demo.addCanvas(220, 70, canvas -> canvas
                            .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                            .position(badge("v1.8"), 8, 8)
                            .position(badge("charts"), 84, 26)
                            .position(badge("paint"), 160, 8)));

            feature(flow, "Transforms — render-time rotation", """
                    section.addShape(s -> s.size(120, 26).cornerRadius(13)
                        .fillColor(GOLD).rotate(-6))""",
                    demo -> demo.addShape(s -> s
                            .size(120, 26).cornerRadius(13).fillColor(GOLD)
                            .transform(DocumentTransform.rotate(-6))));

            feature(flow, "Barcodes — QR and Code 128 with theme tinting", """
                    section.addBarcode(b -> b.qrCode()
                        .data("https://github.com/DemchaAV/GraphCompose")
                        .foreground(TEAL).size(86, 86));
                    section.addBarcode(b -> b.code128().data("GC-2026-001").size(200, 44))""",
                    demo -> demo.addRow(r -> r.spacing(16).weights(1, 2)
                            .addSection("Qr", q -> q.addBarcode(b -> b.qrCode()
                                    .data("https://github.com/DemchaAV/GraphCompose")
                                    .foreground(TEAL).size(86, 86)))
                            .addSection("C128", c -> c.addBarcode(b -> b.code128()
                                    .data("GC-2026-001").size(200, 44)))));

            feature(flow, "Page chrome — this document's own header, footer, outline", """
                    document.metadata(DocumentMetadata.builder().title("…").author("…").build());
                    document.header(DocumentHeaderFooter.builder().zone(HEADER)
                        .leftText("GraphCompose · Feature catalog").rightText("{date}")…);
                    document.footer(…centerText("Page {page} of {pages}")…);
                    paragraph.bookmark(new DocumentBookmarkOptions("Feature catalog", 0))""",
                    demo -> demo.addParagraph(p -> p
                            .text("Look around: the running header and the page-X-of-Y footer on every "
                                    + "page come from the calls above, and every block heading is a "
                                    + "clickable entry in the PDF bookmarks panel. A document.watermark(…) "
                                    + "call stamps text or an image behind or above the content the same way.")
                            .textStyle(THEME.text().body())
                            .lineSpacing(1.35)
                            .margin(DocumentInsets.zero())));

            flow.build();
            document.buildPdf();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }

    /**
     * One catalogue block: outline-visible heading, the exact API call in a
     * code panel, and the live result — kept together across page breaks.
     */
    private static void feature(com.demcha.compose.document.dsl.PageFlowBuilder flow,
                                String title, String code, Consumer<SectionBuilder> demo) {
        flow.addSection(title.replaceAll("[^A-Za-z0-9]", ""), section -> {
            section.keepTogether()
                    .softPanel(DocumentColor.WHITE, 8, 14)
                    .stroke(DocumentStroke.of(THEME.palette().rule(), 0.5))
                    .spacing(8)
                    .addParagraph(p -> p
                            .text(title)
                            .textStyle(DocumentTextStyle.builder()
                                    .fontName(FontName.HELVETICA_BOLD).size(11).color(INK).build())
                            .bookmark(new DocumentBookmarkOptions(title, 1))
                            .margin(DocumentInsets.zero()))
                    .addSection("Code", c -> c
                            .softPanel(CODE_BG, 6, 10)
                            .addParagraph(p -> p
                                    // Non-breaking spaces survive line wrapping,
                                    // so snippet indentation stays visible.
                                    .text(code.replace("    ", "    "))
                                    .textStyle(DocumentTextStyle.builder()
                                            .fontName(FontName.COURIER).size(7.6).color(CODE_INK).build())
                                    .lineSpacing(1.25)
                                    .margin(DocumentInsets.zero())));
            demo.accept(section);
        });
    }

    /** Classpath demo photo shared by the image fit-mode pair. */
    private static com.demcha.compose.document.image.DocumentImageData catalogImage() {
        try (java.io.InputStream stream = java.util.Objects.requireNonNull(
                FeatureCatalogExample.class.getResourceAsStream("/engine-hero.png"),
                "engine-hero.png missing from examples/src/main/resources/")) {
            return com.demcha.compose.document.image.DocumentImageData.fromBytes(
                    stream.readAllBytes());
        } catch (java.io.IOException e) {
            throw new IllegalStateException("failed to load catalog demo image", e);
        }
    }

    private static com.demcha.compose.document.node.DocumentNode badge(String text) {
        return new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                .roundedRect(64, 22, 11)
                .fillColor(TEAL)
                .center(new com.demcha.compose.document.dsl.ParagraphBuilder()
                        .text(text)
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA_BOLD).size(9)
                                .color(DocumentColor.WHITE).build())
                        .align(com.demcha.compose.document.node.TextAlign.CENTER)
                        .build())
                .build();
    }
}
