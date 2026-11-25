package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.CanvasLayerBuilder;
import com.demcha.compose.document.dsl.PageBreakBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A canvas holds its height in Word as it does on the page.
 *
 * <p>The page gives a canvas its height whatever it holds, and Word writes what it holds one block
 * after another: the room under what it writes was lost, so what followed it stood that much
 * higher — in the flow, after a canvas that only draws, at the end of a row's cell it made the
 * tallest, at the end of a layer stack's column, and in a shape container's layer. It is the
 * space below what it writes now, and the report names only what Word cannot hold: what it writes
 * running past its height, a drawing in what it writes that takes no room, and a height nothing
 * measures. A canvas that only draws holds none of its own where what is round it measures that
 * room already — before the first block a band's layer writes, in drawings held whole, as a
 * layer laid over another, or in a block a canvas round it counts as taking no room — so the
 * room is not counted twice.</p>
 *
 * <p>A canvas whose blocks read down sets each as far below the one before as it places it, and
 * owes the room it leaves under the last: written one block under another from its corner, those
 * distances were lost, and a canvas pulled up by its margin owed that margin's depth too much
 * room.</p>
 *
 * <p>Each expected distance is the page's, read from the layout's placements, or the difference
 * two layouts make; each Word distance is read from the file — the space above and below each
 * paragraph, and its exact line.</p>
 */
class DocxCanvasRoomTest {

    private static final DocumentColor INK = DocumentColor.rgb(26, 86, 148);
    /**
     * What a line of the default text in the flow is raised into the space above it, in points
     * ({@link DocxExports#DEFAULT_LINE_RAISE}): the gap Word writes above it is that much less
     * than the page's.
     */
    private static final double RAISE = DocxExports.DEFAULT_LINE_RAISE / 20.0;

    @Test
    void whatFollowsACanvasStartsBelowItsRoomNotBelowWhatItWrites() throws Exception {
        Exported exported = export(page -> page
                .addCanvas(360, 120, canvas -> canvas.position(paragraph("Caption"), 0, 0))
                .add(paragraph("Below")));

        assertThat(gapInWord(exported.document(), "Caption", "Below"))
                .as("the canvas's room under its caption")
                .isCloseTo(gapOnThePage(exported.layout(), "Caption", "Below") - RAISE, within(0.1))
                .isGreaterThan(80);
        assertThat(exported.report().bySubject()).doesNotContainKey("CanvasLayerNode");
    }

    @Test
    void aCanvasWithPaddingAndMarginHoldsItsWholeBox() throws Exception {
        Exported exported = export(page -> page
                .add(paragraph("Above"))
                .addCanvas(360, 90, canvas -> canvas
                        .padding(new DocumentInsets(6, 0, 10, 0))
                        .margin(new DocumentInsets(4, 0, 8, 0))
                        .position(paragraph("Caption"), 0, 0))
                .add(paragraph("Below")));

        assertThat(gapInWord(exported.document(), "Above", "Caption")).as("its top margin and padding")
                .isCloseTo(gapOnThePage(exported.layout(), "Above", "Caption"), within(0.1));
        assertThat(gapInWord(exported.document(), "Caption", "Below")).as("its room, padding and margin below")
                .isCloseTo(gapOnThePage(exported.layout(), "Caption", "Below") - RAISE, within(0.1));
    }

    @Test
    void aCanvasWhoseBlocksReadDownSetsEachAsFarBelowTheOneBeforeAsItPlacesIt() throws Exception {
        // A name over a title, set apart down the canvas: each stands as far below the one before
        // as the canvas places it — the first as far below its corner — and the room under the
        // last is what the canvas leaves below its foot. The name's bottom margin is part of its
        // foot: the title is placed below it.
        Exported exported = export(page -> page
                .add(paragraph("Above"))
                .addCanvas(360, 120, canvas -> canvas
                        .position(new ParagraphBuilder().name("Name").text("Name")
                                .margin(new DocumentInsets(0, 0, 6, 0)).build(), 0, 20)
                        .position(paragraph("Title"), 0, 50))
                .add(paragraph("Below")));
        XWPFDocument document = exported.document();
        LayoutGraph layout = exported.layout();

        assertThat(gapInWord(document, "Above", "Name")).as("the name as far down its canvas as it places it")
                .isCloseTo(gapOnThePage(layout, "Above", "Name"), within(0.1)).isGreaterThan(19);
        assertThat(gapInWord(document, "Name", "Title")).as("the title as far below the name")
                .isCloseTo(gapOnThePage(layout, "Name", "Title"), within(0.1)).isGreaterThan(6);
        assertThat(gapInWord(document, "Title", "Below")).as("the room under the title")
                .isCloseTo(gapOnThePage(layout, "Title", "Below") - RAISE, within(0.1));
        assertThat(exported.report().bySubject()).doesNotContainKey("CanvasLayerNode");
    }

    @Test
    void aCanvasPulledUpByItsMarginOwesItsRoomFromWhereWordSetsItsFirstBlock() throws Exception {
        // MintEditorial's banded masthead: a canvas pulled up to the page's top edge by a margin as
        // deep as the page's, its name that far down it. Word starts the name at the page's
        // margin, which is where the canvas places it; measured from the canvas's corner above
        // that, the room under the title was the margin's depth too large.
        Exported exported = export(page -> page
                .addCanvas(360, 120, canvas -> canvas
                        .margin(new DocumentInsets(-20, 0, 0, 0))
                        .position(paragraph("Name"), 0, 20)
                        .position(paragraph("Title"), 0, 50))
                .add(paragraph("Below")));
        XWPFDocument document = exported.document();
        XWPFParagraph name = paragraphOf(document, "Name");
        XWPFParagraph title = paragraphOf(document, "Title");

        assertThat(before(name)).as("the name at the page's margin").isZero();
        assertThat(gapInWord(document, "Name", "Title"))
                .isCloseTo(gapOnThePage(exported.layout(), "Name", "Title"), within(0.1));
        assertThat(lineOf(name) + gapInWord(document, "Name", "Title") + lineOf(title)
                   + gapInWord(document, "Title", "Below"))
                .as("from the page's margin to the next block: the canvas's height less the margin it is pulled up by")
                .isCloseTo(120 - 20 - RAISE, within(0.1));

        // Under a paragraph, the pull and the name's place cancel there too.
        Exported underAParagraph = export(page -> page
                .add(paragraph("Above"))
                .addCanvas(360, 120, canvas -> canvas
                        .margin(new DocumentInsets(-12, 0, 0, 0))
                        .position(paragraph("Name"), 0, 20)
                        .position(paragraph("Title"), 0, 50))
                .add(paragraph("Below")));
        assertThat(gapInWord(underAParagraph.document(), "Above", "Name"))
                .isCloseTo(gapOnThePage(underAParagraph.layout(), "Above", "Name"), within(0.1)).isGreaterThan(7);
    }

