package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.CanvasLayerBuilder;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.ListBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A canvas whose blocks read down, written as its contents in the flow, sets each block as far
 * across and as wide as it places it.
 *
 * <p>Written from the canvas's left edge, each block stood as far left of where the page sets it
 * as the canvas placed it across, and its text broke at the width of the column the canvas
 * stands in rather than at the canvas's own. Each block is now held in from where the layout
 * places it to the width the canvas lays it out in — the canvas's own, its margins taken off, or
 * the width the block sets itself — where the page breaks its lines. In a table cell it is
 * written at the canvas's left edge, as before, and named.</p>
 *
 * <p>Each expected place is the page's, read from the layout's placements, or the difference two
 * layouts make; each Word place is read from the file — a paragraph's indents, a table's.</p>
 */
class DocxCanvasAcrossTest {

    private static final DocumentColor INK = DocumentColor.rgb(26, 86, 148);
    private static final double PAGE_WIDTH = 400;
    private static final double MARGIN = 20;
    private static final double COLUMN = PAGE_WIDTH - 2 * MARGIN;

    @Test
    void eachBlockStandsAsFarAcrossAsItsCanvasPlacesIt() throws Exception {
        Exported moved = export(blocksAcross(40, 50, 70, 90));
        Exported flush = export(blocksAcross(0, 0, 0, 0));

        assertThat(left(paragraphOf(moved.document(), "Name")) - left(paragraphOf(flush.document(), "Name")))
                .as("a paragraph 40pt across").isCloseTo(40, within(0.05));
        assertThat(left(paragraphOf(moved.document(), "First item")) - left(paragraphOf(flush.document(), "First item")))
                .as("a list 50pt across").isCloseTo(50, within(0.05));
        assertThat(left(pictureParagraph(moved.document())) - left(pictureParagraph(flush.document())))
                .as("a picture 70pt across").isCloseTo(70, within(0.05));
        assertThat(tableIndent(moved.document()) - tableIndent(flush.document()))
                .as("a table 90pt across").isCloseTo(90, within(0.05));
        assertThat(left(paragraphOf(moved.document(), "Name")))
                .as("where the page sets it, from the column's edge")
                .isCloseTo(placed(moved.layout(), "Name").placementX() - MARGIN, within(0.05));
        assertThat(moved.report().bySubject()).as("nothing of it lost").doesNotContainKey("CanvasLayerNode");
    }

    @Test
    void aCanvasNarrowerThanItsColumnSetsItsTextInItsOwnWidth() throws Exception {
        // Padded, and moved in by its margin: its text starts inside both, and each paragraph is
        // written in the canvas's 200pt from where the page places it, at its own alignment.
        Exported exported = export(page -> page
                .addParagraph("Above")
                .addCanvas(200, 140, canvas -> canvas
                        .padding(DocumentInsets.of(10)).margin(new DocumentInsets(0, 0, 0, 20))
                        .position(new ShapeBuilder().size(200, 140).fillColor(INK).build(), 0, 0)
                        .position(paragraph("Wraps", "At zero, a line that wraps in the narrow canvas width",
                                TextAlign.LEFT), 0, 0)
                        .position(paragraph("Centred", "Centred at thirty", TextAlign.CENTER), 30, 50)
                        .position(paragraph("Right", "Right at ten", TextAlign.RIGHT), 10, 80))
                .addParagraph("Below"));

        for (String name : List.of("Wraps", "Centred", "Right")) {
            XWPFParagraph paragraph = paragraphOf(exported.document(), textOf(name));
            double writtenLeft = MARGIN + left(paragraph);
            assertThat(writtenLeft).as("%s: from where the page places it", name)
                    .isCloseTo(placed(exported.layout(), name).placementX(), within(0.05));
            assertThat(COLUMN - left(paragraph) - right(paragraph)).as("%s: in the canvas's width", name)
                    .isCloseTo(200, within(0.05));
        }
        assertThat(exported.report().bySubject()).as("its width is in the file").doesNotContainKey("CanvasLayerNode");
    }

