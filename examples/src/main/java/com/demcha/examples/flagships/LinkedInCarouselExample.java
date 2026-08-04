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
import com.demcha.compose.document.chart.ValueLabelMode;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.examples.support.ExampleVersion;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * A six-slide carousel sized for a LinkedIn document post, composed through the
 * canonical DSL and rendered by the engine it describes.
 *
 * <p>Portrait 1280&times;1600 (4:5), which is the tallest frame LinkedIn shows
 * without cropping and therefore the most readable on a phone. The width is set
 * by the opening slide: it carries {@link SocialCardExample}'s card at native
 * size rather than a scaled copy. Type is sized for that phone, not for a
 * desktop preview.</p>
 *
 * <p>Every figure is read at render time — the version from the filtered
 * {@code banner.properties}, the comparative timings from the committed
 * benchmark snapshot through {@link EngineDeckData}. Nothing here is a literal
 * that has to be remembered on the next release, which is the failure mode the
 * older flagship assets kept hitting.</p>
 *
 * @since 2.1.1
 */
public final class LinkedInCarouselExample {

    /**
     * LinkedIn renders document posts in a 4:5 frame; this is that frame, sized
     * so the opening slide can carry {@link SocialCardExample}'s card at its
     * native {@value SocialCardExample#CARD_WIDTH}pt width without scaling.
     */
    public static final double PAGE_WIDTH = SocialCardExample.CARD_WIDTH;
    /** Four-by-five against {@link #PAGE_WIDTH}. */
    public static final double PAGE_HEIGHT = PAGE_WIDTH * 5 / 4;

    /**
     * Everything below was drawn against a 1080-wide page. Rather than restate
     * every size, the type helpers and the box metrics scale by this ratio, so
     * the slides keep their proportions on the wider frame. Apply it to any
     * length that was eyeballed against the narrower page; the only literals
     * that stay absolute are ones derived from the page itself.
     */
    private static final double SCALE = PAGE_WIDTH / 1080.0;

    private static final double MARGIN = 80 * SCALE;
    private static final double CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;

    private static final DocumentColor NIGHT = DocumentColor.rgb(9, 12, 27);
    private static final DocumentColor SURFACE = DocumentColor.rgb(20, 27, 51);
    private static final DocumentColor SURFACE_2 = DocumentColor.rgb(27, 34, 64);
    private static final DocumentColor LINE = DocumentColor.rgb(48, 56, 96);
    private static final DocumentColor ON_DARK = DocumentColor.rgb(245, 247, 252);
    private static final DocumentColor ON_DARK_MUTED = DocumentColor.rgb(168, 177, 202);
    private static final DocumentColor VIOLET = DocumentColor.rgb(124, 92, 252);
    private static final DocumentColor VIOLET_LIGHT = DocumentColor.rgb(181, 159, 255);
    private static final DocumentColor MINT = DocumentColor.rgb(55, 214, 161);
    private static final DocumentColor AMBER = DocumentColor.rgb(230, 160, 68);

    /** Uniform padding inside every card, as passed to {@code softPanel}. */
    private static final double CARD_PAD = 30 * SCALE;

    /**
     * Width available inside a card.
     *
     * <p>A section measures to its longest line, so a stack of cards whose text
     * happens to be shorter renders with ragged right edges. A zero-height
     * spacer of this width sets the floor, and every card lines up.</p>
     */
    private static final double CARD_INNER = CONTENT_WIDTH - 2 * CARD_PAD;

    /**
     * The smallest useful program, shown on the closing slide.
     *
     * <p>Indented with non-breaking spaces: leading ordinary whitespace is
     * trimmed during inline layout, which would flatten the code to the left
     * margin and make it read as broken. Courier is monospaced, so the
     * substitute occupies the same advance.</p>
     */
    private static final String[] SNIPPET = {
        "try (DocumentSession doc =",
        indent(8) + "GraphCompose.document(out).create()) {",
        indent(4) + "doc.pageFlow()",
        indent(7) + ".addParagraph(p -> p.text(\"Hello.\"))",
        indent(7) + ".build();",
        indent(4) + "doc.buildPdf();" + indent(6) + "// or buildPptx(out)",
        "}",
    };

    private static String indent(int spaces) {
        return " ".repeat(spaces);
    }

    /** The scaling tier the comparative benchmark reports at its largest size. */
    private static final int TIER = 1000;

    private LinkedInCarouselExample() {
    }