    @Test
    void aCanvasPlacingItsFirstBlockAboveItsCornerOwesItsRoomFromWhereWordSetsIt() throws Exception {
        // Its name 10pt above its corner, where Word cannot set it, and its title under the name:
        // Word sets the name at the corner and the title as far below the corner as it places it,
        // so the canvas holds its height, and the place it loses is named.
        Exported exported = export(page -> page
                .addCanvas(360, 100, canvas -> canvas
                        .position(paragraph("Name"), 0, -10)
                        .position(paragraph("Title"), 0, 30))
                .add(paragraph("Below")));
        XWPFDocument document = exported.document();
        XWPFParagraph name = paragraphOf(document, "Name");
        XWPFParagraph title = paragraphOf(document, "Title");

        assertThat(before(name)).as("the name at its corner").isZero();
        assertThat(lineOf(name) + gapInWord(document, "Name", "Title")).as("the title 30pt below its corner")
                .isCloseTo(30, within(0.1));
        assertThat(lineOf(name) + gapInWord(document, "Name", "Title") + lineOf(title)
                   + gapInWord(document, "Title", "Below"))
                .as("the canvas's height").isCloseTo(100 - RAISE, within(0.1));
        assertThat(detailOf(exported.report())).isEqualTo("written as its contents; a block it places higher than the "
                + "foot of the one before, or than its corner, is written at that foot or corner");
    }

    @Test
    void aCanvasTheLayoutMovesToANewPageHoldsItsFirstBlocksSpaceInTheLineThere() throws Exception {
        // Word drops a paragraph's space above at the top of a page: the line holding the canvas's
        // top edge there holds its name's 40pt below its corner too.
        Exported exported = export(page -> page
                .add(new com.demcha.compose.document.dsl.SpacerBuilder().height(500).build())
                .addCanvas(360, 120, canvas -> canvas
                        .position(paragraph("Name"), 0, 40)
                        .position(paragraph("Title"), 0, 70))
                .add(paragraph("Below")));
        XWPFDocument document = exported.document();
        XWPFParagraph name = paragraphOf(document, "Name");
        List<IBodyElement> body = document.getBodyElements();
        XWPFParagraph line = (XWPFParagraph) body.get(body.indexOf(name) - 1);

        assertThat(placed(exported.layout(), "Name").startPage()).as("moved to the second page").isEqualTo(1);
        assertThat(before(name)).as("nothing above the name for Word to drop").isZero();
        assertThat(lineOf(line)).as("the line opening the page holds the name's place")
                .isCloseTo(600 - 20 - top(exported.layout(), "Name"), within(0.1)).isGreaterThan(39);
    }

    @Test
    void aCanvasPulledUpByItsMarginOverATableStartsTheTableWhereItPlacesIt() throws Exception {
        // Word has no space above a table, and none below zero anywhere: the margin's pull comes
        // out of the space above the table rather than being dropped beside it.
        Exported exported = export(page -> page
                .addCanvas(360, 120, canvas -> canvas
                        .margin(new DocumentInsets(-20, 0, 0, 0))
                        .position(new com.demcha.compose.document.dsl.TableBuilder().name("Grid")
                                .columns(DocumentTableColumn.fixed(200))
                                .rowCells(com.demcha.compose.document.table.DocumentTableCell.node(paragraph("Cell")))
                                .build(), 0, 20)
                        .position(paragraph("Title"), 0, 60))
                .add(paragraph("Below")));
        XWPFDocument document = exported.document();
        List<IBodyElement> body = document.getBodyElements();
        double above = 0;
        for (IBodyElement element : body.subList(0, body.indexOf(document.getTables().get(0)))) {
            XWPFParagraph paragraph = (XWPFParagraph) element;
            above += before(paragraph) + after(paragraph) + (spacingOf(paragraph) != null
                    && spacingOf(paragraph).getLineRule() == STLineSpacingRule.EXACT ? lineOf(paragraph) : 0);
        }

        assertThat(above).as("the table at the page's margin, where the canvas places it").isLessThan(0.5);
        assertThat(exported.report().bySubject()).doesNotContainKey("CanvasLayerNode");
    }

    @Test
    void aCanvasPulledUpByItsMarginOpeningANewPageNamesTheSpaceWordDropsThere() throws Exception {
        // Its name 20pt below the page's margin on the page the layout moves it to: the line that
        // holds a canvas's top edge there holds nothing above the margin, and the space above the
        // name, at the page's top, Word drops.
        Exported exported = export(page -> page
                .add(new com.demcha.compose.document.dsl.SpacerBuilder().height(500).build())
                .addCanvas(360, 120, canvas -> canvas
                        .margin(new DocumentInsets(-20, 0, 0, 0))
                        .position(paragraph("Name"), 0, 40)
                        .position(paragraph("Title"), 0, 70))
                .add(paragraph("Below")));

        assertThat(placed(exported.layout(), "Name").startPage()).as("moved to the second page").isEqualTo(1);
        // The spacer's room above it and the name's 20pt are both owed there, and both dropped.
        assertThat(before(paragraphOf(exported.document(), "Name"))).as("written above the name, at the page's top")
                .isCloseTo(500 + 20, within(0.5));
        assertThat(detailOf(exported.report())).isEqualTo("written as its contents; on the page it opens, the space "
                + "it places above its first block stands at the top of the page, where Word drops it");
    }

