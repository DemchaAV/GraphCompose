package com.demcha.examples.flagships;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Single-page cinematic engine showcase used as the
 * {@code assets/readme/repository_showcase_render.png} source. NOT a
 * business document, NOT a CV — a brand promo page that demonstrates
 * the visual register the engine can hit when an author cares about
 * presentation as much as data:
 *
 * <ul>
 *   <li><b>Top navy band</b> — full-width branded marquee (canonical
 *       {@code pageBackgrounds} band fill).</li>
 *   <li><b>Cinematic hero image</b> — pre-rendered brand artwork
 *       (semantic graph → polished PDFs visual metaphor) mounted in a
 *       rounded clip frame ({@code ShapeContainerNode} +
 *       {@code ClipPolicy.CLIP_PATH}).</li>
 *   <li><b>Magazine-headline typography</b> — three-line bold lockup
 *       set in the canonical serif heading style.</li>
 *   <li><b>Three KPI cards</b> — soft-panel sections with accent rules
 *       (V2 templates count · test count · Maven Central debut).</li>
 *   <li><b>Three-column capability grid</b> — labelled rich-text
 *       paragraphs showcasing inline style mixing.</li>
 *   <li><b>Footer brand stripe</b> — accent rule + brand links.</li>
 * </ul>
 *
 * <p>The hero image lives in {@code examples/src/main/resources/engine-hero.png}
 * and is loaded over the classpath so the example runs without any
 * filesystem path assumptions.</p>
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/flagships/engine-showcase.pdf}
 * — page 1 is rasterised by {@link com.demcha.examples.support.PdfPageRasterizer}
 * into {@code assets/readme/repository_showcase_render.png}.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.6.6
 */
public final class EngineShowcase {

    // ── Theme palette — cinematic navy + electric accent ─────────
    private static final DocumentColor PAPER       = DocumentColor.rgb(248, 247, 242);
    private static final DocumentColor NAVY        = DocumentColor.rgb(18, 26, 56);
    private static final DocumentColor NAVY_DARK   = DocumentColor.rgb(10, 14, 36);
    private static final DocumentColor ACCENT      = DocumentColor.rgb(255, 138, 36);
    private static final DocumentColor ACCENT_SOFT = DocumentColor.rgb(255, 198, 138);
    private static final DocumentColor INK         = DocumentColor.rgb(20, 24, 38);
    private static final DocumentColor MUTED       = DocumentColor.rgb(110, 116, 132);
    private static final DocumentColor RULE        = DocumentColor.rgb(220, 218, 210);
    private static final DocumentColor CARD_RING   = DocumentColor.rgb(232, 226, 212);
    private static final DocumentColor PANEL_TINT  = DocumentColor.rgb(232, 234, 246);
    private static final DocumentColor PANEL_RING  = DocumentColor.rgb(208, 214, 234);

    private EngineShowcase() {
    }

