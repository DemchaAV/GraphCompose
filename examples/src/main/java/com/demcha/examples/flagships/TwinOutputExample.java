package com.demcha.examples.flagships;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
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
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The dual-output hook, stated by the artifact itself: one 16:9 page written
 * once against the canonical DSL and emitted twice from the same
 * {@link DocumentSession} — a print-ready PDF via {@code buildPdf()} and a
 * PowerPoint deck via {@code buildPptx(Path)}. Both backends consume the same
 * resolved layout graph, so the slide carries the page's exact geometry, and
 * every panel, chip, and text line on it stays a native, editable PowerPoint
 * shape.
 *
 * <p>The page design reuses the {@link EngineDeckV2Example} hero language
 * (night gradient, violet accents, proof chips) so the pair slots into the
 * README next to the release banner.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.1.0
 */
public final class TwinOutputExample {

    private static final double PAGE_WIDTH = DocumentPageSize.SLIDE_16_9.width();
    private static final double PAGE_HEIGHT = DocumentPageSize.SLIDE_16_9.height();

    // Night surfaces shared with the v2 hero.
    private static final DocumentColor NIGHT = DocumentColor.rgb(9, 12, 27);
    private static final DocumentColor NIGHT_VIOLET = DocumentColor.rgb(31, 21, 61);
    private static final DocumentColor DARK_CARD = DocumentColor.rgb(20, 27, 51);
    private static final DocumentColor DARK_CARD_2 = DocumentColor.rgb(27, 34, 64);
    private static final DocumentColor DARK_LINE = DocumentColor.rgb(64, 67, 108);
    private static final DocumentColor ON_DARK = DocumentColor.rgb(245, 247, 252);
    private static final DocumentColor ON_DARK_MUTED = DocumentColor.rgb(177, 185, 207);
    private static final DocumentColor CODE_TXT = DocumentColor.rgb(190, 195, 214);
    private static final DocumentColor CODE_TYPE = DocumentColor.rgb(150, 190, 255);
    private static final DocumentColor CODE_STR = DocumentColor.rgb(120, 204, 170);

    // Accent family.
    private static final DocumentColor VIOLET = DocumentColor.rgb(124, 92, 252);
    private static final DocumentColor VIOLET_LIGHT = DocumentColor.rgb(181, 159, 255);
    private static final DocumentColor BLUE = DocumentColor.rgb(78, 161, 255);
    private static final DocumentColor MINT = DocumentColor.rgb(55, 214, 161);

    private TwinOutputExample() {
    }

