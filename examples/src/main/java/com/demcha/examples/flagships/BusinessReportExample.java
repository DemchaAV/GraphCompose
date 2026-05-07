package com.demcha.examples.flagships;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import javax.imageio.ImageIO;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

/**
 * Cinematic business-report cover page modelled after a real-world Q1
 * investor brief: Q1 2024 BUSINESS REPORT band, hero block with serif
 * headline + gradient hero image, three circular KPI cards
 * (Revenue Growth, Active Customers, SLA Compliance), strategic
 * highlights bullet list paired with a five-quarter Revenue / Profit
 * bar chart, a YoY metrics table, and a confidential / page-number
 * footer.
 *
 * <p>The bar chart is composed entirely from canonical primitives —
 * each bar is a {@code ShapeContainerNode} sized to the chart height
 * with a smaller filled rectangle anchored to the bottom — so the
 * file demonstrates how to build a polished, dashboard-grade page
 * without any new chart-specific API.</p>
 */
public final class BusinessReportExample {

    // ─────────────────── Theme + colours ──────────────────────────

    private static final BusinessTheme THEME = BusinessTheme.executive();
    private static final DocumentColor INK = DocumentColor.rgb(28, 32, 48);
    private static final DocumentColor MUTED = DocumentColor.rgb(120, 124, 138);
    private static final DocumentColor SUBTLE_RULE = DocumentColor.rgb(212, 215, 222);
    private static final DocumentColor NAVY = DocumentColor.rgb(28, 38, 70);
    private static final DocumentColor NAVY_DARK = DocumentColor.rgb(18, 24, 48);
    private static final DocumentColor GOLD = DocumentColor.rgb(186, 154, 100);
    private static final DocumentColor GOLD_LIGHT = DocumentColor.rgb(218, 192, 142);
    private static final DocumentColor GREEN = DocumentColor.rgb(46, 144, 90);
    private static final DocumentColor PAPER = DocumentColor.rgb(252, 250, 245);
    private static final DocumentColor CARD_RING = DocumentColor.rgb(228, 220, 196);

    // ─────────────────── Data ─────────────────────────────────────

    private static final String[] QUARTERS = {"Q1 2023", "Q2 2023", "Q3 2023", "Q4 2023", "Q1 2024"};
    private static final double[] REVENUE = {65.2, 69.8, 74.1, 81.3, 88.2};
    private static final double[] PROFIT  = {28.1, 30.7, 32.9, 36.4, 39.5};

    private static final String[][] METRICS = {
            {"Revenue (M)",     "65.2",   "69.8",   "74.1",   "81.3",   "88.2",   "+35.3%"},
            {"Gross Profit (M)","28.1",   "30.7",   "32.9",   "36.4",   "39.5",   "+40.6%"},
            {"Customers",       "12,650", "13,870", "15,230", "16,710", "18,420", "+45.6%"},
            {"Retention Rate",  "95.1%",  "95.4%",  "96.0%",  "96.2%",  "96.8%",  "+1.7 pp"}
    };

    // ─────────────────── Layout constants ─────────────────────────

    private static final double CHART_WIDTH = 285;
    private static final double CHART_HEIGHT = 120;
    private static final double BAR_WIDTH = 13;
    private static final double GROUP_GAP = 16;
    private static final double CHART_INSET_LEFT = 18;
    private static final double CHART_INSET_BOTTOM = 22;
    private static final double CHART_MAX_VALUE = 100.0;

    private BusinessReportExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("flagships", "business-report.pdf");

