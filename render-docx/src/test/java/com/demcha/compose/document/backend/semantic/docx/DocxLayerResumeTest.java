package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.ListBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A later layer of a band or a column resumes at the page's distance below the layers before it,
 * though its first block holds more above its content than that distance.
 *
 * <p>A layer after another is written after it, and the space above its first block is the page's
 * gap from the layers above to the block's content, less what the block writes above that content
 * itself: its margin, its padding, a contained picture's room in its box. Where the block holds
 * more than the gap, the difference is below zero and was kept at zero, so the block wrote all of
 * it after the layers before it. Measured in Word 16 and LibreOffice on Windows, a caption with a
 * 20pt margin 7pt under the line above stood 12.7pt low, so did one with a 20pt padding, a picture
 * contained in a box level with that line 12.9pt, and a table with a 20pt margin 12.6pt. A list
 * lost its own margin there instead, the resume taking its place, and one in a later layer stood 7pt
 * high, one opening a band 10 to 20pt high. Given back, each stands within 0.4pt of the page's
 * distance from the line above it.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxLayerResumeTest {

    /** The page's gap from "Opening" to a block whose 20pt edge starts at the band's top: 7.05pt. */
    private static final long GAP = 141;

    /** Word may raise a line into the space above it, a point at most here. */
    private static final long RAISE = 20;

    @Test
    void aLaterLayersMarginReachingAboveTheLayersBeforeIsGivenBack() throws Exception {
        try (XWPFDocument document = export(band(new ParagraphBuilder().text("Caption")
                .margin(DocumentInsets.top(20)).build()))) {
            assertThat(before(anywhere(document, "Caption"))).as("the page's 7pt gap, not the 20pt margin")
                    .isBetween(GAP - RAISE, GAP);
        }
    }

    @Test
    void aLaterLayersPaddingReachingAboveIsGivenBackAndNotNamed() throws Exception {
        Consumer<PageFlowBuilder> content = band(new ParagraphBuilder().text("Caption")
                .padding(DocumentInsets.top(20)).build());
        try (XWPFDocument document = export(content)) {
            assertThat(before(anywhere(document, "Caption"))).isBetween(GAP - RAISE, GAP);
        }
        assertThat(DocxExports.report(400, 600, 20, content).bySubject().get("overlapping layer"))
                .as("its text starts below the line above").isNull();
    }

    @Test
    void aContainedPicturesRoomReachingAboveIsGivenBackAndNotNamed() throws Exception {
        // A 200x100 picture contained in a 100pt box at the band's top has 25pt of room above it:
        // the page draws it 12.05pt under the line above.
        Consumer<PageFlowBuilder> content = band(new ImageBuilder().source(picture(200, 100)).fitToBounds(100, 100)
                .build());
        try (XWPFDocument document = export(content)) {
            assertThat(before(pictureIn(document))).as("12.05pt, not its 25pt of room").isEqualTo(241);
        }
        assertThat(DocxExports.report(400, 600, 20, content).bySubject().get("overlapping layer"))
                .as("the picture starts below the line above, its box above it").isNull();
    }

    @Test
    void aColumnsLaterLayerGivesItsMarginBackInItsCell() throws Exception {
        try (XWPFDocument document = export(page -> page.addParagraph("Above").addLayerStack(stack -> stack
                .topLeft(new SectionBuilder().padding(new DocumentInsets(0, 150, 0, 0)).addParagraph("Opening").build())
                .topLeft(new SectionBuilder().padding(new DocumentInsets(0, 150, 0, 0))
                        .addParagraph(p -> p.text("Caption").margin(DocumentInsets.top(20))).build())
                .topLeft(new SectionBuilder().padding(new DocumentInsets(0, 0, 0, 150)).addParagraph("Beside").build())))) {
            assertThat(document.getTables()).as("the premise: written as columns").hasSize(1);
            assertThat(before(anywhere(document, "Caption"))).isBetween(GAP - RAISE, GAP);
        }
    }

    @Test
    void aTableInALaterLayerTakesItsMarginOutOfTheGap() throws Exception {
        try (XWPFDocument document = export(band(new TableBuilder().columns(DocumentTableColumn.auto())
                .row("Caption").margin(DocumentInsets.top(20)).build()))) {
            assertThat(after(anywhere(document, "Opening"))).as("the gap above the table: 7pt, not 20")
                    .isBetween(GAP - RAISE, GAP + RAISE);
        }
    }

    @Test
    void aListInALaterLayerKeepsItsMarginAfterTheResume() throws Exception {
        try (XWPFDocument document = export(band(new ListBuilder().items("Caption")
                .margin(DocumentInsets.top(20)).build()))) {
            assertThat(before(anywhere(document, "Caption"))).as("the page's gap, its margin no longer lost")
                    .isBetween(GAP - RAISE, GAP);
        }
    }

    @Test
    void aListOpeningABandKeepsItsMargin() throws Exception {
        // The band starts at the list's margin box: the space above its first item is the margin.
        try (XWPFDocument document = export(page -> page.addParagraph("Above").addLayerStack(stack -> stack
                .back(new ShapeBuilder().size(200, 80).build())
                .layer(new ListBuilder().margin(DocumentInsets.top(20)).items("Title").build(), LayerAlign.TOP_LEFT)))) {
            XWPFParagraph title = anywhere(document, "Title");

            assertThat(before(title)).isBetween(400 - RAISE, 400L);
            assertThat(title.getRuns()).as("raised into that space, if at all, not by its position")
                    .noneMatch(run -> run.getCTR().isSetRPr() && run.getCTR().getRPr().sizeOfPositionArray() > 0);
        }
    }

    @Test
    void aCaptionOverAPictureIsWrittenUnderItAndNamed() throws Exception {
        // The page sets the caption over the picture's foot: Word writes it right under the picture,
        // its 6pt margin given back, and the report names it.
        Consumer<PageFlowBuilder> content = page -> page.addParagraph("Above").addLayerStack(stack -> stack
                .topLeft(new ImageBuilder().source(picture(200, 100)).size(120, 60).build())
                .bottomRight(new ParagraphBuilder().text("Caption").margin(DocumentInsets.top(6)).build()));
        try (XWPFDocument document = export(content)) {
            assertThat(before(anywhere(document, "Caption"))).isZero();
        }
        assertThat(DocxExports.report(400, 600, 20, content).bySubject().get("overlapping layer")).isNotNull();
    }

    @Test
    void aGapWiderThanTheMarginIsTheGap() throws Exception {
        // Set 30pt down its stack with a 5pt margin, the caption starts 22pt under the line above.
        try (XWPFDocument document = export(band(stack -> stack.position(new ParagraphBuilder().text("Caption")
                .margin(DocumentInsets.top(5)).build(), 0, 30, LayerAlign.TOP_LEFT)))) {
            assertThat(before(anywhere(document, "Caption"))).isBetween(441 - RAISE, 441L);
        }
    }

    @Test
    void aCaptionWithAnEdgeBelowZeroOverAPictureGivesNothing() throws Exception {
        // An edge below zero is not written in a resumed layer: the caption stands right under the
        // picture, as one with no margin does, and not its 4pt lower.
        try (XWPFDocument document = export(page -> page.addParagraph("Above").addLayerStack(stack -> stack
                .topLeft(new ImageBuilder().source(picture(200, 100)).size(120, 60).build())
                .bottomRight(new ParagraphBuilder().text("Caption").margin(new DocumentInsets(-4, 0, 0, 0)).build())))) {
            assertThat(before(anywhere(document, "Caption"))).isZero();
        }
    }

    @Test
    void aBlockInARowKeepsTheGapAndIsNotNamedForIt() throws Exception {
        // The caption's margin is written in the row's cell, not where the layer resumes: the layer
        // keeps the gap less the margin, at zero, and nothing is named as pulled over the line above.
        Consumer<PageFlowBuilder> content = band(new com.demcha.compose.document.dsl.RowBuilder()
                .addParagraph(p -> p.text("Caption").margin(DocumentInsets.top(20))).addParagraph("Beside").build());
        assertThat(DocxExports.report(400, 600, 20, content).bySubject().get("space above")).isNull();
    }

    @Test
    void aSpacerOpeningALaterLayerKeepsItsHeight() throws Exception {
        // A spacer writes neither its margin nor its padding, so it has no edge to give back: the
        // layer resumes at the gap less its margin, at zero, as before, the margin named with the
        // spacer, and the spacer's 8pt stands whole above the caption.
        Consumer<PageFlowBuilder> content = band(new SectionBuilder()
                .add(new com.demcha.compose.document.node.SpacerNode("Gap", 0, 8, DocumentInsets.zero(),
                        DocumentInsets.top(20)))
                .addParagraph("Caption").build());
        try (XWPFDocument document = export(content)) {
            assertThat(before(anywhere(document, "Caption"))).as("the spacer's 8pt, less its hairline")
                    .isBetween(8 * 20 - 2 - RAISE, 8 * 20 - 2L);
        }
        assertThat(DocxExports.report(400, 600, 20, content).bySubject().get("space above")).isNull();
    }

    /** A band under a line: "Opening" at its top, then the given layer at its foot. */
    private static Consumer<PageFlowBuilder> band(DocumentNode later) {
        return band(stack -> stack.bottomLeft(later));
    }

    private static Consumer<PageFlowBuilder> band(UnaryOperator<LayerStackBuilder> later) {
        return page -> page.addParagraph("Above").addLayerStack(stack -> later.apply(stack
                .topLeft(new ParagraphBuilder().text("Opening").build())));
    }

    private static XWPFDocument export(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.withLayout(400, 600, 20, content);
    }

    /** The paragraph reading {@code text}, in the body or in a table's cell. */
    private static XWPFParagraph anywhere(XWPFDocument document, String text) {
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            if (paragraph.getText().equals(text)) {
                return paragraph;
            }
        }
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        if (paragraph.getText().equals(text)) {
                            return paragraph;
                        }
                    }
                }
            }
        }
        throw new AssertionError("no paragraph reads " + text);
    }

    private static XWPFParagraph pictureIn(XWPFDocument document) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getRuns().stream().anyMatch(run -> !run.getEmbeddedPictures().isEmpty()))
                .findFirst().orElseThrow();
    }

    private static long before(XWPFParagraph paragraph) {
        CTSpacing spacing = spacing(paragraph);
        return spacing != null && spacing.isSetBefore() ? DocxTwips.of(spacing.getBefore()) : 0;
    }

    private static long after(XWPFParagraph paragraph) {
        CTSpacing spacing = spacing(paragraph);
        return spacing != null && spacing.isSetAfter() ? DocxTwips.of(spacing.getAfter()) : 0;
    }

    private static CTSpacing spacing(XWPFParagraph paragraph) {
        return paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr().getSpacing() : null;
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