    @Test
    void aTableOpeningANewPageInACanvasStandsUnderTheLineHoldingItsSpace() throws Exception {
        // The line holding the canvas's top edge on the page it opens holds the table's 20pt too,
        // and the table owes none of it again: Word has no space above a table, and paid below the
        // line it stood that much low.
        Exported exported = export(page -> page
                .add(new com.demcha.compose.document.dsl.SpacerBuilder().height(500).build())
                .addCanvas(360, 120, canvas -> canvas
                        .position(grid(), 0, 20)
                        .position(paragraph("Title"), 0, 60))
                .add(paragraph("Below")));
        XWPFDocument document = exported.document();
        List<IBodyElement> body = document.getBodyElements();
        XWPFParagraph line = (XWPFParagraph) body.get(body.indexOf(document.getTables().get(0)) - 1);

        assertThat(placed(exported.layout(), "Grid").startPage()).as("moved to the second page").isEqualTo(1);
        assertThat(lineOf(line)).as("the line opening the page holds the table's place")
                .isCloseTo(600 - 20 - top(exported.layout(), "Grid"), within(0.1)).isGreaterThan(19);
        // The spacer's room is the line's space above, which Word drops at the page's top.
        assertThat(after(line)).as("nothing more between the line and the table").isZero();
    }

    @Test
    void aPulledUpCanvasOpeningANewPageWithATableIsNotNamedForTheSpaceItsTableHolds() throws Exception {
        // A table the layout starts on a new page holds the space above it in a line of its own.
        Exported exported = export(page -> page
                .add(new com.demcha.compose.document.dsl.SpacerBuilder().height(500).build())
                .addCanvas(360, 120, canvas -> canvas
                        .margin(new DocumentInsets(-20, 0, 0, 0))
                        .position(grid(), 0, 40)
                        .position(paragraph("Title"), 0, 80))
                .add(paragraph("Below")));

        assertThat(placed(exported.layout(), "Grid").startPage()).as("moved to the second page").isEqualTo(1);
        assertThat(exported.report().bySubject()).doesNotContainKey("CanvasLayerNode");
    }

    @Test
    void aPullDeeperThanTheSpaceAboveAFirstTableIsNamed() throws Exception {
        // Pulled up 30pt, its table placed 10pt below its corner: 20pt above where the flow sets
        // the canvas, which Word, having no space below zero above a table, does not do.
        Exported exported = export(page -> page
                .add(paragraph("Above"))
                .addCanvas(360, 120, canvas -> canvas
                        .margin(new DocumentInsets(-30, 0, 0, 0))
                        .position(grid(), 0, 10)
                        .position(paragraph("Title"), 0, 60))
                .add(paragraph("Below")));

        assertThat(detailOf(exported.report())).isEqualTo("written as its contents; its negative top margin pulls its "
                + "first block, which is not a paragraph, further up than it places it below its corner, and Word "
                + "sets that block as much lower");
    }

    @Test
    void aCanvasWhoseBlocksReadDownInARowsCellSetsEachAsFarDownAsItPlacesIt() throws Exception {
        Exported exported = export(page -> page
                .addRow(row -> row.columns(DocumentRowColumn.fixed(200), DocumentRowColumn.weight(1))
                        .addSection("Card", cell -> cell.add(new CanvasLayerBuilder(180, 100)
                                .position(paragraph("Name"), 0, 10)
                                .position(paragraph("Title"), 0, 50).build()))
                        .add(paragraph("Beside"))));
        XWPFDocument document = exported.document();

        assertThat(gapInWord(document, "Name", "Title")).as("the title as far below the name as it places it")
                .isCloseTo(gapOnThePage(exported.layout(), "Name", "Title"), within(0.1)).isGreaterThan(20);
        assertThat(before(paragraphOf(document, "Name"))).as("the name 10pt down its canvas").isCloseTo(10, within(0.1));
    }

    @Test
    void aCanvasWhoseBlocksDoNotReadDownStillWritesThemFromItsCorner() throws Exception {
        // Side by side, and holding a link no text box carries (DocxCanvasOverItsPageTest): written
        // as its contents, one block under another from its corner, and its room under the last
        // measured from there.
        Exported exported = export(page -> page
                .addCanvas(300, 120, canvas -> canvas
                        .position(paragraph("West"), 0, 10)
                        .position(new ParagraphBuilder().name("Site").text("Site").margin(DocumentInsets.zero())
                                .linkTarget(com.demcha.compose.document.node.DocumentLinkTarget.external(
                                        "https://example.org")).build(), 150, 12))
                .add(paragraph("Below")));
        XWPFDocument document = exported.document();
        LayoutGraph layout = exported.layout();
        double stacked = placed(layout, "West").placementHeight() + placed(layout, "Site").placementHeight();

        assertThat(before(paragraphOf(document, "West"))).as("at its corner, not 10pt down").isZero();
        assertThat(gapInWord(document, "West", "Site")).as("one under the other").isZero();
        assertThat(gapInWord(document, "Site", "Below")).as("its room under the stack from its corner")
                .isCloseTo(120 - stacked - RAISE, within(0.1));
        assertThat(detailOf(exported.report())).contains("what it writes is written from its corner, one block after "
                + "another, not where it places it");
    }

    @Test
    void aCanvasThatOnlyDrawsHoldsItsWholeRoom() throws Exception {
        Exported exported = export(page -> page
                .add(paragraph("Above"))
                .addCanvas(100, 80, canvas -> canvas.position(new ShapeBuilder().size(40, 40).fillColor(INK).build(),
                        20, 20))
                .add(paragraph("Below")));

        assertThat(gapInWord(exported.document(), "Above", "Below"))
                .isCloseTo(gapOnThePage(exported.layout(), "Above", "Below") - RAISE, within(0.1))
                .isCloseTo(80 - RAISE, within(0.1));
        assertThat(exported.report().bySubject()).doesNotContainKey("CanvasLayerNode");
    }