    public static Path generate() throws Exception {
        Path output = ExampleOutputPaths.prepare("flagships", "engine-showcase.pdf");
        DocumentImageData hero = loadHeroImage();

        try (DocumentSession document = GraphCompose.document(output)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(PAPER)
                .margin(36, 38, 32, 38)
                .create()) {

            document.pageFlow()
                    .name("EngineShowcase")
                    .spacing(14)

                    // ── Marquee band: brand + version ──────────────
                    .addRow("Marquee", row -> row
                            .spacing(0)
                            .weights(1, 1)
                            .addSection("MarqueeLeft", section -> section
                                    .addParagraph(p -> p
                                            .text("GRAPHCOMPOSE")
                                            .textStyle(bandLeft())
                                            .margin(DocumentInsets.zero())))
                            .addSection("MarqueeRight", section -> section
                                    .addParagraph(p -> p
                                            .text("v1.6 · MAVEN CENTRAL")
                                            .textStyle(bandRight())
                                            .align(TextAlign.RIGHT)
                                            .margin(DocumentInsets.zero()))))

                    // ── Thin orange rule ──────────────────────────
                    .addShape(s -> s.size(516, 1.2).fillColor(ACCENT)
                            .margin(DocumentInsets.zero()))

                    // ── Hero frame: nested rounded shapes form a thick
                    //     orange border around the brand artwork (avoids the
                    //     stroke-on-rounded-path corner artifact at thick
                    //     stroke widths). Outer = ACCENT fill, inner = NAVY
                    //     fill clipped to round, image clipped inside that.
                    .addContainer(frame -> frame
                            .name("HeroFrame")
                            .roundedRect(519, 287, 16)
                            .fillColor(ACCENT)
                            .center(new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                                    .name("HeroInner")
                                    .roundedRect(513, 281, 13)
                                    .fillColor(NAVY_DARK)
                                    .clipPolicy(ClipPolicy.CLIP_PATH)
                                    .center(new com.demcha.compose.document.dsl.ImageBuilder()
                                            .name("HeroImage")
                                            .source(hero)
                                            .size(509, 277)
                                            .fitMode(DocumentImageFitMode.COVER)
                                            .build())
                                    .build()))

                    // ── Magazine-headline lockup ──────────────────
                    .addSection("Lockup", section -> section
                            .padding(new DocumentInsets(8, 4, 0, 4))
                            .spacing(2)
                            .addParagraph(p -> p
                                    .text("One engine.")
                                    .textStyle(headline())
                                    .margin(DocumentInsets.zero()))
                            .addParagraph(p -> p
                                    .text("Every PDF you ship.")
                                    .textStyle(headlineAccent())
                                    .margin(DocumentInsets.zero()))
                            .addParagraph(p -> p
                                    .text("A declarative Java engine for production PDFs. Semantic graph compiles to deterministic layout, paginates predictably, and renders through PDFBox — without leaking PDFBox into your app.")
                                    .textStyle(tagline())
                                    .lineSpacing(1.35)
                                    .margin(new DocumentInsets(6, 0, 0, 0))))

                    // ── Three KPI cards — engine capability counts ─
                    .addRow("KpiRow", row -> row
                            .spacing(12)
                            .weights(1, 1, 1)
                            .addSection("KpiPresets", section -> kpiCard(section,
                                    "31", "V2 Presets",
                                    "Layered CV + cover-letter preset architecture — data · theme · components · widgets, swap one without rewriting the others."))
                            .addSection("KpiTests", section -> kpiCard(section,
                                    "1,033", "Tests · 0 failures",
                                    "Snapshot baselines, japicmp binary-compat gate, parallel-session stress harness, JMH benchmark suite."))
                            .addSection("KpiNodes", section -> kpiCard(section,
                                    "17", "Semantic Primitives",
                                    "Modules, sections, paragraphs, lists, tables, rows, shapes, images — every node is themable and snapshot-tested.")))

                    // ── Capability columns ────────────────────────
                    .addRow("Capabilities", row -> row
                            .spacing(14)
                            .weights(1, 1, 1)
                            .addSection("CapDsl", section -> capabilityColumn(section,
                                    "DECLARATIVE BY DESIGN",
                                    "Compose by intent — modules, sections, rows. Zero PDFBox imports in your application code."))
                            .addSection("CapDeterm", section -> capabilityColumn(section,
                                    "DETERMINISTIC RENDER",
                                    "Identical input, identical PDF — byte-comparable. Layout snapshots, JMH-benchmarked hot paths."))
                            .addSection("CapTheme", section -> capabilityColumn(section,
                                    "CINEMATIC THEMING",
                                    "BusinessTheme · CvTheme palettes, component-level tokens, v2 layered preset architecture.")))

                    // ── Footer brand stripe ───────────────────────
                    .addShape(s -> s.size(516, 0.6).fillColor(RULE)
                            .margin(new DocumentInsets(8, 0, 0, 0)))
                    .addRow("Footer", row -> row
                            .spacing(0)
                            .weights(2, 1)
                            .addSection("FooterLeft", section -> section
                                    .addParagraph(p -> p
                                            .text("graphcompose.dev  ·  github.com/DemchaAV/GraphCompose")
                                            .textStyle(footer())
                                            .margin(DocumentInsets.zero())))
                            .addSection("FooterRight", section -> section
                                    .addParagraph(p -> p
                                            .text("Java · PDFBox · MIT")
                                            .textStyle(footer())
                                            .align(TextAlign.RIGHT)
                                            .margin(DocumentInsets.zero()))))

                    .build();

            document.buildPdf();
        }
        return output;
    }

