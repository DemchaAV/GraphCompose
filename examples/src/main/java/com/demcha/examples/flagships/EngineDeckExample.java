package com.demcha.examples.flagships;

import static com.demcha.examples.flagships.EngineDeckTheme.BODY;
import static com.demcha.examples.flagships.EngineDeckTheme.CARD_DARK;
import static com.demcha.examples.flagships.EngineDeckTheme.CODE_BG;
import static com.demcha.examples.flagships.EngineDeckTheme.CODE_FN;
import static com.demcha.examples.flagships.EngineDeckTheme.CODE_STR;
import static com.demcha.examples.flagships.EngineDeckTheme.CODE_TXT;
import static com.demcha.examples.flagships.EngineDeckTheme.GREEN;
import static com.demcha.examples.flagships.EngineDeckTheme.HERO_BG;
import static com.demcha.examples.flagships.EngineDeckTheme.HERO_TEXT;
import static com.demcha.examples.flagships.EngineDeckTheme.INK;
import static com.demcha.examples.flagships.EngineDeckTheme.MUTED;
import static com.demcha.examples.flagships.EngineDeckTheme.ON_DARK;
import static com.demcha.examples.flagships.EngineDeckTheme.ON_DARK_MUTED;
import static com.demcha.examples.flagships.EngineDeckTheme.RULE_DARK;
import static com.demcha.examples.flagships.EngineDeckTheme.SLATE;
import static com.demcha.examples.flagships.EngineDeckTheme.SURFACE_LINE;
import static com.demcha.examples.flagships.EngineDeckTheme.VIOLET;
import static com.demcha.examples.flagships.EngineDeckTheme.VIOLET_DEEP;
import static com.demcha.examples.flagships.EngineDeckTheme.VIOLET_LIGHT;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.chart.ChartSpec;
import com.demcha.compose.document.chart.ChartStyle;
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
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
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
 * ({@code #7C5CFC} / {@code #6128D9}) on a near-black panel. Feature icons are
 * read from {@code /showcase/*.svg} classpath resources — monochrome glyphs
 * recoloured to light variants ({@code *-light.svg}) so they read on the dark
 * cards — via {@link SvgIcon#parse(String)} as native PDF Béziers, no
 * rasterization.</p>
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
     * Renders page&nbsp;1's banner as a standalone, full-bleed hero. The dark
     * violet field is painted as the canonical {@code pageBackground}, so it
     * fills the whole landscape page — margins and corners included — and the
     * rasterised image carries no white frame, only the banner itself. This is
     * the source of the repository README hero
     * ({@code assets/readme/repository_showcase_render.png}, produced by
     * {@link com.demcha.examples.support.PdfPageRasterizer}); re-render it after a
     * version bump — the banner reads {@link #VERSION} / {@link #CODENAME}, so
     * the hero stays current with one rebuild.
     *
     * @return the generated single-page banner PDF path
     * @throws Exception when rendering or icon IO fails
     */
    public static Path generateBanner() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("flagships", "engine-banner.pdf");
        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4.landscape())
                .pageBackground(HERO_BG)
                .margin(16, 16, 16, 16)
                .create()) {
            document.metadata(DocumentMetadata.builder()
                    .title("GraphCompose v" + VERSION + " — " + CODENAME)
                    .author("GraphCompose")
                    .subject("GraphCompose banner — the engine's own brand hero")
                    .producer("GraphCompose (PDFBox 3.0)")
                    .build());
            document.pageFlow()
                    .name("EngineBanner")
                    .addSection("Banner", EngineDeckExample::banner)
                    .build();
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
        EngineDeckData.BenchRun bench = EngineDeckData.loadBench();
        document.metadata(DocumentMetadata.builder()
                    .title("GraphCompose " + VERSION + " — " + CODENAME)
                    .author("GraphCompose")
                    .subject("What the GraphCompose document engine is — rendered by the engine itself")
                    .keywords("graphcompose, pdf, java, dsl, charts, svg, showcase, " + VERSION)
                    .creator("GraphCompose Examples")
                    .producer("GraphCompose (PDFBox 3.0)")
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
                        List<EngineDeckData.PipelineStep> steps = EngineDeckData.pipeline();
                        for (int i = 0; i < steps.size(); i++) {
                            EngineDeckData.PipelineStep st = steps.get(i);
                            pipelineStep(row, st.number(), st.icon(), st.title(), st.desc());
                            if (i < steps.size() - 1) {
                                pipeArrow(row);
                            }
                        }
                    })
                    .addRow("Proof", row -> {
                        row.spacing(16).evenWeights();
                        for (EngineDeckData.Proof p : EngineDeckData.proof()) {
                            proofCard(row, p.title(), p.desc());
                        }
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
                    .addTable(t -> EngineDeckCharts.benchTable(bench, t))
                    .addRow("CmpCharts", row -> {
                        row.spacing(16).evenWeights();
                        row.addSection("TimeCard", s -> chartCard(s,
                                "Render time at 1000 rows — ms (lower is faster)",
                                EngineDeckCharts.timeChart(bench), EngineDeckCharts.groupedStyle()));
                        row.addSection("MemCard", s -> chartCard(s,
                                "Peak heap at 1000 rows — MB (lower is lighter)",
                                EngineDeckCharts.memoryChart(bench), EngineDeckCharts.groupedStyle()));
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
                                "Render time vs. report size — ms",
                                EngineDeckCharts.scalingLineChart(bench), EngineDeckCharts.lineStyle()));
                        row.addSection("MemArea", s -> chartCard(s,
                                "Peak heap vs. report size — MB",
                                EngineDeckCharts.memoryAreaChart(bench), EngineDeckCharts.areaStyle()));
                    })
                    .addRow("ScaleBottom", row -> {
                        row.spacing(16).evenWeights();
                        row.addSection("ShareDonut", s -> chartCard(s,
                                "Memory at 1000 rows — share of total",
                                EngineDeckCharts.memoryShareDonut(bench), EngineDeckCharts.donutStyle()));
                        row.addSection("ThroughCard", s -> chartCard(s,
                                "Throughput at 1000 rows — docs/sec (higher is better)",
                                EngineDeckCharts.throughputChart(bench), EngineDeckCharts.groupedStyle()));
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
                    for (EngineDeckData.IconLabel c : EngineDeckData.capabilities()) {
                        chip(row, c.icon(), c.label());
                    }
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
        // Pipeline order, top → bottom: Layout first, DSL last (from the data layer).
        List<EngineDeckData.IconLabel> items = EngineDeckData.engineCapabilities();
        // pad keeps each card's outline off the container edge — the grid box
        // clips children to its bounds, which would otherwise shave the outer
        // strokes (top row top, bottom row bottom, right column right).
        double cardW = 93, cardH = 30, gapX = 8, gapY = 8, pad = 2;
        ShapeContainerBuilder g = new ShapeContainerBuilder().name("EngineGrid")
                .rectangle(cardW * 2 + gapX + pad * 2, cardH * 3 + gapY * 2 + pad * 2).fillColor(HERO_BG);
        // Column-major: each column reads top→bottom as the pipeline, so the
        // left column is Layout→Pagination→Themes and the right is
        // Components→Snapshot Tests→DSL (Layout top, DSL bottom).
        for (int i = 0; i < items.size(); i++) {
            double x = pad + (i / 3) * (cardW + gapX);
            double y = pad + (i % 3) * (cardH + gapY);
            g.position(engineCard(items.get(i).icon(), items.get(i).label(), cardW, cardH),
                    x, y, LayerAlign.TOP_LEFT);
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
        for (EngineDeckData.Backend b : EngineDeckData.backends()) {
            s.add(backendCard(b.icon(), b.title(), b.sub(), b.live()));
        }
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