    @Test
    void aCanvasWrittenOverItsPageHoldsItsWholeRoom() throws Exception {
        // Its text set apart stands in text boxes (DocxCanvasOverItsPageTest), and none of it in
        // the flow, which holds its box as for a canvas that only draws.
        Exported exported = export(page -> page
                .add(paragraph("Above"))
                .addCanvas(300, 90, canvas -> canvas
                        .margin(new DocumentInsets(4, 0, 8, 0))
                        .position(paragraph("West"), 0, 10)
                        .position(paragraph("East"), 150, 18))
                .add(paragraph("Below")));

        assertThat(gapInWord(exported.document(), "Above", "Below"))
                .isCloseTo(gapOnThePage(exported.layout(), "Above", "Below") - RAISE, within(0.1))
                .isCloseTo(4 + 90 + 8 - RAISE, within(0.1));
    }

    @Test
    void aCanvasThatOnlyDrawsHoldsItsBoxOnceWhatItDrawsInHasEdgesToo() throws Exception {
        // A marker's recipe is a section of its own; its edges are inside the canvas's box.
        Exported exported = export(page -> page
                .add(paragraph("Above"))
                .addCanvas(100, 80, canvas -> canvas.position(new SectionBuilder()
                        .padding(new DocumentInsets(4, 0, 10, 0))
                        .add(new ShapeBuilder().size(40, 40).fillColor(INK).build()).build(), 0, 0))
                .add(paragraph("Below")));

        assertThat(gapInWord(exported.document(), "Above", "Below"))
                .isCloseTo(gapOnThePage(exported.layout(), "Above", "Below") - RAISE, within(0.1));
    }

    @Test
    void aCanvasInAStackOfOneLayerHoldsItsRoomInTheFlow() throws Exception {
        // A stack of one layer lays nothing over anything: a canvas in its layer stands in a flow.
        Exported writing = export(page -> page
                .addLayerStack(stack -> stack.layer(new CanvasLayerBuilder(360, 80)
                        .position(paragraph("Caption"), 0, 0).build(), LayerAlign.TOP_LEFT))
                .add(paragraph("Below")));
        Exported drawing = export(page -> page
                .add(paragraph("Above"))
                .addLayerStack(stack -> stack.layer(new SectionBuilder()
                        .add(new CanvasLayerBuilder(360, 80)
                                .position(new ShapeBuilder().size(40, 40).fillColor(INK).build(), 0, 0).build())
                        .add(paragraph("Caption")).build(), LayerAlign.TOP_LEFT)));

        assertThat(gapInWord(writing.document(), "Caption", "Below"))
                .isCloseTo(gapOnThePage(writing.layout(), "Caption", "Below") - RAISE, within(0.1))
                .isGreaterThan(40);
        assertThat(gapInWord(drawing.document(), "Above", "Caption"))
                .as("the canvas only draws, the caption under it raised into the space above it, as in the flow")
                .isCloseTo(gapOnThePage(drawing.layout(), "Above", "Caption") - RAISE, within(0.1))
                .isGreaterThan(40);
    }

    @Test
    void aCanvasBeforeABreakInAStackOfOneLayerHoldsItsRoom() throws Exception {
        // The page breaks no page in a stack: the break is a box of no height, and what follows it
        // follows the canvas on the same page.
        Exported exported = export(page -> page
                .addLayerStack(stack -> stack.layer(new SectionBuilder()
                        .add(new CanvasLayerBuilder(360, 80).position(paragraph("Caption"), 0, 0).build())
                        .add(new PageBreakBuilder().build())
                        .add(paragraph("Below")).build(), LayerAlign.TOP_LEFT)));

        assertThat(gapInWord(exported.document(), "Caption", "Below"))
                .isCloseTo(gapOnThePage(exported.layout(), "Caption", "Below") - RAISE, within(0.1))
                .isGreaterThan(40);
    }

    @Test
    void aCanvasItsRowsTallestCellHoldsTheRowAsTallAsItIs() throws Exception {
        // The row is as tall as the canvas on the page; its cell writes the caption alone.
        Exported exported = export(page -> page
                .addRow("Row", row -> row.columns(DocumentRowColumn.fixed(120), DocumentRowColumn.weight(1))
                        .addSection("Marker", cell -> cell
                                .add(new CanvasLayerBuilder(120, 90).position(paragraph("Caption"), 0, 0).build()))
                        .add(paragraph("Beside")))
                .add(paragraph("Below")));
        XWPFTableCell cell = exported.document().getTables().get(0).getRow(0).getCell(0);

        assertThat(heightInWord(cell)).as("the cell as tall as the row the page sets")
                .isCloseTo(placed(exported.layout(), "Row").placementHeight(), within(0.1));
        assertThat(exported.report().bySubject()).doesNotContainKey("CanvasLayerNode");
    }

    @Test
    void aCanvasEndingALayerStacksColumnHoldsTheColumnAsTallAsItIs() throws Exception {
        Exported exported = export(600, page -> page.addLayerStack(stack -> stack
                .layer(new SectionBuilder().name("Left").margin(new DocumentInsets(0, 300, 0, 0))
                        .add(paragraph("Heading"))
                        .add(new CanvasLayerBuilder(200, 100).position(paragraph("Caption"), 0, 0).build())
                        .build(), LayerAlign.TOP_LEFT)
                .layer(new SectionBuilder().margin(new DocumentInsets(0, 0, 0, 280))
                        .add(paragraph("Right")).build(), LayerAlign.TOP_LEFT))
                .add(paragraph("Below")));
        XWPFTableCell left = cellHolding(exported.document(), "Caption");

        assertThat(heightInWord(left)).as("the column as long as the layer the page sets")
                .isCloseTo(placed(exported.layout(), "Left").placementHeight(), within(0.1));
    }

    @Test
    void aCanvasFollowedInsideAShapeContainersLayerHoldsItsRoomThere() throws Exception {
        Exported exported = export(page -> page
                .add(new ShapeContainerBuilder().name("Card").rectangle(300, 200)
                        .layer(new SectionBuilder()
                                .add(new CanvasLayerBuilder(200, 70).position(paragraph("Caption"), 0, 0).build())
                                .add(paragraph("Below"))
                                .build(), LayerAlign.TOP_LEFT)
                        .build()));

        assertThat(gapInWord(exported.document(), "Caption", "Below"))
                .isCloseTo(gapOnThePage(exported.layout(), "Caption", "Below"), within(0.1))
                .isGreaterThan(40);
    }

