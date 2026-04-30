package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for the v1.5 Phase C transform mixin
 * ({@code rotate} / {@code scale}) and per-layer {@code zIndex}. Three
 * sections demonstrate each feature in turn against the
 * {@link BusinessTheme} so the output stays visually coherent with the
 * rest of the example gallery.
 *
 * @author Artem Demchyshyn
 */
public final class TransformsExample {
    private static final BusinessTheme THEME = BusinessTheme.modern();
    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(102, 106, 118);
    private static final DocumentColor DEEP_TEAL = DocumentColor.rgb(20, 60, 75);
    private static final DocumentColor GOLD = DocumentColor.rgb(196, 153, 76);
    private static final DocumentColor RED = DocumentColor.rgb(180, 40, 40);

    private TransformsExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("transforms.pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(THEME.pageBackground())
                .margin(28, 28, 28, 28)
                .create()) {

            document.pageFlow()
                    .name("TransformsShowcase")
                    .spacing(16)
                    .addSection("Hero", section -> section
                            .softPanel(THEME.palette().surfaceMuted(), 10, 14)
                            .accentLeft(GOLD, 4)
                            .spacing(6)
                            .addParagraph(p -> p
                                    .text("Transforms and z-index")
                                    .textStyle(THEME.text().h1())
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("Rotate, scale, and explicitly stack layers — composed naturally on top ")
                                    .accent("of the existing shape-container DSL", GOLD)
                                    .plain(".")))
                    .addSection("Rotate", section -> section
                            .spacing(8)
                            .addParagraph(p -> p
                                    .text("rotate(15)")
                                    .textStyle(THEME.text().h2())
                                    .margin(DocumentInsets.zero()))
                            .addRow("RotateRow", row -> row
                                    .spacing(18)
                                    .weights(1, 1, 1)
                                    .addSection("RotateSlot1", col -> col
                                            .addCircle(110, DEEP_TEAL, circle -> circle
                                                    .name("RotatedSeal")
                                                    .padding(10)
                                                    .stroke(DocumentStroke.of(GOLD, 1.4))
                                                    .rotate(15)
                                                    .center(label("M&A", style(FontName.HELVETICA_BOLD, 22,
                                                            DocumentTextDecoration.BOLD, DocumentColor.WHITE)))))
                                    .addSection("RotateSlot2", col -> col
                                            .addCircle(110, DEEP_TEAL, circle -> circle
                                                    .name("RotatedReverse")
                                                    .padding(10)
                                                    .stroke(DocumentStroke.of(GOLD, 1.4))
                                                    .rotate(-15)
                                                    .center(label("M&A", style(FontName.HELVETICA_BOLD, 22,
                                                            DocumentTextDecoration.BOLD, DocumentColor.WHITE)))))
                                    .addSection("RotateSlot3", col -> col
                                            .addCircle(110, DEEP_TEAL, circle -> circle
                                                    .name("PlainSeal")
                                                    .padding(10)
                                                    .stroke(DocumentStroke.of(GOLD, 1.4))
                                                    .center(label("M&A", style(FontName.HELVETICA_BOLD, 22,
                                                            DocumentTextDecoration.BOLD, DocumentColor.WHITE))))))
                            .addParagraph(p -> p
                                    .text("Tilt left, tilt right, no tilt — outline plus label rotate together.")
                                    .textStyle(caption())
                                    .align(TextAlign.CENTER)
                                    .margin(DocumentInsets.zero())))
                    .addSection("Scale", section -> section
                            .spacing(8)
                            .addParagraph(p -> p
                                    .text("scale(...)")
                                    .textStyle(THEME.text().h2())
                                    .margin(DocumentInsets.zero()))
                            .addRow("ScaleRow", row -> row
                                    .spacing(18)
                                    .weights(1, 1, 1)
                                    .addSection("ScaleSlot1", col -> col
                                            .addContainer(card -> card
                                                    .name("ShrunkCard")
                                                    .roundedRect(180, 100, 12)
                                                    .fillColor(DocumentColor.WHITE)
                                                    .stroke(DocumentStroke.of(DEEP_TEAL, 0.8))
                                                    .padding(12)
                                                    .scale(0.7)
                                                    .center(label("scale(0.7)", style(FontName.HELVETICA_BOLD, 14,
                                                            DocumentTextDecoration.BOLD, INK)))))
                                    .addSection("ScaleSlot2", col -> col
                                            .addContainer(card -> card
                                                    .name("WideCard")
                                                    .roundedRect(180, 100, 12)
                                                    .fillColor(DocumentColor.WHITE)
                                                    .stroke(DocumentStroke.of(DEEP_TEAL, 0.8))
                                                    .padding(12)
                                                    .scale(1.1, 0.85)
                                                    .center(label("1.1 x 0.85", style(FontName.HELVETICA_BOLD, 14,
                                                            DocumentTextDecoration.BOLD, INK)))))
                                    .addSection("ScaleSlot3", col -> col
                                            .addContainer(card -> card
                                                    .name("PlainCard")
                                                    .roundedRect(180, 100, 12)
                                                    .fillColor(DocumentColor.WHITE)
                                                    .stroke(DocumentStroke.of(DEEP_TEAL, 0.8))
                                                    .padding(12)
                                                    .center(label("1.0", style(FontName.HELVETICA_BOLD, 14,
                                                            DocumentTextDecoration.BOLD, INK))))))
                            .addParagraph(p -> p
                                    .text("Scaling pivots around the outline centre and composes with rotate().")
                                    .textStyle(caption())
                                    .align(TextAlign.CENTER)
                                    .margin(DocumentInsets.zero())))
                    .addSection("ZIndex", section -> section
                            .spacing(8)
                            .addParagraph(p -> p
                                    .text("zIndex — re-stack layers without re-ordering source")
                                    .textStyle(THEME.text().h2())
                                    .margin(DocumentInsets.zero()))
                            .addContainer(stage -> stage
                                    .name("ZSwapStage")
                                    .roundedRect(420, 160, 12)
                                    .fillColor(DocumentColor.WHITE)
                                    .stroke(DocumentStroke.of(MUTED, 0.6))
                                    .padding(12)
                                    // Source order: RED first with zIndex=10, TEAL second.
                                    // RED still draws on top despite earlier source position.
                                    .position(filledSquare("RedSquare", 110, RED),
                                            -30, 0, LayerAlign.CENTER, 10)
                                    .position(filledSquare("TealSquare", 110, DEEP_TEAL),
                                            30, 0, LayerAlign.CENTER))
                            .addParagraph(p -> p
                                    .text("RED.zIndex(10) draws on top of TEAL even though RED is declared first.")
                                    .textStyle(caption())
                                    .align(TextAlign.CENTER)
                                    .margin(DocumentInsets.zero())))
                    .build();
        }

        return outputFile;
    }

    private static DocumentNode label(String text, DocumentTextStyle textStyle) {
        return new ParagraphBuilder()
                .text(text)
                .textStyle(textStyle)
                .align(TextAlign.CENTER)
                .margin(DocumentInsets.zero())
                .build();
    }

    private static DocumentNode filledSquare(String name, double size, DocumentColor color) {
        return new ShapeBuilder()
                .name(name)
                .size(size, size)
                .fillColor(color)
                .cornerRadius(6)
                .build();
    }

    private static DocumentTextStyle style(FontName font,
                                           double size,
                                           DocumentTextDecoration decoration,
                                           DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(font)
                .decoration(decoration)
                .size(size)
                .color(color)
                .build();
    }

    private static DocumentTextStyle caption() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(9)
                .color(MUTED)
                .build();
    }
}
