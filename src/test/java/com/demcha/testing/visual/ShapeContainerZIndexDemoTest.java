package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.node.LayerAlign;
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
 * Renders Phase C.3 z-index demos to PDF artefacts under
 * {@code target/visual-tests/shape-container-zindex/}. These document
 * the new z-order behaviour visually: a layer declared earlier in
 * source code can still render on top when its {@code zIndex} is
 * higher.
 *
 * @author Artem Demchyshyn
 */
class ShapeContainerZIndexDemoTest {

    private static final DocumentColor TEAL = DocumentColor.rgb(20, 60, 75);
    private static final DocumentColor GOLD = DocumentColor.rgb(196, 153, 76);
    private static final DocumentColor PAPER = DocumentColor.rgb(252, 248, 240);
    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor RED = DocumentColor.rgb(180, 40, 40);

    @Test
    void higherZIndexSquareRendersOnTopOfLowerZIndexSquare() throws Exception {
        // Two intersecting squares declared in source order RED then
        // TEAL, but RED carries zIndex=10 so it draws on top.
        Path output = VisualTestOutputs.preparePdf("z-swap-overlap", "shape-container-zindex");
        try (DocumentSession document = baseDocument(280, 220)) {
            document.pageFlow()
                    .name("ZSwapDemo")
                    .spacing(8)
                    .addContainer(card -> card
                            .name("ZSwapStage")
                            .roundedRect(220, 130, 10)
                            .fillColor(DocumentColor.WHITE)
                            .stroke(DocumentStroke.of(INK, 0.6))
                            .padding(8)
                            // Source order: RED first, TEAL second.
                            // With zIndex 10 vs 0, RED draws AFTER TEAL.
                            .position(new ShapeBuilder()
                                            .name("RedSquare")
                                            .size(80, 80)
                                            .fillColor(RED)
                                            .cornerRadius(4)
                                            .build(),
                                    -18, 0, LayerAlign.CENTER, 10)
                            .position(new ShapeBuilder()
                                            .name("TealSquare")
                                            .size(80, 80)
                                            .fillColor(TEAL)
                                            .cornerRadius(4)
                                            .build(),
                                    18, 0, LayerAlign.CENTER))
                    .addParagraph(p -> p
                            .text("RED.zIndex(10) draws on top of TEAL despite earlier declaration")
                            .textStyle(style(8.5, INK, false))
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.zero()))
                    .build();
            Files.write(output, document.toPdfBytes());
        }
        assertPdf(output);
    }

    @Test
    void badgeOverlayLandsOnTopByZIndex() throws Exception {
        // Realistic case: a feature card with a "NEW" badge that needs
        // to render after the photo backdrop even when authors compose
        // the card top-down.
        Path output = VisualTestOutputs.preparePdf("badge-overlay", "shape-container-zindex");
        try (DocumentSession document = baseDocument(320, 230)) {
            document.pageFlow()
                    .name("BadgeOverlayDemo")
                    .spacing(8)
                    .addContainer(card -> card
                            .name("FeatureCard")
                            .roundedRect(240, 140, 12)
                            .fillColor(DocumentColor.WHITE)
                            .stroke(DocumentStroke.of(INK, 0.6))
                            .padding(10)
                            // Backdrop teal box first; badge declared
                            // BELOW it but with zIndex=5 to land on top.
                            .center(new ShapeBuilder()
                                    .name("CardBackdrop")
                                    .size(180, 80)
                                    .fillColor(TEAL)
                                    .cornerRadius(6)
                                    .build())
                            .position(label("NEW", 9, DocumentColor.WHITE, true),
                                    -20, 8, LayerAlign.TOP_RIGHT, 5)
                            .position(new ShapeBuilder()
                                            .name("BadgeBackground")
                                            .size(40, 18)
                                            .fillColor(GOLD)
                                            .cornerRadius(9)
                                            .build(),
                                    -28, 4, LayerAlign.TOP_RIGHT, 4))
                    .addParagraph(p -> p
                            .text("Badge background (zIndex=4) + label (zIndex=5) both above the teal backdrop")
                            .textStyle(style(8, INK, false))
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
