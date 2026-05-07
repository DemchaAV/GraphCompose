package com.demcha.examples.features.canvas;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for the v1.6 Phase C
 * {@link com.demcha.compose.document.node.CanvasLayerNode} —
 * places child nodes at explicit {@code (x, y)} coordinates
 * inside a fixed-size bounding box. The generated PDF is a
 * "Certificate of Achievement" with hand-positioned title,
 * subtitle, recipient name, citation, decorative thin-rule
 * shapes, and a seal block.
 *
 * <p>Each piece of the certificate is positioned via
 * {@code canvas.position(child, x, y)} where {@code (0, 0)} is
 * the canvas's top-left corner and positive {@code y} extends
 * downward. No flow / row / stack-alignment is involved —
 * coordinates are explicit.</p>
 *
 * @author Artem Demchyshyn
 */
public final class CanvasLayerExample {

    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(102, 106, 118);
    private static final DocumentColor ACCENT = DocumentColor.rgb(170, 110, 30);
    private static final DocumentColor SEAL_FILL = DocumentColor.rgb(245, 232, 200);
    private static final DocumentColor RULE = DocumentColor.rgb(180, 168, 140);

    private CanvasLayerExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("features/canvas", "canvas-layer-showcase.pdf");

        DocumentTextStyle eyebrow = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(10)
                .color(ACCENT)
                .decoration(DocumentTextDecoration.BOLD)
                .build();
        DocumentTextStyle headline = DocumentTextStyle.builder()
                .fontName(FontName.TIMES_BOLD)
                .size(34)
                .color(INK)
                .decoration(DocumentTextDecoration.BOLD)
                .build();
        DocumentTextStyle awarded = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(11)
                .color(MUTED)
                .build();
        DocumentTextStyle name = DocumentTextStyle.builder()
                .fontName(FontName.TIMES_ITALIC)
                .size(24)
                .color(INK)
                .build();
        DocumentTextStyle bodyText = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(11)
                .color(INK)
                .build();
        DocumentTextStyle sealLabel = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(9)
                .color(ACCENT)
                .decoration(DocumentTextDecoration.BOLD)
                .build();
        DocumentTextStyle signatureLabel = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(9)
                .color(MUTED)
                .build();
        DocumentTextStyle title = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(15)
                .color(INK)
                .decoration(DocumentTextDecoration.BOLD)
                .build();
        DocumentTextStyle caption = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_OBLIQUE)
                .size(10)
                .color(MUTED)
                .build();

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .margin(36, 36, 36, 36)
                .create()) {

            document.pageFlow()
                    .name("CanvasShowcase")
                    .spacing(8)
                    .addParagraph("v1.6 Phase C — CanvasLayerNode (controlled free-canvas)", title)
                    .addParagraph(
                            "Every element below is placed at an explicit (x, y) inside the canvas's "
                                    + "523 x 360 bounding box. Coordinates use the screen convention: "
                                    + "(0, 0) is the canvas's top-left, positive x extends right, "
                                    + "positive y extends down.", caption)
                    .addCanvas(523, 360, canvas -> canvas
                            .name("Certificate")
                            .clipPolicy(ClipPolicy.CLIP_BOUNDS)
                            // Two thin decorative rules (top + bottom).
                            .position(rule(503, 1.2, ACCENT), 10, 30)
                            .position(rule(503, 0.4, RULE), 10, 36)
                            .position(rule(503, 1.2, ACCENT), 10, 320)
                            .position(rule(503, 0.4, RULE), 10, 326)
                            // Eyebrow + headline + awarded text + name.
                            .position(new ParagraphNode(
                                    "Eyebrow", "CERTIFICATE OF ACHIEVEMENT",
                                    eyebrow, TextAlign.CENTER, 0.0,
                                    DocumentInsets.zero(), DocumentInsets.zero()),
                                    100, 60)
                            .position(new ParagraphNode(
                                    "Headline", "GraphCompose v1.6",
                                    headline, TextAlign.CENTER, 0.0,
                                    DocumentInsets.zero(), DocumentInsets.zero()),
                                    100, 90)
                            .position(new ParagraphNode(
                                    "Awarded", "Awarded to",
                                    awarded, TextAlign.CENTER, 0.0,
                                    DocumentInsets.zero(), DocumentInsets.zero()),
                                    100, 145)
                            .position(new ParagraphNode(
                                    "Recipient", "Artem Demchyshyn",
                                    name, TextAlign.CENTER, 0.0,
                                    DocumentInsets.zero(), DocumentInsets.zero()),
                                    100, 165)
                            .position(new ParagraphNode(
                                    "Citation",
                                    "for shipping the v1.6 expressive release with Templates v2, "
                                            + "nested lists, composed table cells, and pixel-precise "
                                            + "free-canvas layout in a single iteration.",
                                    bodyText, TextAlign.CENTER, 1.5,
                                    DocumentInsets.zero(), DocumentInsets.zero()),
                                    80, 205)
                            // Decorative seal block (right side, mid-height).
                            .position(new ShapeNode(
                                    "SealBlock", 80, 50,
                                    SEAL_FILL,
                                    DocumentStroke.of(ACCENT, 1.0),
                                    null, null,
                                    DocumentInsets.zero(), DocumentInsets.zero()),
                                    410, 250)
                            .position(new ParagraphNode(
                                    "SealLabel", "OFFICIAL",
                                    sealLabel, TextAlign.CENTER, 0.0,
                                    DocumentInsets.zero(), DocumentInsets.zero()),
                                    415, 270)
                            // Two signature lines at the bottom-left + labels.
                            .position(rule(150, 0.5, INK), 40, 290)
                            .position(new ParagraphNode(
                                    "SignatureLeftLabel", "Lead Engineer",
                                    signatureLabel, TextAlign.LEFT, 0.0,
                                    DocumentInsets.zero(), DocumentInsets.zero()),
                                    40, 295)
                            .position(rule(150, 0.5, INK), 220, 290)
                            .position(new ParagraphNode(
                                    "SignatureRightLabel", "Project Owner",
                                    signatureLabel, TextAlign.LEFT, 0.0,
                                    DocumentInsets.zero(), DocumentInsets.zero()),
                                    220, 295))
                    .addParagraph(
                            "Notes: the canvas reserves a fixed 523x360 rectangle in the surrounding flow "
                                    + "regardless of where children land. Children whose (x, y) falls inside "
                                    + "the bounding box render in source order; clip policy CLIP_BOUNDS hides "
                                    + "anything that overflows.", caption)
                    .build();

            document.buildPdf();
        }
        return outputFile;
    }

    /**
     * Convenience: a thin filled rectangle used as a horizontal rule
     * inside the canvas. Built from {@link ShapeNode} so the example
     * stays on plain canonical primitives.
     */
    private static ShapeNode rule(double width, double thickness, DocumentColor color) {
        return new ShapeNode(
                "Rule",
                width,
                thickness,
                color,
                null,
                null, null,
                DocumentInsets.zero(),
                DocumentInsets.zero());
    }

    public static void main(String[] args) throws Exception {
        Path output = generate();
        System.out.println("Generated: " + output);
    }
}
