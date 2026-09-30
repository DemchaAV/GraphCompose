package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.pdf.PdfFontLibraryFactory;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A line's letters reach as far as their own outlines, read from the face the layout measured.
 */
class DocxInkTest {

    private static final double SIZE = 20;

    @Test
    void lettersReachAsFarAsTheirOwnOutlines() throws Exception {
        TrueTypeFont face = spectralRegular();
        double scale = SIZE / face.getUnitsPerEm();
        double xHeight = face.getGlyph().getGlyph(face.nameToGID("x")).getBoundingBox().getUpperRightY() * scale;
        double pDepth = -face.getGlyph().getGlyph(face.nameToGID("p")).getBoundingBox().getLowerLeftY() * scale;

        double[] low = DocxInk.of(line("xxx"), fonts());
        double[] deep = DocxInk.of(line("xpx"), fonts());

        assertThat(low[0]).as("to the top of an x").isCloseTo(xHeight, within(0.05));
        assertThat(low[1]).as("an x barely below the baseline").isLessThan(0.5);
        assertThat(deep[1]).as("to the foot of a p").isCloseTo(pDepth, within(0.05));
    }

    @Test
    void aLetterReachesLessFarThanTheFacesAscentAndDescent() throws Exception {
        ParagraphLine line = line("Brand");
        double[] reach = DocxInk.of(line, fonts());

        assertThat(reach[0] + reach[1]).as("the letters of one line, not the room the face keeps")
                .isLessThan(line.lineHeight() - 5);
    }

    @Test
    void aLineHoldingAPictureHasNoLettersToRead() throws Exception {
        // A picture's height is not a glyph's: the line's reach is not known.
        java.io.ByteArrayOutputStream png = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(8, 8,
                java.awt.image.BufferedImage.TYPE_INT_RGB), "png", png);
        ParagraphLine line;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400)
                .margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addParagraph(p -> p.inlineText("Icon ", DocumentTextStyle.builder()
                            .fontName(FontName.SPECTRAL).size(SIZE).build())
                    .inlineImage(com.demcha.compose.document.image.DocumentImageData.fromBytes(png.toByteArray()),
                            8, 8)));
            line = session.layoutGraph().fragments().stream()
                    .filter(fragment -> fragment.payload() instanceof ParagraphFragmentPayload)
                    .map(fragment -> ((ParagraphFragmentPayload) fragment.payload()).lines().get(0))
                    .findFirst().orElseThrow();
        }

        assertThat(DocxInk.of(line, fonts())).isNull();
    }

    private static com.demcha.compose.font.FontLibrary fonts() {
        return PdfFontLibraryFactory.measurementLibrary(List.of());
    }

    private static ParagraphLine line(String text) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400)
                .margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addParagraph(p -> p.text(text).textStyle(DocumentTextStyle.builder()
                    .fontName(FontName.SPECTRAL).size(SIZE).build())));
            return session.layoutGraph().fragments().stream()
                    .filter(fragment -> fragment.payload() instanceof ParagraphFragmentPayload)
                    .map(fragment -> ((ParagraphFragmentPayload) fragment.payload()).lines().get(0))
                    .findFirst().orElseThrow();
        }
    }

    private static TrueTypeFont spectralRegular() throws Exception {
        try (InputStream in = DocxInkTest.class.getResourceAsStream("/fonts/google/spectral/Spectral-Regular.ttf")) {
            assertThat(in).as("Spectral on the test class path").isNotNull();
            return new TTFParser().parse(new RandomAccessReadBuffer(in));
        }
    }
}
