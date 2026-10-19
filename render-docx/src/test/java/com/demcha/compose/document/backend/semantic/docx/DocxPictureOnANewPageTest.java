package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A picture the layout moves to a new page keeps the space the layout leaves above it there, in
 * Word.
 *
 * <p>Word drops a paragraph's space above at the top of a page, a picture's paragraph's too.
 * Measured in Word 16 and LibreOffice on Windows, a 100pt photo opening its page with a 20pt
 * margin stood at the page's top in both, 20pt high; a 15pt padding, 15pt; a 200×100 one
 * contained in a 100pt square, 25pt; one its section pads 10pt down, 10pt. Held in a line kept
 * with it, as a text paragraph's edge is, each stood where the page draws it in both, and the
 * page broke where it did.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxPictureOnANewPageTest {

    private static final double GAP = 4;
    private static final double EDGE = 12;

    /**
     * The page's content is 160pt tall: 300 x 200 with a 20pt margin. A spacer fills 150pt of it,
     * and a 60pt picture does not fit under it.
     */
    private static final double CONTENT = 160;

    @Test
    void aPicturesMarginOnANewPageIsHeldInALineKeptWithIt() throws Exception {
        try (XWPFDocument document = movedToANewPage(image -> image.margin(DocumentInsets.top(EDGE)))) {
            XWPFParagraph picture = pictureIn(document);
            XWPFParagraph line = paragraphBefore(document, picture);
            CTSpacing spacing = spacing(line);

            assertThat(line.getText()).isEmpty();
            assertThat(line.getRuns()).as("no picture of its own").allMatch(run -> run.getEmbeddedPictures().isEmpty());
            assertThat(spacing.getLineRule()).isEqualTo(STLineSpacingRule.EXACT);
            assertThat(DocxTwips.of(spacing.getLine())).as("the margin, a line's height").isEqualTo(Math.round(EDGE * 20));
            assertThat(line.getCTP().getPPr().isSetKeepNext()).as("kept with the picture").isTrue();
            assertThat(before(spacing)).as("the filler's height stays above the line").isEqualTo(owed(CONTENT - 10));
            assertThat(before(spacing(picture))).as("its edge is the line's").isZero();
        }
    }

    @Test
    void aPicturesPaddingOnANewPageIsHeld() throws Exception {
        try (XWPFDocument document = movedToANewPage(image -> image.padding(DocumentInsets.top(EDGE)))) {
            XWPFParagraph picture = pictureIn(document);

            assertThat(DocxTwips.of(spacing(paragraphBefore(document, picture)).getLine())).isEqualTo(Math.round(EDGE * 20));
            assertThat(before(spacing(picture))).isZero();
        }
    }

    @Test
    void theRoomAContainedPictureLeavesAboveItOnANewPageIsHeld() throws Exception {
        // 200×100 fitted to a 60pt square is 60×30, 15pt of room above it and 15pt below.
        try (XWPFDocument document = movedToANewPage(image -> image.source(picture(200, 100)).fitToBounds(60, 60))) {
            XWPFParagraph picture = pictureIn(document);

            assertThat(DocxTwips.of(spacing(paragraphBefore(document, picture)).getLine()))
                    .as("the room above it").isEqualTo(300);
            assertThat(before(spacing(picture))).isZero();
            assertThat(before(spacing(paragraphAfter(document, picture))))
                    .as("the room below it, the line under it raised into it as into any space")
                    .isEqualTo(300 - DocxExports.DEFAULT_LINE_RAISE);
        }
    }

    @Test
    void aPictureItsSectionPadsDownOnANewPageHoldsThePadding() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(300, 200, 20, page -> page
                .spacer(0, CONTENT - 10)
                .addSection("Figure", section -> section
                        .padding(DocumentInsets.top(EDGE))
                        .addImage(image -> image.source(picture(60, 60)).size(60, 60))))) {
            XWPFParagraph picture = pictureIn(document);
            CTSpacing spacing = spacing(paragraphBefore(document, picture));

            assertThat(DocxTwips.of(spacing.getLine())).as("the section's padding").isEqualTo(Math.round(EDGE * 20));
            assertThat(before(spacing)).isEqualTo(owed(CONTENT - 10));
            assertThat(before(spacing(picture))).isZero();
        }
    }

    @Test
    void aPictureWhoseGapCarriesToANewPageHoldsTheGapWithItsEdge() throws Exception {
        // 157pt leave 3, less than the gap: the gap and the picture open the second page.
        try (XWPFDocument document = DocxExports.withLayout(300, 200, 20, page -> page
                .spacing(GAP)
                .spacer(0, CONTENT - 3)
                .addImage(image -> image.source(picture(60, 60)).size(60, 60).margin(DocumentInsets.top(EDGE))))) {
            CTSpacing spacing = spacing(paragraphBefore(document, pictureIn(document)));

            assertThat(DocxTwips.of(spacing.getLine())).as("the gap and the margin")
                    .isEqualTo(Math.round((GAP + EDGE) * 20));
            assertThat(before(spacing)).as("the filler's height alone").isEqualTo(owed(CONTENT - 3));
        }
    }

    @Test
    void aPictureWithNoEdgeOnANewPageHasNoLine() throws Exception {
        try (XWPFDocument document = movedToANewPage(UnaryOperator.identity())) {
            assertThat(document.getParagraphs()).as("the filler, the picture and the text after it").hasSize(3);
        }
    }

    @Test
    void aPictureOnThePageOfTheBlockBeforeItIsWrittenAsBefore() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(300, 600, 20, page -> page
                .spacer(0, CONTENT - 10)
                .addImage(image -> image.source(picture(60, 60)).size(60, 60).margin(DocumentInsets.top(EDGE))))) {
            XWPFParagraph picture = pictureIn(document);

            assertThat(document.getParagraphs()).as("the filler and the picture, no line between them").hasSize(2);
            assertThat(before(spacing(picture))).isEqualTo(owed(CONTENT - 10 + EDGE));
        }
    }

    @Test
    void aPictureInARowsCellIsLeftToItsTable() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(300, 200, 20, page -> page
                .spacer(0, CONTENT - 10)
                .addRow(row -> row
                        .addImage(image -> image.source(picture(60, 60)).size(60, 60).margin(DocumentInsets.top(EDGE)))
                        .addParagraph("Beside")))) {
            List<XWPFParagraph> cell = document.getTables().get(0).getRow(0).getCell(0).getParagraphs();

            assertThat(cell.get(0).getRuns()).as("the picture opens its cell, no line before it")
                    .anyMatch(run -> !run.getEmbeddedPictures().isEmpty());
        }
    }

    @Test
    void aPictureOpeningABandIsLeftToTheBand() throws Exception {
        // The band, moved to the second page, sets its layers from its own top, and holds what
        // opens its page itself (raiseIntoTheSpaceAbove).
        java.util.function.Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content = page -> page
                .spacer(0, CONTENT - 10)
                .addLayerStack(stack -> stack
                        .topLeft(new ImageBuilder().source(picture(60, 60)).size(60, 60)
                                .margin(DocumentInsets.top(EDGE)).build())
                        .bottomRight(new com.demcha.compose.document.dsl.ParagraphBuilder().text("Caption").build()));
        try (XWPFDocument moved = DocxExports.withLayout(300, 200, 20, content);
             XWPFDocument onOnePage = DocxExports.withLayout(300, 600, 20, content)) {
            List<XWPFParagraph> paragraphs = moved.getParagraphs();

            assertThat(paragraphs.indexOf(pictureIn(moved))).as("no line of the flow's held before it")
                    .isEqualTo(onOnePage.getParagraphs().indexOf(pictureIn(onOnePage)));
        }
    }

    /** A 60pt picture under a 150pt filler, which the layout moves to the second page, and a line after it. */
    private static XWPFDocument movedToANewPage(UnaryOperator<ImageBuilder> image) throws Exception {
        return DocxExports.withLayout(300, 200, 20, page -> page
                .spacer(0, CONTENT - 10)
                .addImage(builder -> image.apply(builder.source(picture(60, 60)).size(60, 60)))
                .addParagraph("After"));
    }

    /** A spacer's paragraph is a tenth of a point tall, in twips (see holdToHairline). */
    private static final long HAIRLINE = 2;

    /**
     * The space owed above the block after the filler, in twips: {@code points} of it, less the
     * filler's own hairline, which is part of its height.
     */
    private static long owed(double points) {
        return Math.round(points * 20) - HAIRLINE;
    }

    private static XWPFParagraph pictureIn(XWPFDocument document) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getRuns().stream().anyMatch(run -> !run.getEmbeddedPictures().isEmpty()))
                .findFirst().orElseThrow();
    }

    private static XWPFParagraph paragraphBefore(XWPFDocument document, XWPFParagraph paragraph) {
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        return paragraphs.get(paragraphs.indexOf(paragraph) - 1);
    }

    private static XWPFParagraph paragraphAfter(XWPFDocument document, XWPFParagraph paragraph) {
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        return paragraphs.get(paragraphs.indexOf(paragraph) + 1);
    }

    private static CTSpacing spacing(XWPFParagraph paragraph) {
        return paragraph.getCTP().getPPr().getSpacing();
    }

    private static long before(CTSpacing spacing) {
        return spacing != null && spacing.isSetBefore() ? DocxTwips.of(spacing.getBefore()) : 0;
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
}