    @Test
    void aCanvasThatOnlyDrawsEndingAShapeContainersLayerHoldsItsRoomThere() throws Exception {
        // The container measures the space under its layer from the layer's foot, which is the
        // canvas's: the canvas holds its room above that space.
        Exported exported = export(page -> page
                .add(new ShapeContainerBuilder().name("Card").rectangle(300, 200)
                        .layer(new SectionBuilder()
                                .add(paragraph("Title"))
                                .add(new CanvasLayerBuilder(200, 100)
                                        .position(new ShapeBuilder().size(40, 40).fillColor(INK).build(), 0, 0)
                                        .build())
                                .build(), LayerAlign.TOP_LEFT)
                        .build())
                .add(paragraph("Below")));

        assertThat(gapInWord(exported.document(), "Title", "Below"))
                .isCloseTo(gapOnThePage(exported.layout(), "Title", "Below") - RAISE, within(0.1))
                .isGreaterThan(100);
    }

    @Test
    void aCanvasThatOnlyDrawsInABandHoldsNoRoomOfItsOwn() throws Exception {
        // A band measures the space above what it writes on the page, past the canvas's drawing:
        // held again, the title would stand the canvas's height low.
        Exported exported = export(page -> page
                .addLayerStack(stack -> stack.name("Band")
                        .layer(new ShapeBuilder().size(360, 80).fillColor(DocumentColor.rgb(238, 243, 249)).build(),
                                LayerAlign.TOP_LEFT)
                        .layer(new SectionBuilder()
                                .add(drawingCanvas("Logo", 60, 24))
                                .add(paragraph("Title"))
                                .build(), LayerAlign.TOP_LEFT))
                .add(paragraph("Next")));
        PlacedNode band = placed(exported.layout(), "Band");
        PlacedNode title = placed(exported.layout(), "Title");

        assertThat(before(paragraphOf(exported.document(), "Title"))).as("the page's distance down to it, once,"
                                                                        + " less its line's raise into it")
                .isCloseTo(band.placementY() + band.placementHeight() - (title.placementY() + title.placementHeight())
                           - RAISE, within(0.1))
                .isCloseTo(placed(exported.layout(), "Logo").placementHeight() - RAISE, within(0.1));
    }

    @Test
    void aCanvasThatOnlyDrawsBetweenABandLayersBlocksHoldsItsRoom() throws Exception {
        // The band measures where its layer's first block stands, and how far its last stands
        // from its foot; between them, its layer is a flow.
        Exported exported = export(page -> page
                .addLayerStack(stack -> stack
                        .layer(new ShapeBuilder().size(360, 120).fillColor(DocumentColor.rgb(238, 243, 249)).build(),
                                LayerAlign.TOP_LEFT)
                        .layer(new SectionBuilder()
                                .add(paragraph("Title"))
                                .add(drawingCanvas("Logo", 60, 24))
                                .add(paragraph("Subtitle"))
                                .build(), LayerAlign.TOP_LEFT))
                .add(paragraph("Next")));

        assertThat(gapInWord(exported.document(), "Title", "Subtitle"))
                .isCloseTo(gapOnThePage(exported.layout(), "Title", "Subtitle") - RAISE, within(0.1))
                .isGreaterThan(20);
    }

    @Test
    void aCanvasWritingInABandLayerHoldsItsRoomBeforeWhatFollowsThere() throws Exception {
        Exported exported = export(page -> page
                .addLayerStack(stack -> stack
                        .layer(new ShapeBuilder().size(360, 140).fillColor(DocumentColor.rgb(238, 243, 249)).build(),
                                LayerAlign.TOP_LEFT)
                        .layer(new SectionBuilder()
                                .add(new CanvasLayerBuilder(360, 60).position(paragraph("Caption"), 0, 0).build())
                                .add(paragraph("Subtitle"))
                                .build(), LayerAlign.TOP_LEFT))
                .add(paragraph("Next")));

        assertThat(gapInWord(exported.document(), "Caption", "Subtitle"))
                .isCloseTo(gapOnThePage(exported.layout(), "Caption", "Subtitle") - RAISE, within(0.1))
                .isGreaterThan(40);
    }

    @Test
    void aCanvasThatOnlyDrawsOpeningALaterLayerOfAColumnHoldsItsRoom() throws Exception {
        // A later layer of a column resumes the page's distance from the content above it down to
        // its own first leaf, the canvas's drawing: unlike a band's, it leaves the canvas's room
        // to the canvas.
        Exported exported = export(600, page -> page.addLayerStack(stack -> stack
                .layer(new SectionBuilder().margin(new DocumentInsets(0, 300, 0, 0))
                        .add(paragraph("Heading")).build(), LayerAlign.TOP_LEFT)
                .layer(new SectionBuilder().margin(new DocumentInsets(0, 300, 0, 0))
                        .padding(new DocumentInsets(30, 0, 0, 0))
                        .add(drawingCanvas("Logo", 60, 24))
                        .add(paragraph("Caption")).build(), LayerAlign.TOP_LEFT)
                .layer(new SectionBuilder().margin(new DocumentInsets(0, 0, 0, 280))
                        .add(paragraph("Right")).build(), LayerAlign.TOP_LEFT))
                .add(paragraph("Below")));

        assertThat(gapInWord(exported.document(), "Heading", "Caption"))
                .isCloseTo(gapOnThePage(exported.layout(), "Heading", "Caption") - RAISE, within(0.1))
                .isGreaterThan(40);
    }

    @Test
    void aCanvasEndingAPaintedPanelHoldsItsRoomInThePanel() throws Exception {
        Exported exported = export(page -> page
                .addSection("Panel", panel -> panel.fillColor(DocumentColor.rgb(238, 243, 249))
                        .add(new CanvasLayerBuilder(360, 80).position(paragraph("Caption"), 0, 0).build()))
                .add(paragraph("Below")));

        assertThat(heightInWord(cellHolding(exported.document(), "Caption"))).as("the panel as tall as the page's")
                .isCloseTo(placed(exported.layout(), "Panel").placementHeight(), within(0.1));
    }

