package com.demcha.examples.flagships;

import com.demcha.compose.GraphCompose;
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
 * slide. The content bars inside both are placed from the <em>same</em> set of
 * relative fractions ({@link #CONTENT_BARS}), so the picture's assertion that
 * one layout arrives in two shapes is enforced by the code that draws it rather
 * than by the illustrator's hand.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.1.1
 */
public final class SocialCardExample {

    /** GitHub renders social previews at 1280x640; anything else is letterboxed. */
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

    /**
     * Content bars as fractions of the destination shape, in draw order:
     * {@code x, y, width, height}. Both outputs read this same table — that is
     * the whole point of the picture.
     */
    private static final double[][] CONTENT_BARS = {
            {0.07, 0.09, 0.62, 0.085},
            {0.07, 0.30, 0.72, 0.048},
            {0.07, 0.40, 0.55, 0.048},
    };

    /** The chart block, same fractions in both outputs. */
    private static final double[] CHART_BLOCK = {0.07, 0.56, 0.80, 0.32};

    /** Sparkline heights above the block floor, bottom-up like the path axis. */
    private static final double[] SERIES = {0.18, 0.45, 0.38, 0.66, 0.86};

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
     * Runs the example: writes the PDF, then the PNG beside it.
     *
     * @param args optional single argument — the PNG destination path
     * @throws Exception when composition or rendering fails
     */
    public static void main(String[] args) throws Exception {
        Path pdf = generate();
        System.out.println("Generated: " + pdf);
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

        // Left third: the identity block.
        layers.add(at(logoBlock(), 62, 214));
        layers.add(at(text("Tagline", "One layout pass. Two outputs.",
                display(20, ON_DARK), 360), 66, 300));
        layers.add(at(text("Sub",
                "The same resolved layout renders a PDF and an editable deck.",
                body(11.5, ON_DARK_MUTED), 350), 66, 334));
        layers.add(at(text("Footer", "Java 17+   ·   MIT   ·   Maven Central",
                mono(10, MINT), 340), 66, 370));

        // Right two thirds: the diagram.
        layers.addAll(beams());
        layers.add(at(sheet(), 452, 236));
        layers.add(at(outputCard("PdfOut", 232, 292, VIOLET, VIOLET_LIGHT), 892, 58));
        layers.add(at(outputCard("PptxOut", 356, 200, MINT, MINT), 828, 396));
        layers.add(at(text("PdfLabel", "PDF", mono(9.5, VIOLET_LIGHT), 60), 892, 40));
        layers.add(at(text("PptxLabel", "PPTX", mono(9.5, MINT), 60), 828, 378));

        return new CanvasLayerNode("SocialCard", CARD_WIDTH, CARD_HEIGHT, layers,
                ClipPolicy.CLIP_BOUNDS, DocumentInsets.zero(), DocumentInsets.zero());
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
     * The source sheet, drawn as a plane in three-quarter view with a folded
     * corner — a document before anyone has decided what it will be rendered to.
     */
    private static DocumentNode sheet() {
        // PathBuilder coordinates are fractions of the declared box, with y
        // running bottom-up — the same convention BookTemplateExample uses.
        return new PathBuilder()
                .name("SourceSheet")
                .size(148, 208)
                .moveTo(0.02, 0.97)
                .lineTo(0.74, 1.00)
                .lineTo(1.00, 0.80)
                .lineTo(1.00, 0.03)
                .lineTo(0.18, 0.00)
                .closePath()
                .fillColor(SURFACE.withOpacity(0.55))
                .stroke(DocumentStroke.of(SHEET_EDGE.withOpacity(0.85), 1.4))
                .build();
    }

    /**
     * The two beams. Flat translucent fills rather than gradients — the paint
     * surface exposes solid colours, and a flat wedge reads cleaner at the size
     * a social card is actually viewed.
     */
    private static List<CanvasChild> beams() {
        List<CanvasChild> out = new ArrayList<>();
        // Both wedges start on the sheet's right edge, so the eye follows one
        // document splitting rather than two unrelated shapes being lit.
        out.add(at(new PathBuilder()
                .name("BeamPdf")
                .size(292, 292)
                .moveTo(0.00, 0.21)
                .lineTo(1.00, 1.00)
                .lineTo(1.00, 0.00)
                .closePath()
                .fillColor(VIOLET.withOpacity(0.16))
                .build(), 600, 58));
        out.add(at(new PathBuilder()
                .name("BeamPptx")
                .size(228, 200)
                .moveTo(0.00, 0.88)
                .lineTo(1.00, 1.00)
                .lineTo(1.00, 0.00)
                .closePath()
                .fillColor(MINT.withOpacity(0.14))
                .build(), 600, 396));
        out.add(at(dot(9, VIOLET), 596, 286));
        out.add(at(dot(9, MINT), 596, 416));
        return out;
    }

    /**
     * One destination shape. The bars come from {@link #CONTENT_BARS} scaled to
     * this shape's box, so both outputs place the same content at the same
     * relative coordinates by construction.
     *
     * @param name canvas node name
     * @param width shape width in points
     * @param height shape height in points
     * @param outline outline colour
     * @param accent colour for the chart marks
     * @return the composed output shape
     */
    private static DocumentNode outputCard(String name, double width, double height,
                                           DocumentColor outline, DocumentColor accent) {
        ShapeContainerBuilder card = new ShapeContainerBuilder()
                .name(name)
                .roundedRect(width, height, 6)
                .fillColor(DocumentColor.rgba(12, 16, 34, 232))
                .stroke(DocumentStroke.of(outline, 1.5));

        for (double[] bar : CONTENT_BARS) {
            card.position(rect(width * bar[2], height * bar[3], ON_DARK.withOpacity(0.30)),
                    width * bar[0], height * bar[1], LayerAlign.TOP_LEFT);
        }

        double chartW = width * CHART_BLOCK[2];
        double chartH = height * CHART_BLOCK[3];
        card.position(chartBlock(chartW, chartH, accent),
                width * CHART_BLOCK[0], height * CHART_BLOCK[1], LayerAlign.TOP_LEFT);
        return card.build();
    }

    /**
     * A framed sparkline standing in for "a chart the engine drew".
     *
     * <p>{@code SERIES} values are heights above the block's floor, matching the
     * path's bottom-up axis; the marker dots are positioned by a container,
     * whose axis runs top-down, hence the inversion.</p>
     */
    private static DocumentNode chartBlock(double width, double height, DocumentColor accent) {
        ShapeContainerBuilder block = new ShapeContainerBuilder()
                .name("ChartBlock")
                .rectangle(width, height)
                .fillColor(DocumentColor.rgba(0, 0, 0, 0))
                .stroke(DocumentStroke.of(ON_DARK.withOpacity(0.22), 0.8));

        PathBuilder line = new PathBuilder().name("Spark").size(width, height);
        for (int i = 0; i < SERIES.length; i++) {
            double x = 0.10 + 0.20 * i;
            if (i == 0) {
                line.moveTo(x, SERIES[i]);
            } else {
                line.lineTo(x, SERIES[i]);
            }
        }
        block.position(line.stroke(DocumentStroke.of(accent, 1.6)).build(), 0, 0,
                LayerAlign.TOP_LEFT);
        for (int i = 0; i < SERIES.length; i++) {
            block.position(dot(5, accent),
                    width * (0.10 + 0.20 * i) - 2.5,
                    height * (1 - SERIES[i]) - 2.5,
                    LayerAlign.TOP_LEFT);
        }
        return block.build();
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