    /**
     * Builds the page once and emits both artifacts from the same session.
     *
     * @return the generated PDF path; the .pptx twin sits next to it
     * @throws Exception when composition or rendering fails
     */
    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("flagships", "twin-output.pdf");
        Path pptxFile = pdfFile.resolveSibling("twin-output.pptx");
        try (DocumentSession document = GraphCompose.document(pdfFile)
                .pageSize(DocumentPageSize.SLIDE_16_9)
                .pageBackground(NIGHT)
                .margin(DocumentInsets.zero())
                .create()) {
            compose(document);
            document.buildPdf();
            document.buildPptx(pptxFile);
        }
        return pdfFile;
    }

    /**
     * Composes the single hook page shared by both outputs.
     *
     * @param document open session configured for the 960 x 540 slide canvas
     */
    static void compose(DocumentSession document) {
        document.metadata(DocumentMetadata.builder()
                .title("GraphCompose - one source, two formats")
                .author("GraphCompose")
                .subject("The same composed page emitted as a print PDF and an editable PowerPoint slide")
                .keywords("graphcompose, java, pdf, pptx, dual output, editable slides")
                .creator("GraphCompose Examples")
                .producer("GraphCompose")
                .build());
        document.pageFlow()
                .name("TwinOutput")
                .add(scene())
                .build();
    }

    private static DocumentNode scene() {
        List<CanvasChild> layers = new ArrayList<>();
        layers.add(at(background(), 0, 0));
        layers.add(at(glow(430, VIOLET.withOpacity(0.13)), 640, -170));
        layers.add(at(glow(300, BLUE.withOpacity(0.09)), 700, 190));

        layers.add(at(logoLockup(), 42, 24));

        layers.add(at(canvasText("Kicker", "GRAPHCOMPOSE / ONE SOURCE, TWO FORMATS",
                mono(8.8, MINT, true), 430), 52, 108));
        layers.add(at(canvasText("TitleA", "Written once.", display(32, ON_DARK), 420), 52, 134));
        layers.add(at(canvasText("TitleB", "Rendered twice.", display(32, VIOLET_LIGHT), 420), 52, 176));
        layers.add(at(canvasText("Lead",
                "One Java composition, one resolved layout graph, two backends: a print-ready "
                        + "PDF and a PowerPoint slide with the same geometry - where text, panels, "
                        + "and vectors stay native, editable shapes.",
                body(12.3, ON_DARK_MUTED), 450), 54, 228));

        layers.add(at(codeCard(), 52, 292));

        layers.add(at(canvasText("GraphLabel", "ONE RESOLVED LAYOUT / TWO BACKENDS",
                mono(8.8, MINT, true), 690), 622, 118));
        layers.add(at(layoutGraphPill(), 650, 148));
        layers.addAll(connectors());
        layers.add(at(formatCard("pdf-file", "twin-output.pdf",
                "fixed layout - print-ready", MINT), 585, 288));
        layers.add(at(formatCard("ppt-file", "twin-output.pptx",
                "native shapes - editable", BLUE), 785, 288));
        layers.add(at(canvasText("ParityNote", "same coordinates - same metrics - same colours",
                body(9.3, ON_DARK_MUTED), 690), 622, 398));

        String[][] proof = {
                {"one layout graph", "measured once"},
                {"1 page = 1 slide", "identical geometry"},
                {"shapes stay native", "edit in PowerPoint"},
                {"render-pptx", "beta - opt-in module"}
        };
        for (int i = 0; i < proof.length; i++) {
            layers.add(at(proofChip(proof[i][0], proof[i][1]), 48 + i * 224, 457));
        }

        return new CanvasLayerNode("TwinOutputHero", PAGE_WIDTH, PAGE_HEIGHT, layers,
                ClipPolicy.CLIP_BOUNDS, DocumentInsets.zero(), DocumentInsets.zero());
    }

    private static DocumentNode background() {
        return new ShapeBuilder()
                .name("TwinGradient")
                .size(PAGE_WIDTH, PAGE_HEIGHT)
                .fill(new DocumentPaint.Linear(List.of(
                        new DocumentPaint.Stop(0.0, NIGHT),
                        new DocumentPaint.Stop(0.58, DocumentColor.rgb(17, 17, 39)),
                        new DocumentPaint.Stop(1.0, NIGHT_VIOLET)), 12.0))
                .build();
    }

    private static DocumentNode glow(double diameter, DocumentColor color) {
        return new EllipseBuilder().name("TwinGlow").circle(diameter).fillColor(color).build();
    }

    private static DocumentNode logoLockup() {
        return new ShapeContainerBuilder()
                .name("TwinLogo")
                .rectangle(310, 72)
                .fillColor(DocumentColor.rgba(0, 0, 0, 0))
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(icon("logo").node(300), -10, -4, LayerAlign.CENTER_LEFT)
                .build();
    }

    /** One syntax-coloured slice of a code line: {@code kind} selects the style. */
    private record CodeRun(char kind, String text) {
    }

    private static CodeRun t(String text) {
        return new CodeRun('t', text);   // plain code
    }

    private static CodeRun c(String text) {
        return new CodeRun('c', text);   // class / type
    }

    private static CodeRun s(String text) {
        return new CodeRun('s', text);   // string literal
    }

    private static CodeRun k(String text) {
        return new CodeRun('k', text);   // keyword / hero call
    }

    private static DocumentColor codeColor(char kind) {
        return switch (kind) {
            case 'c' -> CODE_TYPE;
            case 's' -> CODE_STR;
            case 'k' -> VIOLET_LIGHT;
            default -> CODE_TXT;
        };
    }

    private static DocumentNode codeCard() {
        // {indent, runs, trailing comment} — indentation is positional because
        // paragraph text trims leading whitespace; each line is ONE paragraph
        // of styled inline runs, so both backends carry it as rich text (the
        // PPTX side lands one editable text frame with coloured runs per line).
        record CodeLine(int indent, List<CodeRun> runs, String note) {
        }
        List<CodeLine> lines = List.of(
                new CodeLine(0, List.of(c("Path"), t(" deck = "), c("Path"), t(".of("),
                        s("\"twin-output.pptx\""), t(");")), ""),
                new CodeLine(0, List.of(k("try"), t(" ("), c("DocumentSession"), t(" doc = "),
                        c("GraphCompose")), ""),
                new CodeLine(2, List.of(t(".document("), c("Path"), t(".of("),
                        s("\"twin-output.pdf\""), t("))")), ""),
                new CodeLine(2, List.of(t(".pageSize("), c("DocumentPageSize"),
                        t(".SLIDE_16_9)")), ""),
                new CodeLine(2, List.of(t(".create()) {")), ""),
                new CodeLine(1, List.of(t("compose(doc);")), "// one description"),
                new CodeLine(1, List.of(t("doc."), k("buildPdf"), t("();")), "// print-ready PDF"),
                new CodeLine(1, List.of(t("doc."), k("buildPptx"), t("(deck);")), "// editable PowerPoint"),
                new CodeLine(0, List.of(t("}")), ""));
        ShapeContainerBuilder card = new ShapeContainerBuilder()
                .name("TwinCode")
                .roundedRect(470, 152, 14)
                .fillColor(DocumentColor.rgba(7, 10, 24, 220))
                .stroke(DocumentStroke.of(DARK_LINE, 0.8));
        for (int i = 0; i < lines.size(); i++) {
            CodeLine line = lines.get(i);
            double y = 12 + i * 14.6;
            ParagraphBuilder code = new ParagraphBuilder()
                    .name("TwinCodeLine" + i)
                    .margin(DocumentInsets.zero());
            for (CodeRun run : line.runs()) {
                code.inlineText(run.text(), mono(9.5, codeColor(run.kind()), false));
            }
            card.position(code.build(), 20 + line.indent() * 18, y, LayerAlign.TOP_LEFT);
            if (!line.note().isEmpty()) {
                card.position(new ParagraphBuilder()
                        .name("TwinCodeNote" + i)
                        .text(line.note())
                        .textStyle(mono(9.5, MINT, false))
                        .margin(DocumentInsets.zero())
                        .build(), 250, y, LayerAlign.TOP_LEFT);
            }
        }
        return card.build();
    }

    private static DocumentNode layoutGraphPill() {
        return new ShapeContainerBuilder()
                .name("TwinLayoutGraph")
                .roundedRect(240, 56, 15)
                .fillColor(DARK_CARD_2)
                .stroke(DocumentStroke.of(VIOLET, 1.2))
                .position(icon("layout-light").node(24), 18, 0, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder()
                        .text("LayoutGraph")
                        .textStyle(mono(11.5, ON_DARK, false))
                        .margin(DocumentInsets.zero())
                        .build(), 54, -9, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder()
                        .text("deterministic geometry, measured once")
                        .textStyle(body(8.6, VIOLET_LIGHT))
                        .margin(DocumentInsets.zero())
                        .build(), 54, 9, LayerAlign.CENTER_LEFT)
                .build();
    }

    private static List<CanvasChild> connectors() {
        List<CanvasChild> shapes = new ArrayList<>();
        DocumentColor connector = DocumentColor.rgba(167, 139, 250, 150);
        shapes.add(at(rect(2, 38, connector), 769, 204));
        shapes.add(at(rect(204, 2, connector), 668, 242));
        shapes.add(at(rect(2, 44, connector), 668, 242));
        shapes.add(at(rect(2, 44, connector), 870, 242));
        shapes.add(at(dot(8, VIOLET), 766, 239));
        shapes.add(at(dot(6, VIOLET_LIGHT), 666, 240));
        shapes.add(at(dot(6, VIOLET_LIGHT), 868, 240));
        return shapes;
    }

    private static DocumentNode formatCard(String iconName, String fileName, String lane,
                                           DocumentColor accent) {
        return new ShapeContainerBuilder()
                .name("TwinFormat_" + fileName)
                .roundedRect(170, 92, 14)
                .fillColor(DARK_CARD)
                .stroke(DocumentStroke.of(accent.withOpacity(0.78), 1.0))
                .position(icon(iconName + "-light").node(22), 14, -22, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder()
                        .text(fileName)
                        .textStyle(mono(9.6, ON_DARK, false))
                        .margin(DocumentInsets.zero())
                        .build(), 44, -22, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder()
                        .text(lane)
                        .textStyle(body(9.0, accent))
                        .margin(DocumentInsets.zero())
                        .build(), 14, 8, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder()
                        .text("from the same session")
                        .textStyle(body(8.2, ON_DARK_MUTED))
                        .margin(DocumentInsets.zero())
                        .build(), 14, 26, LayerAlign.CENTER_LEFT)
                .build();
    }

    private static DocumentNode proofChip(String title, String subtitle) {
        return new ShapeContainerBuilder()
                .name("TwinProof_" + title)
                .roundedRect(202, 54, 13)
                .fillColor(DocumentColor.rgba(255, 255, 255, 22))
                .stroke(DocumentStroke.of(DARK_LINE, 0.9))
                .position(dot(9, MINT), 16, -8, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder()
                        .text(title)
                        .textStyle(mono(10.2, ON_DARK, false))
                        .margin(DocumentInsets.zero())
                        .build(), 34, -9, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder()
                        .text(subtitle)
                        .textStyle(body(9.5, ON_DARK_MUTED))
                        .margin(DocumentInsets.zero())
                        .build(), 34, 10, LayerAlign.CENTER_LEFT)
                .build();
    }

    // ------------------------------------------------------------------ util

    private static CanvasChild at(DocumentNode node, double x, double y) {
        return new CanvasChild(node, x, y);
    }

    private static DocumentNode canvasText(String name, String text, DocumentTextStyle style,
                                           double rightMargin) {
        return new ParagraphBuilder()
                .name(name)
                .text(text)
                .textStyle(style)
                .align(TextAlign.LEFT)
                .lineSpacing(1.18)
                .margin(new DocumentInsets(0, rightMargin, 0, 0))
                .build();
    }

    private static DocumentNode rect(double width, double height, DocumentColor fill) {
        return new ShapeBuilder().size(width, height).fillColor(fill).build();
    }

    private static DocumentNode dot(double diameter, DocumentColor fill) {
        return new EllipseBuilder().circle(diameter).fillColor(fill).build();
    }

    private static SvgIcon icon(String name) {
        try (InputStream in = Objects.requireNonNull(
                TwinOutputExample.class.getResourceAsStream("/showcase/" + name + ".svg"),
                "showcase icon missing: " + name)) {
            return SvgIcon.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("failed to load showcase icon: " + name, e);
        }
    }

    private static DocumentTextStyle display(double size, DocumentColor color) {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA_BOLD).size(size).color(color).build();
    }

    private static DocumentTextStyle body(double size, DocumentColor color) {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(size).color(color).build();
    }

    private static DocumentTextStyle mono(double size, DocumentColor color, boolean bold) {
        return DocumentTextStyle.builder()
                .fontName(bold ? FontName.COURIER_BOLD : FontName.COURIER)
                .size(size)
                .color(color)
                .build();
    }

    /**
     * Generates both artifacts from the command line.
     *
     * @param args unused
     * @throws Exception when rendering fails
     */
    public static void main(String[] args) throws Exception {
        Path pdf = generate();
        System.out.println("Generated: " + pdf);
        System.out.println("Generated: " + pdf.resolveSibling("twin-output.pptx"));
    }
}