    /**
     * Runs the example.
     *
     * @param args ignored
     * @throws Exception when composition or rendering fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
        System.out.println("Generated: " + generatePptx());
    }

    /**
     * Writes the carousel as a PDF — the file LinkedIn accepts as a document post.
     *
     * @return generated PDF path under {@code target/generated-pdfs/flagships}
     * @throws Exception when composition or rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("flagships", "linkedin-carousel.pdf");
        try (DocumentSession document = session(outputFile)) {
            compose(document);
            document.buildPdf();
        }
        return outputFile;
    }

    /**
     * Writes the same carousel as an editable deck.
     *
     * <p>Slide two claims the PPTX backend places the same geometry as the PDF
     * one. Emitting this file from the same composition is the cheapest way to
     * keep that claim true whenever this example changes.</p>
     *
     * @return generated PPTX path under {@code target/generated-pdfs/flagships}
     * @throws Exception when composition or rendering fails
     */
    public static Path generatePptx() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("flagships", "linkedin-carousel.pptx");
        try (DocumentSession document = session(outputFile)) {
            compose(document);
            document.buildPptx(outputFile);
        }
        return outputFile;
    }

    private static DocumentSession session(Path outputFile) {
        return GraphCompose.document(outputFile)
                .pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                .pageBackground(NIGHT)
                .margin((float) MARGIN, (float) MARGIN, (float) MARGIN, (float) MARGIN)
                .create();
    }

    /**
     * Composes all six slides.
     *
     * @param document the open session to compose into
     */
    static void compose(DocumentSession document) {
        EngineDeckData.BenchRun bench = EngineDeckData.loadBench();

        document.metadata(DocumentMetadata.builder()
                .title("GraphCompose " + ExampleVersion.currentLine())
                .subject("One composition, rendered to PDF and PowerPoint")
                .creator("GraphCompose Examples")
                .producer("GraphCompose")
                .build());

        // The opening slide is the social card at native size. Its own page gets
        // no margin so the artwork reaches the edges, and a spacer centres it in
        // the taller frame - both share the same night background, so the space above
        // and below reads as deliberate letterboxing rather than a gap.
        document.pageMargins(List.of(PageMarginRule.page(1, DocumentInsets.zero())));

        document.pageFlow()
                .name("Carousel")
                .spacing(0)
                .addSection("Card", LinkedInCarouselExample::cardSlide)
                .addPageBreak(b -> b.name("ToCover"))
                .addSection("Cover", LinkedInCarouselExample::cover)
                .addPageBreak(b -> b.name("ToBackend"))
                .addSection("Backend", LinkedInCarouselExample::backendSlide)
                .addPageBreak(b -> b.name("ToNumbers"))
                .addSection("Numbers", s -> numbersSlide(s, bench))
                .addPageBreak(b -> b.name("ToGuarantees"))
                .addSection("Guarantees", LinkedInCarouselExample::guaranteesSlide)
                .addPageBreak(b -> b.name("ToClose"))
                .addSection("Close", LinkedInCarouselExample::closeSlide)
                .build();
    }

    // ───────────────────────── slides ─────────────────────────

    private static void cardSlide(SectionBuilder slide) {
        slide.spacing(0);
        // Floored: a card taller than the page would otherwise ask for a
        // negative spacer rather than simply filling the frame.
        double bleed = Math.max(0, (PAGE_HEIGHT - SocialCardExample.CARD_HEIGHT) / 2);
        slide.addSpacer(sp -> sp.width(PAGE_WIDTH).height(bleed));
        slide.add(SocialCardExample.scene());
    }

    private static void cover(SectionBuilder slide) {
        slide.spacing(56 * SCALE);
        eyebrow(slide, "GRAPHCOMPOSE " + ExampleVersion.currentLine(), VIOLET_LIGHT);
        slide.addParagraph(p -> p
                .text("One composition.\nTwo formats.")
                .textStyle(headline(132))
                // lineSpacing is EXTRA space in points, not a multiplier: the layout
                // adds it between wrapped lines on top of the font's own line height.
                // Display type needs proportionally less of it than body copy.
                .lineSpacing(8 * SCALE)
                .margin(DocumentInsets.zero()));
        slide.addShape(shape -> shape.size(160 * SCALE, 7 * SCALE).fillColor(VIOLET)
                .margin(DocumentInsets.symmetric(14, 0)));
        slide.addParagraph(p -> p
                .text("A Java DSL describes the document. The engine resolves layout "
                      + "and pagination once, then hands the same fragments to every "
                      + "backend — so PDF and PowerPoint are the same geometry, "
                      + "not two hand-kept designs.")
                .textStyle(body(40, ON_DARK_MUTED))
                .lineSpacing(5 * SCALE)
                .margin(DocumentInsets.zero()));
        slide.addRow("Formats", row -> {
            row.spacing(14 * SCALE).evenWeights();
            formatChip(row, "PDF", "fixed layout", "PDFBox", MINT);
            formatChip(row, "PPTX", "fixed layout", "Apache POI", VIOLET_LIGHT);
            formatChip(row, "DOCX", "semantic", "Apache POI", AMBER);
        });
        slide.addParagraph(p -> footnote(p,
                "This carousel is a GraphCompose document. So is the deck it links to."));
    }

    private static void backendSlide(SectionBuilder slide) {
        slide.spacing(30 * SCALE);
        eyebrow(slide, "WHAT " + ExampleVersion.currentLine() + " ADDS", MINT);
        slide.addParagraph(p -> p
                .text("PowerPoint is a\nfirst-class backend.")
                .textStyle(headline(82))
                .lineSpacing(6 * SCALE)
                .margin(DocumentInsets.zero()));
        point(slide, "Fixed layout, not export", MINT,
                "PptxFixedLayoutBackend implements the same FixedLayoutRenderer "
                + "contract as the PDF backend and is discovered through the same "
                + "ServiceLoader SPI. It consumes placed fragments, so it never "
                + "re-flows your document.");
        point(slide, "Native shapes, not screenshots", VIOLET_LIGHT,
                "Text stays text, rectangles stay rectangles, charts stay drawn "
                + "geometry. The deck opens editable in PowerPoint instead of "
                + "arriving as a picture of one.");
        point(slide, "One source, two files", AMBER,
                "The same compose(...) call writes the PDF and the PPTX. Nothing "
                + "is authored twice, so the two cannot drift apart.");
        point(slide, "It tells you what it swapped", VIOLET,
                "PowerPoint has no Helvetica or Courier. When a PDF built-in is "
                + "substituted the render logs the family, the replacement and how "
                + "to get identical glyphs - rather than silently shifting your text.");
    }

    private static void numbersSlide(SectionBuilder slide, EngineDeckData.BenchRun bench) {
        slide.spacing(40 * SCALE);
        eyebrow(slide, "MEASURED, NOT CLAIMED", AMBER);
        slide.addParagraph(p -> p
                .text("A " + TIER + "-row report")
                .textStyle(headline(82))
                .margin(DocumentInsets.zero()));
        slide.addParagraph(p -> p
                .text("Average render latency, milliseconds — lower is faster.")
                .textStyle(body(30, ON_DARK_MUTED))
                .margin(DocumentInsets.bottom(10 * SCALE)));

        slide.addSection("Chart", card -> card
                .softPanel(SURFACE, 20 * SCALE, 26 * SCALE)
                .stroke(DocumentStroke.of(LINE, 1 * SCALE))
                .chart(latencyChart(bench), latencyStyle()));

        slide.addRow("Ratios", row -> {
            row.spacing(14 * SCALE).evenWeights();
            ratio(row, times(bench.timeMs("iText 9", TIER) / bench.timeMs("GraphCompose", TIER)),
                    "faster than iText 9", VIOLET_LIGHT);
            ratio(row, times(bench.heapMb("iText 9", TIER) / bench.heapMb("GraphCompose", TIER)),
                    "lighter than iText 9", MINT);
            ratio(row, times(bench.heapMb("JasperReports", TIER) / bench.heapMb("GraphCompose", TIER)),
                    "lighter than Jasper", AMBER);
        });

        slide.addParagraph(p -> footnote(p,
                "One machine, single run, " + snapshotDate(bench) + ". Read the ratios rather "
                + "than the timings: both libraries are measured in the same run, so their "
                + "proportions survive a change of machine while the milliseconds do not. "
                + "On time, Jasper reaches parity at this size."));
    }

    private static void guaranteesSlide(SectionBuilder slide) {
        slide.spacing(30 * SCALE);
        eyebrow(slide, "HOW IT STAYS HONEST", VIOLET_LIGHT);
        slide.addParagraph(p -> p
                .text("Guarantees you\ncan run.")
                .textStyle(headline(82))
                .lineSpacing(6 * SCALE)
                .margin(DocumentInsets.zero()));
        point(slide, "Deterministic layout snapshots", VIOLET_LIGHT,
                "Every flagship document has its geometry recorded as JSON. A layout "
                + "change that moves a fragment fails the build instead of quietly "
                + "shipping.");
        point(slide, "Measurement counts, not stopwatches", MINT,
                "A CI gate asserts the exact number of text measurements the layout "
                + "pass costs. Those are integers, identical on every machine, so they "
                + "separate a slower engine from a slower laptop — which a millisecond "
                + "ceiling cannot.");
        point(slide, "A capability matrix, written down", AMBER,
                "Where a backend cannot do something, the difference is documented per "
                + "payload rather than discovered in your output.");
        point(slide, "Binary compatibility, gated", VIOLET,
                "japicmp checks every build against the published 2.0 baseline, so an "
                + "accidental break in a minor release fails CI instead of your app.");
    }

    private static void closeSlide(SectionBuilder slide) {
        slide.spacing(30 * SCALE);
        eyebrow(slide, "OPEN SOURCE · MIT", MINT);
        slide.addParagraph(p -> p
                .text("Try it.")
                .textStyle(headline(132))
                .margin(DocumentInsets.zero()));
        slide.addSection("Coords", card -> {
            card.softPanel(SURFACE, 20 * SCALE, CARD_PAD)
                    .stroke(DocumentStroke.of(LINE, 1 * SCALE))
                    .spacing(4 * SCALE);
            card.addSpacer(sp -> sp.width(CARD_INNER).height(0));
            // Split on the colon rather than set in one line: the coordinate is
            // wider than the card at this size and broke mid-artifact.
            card.addParagraph(p -> p
                    .text("io.github.demchaav")
                    .textStyle(mono(31, ON_DARK_MUTED))
                    .margin(DocumentInsets.zero()));
            card.addParagraph(p -> p
                    .text("graph-compose-bundle")
                    .textStyle(mono(40, ON_DARK))
                    .margin(DocumentInsets.zero()));
            // Deliberately not the reactor version: this file renders from a
            // -SNAPSHOT tree, and printing that would advertise a coordinate that
            // is not on Central.
            card.addParagraph(p -> p
                    .text("latest release on Maven Central")
                    .textStyle(mono(27, MINT))
                    .margin(DocumentInsets.zero()));
        });
        slide.addSection("Snippet", card -> {
            card.softPanel(SURFACE_2, 20 * SCALE, CARD_PAD)
                    .accentLeft(VIOLET, 4 * SCALE)
                    .spacing(2 * SCALE);
            card.addSpacer(sp -> sp.width(CARD_INNER).height(0));
            for (String line : SNIPPET) {
                card.addParagraph(p -> p
                        .text(line)
                        .textStyle(mono(30, line.trim().startsWith("//")
                                ? DocumentColor.rgb(126, 135, 165) : ON_DARK))
                        .margin(DocumentInsets.zero()));
            }
        });
        slide.addParagraph(p -> p
                .text("github.com/DemchaAV/GraphCompose")
                .textStyle(body(46, VIOLET_LIGHT))
                .margin(DocumentInsets.top(4 * SCALE)));
        slide.addParagraph(p -> footnote(p,
                "Java 17+. Every slide in this carousel was composed with the library "
                + "and rendered by its own PDF backend - the PPTX beside it comes from "
                + "the same composition."));
    }

    // ───────────────────────── pieces ─────────────────────────

    private static void eyebrow(SectionBuilder slide, String text, DocumentColor accent) {
        slide.addParagraph(p -> p
                .text(text)
                .textStyle(mono(26, accent))
                .margin(DocumentInsets.zero()));
    }

    private static void point(SectionBuilder slide, String title, DocumentColor accent, String body) {
        // Wrapped in a single-child row: a bare section sizes itself to its
        // longest line, which leaves a stack of cards with ragged right edges.
        // An even-weighted row gives every card the full content width.
        slide.addSection("Point", card -> {
            card.softPanel(SURFACE, 26 * SCALE, CARD_PAD)
                    .accentLeft(accent, 4 * SCALE)
                    .spacing(6 * SCALE);
            card.addSpacer(sp -> sp.width(CARD_INNER).height(0));
            card.addParagraph(p -> p
                    .text(title)
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(FontName.HELVETICA)
                            .decoration(DocumentTextDecoration.BOLD)
                            .size(40 * SCALE)
                            .color(ON_DARK)
                            .build())
                    .margin(DocumentInsets.zero()));
            card.addParagraph(p -> p
                    .text(body)
                    .textStyle(body(28, ON_DARK_MUTED))
                    .lineSpacing(4 * SCALE)
                    .margin(DocumentInsets.zero()));
        });
    }

    private static void formatChip(RowBuilder row, String name, String lane,
                                   String engine, DocumentColor accent) {
        row.addSection(name, card -> {
            card.softPanel(SURFACE_2, 20, 24)
                    .stroke(DocumentStroke.of(LINE, 1 * SCALE))
                    .spacing(2 * SCALE);
            card.addParagraph(p -> p
                    .text(name)
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(FontName.HELVETICA)
                            .decoration(DocumentTextDecoration.BOLD)
                            .size(56 * SCALE)
                            .color(accent)
                            .build())
                    .align(TextAlign.CENTER)
                    .margin(DocumentInsets.zero()));
            card.addParagraph(p -> p
                    .text(lane)
                    .textStyle(mono(27, ON_DARK_MUTED))
                    .align(TextAlign.CENTER)
                    .margin(DocumentInsets.zero()));
            card.addParagraph(p -> p
                    .text(engine)
                    .textStyle(mono(23, DocumentColor.rgb(122, 131, 162)))
                    .align(TextAlign.CENTER)
                    .margin(DocumentInsets.top(4 * SCALE)));
        });
    }

    private static void ratio(RowBuilder row, String value, String label, DocumentColor accent) {
        row.addSection("Ratio", card -> {
            card.softPanel(SURFACE_2, 14 * SCALE, 24 * SCALE)
                    .accentTop(accent, 3 * SCALE)
                    .spacing(2 * SCALE);
            card.addParagraph(p -> p
                    .text(value)
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(FontName.HELVETICA)
                            .decoration(DocumentTextDecoration.BOLD)
                            .size(64 * SCALE)
                            .color(accent)
                            .build())
                    .margin(DocumentInsets.zero()));
            card.addParagraph(p -> p
                    .text(label)
                    .textStyle(body(24, ON_DARK_MUTED))
                    .lineSpacing(3 * SCALE)
                    .margin(DocumentInsets.zero()));
        });
    }

    private static ChartSpec latencyChart(EngineDeckData.BenchRun bench) {
        return ChartSpec.bar()
                .data(ChartData.builder()
                        .categories("GraphCompose", "iText 9", "JasperReports")
                        .series("ms",
                                bench.timeMs("GraphCompose", TIER),
                                bench.timeMs("iText 9", TIER),
                                bench.timeMs("JasperReports", TIER))
                        .build())
                .valueAxis(AxisSpec.builder().baselineAtZero(true).build())
                .showCategoryLabels(true)
                .legend(LegendPosition.NONE)
                .valueLabels(ValueLabelMode.OUTSIDE)
                .size(ChartSize.fixedHeight(470 * SCALE))
                .build();
    }

    private static ChartStyle latencyStyle() {
        return ChartStyle.builder()
                .seriesPaint(0, DocumentPaint.solid(VIOLET))
                .barCornerRadius(DocumentCornerRadius.of(4 * SCALE))
                .barWidthRatio(0.5)
                .axisTextStyle(mono(22, ON_DARK_MUTED))
                .valueLabelTextStyle(mono(25, ON_DARK))
                // The labels sit over the gridlines, which on this surface read as
                // strikethroughs across the digits. The halo punches a chip of the
                // card's own fill out from under each one.
                .valueLabelHalo(DocumentPaint.solid(SURFACE))
                .build();
    }

    private static ParagraphBuilder footnote(ParagraphBuilder p, String text) {
        return p.text(text)
                .textStyle(body(22, DocumentColor.rgb(130, 139, 170)))
                .lineSpacing(3 * SCALE)
                .margin(DocumentInsets.top(6 * SCALE));
    }

    private static String times(double ratio) {
        return String.format(Locale.ROOT, "%.1fx", ratio);
    }

    private static String snapshotDate(EngineDeckData.BenchRun bench) {
        String timestamp = bench.timestamp();
        return timestamp.substring(0, Math.min(10, timestamp.length()));
    }

    private static DocumentTextStyle headline(double size) {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .decoration(DocumentTextDecoration.BOLD)
                .size(size * SCALE)
                .color(ON_DARK)
                .build();
    }

    private static DocumentTextStyle body(double size, DocumentColor color) {
        return DocumentTextStyle.builder().size(size * SCALE).color(color).build();
    }

    private static DocumentTextStyle mono(double size, DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(FontName.COURIER)
                .size(size * SCALE)
                .color(color)
                .build();
    }
}
