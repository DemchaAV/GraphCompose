package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the slide backend's half of the right-to-left contract.
 *
 * <p>It is the opposite of the PDF backend's, which is why it needs its own test: the
 * PDF reverses each run because a content stream draws characters in the order given,
 * while PowerPoint has a bidirectional engine of its own and must receive logical
 * text — reversed here as well, it would display it doubly reversed, reading forwards
 * again but with the letters shuffled. What the layout does own on a slide is where
 * each word sits, so a right-to-left line goes through per-span absolute frames
 * instead of the one shared frame PowerPoint would reflow into reading order.</p>
 */
class PptxRtlParagraphTest {

    private static final String HEBREW = "שלום";
    private static final String HEBREW_WORLD = "עולם";

    @Test
    void aRightToLeftLineIsPinnedInAbsoluteFramesWithLogicalText() throws Exception {
        // A plain Hebrew line is one directional run, so one span and one frame — the
        // point is which KIND of frame. The shared line frame lets PowerPoint reflow
        // its runs into reading order, undoing the layout's ordering; the absolute
        // span frame pins the text where the page put it.
        byte[] pptx = export(HEBREW + " " + HEBREW_WORLD, TextDirection.RTL);

        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
            List<XSLFTextShape> textShapes = textShapesOf(show);

            assertThat(textShapes).isNotEmpty();
            assertThat(textShapes)
                    .allSatisfy(shape -> assertThat(shape.getShapeName())
                            .describedAs("a right-to-left line goes through per-span absolute "
                                    + "frames; naming the frame it must be rather than the one "
                                    + "it must not keeps this failing if a third kind appears, "
                                    + "or if the shared frame is simply renamed")
                            .isEqualTo("GraphCompose Inline Text Span"));

            String joined = String.join(" ",
                    textShapes.stream().map(XSLFTextShape::getText).toList());
            assertThat(joined)
                    .describedAs("the text PowerPoint receives stays logical — its own "
                            + "bidirectional engine draws it right to left; reversed here "
                            + "as well it would display doubly reversed")
                    .contains(HEBREW)
                    .doesNotContain(new StringBuilder(HEBREW).reverse().toString());
        }
    }

    @Test
    void aRichRightToLeftLineIsPinnedPerWord() throws Exception {
        // The inline path arrives as one span per word, so each word gets its own
        // absolute frame and the visual order between the frames is the layout's.
        byte[] pptx;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {

            session.pageFlow(page -> page
                    .addParagraph(p -> p
                            .rich(com.demcha.compose.document.dsl.RichText.text(
                                    HEBREW + " " + HEBREW_WORLD))
                            .direction(TextDirection.RTL)
                            .textStyle(DocumentTextStyle.builder()
                                    .fontName(FontName.DAVID_LIBRE)
                                    .size(20)
                                    .build())));

            pptx = session.toPptxBytes();
        }

        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
            assertThat(textShapesOf(show))
                    .describedAs("one frame per word, in the layout's order")
                    .hasSizeGreaterThanOrEqualTo(2);
        }
    }

    @Test
    void aLatinParagraphStillUsesTheSharedFrame() throws Exception {
        byte[] pptx = export("Hello world", TextDirection.LTR);

        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
            List<XSLFTextShape> textShapes = textShapesOf(show);

            assertThat(textShapes)
                    .describedAs("the path every existing slide takes must not change")
                    .hasSize(1);
            assertThat(textShapes.get(0).getText()).isEqualTo("Hello world");
            assertThat(textShapes.get(0).getShapeName()).isEqualTo("GraphCompose Text Line");
        }
    }

    @Test
    void arabicReachesPowerPointAsBaseLettersNotForms() throws Exception {
        // The span carries shaped Arabic because measurement measures what the PDF
        // draws; PowerPoint shapes Arabic itself, so it must get the base letters
        // back. Handing it presentation forms would freeze this engine's shaping
        // into a file a user searches and copies from.
        byte[] pptx;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {

            session.pageFlow(page -> page
                    .addParagraph(p -> p
                            .text("مرحبا")
                            .direction(TextDirection.RTL)
                            .textStyle(DocumentTextStyle.builder()
                                    .fontName(FontName.AMIRI)
                                    .size(20)
                                    .build())));

            pptx = session.toPptxBytes();
        }

        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
            String joined = String.join("",
                    textShapesOf(show).stream().map(XSLFTextShape::getText).toList());
            assertThat(joined).isEqualTo("مرحبا");
            assertThat(joined.chars().anyMatch(cp -> cp >= 0xFE70 && cp <= 0xFEFC))
                    .describedAs("no presentation form may leak into the slide text")
                    .isFalse();
        }
    }

    private static List<XSLFTextShape> textShapesOf(XMLSlideShow show) {
        return show.getSlides().get(0).getShapes().stream()
                .filter(XSLFTextShape.class::isInstance)
                .map(XSLFTextShape.class::cast)
                .filter(shape -> !shape.getText().isBlank())
                .toList();
    }

    private static byte[] export(String text, TextDirection direction) {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {

            session.pageFlow(page -> page
                    .addParagraph(p -> p
                            .text(text)
                            .direction(direction)
                            .textStyle(DocumentTextStyle.builder()
                                    .fontName(FontName.DAVID_LIBRE)
                                    .size(20)
                                    .build())));

            return session.toPptxBytes();
        }
    }
}
