package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A portrait in a ring: the photo the size the page draws it, standing as far inside the ring as
 * the page sets it.
 *
 * <p>{@code NavySidebar}'s portrait is a 127pt ring round a 123.8pt photo, in a column 123.8pt
 * wide. Sized from the width left inside the ring, the photo came out 120.6pt; written flush with
 * the ring's top, it lost the 1.6pt the ring holds round it above and below, and the column under
 * it stood 5.2pt high in Word.</p>
 */
class DocxPortraitTest {

    private static final double PHOTO = 123.8;
    private static final double RING = 1.6;

    @Test
    void aPhotoInARingWiderThanItsColumnKeepsItsSize() throws Exception {
        try (XWPFDocument document = portrait()) {
            XWPFParagraph photo = photoParagraph(document);
            long width = photo.getCTP().getRList().get(0).getDrawingArray(0).getInlineArray(0).getExtent().getCx();

            assertThat(width).as("the photo the page draws, not the width left inside the ring")
                    .isEqualTo(Math.round(PHOTO * 12700));
        }
    }

    @Test
    void aPaddedPictureIsTheSizeItAskedFor() throws Exception {
        // Its placement holds its padding, which is written as the space around its paragraph.
        try (XWPFDocument document = DocxExports.withLayout(400, 400, 20, page -> page
                .addImage(image -> image.name("Logo").source(DocumentImageData.fromBytes(pngBytes()))
                        .size(100, 50).padding(DocumentInsets.of(10))))) {
            var extent = document.getParagraphs().stream()
                    .filter(p -> !p.getRuns().isEmpty() && p.getRuns().get(0).getCTR().sizeOfDrawingArray() > 0)
                    .findFirst().orElseThrow()
                    .getRuns().get(0).getCTR().getDrawingArray(0).getInlineArray(0).getExtent();

            assertThat(extent.getCx()).isEqualTo(100L * 12700);
            assertThat(extent.getCy()).isEqualTo(50L * 12700);
        }
    }

    @Test
    void aPhotoStandsAsFarInsideItsRingAsThePageSetsIt() throws Exception {
        try (XWPFDocument document = portrait()) {
            List<XWPFParagraph> paragraphs = sidebar(document);
            XWPFParagraph photo = photoParagraph(document);
            XWPFParagraph heading = paragraphs.get(paragraphs.indexOf(photo) + 1);

            assertThat(beforeOf(photo)).as("the sidebar's 25pt and the ring above the photo")
                    .isEqualTo(Math.round((25 + RING) * 20));
            assertThat(beforeOf(heading)).as("the ring below the photo, and the heading's 22pt")
                    .isEqualTo(Math.round((RING + 22) * 20));
        }
    }

    private static XWPFDocument portrait() throws Exception {
        // As the template lays it: a sidebar column of a row, padded to 123.8pt, the ring in a
        // section of its own at its top.
        double pad = (180 - PHOTO) / 2;
        return DocxExports.withLayout(400, 400, 20, page -> page.addRow("Page", row -> row
                .columns(com.demcha.compose.document.style.DocumentRowColumn.fixed(180),
                        com.demcha.compose.document.style.DocumentRowColumn.weight(1))
                .addSection("Sidebar", sidebar -> sidebar.padding(new DocumentInsets(25, pad, 0, pad))
                .addSection("Avatar", avatar -> avatar.addContainer(ring -> ring.name("AvatarRing")
                        .circle(PHOTO + 2 * RING)
                        .fillColor(DocumentColor.rgb(220, 224, 230))
                        .center(new ShapeContainerBuilder().name("AvatarCircle").circle(PHOTO)
                                .clipPolicy(ClipPolicy.CLIP_PATH)
                                .fillColor(DocumentColor.rgb(20, 40, 70))
                                .center(new ImageBuilder().name("AvatarPhoto")
                                        .source(DocumentImageData.fromBytes(pngBytes()))
                                        .size(PHOTO, PHOTO).build())
                                .build())))
                .addParagraph(p -> p.text("CONTACT").margin(new DocumentInsets(22, 0, 0, 0))))
                .addParagraph(p -> p.text("Main"))));
    }

    /** The sidebar cell's paragraphs. */
    private static List<XWPFParagraph> sidebar(XWPFDocument document) {
        return document.getTables().get(0).getRow(0).getCell(0).getParagraphs();
    }

    private static XWPFParagraph photoParagraph(XWPFDocument document) {
        return sidebar(document).stream()
                .filter(p -> !p.getRuns().isEmpty() && p.getRuns().get(0).getCTR().sizeOfDrawingArray() > 0
                             && p.getRuns().get(0).getCTR().getDrawingArray(0).sizeOfInlineArray() > 0)
                .findFirst().orElseThrow();
    }

    private static long beforeOf(XWPFParagraph paragraph) {
        var spacing = paragraph.getCTP().getPPr().getSpacing();
        return spacing.isSetBefore() ? ((Number) spacing.getBefore()).longValue() : 0;
    }

    private static byte[] pngBytes() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }
}
