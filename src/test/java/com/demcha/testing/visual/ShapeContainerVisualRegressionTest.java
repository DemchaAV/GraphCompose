package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.junit.jupiter.api.Test;

class ShapeContainerVisualRegressionTest {
    private static final PdfVisualRegression VISUAL = PdfVisualRegression.standard()
            .perPixelTolerance(6)
            .mismatchedPixelBudget(0);

    private static final DocumentColor TEAL = DocumentColor.rgb(20, 60, 75);
    private static final DocumentColor GOLD = DocumentColor.rgb(196, 153, 76);
    private static final DocumentColor PAPER = DocumentColor.rgb(252, 248, 240);
    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor RULE = DocumentColor.rgb(212, 200, 178);

    @Test
    void circleWithTextMatchesVisualBaseline() throws Exception {
        VISUAL.assertMatchesBaseline("shape-container-circle-with-text", circleWithText());
    }

    @Test
    void ellipseWithOverlayMatchesVisualBaseline() throws Exception {
        VISUAL.assertMatchesBaseline("shape-container-ellipse-overlay", ellipseWithOverlay());
    }

    @Test
    void roundedRectCardMatchesVisualBaseline() throws Exception {
        VISUAL.assertMatchesBaseline("shape-container-rounded-card", roundedRectCard());
    }

    private static byte[] circleWithText() throws Exception {
        try (DocumentSession document = baseDocument(240, 180)) {
            document.pageFlow()
                    .name("VisualCircle")
                    .spacing(8)
                    .addCircle(98, TEAL, circle -> circle
                            .name("VisualInitialCircle")
                            .padding(10)
                            .stroke(DocumentStroke.of(GOLD, 1.3))
                            .center(label("GC", 24, DocumentColor.WHITE, true))
                            .position(label("CLIP", 7, GOLD, true), 0, -10, LayerAlign.BOTTOM_CENTER))
                    .addParagraph(paragraph -> paragraph
                            .text("Path-clipped circle")
                            .textStyle(style(9, INK, false))
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.zero()))
                    .build();
            return document.toPdfBytes();
        }
    }

    private static byte[] ellipseWithOverlay() throws Exception {
        try (DocumentSession document = baseDocument(300, 190)) {
            document.pageFlow()
                    .name("VisualEllipse")
                    .spacing(8)
                    .addEllipse(180, 92, GOLD, ellipse -> ellipse
                            .name("VisualOffsetEllipse")
                            .padding(10)
                            .stroke(DocumentStroke.of(TEAL, 1.0))
                            .center(label("Overlay", 16, INK, true))
                            .topRight(new ShapeBuilder()
                                    .name("VisualBadge")
                                    .size(44, 18)
                                    .fillColor(TEAL)
                                    .cornerRadius(9)
                                    .build())
                            .position(label("TOP", 7, DocumentColor.WHITE, true), -6, 5, LayerAlign.TOP_RIGHT))
                    .addParagraph(paragraph -> paragraph
                            .text("Anchor plus offset")
                            .textStyle(style(9, INK, false))
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.zero()))
                    .build();
            return document.toPdfBytes();
        }
    }

    private static byte[] roundedRectCard() throws Exception {
        try (DocumentSession document = baseDocument(320, 210)) {
            document.pageFlow()
                    .name("VisualRoundedCard")
                    .spacing(8)
                    .addContainer(card -> card
                            .name("VisualFeatureCard")
                            .roundedRect(190, 112, 16)
                            .fillColor(DocumentColor.WHITE)
                            .stroke(DocumentStroke.of(RULE, 0.8))
                            .padding(13)
                            .center(new SectionBuilder()
                                    .name("CardCopy")
                                    .spacing(5)
                                    .addParagraph(paragraph -> paragraph
                                            .text("Feature card")
                                            .textStyle(style(14, TEAL, true))
                                            .margin(DocumentInsets.zero()))
                                    .addRich(rich -> rich
                                            .plain("Children stay ")
                                            .bold("semantic")
                                            .plain(" and clipped."))
                                    .build())
                            .topRight(new ShapeBuilder()
                                    .name("CardBadge")
                                    .size(46, 18)
                                    .fillColor(TEAL)
                                    .cornerRadius(9)
                                    .build())
                            .position(label("NEW", 7, DocumentColor.WHITE, true), -6, 5, LayerAlign.TOP_RIGHT))
                    .build();
            return document.toPdfBytes();
        }
    }

    private static DocumentSession baseDocument(double width, double height) {
        return GraphCompose.document()
                .pageSize(width, height)
                .pageBackground(PAPER)
                .margin(DocumentInsets.of(18))
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
