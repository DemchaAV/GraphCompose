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
import com.demcha.compose.document.chart.NumberFormatSpec;
import com.demcha.compose.document.chart.PointMarker;
import com.demcha.compose.document.chart.SliceLabelMode;
import com.demcha.compose.document.chart.ValueLabelMode;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
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
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Flagship "what is GraphCompose" capability deck — a multi-page landscape
 * brand document the engine renders <em>about itself</em>. Page&nbsp;1 is a
 * full-width banner infographic — logo and tagline over a four-step flow
 * (DSL code → layout engine → output backends → real rendered documents) and
 * a capability strip; the following pages explain the authoring pipeline,
 * position GraphCompose against the field with native vector charts, and
 * close with a chart gallery — every glyph, panel, chart, and embedded
 * thumbnail produced by the same canonical DSL the rest of the examples use.
 *
 * <p>The "Real PDF Output" column embeds rasterised thumbnails of three real
 * example documents (a CV, an invoice, a business report) bundled under
 * {@code /showcase/thumbs/}, so the banner literally shows the engine's own
 * output.</p>
 *
 * <p>Sibling to {@link EngineShowcase}, the single-page portrait brand promo
 * that sources the README hero image — this deck is the landscape,
 * multi-page, chart-driven companion. The two are intentionally separate
 * artifacts with separate output files ({@code engine-deck.pdf} here).</p>
 *
 * <p>The whole document is landscape: the engine renders one orientation per
 * {@code buildPdf()} call (the layout pass binds a single page canvas), so a
 * mixed portrait/landscape document is not expressible — landscape suits the
 * wide hero and the side-by-side comparison charts anyway.</p>
 *
 * <p>The branding follows the supplied {@code logo.svg}: a violet identity
 * ({@code #7C5CFC} / {@code #6128D9}) on a near-black panel, with the
 * monochrome feature icons sitting on light chips where they read cleanly.
 * Icons are read from {@code /showcase/*.svg} classpath resources via
 * {@link SvgIcon#parse(String)} — native PDF Béziers, no rasterization.</p>
 *
 * <p>The comparison pages render <b>real measured data</b>: at build time a
 * snapshot of the repository's comparative benchmark
 * ({@code target/benchmarks/comparative/latest.json}) is bundled under
 * {@code resources/benchmarks/}, and the example loads it at render time and
 * draws the table and charts straight from it — the engine demonstrating
 * itself on its own numbers. Refresh by re-running the benchmark suite and
 * re-copying the result file (see {@code loadBench()}). Numbers are
 * single-machine and vary by hardware; the page carries the run metadata.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public final class EngineDeckExample {

    private static final String VERSION = "1.8.0";
    private static final String CODENAME = "illustrative";

    // ── Violet brand identity (from logo.svg) ──────────────────────────────
    private static final DocumentColor HERO_BG = DocumentColor.rgb(18, 18, 33);
    private static final DocumentColor VIOLET = DocumentColor.rgb(124, 92, 252);
    private static final DocumentColor VIOLET_DEEP = DocumentColor.rgb(97, 40, 217);
    private static final DocumentColor VIOLET_LIGHT = DocumentColor.rgb(167, 139, 250);

    // ── Neutrals ───────────────────────────────────────────────────────────
    private static final DocumentColor INK = DocumentColor.rgb(28, 30, 46);
    private static final DocumentColor MUTED = DocumentColor.rgb(112, 116, 132);
    private static final DocumentColor BODY = DocumentColor.rgb(64, 68, 84);
    private static final DocumentColor SURFACE = DocumentColor.rgb(247, 248, 252);
    private static final DocumentColor SURFACE_LINE = DocumentColor.rgb(226, 229, 239);
    private static final DocumentColor HERO_TEXT = DocumentColor.rgb(208, 211, 226);

    // ── Banner (page 1) ──────────────────────────────────────────────────────
    private static final DocumentColor CODE_BG = DocumentColor.rgb(12, 12, 22);
    private static final DocumentColor CARD_DARK = DocumentColor.rgb(36, 35, 60);
    private static final DocumentColor RULE_DARK = DocumentColor.rgb(58, 56, 92);
    private static final DocumentColor GREEN = DocumentColor.rgb(46, 196, 138);
    private static final DocumentColor CODE_TXT = DocumentColor.rgb(178, 182, 202);
    private static final DocumentColor CODE_STR = DocumentColor.rgb(120, 204, 170);
    private static final DocumentColor CODE_FN = DocumentColor.rgb(150, 190, 255);
    private static final DocumentColor ON_DARK = DocumentColor.rgb(230, 232, 242);
    private static final DocumentColor ON_DARK_MUTED = DocumentColor.rgb(152, 156, 178);

    // ── Comparison series ────────────────────────────────────────────────────
    private static final DocumentColor SLATE = DocumentColor.rgb(118, 126, 148);
    private static final DocumentColor AMBER = DocumentColor.rgb(224, 158, 72);

    private EngineDeckExample() {
    }

    /**
     * Builds the landscape showcase PDF.
     *
     * @return the generated file path
     * @throws Exception when rendering or icon IO fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("flagships", "engine-deck.pdf");
        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4.landscape())
                .margin(16, 16, 30, 16)
                .create()) {
            compose(document);
            document.buildPdf();
        }
        return outputFile;
    }

    /**
     * Composes the four deck pages onto a session — shared by {@link #generate()}
     * and the layout snapshot test, so the test guards the very layout we ship.
     * Page size and margin live on the session builder (see {@code generate()}).
     */
    static void compose(DocumentSession document) {
        BenchRun bench = loadBench();
        document.metadata(DocumentMetadata.builder()
                    .title("GraphCompose " + VERSION + " — " + CODENAME)
                    .author("GraphCompose")
                    .subject("What the GraphCompose document engine is — rendered by the engine itself")
                    .keywords("graphcompose, pdf, java, dsl, charts, svg, showcase, " + VERSION)
                    .creator("GraphCompose Examples")
                    .producer("GraphCompose")
                    .build());

            document.footer(DocumentHeaderFooter.builder()
                    .zone(DocumentHeaderFooterZone.FOOTER)
                    .leftText("GraphCompose · v" + VERSION + " “" + CODENAME + "”")
                    .rightText("Page {page} of {pages}")
                    .fontSize(8.5f)
                    .textColor(MUTED)
                    .showSeparator(true)
                    .separatorColor(VIOLET_LIGHT)
                    .separatorThickness(0.5f)
                    .build());

            document.pageFlow()
                    .name("EngineShowcase")
                    .spacing(16)

                    // ═════════ PAGE 1 — banner ═════════
                    .addSection("Banner", EngineDeckExample::banner)

                    // ═════════ PAGE 2 — how it works ═════════
                    .addPageBreak(b -> b.name("ToHowItWorks"))
                    .addSection("HowKicker", s -> kicker(s, "HOW IT WORKS",
                            "From one Java file to a designed PDF"))
                    .addSection("HowBody", s -> s
                            .padding(DocumentInsets.zero())
                            .addParagraph(p -> p
                                    .rich(rich -> rich
                                            .plain("You describe the document semantically — ")
                                            .bold("sections, rows, tables, charts, shapes, layers")
                                            .plain(" — and the engine resolves the rest: it ")
                                            .accent("measures", VIOLET_DEEP)
                                            .plain(" every node twice, ")
                                            .accent("paginates", VIOLET_DEEP)
                                            .plain(" the flow row-by-row, and ")
                                            .accent("renders", VIOLET_DEEP)
                                            .plain(" through an isolated PDFBox backend. No manual coordinates, no XML templates."))
                                    .lineSpacing(1.55)))
                    .addRow("Pipeline", row -> {
                        row.spacing(8).weights(1, 0.14, 1, 0.14, 1, 0.14, 1);
                        pipelineStep(row, "1", "dsl", "AUTHOR", "Fluent DSL describes intent.");
                        pipeArrow(row);
                        pipelineStep(row, "2", "layout", "MEASURE", "Two-pass geometry, every node.");
                        pipeArrow(row);
                        pipelineStep(row, "3", "page-break", "PAGINATE", "Split the flow across pages.");
                        pipeArrow(row);
                        pipelineStep(row, "4", "pdf-file", "RENDER", "PDFBox writes the bytes.");
                    })
                    .addRow("Proof", row -> {
                        row.spacing(16).evenWeights();
                        proofCard(row, "Deterministic",
                                "The same input renders the same bytes on every machine — layout is reproducible, "
                                        + "not best-effort.");
                        proofCard(row, "Regression-tested",
                                "layoutSnapshot() diffs the geometry and PdfVisualRegression pixel-tests fonts and "
                                        + "colour, right in your pull requests.");
                        proofCard(row, "Production-ready",
                                "An isolated PDFBox backend streams straight to an HTTP response — no temp files, "
                                        + "no manual coordinates.");
                    })

                    // ═════════ PAGE 3 — measured comparison (real data) ═════════
                    .addPageBreak(b -> b.name("ToComparison"))
                    .addSection("CmpKicker", s -> kicker(s, "BENCHMARKED",
                            "GraphCompose vs. the field"))
                    .addSection("CmpBody", s -> s
                            .padding(DocumentInsets.zero())
                            .addParagraph(p -> p
                                    .text("The same documents through three engines — render time and peak heap, "
                                            + "measured by this repository's own harness. The table and charts below "
                                            + "are drawn by GraphCompose straight from the result file.")
                                    .textStyle(body())
                                    .lineSpacing(1.45)
                                    .margin(DocumentInsets.bottom(2))))
                    .addTable(t -> benchTable(bench, t))
                    .addRow("CmpCharts", row -> {
                        row.spacing(16).evenWeights();
                        row.addSection("TimeCard", s -> chartCard(s,
                                "Render time at 1000 rows — ms (lower is faster)",
                                timeChart(bench), groupedStyle()));
                        row.addSection("MemCard", s -> chartCard(s,
                                "Peak heap at 1000 rows — MB (lower is lighter)",
                                memoryChart(bench), groupedStyle()));
                    })
                    .addParagraph(p -> p
                            .text("Measured " + bench.timestamp() + " · " + bench.warmup() + " warmup / "
                                    + bench.measure() + " measurement iterations, single machine — numbers vary by "
                                    + "hardware and document. Reproduce: scripts/run-benchmarks.ps1 · "
                                    + "see docs/operations/benchmarks.md.")
                            .textStyle(caption())
                            .lineSpacing(1.4)
                            .margin(DocumentInsets.top(6)))

                    // ═════════ PAGE 4 — how it scales (real data) ═════════
                    .addPageBreak(b -> b.name("ToScaling"))
                    .addSection("ScaleKicker", s -> kicker(s, "SCALING",
                            "How GraphCompose scales"))
                    .addSection("ScaleBody", s -> s
                            .padding(DocumentInsets.zero())
                            .addParagraph(p -> p
                                    .text("As the report grows from 40 to 1000 rows, the time lead over iText widens "
                                            + "and GraphCompose stays markedly lighter on memory than both rivals — "
                                            + "every series below read from the same benchmark file.")
                                    .textStyle(body())
                                    .lineSpacing(1.45)
                                    .margin(DocumentInsets.bottom(2))))
                    .addRow("ScaleTop", row -> {
                        row.spacing(16).evenWeights();
                        row.addSection("ScaleLine", s -> chartCard(s,
                                "Render time vs. report size — ms", scalingLineChart(bench), lineStyle()));
                        row.addSection("MemArea", s -> chartCard(s,
                                "Peak heap vs. report size — MB", memoryAreaChart(bench), areaStyle()));
                    })
                    .addRow("ScaleBottom", row -> {
                        row.spacing(16).evenWeights();
                        row.addSection("ShareDonut", s -> chartCard(s,
                                "Memory at 1000 rows — share of total", memoryShareDonut(bench), donutStyle()));
                        row.addSection("ThroughCard", s -> chartCard(s,
                                "Throughput at 1000 rows — docs/sec (higher is better)",
                                throughputChart(bench), groupedStyle()));
                    })
                    .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Page 1 — full-page banner: a 4-step infographic (DSL → engine → backends
    // → real output) on a rounded dark violet panel, capped by a capability
    // strip. The columns are a flow row; the horizontal micro-content (engine
    // icon grid, dark backend cards, document cascade, chips) is composed as
    // fixed-size positioned nodes to stay within the no-nested-row rule.
    // ─────────────────────────────────────────────────────────────────────────

    private static void banner(SectionBuilder s) {
        s.softPanel(HERO_BG, 12, 30)
                .spacing(16)
                // Logo + version badge on one line; tagline below.
                .add(brandLine())
                .addParagraph(p -> p
                        .text("Open-source Java library for generating structured business "
                                + "PDF documents with a declarative DSL.")
                        .textStyle(tagline()).lineSpacing(1.3).margin(DocumentInsets.top(4)))
                .addShape(sh -> sh.size(749, 1.2).fillColor(RULE_DARK).margin(DocumentInsets.top(8)))
                .addRow("Flow", row -> {
                    row.spacing(6).weights(1.9, 0.18, 2.2, 0.18, 1.3, 0.18, 1.95);
                    row.addSection("Step1", EngineDeckExample::stepDsl);
                    row.addSection("Arrow1", EngineDeckExample::arrowCell);
                    row.addSection("Step2", EngineDeckExample::stepEngine);
                    row.addSection("Arrow2", EngineDeckExample::arrowCell);
                    row.addSection("Step3", EngineDeckExample::stepBackends);
                    row.addSection("Arrow3", EngineDeckExample::arrowCell);
                    row.addSection("Step4", EngineDeckExample::stepDocs);
                })
                .addShape(sh -> sh.size(749, 1.2).fillColor(RULE_DARK).margin(DocumentInsets.top(6)))
                .addRow("BannerChips", row -> {
                    row.spacing(22).evenWeights();
                    chip(row, "github", "Open Source");
                    chip(row, "maven", "Maven Central");
                    chip(row, "java", "Java 17+");
                    chip(row, "license", "MIT License");
                });
    }

    /** Logo with the version badge set right beside it; tagline goes below. */
    private static DocumentNode brandLine() {
        SvgIcon lg = logo();
        double logoW = 440, logoH = logoW / lg.aspectRatio();
        return new ShapeContainerBuilder().name("BrandLine")
                .rectangle(logoW + 230, logoH).fillColor(HERO_BG)
                // The wordmark SVG carries left/top padding; nudge it back onto
                // the content grid. Version stays centred beside the wordmark.
                .position(lg.node(logoW), -18, -6, LayerAlign.CENTER_LEFT)
                .position(versionBlock(), logoW - 14, -1, LayerAlign.CENTER_LEFT)
                .build();
    }

    /** Version pill ("v1.8.0") with the codename centred beside it as a tag. */
    private static DocumentNode versionBlock() {
        DocumentNode pill = new ShapeContainerBuilder().name("VerPill")
                .roundedRect(96, 30, 8).fillColor(VIOLET_DEEP)
                .center(new ParagraphBuilder().text("v" + VERSION)
                        .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA_BOLD)
                                .size(14).color(DocumentColor.WHITE).build())
                        .align(TextAlign.CENTER).margin(DocumentInsets.zero()).build())
                .build();
        DocumentNode codename = new ParagraphBuilder().text(CODENAME)
                .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA_OBLIQUE)
                        .size(14).color(GREEN).build())
                .margin(DocumentInsets.zero()).build();
        return new ShapeContainerBuilder().name("VersionBlock")
                .rectangle(212, 30).fillColor(HERO_BG)
                .position(pill, 0, 0, LayerAlign.CENTER_LEFT)
                .position(codename, 110, 0, LayerAlign.CENTER_LEFT)
                .build();
    }

    /** Column header: a violet step number inline with the title. */
    private static void colHeader(SectionBuilder s, String number, String title) {
        s.addRich(r -> r
                .style(number, DocumentTextStyle.builder().fontName(FontName.HELVETICA_BOLD)
                        .size(16).color(VIOLET).build())
                .style("   " + title, colTitle()));
    }

    private static void arrowCell(SectionBuilder s) {
        s.padding(DocumentInsets.zero())
                .addParagraph(p -> p.text(">")
                        .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA_BOLD)
                                .size(26).color(VIOLET).build())
                        .align(TextAlign.CENTER).margin(DocumentInsets.top(96)));
    }

    /** Column 1 — the DSL, shown as a dark code panel. */
    private static void stepDsl(SectionBuilder s) {
        s.spacing(10);
        colHeader(s, "1", "Java · Declarative DSL");
        // Real, compiling GraphCompose code (imports elided): Path + the canonical
        // document builder + a paragraph + buildPdf — exactly the Hello-world.
        s.addSection("CodePanel", c -> c.softPanel(CODE_BG, 8, 13).spacing(4)
                .addRich(r -> r.style("var", codeKw()).style(" out = ", codeBase()).style("Path.of", codeFn()).style("(", codeBase()).style("\"deck.pdf\"", codeStr()).style(");", codeBase()))
                .addRich(r -> r.style("try", codeKw()).style(" (var doc = ", codeBase()).style("GraphCompose", codeFn()))
                .addRich(r -> r.style("    .document", codeFn()).style("(out)", codeBase()))
                .addRich(r -> r.style("    .pageSize", codeFn()).style("(", codeBase()).style("DocumentPageSize", codeFn()).style(".A4)", codeBase()))
                .addRich(r -> r.style("    .create", codeFn()).style("()) {", codeBase()))
                .addRich(r -> r.style("  doc.", codeBase()).style("pageFlow", codeFn()).style("(page -> page", codeBase()))
                .addRich(r -> r.style("    .addParagraph", codeFn()).style("(", codeBase()).style("\"GraphCompose\"", codeStr()).style(")", codeBase()))
                .addRich(r -> r.style("    .addParagraph", codeFn()).style("(", codeBase()).style("\"Cinematic.\"", codeStr()).style("));", codeBase()))
                .addRich(r -> r.style("  doc.", codeBase()).style("buildPdf", codeFn()).style("();", codeBase()))
                .addRich(r -> r.style("}", codeBase())));
        s.addParagraph(p -> p.text("You describe the document intent — the engine resolves the rest.")
                .textStyle(noteOnDark()).lineSpacing(1.3).margin(DocumentInsets.top(6)));
    }

    /** Column 2 — the engine, as a two-column icon grid of capabilities. */
    private static void stepEngine(SectionBuilder s) {
        s.spacing(10);
        colHeader(s, "2", "GraphCompose Engine");
        s.addParagraph(p -> p.text("Semantic graph · deterministic layout")
                .textStyle(colSub()).margin(DocumentInsets.bottom(16)));
        s.add(engineGrid());
    }

    private static DocumentNode engineGrid() {
        // Pipeline order, top → bottom: Layout first, DSL last.
        String[][] items = {
                {"layout", "Layout"}, {"page-break", "Pagination"},
                {"themes", "Themes"}, {"code", "Components"},
                {"testing", "Snapshot Tests"}, {"dsl", "DSL"},
        };
        // pad keeps each card's outline off the container edge — the grid box
        // clips children to its bounds, which would otherwise shave the outer
        // strokes (top row top, bottom row bottom, right column right).
        double cardW = 93, cardH = 30, gapX = 8, gapY = 8, pad = 2;
        ShapeContainerBuilder g = new ShapeContainerBuilder().name("EngineGrid")
                .rectangle(cardW * 2 + gapX + pad * 2, cardH * 3 + gapY * 2 + pad * 2).fillColor(HERO_BG);
        // Column-major: each column reads top→bottom as the pipeline, so the
        // left column is Layout→Pagination→Themes and the right is
        // Components→Snapshot Tests→DSL (Layout top, DSL bottom).
        for (int i = 0; i < items.length; i++) {
            double x = pad + (i / 3) * (cardW + gapX);
            double y = pad + (i % 3) * (cardH + gapY);
            g.position(engineCard(items[i][0], items[i][1], cardW, cardH), x, y, LayerAlign.TOP_LEFT);
        }
        return g.build();
    }

    /** One engine capability in its own dark card: light icon + centred label. */
    private static DocumentNode engineCard(String iconName, String label, double w, double h) {
        return new ShapeContainerBuilder().name("Eng_" + label)
                .roundedRect(w, h, 7).fillColor(HERO_BG)
                .stroke(DocumentStroke.of(RULE_DARK, 0.9))
                .position(lightIcon(iconName).node(16), 9, 0, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder().text(label).textStyle(gridLabel())
                        .margin(DocumentInsets.zero()).build(), 30, 0, LayerAlign.CENTER_LEFT)
                .build();
    }

    /** Column 3 — the output backends, as dark status cards. */
    private static void stepBackends(SectionBuilder s) {
        s.spacing(10);
        colHeader(s, "3", "Output Backends");
        s.add(backendCard("pdf-file", "PDFBox 3.0", "Production backend", true));
        s.add(backendCard("docx", "DOCX export", "Semantic export", false));
        s.add(backendCard("ppt-file", "PPTX", "Planned", false));
    }

    private static DocumentNode backendCard(String iconName, String title, String sub, boolean live) {
        double w = 110, h = 44;
        DocumentColor accent = live ? GREEN : SLATE;
        return new ShapeContainerBuilder().name("Backend_" + title)
                .roundedRect(w, h, 8).fillColor(HERO_BG)
                .stroke(DocumentStroke.of(RULE_DARK, 0.9))
                .position(lightIcon(iconName).node(18), 12, 0, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder().text(title).textStyle(backendTitle())
                        .margin(DocumentInsets.zero()).build(), 37, -7, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder().text(sub)
                        .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA_BOLD)
                                .size(7.5).color(accent).build())
                        .margin(DocumentInsets.zero()).build(), 37, 8, LayerAlign.CENTER_LEFT)
                .build();
    }

    /** Column 4 — real example PDFs as a cascade of large thumbnails. */
    private static void stepDocs(SectionBuilder s) {
        s.spacing(10);
        colHeader(s, "4", "Real PDF Output");
        s.add(docCascade());
        s.addParagraph(p -> p.text("CVs, invoices, reports — every one rendered by the engine in this repo.")
                .textStyle(itemDesc()).lineSpacing(1.3).margin(DocumentInsets.top(8)));
    }

    private static DocumentNode docCascade() {
        double w = 92, h = w / 0.707;
        return new ShapeContainerBuilder().name("DocCascade")
                .rectangle(174, h + 16).fillColor(HERO_BG)
                .position(docThumb("thumb-cv", w), 0, 16, LayerAlign.TOP_LEFT)
                .position(docThumb("thumb-invoice", w), 41, 8, LayerAlign.TOP_LEFT)
                .position(docThumb("thumb-report", w), 82, 0, LayerAlign.TOP_LEFT)
                .build();
    }

    private static DocumentNode docThumb(String name, double w) {
        double h = w / 0.707;
        return new ShapeContainerBuilder().name("Thumb_" + name)
                .roundedRect(w, h, 4).fillColor(DocumentColor.WHITE)
                .stroke(DocumentStroke.of(RULE_DARK, 1.0))
                .clipPolicy(ClipPolicy.CLIP_PATH)
                .center(new ImageBuilder().name("Img_" + name)
                        .source(thumb(name)).size(w - 3, h - 3)
                        .fitMode(DocumentImageFitMode.COVER).build())
                .build();
    }

    private static DocumentImageData thumb(String name) {
        try (InputStream in = Objects.requireNonNull(
                EngineDeckExample.class.getResourceAsStream("/showcase/thumbs/" + name + ".png"),
                "thumbnail missing: " + name)) {
            return DocumentImageData.fromBytes(in.readAllBytes());
        } catch (Exception e) {
            throw new IllegalStateException("failed to load thumbnail: " + name, e);
        }
    }

    /** Dark capability chip: a light icon beside a vertically-centred label. */
    private static void chip(RowBuilder row, String iconName, String label) {
        double w = 158, h = 42;
        DocumentNode chipNode = new ShapeContainerBuilder().name("Chip_" + label)
                .roundedRect(w, h, 9).fillColor(CARD_DARK)
                .stroke(DocumentStroke.of(RULE_DARK, 0.8))
                .position(lightIcon(iconName).node(22), 16, 0, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder().text(label).textStyle(chipLabelLight())
                        .margin(DocumentInsets.zero()).build(), 46, 0, LayerAlign.CENTER_LEFT)
                .build();
        row.addSection(c -> c.addAligned(HorizontalAlign.CENTER, chipNode));
    }

    private static SvgIcon lightIcon(String name) {
        return icon(name + "-light");
    }

    // ── Banner text styles ───────────────────────────────────────────────────
    private static DocumentTextStyle colTitle() {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA_BOLD).size(13).color(ON_DARK).build();
    }

    private static DocumentTextStyle colSub() {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(8.5).color(ON_DARK_MUTED).build();
    }

    private static DocumentTextStyle gridLabel() {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA_BOLD).size(8.5).color(ON_DARK).build();
    }

    private static DocumentTextStyle backendTitle() {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA_BOLD).size(9.5).color(ON_DARK).build();
    }

    private static DocumentTextStyle chipLabelLight() {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA_BOLD).size(9.5).color(ON_DARK).build();
    }

    private static DocumentTextStyle itemDesc() {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(8).color(ON_DARK_MUTED).build();
    }

    private static DocumentTextStyle noteOnDark() {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(8).color(ON_DARK_MUTED).build();
    }

    private static DocumentTextStyle codeBase() {
        return DocumentTextStyle.builder().fontName(FontName.COURIER).size(6.8).color(CODE_TXT).build();
    }

    private static DocumentTextStyle codeKw() {
        return DocumentTextStyle.builder().fontName(FontName.COURIER).size(6.8).color(VIOLET_LIGHT).build();
    }

    private static DocumentTextStyle codeFn() {
        return DocumentTextStyle.builder().fontName(FontName.COURIER).size(6.8).color(CODE_FN).build();
    }

    private static DocumentTextStyle codeStr() {
        return DocumentTextStyle.builder().fontName(FontName.COURIER).size(6.8).color(CODE_STR).build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reusable cards
    // ─────────────────────────────────────────────────────────────────────────

    /** Clean outlined pipeline step: number, icon, title, one-line description. */
    private static void pipelineStep(com.demcha.compose.document.dsl.RowBuilder row,
                                     String number, String icon, String label, String sub) {
        row.addSection(s -> s
                .softPanel(DocumentColor.WHITE, 10, 14)
                .stroke(DocumentStroke.of(SURFACE_LINE, 0.9))
                .accentTop(VIOLET, 2.5)
                .spacing(7)
                .addParagraph(p -> p.text(number).textStyle(stepNum())
                        .align(TextAlign.CENTER).margin(DocumentInsets.zero()))
                .addSvgIcon(icon(icon), 30, HorizontalAlign.CENTER)
                .addParagraph(p -> p.text(label).textStyle(chipLabel())
                        .align(TextAlign.CENTER).margin(DocumentInsets.zero()))
                .addParagraph(p -> p.text(sub).textStyle(caption())
                        .align(TextAlign.CENTER).lineSpacing(1.3).margin(DocumentInsets.zero())));
    }

    private static void pipeArrow(com.demcha.compose.document.dsl.RowBuilder row) {
        row.addSection(s -> s.padding(DocumentInsets.zero())
                .addParagraph(p -> p.text(">").textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA_BOLD).size(20).color(VIOLET).build())
                        .align(TextAlign.CENTER).margin(DocumentInsets.top(46))));
    }

    /** Clean outlined "why it's solid" card: bold title + a sentence. */
    private static void proofCard(com.demcha.compose.document.dsl.RowBuilder row,
                                  String title, String desc) {
        row.addSection(s -> s
                .softPanel(DocumentColor.WHITE, 10, 16)
                .stroke(DocumentStroke.of(SURFACE_LINE, 0.9))
                .accentLeft(VIOLET, 3)
                .spacing(6)
                .addParagraph(p -> p.text(title).textStyle(h3()).margin(DocumentInsets.zero()))
                .addParagraph(p -> p.text(desc).textStyle(body()).lineSpacing(1.4).margin(DocumentInsets.zero())));
    }

    /** White chart card: titled panel wrapping one chart. */
    private static void chartCard(SectionBuilder section, String title,
                                  ChartSpec spec, ChartStyle style) {
        section
                .softPanel(DocumentColor.WHITE, 10, 14)
                .stroke(DocumentStroke.of(SURFACE_LINE, 0.8))
                .accentTop(VIOLET, 2.5)
                .spacing(8)
                .addParagraph(p -> p.text(title).textStyle(h3()).margin(DocumentInsets.zero()))
                .chart(spec, style);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Real benchmark data — loaded at render time from the bundled result file
    // (a snapshot of target/benchmarks/comparative/latest.json). The point: this
    // PDF reads a real benchmark file and the engine draws the table and charts
    // below from it. Refresh: run scripts/run-benchmarks.ps1, then copy
    // target/benchmarks/comparative/latest.json to resources/benchmarks/.
    // ─────────────────────────────────────────────────────────────────────────

    private static final String[] LIBS = {"GraphCompose", "iText 9", "JasperReports"};
    private static final int[] SIZES = {40, 200, 1000};

    /** A comparative benchmark run: metadata plus per-scenario time/heap. */
    private record BenchRun(String timestamp, int warmup, int measure, Map<String, double[]> rows) {
        double timeMs(String label) {
            return rows.get(label)[0];
        }

        double heapMb(String label) {
            return rows.get(label)[1];
        }

        double timeMs(String lib, int size) {
            return timeMs(lib + " (" + size + " rows)");
        }

        double heapMb(String lib, int size) {
            return heapMb(lib + " (" + size + " rows)");
        }
    }

    private static BenchRun loadBench() {
        try (InputStream in = Objects.requireNonNull(
                EngineDeckExample.class.getResourceAsStream("/benchmarks/comparative.json"),
                "benchmark data missing: /benchmarks/comparative.json")) {
            JsonNode root = new ObjectMapper().readTree(in);
            Map<String, double[]> rows = new LinkedHashMap<>();
            for (JsonNode r : root.get("libraries")) {
                rows.put(r.get("library").asText(),
                        new double[]{r.get("avgTimeMs").asDouble(), r.get("avgHeapMb").asDouble()});
            }
            return new BenchRun(root.get("timestamp").asText(),
                    root.get("warmupIterations").asInt(),
                    root.get("measurementIterations").asInt(),
                    rows);
        } catch (Exception e) {
            throw new IllegalStateException("failed to load benchmark data", e);
        }
    }

    // ── Page 3 — measured comparison ──────────────────────────────────────────

    private static void benchTable(BenchRun b, com.demcha.compose.document.dsl.TableBuilder table) {
        table.name("BenchTable")
                .columns(
                        DocumentTableColumn.fixed(120),
                        DocumentTableColumn.auto(),
                        DocumentTableColumn.auto(),
                        DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.symmetric(7, 10))
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA).size(9).color(INK).build())
                        .stroke(DocumentStroke.of(SURFACE_LINE, 0.5))
                        .build())
                .headerStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.symmetric(8, 10))
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA_BOLD).size(9).color(DocumentColor.WHITE).build())
                        .fillColor(VIOLET_DEEP)
                        .stroke(DocumentStroke.of(VIOLET_DEEP, 0.5))
                        .build())
                .headerRow("Report size", "GraphCompose", "iText 9", "JasperReports")
                .zebra(DocumentColor.WHITE, SURFACE)
                .row("1 page · 3 lines",
                        cell(b, "GraphCompose Canonical"), cell(b, "iText 9"), cell(b, "JasperReports"));
        for (int size : SIZES) {
            table.row(size + " rows",
                    cell(b, "GraphCompose (" + size + " rows)"),
                    cell(b, "iText 9 (" + size + " rows)"),
                    cell(b, "JasperReports (" + size + " rows)"));
        }
    }

    private static String cell(BenchRun b, String label) {
        return String.format(Locale.ROOT, "%.1f ms · %.1f MB", b.timeMs(label), b.heapMb(label));
    }

    /** Three coloured bars (one per library) at the 1000-row scenario. */
    private static ChartSpec timeChart(BenchRun b) {
        return ChartSpec.bar()
                .data(ChartData.builder()
                        .categories("1000-row report")
                        .series("GraphCompose", b.timeMs("GraphCompose", 1000))
                        .series("iText 9", b.timeMs("iText 9", 1000))
                        .series("JasperReports", b.timeMs("JasperReports", 1000))
                        .build())
                .valueAxis(AxisSpec.builder().baselineAtZero(true)
                        .format(NumberFormatSpec.pattern("#,##0.0").withSuffix(" ms")).build())
                .legend(LegendPosition.BOTTOM)
                .valueLabels(ValueLabelMode.OUTSIDE)
                .size(ChartSize.aspectRatio(16, 8))
                .build();
    }

    private static ChartSpec memoryChart(BenchRun b) {
        return ChartSpec.bar()
                .data(ChartData.builder()
                        .categories("1000-row report")
                        .series("GraphCompose", b.heapMb("GraphCompose", 1000))
                        .series("iText 9", b.heapMb("iText 9", 1000))
                        .series("JasperReports", b.heapMb("JasperReports", 1000))
                        .build())
                .valueAxis(AxisSpec.builder().baselineAtZero(true)
                        .format(NumberFormatSpec.pattern("#,##0.0").withSuffix(" MB")).build())
                .legend(LegendPosition.BOTTOM)
                .valueLabels(ValueLabelMode.OUTSIDE)
                .size(ChartSize.aspectRatio(16, 8))
                .build();
    }

    // ── Page 4 — scaling (real data) ──────────────────────────────────────────

    /** One series per library across the 40 / 200 / 1000-row sweep. */
    private static ChartData bySize(BenchRun b, boolean time) {
        var d = ChartData.builder().categories("40", "200", "1000");
        for (String lib : LIBS) {
            double[] v = new double[SIZES.length];
            for (int i = 0; i < SIZES.length; i++) {
                v[i] = time ? b.timeMs(lib, SIZES[i]) : b.heapMb(lib, SIZES[i]);
            }
            d = d.series(lib, v);
        }
        return d.build();
    }

    private static ChartSpec scalingLineChart(BenchRun b) {
        return ChartSpec.line()
                .data(bySize(b, true))
                .valueAxis(AxisSpec.builder().baselineAtZero(true)
                        .format(NumberFormatSpec.pattern("#,##0").withSuffix(" ms")).build())
                .legend(LegendPosition.BOTTOM)
                .size(ChartSize.aspectRatio(16, 6.5))
                .build();
    }

    private static ChartSpec memoryAreaChart(BenchRun b) {
        return ChartSpec.line()
                .data(bySize(b, false))
                .smooth(true)
                .area(true)
                .valueAxis(AxisSpec.builder().baselineAtZero(true)
                        .format(NumberFormatSpec.pattern("#,##0").withSuffix(" MB")).build())
                .legend(LegendPosition.BOTTOM)
                .size(ChartSize.aspectRatio(16, 6.5))
                .build();
    }

    private static ChartSpec memoryShareDonut(BenchRun b) {
        return ChartSpec.pie()
                .data(ChartData.builder()
                        .categories("GraphCompose", "iText 9", "JasperReports")
                        .series("Heap",
                                b.heapMb("GraphCompose", 1000),
                                b.heapMb("iText 9", 1000),
                                b.heapMb("JasperReports", 1000))
                        .build())
                .donutRatio(0.58)
                .sliceLabels(SliceLabelMode.PERCENT)
                .centerText("1000 rows")
                .legend(LegendPosition.BOTTOM)
                .size(ChartSize.aspectRatio(16, 6.5))
                .build();
    }

    private static ChartSpec throughputChart(BenchRun b) {
        return ChartSpec.bar()
                .data(ChartData.builder()
                        .categories("docs / sec")
                        .series("GraphCompose", 1000.0 / b.timeMs("GraphCompose", 1000))
                        .series("iText 9", 1000.0 / b.timeMs("iText 9", 1000))
                        .series("JasperReports", 1000.0 / b.timeMs("JasperReports", 1000))
                        .build())
                .valueAxis(AxisSpec.builder().baselineAtZero(true)
                        .format(NumberFormatSpec.pattern("#,##0.0")).build())
                .legend(LegendPosition.BOTTOM)
                .valueLabels(ValueLabelMode.OUTSIDE)
                .size(ChartSize.aspectRatio(16, 6.5))
                .build();
    }

    // ── Chart styles (GraphCompose = violet, iText = slate, Jasper = amber) ───

    private static ChartStyle groupedStyle() {
        return ChartStyle.builder()
                .seriesPaint(0, DocumentPaint.solid(VIOLET))
                .seriesPaint(1, DocumentPaint.solid(SLATE))
                .seriesPaint(2, DocumentPaint.solid(AMBER))
                .barCornerRadius(DocumentCornerRadius.top(2))
                .build();
    }

    private static ChartStyle areaStyle() {
        return ChartStyle.builder()
                .seriesPaint(0, DocumentPaint.solid(VIOLET))
                .seriesPaint(1, DocumentPaint.solid(SLATE))
                .seriesPaint(2, DocumentPaint.solid(AMBER))
                .lineWidth(1.6)
                .build();
    }

    private static ChartStyle lineStyle() {
        return ChartStyle.builder()
                .seriesPaint(0, DocumentPaint.solid(VIOLET))
                .seriesPaint(1, DocumentPaint.solid(SLATE))
                .seriesPaint(2, DocumentPaint.solid(AMBER))
                .lineWidth(2.0)
                .pointMarker(PointMarker.circle(5.0)
                        .withStroke(DocumentStroke.of(DocumentColor.WHITE, 1.2)))
                .build();
    }

    private static ChartStyle donutStyle() {
        return ChartStyle.builder()
                .seriesPaint(0, DocumentPaint.solid(VIOLET))
                .seriesPaint(1, DocumentPaint.solid(SLATE))
                .seriesPaint(2, DocumentPaint.solid(AMBER))
                .sliceGapDegrees(2.0)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Section + text helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static void kicker(SectionBuilder section, String kicker, String heading) {
        section
                .padding(DocumentInsets.zero())
                .spacing(7)
                .addParagraph(p -> p.text(kicker).textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA_BOLD).size(9).color(VIOLET_DEEP).build())
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p.text(heading).textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA_BOLD).size(18).color(INK).build())
                        .margin(DocumentInsets.zero()));
    }

    private static DocumentTextStyle tagline() {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(14.5).color(HERO_TEXT).build();
    }

    private static DocumentTextStyle h3() {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA_BOLD).size(11).color(INK).build();
    }

    private static DocumentTextStyle body() {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(9.6).color(BODY).build();
    }

    private static DocumentTextStyle caption() {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(8).color(MUTED).build();
    }

    private static DocumentTextStyle chipLabel() {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA_BOLD).size(9.5).color(INK).build();
    }

    private static DocumentTextStyle stepNum() {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA_BOLD).size(16).color(VIOLET).build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Icon loading
    // ─────────────────────────────────────────────────────────────────────────

    private static SvgIcon logo() {
        return icon("logo");
    }

    /** Reads a bundled showcase icon from {@code /showcase/<name>.svg}. */
    private static SvgIcon icon(String name) {
        try (InputStream in = Objects.requireNonNull(
                EngineDeckExample.class.getResourceAsStream("/showcase/" + name + ".svg"),
                "showcase icon missing: " + name)) {
            return SvgIcon.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("failed to load showcase icon: " + name, e);
        }
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
}
