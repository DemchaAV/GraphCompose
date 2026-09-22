package com.demcha.examples.support;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.CanvasChild;
import com.demcha.compose.document.node.CanvasLayerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.font.FontName;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Renders the link-preview cover the showcase site publishes as its {@code og:image}.
 *
 * <p>The home page used to hand X and Slack a portrait page of one proposal, 893 by 1263.
 * A large-image card is landscape, so the preview showed a crop of whatever fell in the
 * middle of that page — a paragraph of a document nobody had asked about, under a link
 * about the library. This is a cover drawn for that shape: 1200 by 630, the size every
 * platform crops least, with the wordmark, what the library does, and three real documents
 * from the catalogue standing in for the rest.</p>
 *
 * <p>Nothing on it dates: no version, no measured figure, no release name. A cover that
 * carries one has to be redrawn on the cut, and a cut that forgets publishes a stale claim
 * to every feed that reads the page. What the page states about the release is stated in
 * the page, where {@code web-src/data/release.json} already moves it.</p>
 *
 * <p>The three documents are read from {@code web/showcase/thumbnails/} — the previews the
 * catalogue itself publishes, so the cover cannot show a template the site does not have,
 * and a rerun after a render change picks the new ones up.</p>
 *
 * <p>Usage — pass an explicit output path:</p>
 * <pre>
 * ./mvnw -B -ntp -f examples/pom.xml -DskipTests exec:java \
 *   -Dexec.mainClass=com.demcha.examples.support.SiteSocialCoverRenderer \
 *   -Dexec.args="&lt;outputPng&gt;"
 * </pre>
 *
 * @author Artem Demchyshyn
 * @since 2.4.2
 */
public final class SiteSocialCoverRenderer {

    /** The size a large-image card is composed at; every major platform crops it least. */
    public static final double COVER_WIDTH = 1200;
    /** The size a large-image card is composed at; every major platform crops it least. */
    public static final double COVER_HEIGHT = 630;

    /** 72 DPI over a page declared in points writes exactly COVER_WIDTH by COVER_HEIGHT pixels. */
    private static final int POINTS_PER_INCH = 72;

    // The site's own palette, as web/styles.css declares it for the dark theme.
    private static final DocumentColor NIGHT = DocumentColor.rgb(11, 16, 32);
    private static final DocumentColor SURFACE = DocumentColor.rgb(17, 24, 39);
    private static final DocumentColor GRID = DocumentColor.rgb(30, 41, 69);
    private static final DocumentColor ON_DARK = DocumentColor.rgb(248, 250, 252);
    private static final DocumentColor ON_DARK_MUTED = DocumentColor.rgb(148, 163, 184);
    private static final DocumentColor ACCENT = DocumentColor.rgb(99, 102, 241);
    private static final DocumentColor ACCENT_TEXT = DocumentColor.rgb(165, 180, 252);
    private static final DocumentColor SHEET_EDGE = DocumentColor.rgb(203, 213, 225);

    /** The documents the cover shows, as their paths under the published catalogue. */
    private static final String[] SHOWN = {
            "web/showcase/thumbnails/templates/cv/cv-blue-banner-v2.png",
            "web/showcase/thumbnails/templates/invoice/invoice-modern-v2.png",
            "web/showcase/thumbnails/templates/proposal/proposal-editorial-v2.png"
    };

    /** Each shown document, drawn at this width; the height follows the thumbnails' 320x453. */
    private static final double SHEET_WIDTH = 210;
    private static final double SHEET_HEIGHT = SHEET_WIDTH * 453 / 320;

    /** The card around a sheet: the sheet plus 6pt of surface on every side. */
    private static final double SHEET_CARD_WIDTH = SHEET_WIDTH + 12;
    private static final double SHEET_CARD_HEIGHT = SHEET_HEIGHT + 12;

    /**
     * Where the fan starts and how far each sheet steps.
     *
     * <p>The last card has to land inside the page: at {@code LEFT + 2 * STEP + card width}
     * it ends at 1158 of 1200, leaving a margin that matches the one on the left. An earlier
     * pass stepped 176 from 648 and put the third document 48 points past the right edge,
     * which a render showed and no assertion would have.</p>
     */
    private static final double FAN_LEFT = 636;
    private static final double FAN_STEP = 150;

    /** The fan, centred in the page, with the middle sheet lifted to give it a front. */
    private static final double FAN_TOP = (COVER_HEIGHT - SHEET_CARD_HEIGHT) / 2;
    private static final double FAN_LIFT = 28;

    private SiteSocialCoverRenderer() {
    }

