package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.CanvasLayerBuilder;
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
        assertThat(gapInWord(drawing.document(), "Above", "Caption")).as("the canvas only draws, the caption under it")
                .isCloseTo(gapOnThePage(drawing.layout(), "Above", "Caption"), within(0.1))
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

        assertThat(before(paragraphOf(exported.document(), "Title"))).as("the page's distance down to it, once")
                .isCloseTo(band.placementY() + band.placementHeight() - (title.placementY() + title.placementHeight()),
                        within(0.1))
                .isCloseTo(placed(exported.layout(), "Logo").placementHeight(), within(0.1));
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
                .isCloseTo(gapOnThePage(exported.layout(), "Title", "Subtitle"), within(0.1))
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
                .isCloseTo(gapOnThePage(exported.layout(), "Caption", "Subtitle"), within(0.1))
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

            assertThat(before(caption) + lineOf(caption) + gapInWord(exported.document(), "Caption", "Below"))
                    .as("the outer canvas's height, its caption written at its corner")
                    .isCloseTo(placed(exported.layout(), "Outer").placementHeight() - RAISE, within(0.1));
            assertThat(detailOf(exported.report())).isEqualTo("written as its contents; what it writes is written "
                    + "from its corner, one block after another, not where it places it");
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
        XWPFTableCell marker = exported.document().getTables().get(0).getRow(0).getCell(0);

        assertThat(marker.getParagraphs()).hasSize(1);
        assertThat(before(marker.getParagraphs().get(0))).as("the marker's room")
                .isCloseTo(placed(exported.layout(), "marker").placementHeight(), within(0.1));
        assertThat(exported.report().bySubject()).doesNotContainKey("CanvasLayerNode");
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
        return all.stream().filter(paragraph -> paragraph.getText().equals(text)).findFirst()
                .orElseThrow(() -> new AssertionError("no paragraph reading " + text));
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
