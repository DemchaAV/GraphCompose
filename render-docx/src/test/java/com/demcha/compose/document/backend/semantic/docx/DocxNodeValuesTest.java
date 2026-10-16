package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentLetterSpacing;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two builds of a node tree read as the same by their values, down to every run, style and
 * picture: a colour, a picture's data and an SVG drawing built afresh among them.
 *
 * @author Artem Demchyshyn
 */
class DocxNodeValuesTest {

    private static final String SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 10 10\">"
                                      + "<path d=\"M0 0 L10 10\" stroke=\"#000\"/></svg>";

    @Test
    void textInAColourBuiltAfreshReadsAsTheSame() {
        assertThat(DocxNodeValues.alike(text(DocumentColor.rgb(200, 0, 0)), text(DocumentColor.rgb(200, 0, 0)))).isTrue();
        assertThat(DocxNodeValues.alike(text(DocumentColor.rgb(200, 0, 0)), text(DocumentColor.rgb(0, 0, 200)))).isFalse();
    }

    @Test
    void aPictureReadsByItsDataWhateverItsBox() {
        assertThat(DocxNodeValues.alike(picture(png(4)), picture(png(4)))).as("the same bytes, read afresh").isTrue();
        assertThat(DocxNodeValues.alike(picture(png(4)), picture(png(8))))
                .as("another picture in the same box").isFalse();
    }

    @Test
    void trackedLettersReadAsOtherwise() {
        DocumentTextStyle plain = DocumentTextStyle.DEFAULT;
        DocumentNode tracked = new ParagraphBuilder().text("Acme")
                .textStyle(plain.withLetterSpacing(DocumentLetterSpacing.points(2))).build();

        assertThat(DocxNodeValues.alike(new ParagraphBuilder().text("Acme").textStyle(plain).build(), tracked)).isFalse();
    }

    @Test
    void anSvgDrawingParsedTwiceReadsAsTheSame() {
        assertThat(DocxNodeValues.alike(SvgIcon.parse(SVG), SvgIcon.parse(SVG))).isTrue();
        assertThat(DocxNodeValues.alike(SvgIcon.parse(SVG), SvgIcon.parse(SVG.replace("L10 10", "L10 0")))).isFalse();
    }

    @Test
    void contentBuiltAgainstNoneReadsAsOtherwise() {
        assertThat(DocxNodeValues.alike(text(DocumentColor.BLACK), null)).isFalse();
        assertThat(DocxNodeValues.alike(null, null)).isTrue();
    }

    private static DocumentNode text(DocumentColor colour) {
        return new ParagraphBuilder().text("Acme").textStyle(DocumentTextStyle.DEFAULT.withColor(colour)).build();
    }

    private static DocumentNode picture(byte[] data) {
        return new ImageBuilder().source(data).size(48, 24).build();
    }

    private static byte[] png(int side) {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(side, side,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
