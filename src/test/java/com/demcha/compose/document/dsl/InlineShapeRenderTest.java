package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.ShapeOutline;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end coverage for inline shape runs: the measure → tokenize → span →
 * PDF render pipeline must paint geometric figures (dots, diamonds, stars, …)
 * without dropping them or substituting glyphs.
 */
class InlineShapeRenderTest {

    private static final DocumentColor ACCENT = DocumentColor.of(new java.awt.Color(40, 90, 180));

    @Test
    void ratingShapesRenderEndToEndKeepingTextWithoutGlyphSubstitution() throws Exception {
        byte[] pdf = renderRatingRow();
        assertThat(pdf).isNotEmpty();

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Java");
            assertThat(text).doesNotContain("?");
        }
    }

    @Test
    void inlineShapesActuallyPaintTheirFillColor() throws Exception {
        try (PDDocument document = Loader.loadPDF(renderRatingRow())) {
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 96);
            // The accent fill only enters the page through the inline figures —
            // the text is default black and the background white — so finding
            // accent pixels proves the figures were drawn, not silently dropped.
            assertThat(containsColorNear(image, 40, 90, 180, 45))
                    .as("inline shapes must paint their accent fill")
                    .isTrue();
        }
    }

    @Test
    void linkedInlineShapeEmitsClickableAnnotation() throws Exception {
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(220, 120)
                .margin(14, 14, 14, 14)
                .create()) {
            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addParagraph(paragraph -> paragraph
                            .inlineText("Home ")
                            .shape(ShapeOutline.diamond(8, 8), ACCENT, null,
                                    InlineImageAlignment.CENTER, 0.0,
                                    new DocumentLinkOptions("https://example.com")))
                    .build();
            pdf = session.toPdfBytes();
        }

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getPage(0).getAnnotations())
                    .anyMatch(annotation -> annotation instanceof PDAnnotationLink);
        }
    }

    @Test
    void everyOutlineKindRendersWithoutThrowing() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(280, 160)
                .margin(14, 14, 14, 14)
                .create()) {
            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addParagraph(paragraph -> paragraph
                            .inlineText("Shapes ")
                            .shape(new ShapeOutline.Rectangle(8, 8), ACCENT)
                            .shape(new ShapeOutline.RoundedRectangle(8, 8, 2), ACCENT)
                            .shape(new ShapeOutline.Ellipse(8, 8), ACCENT, null,
                                    InlineImageAlignment.TEXT_TOP, 0.0, null)
                            .diamond(8, ACCENT)
                            .triangle(8, ACCENT)
                            .star(8, ACCENT)
                            .arrow(8, ShapeOutline.Direction.RIGHT, ACCENT)
                            .chevron(8, ShapeOutline.Direction.LEFT, ACCENT)
                            .shape(ShapeOutline.checkmark(8, 8), ACCENT)
                            .shape(ShapeOutline.plus(8, 8), ACCENT)
                            .shape(ShapeOutline.regularPolygon(8, 8, 6), ACCENT))
                    .build();
            assertThat(session.toPdfBytes()).isNotEmpty();
        }
    }

    private static byte[] renderRatingRow() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 160)
                .margin(16, 16, 16, 16)
                .create()) {
            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addParagraph(paragraph -> paragraph
                            .name("SkillRating")
                            .inlineText("Java ")
                            .dot(7, ACCENT)
                            .dot(7, ACCENT)
                            .dot(7, ACCENT)
                            .dot(7, null, DocumentStroke.of(ACCENT, 0.6))
                            .inlineText("   ")
                            .diamond(8, ACCENT)
                            .star(9, ACCENT))
                    .build();
            return session.toPdfBytes();
        }
    }

    private static boolean containsColorNear(BufferedImage image, int r, int g, int b, int tolerance) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int rr = (rgb >> 16) & 0xFF;
                int gg = (rgb >> 8) & 0xFF;
                int bb = rgb & 0xFF;
                if (Math.abs(rr - r) <= tolerance
                        && Math.abs(gg - g) <= tolerance
                        && Math.abs(bb - b) <= tolerance) {
                    return true;
                }
            }
        }
        return false;
    }
}