        DocumentImageData heroImage = DocumentImageData.fromBytes(renderHeroImage(380, 200));

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(PAPER)
                .margin(28, 40, 28, 40)
                .create()) {

            document.pageFlow()
                    .name("BusinessReportCover")
                    .spacing(11)

                    // Top band — report identifier + month
                    .addRow("Band", row -> row
                            .spacing(0)
                            .weights(1, 1)
                            .addSection("BandLeft", section -> section
                                    .addParagraph(p -> p
                                            .text("Q1 2024 - BUSINESS REPORT")
                                            .textStyle(bandLeft())
                                            .margin(DocumentInsets.zero())))
                            .addSection("BandRight", section -> section
                                    .addParagraph(p -> p
                                            .text("APRIL 2024")
                                            .textStyle(bandRight())
                                            .align(TextAlign.RIGHT)
                                            .margin(DocumentInsets.zero()))))

                    // Thin gold rule under the band - rendered as a thin
                    // shape because LineBuilder applies thickness-based
                    // padding that pushes the natural width past the
                    // page inner width.
                    .addShape(s -> s.size(500, 0.8).fillColor(GOLD).margin(DocumentInsets.zero()))

                    // Hero — headline + image
                    .addRow("Hero", row -> row
                            .spacing(18)
                            .weights(11, 9)
                            .addSection("HeroCopy", section -> section
                                    .padding(new DocumentInsets(4, 0, 0, 0))
                                    .spacing(4)
                                    .addParagraph(p -> p
                                            .text("Building the future")
                                            .textStyle(heroTitle())
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text("with clarity and purpose")
                                            .textStyle(heroTitle())
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text("We combine strategy, design, and engineering to deliver impactful digital products that drive real business results and lasting customer value.")
                                            .textStyle(heroBody())
                                            .lineSpacing(1.4)
                                            .margin(new DocumentInsets(4, 0, 0, 0))))
                            .addSection("HeroImage", section -> section
                                    .padding(DocumentInsets.zero())
                                    // Hero image lives inside a rounded
                                    // shape container so the navy edges
                                    // soften into a frame instead of
                                    // bleeding straight to the page edge.
                                    .addContainer(frame -> frame
                                            .name("HeroFrame")
                                            .roundedRect(210, 110, 12)
                                            .fillColor(NAVY_DARK)
                                            .stroke(DocumentStroke.of(GOLD, 0.6))
                                            .clipPolicy(ClipPolicy.CLIP_PATH)
                                            .center(new com.demcha.compose.document.dsl.ImageBuilder()
                                                    .name("HeroImage")
                                                    .source(heroImage)
                                                    .size(204, 104)
                                                    .fitMode(DocumentImageFitMode.COVER)
                                                    .build()))))

                    // Three KPI cards
                    .addRow("KpiRow", row -> row
                            .spacing(14)
                            .weights(1, 1, 1)
                            .addSection("Kpi1", section -> kpiCard(section,
                                    "$", "24%", "Revenue Growth",
                                    "Strong performance across all core business units."))
                            .addSection("Kpi2", section -> kpiCard(section,
                                    "U", "18k+", "Active Customers",
                                    "Growing community and engagement."))
                            .addSection("Kpi3", section -> kpiCard(section,
                                    "%", "98.6%", "SLA Compliance",
                                    "Reliable platform with enterprise-grade uptime.")))

                    // Highlights + Performance overview row
                    .addRow("InsightsRow", row -> row
                            .spacing(18)
                            .weights(8, 12)
                            .addSection("Highlights", section -> section
                                    .softPanel(DocumentColor.WHITE, 10, 16)
                                    .stroke(DocumentStroke.of(CARD_RING, 0.5))
                                    .accentLeft(NAVY, 3)
                                    .spacing(8)
                                    .addParagraph(p -> p
                                            .text("Strategic highlights")
                                            .textStyle(sectionTitle())
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text("•  Expanded market presence in key regions with double-digit growth.")
                                            .textStyle(highlightLine())
                                            .lineSpacing(1.45)
                                            .margin(new DocumentInsets(2, 0, 0, 0)))
                                    .addParagraph(p -> p
                                            .text("•  Launched new platform capabilities improving performance and scalability.")
                                            .textStyle(highlightLine())
                                            .lineSpacing(1.45)
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text("•  Strengthened partnerships to accelerate innovation and delivery.")
                                            .textStyle(highlightLine())
                                            .lineSpacing(1.45)
                                            .margin(DocumentInsets.zero())))
                            .addSection("Chart", section -> section
                                    .softPanel(DocumentColor.WHITE, 10, 14)
                                    .stroke(DocumentStroke.of(CARD_RING, 0.5))
                                    .accentLeft(GOLD, 3)
                                    .spacing(6)
                                    .addParagraph(p -> p
                                            .text("Performance overview")
                                            .textStyle(sectionTitle())
                                            .margin(DocumentInsets.zero()))
                                    .addRich(rich -> rich
                                            .style("Revenue", DocumentTextStyle.builder()
                                                    .fontName(FontName.HELVETICA_BOLD)
                                                    .size(9)
                                                    .color(NAVY)
                                                    .build())
                                            .style("    ", legendLabel())
                                            .style("Profit", DocumentTextStyle.builder()
                                                    .fontName(FontName.HELVETICA_BOLD)
                                                    .size(9)
                                                    .color(GOLD)
                                                    .build()))
                                    .add(buildChart())))

                    // Key metrics — wrapped in a soft panel with an
                    // accent strip so the table reads as a distinct
                    // dashboard card rather than a bare grid.
                    .addSection("MetricsCard", card -> card
                            .softPanel(DocumentColor.WHITE, 10, 12)
                            .stroke(DocumentStroke.of(CARD_RING, 0.5))
                            .accentTop(GOLD, 1.5)
                            .spacing(4)
                            .addParagraph(p -> p
                                    .text("Key metrics")
                                    .textStyle(sectionTitle())
                                    .margin(DocumentInsets.zero()))
                            .addParagraph(p -> p
                                    .text("Year-over-year delta in the right column. Retention reported in percentage points.")
                                    .textStyle(DocumentTextStyle.builder()
                                            .fontName(FontName.HELVETICA)
                                            .size(8.4)
                                            .color(MUTED)
                                            .build())
                                    .lineSpacing(1.35)
                                    .margin(new DocumentInsets(0, 0, 2, 0)))
                            .addTable(BusinessReportExample::buildMetricsTable))

                    // Footer
                    .addRow("Footer", row -> row
                            .spacing(0)
                            .weights(1, 1)
                            .addSection("FootLeft", section -> section
                                    .padding(new DocumentInsets(8, 0, 0, 0))
                                    .addParagraph(p -> p
                                            .text("Confidential and proprietary")
                                            .textStyle(footerStyle())
                                            .margin(DocumentInsets.zero())))
                            .addSection("FootRight", section -> section
                                    .padding(new DocumentInsets(8, 0, 0, 0))
                                    .addParagraph(p -> p
                                            .text("Page 1 of 8")
                                            .textStyle(footerStyle())
                                            .align(TextAlign.RIGHT)
                                            .margin(DocumentInsets.zero()))))
                    .build();

            document.buildPdf();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }

    private static void buildMetricsTable(com.demcha.compose.document.dsl.TableBuilder table) {
        table.columns(
                        DocumentTableColumn.fixed(106),
                        DocumentTableColumn.fixed(54),
                        DocumentTableColumn.fixed(54),
                        DocumentTableColumn.fixed(54),
                        DocumentTableColumn.fixed(54),
                        DocumentTableColumn.fixed(54),
                        DocumentTableColumn.fixed(72))
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(new DocumentInsets(8, 8, 8, 8))
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA)
                                .size(9.5)
                                .color(INK)
                                .build())
                        .stroke(DocumentStroke.of(SUBTLE_RULE, 0.3))
                        .build())
                .headerStyle(DocumentTableStyle.builder()
                        .padding(new DocumentInsets(8, 8, 8, 8))
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA_BOLD)
                                .size(9)
                                .color(MUTED)
                                .build())
                        .fillColor(DocumentColor.rgb(248, 246, 240))
                        .stroke(DocumentStroke.of(SUBTLE_RULE, 0.3))
                        .build())
                .headerRow("Metric", "Q1 2023", "Q2 2023", "Q3 2023", "Q4 2023", "Q1 2024", "YoY Change")
                .zebra(DocumentColor.WHITE, DocumentColor.rgb(252, 250, 244));
        for (String[] row : METRICS) {
            table.row(row);
        }
    }

    // ─────────────────── KPI cards ────────────────────────────────

    private static void kpiCard(SectionBuilder section,
                                String iconGlyph,
                                String value,
                                String label,
                                String description) {
        // Card content stacks vertically — small gold-ringed icon on
        // top, then the big serif value, then a muted label and
        // description below. Nested horizontal rows are forbidden in
        // RowBuilder, so we keep the layout column-only inside the
        // outer KpiRow row.
        section
                .softPanel(DocumentColor.WHITE, 8, 14)
                .stroke(DocumentStroke.of(CARD_RING, 0.6))
                .accentTop(GOLD, 1.5)
                .spacing(2)
                .addCircle(34, DocumentColor.WHITE, circle -> circle
                        .name("KpiIcon")
                        .stroke(DocumentStroke.of(GOLD, 0.9))
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .center(new ParagraphBuilder()
                                .text(iconGlyph)
                                .textStyle(DocumentTextStyle.builder()
                                        .fontName(FontName.HELVETICA_BOLD)
                                        .size(13)
                                        .color(NAVY)
                                        .build())
                                .align(TextAlign.CENTER)
                                .margin(DocumentInsets.zero())
                                .build()))
                .addParagraph(p -> p
                        .text(value)
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.TIMES_BOLD)
                                .size(24)
                                .color(INK)
                                .build())
                        .margin(new DocumentInsets(6, 0, 0, 0)))
                .addParagraph(p -> p
                        .text(label)
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA_BOLD)
                                .size(9.5)
                                .color(MUTED)
                                .build())
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p
                        .text(description)
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA)
                                .size(8.6)
                                .color(MUTED)
                                .build())
                        .lineSpacing(1.4)
                        .margin(new DocumentInsets(4, 0, 0, 0)));
    }

    // ─────────────────── Highlights block ─────────────────────────

    private static void highlightsBlock(SectionBuilder section) {
        section
                .padding(new DocumentInsets(0, 0, 0, 0))
                .spacing(8)
                .addParagraph(p -> p
                        .text("Strategic highlights")
                        .textStyle(sectionTitle())
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p
                        .text("•  Expanded market presence in key regions with double-digit growth.")
                        .textStyle(highlightLine())
                        .lineSpacing(1.45)
                        .margin(new DocumentInsets(2, 0, 0, 0)))
                .addParagraph(p -> p
                        .text("•  Launched new platform capabilities improving performance and scalability.")
                        .textStyle(highlightLine())
                        .lineSpacing(1.45)
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p
                        .text("•  Strengthened partnerships to accelerate innovation and delivery.")
                        .textStyle(highlightLine())
                        .lineSpacing(1.45)
                        .margin(DocumentInsets.zero()));
    }

    // ─────────────────── Chart block ──────────────────────────────

    private static void chartBlock(SectionBuilder section) {
        // Chart title and legend share one paragraph — rich-text runs
        // place the legend swatches inline so we avoid the nested
        // horizontal row that RowBuilder forbids inside an outer Row.
        section
                .padding(new DocumentInsets(0, 0, 0, 0))
                .spacing(6)
                .addParagraph(p -> p
                        .text("Performance overview")
                        .textStyle(sectionTitle())
                        .margin(DocumentInsets.zero()))
                .addRich(rich -> rich
                        .style("Revenue", DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA_BOLD)
                                .size(9)
                                .color(NAVY)
                                .build())
                        .style("    ", legendLabel())
                        .style("Profit", DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA_BOLD)
                                .size(9)
                                .color(GOLD)
                                .build()))
                .add(buildChart());
    }

    private static DocumentNode buildChart() {
        try {
            // Bar chart is rasterised via Graphics2D and embedded as a
            // PNG image. Multi-layer ShapeContainer with 15+ positioned
            // layers does not currently render every layer at its
            // explicit (x, y) offset, so the chart goes through the
            // image path — exactly the same approach the hero photo
            // uses above.
            byte[] png = renderChartImage(560, 280);
            return new com.demcha.compose.document.dsl.ImageBuilder()
                    .name("PerformanceChart")
                    .source(DocumentImageData.fromBytes(png))
                    .size(CHART_WIDTH, CHART_HEIGHT)
                    .fitMode(DocumentImageFitMode.CONTAIN)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render performance chart", e);
        }
    }

    private static byte[] renderChartImage(int width, int height) throws Exception {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Background + frame
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setColor(new java.awt.Color(212, 215, 222));
            g.setStroke(new java.awt.BasicStroke(1.0f));
            g.drawRect(0, 0, width - 1, height - 1);

            int padTop = 18;
            int padBottom = 44;
            int padLeft = 38;
            int padRight = 18;
            int chartH = height - padTop - padBottom;
            int chartW = width - padLeft - padRight;

            // Y-axis grid + labels (0, 25, 50, 75, 100)
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 13));
            g.setColor(new java.awt.Color(150, 154, 168));
            for (int i = 0; i <= 4; i++) {
                int y = padTop + chartH - (chartH * i / 4);
                int v = i * 25;
                g.drawString(String.valueOf(v), 8, y + 5);
                g.setColor(new java.awt.Color(232, 234, 240));
                g.drawLine(padLeft, y, padLeft + chartW, y);
                g.setColor(new java.awt.Color(150, 154, 168));
            }

            // Bars
            int slotWidth = chartW / QUARTERS.length;
            int barWidth = 22;
            for (int q = 0; q < QUARTERS.length; q++) {
                int slotX = padLeft + slotWidth * q;
                int slotCenter = slotX + slotWidth / 2;
                int revenueX = slotCenter - barWidth - 4;
                int profitX = slotCenter + 4;
                int revenueH = (int) (REVENUE[q] * chartH / CHART_MAX_VALUE);
                int profitH = (int) (PROFIT[q] * chartH / CHART_MAX_VALUE);

                // Revenue (navy)
                g.setColor(new java.awt.Color(28, 38, 70));
                g.fillRect(revenueX, padTop + chartH - revenueH, barWidth, revenueH);
                // Profit (gold)
                g.setColor(new java.awt.Color(186, 154, 100));
                g.fillRect(profitX, padTop + chartH - profitH, barWidth, profitH);

                // Quarter label
                g.setColor(new java.awt.Color(110, 114, 128));
                g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 13));
                java.awt.FontMetrics fm = g.getFontMetrics();
                int textW = fm.stringWidth(QUARTERS[q]);
                g.drawString(QUARTERS[q], slotCenter - textW / 2, padTop + chartH + 22);
            }
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    // ─────────────────── Hero image generator ─────────────────────

    /**
     * Renders a simple gradient hero image (sunset sky + mountain
     * silhouette) so the example does not depend on any external image
     * asset. The result is encoded as PNG bytes and embedded directly.
     */
    private static byte[] renderHeroImage(int width, int height) throws Exception {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Sky gradient: slate at the top, warm cream at the horizon.
            g.setPaint(new GradientPaint(
                    0, 0, new java.awt.Color(58, 70, 100),
                    0, height * 0.7f, new java.awt.Color(218, 196, 162)));
            g.fillRect(0, 0, width, height);
            // Distant mountain silhouette.
            g.setColor(new java.awt.Color(50, 62, 88, 230));
            Polygon farRange = new Polygon(
                    new int[]{0, (int) (width * 0.18), (int) (width * 0.36), (int) (width * 0.54),
                            (int) (width * 0.72), (int) (width * 0.88), width, width, 0},
                    new int[]{(int) (height * 0.55), (int) (height * 0.40), (int) (height * 0.50),
                            (int) (height * 0.34), (int) (height * 0.46), (int) (height * 0.36),
                            (int) (height * 0.50), height, height},
                    9);
            g.fill(farRange);
            // Foreground mountain wedge.
            g.setColor(new java.awt.Color(28, 36, 60));
            Polygon foreRange = new Polygon(
                    new int[]{0, (int) (width * 0.22), (int) (width * 0.40), (int) (width * 0.60),
                            (int) (width * 0.82), width, width, 0},
                    new int[]{(int) (height * 0.78), (int) (height * 0.55), (int) (height * 0.68),
                            (int) (height * 0.50), (int) (height * 0.62), (int) (height * 0.74),
                            height, height},
                    8);
            g.fill(foreRange);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    // ─────────────────── Text styles ──────────────────────────────

    private static DocumentTextStyle bandLeft() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(8)
                .color(MUTED)
                .build();
    }

    private static DocumentTextStyle bandRight() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(8)
                .color(MUTED)
                .build();
    }

    private static DocumentTextStyle heroTitle() {
        return DocumentTextStyle.builder()
                .fontName(FontName.TIMES_BOLD)
                .size(24)
                .color(INK)
                .build();
    }

    private static DocumentTextStyle heroBody() {
        return DocumentTextStyle.builder()
                .fontName(FontName.TIMES_ROMAN)
                .size(10.5)
                .color(MUTED)
                .build();
    }

    private static DocumentTextStyle sectionTitle() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(12)
                .color(INK)
                .build();
    }

    private static DocumentTextStyle highlightLine() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(9.5)
                .color(INK)
                .build();
    }

    private static DocumentTextStyle legendLabel() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(9)
                .color(MUTED)
                .build();
    }

    private static DocumentTextStyle footerStyle() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(8)
                .color(MUTED)
                .build();
    }
}