    @Test
    void aCanvasThatOnlyDrawsInsideAnotherCanvasHoldsNoRoomOfItsOwn() throws Exception {
        // The outer canvas holds its whole room under what it writes. What it lays at its corner,
        // a seal or a block holding one, takes no room among what it writes, and holds none of its
        // own — its edges included — or the outer canvas would stand that much taller.
        for (DocumentNode corner : List.of(
                drawingCanvas("Seal", 60, 60),
                new CanvasLayerBuilder(60, 60).name("Seal").padding(DocumentInsets.of(6))
                        .position(new ShapeBuilder().size(48, 48).fillColor(INK).build(), 0, 0).build(),
                new SectionBuilder().add(drawingCanvas("Seal", 60, 60)).build())) {
            Exported exported = export(page -> page
                    .add(new CanvasLayerBuilder(360, 200).name("Outer")
                            .position(corner, 0, 0)
                            .position(paragraph("Caption"), 0, 80).build())
                    .add(paragraph("Below")));
            XWPFParagraph caption = paragraphOf(exported.document(), "Caption");

            assertThat(before(caption)).as("its caption as far down as it places it").isCloseTo(80, within(0.1));
            assertThat(before(caption) + lineOf(caption) + gapInWord(exported.document(), "Caption", "Below"))
                    .as("the outer canvas's height")
                    .isCloseTo(placed(exported.layout(), "Outer").placementHeight() - RAISE, within(0.1));
            assertThat(exported.report().bySubject()).as("its caption stands where it places it")
                    .doesNotContainKey("CanvasLayerNode");
        }
    }

    @Test
    void aCanvasThatOnlyDrawsLaidOverAnotherLayerHoldsNoRoomOfItsOwn() throws Exception {
        // A layer of a shape container stands over its other layers on the page: its room is the
        // container's, and what the container writes stands where it does without the canvas.
        Function<Boolean, Consumer<PageFlowBuilder>> card = withTheCanvas -> page -> {
            ShapeContainerBuilder container = new ShapeContainerBuilder().name("Card").rectangle(300, 120);
            if (withTheCanvas) {
                container.layer(drawingCanvas("Seal", 60, 60), LayerAlign.TOP_LEFT);
            }
            page.add(paragraph("Above"))
                    .add(container.layer(paragraph("One"), LayerAlign.TOP_LEFT)
                            .layer(paragraph("Two"), LayerAlign.BOTTOM_LEFT).build())
                    .add(paragraph("Below"));
        };
        Exported with = export(card.apply(true));
        Exported without = export(card.apply(false));

        assertThat(gapInWord(with.document(), "Above", "One"))
                .isCloseTo(gapInWord(without.document(), "Above", "One"), within(0.01));
        assertThat(gapInWord(with.document(), "Two", "Below"))
                .isCloseTo(gapInWord(without.document(), "Two", "Below"), within(0.01));
    }

    @Test
    void aCanvasThatOnlyDrawsInsideWhatIsHeldWholeHoldsNoRoomOfItsOwn() throws Exception {
        // Inside a canvas held whole, and inside a stack of drawings held whole, the room is held
        // once, by the outer one.
        Exported inACanvas = export(page -> page
                .add(paragraph("Above"))
                .addCanvas(100, 80, canvas -> canvas.position(new SectionBuilder()
                        .add(drawingCanvas("Inner", 40, 40))
                        .add(new ShapeBuilder().size(40, 20).fillColor(INK).build()).build(), 0, 0))
                .add(paragraph("Below")));
        Exported inAStack = export(page -> page
                .add(paragraph("Above"))
                .addLayerStack(stack -> stack
                        .layer(new ShapeBuilder().size(100, 80).fillColor(INK).build(), LayerAlign.TOP_LEFT)
                        .layer(new SectionBuilder()
                                .add(drawingCanvas("Inner", 40, 40))
                                .add(new ShapeBuilder().size(40, 20).fillColor(INK).build()).build(),
                                LayerAlign.TOP_LEFT))
                .add(paragraph("Below")));

        assertThat(gapInWord(inACanvas.document(), "Above", "Below")).as("the outer canvas's room")
                .isCloseTo(gapOnThePage(inACanvas.layout(), "Above", "Below") - RAISE, within(0.1));
        assertThat(gapInWord(inAStack.document(), "Above", "Below")).as("the stack's room")
                .isCloseTo(gapOnThePage(inAStack.layout(), "Above", "Below") - RAISE, within(0.1));
    }

    @Test
    void aCanvasEndingATimelineEntrysBodyHoldsItsRoomAboveTheNextEntry() throws Exception {
        // On the rail, an entry's body is laid out under its row, in the content column; a canvas
        // ending it holds its room above the next entry. Two canvases 60pt apart move the next
        // entry 60pt on the page, and the space under the caption as much in Word.
        Exported tall = export(timelineEndingABodyWithACanvas(80));
        Exported short_ = export(timelineEndingABodyWithACanvas(20));
        double onThePage = top(short_.layout(), "Engineer") - top(tall.layout(), "Engineer");

        assertThat(after(paragraphOf(tall.document(), "Caption")) - after(paragraphOf(short_.document(), "Caption")))
                .isCloseTo(onThePage, within(0.1))
                .isCloseTo(60, within(0.1));
    }

    private static Consumer<PageFlowBuilder> timelineEndingABodyWithACanvas(double height) {
        return page -> page.addTimeline(timeline -> timeline.markerOnRail()
                .entry(com.demcha.compose.document.dsl.TimelineMarker.dot(8, INK), entry -> entry
                        .title("Senior Engineer")
                        .add(body -> body.add(new CanvasLayerBuilder(300, height)
                                .position(paragraph("Caption"), 0, 0).build())))
                .entry(com.demcha.compose.document.dsl.TimelineMarker.dot(8, INK), entry -> entry
                        .content(content -> content.add(paragraph("Engineer")))));
    }

    @Test
    void aCanvasBeforeAPageBreakOwesNothingThere() throws Exception {
        // The page ends there; space written below the paragraph above would stay on that page.
        Exported drawing = export(page -> page
                .add(paragraph("Above"))
                .addCanvas(100, 80, canvas -> canvas.position(new ShapeBuilder().size(40, 40).fillColor(INK).build(),
                        20, 20))
                .addPageBreak(pageBreak -> { })
                .add(paragraph("Next page")));
        Exported writing = export(page -> page
                .add(paragraph("Above"))
                .addCanvas(360, 80, canvas -> canvas.position(paragraph("Caption"), 0, 0))
                .addPageBreak(pageBreak -> { })
                .add(paragraph("Next page")));

        assertThat(after(paragraphOf(drawing.document(), "Above"))).isZero();
        assertThat(before(paragraphOf(drawing.document(), "Next page"))).isZero();
        assertThat(after(paragraphOf(writing.document(), "Caption"))).isZero();
        assertThat(before(paragraphOf(writing.document(), "Next page"))).isZero();
    }

