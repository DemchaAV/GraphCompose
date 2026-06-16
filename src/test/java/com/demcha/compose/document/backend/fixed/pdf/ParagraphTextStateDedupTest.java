package com.demcha.compose.document.backend.fixed.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.style.DocumentColor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Guards Finding 5: the paragraph render handler tracks the last-written font and
 * non-stroking colour, so a single-style paragraph that wraps into many spans
 * emits <b>one</b> {@code setFont} ({@code Tf}) operator for the whole paragraph
 * instead of one per span.
 *
 * <p>Renders a real one-page document and inspects the page content stream through
 * the established {@link PDFStreamParser} token pattern — the proof lives entirely
 * in test scope, with no instrumentation in the render handler.</p>
 */
class ParagraphTextStateDedupTest {

    @Test
    void singleStyleParagraphEmitsOneFontOperatorAcrossManySpans() throws Exception {
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 800)
                .margin(24, 24, 24, 24)
                .create()) {
            // One uniform style; long enough to wrap into many lines/spans on a
            // single page so the dedup is meaningful (without the guard this emits
            // a Tf per span).
            String body = ("GraphCompose lays out structured documents across pages "
                    + "while keeping headers and footers stable. ").repeat(8);
            session.pageFlow(flow -> flow.addParagraph(p -> p.text(body)));
            pdf = session.toPdfBytes();
        }

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages())
                    .describedAs("body is sized to stay on one page so one q...Q block covers every span")
                    .isEqualTo(1);
            int fontOps = operatorCount(document, "Tf");
            int textDraws = operatorCount(document, "Tj") + operatorCount(document, "TJ");

            assertThat(textDraws)
                    .describedAs("the paragraph must wrap into several drawn spans for the dedup to be meaningful")
                    .isGreaterThanOrEqualTo(2);
            assertThat(fontOps)
                    .describedAs("one setFont for the whole single-style paragraph, not one per span")
                    .isEqualTo(1);
        }
    }

    @Test
    void multiStyleParagraphReEmitsFontOnEachStyleChange() throws Exception {
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 800)
                .margin(24, 24, 24, 24)
                .create()) {
            // Three consecutive runs with distinct decorations (regular / bold /
            // regular) on one line: the tracker must re-emit Tf at each change,
            // not over-dedup them into a single setFont (which would draw the bold
            // run in the regular font).
            session.pageFlow(flow -> flow.addParagraph(p ->
                    p.rich(r -> r.plain("alpha ").bold("bravo ").plain("charlie"))));
            pdf = session.toPdfBytes();
        }

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(operatorCount(document, "Tf"))
                    .describedAs("a style change within a paragraph must re-emit setFont (single-style baseline is 1)")
                    .isGreaterThanOrEqualTo(2);
        }
    }

    @Test
    void sameStyleTextAroundAnInlineShapeReEmitsTheFont() throws Exception {
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 200)
                .margin(24, 24, 24, 24)
                .create()) {
            // Two same-style text runs with an inline shape between them. The
            // shape ends the text block and runs its own graphics-state/colour
            // ops, so the handler must invalidate the tracked font — otherwise
            // the second run trusts the stale state, skips its Tf, and draws
            // through whatever the shape left set. Same style, so without the
            // invalidate the two runs would dedup to a single setFont.
            session.pageFlow(flow -> flow.addParagraph(p -> p.rich(r -> r
                    .plain("alpha ")
                    .diamond(8, DocumentColor.rgb(196, 30, 58))
                    .plain(" bravo"))));
            pdf = session.toPdfBytes();
        }

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(operatorCount(document, "Tf"))
                    .describedAs("the text span after an inline shape must re-emit setFont")
                    .isEqualTo(2);
        }
    }

    @Test
    void sameStyleTextAroundAnInlineImageReEmitsTheFont() throws Exception {
        DocumentImageData dot = DocumentImageData.fromBytes(onePixelPng());
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 200)
                .margin(24, 24, 24, 24)
                .create()) {
            // Same guard for the inline-image span: drawImage breaks the text
            // block, so the text after it must re-emit its setFont.
            session.pageFlow(flow -> flow.addParagraph(p -> p.rich(r -> r
                    .plain("alpha ")
                    .image(dot, 8, 8)
                    .plain(" bravo"))));
            pdf = session.toPdfBytes();
        }

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(operatorCount(document, "Tf"))
                    .describedAs("the text span after an inline image must re-emit setFont")
                    .isEqualTo(2);
        }
    }

    private static byte[] onePixelPng() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, java.awt.Color.WHITE.getRGB());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static int operatorCount(PDDocument document, String operatorName) throws IOException {
        int count = 0;
        for (var page : document.getPages()) {
            List<Object> tokens = new PDFStreamParser(page).parse();
            for (Object token : tokens) {
                if (token instanceof Operator operator && operatorName.equals(operator.getName())) {
                    count++;
                }
            }
        }
        return count;
    }
}
