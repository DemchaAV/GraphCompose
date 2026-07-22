package com.demcha.examples.flagships;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageMarginRule;
import com.demcha.compose.document.chart.AxisSpec;
import com.demcha.compose.document.chart.ChartData;
import com.demcha.compose.document.chart.ChartSize;
import com.demcha.compose.document.chart.ChartSpec;
import com.demcha.compose.document.chart.ChartStyle;
import com.demcha.compose.document.chart.LegendPosition;
import com.demcha.compose.document.chart.LineInterpolation;
import com.demcha.compose.document.chart.NumberFormatSpec;
import com.demcha.compose.document.chart.ValueLabelMode;
import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.CanvasChild;
import com.demcha.compose.document.node.CanvasLayerNode;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.document.output.DocumentPageNumbering;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * Parallel 2.0 release concept for the GraphCompose README hero and engine
 * capability deck. The existing {@link EngineDeckExample} remains the active
 * release asset; this class deliberately writes {@code *-v2} files so both
 * designs can be reviewed side by side before any release-flow switch.
 *
 * <p>The first page is a 16:9 full-bleed banner shared byte-for-layout with
 * {@link #renderBannerImage(int)}. The remaining pages explain the 2.0 module
 * split, the two renderer lanes, dependency choices, and the repository's
 * bundled comparative reference snapshot. Every panel, connector, SVG icon
 * and chart is composed through the canonical document DSL.</p>
 *
 * <p>Benchmark wording is intentionally conservative. The bundled JSON is a
 * dated single-machine reference run without hardware or commit provenance;
 * allocation values are average thread-allocated MiB per render, not peak
 * heap, and scaling iteration metadata is absent from the historical file.
 * A final 2.0 release asset should refresh both data and provenance on the
 * release commit.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.0.0
 */
public final class EngineDeckV2Example {

    /** Widescreen page shared by the PNG banner and all deck pages. */
    public static final double PAGE_WIDTH = 960;
    /** Widescreen page shared by the PNG banner and all deck pages. */
    public static final double PAGE_HEIGHT = 540;

    private static final double BODY_LEFT = 42;
    private static final double BODY_RIGHT = 42;
    private static final double BODY_TOP = 34;
    private static final double BODY_BOTTOM = 28;
    private static final double BODY_WIDTH = PAGE_WIDTH - BODY_LEFT - BODY_RIGHT;

    // Brand / dark surfaces.
    private static final DocumentColor NIGHT = DocumentColor.rgb(9, 12, 27);
    private static final DocumentColor NIGHT_VIOLET = DocumentColor.rgb(31, 21, 61);
    private static final DocumentColor DARK_CARD = DocumentColor.rgb(20, 27, 51);
    private static final DocumentColor DARK_CARD_2 = DocumentColor.rgb(27, 34, 64);
    private static final DocumentColor DARK_LINE = DocumentColor.rgb(64, 67, 108);
    private static final DocumentColor ON_DARK = DocumentColor.rgb(245, 247, 252);
    private static final DocumentColor ON_DARK_MUTED = DocumentColor.rgb(177, 185, 207);

    // Accent family.
    private static final DocumentColor VIOLET = DocumentColor.rgb(124, 92, 252);
    private static final DocumentColor VIOLET_DEEP = DocumentColor.rgb(100, 53, 232);
    private static final DocumentColor VIOLET_LIGHT = DocumentColor.rgb(181, 159, 255);
    private static final DocumentColor BLUE = DocumentColor.rgb(78, 161, 255);
    private static final DocumentColor MINT = DocumentColor.rgb(55, 214, 161);
    private static final DocumentColor AMBER = DocumentColor.rgb(230, 160, 68);

    // Light pages.
    private static final DocumentColor PAPER = DocumentColor.rgb(246, 248, 252);
    private static final DocumentColor WHITE = DocumentColor.WHITE;
    private static final DocumentColor INK = DocumentColor.rgb(24, 29, 48);
    private static final DocumentColor BODY = DocumentColor.rgb(70, 77, 101);
    private static final DocumentColor MUTED = DocumentColor.rgb(116, 124, 148);
    private static final DocumentColor LINE = DocumentColor.rgb(221, 226, 238);
    private static final DocumentColor PALE_VIOLET = DocumentColor.rgb(239, 235, 255);
    private static final DocumentColor PALE_BLUE = DocumentColor.rgb(232, 243, 255);
    private static final DocumentColor PALE_MINT = DocumentColor.rgb(230, 249, 242);

    private static final String VERSION = loadVersion();

    private EngineDeckV2Example() {
    }

    /**
     * Builds the five-page widescreen v2 deck.
     *
     * @return generated PDF path under {@code target/generated-pdfs/flagships}
     * @throws Exception when composition or rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("flagships", "engine-deck-v2.pdf");
        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                .pageBackground(PAPER)
                .margin((float) BODY_TOP, (float) BODY_RIGHT, (float) BODY_BOTTOM, (float) BODY_LEFT)
                .create()) {
            compose(document);
            document.buildPdf();
        }
        return outputFile;
    }

    /**
     * Renders page one directly to a widescreen image without a PDF round trip.
     *
     * @param dpi raster resolution in dots per inch
     * @return rendered banner image
     * @throws Exception when composition or rendering fails
     */
    public static BufferedImage renderBannerImage(int dpi) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                .pageBackground(NIGHT)
                .margin(DocumentInsets.zero())
                .create()) {
            composeBanner(document);
            return document.toImage(0, dpi);
        }
    }

    /**
     * Generates a vector PDF containing only the v2 banner page.
     *
     * @return generated single-page PDF path
     * @throws Exception when composition or rendering fails
     */
    public static Path generateBanner() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("flagships", "engine-banner-v2.pdf");
        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                .pageBackground(NIGHT)
                .margin(DocumentInsets.zero())
                .create()) {
            composeBanner(document);
            document.buildPdf();
        }
        return outputFile;
    }

    /**
     * Composes the exact deck used by {@link #generate()} so tests can exercise
     * the shipped layout without duplicating page construction.
     *
     * @param document open document session configured for 960 x 540 pages
     */
    static void compose(DocumentSession document) {
        EngineDeckData.BenchRun bench = EngineDeckData.loadBench();
        document.pageMargins(List.of(PageMarginRule.page(1, DocumentInsets.zero())));
        document.metadata(deckMetadata());
        document.chrome().footer(DocumentHeaderFooter.builder()
                .rightText("{page} / {pages}")
                .fontSize(8f)
                .textColor(VIOLET_DEEP)
                .numbering(DocumentPageNumbering.builder()
                        .countFrom(2)
                        .startAt(1)
                        .build())
                .build());

        document.pageFlow()
                .name("EngineDeckV2")
                .spacing(12)
                .add(heroScene())

                .addSection("Architecture", s -> architecturePage(s))

                .addPageBreak(b -> b.name("ToModules"))
                .addSection("Modules", s -> modulesPage(s))

                .addPageBreak(b -> b.name("ToReferenceSnapshot"))
                .addSection("ReferenceSnapshot", s -> referencePage(s, bench))

                .addPageBreak(b -> b.name("ToScalingProfile"))
                .addSection("ScalingProfile", s -> scalingPage(s, bench))
                .build();
    }

    private static void composeBanner(DocumentSession document) {
        document.metadata(DocumentMetadata.builder()
                .title("GraphCompose " + VERSION + " - module-first banner v2")
                .author("GraphCompose")
                .subject("Parallel release banner concept for the GraphCompose 2.0 module split")
                .keywords("graphcompose, java, document engine, modules, renderer, " + VERSION)
                .creator("GraphCompose Examples")
                .producer("GraphCompose")
                .build());
        document.pageFlow()
                .name("EngineBannerV2")
                .add(heroScene())
                .build();
    }

    private static DocumentMetadata deckMetadata() {
        return DocumentMetadata.builder()
                .title("GraphCompose " + VERSION + " - module-first engine deck")
                .author("GraphCompose")
                .subject("Lean core, opt-in render modules, canonical authoring and measured reference data")
                .keywords("graphcompose, java, pdf, docx, pptx, modules, renderer, benchmarks")
                .creator("GraphCompose Examples")
                .producer("GraphCompose")
                .build();
    }

    // ---------------------------------------------------------------------
    // Page 1 - shared full-bleed hero.
    // ---------------------------------------------------------------------

    private static DocumentNode heroScene() {
        List<CanvasChild> layers = new ArrayList<>();
        layers.add(at(gradientBackground(), 0, 0));
        layers.add(at(glow(440, VIOLET.withOpacity(0.13)), 635, -180));
        layers.add(at(glow(310, BLUE.withOpacity(0.09)), 765, 85));
        layers.addAll(heroGrid());

        layers.add(at(heroLogo(), 42, 24));
        layers.add(at(versionPill(), 742, 36));

        layers.add(at(canvasText("HeroKicker", "GRAPHCOMPOSE 2.0 / MODULE-FIRST",
                darkEyebrow(), TextAlign.LEFT, 420), 52, 126));
        layers.add(at(canvasText("HeroLine1", "Compose once.", heroTitle(), TextAlign.LEFT, 410), 52, 156));
        layers.add(at(canvasText("HeroLine2", "Render through modules.", heroAccentTitle(), TextAlign.LEFT, 330), 52, 202));
        layers.add(at(canvasText("HeroBody",
                "A lean Java document core with a semantic model, deterministic layout geometry "
                        + "and opt-in output backends.",
                heroBody(), TextAlign.LEFT, 410), 54, 262));
        layers.add(at(heroCoordinate(), 52, 333));

        layers.add(at(canvasText("GraphLabel", "THE 2.0 MODULE GRAPH",
                darkEyebrow(), TextAlign.LEFT, 690), 622, 112));
        layers.addAll(heroModuleConnectors());
        layers.add(at(heroCoreCard(), 646, 152));
        layers.add(at(heroModuleCard("pdf-file", "render-pdf", "fixed layout", MINT), 582, 330));
        layers.add(at(heroModuleCard("docx", "render-docx", "semantic", BLUE), 712, 330));
        layers.add(at(heroModuleCard("code", "backend SPI", "extension slot", VIOLET_LIGHT), 842, 330));

        String[][] proof = {
                {"graph-compose-core", "lean engine"},
                {"graph-compose", "drop-in PDF"},
                {"ServiceLoader SPI", "bring a renderer"},
                {"layoutSnapshot()", "geometry regression"}
        };
        for (int i = 0; i < proof.length; i++) {
            layers.add(at(heroProofChip(proof[i][0], proof[i][1]), 48 + i * 224, 457));
        }

        return new CanvasLayerNode("ModuleFirstHero", PAGE_WIDTH, PAGE_HEIGHT, layers,
                ClipPolicy.CLIP_BOUNDS, DocumentInsets.zero(), DocumentInsets.zero());
    }

    private static DocumentNode gradientBackground() {
        return new ShapeBuilder()
                .name("HeroGradient")
                .size(PAGE_WIDTH, PAGE_HEIGHT)
                .fill(new DocumentPaint.Linear(List.of(
                        new DocumentPaint.Stop(0.0, NIGHT),
                        new DocumentPaint.Stop(0.58, DocumentColor.rgb(17, 17, 39)),
                        new DocumentPaint.Stop(1.0, NIGHT_VIOLET)), 12.0))
                .build();
    }

    private static DocumentNode glow(double diameter, DocumentColor color) {
        return new EllipseBuilder()
                .name("HeroGlow")
                .circle(diameter)
                .fillColor(color)
                .build();
    }

    private static List<CanvasChild> heroGrid() {
        List<CanvasChild> grid = new ArrayList<>();
        DocumentColor gridColor = DocumentColor.rgba(150, 139, 250, 18);
        for (int i = 0; i < 7; i++) {
            grid.add(at(rect(420, 0.7, gridColor), 540, 92 + i * 58));
        }
        for (int i = 0; i < 8; i++) {
            grid.add(at(rect(0.7, 405, gridColor), 556 + i * 58, 82));
        }
        return grid;
    }

    private static DocumentNode versionPill() {
        String text = VERSION.startsWith("v") ? VERSION : "v" + VERSION;
        return new ShapeContainerBuilder()
                .name("VersionPillV2")
                .roundedRect(172, 32, 10)
                .fillColor(DocumentColor.rgba(124, 92, 252, 42))
                .stroke(DocumentStroke.of(VIOLET.withOpacity(0.9), 0.9))
                .center(new ParagraphBuilder()
                        .text(text)
                        .textStyle(darkMono(11.5, ON_DARK))
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero())
                        .build())
                .build();
    }

    private static DocumentNode heroLogo() {
        return new ShapeContainerBuilder()
                .name("HeroLogo")
                .rectangle(310, 72)
                .fillColor(DocumentColor.rgba(0, 0, 0, 0))
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(logo().node(300), -10, -4, LayerAlign.CENTER_LEFT)
                .build();
    }

    private static DocumentNode heroCoordinate() {
        return new ShapeContainerBuilder()
                .name("HeroCoordinate")
                .roundedRect(470, 52, 12)
                .fillColor(DocumentColor.rgba(7, 10, 24, 205))
                .stroke(DocumentStroke.of(DARK_LINE, 0.8))
                .position(icon("maven-light").node(20), 17, 0, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder()
                        .text("io.github.demchaav:graph-compose-core")
                        .textStyle(darkMono(10.5, ON_DARK))
                        .margin(DocumentInsets.zero())
                        .build(), 52, -7, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder()
                        .text("+ choose a renderer module")
                        .textStyle(darkBody(9.3, MINT))
                        .margin(DocumentInsets.zero())
                        .build(), 52, 11, LayerAlign.CENTER_LEFT)
                .build();
    }

    private static List<CanvasChild> heroModuleConnectors() {
        List<CanvasChild> connectors = new ArrayList<>();
        DocumentColor connector = DocumentColor.rgba(167, 139, 250, 150);
        connectors.add(at(rect(2, 52, connector), 765, 278));
        connectors.add(at(rect(260, 2, connector), 636, 305));
        connectors.add(at(rect(2, 25, connector), 635, 305));
        connectors.add(at(rect(2, 25, connector), 895, 305));
        connectors.add(at(dot(8, VIOLET), 762, 302));
        connectors.add(at(dot(6, VIOLET_LIGHT), 633, 303));
        connectors.add(at(dot(6, VIOLET_LIGHT), 893, 303));
        return connectors;
    }

    private static DocumentNode heroCoreCard() {
        return new ShapeContainerBuilder()
                .name("HeroCore")
                .roundedRect(240, 126, 18)
                .fillColor(DARK_CARD_2)
                .stroke(DocumentStroke.of(VIOLET, 1.2))
                .position(icon("layout-light").node(30), 22, -28, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder()
                        .text("graph-compose-core")
                        .textStyle(darkMono(12.5, ON_DARK))
                        .margin(DocumentInsets.zero())
                        .build(), 64, -31, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder()
                        .text("semantic graph")
                        .textStyle(darkBody(10.2, VIOLET_LIGHT))
                        .margin(DocumentInsets.zero())
                        .build(), 64, -9, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder()
                        .text("authoring · nodes · layout services")
                        .textStyle(darkBody(9.0, ON_DARK_MUTED))
                        .margin(DocumentInsets.zero())
                        .build(), 22, 25, LayerAlign.CENTER_LEFT)
                .position(statusPill("NO PDFBOX / POI"), 22, 45, LayerAlign.CENTER_LEFT)
                .build();
    }

    private static DocumentNode heroModuleCard(String iconName, String name, String lane,
                                               DocumentColor accent) {
        return new ShapeContainerBuilder()
                .name("HeroModule_" + name)
                .roundedRect(108, 66, 13)
                .fillColor(DARK_CARD)
                .stroke(DocumentStroke.of(accent.withOpacity(0.78), 0.9))
                .position(icon(iconName + "-light").node(18), 12, -13, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder()
                        .text(name)
                        .textStyle(darkMono(9.2, ON_DARK))
                        .margin(DocumentInsets.zero())
                        .build(), 37, -13, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder()
                        .text(lane)
                        .textStyle(darkBody(9.0, accent))
                        .margin(DocumentInsets.zero())
                        .build(), 12, 15, LayerAlign.CENTER_LEFT)
                .build();
    }

    private static DocumentNode statusPill(String label) {
        return new ShapeContainerBuilder()
                .name("Status_" + label)
                .roundedRect(150, 22, 7)
                .fillColor(DocumentColor.rgba(55, 214, 161, 24))
                .stroke(DocumentStroke.of(MINT.withOpacity(0.55), 0.6))
                .center(new ParagraphBuilder()
                        .text(label)
                        .textStyle(darkMono(8.6, MINT))
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero())
                        .build())
                .build();
    }

    private static DocumentNode heroProofChip(String title, String subtitle) {
        return new ShapeContainerBuilder()
                .name("HeroProof_" + title)
                .roundedRect(202, 54, 13)
                .fillColor(DocumentColor.rgba(255, 255, 255, 22))
                .stroke(DocumentStroke.of(DARK_LINE, 0.9))
                .position(dot(9, MINT), 16, -8, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder()
                        .text(title)
                        .textStyle(darkMono(10.8, ON_DARK))
                        .margin(DocumentInsets.zero())
                        .build(), 34, -9, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder()
                        .text(subtitle)
                        .textStyle(darkBody(9.5, ON_DARK_MUTED))
                        .margin(DocumentInsets.zero())
                        .build(), 34, 10, LayerAlign.CENTER_LEFT)
                .build();
    }

    // ---------------------------------------------------------------------
    // Page 2 - exact fixed-layout and semantic-export lanes.
    // ---------------------------------------------------------------------

    private static void architecturePage(SectionBuilder page) {
        page.padding(DocumentInsets.zero())
                .spacing(11);
        pageHeader(page, "01 / ARCHITECTURE", "Semantic at the core. Format at the edge.",
                "The canonical DSL produces one semantic DocumentGraph. Fixed-layout output resolves geometry; "
                        + "semantic exporters consume the graph directly.",
                "Architecture");
        page.add(architectureCanvas());
        page.addRow("ArchitectureProof", row -> {
            row.spacing(10).evenWeights();
            proofCard(row, "LEAN CORE", "No PDFBox, POI, zxing or template families in graph-compose-core.",
                    PALE_VIOLET, VIOLET);
            proofCard(row, "FIXED BACKEND SPI", "FixedLayoutBackendProvider and FontMetricsProvider are discovered by ServiceLoader.",
                    PALE_BLUE, BLUE);
            proofCard(row, "SEMANTIC EXPORT", "DOCX is a working POI exporter; PPTX is a slide-safe manifest skeleton in 2.0.",
                    PALE_MINT, MINT);
        });
        page.addParagraph(p -> pageNote(p, "The renderer boundary is explicit; Android or another backend is an extension opportunity, not a shipped 2.0 claim."));
    }

    private static DocumentNode architectureCanvas() {
        double w = BODY_WIDTH;
        double h = 244;
        List<CanvasChild> nodes = new ArrayList<>();
        nodes.add(at(roundedRect(w, h, 16, WHITE, LINE), 0, 0));

        nodes.add(at(laneLabel("AUTHOR", VIOLET), 24, 20));
        nodes.add(at(laneLabel("ONE SEMANTIC GRAPH", BLUE), 250, 20));
        nodes.add(at(laneLabel("TWO OUTPUT LANES", MINT), 524, 20));

        nodes.add(at(architectureCard("dsl", "GraphCompose.document(...)\nDocumentDsl + templates",
                "intent, not coordinates", 196, 78, PALE_VIOLET, VIOLET), 24, 68));
        nodes.add(at(architectureCard("code", "semantic DocumentGraph",
                "renderer-neutral nodes", 220, 78, PALE_BLUE, BLUE), 268, 68));

        nodes.add(at(rect(48, 2, VIOLET.withOpacity(0.7)), 220, 106));
        nodes.add(at(dot(7, VIOLET), 240.5, 103.5));

        nodes.add(at(rect(42, 2, BLUE.withOpacity(0.75)), 488, 106));
        nodes.add(at(rect(2, 116, BLUE.withOpacity(0.55)), 528, 62));
        nodes.add(at(rect(28, 2, BLUE.withOpacity(0.7)), 528, 75));
        nodes.add(at(rect(28, 2, MINT.withOpacity(0.7)), 528, 163));

        nodes.add(at(architectureCard("layout", "prepare · paginate · place",
                "measured LayoutGraph", 205, 62, PALE_VIOLET, VIOLET), 556, 48));
        nodes.add(at(architectureCard("docx", "SemanticBackend.export(...)\nDOCX bytes · PPTX manifest",
                "bypasses fixed geometry", 205, 62, PALE_MINT, MINT), 556, 136));

        nodes.add(at(rect(24, 2, VIOLET.withOpacity(0.7)), 761, 78));
        nodes.add(at(rect(24, 2, MINT.withOpacity(0.7)), 761, 166));
        nodes.add(at(endpoint("render-pdf", "PDF / custom fixed", VIOLET), 785, 48));
        nodes.add(at(endpoint("semantic", "DOCX + PPTX", MINT), 785, 136));

        nodes.add(at(canvasText("ArchitectureNote",
                "Deterministic geometry is testable before bytes are rendered through layoutSnapshot().",
                smallBody(), TextAlign.LEFT, 106), 24, 205));
        return new CanvasLayerNode("ArchitectureLanes", w, h, nodes,
                ClipPolicy.CLIP_BOUNDS, DocumentInsets.zero(), DocumentInsets.zero());
    }

    private static DocumentNode laneLabel(String text, DocumentColor color) {
        return new ParagraphBuilder()
                .text(text)
                .textStyle(mono(8.5, color, true))
                .margin(DocumentInsets.zero())
                .build();
    }

    private static DocumentNode architectureCard(String iconName, String title, String subtitle,
                                                 double w, double h, DocumentColor fill,
                                                 DocumentColor accent) {
        return new ShapeContainerBuilder()
                .name("ArchitectureCard_" + title)
                .roundedRect(w, h, 12)
                .fillColor(fill)
                .stroke(DocumentStroke.of(accent.withOpacity(0.55), 0.8))
                .position(icon(iconName).node(22), 14, -14, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder()
                        .text(title)
                        .textStyle(mono(9.2, INK, true))
                        .lineSpacing(1.15)
                        .margin(DocumentInsets.zero())
                        .build(), 47, -13, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder()
                        .text(subtitle)
                        .textStyle(body(8.4, MUTED))
                        .margin(DocumentInsets.zero())
                        .build(), 47, 20, LayerAlign.CENTER_LEFT)
                .build();
    }

    private static DocumentNode endpoint(String title, String subtitle, DocumentColor accent) {
        return new ShapeContainerBuilder()
                .name("Endpoint_" + title)
                .roundedRect(76, 62, 11)
                .fillColor(accent.withOpacity(0.10))
                .stroke(DocumentStroke.of(accent.withOpacity(0.75), 0.8))
                .position(new ParagraphBuilder().text(title).textStyle(mono(7.2, accent, true))
                        .align(TextAlign.CENTER).margin(DocumentInsets.zero()).build(), 0, -10, LayerAlign.CENTER)
                .position(new ParagraphBuilder().text(subtitle).textStyle(body(6.6, MUTED))
                        .align(TextAlign.CENTER).lineSpacing(1.05).margin(DocumentInsets.zero()).build(), 0, 13, LayerAlign.CENTER)
                .build();
    }

    // ---------------------------------------------------------------------
    // Page 3 - dependency choices and module map.
    // ---------------------------------------------------------------------

    private static void modulesPage(SectionBuilder page) {
        page.padding(DocumentInsets.zero()).spacing(11);
        pageHeader(page, "02 / PACKAGING", "Choose only what you need.",
                "Eight engine-train artifacts move in lockstep. Fonts and emoji keep independent version lines; "
                        + "heavy render and template dependencies are opt-in.",
                "Modules");

        page.addRow("DependencyRecipes", row -> {
            row.spacing(12).evenWeights();
            dependencyCard(row, "DROP-IN PDF", "graph-compose",
                    "Core + PDF renderer. Canonical callers keeping this coordinate retain PDF out of the box.",
                    VIOLET, "DEFAULT");
            dependencyCard(row, "LEAN / CUSTOM", "graph-compose-core",
                    "Authoring, semantic model and layout services. Add a renderer module only when needed.",
                    BLUE, "LEAN");
            dependencyCard(row, "BATTERIES INCLUDED", "graph-compose-bundle",
                    "Default PDF stack plus templates, bundled fonts and emoji. Office exporters stay opt-in.",
                    MINT, "BUNDLE");
        });

        page.add(moduleMatrix());
        page.addParagraph(p -> pageNote(p,
                "Accepted migration trade-off: built-in templates are no longer carried by graph-compose; add graph-compose-templates or use the bundle."));
    }

    private static void dependencyCard(RowBuilder row, String kicker, String coordinate,
                                       String description, DocumentColor accent, String badge) {
        row.addSection(s -> s
                .softPanel(WHITE, 14, 16)
                .stroke(DocumentStroke.of(LINE, 0.8))
                .accentTop(accent, 3)
                .spacing(7)
                .addRich(r -> r
                        .style(kicker, mono(8.2, accent, true))
                        .style("   " + badge, mono(7.3, MUTED, true)))
                .addParagraph(p -> p.text(coordinate).textStyle(mono(11.0, INK, true))
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p.text(description).textStyle(body(9.0, BODY))
                        .lineSpacing(1.35).margin(DocumentInsets.zero())));
    }

    private static DocumentNode moduleMatrix() {
        double w = BODY_WIDTH;
        double h = 180;
        List<CanvasChild> nodes = new ArrayList<>();
        nodes.add(at(roundedRect(w, h, 15, DARK_CARD, DARK_LINE), 0, 0));
        nodes.add(at(canvasText("MatrixKicker", "THE LOCKSTEP 2.0 TRAIN",
                darkEyebrow(), TextAlign.LEFT, 610), 20, 17));
        nodes.add(at(canvasText("MatrixBody",
                "One version, smaller dependency trees, explicit format boundaries.",
                darkBody(9.2, ON_DARK_MUTED), TextAlign.LEFT, 410), 20, 39));

        String[][] modules = {
                {"core", "authoring + model"},
                {"render-pdf", "fixed PDF"},
                {"render-docx", "semantic DOCX"},
                {"render-pptx", "manifest only"},
                {"templates", "opt-in presets"},
                {"testing", "snapshot tools"},
                {"graph-compose", "PDF wrapper"},
                {"bundle", "PDF + templates + assets"}
        };
        DocumentColor[] accents = {VIOLET, MINT, BLUE, AMBER, VIOLET_LIGHT, BLUE, MINT, VIOLET};
        for (int i = 0; i < modules.length; i++) {
            double x = 20 + (i % 4) * 213;
            double y = 77 + (i / 4) * 50;
            nodes.add(at(matrixModule(modules[i][0], modules[i][1], accents[i]), x, y));
        }
        return new CanvasLayerNode("ModuleMatrix", w, h, nodes,
                ClipPolicy.CLIP_BOUNDS, DocumentInsets.zero(), DocumentInsets.zero());
    }

    private static DocumentNode matrixModule(String name, String description, DocumentColor accent) {
        return new ShapeContainerBuilder()
                .name("MatrixModule_" + name)
                .roundedRect(195, 38, 9)
                .fillColor(DocumentColor.rgba(255, 255, 255, 10))
                .stroke(DocumentStroke.of(accent.withOpacity(0.48), 0.7))
                .position(dot(7, accent), 12, -7, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder().text(name).textStyle(darkMono(8.8, ON_DARK))
                        .margin(DocumentInsets.zero()).build(), 28, -8, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder().text(description).textStyle(darkBody(7.8, ON_DARK_MUTED))
                        .margin(DocumentInsets.zero()).build(), 28, 9, LayerAlign.CENTER_LEFT)
                .build();
    }

    // ---------------------------------------------------------------------
    // Pages 4-5 - benchmark reference data, honestly labelled.
    // ---------------------------------------------------------------------

    private static void referencePage(SectionBuilder page, EngineDeckData.BenchRun bench) {
        String snapshotDate = snapshotDate(bench);
        page.padding(DocumentInsets.zero()).spacing(9);
        pageHeader(page, "03 / REFERENCE RUN", "Measured locally. Read with context.",
                "Equivalent 1000-row report fixtures from the bundled " + snapshotDate + " single-machine snapshot. "
                        + "These values are directional, not a final 2.0 marketing benchmark.",
                "Reference run");

        page.addRow("ReferenceStats", row -> {
            row.spacing(10).evenWeights();
            statCard(row, format(bench.timeMs("GraphCompose", 1000), " ms"),
                    "GraphCompose avg render", VIOLET, "1000 rows");
            statCard(row, format(bench.heapMb("GraphCompose", 1000), " MiB"),
                    "avg allocated / render", BLUE, "thread allocation");
            statCard(row, "40 / 200 / 1000", "report rows", MINT, "scaling fixtures");
            statCard(row, snapshotDate,
                    "snapshot date", AMBER, "provenance incomplete");
        });

        page.addRow("ReferenceCharts", row -> {
            row.spacing(12).evenWeights();
            row.addSection("LatencyCard", s -> chartCard(s,
                    "Average render latency at 1000 rows - ms",
                    EngineDeckCharts.timeChart(bench), EngineDeckCharts.groupedStyle()));
            row.addSection("AllocationCard", s -> chartCard(s,
                    "Average thread allocation per render - MiB",
                    allocationChart(bench), EngineDeckCharts.groupedStyle()));
        });

        page.addParagraph(p -> p
                .text("Scope: one developer machine; CPU, OS, JDK and commit were not stored in this historical file. "
                        + "The JSON stores 50/100 only for its small-invoice tier and omits scaling iteration metadata. "
                        + "Refresh both data and provenance on the final 2.0 commit before release claims.")
                .textStyle(body(7.9, MUTED))
                .lineSpacing(1.25)
                .margin(DocumentInsets.top(1)));
        page.addParagraph(p -> pageNote(p, "Reproduce: scripts/run-benchmarks.ps1 - Current docs: docs/operations/benchmarks.md"));
    }

    private static void scalingPage(SectionBuilder page, EngineDeckData.BenchRun bench) {
        double latencyRatio = bench.timeMs("iText 9", 1000) / bench.timeMs("GraphCompose", 1000);
        double allocationRatio = bench.heapMb("iText 9", 1000) / bench.heapMb("GraphCompose", 1000);
        double jasperGapPercent = 100.0
                * (bench.timeMs("JasperReports", 1000) - bench.timeMs("GraphCompose", 1000))
                / bench.timeMs("JasperReports", 1000);
        page.padding(DocumentInsets.zero()).spacing(9);
        pageHeader(page, "04 / SCALING PROFILE", "Watch the curve, not one headline number.",
                "The same local reference run across 40, 200 and 1000 rows. Allocation values are independent "
                        + "per-render measurements; they are not shares of a common heap.",
                "Scaling profile");

        page.addRow("ScalingCharts", row -> {
            row.spacing(12).evenWeights();
            row.addSection("ScalingLatency", s -> chartCard(s,
                    "Average render latency vs. report size - ms",
                    EngineDeckCharts.scalingLineChart(bench), EngineDeckCharts.lineStyle()));
            row.addSection("ScalingAllocation", s -> chartCard(s,
                    "Average allocation vs. report size - MiB / render",
                    allocationScalingChart(bench), EngineDeckCharts.areaStyle()));
        });

        page.addRow("ScalingNotes", row -> {
            row.spacing(10).evenWeights();
            insightCard(row, ratio(latencyRatio),
                    "iText 9 took this many times longer in the 1000-row fixture", VIOLET);
            insightCard(row, ratio(allocationRatio),
                    "iText 9 allocated this many times more in the same fixture", BLUE);
            insightCard(row, percentage(jasperGapPercent),
                    "latency gap vs JasperReports - inside the documented 5-10% local noise band", AMBER);
        });
        page.addParagraph(p -> p
                .rich(r -> r
                        .plain("What 2.0 can claim today: ")
                        .bold("modular packaging, renderer isolation and deterministic layout snapshots")
                        .plain(". Refresh performance evidence separately on the release commit."))
                .textStyle(body(8.4, BODY))
                .lineSpacing(1.3)
                .margin(DocumentInsets.zero()));
        page.addParagraph(p -> pageNote(p, "GraphCompose renders this deck itself: gradients, canvas layers, SVG, clipping and native charts."));
    }

    private static void statCard(RowBuilder row, String value, String label,
                                 DocumentColor accent, String note) {
        row.addSection(s -> s
                .softPanel(WHITE, 11, 12)
                .stroke(DocumentStroke.of(LINE, 0.8))
                .accentLeft(accent, 3)
                .spacing(3)
                .addParagraph(p -> p.text(value).textStyle(display(16, INK))
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p.text(label).textStyle(body(8.3, BODY))
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p.text(note).textStyle(mono(6.9, accent, true))
                        .margin(DocumentInsets.zero())));
    }

    private static void insightCard(RowBuilder row, String value, String text, DocumentColor accent) {
        row.addSection(s -> s
                .softPanel(accent.withOpacity(0.09), 11, 12)
                .stroke(DocumentStroke.of(accent.withOpacity(0.42), 0.7))
                .spacing(3)
                .addParagraph(p -> p.text(value).textStyle(display(15, accent))
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p.text(text).textStyle(body(8.3, BODY))
                        .lineSpacing(1.2).margin(DocumentInsets.zero())));
    }

    private static void chartCard(SectionBuilder section, String title,
                                  ChartSpec spec, ChartStyle style) {
        section.softPanel(WHITE, 12, 13)
                .stroke(DocumentStroke.of(LINE, 0.8))
                .spacing(6)
                .addParagraph(p -> p.text(title).textStyle(body(9.2, INK, true))
                        .margin(DocumentInsets.zero()))
                .chart(spec, style);
    }

    private static ChartSpec allocationChart(EngineDeckData.BenchRun bench) {
        return ChartSpec.bar()
                .data(ChartData.builder()
                        .categories("1000-row report")
                        .series("GraphCompose", bench.heapMb("GraphCompose", 1000))
                        .series("iText 9", bench.heapMb("iText 9", 1000))
                        .series("JasperReports", bench.heapMb("JasperReports", 1000))
                        .build())
                .valueAxis(AxisSpec.builder().baselineAtZero(true)
                        .format(NumberFormatSpec.pattern("#,##0.0").withSuffix(" MiB")).build())
                .legend(LegendPosition.BOTTOM)
                .valueLabels(ValueLabelMode.OUTSIDE)
                .size(ChartSize.aspectRatio(16, 8))
                .build();
    }

    private static ChartSpec allocationScalingChart(EngineDeckData.BenchRun bench) {
        var data = ChartData.builder().categories("40", "200", "1000");
        for (String library : EngineDeckData.LIBS) {
            double[] values = new double[EngineDeckData.SIZES.length];
            for (int i = 0; i < EngineDeckData.SIZES.length; i++) {
                values[i] = bench.heapMb(library, EngineDeckData.SIZES[i]);
            }
            data = data.series(library, values);
        }
        return ChartSpec.line()
                .data(data.build())
                .interpolation(LineInterpolation.SMOOTH)
                .area(true)
                .valueAxis(AxisSpec.builder().baselineAtZero(true)
                        .format(NumberFormatSpec.pattern("#,##0").withSuffix(" MiB")).build())
                .legend(LegendPosition.BOTTOM)
                .size(ChartSize.aspectRatio(16, 6.5))
                .build();
    }

    // ---------------------------------------------------------------------
    // Shared page components and drawing helpers.
    // ---------------------------------------------------------------------

    private static void pageHeader(SectionBuilder page, String eyebrow, String title,
                                   String subtitle, String bookmark) {
        page.bookmark(new DocumentBookmarkOptions(bookmark, 0))
                .addParagraph(p -> p.text(eyebrow).textStyle(mono(8.2, VIOLET_DEEP, true))
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p.text(title).textStyle(display(23, INK))
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p.text(subtitle).textStyle(body(9.4, BODY))
                        .lineSpacing(1.3).margin(DocumentInsets.zero()));
    }

    private static void proofCard(RowBuilder row, String title, String text,
                                  DocumentColor fill, DocumentColor accent) {
        row.addSection(s -> s
                .softPanel(fill, 11, 11)
                .stroke(DocumentStroke.of(accent.withOpacity(0.35), 0.7))
                .spacing(4)
                .addParagraph(p -> p.text(title).textStyle(mono(7.8, accent, true))
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p.text(text).textStyle(body(8.1, BODY))
                        .lineSpacing(1.2).margin(DocumentInsets.zero())));
    }

    private static ParagraphBuilder pageNote(ParagraphBuilder paragraph, String text) {
        return paragraph
                .text(text)
                .textStyle(body(7.7, MUTED))
                .margin(DocumentInsets.zero());
    }

    private static DocumentNode canvasText(String name, String text, DocumentTextStyle style,
                                           TextAlign align, double rightMargin) {
        return new ParagraphBuilder()
                .name(name)
                .text(text)
                .textStyle(style)
                .align(align)
                .lineSpacing(1.18)
                .margin(new DocumentInsets(0, rightMargin, 0, 0))
                .build();
    }

    private static CanvasChild at(DocumentNode node, double x, double y) {
        return new CanvasChild(node, x, y);
    }

    private static DocumentNode rect(double width, double height, DocumentColor fill) {
        return new ShapeBuilder().size(width, height).fillColor(fill).build();
    }

    private static DocumentNode roundedRect(double width, double height, double radius,
                                            DocumentColor fill, DocumentColor stroke) {
        return new ShapeBuilder()
                .size(width, height)
                .fillColor(fill)
                .stroke(DocumentStroke.of(stroke, 0.8))
                .cornerRadius(DocumentCornerRadius.of(radius))
                .build();
    }

    private static DocumentNode dot(double diameter, DocumentColor fill) {
        return new EllipseBuilder().circle(diameter).fillColor(fill).build();
    }

    private static String format(double value, String suffix) {
        return String.format(Locale.ROOT, "%.1f%s", value, suffix);
    }

    private static String ratio(double value) {
        return String.format(Locale.ROOT, "%.2fx", value);
    }

    private static String percentage(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }

    private static String snapshotDate(EngineDeckData.BenchRun bench) {
        String timestamp = bench.timestamp();
        return timestamp.substring(0, Math.min(10, timestamp.length()));
    }

    private static String loadVersion() {
        Properties banner = new Properties();
        try (InputStream in = EngineDeckV2Example.class.getResourceAsStream("/banner.properties")) {
            if (in != null) {
                banner.load(in);
            }
        } catch (IOException ignored) {
            // Fall through to the development label below.
        }
        String value = banner.getProperty("version");
        return value == null || value.isBlank() || value.startsWith("@") ? "dev" : value.trim();
    }

    private static SvgIcon logo() {
        return icon("logo");
    }

    private static SvgIcon icon(String name) {
        try (InputStream in = Objects.requireNonNull(
                EngineDeckV2Example.class.getResourceAsStream("/showcase/" + name + ".svg"),
                "showcase icon missing: " + name)) {
            return SvgIcon.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("failed to load showcase icon: " + name, e);
        }
    }

    private static DocumentTextStyle display(double size, DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(size)
                .color(color)
                .build();
    }

    private static DocumentTextStyle heroTitle() {
        return display(35, ON_DARK);
    }

    private static DocumentTextStyle heroAccentTitle() {
        return display(35, VIOLET_LIGHT);
    }

    private static DocumentTextStyle heroBody() {
        return body(13.0, ON_DARK_MUTED);
    }

    private static DocumentTextStyle darkEyebrow() {
        return mono(8.8, MINT, true);
    }

    private static DocumentTextStyle darkMono(double size, DocumentColor color) {
        return mono(size, color, false);
    }

    private static DocumentTextStyle darkBody(double size, DocumentColor color) {
        return body(size, color);
    }

    private static DocumentTextStyle smallBody() {
        return body(8.3, BODY);
    }

    private static DocumentTextStyle body(double size, DocumentColor color) {
        return body(size, color, false);
    }

    private static DocumentTextStyle body(double size, DocumentColor color, boolean bold) {
        DocumentTextStyle.Builder builder = DocumentTextStyle.builder()
                .fontName(bold ? FontName.HELVETICA_BOLD : FontName.HELVETICA)
                .size(size)
                .color(color);
        return builder.build();
    }

    private static DocumentTextStyle mono(double size, DocumentColor color, boolean bold) {
        DocumentTextStyle.Builder builder = DocumentTextStyle.builder()
                .fontName(bold ? FontName.COURIER_BOLD : FontName.COURIER)
                .size(size)
                .color(color);
        return builder.build();
    }

    /**
     * CLI entry point for the parallel v2 deck.
     *
     * @param args ignored
     * @throws Exception when rendering fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
