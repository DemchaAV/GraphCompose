package com.demcha.compose.document.table;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.EllipseFragmentPayload;
import com.demcha.compose.document.layout.payloads.ImageFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.PageFieldKind;
import com.demcha.compose.document.node.PageFieldNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.testing.VisualTestOutputs;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A leaf of a size of its own composed in a table cell is laid out at the width it measured, as
 * the flow and a row slot lay one out; text in the width it aligns in.
 *
 * <p>The cell gave every leaf its inner width. One of a size of its own was stretched to it: in a
 * 120pt column padded 4pt, a picture fitted to 100pt was drawn in a box 112pt wide, 112×56 rather
 * than 100×50, and a 20pt circle as an ellipse 112pt wide. The row was sized from what the leaf
 * measured all along, so only the width was wrong. Text keeps the cell's inner width, which it
 * aligns in, and a composite is placed in the cell's inner box as before.</p>
 */
class ComposedCellLeafWidthTest {

    private static final double TOLERANCE = 0.01;

    /** The 120pt column less the default 4pt cell padding on each side. */
    private static final double INNER = 112;

    @Test
    void aFittedPictureIsDrawnInItsOwnBox() throws Exception {
        PlacedFragment picture = single(layout(new ImageBuilder().source(picture(200, 100)).fitToBounds(100, 100).build()),
                ImageFragmentPayload.class);

        assertThat(picture.width()).as("its own width, not the cell's").isCloseTo(100, within(TOLERANCE));
        assertThat(picture.height()).isCloseTo(100, within(TOLERANCE));
    }

    @Test
    void aCircleStaysACircle() {
        PlacedFragment circle = single(layout(new EllipseBuilder().circle(20).fillColor(DocumentColor.BLACK).build()),
                EllipseFragmentPayload.class);

        assertThat(circle.width()).isCloseTo(20, within(TOLERANCE));
        assertThat(circle.height()).isCloseTo(20, within(TOLERANCE));
    }

    @Test
    void aLeafStartsAtTheCellsContentEdge() {
        LayoutGraph graph = layout(new ShapeBuilder().size(30, 10).fillColor(DocumentColor.BLACK).build());
        PlacedFragment shape = single(graph, ShapeFragmentPayload.class);

        assertThat(shape.width()).isCloseTo(30, within(TOLERANCE));
        assertThat(shape.x()).as("the page margin and the cell's padding, as before")
                .isCloseTo(72 + 4, within(TOLERANCE));
    }

    @Test
    void aShapeWiderThanItsCellIsHeldToIt() {
        // Measured wider than the content box, it was laid out at the cell's width before too.
        LayoutGraph graph = layout(new ShapeBuilder().size(INNER + 20, 10).fillColor(DocumentColor.BLACK).build());

        assertThat(single(graph, ShapeFragmentPayload.class).width()).isCloseTo(INNER, within(TOLERANCE));
    }

    @Test
    void aCentredParagraphKeepsTheCellsWidth() {
        DocumentNode centred = new ParagraphBuilder().text("Centred").align(TextAlign.CENTER).build();

        assertThat(single(layout(centred), ParagraphFragmentPayload.class).width())
                .as("text that aligns in the width it is given measures that width")
                .isCloseTo(INNER, within(TOLERANCE));
    }

    @Test
    void aLeftAlignedParagraphKeepsTheCellsWidth() {
        DocumentNode left = new ParagraphBuilder().text("Left").build();

        assertThat(single(layout(left), ParagraphFragmentPayload.class).width())
                .as("text is set in the width it is given, though it measures as wide as its words")
                .isCloseTo(INNER, within(TOLERANCE));
    }

    @Test
    void aPageNumberSetRightStandsAtTheCellsRightEdge() {
        // It measures as wide as its digits, whatever its alignment.
        DocumentNode number = new PageFieldNode("Number", PageFieldKind.NUMBER, DocumentTextStyle.DEFAULT,
                TextAlign.RIGHT, DocumentInsets.zero(), DocumentInsets.zero());

        assertThat(single(layout(number), ParagraphFragmentPayload.class).width())
                .as("the width it aligns in").isCloseTo(INNER, within(TOLERANCE));
    }

    @Test
    void theCellsRenderedForInspection() throws Exception {
        try (DocumentSession session = session()) {
            session.pageFlow(page -> page
                    .addTable(table -> table
                            .columns(DocumentTableColumn.fixed(120), DocumentTableColumn.fixed(100))
                            .rowCells(DocumentTableCell.node(new ImageBuilder().source(picture(200, 100))
                                            .fitToBounds(100, 100).build()),
                                    DocumentTableCell.text("Picture"))
                            .rowCells(DocumentTableCell.node(new EllipseBuilder().circle(20)
                                            .fillColor(DocumentColor.BLACK).build()),
                                    DocumentTableCell.text("Circle"))));
            byte[] pdf = session.toPdfBytes();
            Files.write(VisualTestOutputs.preparePdf("composed-cell-leaf-width", "table"), pdf);
            assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        }
    }

    private static LayoutGraph layout(DocumentNode composed) {
        try (DocumentSession session = session()) {
            session.pageFlow(page -> page.addTable(table -> table
                    .columns(DocumentTableColumn.fixed(120), DocumentTableColumn.fixed(100))
                    .rowCells(DocumentTableCell.node(composed), DocumentTableCell.text("Beside"))));
            return session.layoutGraph();
        }
    }

    private static DocumentSession session() {
        return GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(72)).create();
    }

    private static PlacedFragment single(LayoutGraph graph, Class<?> payload) {
        List<PlacedFragment> found = fragments(graph, payload);
        assertThat(found).hasSize(1);
        return found.get(0);
    }

    private static List<PlacedFragment> fragments(LayoutGraph graph, Class<?> payload) {
        return graph.fragments().stream().filter(fragment -> payload.isInstance(fragment.payload())).toList();
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