    /**
     * Runs the renderer.
     *
     * @param args one argument — where to write the PNG
     * @throws Exception when composition or rendering fails
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args[0].isBlank()) {
            System.err.println("Usage: SiteSocialCoverRenderer <outputPng>");
            System.exit(2);
        }
        Path written = render(Paths.get(args[0]).toAbsolutePath().normalize());
        System.out.println("Generated: " + written + " ("
                + (int) COVER_WIDTH + "x" + (int) COVER_HEIGHT + ")");
    }

    /**
     * Composes the cover and writes it as a PNG, creating parent directories as needed.
     *
     * @param outputPng destination file
     * @return the written path
     * @throws Exception when composition or rendering fails
     */
    public static Path render(Path outputPng) throws Exception {
        BufferedImage image;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(COVER_WIDTH, COVER_HEIGHT)
                .pageBackground(NIGHT)
                .margin(DocumentInsets.zero())
                .create()) {
            document.pageFlow().name("SocialCover").add(scene()).build();
            image = document.toImage(0, POINTS_PER_INCH);
        }
        Files.createDirectories(outputPng.toAbsolutePath().getParent());
        ImageIO.write(image, "png", outputPng.toFile());
        return outputPng;
    }

    /** The cover as a single node. */
    private static DocumentNode scene() {
        List<CanvasChild> layers = new ArrayList<>(grid());

        // The wordmark carries the mark and the name together - it is one set of paths, so
        // the two cannot drift apart or be scaled against each other by accident.
        layers.add(at(icon("logo").node(430), 72, 92));
        layers.add(at(rule(96), 76, 232));
        layers.add(at(text("Tagline", "Document generation for Java",
                display(36, ON_DARK), 520), 74, 260));
        layers.add(at(text("Subtitle",
                "Compose a document once. Render it to PDF, PowerPoint or Word.",
                body(18, ON_DARK_MUTED), 470), 76, 322));
        layers.add(at(text("Formats", "PDF   ·   PPTX   ·   DOCX",
                body(15, ACCENT_TEXT), 420), 76, 470));

        // Three real documents from the catalogue, fanned so each stays readable as a shape
        // while the group reads as a stack rather than as three unrelated pictures.
        for (int i = 0; i < SHOWN.length; i++) {
            boolean middle = i == SHOWN.length / 2;
            layers.add(at(sheet(SHOWN[i]),
                    FAN_LEFT + i * FAN_STEP,
                    middle ? FAN_TOP - FAN_LIFT : FAN_TOP));
        }
        return new CanvasLayerNode("SocialCover", COVER_WIDTH, COVER_HEIGHT, layers,
                ClipPolicy.CLIP_BOUNDS, DocumentInsets.zero(), DocumentInsets.zero());
    }

    /** One catalogue document, in a card with the site's own surface and edge. */
    private static DocumentNode sheet(String thumbnail) {
        Path file = repoRoot().resolve(thumbnail);
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("the cover names a preview the catalogue does not "
                    + "publish: " + thumbnail);
        }
        return new ShapeContainerBuilder()
                .name("Sheet")
                .roundedRect(SHEET_CARD_WIDTH, SHEET_CARD_HEIGHT, 10)
                .fillColor(SURFACE)
                .stroke(DocumentStroke.of(SHEET_EDGE.withOpacity(0.28), 1))
                .position(new ImageBuilder()
                        .name("Preview")
                        .source(file)
                        .size(SHEET_WIDTH, SHEET_HEIGHT)
                        .build(), 6, 6, LayerAlign.TOP_LEFT)
                .build();
    }

    /** The faint grid the site's own hero carries, so the cover reads as part of it. */
    private static List<CanvasChild> grid() {
        List<CanvasChild> lines = new ArrayList<>();
        for (double y = 0; y <= COVER_HEIGHT; y += 42) {
            lines.add(at(new ShapeBuilder().size(COVER_WIDTH, 1)
                    .fillColor(GRID.withOpacity(0.55)).build(), 0, y));
        }
        for (double x = 0; x <= COVER_WIDTH; x += 42) {
            lines.add(at(new ShapeBuilder().size(1, COVER_HEIGHT)
                    .fillColor(GRID.withOpacity(0.55)).build(), x, 0));
        }
        return lines;
    }

    private static DocumentNode rule(double width) {
        return new ShapeBuilder().size(width, 4).fillColor(ACCENT).build();
    }

    private static CanvasChild at(DocumentNode node, double x, double y) {
        return new CanvasChild(node, x, y);
    }

    private static DocumentNode text(String name, String value, DocumentTextStyle style,
                                     double width) {
        return new ParagraphBuilder()
                .name(name)
                .text(value)
                .textStyle(style)
                .align(TextAlign.LEFT)
                .lineSpacing(1.3)
                .margin(new DocumentInsets(0, COVER_WIDTH - width, 0, 0))
                .build();
    }

    /**
     * Bold display type. The weight comes from the decoration rather than from a
     * {@code *_BOLD} font name: the standard-14 style variants are family aliases, so naming
     * one selects the family and leaves the face at regular.
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

    private static SvgIcon icon(String name) {
        try (InputStream in = Objects.requireNonNull(
                SiteSocialCoverRenderer.class.getResourceAsStream("/showcase/" + name + ".svg"),
                "showcase icon missing: " + name)) {
            return SvgIcon.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("failed to load showcase icon: " + name, e);
        }
    }

    /**
     * The repository root, found by walking up from the working directory until the published
     * catalogue is under it. The renderer is started from the examples module by the site
     * tooling and from the repository root by hand, and the previews it reads live at a path
     * relative to neither.
     *
     * @return the directory holding {@code web/showcase}
     */
    private static Path repoRoot() {
        Path here = Paths.get("").toAbsolutePath().normalize();
        for (Path candidate = here; candidate != null; candidate = candidate.getParent()) {
            if (Files.isDirectory(candidate.resolve("web/showcase/thumbnails"))) {
                return candidate;
            }
        }
        throw new IllegalStateException("no web/showcase/thumbnails above " + here
                + ": run this from the repository or the examples module");
    }
}