    @Test
    void aParagraphSetPastTheColumnsEdgeKeepsTheCanvasWidth() throws Exception {
        // Held to the box of its longest line, which ends past the column's edge, the line filling
        // it broke a word early in Word and LibreOffice.
        Exported exported = export(page -> page
                .addCanvas(300, 220, canvas -> canvas
                        .position(paragraph("Title", textOf("Title"), TextAlign.LEFT), 100, 0))
                .addParagraph("Below"));

        PlacedNode box = placed(exported.layout(), "Title");
        assertThat(box.placementX() + box.placementWidth()).as("set past the column's right edge")
                .isGreaterThan(MARGIN + COLUMN + 0.5);
        assertThat(box.placementWidth()).as("its longest line narrower than the canvas").isLessThan(299);
        XWPFParagraph title = paragraphOf(exported.document(), textOf("Title"));
        assertThat(left(title)).isCloseTo(box.placementX() - MARGIN, within(0.05));
        assertThat(COLUMN - left(title) - right(title)).as("the canvas's width, out past the column's edge")
                .isCloseTo(300, within(0.05));
    }

    @Test
    void eachBlockIsAsWideAsItsCanvasLaysItOutNotAsTheBoxItComesTo() throws Exception {
        // A section and a list come to their widest line; held to that, a line filling it broke a
        // word early. A block given a width of its own, a margin, is laid out in that.
        Exported exported = export(page -> page
                .addCanvas(300, 300, canvas -> canvas
                        .position(new SectionBuilder().name("Section").add(paragraph("Inside", textOf("Inside"),
                                TextAlign.LEFT)).build(), 50, 0)
                        .position(new ListBuilder().name("List").items(textOf("Item")).build(), 30, 70)
                        .position(new SectionBuilder().name("Fixed").fixedWidth(150).add(paragraph("InFixed",
                                textOf("InFixed"), TextAlign.LEFT)).build(), 20, 140)
                        .position(new ParagraphBuilder().name("Margined").text(textOf("Margined"))
                                .margin(new DocumentInsets(0, 30, 0, 10)).build(), 0, 220))
                .addParagraph("Below"));

        assertThat(placed(exported.layout(), "Section").placementWidth()).as("a section narrower than the canvas")
                .isLessThan(299);
        assertThat(roomOf(exported, "Inside")).as("a section's paragraph").isCloseTo(300, within(0.05));
        XWPFParagraph item = paragraphOf(exported.document(), textOf("Item"));
        assertThat(MARGIN + COLUMN - right(item)).as("a list's right edge the canvas's width from its place")
                .isCloseTo(placed(exported.layout(), "List").placementX() + 300, within(0.05));
        assertThat(roomOf(exported, "InFixed")).as("a section of its own width").isCloseTo(150, within(0.05));
        assertThat(roomOf(exported, "Margined")).as("a paragraph's margins taken off").isCloseTo(260, within(0.05));
        assertThat(left(paragraphOf(exported.document(), textOf("Margined")))).as("inside its left margin")
                .isCloseTo(10, within(0.05));
    }

    @Test
    void aCanvasInAStackOfOneLayerSetsItsBlocksAcrossToo() throws Exception {
        // A template wraps a block in a stack of one layer, which lays nothing over anything: the
        // stack holds the canvas where it sets it, and the canvas its block where it places it.
        Exported exported = export(page -> page
                .addLayerStack(stack -> stack.layer(new CanvasLayerBuilder(300, 80)
                        .position(paragraph("Caption", "Caption", TextAlign.LEFT), 10, 30)
                        .build(), com.demcha.compose.document.node.LayerAlign.TOP_LEFT))
                .addParagraph("Below"));

        assertThat(left(paragraphOf(exported.document(), "Caption"))).as("where the page sets it")
                .isCloseTo(placed(exported.layout(), "Caption").placementX() - MARGIN, within(0.05))
                .isGreaterThan(9.5);
        assertThat(exported.report().bySubject()).doesNotContainKey("CanvasLayerNode");
    }

