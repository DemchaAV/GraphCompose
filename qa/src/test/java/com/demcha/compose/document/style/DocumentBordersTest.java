package com.demcha.compose.document.style;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentBordersTest {
    private static final double COLOR_TOLERANCE = 0.01;

    @Test
    void perSideBordersRenderEachConfiguredSideStroke() throws Exception {
        byte[] pdfBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 200)
                .margin(DocumentInsets.of(10))
                .create()) {

            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addSection("Card", section -> section
                            .borders(DocumentBorders.bottom(DocumentStroke.of(DocumentColor.ROYAL_BLUE, 2.0)))
                            .padding(DocumentInsets.of(8))
                            .addParagraph("Bottom-only border", DocumentTextStyle.DEFAULT))
                    .build();

            pdfBytes = session.toPdfBytes();
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(strokeColors(document))
                    .anyMatch(rgb -> matches(rgb, DocumentColor.ROYAL_BLUE.color()));
            assertThat(hasOperator(document, "l"))
                    .as("Per-side bottom border must produce a line stroke operator")
                    .isTrue();
        }
    }

    @Test
    void documentBordersHelpersBuildExpectedPerSideStrokes() {
        DocumentStroke stroke = DocumentStroke.of(DocumentColor.BLACK, 1);
        DocumentBorders only = DocumentBorders.bottom(stroke);
        assertThat(only.bottom()).isEqualTo(stroke);
        assertThat(only.top()).isNull();
        assertThat(only.left()).isNull();
        assertThat(only.right()).isNull();
        assertThat(only.hasAny()).isTrue();

        DocumentBorders all = DocumentBorders.all(stroke);
        assertThat(all.top()).isEqualTo(stroke);
        assertThat(all.right()).isEqualTo(stroke);
        assertThat(all.bottom()).isEqualTo(stroke);
        assertThat(all.left()).isEqualTo(stroke);

        DocumentBorders horizontal = DocumentBorders.horizontal(stroke);
        assertThat(horizontal.top()).isEqualTo(stroke);
        assertThat(horizontal.bottom()).isEqualTo(stroke);
        assertThat(horizontal.left()).isNull();
        assertThat(horizontal.right()).isNull();

        assertThat(DocumentBorders.NONE.hasAny()).isFalse();
    }

    private static List<Color> strokeColors(PDDocument document) throws IOException {
        java.util.List<Color> colors = new java.util.ArrayList<>();
        for (var page : document.getPages()) {
            List<Object> tokens = new PDFStreamParser(page).parse();
            for (int index = 3; index < tokens.size(); index++) {
                Object token = tokens.get(index);
                if (token instanceof Operator op && ("RG".equals(op.getName()) || "SC".equals(op.getName()))) {
                    if (tokens.get(index - 3) instanceof COSNumber red
                            && tokens.get(index - 2) instanceof COSNumber green
                            && tokens.get(index - 1) instanceof COSNumber blue) {
                        colors.add(new Color(
                                clampInt((int) Math.round(red.floatValue() * 255)),
                                clampInt((int) Math.round(green.floatValue() * 255)),
                                clampInt((int) Math.round(blue.floatValue() * 255))));
                    }
                }
            }
        }
        return List.copyOf(colors);
    }

    private static boolean hasOperator(PDDocument document, String operatorName) throws IOException {
        for (var page : document.getPages()) {
            List<Object> tokens = new PDFStreamParser(page).parse();
            for (Object token : tokens) {
                if (token instanceof Operator operator && operatorName.equals(operator.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matches(Color actual, Color expected) {
        return Math.abs(actual.getRed() - expected.getRed()) <= 4
                && Math.abs(actual.getGreen() - expected.getGreen()) <= 4
                && Math.abs(actual.getBlue() - expected.getBlue()) <= 4;
    }

    private static int clampInt(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
