package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Image sizing and fit in the DOCX semantic backend.
 *
 * <p>The drawn box used to come from the node's literal width and height and fell back to a
 * hardcoded 100 × 100 pt when either was missing, so an image sized only by {@code scale}, or
 * by one dimension with the other implied, came out at a size nothing asked for. {@code
 * fitMode} was not read at all, which made {@code CONTAIN} and {@code COVER} behave as
 * {@code STRETCH} — the aspect ratio was not preserved and {@code COVER} did not crop. And
 * every image was declared PNG whatever its bytes were.</p>
 *
 * <p>The source is a 40 × 20 image, so its 2:1 ratio makes each fit mode produce a different
 * answer in a square box and the three cannot pass for one another.</p>
 */
class DocxImageFitTest {

    private static final int SOURCE_WIDTH = 40;
    private static final int SOURCE_HEIGHT = 20;

    @Test
    void anImageSizedOnlyByScaleUsesItsIntrinsicSize() throws Exception {
        // The 100 x 100 fallback ignored both the intrinsic size and the scale.
        XWPFPicture picture = firstPicture(image -> image.scale(0.5));

        assertThat(emuWidth(picture)).isEqualTo(Units.toEMU(SOURCE_WIDTH * 0.5));
        assertThat(emuHeight(picture)).isEqualTo(Units.toEMU(SOURCE_HEIGHT * 0.5));
    }

    @Test
    void oneDimensionImpliesTheOtherFromTheAspectRatio() throws Exception {
        XWPFPicture picture = firstPicture(image -> image.width(80));

        assertThat(emuWidth(picture)).isEqualTo(Units.toEMU(80.0));
        assertThat(emuHeight(picture)).isEqualTo(Units.toEMU(40.0));
    }

    @Test
    void containKeepsTheAspectRatioInsideTheBox() throws Exception {
        // A square box around a 2:1 image: CONTAIN fits the width and leaves height over.
        XWPFPicture picture = firstPicture(image -> image
                .fitToBounds(100, 100)
                .fitMode(DocumentImageFitMode.CONTAIN));

        assertThat(emuWidth(picture)).isEqualTo(Units.toEMU(100.0));
        assertThat(emuHeight(picture)).isEqualTo(Units.toEMU(50.0));
        assertThat(srcRect(picture)).isNull();
    }

    @Test
    void coverFillsTheBoxAndCropsTheOverflowInSourceSpace() throws Exception {
        XWPFPicture picture = firstPicture(image -> image
                .fitToBounds(100, 100)
                .fitMode(DocumentImageFitMode.COVER));

        // The picture occupies the whole box; what does not fit is cropped, not squashed.
        assertThat(emuWidth(picture)).isEqualTo(Units.toEMU(100.0));
        assertThat(emuHeight(picture)).isEqualTo(Units.toEMU(100.0));

        // Covering a square box with a 2:1 source scales by height, making the drawn width
        // twice the box — so half the source width is hidden, a quarter on each side.
        var crop = srcRect(picture);
        assertThat(crop).isNotNull();
        assertThat(crop.getL()).isEqualTo(25_000);
        assertThat(crop.getR()).isEqualTo(25_000);
        assertThat(crop.getT()).isEqualTo(0);
        assertThat(crop.getB()).isEqualTo(0);
    }

    @Test
    void coverCropsTheOtherAxisWhenTheSourceIsTall() throws Exception {
        // The crop is computed per axis, and a wide source only ever exercises one of them.
        XWPFPicture picture = firstPicture(imageBytes("png", SOURCE_HEIGHT, SOURCE_WIDTH),
                image -> image.fitToBounds(100, 100).fitMode(DocumentImageFitMode.COVER));

        var crop = srcRect(picture);
        assertThat(crop).isNotNull();
        assertThat(crop.getT()).isEqualTo(25_000);
        assertThat(crop.getB()).isEqualTo(25_000);
        assertThat(crop.getL()).isEqualTo(0);
        assertThat(crop.getR()).isEqualTo(0);
    }

    @Test
    void stretchFillsTheBoxExactlyAndCropsNothing() throws Exception {
        XWPFPicture picture = firstPicture(image -> image
                .fitToBounds(100, 100)
                .fitMode(DocumentImageFitMode.STRETCH));

        assertThat(emuWidth(picture)).isEqualTo(Units.toEMU(100.0));
        assertThat(emuHeight(picture)).isEqualTo(Units.toEMU(100.0));
        // Filling the box distorts on purpose; nothing is hidden.
        assertThat(srcRect(picture)).isNull();
    }

    @Test
    void aJpegIsDeclaredAsAJpegRatherThanAsPng() throws Exception {
        XWPFPicture png = firstPicture(imageBytes("png"), image -> image.width(40));
        XWPFPicture jpeg = firstPicture(imageBytes("jpg"), image -> image.width(40));

        assertThat(png.getPictureData().suggestFileExtension()).isEqualTo("png");
        assertThat(jpeg.getPictureData().suggestFileExtension()).isEqualTo("jpeg");
    }

    @Test
    void anImageWiderThanThePageIsHeldToItsContentWidth() throws Exception {
        // The same clamp layout applies, so the two do not disagree about the drawn size.
        XWPFPicture picture = firstPicture(image -> image.width(10_000));

        double contentWidth = 595 - 2 * 36;
        assertThat(emuWidth(picture)).isEqualTo(Units.toEMU(contentWidth));
        assertThat((double) emuHeight(picture))
                .isEqualTo(Units.toEMU(contentWidth / 2.0), within(2.0 * Units.EMU_PER_POINT));
    }

    private static org.openxmlformats.schemas.drawingml.x2006.main.CTRelativeRect srcRect(
            XWPFPicture picture) {
        return picture.getCTPicture().getBlipFill().getSrcRect();
    }

    private static long emuWidth(XWPFPicture picture) {
        return picture.getCTPicture().getSpPr().getXfrm().getExt().getCx();
    }

    private static long emuHeight(XWPFPicture picture) {
        return picture.getCTPicture().getSpPr().getXfrm().getExt().getCy();
    }

    private static XWPFPicture firstPicture(
            Consumer<com.demcha.compose.document.dsl.ImageBuilder> spec) throws Exception {
        return firstPicture(imageBytes("png"), spec);
    }

    private static XWPFPicture firstPicture(
            byte[] bytes, Consumer<com.demcha.compose.document.dsl.ImageBuilder> spec)
            throws Exception {
        byte[] docx;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {
            session.dsl().pageFlow().name("Flow")
                    .addImage(image -> {
                        image.source(DocumentImageData.fromBytes(bytes));
                        spec.accept(image);
                    })
                    .build();
            docx = session.export(new DocxSemanticBackend());
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            List<XWPFPicture> pictures = document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .flatMap(run -> run.getEmbeddedPictures().stream())
                    .toList();
            assertThat(pictures).hasSize(1);
            return pictures.get(0);
        }
    }

    private static byte[] imageBytes(String format) throws Exception {
        return imageBytes(format, SOURCE_WIDTH, SOURCE_HEIGHT);
    }

    private static byte[] imageBytes(String format, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, (x * 6) << 16 | (y * 12) << 8 | 0x40);
            }
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, out);
            return out.toByteArray();
        }
    }
}
