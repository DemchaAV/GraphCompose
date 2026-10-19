package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A picture contained in its box keeps the room it leaves round it.
 *
 * <p>The page draws a contained picture in its box's middle. Measured in Word 16 and LibreOffice
 * on Windows, a 200×100 picture fitted to a 100pt square stood at the square's top, and the
 * paragraph under it 51pt high, in both. With the room above and below written as its paragraph's
 * space, the picture stood where the page draws it in both, and a 100×200 one held in by the room
 * at its side too.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxContainedPictureTest {

    @Test
    void aWidePictureKeepsTheRoomAboveAndBelowIt() throws Exception {
        Exported exported = export(page -> page.addParagraph("Before")
                .addImage(image -> image.source(picture(200, 100)).fitToBounds(100, 100)).addParagraph("After"));

        XWPFParagraph picture = exported.pictureParagraph();
        assertThat(picture.getSpacingBefore()).as("25pt above").isEqualTo(500);
        assertThat(exported.below(picture)).as("and 25pt below, the line under it raised into it as into any space")
                .isEqualTo(500 - DocxExports.DEFAULT_LINE_RAISE);
        assertThat(left(picture)).as("filling the box across").isZero();
    }

    @Test
    void aTallPictureKeepsTheRoomAtItsSide() throws Exception {
        Exported exported = export(page -> page.addParagraph("Before")
                .addImage(image -> image.source(picture(100, 200)).fitToBounds(100, 100)).addParagraph("After"));

        XWPFParagraph picture = exported.pictureParagraph();
        assertThat(left(picture)).as("25pt in, the middle of its box").isEqualTo(500);
        assertThat(Math.max(0, picture.getSpacingBefore())).as("filling it down").isZero();
    }

    @Test
    void aPictureThatFitsItsBoxIsWrittenAsBefore() throws Exception {
        Exported exported = export(page -> page.addParagraph("Before")
                .addImage(image -> image.source(picture(200, 100)).fitToBounds(200, 100)).addParagraph("After"));

        XWPFParagraph picture = exported.pictureParagraph();
        assertThat(Math.max(0, picture.getSpacingBefore())).isZero();
        assertThat(left(picture)).isZero();
    }

    @Test
    void aStretchedPictureFillsItsBox() throws Exception {
        Exported exported = export(page -> page.addParagraph("Before")
                .addImage(image -> image.source(picture(200, 100)).size(100, 100).fitMode(DocumentImageFitMode.STRETCH))
                .addParagraph("After"));

        assertThat(Math.max(0, exported.pictureParagraph().getSpacingBefore())).isZero();
    }

    @Test
    void aContainedPictureInARowsCellKeepsItsRoomThere() throws Exception {
        Exported exported = export(page -> page.addParagraph("Before").addRow(row -> row
                        .addImage(image -> image.source(picture(200, 100)).fitToBounds(100, 100))
                        .addParagraph("Beside"))
                .addParagraph("After"));

        XWPFParagraph picture = pictureIn(exported.document().getTables().get(0).getRow(0).getCell(0).getParagraphs());
        assertThat(picture.getSpacingBefore()).isEqualTo(500);
        assertThat(picture.getSpacingAfter()).as("the room below paid in the cell").isEqualTo(500);
    }

    @Test
    void aContainedPictureComposedInATablesCellKeepsItsRoomThere() throws Exception {
        Exported exported = export(page -> page.addParagraph("Before").addTable(table -> table
                        .columns(com.demcha.compose.document.table.DocumentTableColumn.fixed(120),
                                com.demcha.compose.document.table.DocumentTableColumn.fixed(100))
                        .rowCells(com.demcha.compose.document.table.DocumentTableCell.node(new ImageBuilder()
                                        .source(picture(200, 100)).fitToBounds(100, 100).build()),
                                com.demcha.compose.document.table.DocumentTableCell.text("Beside")))
                .addParagraph("After"));

        XWPFParagraph picture = pictureIn(exported.document().getTables().get(0).getRow(0).getCell(0).getParagraphs());
        assertThat(picture.getSpacingBefore()).isEqualTo(500);
    }

    @Test
    void aCentredTallPictureStandsInItsBoxsMiddle() throws Exception {
        // The 100pt box centred in the 256pt column stands 78pt in, and the picture 25pt further.
        Exported exported = export(page -> page.addParagraph("Before")
                .addAligned(com.demcha.compose.document.node.HorizontalAlign.CENTER,
                        new ImageBuilder().source(picture(100, 200)).fitToBounds(100, 100).build())
                .addParagraph("After"));

        assertThat(left(exported.pictureParagraph())).isCloseTo(Math.round((78 + 25) * 20.0),
                org.assertj.core.data.Offset.offset(2L));
    }

    @Test
    void aContainedPictureInABandKeepsTheRoomBelowItAsItsOwn() throws Exception {
        // A band owes the space under it from the picture's box, which holds the room below it.
        Exported exported = export(page -> page.addParagraph("Before").addLayerStack(stack -> stack
                        .topLeft(new com.demcha.compose.document.dsl.ParagraphBuilder().text("Heading").build())
                        .bottomLeft(new ImageBuilder().source(picture(200, 100)).fitToBounds(100, 100).build()))
                .addParagraph("After"));

        assertThat(exported.pictureParagraph().getSpacingAfter()).isEqualTo(500);
    }

    private static XWPFParagraph pictureIn(List<XWPFParagraph> paragraphs) {
        return paragraphs.stream()
                .filter(paragraph -> !paragraph.getRuns().isEmpty()
                                     && !paragraph.getRuns().get(0).getEmbeddedPictures().isEmpty())
                .findFirst().orElseThrow();
    }

    private static long left(XWPFParagraph paragraph) {
        CTInd indent = paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr().getInd() : null;
        return indent == null || !indent.isSetLeft() ? 0L : DocxTwips.of(indent.getLeft());
    }

    private static DocumentImageData picture(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try {
            javax.imageio.ImageIO.write(image, "png", png);
        } catch (java.io.IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
        return DocumentImageData.fromBytes(png.toByteArray());
    }

    private record Exported(XWPFDocument document) {

        XWPFParagraph pictureParagraph() {
            return pictureIn(document.getParagraphs());
        }

        /** The space under a paragraph: its own after and the next one's before, in twips. */
        long below(XWPFParagraph paragraph) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            XWPFParagraph next = paragraphs.get(paragraphs.indexOf(paragraph) + 1);
            return Math.max(0, paragraph.getSpacingAfter()) + Math.max(0, next.getSpacingBefore());
        }
    }

    private static Exported export(Consumer<PageFlowBuilder> content) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(72)).create()) {
            session.pageFlow(content::accept);
            return new Exported(new XWPFDocument(new ByteArrayInputStream(session.export(new DocxSemanticBackend()))));
        }
    }
}
