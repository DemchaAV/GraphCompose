package com.demcha.examples.flagships;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.chart.AxisSpec;
import com.demcha.compose.document.chart.ChartData;
import com.demcha.compose.document.chart.ChartSize;
import com.demcha.compose.document.chart.ChartSpec;
import com.demcha.compose.document.chart.ChartStyle;
import com.demcha.compose.document.chart.LegendPosition;
import com.demcha.compose.document.chart.ValueLabelMode;
import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.PathBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.CanvasChild;
import com.demcha.compose.document.node.CanvasLayerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Renders the repository's social preview card — the 1280x640 image GitHub
 * shows when the repo is linked on Threads, X, Slack or LinkedIn.
 *
 * <p>The card is itself a GraphCompose document. That is not only a flourish:
 * a generated card is regenerated from the palette and the wordmark the rest of
 * the brand already uses, so it cannot drift from them, and the release script
 * can refresh it the way it refreshes the README hero.</p>
 *
 * <p>What it draws is the claim the 2.1 line rests on. A single sheet on the
 * left resolves into two shapes on the right — a portrait page and a 16:9
 * slide. Both shapes are handed the <em>same</em> content: the same title, the
 * same line of body text and the same chart spec, built once by
 * {@link #documentBody}. The engine lays that content out into each box. So the
 * picture is not an illustration of the claim — it is the claim running.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.1.1
 */
public final class SocialCardExample {

    /**
     * GitHub renders social previews at 1280x640; anything else is letterboxed.
     *
     * <p>Feeds show the card at roughly 40% of that, so every type size here is
     * chosen to survive the reduction rather than to look balanced at 1:1.</p>
     */
    public static final double CARD_WIDTH = 1280;
    /** GitHub renders social previews at 1280x640; anything else is letterboxed. */
    public static final double CARD_HEIGHT = 640;

    private static final DocumentColor NIGHT = DocumentColor.rgb(9, 12, 27);
    private static final DocumentColor SURFACE = DocumentColor.rgb(20, 27, 51);
    private static final DocumentColor GRID = DocumentColor.rgb(38, 44, 78);
    private static final DocumentColor ON_DARK = DocumentColor.rgb(245, 247, 252);
    private static final DocumentColor ON_DARK_MUTED = DocumentColor.rgb(177, 185, 207);
    private static final DocumentColor VIOLET = DocumentColor.rgb(124, 92, 252);
    private static final DocumentColor VIOLET_LIGHT = DocumentColor.rgb(181, 159, 255);
    private static final DocumentColor MINT = DocumentColor.rgb(55, 214, 161);
    private static final DocumentColor SHEET_EDGE = DocumentColor.rgb(206, 213, 235);

    /** Inner padding of an output shape, in points. */
    private static final double CARD_PAD = 14;

    /** Height the title, body line and their spacing consume above the chart. */
    private static final double TEXT_BLOCK = 54;

    /**
     * Body lines below the chart, as fractions of the content width. Ragged on
     * purpose — a paragraph that ends mid-line is what makes a block read as
     * prose rather than as a bar chart lying on its side.
     */
    private static final double[] BODY_LINES = {1.00, 0.94, 0.97, 0.62};

    /** Height one body line and its gap consume. */
    private static final double BODY_LINE_STEP = 11;

    /** Horizontal gap between leaf fragments in the graph panel. */
    private static final double LEAF_STEP = 29;

    /** The one document both outputs render. Same text, same series, both shapes. */
    private static final String DOC_TITLE = "Quarterly revenue";
    private static final String DOC_LINE = "Composed once, resolved once, rendered twice.";

    private SocialCardExample() {
    }

    /**
     * Renders the card straight to an image, with no PDF round trip.
     *
     * @param dpi raster resolution; 72 yields exactly 1280x640 pixels,
     *            because the page is declared in points
     * @return the rendered card
     * @throws Exception when composition or rendering fails
     */
    public static BufferedImage renderCardImage(int dpi) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(CARD_WIDTH, CARD_HEIGHT)
                .pageBackground(NIGHT)
                .margin(DocumentInsets.zero())
                .create()) {
            compose(document);
            return document.toImage(0, dpi);
        }
    }

    /**
     * Writes the card as a PNG for upload under Settings &rarr; Social preview.
     *
     * @param target destination file; parent directories must exist
     * @throws Exception when composition, rendering or writing fails
     */
    public static void writePng(Path target) throws Exception {
        BufferedImage image = renderCardImage(72);
        ImageIO.write(image, "png", target.toFile());
    }

    /**
     * Generates the card as a single-page PDF beside the other flagship output.
     *
     * @return generated PDF path under {@code target/generated-pdfs/flagships}
     * @throws Exception when composition or rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("flagships", "social-card.pdf");
        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(CARD_WIDTH, CARD_HEIGHT)
                .pageBackground(NIGHT)
                .margin(DocumentInsets.zero())
                .create()) {
            compose(document);
            document.buildPdf();
        }
        return outputFile;
    }

    /**
     * Emits the same card as an editable PowerPoint deck.
     *
     * <p>The card claims one composition reaches a PDF and a deck; writing it
     * both ways from one session is the cheapest possible proof, and it keeps
     * the claim honest whenever this file changes.</p>
     *
     * @return generated PPTX path under {@code target/generated-pdfs/flagships}
     * @throws Exception when composition or rendering fails
     */
    public static Path generatePptx() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("flagships", "social-card.pptx");
        try (DocumentSession document = GraphCompose.document()
                .pageSize(CARD_WIDTH, CARD_HEIGHT)
                .pageBackground(NIGHT)
                .margin(DocumentInsets.zero())
                .create()) {
            compose(document);
            document.buildPptx(outputFile);
        }
        return outputFile;
    }

    /**
     * Runs the example: writes the PDF, the deck, then the PNG beside them.
     *
     * @param args optional single argument — the PNG destination path
     * @throws Exception when composition or rendering fails
     */
    public static void main(String[] args) throws Exception {
        Path pdf = generate();
        System.out.println("Generated: " + pdf);
        System.out.println("Generated: " + generatePptx());
        Path png = args.length > 0 ? Path.of(args[0]) : pdf.resolveSibling("social-card.png");
        Files.createDirectories(png.toAbsolutePath().getParent());
        writePng(png);
        System.out.println("Generated: " + png);
    }

    private static void compose(DocumentSession document) {
        document.metadata(DocumentMetadata.builder()
                .title("GraphCompose - social preview card")
                .author("GraphCompose")
                .subject("Repository social preview: one layout pass, two outputs")
                .keywords("graphcompose, java, pdf, pptx, document engine")
                .build());
        document.pageFlow()
                .name("SocialCard")
                .add(scene())
                .build();
    }

    private static DocumentNode scene() {
        List<CanvasChild> layers = new ArrayList<>();
        layers.addAll(grid());

        // Header. A social card has to say whose it is; the pipeline says why.
        layers.add(at(logoBlock(), 56, 34));
        layers.add(at(text("Tagline", "One layout pass. Two outputs.",
                display(18, ON_DARK), 320), 60, 104));
        layers.add(at(text("Footer", "Java 17+   ·   MIT   ·   Maven Central",
                mono(11.5, MINT), 320), 60, 586));

        // Stage 1 - what you hand the engine.
        layers.add(at(sourceTray(), 62, 300));
        layers.add(at(arrow(46), 196, 356));

        // Stage 2 - the resolved graph. The middle is the product.
        layers.add(at(graphPanel(), 254, 150));
        layers.add(at(text("GraphLabel", "RESOLVED LAYOUT GRAPH",
                mono(11, VIOLET_LIGHT), 300), 258, 124));

        // Stage 3 - two backends reading that one graph.
        layers.addAll(beams());
        layers.add(at(outputCard("PdfOut", 190, 250, VIOLET, VIOLET_LIGHT), 782, 84));
        layers.add(at(outputCard("PptxOut", 400, 196, MINT, MINT), 782, 396));
        layers.add(at(text("PdfLabel", "PDF", mono(12, VIOLET_LIGHT), 80), 786, 56));
        layers.add(at(text("PptxLabel", "PPTX", mono(12, MINT), 80), 786, 368));

        return new CanvasLayerNode("SocialCard", CARD_WIDTH, CARD_HEIGHT, layers,
                ClipPolicy.CLIP_BOUNDS, DocumentInsets.zero(), DocumentInsets.zero());
    }

    /** Stage 1: a tray of authored documents waiting to be composed. */
    private static DocumentNode sourceTray() {
        ShapeContainerBuilder tray = new ShapeContainerBuilder()
                .name("SourceTray")
                .roundedRect(118, 112, 6)
                .fillColor(DocumentColor.rgba(0, 0, 0, 0))
                .stroke(DocumentStroke.of(MINT.withOpacity(0.45), 0.9));
        for (int i = 0; i < 3; i++) {
            tray.position(miniSheet(), 16 + i * 12, 20 + i * 7, LayerAlign.TOP_LEFT);
        }
        return tray.build();
    }

    private static DocumentNode miniSheet() {
        ShapeContainerBuilder sheet = new ShapeContainerBuilder()
                .name("MiniSheet")
                .roundedRect(58, 74, 3)
                .fillColor(SURFACE.withOpacity(0.92))
                .stroke(DocumentStroke.of(SHEET_EDGE.withOpacity(0.45), 0.7));
        for (int i = 0; i < 4; i++) {
            sheet.position(rect(i == 0 ? 26 : 38 - i * 4, 2.4, ON_DARK.withOpacity(0.24)),
                    9, 12 + i * 8, LayerAlign.TOP_LEFT);
        }
        return sheet.build();
    }

    /**
     * A short connector between stages.
     *
     * @param width connector length in points
     * @return the composed connector
     */
    private static DocumentNode arrow(double width) {
        return new ShapeContainerBuilder()
                .name("Arrow")
                .rectangle(width, 10)
                .fillColor(DocumentColor.rgba(0, 0, 0, 0))
                .position(rect(width - 8, 1.1, MINT.withOpacity(0.75)), 0, 4.5,
                        LayerAlign.TOP_LEFT)
                .position(dot(5, MINT), width - 8, 2.5, LayerAlign.TOP_LEFT)
                .build();
    }

    /**
     * Stage 2: the resolved layout graph - the thing the name is about.
     *
     * <p>A document root, its sections, and the leaf fragments each section
     * resolves to, over the flat ordered fragment stream the compiler emits.
     * Every backend downstream reads this and nothing else, which is why two of
     * them agree.</p>
     *
     * @return the composed panel
     */
    private static DocumentNode graphPanel() {
        double w = 416;
        double h = 396;
        ShapeContainerBuilder panel = new ShapeContainerBuilder()
                .name("GraphPanel")
                .roundedRect(w, h, 8)
                .fillColor(DocumentColor.rgba(15, 20, 40, 170))
                .stroke(DocumentStroke.of(VIOLET.withOpacity(0.42), 1.0));

        double[] tier = {0.14, 0.42, 0.70};

        // The root hangs over the middle of the TIER ROW, which is not the middle
        // of the panel: the drops sit at w*t + 31, so their span is centred 2.3pt
        // left of w/2. Centring the root on the panel put it visibly off its own
        // subtree.
        double rowCentre = w * (tier[0] + tier[2]) / 2 + 31;
        panel.position(graphNode(68, 30, VIOLET_LIGHT), rowCentre - 34, 26,
                LayerAlign.TOP_LEFT);
        panel.position(rect(1.0, 20, VIOLET.withOpacity(0.5)), rowCentre, 56,
                LayerAlign.TOP_LEFT);

        // The rail spans exactly first-drop to last-drop; any overshoot shows as
        // a whisker past the junction at this size.
        panel.position(rect(w * (tier[2] - tier[0]), 1.0, VIOLET.withOpacity(0.5)),
                w * tier[0] + 31, 76, LayerAlign.TOP_LEFT);
        for (double t : tier) {
            panel.position(rect(1.0, 20, VIOLET.withOpacity(0.5)), w * t + 31, 76,
                    LayerAlign.TOP_LEFT);
            panel.position(graphNode(62, 28, VIOLET.withOpacity(0.85)), w * t, 96,
                    LayerAlign.TOP_LEFT);
            panel.position(rect(1.0, 18, VIOLET.withOpacity(0.35)), w * t + 31, 124,
                    LayerAlign.TOP_LEFT);
            panel.position(rect(LEAF_STEP * 2, 1.0, VIOLET.withOpacity(0.35)),
                    w * t + 2, 142, LayerAlign.TOP_LEFT);
            for (int leaf = 0; leaf < 3; leaf++) {
                double lx = w * t + 2 + leaf * LEAF_STEP;
                panel.position(rect(1.0, 12, VIOLET.withOpacity(0.35)), lx, 142,
                        LayerAlign.TOP_LEFT);
                panel.position(leafNode(), lx - 9, 154, LayerAlign.TOP_LEFT);
            }
        }

        // The flat, ordered fragment stream the backends actually consume.
        panel.position(rect(w - 56, 1.0, VIOLET.withOpacity(0.28)), 28, 216,
                LayerAlign.TOP_LEFT);
        for (int i = 0; i < 9; i++) {
            panel.position(rect(28, 10, VIOLET.withOpacity(0.30)), 30 + i * 40, 234,
                    LayerAlign.TOP_LEFT);
        }
        for (int i = 0; i < 7; i++) {
            panel.position(rect(36, 6, ON_DARK.withOpacity(0.13)), 30 + i * 52, 264,
                    LayerAlign.TOP_LEFT);
        }
        for (int i = 0; i < 5; i++) {
            panel.position(rect(52, 6, ON_DARK.withOpacity(0.10)), 30 + i * 72, 284,
                    LayerAlign.TOP_LEFT);
        }
        panel.position(rect(w - 56, 1.0, VIOLET.withOpacity(0.22)), 28, 312,
                LayerAlign.TOP_LEFT);
        panel.position(textIn("GraphFoot", "measure  ·  paginate  ·  place",
                mono(9.5, ON_DARK_MUTED), w - 56), 28, 326, LayerAlign.TOP_LEFT);
        return panel.build();
    }

    /**
     * One node box in the tree.
     *
     * @param w box width in points
     * @param h box height in points
     * @param accent outline colour
     * @return the composed node
     */
    private static DocumentNode graphNode(double w, double h, DocumentColor accent) {
        return new ShapeContainerBuilder()
                .name("GraphNode")
                .roundedRect(w, h, 4)
                .fillColor(SURFACE.withOpacity(0.85))
                .stroke(DocumentStroke.of(accent, 0.9))
                .position(rect(w * 0.55, 3, accent.withOpacity(0.55)), 8, 8,
                        LayerAlign.TOP_LEFT)
                .position(rect(w * 0.36, 2.4, ON_DARK.withOpacity(0.22)), 8, 16,
                        LayerAlign.TOP_LEFT)
                .build();
    }

    /**
     * A leaf fragment box. A plain shape rather than a container: a
     * {@code ShapeContainerNode} requires at least one layer, and this one holds
     * nothing.
     *
     * @return the composed leaf
     */
    private static DocumentNode leafNode() {
        return new ShapeBuilder()
                .size(18, 16)
                .fillColor(SURFACE.withOpacity(0.9))
                .stroke(DocumentStroke.of(VIOLET.withOpacity(0.5), 0.7))
                .cornerRadius(DocumentCornerRadius.of(3))
                .build();
    }

    /** Faint measurement grid: this is a layout engine, so the paper has coordinates. */
    private static List<CanvasChild> grid() {
        List<CanvasChild> lines = new ArrayList<>();
        DocumentColor tint = GRID.withOpacity(0.5);
        for (double x = 80; x < CARD_WIDTH; x += 80) {
            lines.add(at(rect(0.6, CARD_HEIGHT, tint), x, 0));
        }
        for (double y = 80; y < CARD_HEIGHT; y += 80) {
            lines.add(at(rect(CARD_WIDTH, 0.6, tint), 0, y));
        }
        return lines;
    }

    private static DocumentNode logoBlock() {
        return new ShapeContainerBuilder()
                .name("Wordmark")
                .rectangle(340, 74)
                .fillColor(DocumentColor.rgba(0, 0, 0, 0))
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(icon("logo").node(330), -8, -4, LayerAlign.CENTER_LEFT)
                .build();
    }

    /**
     * The two wedges leaving the graph panel. Flat translucent fills: the paint
     * surface exposes solid colours, and a flat wedge reads cleaner at the size
     * a social card is actually viewed.
     *
     * <p>Each wedge stops short of its card's full height so the point tucks
     * behind the straight part of the border. Running it to the corner left a
     * sharp spike poking out past the rounded edge.</p>
     *
     * @return the composed wedges and their origin dots
     */
    private static List<CanvasChild> beams() {
        List<CanvasChild> out = new ArrayList<>();
        out.add(at(new PathBuilder()
                .name("BeamPdf")
                .size(112, 250)
                .moveTo(0.00, 0.34)
                .lineTo(1.00, 0.96)
                .lineTo(1.00, 0.04)
                .closePath()
                .fillColor(VIOLET.withOpacity(0.15))
                .build(), 670, 84));
        out.add(at(new PathBuilder()
                .name("BeamPptx")
                .size(112, 196)
                .moveTo(0.00, 0.72)
                .lineTo(1.00, 0.95)
                .lineTo(1.00, 0.05)
                .closePath()
                .fillColor(MINT.withOpacity(0.13))
                .build(), 670, 396));
        out.add(at(dot(8, VIOLET), 666, 246));
        out.add(at(dot(8, MINT), 666, 448));
        return out;
    }

    /**
     * One destination shape, holding a real rendered document.
     *
     * @param name canvas node name
     * @param width shape width in points
     * @param height shape height in points
     * @param outline outline colour
     * @param accent series colour for the chart
     * @return the composed output shape
     */
    private static DocumentNode outputCard(String name, double width, double height,
                                           DocumentColor outline, DocumentColor accent) {
        return new ShapeContainerBuilder()
                .name(name)
                .roundedRect(width, height, 6)
                .fillColor(DocumentColor.rgba(12, 16, 34, 236))
                .stroke(DocumentStroke.of(outline, 1.5))
                .position(contentBox(width - 2 * CARD_PAD, height - 2 * CARD_PAD,
                        documentBody(accent, width - 2 * CARD_PAD,
                                height - 2 * CARD_PAD - TEXT_BLOCK - proseHeight())),
                        CARD_PAD, CARD_PAD, LayerAlign.TOP_LEFT)
                .build();
    }

    /**
     * An invisible box that gives its child an explicit width to lay out
     * against.
     *
     * <p>A node positioned straight into a card is measured against the card,
     * not against the card minus its padding — which is how the chart ended up
     * touching the border on the right.</p>
     *
     * @param width content width, in points
     * @param height content height, in points
     * @param child the node to constrain
     * @return the composed box
     */
    private static DocumentNode contentBox(double width, double height, DocumentNode child) {
        return new ShapeContainerBuilder()
                .name("ContentBox")
                .rectangle(width, height)
                .fillColor(DocumentColor.rgba(0, 0, 0, 0))
                // Not CLIP_BOUNDS: a clip that could cut ink sends the whole
                // region through the PPTX raster fallback, and the deck would
                // stop being editable exactly where the document lives. The
                // content is sized to this box already, so nothing needs cutting.
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(child, 0, 0, LayerAlign.TOP_LEFT)
                .build();
    }

    /**
     * The document both outputs carry: same title, same line, same series.
     * Only the chart's height follows the box it lands in — which is what a
     * layout engine is for, and why the page and the slide can hold one
     * document without either being cropped.
     *
     * @param accent series colour for the chart
     * @param width content width, in points
     * @param chartHeight height available to the chart, in points
     * @return the composed body
     */
    private static DocumentNode documentBody(DocumentColor accent, double width,
                                            double chartHeight) {
        return new SectionBuilder()
                .spacing(7)
                .padding(DocumentInsets.zero())
                .addParagraph(p -> p.text(DOC_TITLE)
                        .textStyle(display(13, ON_DARK))
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p.text(DOC_LINE)
                        .textStyle(body(8.2, ON_DARK_MUTED))
                        .margin(DocumentInsets.zero()))
                .chart(revenueChart(chartHeight), chartStyle(accent))
                .add(prose(width))
                .build();
    }

    /**
     * The paragraph block under the chart: flat bars standing in for body text.
     *
     * <p>Real text at this size would be unreadable on a social card and would
     * invite the viewer to try; bars say "prose continues here" and stop.</p>
     *
     * @param width content width, in points
     * @return the composed block
     */
    private static DocumentNode prose(double width) {
        ShapeContainerBuilder block = new ShapeContainerBuilder()
                .name("Prose")
                .rectangle(width, proseHeight())
                .fillColor(DocumentColor.rgba(0, 0, 0, 0));
        for (int i = 0; i < BODY_LINES.length; i++) {
            block.position(rect(width * BODY_LINES[i], 4.2, ON_DARK.withOpacity(0.24)),
                    0, i * BODY_LINE_STEP, LayerAlign.TOP_LEFT);
        }
        return block.build();
    }

    /**
     * Height the prose block occupies, including the gap that separates it from
     * the chart above.
     *
     * @return height in points
     */
    private static double proseHeight() {
        return BODY_LINES.length * BODY_LINE_STEP;
    }

    /**
     * Four labelled quarters — small enough to read, real enough to be a chart.
     *
     * @param height drawn height in points
     * @return the chart spec
     */
    private static ChartSpec revenueChart(double height) {
        return ChartSpec.bar()
                .data(ChartData.builder()
                        .categories("Q1", "Q2", "Q3", "Q4")
                        .series("Revenue", 42, 58, 51, 74)
                        .build())
                .valueAxis(AxisSpec.builder().baselineAtZero(true).build())
                .showCategoryLabels(true)
                .legend(LegendPosition.NONE)
                .valueLabels(ValueLabelMode.NONE)
                .size(ChartSize.fixedHeight(height))
                .build();
    }

    private static ChartStyle chartStyle(DocumentColor accent) {
        return ChartStyle.builder()
                .seriesPaint(0, DocumentPaint.solid(accent))
                .barCornerRadius(DocumentCornerRadius.of(1.5))
                .barWidthRatio(0.55)
                .axisTextStyle(mono(7.5, ON_DARK_MUTED))
                .build();
    }

    private static CanvasChild at(DocumentNode node, double x, double y) {
        return new CanvasChild(node, x, y);
    }

    private static DocumentNode rect(double width, double height, DocumentColor fill) {
        return new ShapeBuilder().size(width, height).fillColor(fill).build();
    }

    private static DocumentNode dot(double diameter, DocumentColor fill) {
        return new EllipseBuilder().circle(diameter).fillColor(fill).build();
    }

    /**
     * A paragraph laid out inside a container rather than on the page canvas.
     *
     * <p>{@link #text} sizes its box by subtracting from {@link #CARD_WIDTH},
     * which collapses to nothing inside a narrower parent — the panel footer
     * silently rendered at zero width until this existed.</p>
     *
     * @param name node name
     * @param value the text
     * @param style text style
     * @param available width available inside the parent, in points
     * @return the composed paragraph
     */
    private static DocumentNode textIn(String name, String value, DocumentTextStyle style,
                                       double available) {
        return new ParagraphBuilder()
                .name(name)
                .text(value)
                .textStyle(style)
                .align(TextAlign.LEFT)
                .margin(DocumentInsets.zero())
                .build();
    }

    private static DocumentNode text(String name, String value, DocumentTextStyle style,
                                     double width) {
        return new ParagraphBuilder()
                .name(name)
                .text(value)
                .textStyle(style)
                .align(TextAlign.LEFT)
                .lineSpacing(1.25)
                .margin(new DocumentInsets(0, CARD_WIDTH - width, 0, 0))
                .build();
    }

    /**
     * Bold display type. The weight comes from the decoration, not from a
     * {@code *_BOLD} font name: the standard-14 style variants are family
     * aliases, so naming one selects the family and leaves the face at regular.
     */
    private static DocumentTextStyle display(double size, DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .decoration(DocumentTextDecoration.BOLD)
                .size(size)
                .color(color)
                .build();
    }

    private static DocumentTextStyle body(double size, DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(size)
                .color(color)
                .build();
    }

    private static DocumentTextStyle mono(double size, DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(FontName.COURIER)
                .size(size)
                .color(color)
                .build();
    }

    private static SvgIcon icon(String name) {
        try (InputStream in = Objects.requireNonNull(
                SocialCardExample.class.getResourceAsStream("/showcase/" + name + ".svg"),
                "showcase icon missing: " + name)) {
            return SvgIcon.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("failed to load showcase icon: " + name, e);
        }
    }
}
