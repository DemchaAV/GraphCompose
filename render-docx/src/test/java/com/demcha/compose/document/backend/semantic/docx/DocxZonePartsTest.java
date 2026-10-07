package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.PageFieldKind;
import com.demcha.compose.document.node.PageFieldNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A page zone's parts: whether content built for a page reads as the content written, as far as
 * the zone's line is set by it, and what of a paragraph's pictures the line holds.
 */
class DocxZonePartsTest {

    private static final DocumentImageData PICTURE = DocumentImageData.fromBytes(png());

    @Test
    void contentBuiltAfreshReadsAlikeWhateverItsColourIsMadeOf() {
        // A colour built afresh is not equal to itself: it sets nothing of the line either.
        Function<String, DocumentNode> zone = text -> new RowBuilder().name("Line")
                .addParagraph(p -> p.text(text).textStyle(style(8)))
                .flexSpacer()
                .add(new PageFieldNode(PageFieldKind.NUMBER, style(8)))
                .build();

        assertThat(DocxZoneParts.readAlike(zone.apply("Acme"), zone.apply("Acme"))).isTrue();
        assertThat(DocxZoneParts.readAlike(zone.apply("Acme"), zone.apply("Other"))).as("other text").isFalse();
    }

    @Test
    void anotherFaceSizeOrFieldDoesNotReadAlike() {
        assertThat(DocxZoneParts.readAlike(paragraph("Acme", style(18)), paragraph("Acme", style(8))))
                .as("another size").isFalse();
        assertThat(DocxZoneParts.readAlike(paragraph("Acme", style(8)),
                paragraph("Acme", DocumentTextStyle.builder().size(8).decoration(
                        com.demcha.compose.document.style.DocumentTextDecoration.BOLD).build())))
                .as("another weight").isFalse();
        assertThat(DocxZoneParts.readAlike(new PageFieldNode(PageFieldKind.NUMBER, style(8)),
                new PageFieldNode(PageFieldKind.TOTAL, style(8)))).as("another field").isFalse();
        assertThat(DocxZoneParts.readAlike(paragraph("Acme", style(8)), null)).as("nothing built").isFalse();
        assertThat(DocxZoneParts.readAlike(paragraph("Acme", style(8)),
                new RowBuilder().name("Line").addParagraph(p -> p.text("Acme").textStyle(style(8)))
                        .flexSpacer().build())).as("another count of parts").isFalse();
    }

    @Test
    void thePartsReadOtherwiseAreNamedOneByOne() {
        DocumentNode written = new RowBuilder().name("Line")
                .addParagraph(p -> p.text("Acme").textStyle(style(8)))
                .addParagraph(p -> p.text("End").textStyle(style(8)))
                .build();
        DocumentNode drawn = new RowBuilder().name("Line")
                .addParagraph(p -> p.text("Acme").textStyle(style(8)))
                .addParagraph(p -> p.text("Continued").textStyle(style(8)))
                .build();

        assertThat(DocxZoneParts.partsReadOtherwise(written, drawn)).as("the second part alone")
                .containsExactly(written.children().get(1));
        assertThat(DocxZoneParts.partsReadOtherwise(written, written)).as("read alike").isEmpty();
        assertThat(DocxZoneParts.partsReadOtherwise(written, null)).as("nothing built")
                .containsExactlyInAnyOrderElementsOf(written.children());
        assertThat(DocxZoneParts.partsReadOtherwise(written, paragraph("Acme", style(8))))
                .as("another count of parts").containsExactlyInAnyOrderElementsOf(written.children());
    }

    @Test
    void aRunsFaceOrAPicturesSizeOrPlaceMakesItReadOtherwise() {
        assertThat(DocxZoneParts.readAlike(withPicture(24, InlineImageAlignment.BASELINE, style(8)),
                withPicture(24, InlineImageAlignment.BASELINE, style(8)))).isTrue();
        assertThat(DocxZoneParts.readAlike(withPicture(24, InlineImageAlignment.BASELINE, style(8)),
                withPicture(12, InlineImageAlignment.BASELINE, style(8)))).as("another size").isFalse();
        assertThat(DocxZoneParts.readAlike(withPicture(24, InlineImageAlignment.BASELINE, style(8)),
                withPicture(24, InlineImageAlignment.CENTER, style(8)))).as("another place").isFalse();
        assertThat(DocxZoneParts.readAlike(withPicture(24, InlineImageAlignment.BASELINE, style(8)),
                withPicture(24, InlineImageAlignment.BASELINE, style(18)))).as("a run's other face").isFalse();
    }

    @Test
    void theTallestPictureIsTheLinesAndOnlyAPictureOffTheBaselineStandsOffIt() {
        ParagraphNode paragraph = (ParagraphNode) new ParagraphBuilder()
                .inlineImage(PICTURE, 12, 12)
                .inlineImage(PICTURE, 30, 30, InlineImageAlignment.BASELINE)
                .inlineText(" Acme", style(8)).build();

        assertThat(DocxZoneParts.tallestPicture(paragraph)).isEqualTo(30);
        assertThat(DocxZoneParts.tallestPicture(paragraph("Acme", style(8)))).as("no picture").isZero();
        InlineRun onTheBaseline = runOf(withPicture(24, InlineImageAlignment.BASELINE, style(8)));
        InlineRun centred = runOf(withPicture(24, InlineImageAlignment.CENTER, style(8)));
        InlineRun raised = runOf((ParagraphNode) new ParagraphBuilder()
                .inlineImage(PICTURE, 24, 24, InlineImageAlignment.BASELINE, 2, null).build());
        assertThat(DocxZoneParts.setOffTheBaseline(onTheBaseline)).isFalse();
        assertThat(DocxZoneParts.setOffTheBaseline(centred)).isTrue();
        assertThat(DocxZoneParts.setOffTheBaseline(raised)).as("raised off it").isTrue();
        assertThat(DocxZoneParts.setOffTheBaseline(new com.demcha.compose.document.node.InlineTextRun("Acme")))
                .as("letters").isFalse();
    }

    private static DocumentTextStyle style(double size) {
        return DocumentTextStyle.builder().size(size).color(DocumentColor.rgb(40, 60, 90)).build();
    }

    private static ParagraphNode paragraph(String text, DocumentTextStyle style) {
        return (ParagraphNode) new ParagraphBuilder().text(text).textStyle(style).build();
    }

    private static ParagraphNode withPicture(double size, InlineImageAlignment alignment, DocumentTextStyle style) {
        return (ParagraphNode) new ParagraphBuilder().inlineImage(PICTURE, size, size, alignment)
                .inlineText(" Acme", style).build();
    }

    private static InlineRun runOf(ParagraphNode paragraph) {
        return paragraph.inlineRuns().get(0);
    }

    private static byte[] png() {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(8, 8,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