    @Test
    void aBlockPlacedTwiceStandsAtEachPlaceItIsWritten() throws Exception {
        // The layout keeps one place of a node, its last: each copy is held at its own canvas's
        // place for it, in one canvas and across two.
        DocumentNode twice = paragraph("Twice", "Twice", TextAlign.LEFT);
        Exported exported = export(page -> page
                .addCanvas(300, 120, canvas -> canvas
                        .position(twice, 30, 0)
                        .position(twice, 60, 60))
                .addCanvas(300, 40, canvas -> canvas.position(twice, 150, 0))
                .addParagraph("Below"));

        List<XWPFParagraph> copies = exported.document().getParagraphs().stream()
                .filter(paragraph -> paragraph.getText().equals("Twice")).toList();
        assertThat(copies).as("written at each place").hasSize(3);
        assertThat(copies).extracting(DocxCanvasAcrossTest::left).as("each where its canvas places it")
                .satisfiesExactly(
                        first -> assertThat(first).isCloseTo(30, within(0.05)),
                        second -> assertThat(second).isCloseTo(60, within(0.05)),
                        third -> assertThat(third).isCloseTo(150, within(0.05)));
        assertThat(exported.report().bySubject()).doesNotContainKey("CanvasLayerNode");
    }

    @Test
    void inAPaintedPanelWhatItWritesStandsAtItsLeftEdgeAndIsNamed() throws Exception {
        // A painted panel is a table cell of its own, whose text Word keeps inside it.
        Exported exported = export(page -> page
                .add(new SectionBuilder().name("Card").fillColor(INK).add(new CanvasLayerBuilder(200, 80)
                        .position(paragraph("CardName", "Card at thirty", TextAlign.LEFT), 30, 10)
                        .position(paragraph("CardTitle", "Below at ten", TextAlign.LEFT), 10, 40)
                        .build()).build())
                .addParagraph("Below"));

        assertThat(left(paragraphOf(exported.document(), "Card at thirty"))).as("at the canvas's left edge")
                .isCloseTo(left(paragraphOf(exported.document(), "Below at ten")), within(0.05));
        List<DocxExportReport.Note> notes = exported.report().bySubject().get("CanvasLayerNode");
        assertThat(notes).as("the canvas named").isNotNull().hasSize(1);
        assertThat(notes.get(0).detail()).contains("in a table cell or a painted panel, what it writes is written at "
                                                   + "its left edge, each block as far down as it places it but not "
                                                   + "as far across");
    }

    @Test
    void inATableCellWhatItWritesStandsAtItsLeftEdgeAndIsNamed() throws Exception {
        Exported exported = export(page -> page
                .addRow("Row", row -> row
                        .add(new SectionBuilder().name("Holder").add(new CanvasLayerBuilder(150, 80)
                                .position(paragraph("CellName", "Cell at thirty", TextAlign.LEFT), 30, 10)
                                .position(paragraph("CellTitle", "Below at ten", TextAlign.LEFT), 10, 40)
                                .build()).build())
                        .addParagraph("Beside it"))
                .addParagraph("Below"));

        assertThat(left(paragraphOf(exported.document(), "Cell at thirty"))).as("at the canvas's left edge")
                .isCloseTo(left(paragraphOf(exported.document(), "Below at ten")), within(0.05));
        List<DocxExportReport.Note> notes = exported.report().bySubject().get("CanvasLayerNode");
        assertThat(notes).as("the canvas named").isNotNull().hasSize(1);
        assertThat(notes.get(0).detail()).contains("in a table cell or a painted panel, what it writes is written at "
                                                   + "its left edge, each block as far down as it places it but not "
                                                   + "as far across");
    }

    /**
     * A canvas whose blocks read down: a paragraph, a list, a picture and a table, each across.
     * Padded, so a block at its left edge is indented too, and a table written there carries an
     * indent of its own to compare with (indentTable writes none at the column's edge).
     */
    private static Consumer<PageFlowBuilder> blocksAcross(double nameX, double listX, double pictureX,
                                                          double tableX) {
        return page -> page
                .addParagraph("Above")
                .addCanvas(300, 240, canvas -> canvas
                        .padding(DocumentInsets.of(10))
                        .position(new ShapeBuilder().size(300, 240).fillColor(INK).build(), 0, 0)
                        .position(paragraph("Name", "Name", TextAlign.LEFT), nameX, 10)
                        .position(new ListBuilder().name("List").items("First item").build(), listX, 40)
                        .position(new ImageBuilder().name("Picture").source(DocumentImageData.fromBytes(png()))
                                .size(30, 30).build(), pictureX, 80)
                        .position(new TableBuilder().name("Table").columns(DocumentTableColumn.fixed(120))
                                .rowCells(DocumentTableCell.node(paragraph("Cell", "Cell", TextAlign.LEFT)))
                                .build(), tableX, 130))
                .addParagraph("Below");
    }

