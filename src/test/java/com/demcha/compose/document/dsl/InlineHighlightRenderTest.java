package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * End-to-end coverage for the inline highlight "chip": the layout reserves the
 * chip's horizontal padding, and the PDF render paints a rounded fill behind the
 * glyphs without dropping the text.
 */
class InlineHighlightRenderTest {

    private static final DocumentInsets PAD = DocumentInsets.symmetric(1.0, 4.0);
    private static final DocumentTextStyle MONO =
            DocumentTextStyle.builder().fontName(FontName.COURIER).size(11).build();
    /** Vivid yellow — distinct from black text on a white page. */
    private static final DocumentColor FILL = DocumentColor.rgb(255, 235, 59);

    @Test
    void chipSpanReservesHorizontalPaddingInItsWidth() throws Exception {
        List<ParagraphTextSpan> plain = textSpans(p -> p.inlineText("ABCDEF", MONO));
        List<ParagraphTextSpan> chip = textSpans(p -> p.inlineHighlight("ABCDEF", MONO, DocumentColor.GRAY, 3.0, PAD));
        assertThat(plain).hasSize(1);
        assertThat(chip).hasSize(1);
        assertThat(plain.get(0).background()).isNull();
        assertThat(chip.get(0).background()).isNotNull();
        assertThat(chip.get(0).width())
                .as("chip width = glyph width + horizontal padding")
                .isCloseTo(plain.get(0).width() + PAD.horizontal(), within(0.5));
    }

    @Test
    void chipFillPaintsBehindTheGlyphsAndKeepsTheText() throws Exception {
        byte[] pdf = render(p -> p.inlineText("Status ").inlineHighlight("OK", MONO, FILL, 3.0, PAD));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Status").contains("OK").doesNotContain("?");
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 144);
            assertThat(containsColorNear(image, 255, 235, 59, 40))
                    .as("the chip fill must paint behind the glyphs")
                    .isTrue();
        }
    }

    @Test
    void hugeCornerRadiusClampsInsteadOfThrowing() throws Exception {
        byte[] pdf = render(p -> p.inlineHighlight("X", MONO, FILL, 999.0, PAD));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 144);
            assertThat(containsColorNear(image, 255, 235, 59, 40)).isTrue();
        }
    }

    @Test
    void linkedChipEmitsAClickableAnnotationAndKeepsText() throws Exception {
        byte[] pdf = render(p -> p.inlineText("See ").inlineHighlight(
                "docs", MONO, FILL, 3.0, PAD, new DocumentLinkOptions("https://example.com")));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            boolean hasLink = document.getPage(0).getAnnotations().stream()
                    .anyMatch(PDAnnotationLink.class::isInstance);
            assertThat(hasLink).as("a linked chip emits a clickable annotation").isTrue();
            assertThat(new PDFTextStripper().getText(document)).contains("docs").doesNotContain("?");
        }
    }

    @Test
    void inlineCodeRendersOnAChipWithoutGlyphSubstitution() throws Exception {
        List<ParagraphTextSpan> spans = textSpans(p -> p.inlineText("Pkg ").inlineCode("io.github.demchaav"));
        assertThat(spans.stream().filter(s -> s.background() != null).count())
                .as("inline code is exactly one chip span")
                .isEqualTo(1);
        byte[] pdf = render(p -> p.inlineText("Pkg ").inlineCode("io.github.demchaav"));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(document))
                    .contains("io.github.demchaav").doesNotContain("?");
        }
    }

    private static byte[] render(Consumer<ParagraphBuilder> body) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(320, 140).margin(16, 16, 16, 16).create()) {
            session.dsl().pageFlow().name("Flow").addParagraph(body::accept).build();
            return session.toPdfBytes();
        }
    }

    private static List<ParagraphTextSpan> textSpans(Consumer<ParagraphBuilder> body) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(320, 140).margin(16, 16, 16, 16).create()) {
            session.dsl().pageFlow().name("Flow").addParagraph(body::accept).build();
            LayoutGraph graph = session.layoutGraph();
            return graph.fragments().stream()
                    .map(PlacedFragment::payload)
                    .filter(ParagraphFragmentPayload.class::isInstance)
                    .map(ParagraphFragmentPayload.class::cast)
                    .flatMap(payload -> payload.lines().stream())
                    .flatMap(line -> line.spans().stream())
                    .filter(ParagraphTextSpan.class::isInstance)
                    .map(ParagraphTextSpan.class::cast)
                    .toList();
        }
    }

    private static boolean containsColorNear(BufferedImage image, int r, int g, int b, int tolerance) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                if (Math.abs(((rgb >> 16) & 0xFF) - r) <= tolerance
                        && Math.abs(((rgb >> 8) & 0xFF) - g) <= tolerance
                        && Math.abs((rgb & 0xFF) - b) <= tolerance) {
                    return true;
                }
            }
        }
        return false;
    }
}