    @Test
    void whatACanvasWritesPastItsHeightIsNamed() throws Exception {
        // A line of text in a canvas 10pt tall: the page draws its foot over what follows, and
        // Word makes room for all of it.
        Exported exported = export(page -> page
                .addCanvas(360, 10, canvas -> canvas.position(paragraph("Caption"), 0, 0))
                .add(paragraph("Below")));

        assertThat(detailOf(exported.report())).isEqualTo("written as its contents; what it writes, one block "
                + "under another, runs past its height, which Word makes room for and the page does not");
        assertThat(gapInWord(exported.document(), "Caption", "Below")).as("no room owed under it").isZero();
    }

    @Test
    void aDrawingInWhatACanvasWritesIsNamedForTheRoomWordGivesItNone() throws Exception {
        // Inside a canvas a shape is drawn where the page puts it, and holds no room: the label
        // under it stands its height high — in a section, and in a stack of one layer, which lays
        // nothing over anything.
        DocumentNode inASection = new SectionBuilder()
                .add(new ShapeBuilder().size(300, 40).fillColor(INK).build())
                .add(paragraph("Label")).build();
        DocumentNode inAStackOfOneLayer = new com.demcha.compose.document.dsl.LayerStackBuilder()
                .layer(inASection, LayerAlign.TOP_LEFT).build();
        for (DocumentNode block : List.of(inASection, inAStackOfOneLayer)) {
            Exported exported = export(page -> page
                    .addCanvas(360, 100, canvas -> canvas.position(block, 0, 0))
                    .add(paragraph("Below")));

            assertThat(detailOf(exported.report())).isEqualTo("written as its contents; a drawing in what it writes "
                    + "takes no room in Word, so what stands below the drawing stands higher by its room");
        }
    }

    @Test
    void aCanvasComposedInATableCellNamesTheHeightNothingMeasures() throws Exception {
        // In a composed cell nothing has a placement: what it writes cannot be measured.
        Exported exported = export(page -> page
                .addTable(table -> table.columns(DocumentTableColumn.fixed(200))
                        .rowCells(com.demcha.compose.document.table.DocumentTableCell.node(new SectionBuilder()
                                .add(new CanvasLayerBuilder(180, 40).position(paragraph("Caption"), 0, 0).build())
                                .add(paragraph("After")).build()))));

        // Narrower than its cell, its width is named too.
        assertThat(detailOf(exported.report())).isEqualTo("written as its contents; its height is not measured, "
                + "so it holds only the room of what it writes; its width is not in the file, so its paragraphs "
                + "and lists run the width of the column it stands in");
    }

    @Test
    void aCanvasWithNoLayoutNamesTheHeightNothingMeasures() throws Exception {
        DocxExportReport writing = DocxExports.reportWithoutLayout(400, 600, 20, page -> page
                .addCanvas(360, 120, canvas -> canvas.position(paragraph("Caption"), 0, 0))
                .add(paragraph("Below")));
        DocxExportReport drawingFollowed = DocxExports.reportWithoutLayout(400, 600, 20, page -> page
                .addCanvas(100, 80, canvas -> canvas.position(new ShapeBuilder().size(40, 40).fillColor(INK).build(),
                        20, 20))
                .add(paragraph("Below")));
        String unmeasured = "written as its contents; its height is not measured, so it holds only the room of "
                            + "what it writes";

        assertThat(writing.bySubject().get("CanvasLayerNode")).extracting(DocxExportReport.Note::detail)
                .containsExactly(unmeasured);
        assertThat(drawingFollowed.bySubject().get("CanvasLayerNode")).extracting(DocxExportReport.Note::detail)
                .as("only drawing, but something follows it to move").containsExactly(unmeasured);
    }

    @Test
    void aCanvasThatOnlyDrawsEndingARowsCellWithNoLayoutNamesTheHeightNothingMeasures() throws Exception {
        // With no layout, nothing holds the row to the page's height.
        DocxExportReport report = DocxExports.reportWithoutLayout(400, 600, 20, page -> page
                .addRow(row -> row.columns(DocumentRowColumn.fixed(120), DocumentRowColumn.weight(1))
                        .addSection("Marker", cell -> cell.add(drawingCanvas("Seal", 40, 40)))
                        .add(paragraph("Beside"))));

        assertThat(report.bySubject().get("CanvasLayerNode")).extracting(DocxExportReport.Note::detail)
                .containsExactly("written as its contents; its height is not measured, so it holds only the room "
                                 + "of what it writes");
    }

    @Test
    void aCanvasThatOnlyDrawsAndIsFollowedByNothingWithNoLayoutIsNotNamed() throws Exception {
        DocxExportReport report = DocxExports.reportWithoutLayout(400, 600, 20, page -> page
                .add(paragraph("Above"))
                .addCanvas(100, 80, canvas -> canvas.position(new ShapeBuilder().size(40, 40).fillColor(INK).build(),
                        20, 20)));

        assertThat(report.bySubject()).doesNotContainKey("CanvasLayerNode");
    }

    @Test
    void aTimelinesMarkerAloneInItsCellHoldsItsRoomThere() throws Exception {
        // Each marker is a canvas that only draws, alone in its row's cell: the cell holds its room
        // above a hairline paragraph.
        Exported exported = export(page -> page
                .addTimeline(timeline -> timeline
                        .entry(com.demcha.compose.document.dsl.TimelineMarker.dot(8, INK), entry -> entry
                                .title("Senior Engineer").meta("2023 - Present")))
                .add(paragraph("Below")));
        // The rail, beside the markers, has a column of its own in the gutter first (DocxTimelineRails).
        XWPFTableCell marker = exported.document().getTables().get(0).getRow(0).getCell(1);

        assertThat(marker.getParagraphs()).hasSize(1);
        assertThat(before(marker.getParagraphs().get(0))).as("the marker's room")
                .isCloseTo(placed(exported.layout(), "marker").placementHeight(), within(0.1));
        assertThat(exported.report().bySubject()).doesNotContainKey("CanvasLayerNode");
    }