    private static DocumentNode paragraph(String name, String text, TextAlign align) {
        return new ParagraphBuilder().name(name).text(text).align(align).build();
    }

    private static String textOf(String name) {
        return switch (name) {
            case "Wraps" -> "At zero, a line that wraps in the narrow canvas width";
            case "Centred" -> "Centred at thirty";
            case "Right" -> "Right at ten";
            case "Title" -> "Title at a hundred, long enough to wrap across the canvas edge where the page breaks it";
            case "Inside" -> "A section paragraph long enough to wrap onto a second line of the section in it";
            case "Item" -> "A list item long enough to wrap onto a second line in the canvas it stands in";
            case "InFixed" -> "A paragraph in a section of a fixed width, long enough to wrap in it";
            case "Margined" -> "A paragraph with margins of its own, long enough to wrap inside them on the canvas";
            default -> name;
        };
    }

    /** The width a paragraph is written in, between its indents. */
    private static double roomOf(Exported exported, String name) {
        XWPFParagraph paragraph = paragraphOf(exported.document(), textOf(name));
        return COLUMN - left(paragraph) - right(paragraph);
    }

    private static double left(XWPFParagraph paragraph) {
        CTInd ind = indOf(paragraph);
        return ind == null || !ind.isSetLeft() ? 0 : DocxTwips.of(ind.getLeft()) / 20.0;
    }

    private static double right(XWPFParagraph paragraph) {
        CTInd ind = indOf(paragraph);
        return ind == null || !ind.isSetRight() ? 0 : DocxTwips.of(ind.getRight()) / 20.0;
    }

    private static CTInd indOf(XWPFParagraph paragraph) {
        return paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetInd()
                ? paragraph.getCTP().getPPr().getInd() : null;
    }

    private static double tableIndent(XWPFDocument document) {
        assertThat(document.getTables()).as("the canvas's table").hasSize(1);
        var properties = document.getTables().get(0).getCTTbl().getTblPr();
        return properties.isSetTblInd() ? DocxTwips.of(properties.getTblInd().getW()) / 20.0 : 0;
    }

    private static XWPFParagraph pictureParagraph(XWPFDocument document) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getRuns().stream().anyMatch(run -> !run.getEmbeddedPictures().isEmpty()))
                .findFirst().orElseThrow(() -> new AssertionError("no picture written in the flow"));
    }

    private static XWPFParagraph paragraphOf(XWPFDocument document, String text) {
        List<XWPFParagraph> all = new ArrayList<>(document.getParagraphs());
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    all.addAll(cell.getParagraphs());
                }
            }
        }
        return all.stream().filter(paragraph -> paragraph.getText().equals(text)).findFirst()
                .orElseThrow(() -> new AssertionError("no paragraph reading " + text));
    }

    private static PlacedNode placed(LayoutGraph layout, String name) {
        return layout.nodes().stream().filter(node -> name.equals(node.semanticName())).findFirst()
                .orElseThrow(() -> new AssertionError("no placed node named " + name));
    }

    private static byte[] png() {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(20, 20,
                    java.awt.image.BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private record Exported(XWPFDocument document, LayoutGraph layout, DocxExportReport report) {
    }

    private static Exported export(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document()
                .pageSize(PAGE_WIDTH, 600)
                .margin(DocumentInsets.of(MARGIN))
                .create()) {
            session.pageFlow(content::accept);
            LayoutGraph layout = session.layoutGraph();
            byte[] docx = session.export(new DocxSemanticBackend(report::set));
            return new Exported(new XWPFDocument(new ByteArrayInputStream(docx)), layout, report.get());
        }
    }
}
