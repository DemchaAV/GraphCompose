package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.image.DocumentImageData;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.xmlbeans.XmlCursor;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHyperlink;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An anchor on a block — a section, a table, an image — is a bookmark in Word.
 *
 * <p>Only a paragraph's anchor reached the file, so a link to anything else went nowhere in
 * Word, and a page reference to it had no bookmark to count. The block's bookmark opens where
 * its first line starts and closes where its last ends.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxBlockAnchorTest {

    @Test
    void aSectionsAnchorWrapsItsParagraphsAndALinkReachesIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addParagraph(p -> p.text("See the terms").linkTo("terms"))
                .addSection(s -> s.anchor("terms")
                        .addParagraph(p -> p.text("Terms"))
                        .addParagraph(p -> p.text("Payment is due in 30 days"))))) {
            XWPFParagraph opening = paragraph(document, "Terms");
            XWPFParagraph closing = paragraph(document, "Payment is due in 30 days");
            String linked = linkAnchors(document).get(0);

            assertThat(opening.getCTP().getBookmarkStartList())
                    .extracting(CTBookmark::getName)
                    .containsExactly(linked);
            assertThat(opensBeforeItsText(opening))
                    .as("a link lands on the first word, not after it")
                    .isTrue();
            assertThat(closing.getCTP().getBookmarkEndList())
                    .extracting(end -> end.getId().intValue())
                    .containsExactly(opening.getCTP().getBookmarkStartArray(0).getId().intValue());
        }
    }

    @Test
    void aTablesAnchorOpensInItsFirstCellAndClosesInItsLast() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addTable(t -> t.anchor("totals").autoColumns(2).row("Net", "100").row("Tax", "20")))) {
            XWPFTable table = document.getTables().get(0);
            XWPFTableCell firstCell = table.getRow(0).getCell(0);
            XWPFTableCell lastCell = table.getRow(1).getCell(1);

            assertThat(firstCell.getParagraphs().get(0).getCTP().getBookmarkStartList())
                    .extracting(CTBookmark::getName)
                    .containsExactly("totals");
            assertThat(lastCell.getParagraphs().get(0).getCTP().getBookmarkEndList()).hasSize(1);
        }
    }

    @Test
    void anImagesAnchorMarksThePicture() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addImage(i -> i.source(DocumentImageData.fromBytes(pngBytes()))
                        .width(40).height(20).anchor("logo")))) {
            XWPFParagraph picture = document.getParagraphs().stream()
                    .filter(paragraph -> !paragraph.getRuns().isEmpty()
                                         && !paragraph.getRuns().get(0).getEmbeddedPictures().isEmpty())
                    .findFirst()
                    .orElseThrow();

            assertThat(picture.getCTP().getBookmarkStartList())
                    .extracting(CTBookmark::getName)
                    .containsExactly("logo");
            assertThat(picture.getCTP().getBookmarkEndList()).hasSize(1);
        }
    }

    @Test
    void anAnchoredBlockThatWritesNothingLeavesNoBookmark() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addParagraph(p -> p.text("Before"))
                .addSection(s -> s.anchor("empty")))) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                assertThat(paragraph.getCTP().getBookmarkStartList()).isEmpty();
            }
        }
    }

    private static boolean opensBeforeItsText(XWPFParagraph paragraph) {
        try (XmlCursor cursor = paragraph.getCTP().newCursor()) {
            cursor.toFirstChild();
            if (paragraph.getCTP().isSetPPr()) {
                cursor.toNextSibling();
            }
            return cursor.getObject() instanceof CTBookmark;
        }
    }

    private static XWPFParagraph paragraph(XWPFDocument document, String text) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getText().equals(text))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no paragraph reads " + text));
    }

    private static List<String> linkAnchors(XWPFDocument document) {
        List<String> anchors = new ArrayList<>();
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            for (CTHyperlink link : paragraph.getCTP().getHyperlinkList()) {
                if (link.getAnchor() != null) {
                    anchors.add(link.getAnchor());
                }
            }
        }
        assertThat(anchors).hasSize(1);
        return anchors;
    }

    private static byte[] pngBytes() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }
}
