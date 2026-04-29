package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
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
 * Runnable showcase for the v1.5 shape-as-container DSL.
 */
public final class ShapeContainerExample {
    private static final BusinessTheme THEME = BusinessTheme.modern();
    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(102, 106, 118);
    private static final DocumentColor DEEP_TEAL = DocumentColor.rgb(20, 60, 75);
    private static final DocumentColor GOLD = DocumentColor.rgb(196, 153, 76);
    private static final DocumentColor SOFT_GOLD = DocumentColor.rgb(238, 221, 184);

    private ShapeContainerExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("shape-container.pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(THEME.pageBackground())
                .margin(28, 28, 28, 28)
                .create()) {

            document.pageFlow()
                    .name("ShapeContainerShowcase")
                    .spacing(14)
                    .addSection("Hero", section -> section
                            .softPanel(THEME.palette().surfaceMuted(), 10, 14)
                            .accentLeft(GOLD, 4)
                            .spacing(7)
                            .addParagraph(paragraph -> paragraph
                                    .text("Shape-as-container")
                                    .textStyle(THEME.text().h1())
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("Compose a shape, place normal document nodes inside it, ")
                                    .accent("and clip the children to the outline", GOLD)
                                    .plain(" while the whole unit paginates atomically.")))
                    .addRow("ShapeSamples", row -> row
                            .spacing(18)
                            .weights(1, 1, 1)
                            .addSection("CircleColumn", section -> section
                                    .spacing(8)
                                    .addCircle(118, DEEP_TEAL, circle -> circle
                                            .name("InitialCircle")
                                            .padding(12)
                                            .stroke(DocumentStroke.of(GOLD, 1.4))
                                            .center(label("GC", style(FontName.HELVETICA_BOLD, 24,
                                                    DocumentTextDecoration.BOLD, DocumentColor.WHITE)))
                                            .position(label("CLIP", style(FontName.HELVETICA_BOLD, 8,
                                                            DocumentTextDecoration.BOLD, SOFT_GOLD)),
                                                    0, -12, LayerAlign.BOTTOM_CENTER))
                                    .addParagraph(paragraph -> paragraph
                                            .text("A circle owns its label and clips by the ellipse path.")
                                            .textStyle(caption())
                                            .lineSpacing(1.4)
                                            .margin(DocumentInsets.zero())))
                            .addSection("EllipseColumn", section -> section
                                    .spacing(8)
                                    .addEllipse(168, 92, GOLD, ellipse -> ellipse
                                            .name("OffsetEllipse")
                                            .padding(10)
                                            .stroke(DocumentStroke.of(DEEP_TEAL, 1.0))
                                            .center(label("Overlay", style(FontName.HELVETICA_BOLD, 15,
                                                    DocumentTextDecoration.BOLD, INK)))
                                            .topRight(pill(44, 18, DEEP_TEAL))
                                            .position(label("TOP", style(FontName.HELVETICA_BOLD, 7,
                                                            DocumentTextDecoration.BOLD, DocumentColor.WHITE)),
                                                    -6, 5, LayerAlign.TOP_RIGHT))
                                    .addParagraph(paragraph -> paragraph
                                            .text("Nine-point anchors and offsets use the same vocabulary as LayerStack.")
                                            .textStyle(caption())
                                            .lineSpacing(1.4)
                                            .margin(DocumentInsets.zero())))
                            .addSection("CardColumn", section -> section
                                    .spacing(8)
                                    .addContainer(card -> card
                                            .name("RoundedFeatureCard")
                                            .roundedRect(178, 112, 16)
                                            .fillColor(DocumentColor.WHITE)
                                            .stroke(DocumentStroke.of(THEME.palette().rule(), 0.8))
                                            .clipPolicy(ClipPolicy.CLIP_PATH)
                                            .padding(12)
                                            .center(cardCopy())
                                            .topRight(pill(46, 18, DEEP_TEAL))
                                            .position(label("NEW", style(FontName.HELVETICA_BOLD, 7,
                                                            DocumentTextDecoration.BOLD, DocumentColor.WHITE)),
                                                    -6, 5, LayerAlign.TOP_RIGHT))
                                    .addParagraph(paragraph -> paragraph
                                            .text("A rounded rectangle can host sections, rich text, and badge layers.")
                                            .textStyle(caption())
                                            .lineSpacing(1.4)
                                            .margin(DocumentInsets.zero()))))
                    .addSection("ClipPolicies", section -> section
                            .softPanel(DocumentColor.WHITE, 8, 12)
                            .stroke(DocumentStroke.of(THEME.palette().rule(), 0.5))
                            .spacing(5)
                            .addParagraph(paragraph -> paragraph
                                    .text("Clip policies")
                                    .textStyle(THEME.text().h3())
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("Default ")
                                    .bold("CLIP_PATH")
                                    .plain(" clips to the actual outline. ")
                                    .bold("CLIP_BOUNDS")
                                    .plain(" clips to the bounding box, and ")
                                    .bold("OVERFLOW_VISIBLE")
                                    .plain(" keeps child overflow visible for deliberate effects.")))
                    .build();

            document.buildPdf();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }

    private static DocumentNode label(String text, DocumentTextStyle style) {
        return new ParagraphBuilder()
                .text(text)
                .textStyle(style)
                .align(TextAlign.CENTER)
                .margin(DocumentInsets.zero())
                .build();
    }

    private static DocumentNode pill(double width, double height, DocumentColor color) {
        return new ShapeBuilder()
                .size(width, height)
                .fillColor(color)
                .cornerRadius(height / 2.0)
                .build();
    }

    private static DocumentNode cardCopy() {
        return new SectionBuilder()
                .name("RoundedCardCopy")
                .spacing(5)
                .addParagraph(paragraph -> paragraph
                        .text("Feature card")
                        .textStyle(THEME.text().h3())
                        .margin(DocumentInsets.zero()))
                .addRich(rich -> rich
                        .plain("Children are still ")
                        .bold("semantic nodes")
                        .plain(": paragraphs, sections, badges, and rich text."))
                .build();
    }

    private static DocumentTextStyle caption() {
        return style(FontName.HELVETICA, 8.6, DocumentTextDecoration.DEFAULT, MUTED);
    }

    private static DocumentTextStyle style(FontName font,
                                           double size,
                                           DocumentTextDecoration decoration,
                                           DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(font)
                .size(size)
                .decoration(decoration)
                .color(color)
                .build();
    }
}