    private static DocumentImageData loadHeroImage() throws Exception {
        try (InputStream is = Objects.requireNonNull(
                EngineShowcase.class.getResourceAsStream("/engine-hero.png"),
                "engine-hero.png missing from examples/src/main/resources/")) {
            return DocumentImageData.fromBytes(is.readAllBytes());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    private static void kpiCard(com.demcha.compose.document.dsl.SectionBuilder section,
                                String value, String label, String detail) {
        section
                .softPanel(DocumentColor.WHITE, 8, 12)
                .stroke(DocumentStroke.of(CARD_RING, 0.5))
                .spacing(2)
                .addParagraph(p -> p
                        .text(value)
                        .textStyle(kpiValue())
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p
                        .text(label)
                        .textStyle(kpiLabel())
                        .margin(new DocumentInsets(0, 0, 0, 0)))
                .addParagraph(p -> p
                        .text(detail)
                        .textStyle(kpiDetail())
                        .lineSpacing(1.25)
                        .margin(new DocumentInsets(4, 0, 0, 0)));
    }

    private static void capabilityColumn(com.demcha.compose.document.dsl.SectionBuilder section,
                                         String label, String prose) {
        section
                // Tab-on-left callout: round only the right corners so
                // the accent rule on the left sits flush against a
                // straight edge — square cap on the rule matches a
                // square left side without crossing a rounded curve.
                .softPanel(PANEL_TINT, DocumentCornerRadius.right(6), 0)
                .accentLeft(ACCENT, 3.0f)
                .padding(new DocumentInsets(12, 12, 12, 16))
                .spacing(3)
                .addParagraph(p -> p
                        .text(label)
                        .textStyle(capLabel())
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p
                        .text(prose)
                        .textStyle(capProse())
                        .lineSpacing(1.3)
                        .margin(new DocumentInsets(2, 0, 0, 0)));
    }

    // ── Text styles ──────────────────────────────────────────────

    private static DocumentTextStyle bandLeft() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(10)
                .color(NAVY)
                .build();
    }

    private static DocumentTextStyle bandRight() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(9)
                .color(MUTED)
                .build();
    }

    private static DocumentTextStyle headline() {
        return DocumentTextStyle.builder()
                .fontName(FontName.TIMES_BOLD)
                .size(38)
                .color(INK)
                .build();
    }

    private static DocumentTextStyle headlineAccent() {
        return DocumentTextStyle.builder()
                .fontName(FontName.TIMES_BOLD_ITALIC)
                .size(38)
                .color(ACCENT)
                .build();
    }

    private static DocumentTextStyle tagline() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(11)
                .color(INK)
                .build();
    }

    private static DocumentTextStyle kpiValue() {
        return DocumentTextStyle.builder()
                .fontName(FontName.TIMES_BOLD)
                .size(26)
                .color(NAVY)
                .build();
    }

    private static DocumentTextStyle kpiLabel() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(9)
                .color(ACCENT)
                .build();
    }

    private static DocumentTextStyle kpiDetail() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(8)
                .color(MUTED)
                .build();
    }

    private static DocumentTextStyle capLabel() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(9)
                .color(NAVY)
                .build();
    }

    private static DocumentTextStyle capProse() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(9)
                .color(INK)
                .build();
    }

    private static DocumentTextStyle footer() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(8)
                .color(MUTED)
                .build();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
