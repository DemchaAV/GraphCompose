package com.demcha.examples.flagships;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.PathBuilder;
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
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.examples.support.ExampleVersion;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Single-slide "Available on Maven Central" brand banner, composed with the
 * canonical document DSL and emitted through the fixed-layout PPTX backend via
 * {@link DocumentSession#buildPptx(java.nio.file.Path)}. One resolved 16:9 page becomes one
 * identically-sized slide carrying the same geometry, so the {@code .pptx}
 * opens in PowerPoint as an editable copy of the banner — gradient, rounded
 * panels, native paths and text frames.
 *
 * <p>The palette is a warm amber accent on a deep navy field: the wordmark and
 * tagline sit over a Maven-coordinate card and capability tags, while a compact
 * "one model, two outputs" diagram on the right traces DSL code through the
 * layout engine to PDF and PPTX documents.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.1.0
 */
public final class MavenBannerPptxExample {

    /** 16:9 slide canvas shared by the PPTX slide, the PDF copy and the preview image. */
    private static final double PAGE_W = DocumentPageSize.SLIDE_16_9.width();
    private static final double PAGE_H = DocumentPageSize.SLIDE_16_9.height();

    // Deep-navy surfaces.
    private static final DocumentColor NIGHT = DocumentColor.rgb(13, 17, 33);
    private static final DocumentColor NIGHT_2 = DocumentColor.rgb(18, 23, 44);
    private static final DocumentColor CARD = DocumentColor.rgb(20, 26, 47);
    private static final DocumentColor CARD_2 = DocumentColor.rgb(25, 32, 56);
    private static final DocumentColor COORD_BG = DocumentColor.rgb(16, 21, 40);
    private static final DocumentColor BORDER = DocumentColor.rgb(48, 56, 86);

    // Warm amber accent family.
    private static final DocumentColor ORANGE = DocumentColor.rgb(233, 151, 45);
    private static final DocumentColor ORANGE_HI = DocumentColor.rgb(242, 168, 68);

    // Text.
    private static final DocumentColor WHITE_TX = DocumentColor.rgb(247, 248, 252);
    private static final DocumentColor SUBTLE_TX = DocumentColor.rgb(206, 212, 230);
    private static final DocumentColor MUTED_TX = DocumentColor.rgb(140, 149, 175);
    private static final DocumentColor FAINT_TX = DocumentColor.rgb(104, 114, 142);
    private static final DocumentColor CODE_BASE = DocumentColor.rgb(176, 184, 210);
    private static final DocumentColor CODE_STR = DocumentColor.rgb(122, 205, 168);

    // Document icons (light paper on dark field).
    private static final DocumentColor PAPER = DocumentColor.rgb(238, 241, 248);
    private static final DocumentColor PAPER_HEAD = DocumentColor.rgb(28, 35, 58);
    private static final DocumentColor PAPER_LINE = DocumentColor.rgb(198, 205, 222);

    /** Dark checkmark drawn over the amber "verified" chip in the Maven badge. */
    private static final String CHECK_SVG =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"none\">"
                    + "<path d=\"M5 12.5 L10 17.5 L19 6.5\" stroke=\"#12162B\" stroke-width=\"2.8\" "
                    + "stroke-linecap=\"round\" stroke-linejoin=\"round\"/></svg>";

    private MavenBannerPptxExample() {
    }

    /**
     * Builds the banner as a one-slide {@code .pptx}.
     *
     * @return the generated file path
     * @throws Exception when rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("flagships", "maven-banner.pptx");
        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.SLIDE_16_9)
                .pageBackground(NIGHT)
                .margin(DocumentInsets.zero())
                .create()) {
            compose(document);
            document.buildPptx(outputFile);
        }
        return outputFile;
    }

    /**
     * Builds the same banner as a single-page vector PDF copy.
     *
     * @return the generated file path
     * @throws Exception when rendering fails
     */
    public static Path generatePdf() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("flagships", "maven-banner.pdf");
        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.SLIDE_16_9)
                .pageBackground(NIGHT)
                .margin(DocumentInsets.zero())
                .create()) {
            compose(document);
            document.buildPdf();
        }
        return outputFile;
    }

    /**
     * Renders the banner straight to a PNG preview without a document round trip.
     *
     * @param outputPng destination file
     * @param dpi       raster resolution
     * @return the written path
     * @throws Exception when rendering fails
     */
    public static Path renderPreview(Path outputPng, int dpi) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.SLIDE_16_9)
                .pageBackground(NIGHT)
                .margin(DocumentInsets.zero())
                .create()) {
            compose(document);
            java.awt.image.BufferedImage image = document.toImage(0, dpi);
            Path parent = outputPng.toAbsolutePath().getParent();
            if (parent != null) {
                java.nio.file.Files.createDirectories(parent);
            }
            if (!javax.imageio.ImageIO.write(image, "png", outputPng.toFile())) {
                throw new IllegalStateException("No PNG writer accepted the banner: " + outputPng);
            }
        }
        return outputPng;
    }

    static void compose(DocumentSession document) {
        document.metadata(DocumentMetadata.builder()
                .title("GraphCompose — available on Maven Central")
                .author("GraphCompose")
                .subject("Declarative document generation for Java — one model, PDF and PPTX outputs")
                .keywords("graphcompose, java, maven central, pdf, pptx, document engine")
                .creator("GraphCompose Examples")
                .producer("GraphCompose")
                .build());
        document.pageFlow()
                .name("MavenBanner")
                .add(banner())
                .build();
    }

    // ---------------------------------------------------------------------
    // Full-bleed banner scene.
    // ---------------------------------------------------------------------

    private static DocumentNode banner() {
        List<CanvasChild> layers = new ArrayList<>();
        layers.add(at(gradientBackground(), 0, 0));
        layers.add(at(glow(360, ORANGE.withOpacity(0.06)), 690, -120));
        layers.addAll(dottedGrid());
        layers.add(at(cornerBracket(true), 902, 22));
        layers.add(at(cornerBracket(false), 22, 476));

        // ---- Left column ---------------------------------------------------
        layers.add(at(mavenBadge(), 52, 46));

        layers.add(at(text("Title", "GraphCompose", sansBold(60, WHITE_TX), TextAlign.LEFT, 40), 50, 96));
        layers.add(at(rect(78, 5, ORANGE), 55, 178));
        layers.add(at(text("Tagline", "Declarative document generation for Java",
                sans(20.5, SUBTLE_TX), TextAlign.LEFT, 430), 52, 196));

        layers.add(at(coordinateCard(), 52, 250));
        layers.addAll(tagRow(52, 344));

        layers.add(at(flowCaption(), 52, 440));
        layers.add(at(text("License", "Open-source Java library  ·  MIT License",
                sans(11.5, FAINT_TX), TextAlign.LEFT, 430), 52, 476));

        // ---- Right column: one-model diagram -------------------------------
        layers.add(at(codeCard(), 520, 118));
        layers.addAll(connectors());
        layers.add(at(layoutCard(), 700, 150));
        layers.add(at(documentIcon("PDF", ORANGE), 858, 74));
        layers.add(at(documentIcon("PPTX", ORANGE_HI), 858, 296));
        layers.add(at(text("OneModel", "One model. PDF and PPTX outputs.",
                sans(11.5, MUTED_TX), TextAlign.CENTER, PAGE_W - 380), 556, 430));

        return new CanvasLayerNode("MavenBanner", PAGE_W, PAGE_H, layers,
                ClipPolicy.CLIP_BOUNDS, DocumentInsets.zero(), DocumentInsets.zero());
    }

    private static DocumentNode gradientBackground() {
        return new ShapeBuilder()
                .name("Background")
                .size(PAGE_W, PAGE_H)
                .fill(new DocumentPaint.Linear(List.of(
                        new DocumentPaint.Stop(0.0, NIGHT),
                        new DocumentPaint.Stop(0.6, DocumentColor.rgb(15, 20, 39)),
                        new DocumentPaint.Stop(1.0, NIGHT_2)), 18.0))
                .build();
    }

    private static DocumentNode glow(double diameter, DocumentColor color) {
        return new EllipseBuilder().name("Glow").circle(diameter).fillColor(color).build();
    }

    private static List<CanvasChild> dottedGrid() {
        List<CanvasChild> dots = new ArrayList<>();
        DocumentColor dotColor = DocumentColor.rgba(120, 132, 170, 40);
        for (double x = 516; x <= 930; x += 30) {
            for (double y = 78; y <= 384; y += 30) {
                dots.add(at(new EllipseBuilder().circle(2.4).fillColor(dotColor).build(), x, y));
            }
        }
        return dots;
    }

    /** Thin L-bracket; {@code topRight} flips it for the opposite corner. */
    private static DocumentNode cornerBracket(boolean topRight) {
        DocumentColor c = DocumentColor.rgb(78, 88, 118);
        double len = 42, t = 2;
        ShapeContainerBuilder b = new ShapeContainerBuilder()
                .name(topRight ? "BracketTR" : "BracketBL")
                .rectangle(len, len)
                .fillColor(DocumentColor.rgba(0, 0, 0, 0))
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE);
        if (topRight) {
            b.position(rect(len, t, c), 0, 0, LayerAlign.TOP_RIGHT);
            b.position(rect(t, len, c), 0, 0, LayerAlign.TOP_RIGHT);
        } else {
            b.position(rect(len, t, c), 0, 0, LayerAlign.BOTTOM_LEFT);
            b.position(rect(t, len, c), 0, 0, LayerAlign.BOTTOM_LEFT);
        }
        return b.build();
    }

    private static DocumentNode mavenBadge() {
        DocumentNode chip = new ShapeContainerBuilder()
                .name("MavenCheck")
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .roundedRect(20, 20, 6)
                .fillColor(ORANGE)
                .center(SvgIcon.parse(CHECK_SVG).node(15))
                .build();
        return new ShapeContainerBuilder()
                .name("MavenBadge")
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .roundedRect(244, 34, 17)
                .fillColor(CARD)
                .stroke(DocumentStroke.of(ORANGE.withOpacity(0.5), 1.0))
                .position(chip, 8, 0, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder()
                        .text("AVAILABLE ON MAVEN CENTRAL")
                        .textStyle(monoBold(10, SUBTLE_TX))
                        .margin(DocumentInsets.zero())
                        .build(), 36, 0, LayerAlign.CENTER_LEFT)
                .build();
    }

    private static DocumentNode coordinateCard() {
        return new ShapeContainerBuilder()
                .name("CoordinateCard")
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .roundedRect(436, 66, 12)
                .fillColor(COORD_BG)
                .stroke(DocumentStroke.of(BORDER, 1.0))
                .position(new ParagraphBuilder()
                        .text("io.github.demchaav:graph-compose")
                        .textStyle(mono(14.5, WHITE_TX))
                        .margin(DocumentInsets.zero())
                        .build(), 22, -12, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder()
                        .text("v" + ExampleVersion.withoutQualifier())
                        .textStyle(monoBold(12.5, ORANGE))
                        .margin(DocumentInsets.zero())
                        .build(), 22, 13, LayerAlign.CENTER_LEFT)
                .build();
    }

    private static List<CanvasChild> tagRow(double x0, double y) {
        String[] tags = {"JAVA 17+", "PDF", "PPTX", "AUTO-PAGINATION"};
        double[] widths = {84, 54, 62, 152};
        List<CanvasChild> row = new ArrayList<>();
        double x = x0;
        for (int i = 0; i < tags.length; i++) {
            row.add(at(tagPill(tags[i], widths[i]), x, y));
            x += widths[i] + 11;
        }
        return row;
    }

    private static DocumentNode tagPill(String label, double w) {
        return new ShapeContainerBuilder()
                .name("Tag_" + label)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .roundedRect(w, 30, 9)
                .fillColor(CARD)
                .stroke(DocumentStroke.of(BORDER, 1.0))
                .center(new ParagraphBuilder()
                        .text(label)
                        .textStyle(monoBold(10, SUBTLE_TX))
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero())
                        .build())
                .build();
    }

    private static DocumentNode flowCaption() {
        return new ParagraphBuilder()
                .name("FlowCaption")
                .textStyle(sansBold(18, WHITE_TX))
                .rich(r -> r
                        .plain("Code   ")
                        .accent("›", ORANGE)
                        .plain("   layout   ")
                        .accent("›", ORANGE)
                        .plain("   finished document"))
                .margin(new DocumentInsets(0, 430, 0, 0))
                .build();
    }

    // ---- Right-side diagram --------------------------------------------------

    private static DocumentNode codeCard() {
        ShapeContainerBuilder card = new ShapeContainerBuilder()
                .name("CodeCard")
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .roundedRect(112, 152, 12)
                .fillColor(CARD_2)
                .stroke(DocumentStroke.of(BORDER, 1.0))
                .position(new ParagraphBuilder().text("JAVA").textStyle(monoBold(8.5, MUTED_TX))
                        .margin(DocumentInsets.zero()).build(), 14, 13, LayerAlign.TOP_LEFT)
                .position(rect(84, 1, BORDER), 14, 31, LayerAlign.TOP_LEFT);

        // Leading whitespace in a run is trimmed, so indentation is expressed as
        // an x-offset per nesting level instead of leading spaces.
        Object[][] code = {
                {"document", " {", 0},
                {"column", " {", 1},
                {"text", "()", 2},
                {"table", "()", 2},
                {"}", "", 1},
                {"}", "", 0},
        };
        double y = 44;
        for (Object[] line : code) {
            final String head = (String) line[0];
            final String tail = (String) line[1];
            final int indent = (int) line[2];
            final DocumentColor headColor = head.equals("}") ? CODE_BASE
                    : (indent <= 1 ? ORANGE : CODE_STR);
            card.position(new ParagraphBuilder()
                    .textStyle(mono(9, CODE_BASE))
                    .rich(r -> r.accent(head, headColor).plain(tail))
                    .margin(DocumentInsets.zero())
                    .build(), 14 + indent * 12, y, LayerAlign.TOP_LEFT);
            y += 17;
        }
        return card.build();
    }

    private static DocumentNode layoutCard() {
        DocumentNode square = new ShapeBuilder().size(28, 28)
                .fillColor(ORANGE.withOpacity(0.22))
                .stroke(DocumentStroke.of(ORANGE, 1.0))
                .cornerRadius(DocumentCornerRadius.of(7)).build();
        return new ShapeContainerBuilder()
                .name("LayoutCard")
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .roundedRect(118, 102, 14)
                .fillColor(CARD_2)
                .stroke(DocumentStroke.of(ORANGE, 1.4))
                .position(new ShapeBuilder().size(64, 12).fillColor(ORANGE)
                        .cornerRadius(DocumentCornerRadius.of(6)).build(), 0, 22, LayerAlign.TOP_CENTER)
                .position(square, -18, 44, LayerAlign.TOP_CENTER)
                .position(square, 18, 44, LayerAlign.TOP_CENTER)
                .position(new ParagraphBuilder().text("LAYOUT").textStyle(monoBold(9, SUBTLE_TX))
                        .align(TextAlign.CENTER).margin(DocumentInsets.zero()).build(),
                        0, -14, LayerAlign.BOTTOM_CENTER)
                .build();
    }

    /**
     * Amber connectors + arrowheads linking the code card → layout → both
     * output backends. The layout box feeds a vertical distribution bus that
     * branches into the PDF icon (top) and the PPTX icon (bottom), so the flow
     * visibly reaches each backend.
     */
    private static List<CanvasChild> connectors() {
        double pdfMidY = 132;    // PDF icon left-edge centre
        double pptxMidY = 354;   // PPTX icon left-edge centre
        double busX = 836;       // vertical distribution bus
        double docX = 858;       // both icons' left edge
        List<CanvasChild> parts = new ArrayList<>();
        // code → layout.
        parts.add(at(rect(56, 2, ORANGE), 636, 200));
        parts.add(at(arrowHead(700, 201), 0, 0));
        // layout → distribution bus.
        parts.add(at(rect(busX - 818 + 1, 2, ORANGE), 818, 200));
        parts.add(at(rect(2, pptxMidY - pdfMidY + 2, ORANGE), busX, pdfMidY - 1));
        // bus → each backend.
        parts.add(at(rect(docX - busX, 2, ORANGE), busX, pdfMidY - 1));
        parts.add(at(rect(docX - busX, 2, ORANGE), busX, pptxMidY - 1));
        parts.add(at(arrowHead(docX, pdfMidY), 0, 0));
        parts.add(at(arrowHead(docX, pptxMidY), 0, 0));
        return parts;
    }

    private static DocumentNode arrowHead(double tipX, double tipY) {
        return new PathBuilder().name("Head_" + Math.round(tipX) + "_" + Math.round(tipY)).size(PAGE_W, PAGE_H)
                .moveTo(tipX - 9, tipY - 6)
                .lineTo(tipX - 9, tipY + 6)
                .lineTo(tipX + 1, tipY)
                .closePath()
                .fillColor(ORANGE)
                .build();
    }

    private static DocumentNode documentIcon(String format, DocumentColor tabColor) {
        double w = 92, h = 116;
        ShapeContainerBuilder doc = new ShapeContainerBuilder()
                .name("Doc_" + format)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .roundedRect(w, h, 8)
                .fillColor(PAPER)
                .stroke(DocumentStroke.of(BORDER, 0.8))
                .position(new ShapeBuilder().size(56, 12).fillColor(PAPER_HEAD)
                        .cornerRadius(DocumentCornerRadius.of(3)).build(), 14, 18, LayerAlign.TOP_LEFT)
                .position(new ShapeBuilder().size(44, 5).fillColor(ORANGE)
                        .cornerRadius(DocumentCornerRadius.of(2.5)).build(), 14, 40, LayerAlign.TOP_LEFT)
                .position(rect(64, 4, PAPER_LINE), 14, 56, LayerAlign.TOP_LEFT)
                .position(rect(64, 4, PAPER_LINE), 14, 68, LayerAlign.TOP_LEFT)
                .position(rect(64, 4, PAPER_LINE), 14, 80, LayerAlign.TOP_LEFT)
                .position(rect(40, 4, PAPER_LINE), 14, 92, LayerAlign.TOP_LEFT);
        // Format tab, top-right.
        double tabW = format.length() >= 4 ? 38 : 32;
        doc.position(new ShapeContainerBuilder().name("Tab_" + format)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .roundedRect(tabW, 16, 5)
                .fillColor(tabColor)
                .center(new ParagraphBuilder().text(format).textStyle(monoBold(7.5, NIGHT))
                        .align(TextAlign.CENTER).margin(DocumentInsets.zero()).build())
                .build(), w - tabW - 8, 9, LayerAlign.TOP_LEFT);
        return doc.build();
    }

    // ---- primitives / styles -------------------------------------------------

    private static DocumentNode text(String name, String value, DocumentTextStyle style,
                                     TextAlign align, double rightMargin) {
        return new ParagraphBuilder()
                .name(name)
                .text(value)
                .textStyle(style)
                .align(align)
                .lineSpacing(1.15)
                .margin(new DocumentInsets(0, rightMargin, 0, 0))
                .build();
    }

    private static CanvasChild at(DocumentNode node, double x, double y) {
        return new CanvasChild(node, x, y);
    }

    private static DocumentNode rect(double w, double h, DocumentColor fill) {
        return new ShapeBuilder().size(w, h).fillColor(fill).build();
    }

    private static DocumentTextStyle sans(double size, DocumentColor color) {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(size).color(color).build();
    }

    private static DocumentTextStyle sansBold(double size, DocumentColor color) {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA_BOLD).size(size).color(color).build();
    }

    private static DocumentTextStyle mono(double size, DocumentColor color) {
        return DocumentTextStyle.builder().fontName(FontName.COURIER).size(size).color(color).build();
    }

    private static DocumentTextStyle monoBold(double size, DocumentColor color) {
        return DocumentTextStyle.builder().fontName(FontName.COURIER_BOLD).size(size).color(color).build();
    }

    /**
     * CLI entry point: writes the PPTX, a PDF copy and a PNG preview.
     *
     * @param args ignored
     * @throws Exception when rendering fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println("PPTX:    " + generate());
        System.out.println("PDF:     " + generatePdf());
        System.out.println("Preview: " + renderPreview(
                ExampleOutputPaths.prepare("flagships", "maven-banner.png"), 150));
    }
}
