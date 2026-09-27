package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A block set in an alignment stands where the alignment puts it, not at the left edge of the
 * width it is set in.
 *
 * @author Artem Demchyshyn
 */
class DocxAlignTest {

    @Test
    void aCentredPictureIsHeldInToWhereThePageCentresIt() throws Exception {
        // 360pt between the margins, a 60pt picture: 150pt on either side.
        CTInd indent = pictureIndent(HorizontalAlign.CENTER);

        assertThat(DocxTwips.of(indent.getLeft())).isEqualTo(150 * 20L);
        assertThat(DocxTwips.of(indent.getRight())).isEqualTo(150 * 20L);
    }

    @Test
    void aPictureAlignedRightStandsAtTheRightMargin() throws Exception {
        CTInd indent = pictureIndent(HorizontalAlign.RIGHT);

        assertThat(DocxTwips.of(indent.getLeft())).isEqualTo(300 * 20L);
        assertThat(indent.getRight() == null || DocxTwips.of(indent.getRight()) == 0).isTrue();
    }

    private static CTInd pictureIndent(HorizontalAlign align) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400)
                .margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addAligned(align, new ImageBuilder().name("Portrait")
                    .source(DocumentImageData.fromBytes(png())).size(60, 60).build()));
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(
                    session.export(new DocxSemanticBackend())))) {
                XWPFParagraph picture = document.getParagraphs().stream()
                        .filter(paragraph -> !paragraph.getRuns().isEmpty()
                                             && !paragraph.getRuns().get(0).getEmbeddedPictures().isEmpty())
                        .findFirst()
                        .orElseThrow();
                return picture.getCTP().getPPr().getInd();
            }
        }
    }

    private static byte[] png() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }
}
