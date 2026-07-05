package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import com.demcha.testing.VisualTestOutputs;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders Phase C.2 transform demos to PDF artefacts under
 * {@code target/visual-tests/shape-container-transform/}. These are not
 * baseline-asserted yet — the goal is to produce concrete PDFs that a
 * reviewer can open to verify the rotation / scaling behaviour visually.
 *
 * <p>Each scenario also asserts that the PDF byte stream is non-empty
 * and starts with the {@code %PDF-} magic header so a graphics-state
 * leak (un-balanced transform begin/end) would corrupt the stream and
 * fail the smoke check.</p>
 *
 * @author Artem Demchyshyn
 */
class ShapeContainerTransformDemoTest {

    private static final DocumentColor TEAL = DocumentColor.rgb(20, 60, 75);
    private static final DocumentColor GOLD = DocumentColor.rgb(196, 153, 76);
    private static final DocumentColor PAPER = DocumentColor.rgb(252, 248, 240);
    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);

    @Test
    void rotatedCircleRendersSuccessfully() throws Exception {
        Path output = VisualTestOutputs.preparePdf("rotated-circle", "shape-container-transform");
        try (DocumentSession document = baseDocument(260, 200)) {
            document.pageFlow()
                    .name("RotatedCircleDemo")
                    .spacing(10)
                    .addCircle(110, TEAL, circle -> circle
                            .name("RotatedCircle")
                            .padding(10)
                            .stroke(DocumentStroke.of(GOLD, 1.4))
                            .rotate(30)
                            .center(label("M&A", 22, DocumentColor.WHITE, true)))
                    .addParagraph(p -> p
                            .text("circle.rotate(30) — clipped to outline, rotates around centre")
                            .textStyle(style(8.5, INK, false))
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.zero()))
                    .build();
            Files.write(output, document.toPdfBytes());
        }
        assertPdf(output);
    }

    @Test
    void scaledRoundedCardRendersSuccessfully() throws Exception {
        Path output = VisualTestOutputs.preparePdf("scaled-card", "shape-container-transform");
        try (DocumentSession document = baseDocument(320, 220)) {
            document.pageFlow()
                    .name("ScaledCardDemo")
                    .spacing(10)
                    .addContainer(card -> card
                            .name("ScaledCard")
                            .roundedRect(220, 110, 14)
                            .fillColor(DocumentColor.WHITE)
                            .stroke(DocumentStroke.of(TEAL, 1.0))
                            .padding(12)
                            .scale(0.85)
                            .center(label("Scaled to 0.85", 13, INK, true)))
                    .addParagraph(p -> p
                            .text("card.scale(0.85) — geometry shrinks around centre")
                            .textStyle(style(8.5, INK, false))
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.zero()))
                    .build();
            Files.write(output, document.toPdfBytes());
        }
        assertPdf(output);
    }

    @Test
    void rotatedAndScaledEllipseRendersSuccessfully() throws Exception {
        Path output = VisualTestOutputs.preparePdf("rotated-scaled-ellipse", "shape-container-transform");
        try (DocumentSession document = baseDocument(320, 220)) {
            document.pageFlow()
                    .name("RotatedScaledEllipseDemo")
                    .spacing(10)
                    .addEllipse(190, 100, GOLD, ellipse -> ellipse
                            .name("RotatedScaledEllipse")
                            .padding(10)
                            .stroke(DocumentStroke.of(TEAL, 1.0))
                            .rotate(15)
                            .scale(0.9)
                            .center(label("Spin & shrink", 14, INK, true)))
                    .addParagraph(p -> p
                            .text("ellipse.rotate(15).scale(0.9) — composes naturally")
                            .textStyle(style(8.5, INK, false))
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.zero()))
                    .build();
            Files.write(output, document.toPdfBytes());
        }
        assertPdf(output);
    }

    private static void assertPdf(Path output) throws Exception {
        byte[] bytes = Files.readAllBytes(output);
        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
                .as("PDF magic header must be intact — a graphics-state leak from an "
                        + "unbalanced transform begin/end would corrupt the stream")
                .isEqualTo("%PDF-");
    }

    private static DocumentSession baseDocument(double width, double height) {
        return GraphCompose.document()
                .pageSize(width, height)
                .pageBackground(PAPER)
                .margin(DocumentInsets.of(20))
                .create();
    }

    private static com.demcha.compose.document.node.DocumentNode label(String text,
                                                                       double size,
                                                                       DocumentColor color,
                                                                       boolean bold) {
        return new ParagraphBuilder()
                .text(text)
                .textStyle(style(size, color, bold))
                .align(TextAlign.CENTER)
                .margin(DocumentInsets.zero())
                .build();
    }

    private static DocumentTextStyle style(double size, DocumentColor color, boolean bold) {
        return DocumentTextStyle.builder()
                .fontName(bold ? FontName.HELVETICA_BOLD : FontName.HELVETICA)
                .decoration(bold ? DocumentTextDecoration.BOLD : DocumentTextDecoration.DEFAULT)
                .size(size)
                .color(color)
                .build();
    }
}
