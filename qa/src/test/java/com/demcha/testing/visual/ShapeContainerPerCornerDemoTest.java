package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.font.FontName;
import com.demcha.compose.testing.visual.ImageDiff;
import com.demcha.compose.testing.visual.PdfVisualRegression;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies per-corner {@code ShapeContainer.roundedRect(w, h, DocumentCornerRadius)}:
 * the outline fills and clips with independent corner radii, so "rounded on one
 * side, square on the other" renders directly (no CLIP_PATH parent). Writes a
 * human-review comparison PDF of the five common corner presets.
 */
class ShapeContainerPerCornerDemoTest {

    private static final DocumentColor CARD = DocumentColor.rgb(20, 60, 75);
    private static final DocumentColor PAPER = DocumentColor.rgb(252, 248, 240);
    private static final DocumentColor MUTED = DocumentColor.rgb(102, 106, 118);
    private static final double R = 26.0;
    private static final PdfVisualRegression VISUAL = PdfVisualRegression.standard();

    @Test
    void perCornerOutlineRendersDifferentlyFromUniform() throws Exception {
        BufferedImage uniform = render(DocumentCornerRadius.of(R));
        BufferedImage right = render(DocumentCornerRadius.right(R));
        BufferedImage top = render(DocumentCornerRadius.top(R));

        ImageDiff.Result vsUniform = ImageDiff.compare(uniform, right, 6);
        ImageDiff.Result vsTop = ImageDiff.compare(right, top, 6);
        assertThat(vsUniform.mismatchedPixelCount())
                .as("rounded-right must differ from uniform at the left corners (%s)", vsUniform.summary())
                .isGreaterThan(50L);
        assertThat(vsTop.mismatchedPixelCount())
                .as("rounded-right must differ from rounded-top")
                .isGreaterThan(50L);

        Path out = Path.of("target/visual-tests/shape-per-corner/card-corner-presets.pdf");
        Files.createDirectories(out.getParent());
        Files.write(out, comparisonSheet());
    }

    @Test
    void perCornerClipMasksChildAsymmetrically() throws Exception {
        // No outline fill — only the clipped child is visible, so the corners
        // shown ARE the clip shape (not the per-corner outline fill). Left- vs
        // right-rounded must mask the child differently.
        BufferedImage left = VISUAL.renderPages(clippedChild(DocumentCornerRadius.left(R))).get(0);
        BufferedImage right = VISUAL.renderPages(clippedChild(DocumentCornerRadius.right(R))).get(0);
        assertThat(ImageDiff.compare(left, right, 6).mismatchedPixelCount())
                .as("the per-corner clip must mask a child at left vs right corners")
                .isGreaterThan(50L);
    }

    @Test
    void inlinePerCornerShapeRendersWithoutError() throws Exception {
        // Regression guard for the inline dispatch: a per-corner outline reaches
        // the inline shape path via ParagraphBuilder.shape(...). The handler has
        // a final `else throw`, so dropping the per-corner branch would make this
        // render throw instead of silently drawing nothing.
        try (DocumentSession document = GraphCompose.document()
                .pageSize(120, 60)
                .pageBackground(PAPER)
                .margin(DocumentInsets.of(12))
                .create()) {
            document.pageFlow()
                    .name("InlineSheet")
                    .addParagraph(p -> p.shape(
                            new ShapeOutline.RoundedRectanglePerCorner(36, 36, DocumentCornerRadius.right(R)),
                            CARD))
                    .build();

            byte[] bytes = document.toPdfBytes();
            assertThat(bytes).isNotEmpty();
            assertThat(new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
                    .isEqualTo("%PDF-");
        }
    }

    private static byte[] clippedChild(DocumentCornerRadius corners) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(170, 130)
                .pageBackground(PAPER)
                .margin(DocumentInsets.of(16))
                .create()) {
            document.pageFlow()
                    .name("ClipSheet")
                    .addSection("Row", section -> section
                            .addContainer(c -> c
                                    .name("ClipCard")
                                    .roundedRect(130, 90, corners)
                                    .clipPolicy(ClipPolicy.CLIP_PATH)
                                    .center(new ShapeBuilder().size(130, 90).fillColor(CARD).build())))
                    .build();
            return document.toPdfBytes();
        }
    }

    private static BufferedImage render(DocumentCornerRadius corners) throws Exception {
        return VISUAL.renderPages(card(corners)).get(0);
    }

    private static byte[] card(DocumentCornerRadius corners) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(170, 130)
                .pageBackground(PAPER)
                .margin(DocumentInsets.of(16))
                .create()) {
            document.pageFlow()
                    .name("CardSheet")
                    .addSection("Row", section -> section
                            .addContainer(c -> c
                                    .name("PerCornerCard")
                                    .roundedRect(130, 90, corners)
                                    .fillColor(CARD)
                                    .clipPolicy(ClipPolicy.CLIP_PATH)
                                    .center(label("Aa"))))
                    .build();
            return document.toPdfBytes();
        }
    }

    private static byte[] comparisonSheet() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(620, 150)
                .pageBackground(PAPER)
                .margin(DocumentInsets.of(16))
                .create()) {
            document.pageFlow()
                    .name("CornerPresets")
                    .addRow("Presets", row -> row
                            .spacing(12)
                            .evenWeights()
                            .addSection("Uniform", col -> cornerColumn(col, "of(r)", DocumentCornerRadius.of(R)))
                            .addSection("Left", col -> cornerColumn(col, "left(r)", DocumentCornerRadius.left(R)))
                            .addSection("Right", col -> cornerColumn(col, "right(r)", DocumentCornerRadius.right(R)))
                            .addSection("Top", col -> cornerColumn(col, "top(r)", DocumentCornerRadius.top(R)))
                            .addSection("Bottom", col -> cornerColumn(col, "bottom(r)", DocumentCornerRadius.bottom(R))))
                    .build();
            return document.toPdfBytes();
        }
    }

    private static void cornerColumn(SectionBuilder col, String caption, DocumentCornerRadius corners) {
        col.spacing(5)
                .addContainer(c -> c
                        .name("Card" + caption)
                        .roundedRect(110, 80, corners)
                        .fillColor(CARD)
                        .clipPolicy(ClipPolicy.CLIP_PATH)
                        .center(label("Aa")))
                .addParagraph(p -> p
                        .text("DocumentCornerRadius." + caption)
                        .textStyle(captionStyle())
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()));
    }

    private static DocumentNode label(String text) {
        return new ParagraphBuilder()
                .text(text)
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA_BOLD)
                        .decoration(DocumentTextDecoration.BOLD)
                        .size(22)
                        .color(DocumentColor.WHITE)
                        .build())
                .align(TextAlign.CENTER)
                .margin(DocumentInsets.zero())
                .build();
    }

    private static DocumentTextStyle captionStyle() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(8)
                .color(MUTED)
                .build();
    }
}