    /** A table of one cell, named Grid. */
    private static DocumentNode grid() {
        return new com.demcha.compose.document.dsl.TableBuilder().name("Grid").columns(DocumentTableColumn.fixed(200))
                .rowCells(com.demcha.compose.document.table.DocumentTableCell.node(paragraph("Cell"))).build();
    }

    private static DocumentNode paragraph(String text) {
        return new ParagraphBuilder().name(text).text(text).margin(DocumentInsets.zero()).build();
    }

    /** A canvas that only draws: a square as large as it, at its corner. */
    private static DocumentNode drawingCanvas(String name, double width, double height) {
        return new CanvasLayerBuilder(width, height).name(name)
                .position(new ShapeBuilder().size(width, height).fillColor(INK).build(), 0, 0).build();
    }

    /** Where the page sets a placed block's top, measured up from the page's foot. */
    private static double top(LayoutGraph layout, String name) {
        PlacedNode placed = placed(layout, name);
        return placed.placementY() + placed.placementHeight();
    }

    private static String detailOf(DocxExportReport report) {
        List<DocxExportReport.Note> notes = report.bySubject().get("CanvasLayerNode");
        assertThat(notes).as("the note on the canvas").isNotNull().hasSize(1);
        return notes.get(0).detail();
    }

    /**
     * The space Word puts between two paragraphs, one written right after the other: below the
     * first and above the second.
     */
    private static double gapInWord(XWPFDocument document, String above, String below) {
        XWPFParagraph first = paragraphOf(document, above);
        XWPFParagraph second = paragraphOf(document, below);
        List<IBodyElement> elements = first.getBody().getBodyElements();
        assertThat(second.getBody()).as("%s and %s in one body", above, below).isSameAs(first.getBody());
        assertThat(elements.indexOf(second)).as("%s written right after %s", below, above)
                .isEqualTo(elements.indexOf(first) + 1);
        return after(first) + before(second);
    }

    /** How far apart the page sets two placed blocks: the first's foot to the second's top. */
    private static double gapOnThePage(LayoutGraph layout, String above, String below) {
        PlacedNode first = placed(layout, above);
        PlacedNode second = placed(layout, below);
        return first.placementY() - (second.placementY() + second.placementHeight());
    }

    /** How tall Word sets a cell's paragraphs, each an exact line with the space round it. */
    private static double heightInWord(XWPFTableCell cell) {
        double height = 0;
        for (XWPFParagraph paragraph : cell.getParagraphs()) {
            height += before(paragraph) + lineOf(paragraph) + after(paragraph);
        }
        return height;
    }

    private static PlacedNode placed(LayoutGraph layout, String name) {
        return layout.nodes().stream().filter(node -> name.equals(node.semanticName())).findFirst()
                .orElseThrow(() -> new AssertionError("no placed node named " + name));
    }

    private static double before(XWPFParagraph paragraph) {
        CTSpacing spacing = spacingOf(paragraph);
        return spacing == null || !spacing.isSetBefore() ? 0 : DocxTwips.of(spacing.getBefore()) / 20.0;
    }

    private static double after(XWPFParagraph paragraph) {
        CTSpacing spacing = spacingOf(paragraph);
        return spacing == null || !spacing.isSetAfter() ? 0 : DocxTwips.of(spacing.getAfter()) / 20.0;
    }

    /** A paragraph's exact line, in points. */
    private static double lineOf(XWPFParagraph paragraph) {
        CTSpacing spacing = spacingOf(paragraph);
        assertThat(spacing != null && spacing.getLineRule() == STLineSpacingRule.EXACT)
                .as("%s written in an exact line", paragraph.getText()).isTrue();
        return DocxTwips.of(spacing.getLine()) / 20.0;
    }

    private static CTSpacing spacingOf(XWPFParagraph paragraph) {
        return paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetSpacing()
                ? paragraph.getCTP().getPPr().getSpacing() : null;
    }

    private static XWPFParagraph paragraphOf(XWPFDocument document, String text) {
        List<XWPFParagraph> all = new ArrayList<>(document.getParagraphs());
        for (XWPFTable table : document.getTables()) {
            collect(table, all);
        }
        // Its own runs: a text box anchored in it is read by getText too.
        return all.stream().filter(paragraph -> paragraph.getText().equals(text) || ownText(paragraph).equals(text))
                .findFirst().orElseThrow(() -> new AssertionError("no paragraph reading " + text));
    }

    /** The text of a paragraph's own runs, not of the text boxes anchored in it. */
    private static String ownText(XWPFParagraph paragraph) {
        StringBuilder text = new StringBuilder();
        for (var run : paragraph.getCTP().getRList()) {
            for (var part : run.getTList()) {
                text.append(part.getStringValue());
            }
        }
        return text.toString();
    }

    private static void collect(XWPFTable table, List<XWPFParagraph> all) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                all.addAll(cell.getParagraphs());
                for (XWPFTable nested : cell.getTables()) {
                    collect(nested, all);
                }
            }
        }
    }

    private static XWPFTableCell cellHolding(XWPFDocument document, String text) {
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    if (cell.getParagraphs().stream().anyMatch(paragraph -> paragraph.getText().equals(text))) {
                        return cell;
                    }
                }
            }
        }
        throw new AssertionError("no cell holding " + text);
    }

    private record Exported(XWPFDocument document, LayoutGraph layout, DocxExportReport report) {
    }

    private static Exported export(Consumer<PageFlowBuilder> content) throws Exception {
        return export(400, content);
    }

    private static Exported export(double pageWidth, Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document()
                .pageSize(pageWidth, 600)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(content::accept);
            LayoutGraph layout = session.layoutGraph();
            byte[] docx = session.export(new DocxSemanticBackend(report::set));
            return new Exported(new XWPFDocument(new ByteArrayInputStream(docx)), layout, report.get());
        }
    }
}
