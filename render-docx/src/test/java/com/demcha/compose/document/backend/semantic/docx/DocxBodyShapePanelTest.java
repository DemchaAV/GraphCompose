package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.style.DocumentTransform;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHeightRule;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * A painted rectangle or rounded rectangle holding text, placed in the body, is written as a
 * panel: a table of one cell, its outline's size, its fill and border on the cell, its layers
 * inside it where the page sets them.
 *
 * <p>Drawn at its size with its layers written in the flow, its text took the flow's width and
 * ran past the outline, the block under it stood over the outline, and the outline kept its
 * height when text was added to it in an editor.</p>
 */
class DocxBodyShapePanelTest {

    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final DocumentColor INK = DocumentColor.rgb(26, 86, 148);
    private static final DocumentStroke BORDER = DocumentStroke.of(INK, 1);
    private static final String NOTE = "A note centred in a box narrower than the page: the page wraps it over "
                                       + "three lines inside the box, each line held within its outline.";
    private static final String LONG = "Clipped text in a rounded box too short to hold it. The page sets it from "
                                       + "the top of the box and cuts it at the outline, so the lines that run past "
                                       + "the foot of the box are not seen. Only the lines the box holds are read, "
                                       + "and the box stays as tall as it was made, whatever it is given to hold.";

    @Test
    void aPaintedRoundedBoxHoldingTextInTheBodyIsWrittenAsAPanelItsOutlinesSize() throws Exception {
        Exported exported = export(page -> page
                .addParagraph("Above")
                .add(narrowBox().build())
                .addParagraph("Below"));

        XWPFTable panel = onlyTable(exported.document());
        XWPFTableCell cell = panel.getRow(0).getCell(0);
        assertThat(cell.getText()).contains("A note centred");
        assertThat(cell.getColor()).isEqualToIgnoringCase("EEF3F9");
        // Its outline's width, half its right border past it and the editor's point of slack.
        assertThat(widthOf(cell)).isEqualTo(Math.round((220 + 0.5 + 1) * 20));
        // Its outline's height, the two borders Word adds round a least height taken off.
        assertThat(heightOf(panel.getRow(0))).isEqualTo((90 - 2) * 20);
        assertThat(ruleOf(panel.getRow(0))).isEqualTo(STHeightRule.AT_LEAST);
        assertThat(panel.getRow(0).isCantSplitRow()).as("the layout moves a shape whole").isTrue();
        assertThat(cell.getVerticalAlignment()).isEqualTo(XWPFTableCell.XWPFVertAlign.CENTER);
        // The panel paints the outline: nothing is drawn for it.
        assertThat(exported.body()).doesNotContain("<wp:anchor").doesNotContain("<wp:inline");
        assertThat(exported.report().bySubject()).doesNotContainKey("shape container");
        assertThat(detailOf(exported.report(), "corner radius")).contains("loses its rounded corners");
    }

    @Test
    void itsLayerIsHeldInAcrossWhereTheLayoutPlacesItInsideTheOutline() throws Exception {
        Exported exported = export(page -> page
                .addParagraph("Above")
                .add(narrowBox().build())
                .addParagraph("Below"));

        PlacedNode box = exported.placed("Box");
        PlacedNode note = exported.placed("Note");
        XWPFParagraph written = onlyTable(exported.document()).getRow(0).getCell(0).getParagraphs().get(0);
        // The page centres the note's block in the box; its text is set left in it.
        assertThat(note.placementX() - box.placementX()).isGreaterThan(1);
        assertThat(indentOf(written, true)).isEqualTo(Math.round((note.placementX() - box.placementX()) * 20));
        // Held from the cell's edges, not the flow's: its right side ends at the outline, the room to
        // spare for an editor's wider text taken from what is left of it there.
        assertThat(indentOf(written, false)).isZero();
    }

    @Test
    void aBlockSetAtTheOutlinesLeftKeepsTheRoomThePageLaidItOutIn() throws Exception {
        Exported exported = export(page -> page
                .addParagraph("Above")
                .add(card(ClipPolicy.CLIP_PATH).build())
                .addParagraph("Below"));

        // Its section reports the box of its longest line; held in to it, an editor setting the text
        // a little wider breaks a line a word early. Only the section's padding holds it in.
        List<XWPFParagraph> written = onlyTable(exported.document()).getRow(0).getCell(0).getParagraphs();
        assertThat(written).hasSize(2);
        for (XWPFParagraph paragraph : written) {
            assertThat(indentOf(paragraph, false)).as(paragraph.getText()).isEqualTo(12 * 20);
        }
    }

    @Test
    void aBlockOfAFixedWidthSetAtTheOutlinesLeftIsHeldToItsWidth() throws Exception {
        // The page lays the section's text out in its 160pt, not in the card's room.
        Exported exported = export(page -> page
                .addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Box").roundedRect(328, 80, 12).fillColor(SURFACE).stroke(BORDER)
                        .topLeft(new SectionBuilder().name("Column").fixedWidth(160)
                                .addParagraph(p -> p.text("A column of text held to a fixed width, narrower than "
                                                          + "the card it stands in."))
                                .build())
                        .build())
                .addParagraph("Below"));

        PlacedNode column = exported.placed("Column");
        assertThat(column.placementWidth()).isEqualTo(160, offset(0.01));
        XWPFParagraph written = onlyTable(exported.document()).getRow(0).getCell(0).getParagraphs().get(0);
        assertThat(indentOf(written, false)).isEqualTo(Math.round((328 - 160) * 20));
    }

    @Test
    void aShapeSetAtTheOutlinesLeftIsHeldToItsOwnWidth() throws Exception {
        // A pill at the card's top left: its label is centred in the pill, not in the card.
        Exported exported = export(page -> page
                .addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Box").roundedRect(328, 80, 12).fillColor(SURFACE).stroke(BORDER)
                        .topLeft(new ShapeContainerBuilder().name("Pill").roundedRect(140, 20, 10)
                                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                                .center(new ParagraphBuilder().text("Most popular plan").build())
                                .build())
                        .build())
                .addParagraph("Below"));

        XWPFParagraph label = onlyTable(exported.document()).getRow(0).getCell(0).getParagraphs().get(0);
        assertThat(label.getText()).isEqualTo("Most popular plan");
        // Held in to the pill's 140pt, the label's right edge stands well short of the card's.
        assertThat(indentOf(label, false)).isGreaterThan(Math.round((328 - 140 - 10) * 20));
    }

    @Test
    void aLaterLayerOfAShapeSetFromItsTopIsNamedAgainstTheTop() throws Exception {
        // The first layer, centred but taller than the outline, is set from its top, and the cell
        // with it; a second layer the page centres is named as set at the middle, its cell's at the top.
        Exported exported = export(page -> page
                .addParagraph("Above")
                .add(clippedBox(ClipPolicy.CLIP_PATH)
                        .layer(new ParagraphBuilder().name("Second").text("A second layer").build(), LayerAlign.CENTER)
                        .build())
                .addParagraph("Below"));

        assertThat(exported.report().notes()).filteredOn(note -> note.subject().equals("layer offset"))
                .extracting(DocxExportReport.Note::detail)
                .contains("written as a panel's content: the page sets it at the middle of the shape, and its cell "
                          + "sets all it holds at the top, it under the layers before it");
    }

    @Test
    void aShapeSetFromItsTopTakesItsTopBorderOutOfTheSpaceAboveItsFirstLine() throws Exception {
        Exported card = export(page -> page
                .addParagraph("Above")
                .add(card(ClipPolicy.CLIP_PATH).build())
                .addParagraph("Below"));
        Exported centred = export(page -> page
                .addParagraph("Above")
                .add(narrowBox().build())
                .addParagraph("Below"));

        // The page sets the card's heading 12pt under the outline's top; Word sets a cell's content
        // under the whole of its 1pt top border.
        XWPFParagraph heading = onlyTable(card.document()).getRow(0).getCell(0).getParagraphs().get(0);
        assertThat(spacingBefore(heading)).isEqualTo((12 - 1) * 20);
        assertThat(card.report().bySubject()).doesNotContainKey("ShapeContainerNode");
        // A layer the cell centres stands between both borders, as the page centres it.
        XWPFParagraph note = onlyTable(centred.document()).getRow(0).getCell(0).getParagraphs().get(0);
        assertThat(spacingBefore(note)).isZero();
    }

    @Test
    void aPaddedShapeIsReadFromItsOutlineNotItsPaddedBox() throws Exception {
        // Its padding stands round the outline: the card's heading is still set from the outline's top.
        Exported card = export(page -> page
                .addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Box").roundedRect(300, 80, 12).fillColor(SURFACE).stroke(BORDER)
                        .padding(DocumentInsets.of(10))
                        .topLeft(new SectionBuilder().padding(DocumentInsets.of(12)).spacing(4)
                                .addParagraph(p -> p.text("A padded card"))
                                .addParagraph(p -> p.text("Its text is short."))
                                .build())
                        .build())
                .addParagraph("Below"));
        // Its text runs past the outline's foot by less than the padding under it: the clip still cuts it.
        Exported clipped = export(page -> page
                .addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Box").rectangle(200, 47).fillColor(SURFACE).stroke(BORDER)
                        .padding(DocumentInsets.of(10)).clipPolicy(ClipPolicy.CLIP_BOUNDS)
                        .topLeft(new ParagraphBuilder().name("Note")
                                .text("Three short lines of text, a little taller than the box they stand in, so "
                                      + "that the clip cuts the foot of the third.")
                                .build())
                        .build())
                .addParagraph("Below"));

        XWPFParagraph heading = onlyTable(card.document()).getRow(0).getCell(0).getParagraphs().get(0);
        assertThat(spacingBefore(heading)).isEqualTo((12 - 1) * 20);
        PlacedNode box = clipped.placed("Box");
        PlacedNode note = clipped.placed("Note");
        double past = box.placementY() + 10 - note.placementY();
        assertThat(past).as("how far the text runs past the outline's foot").isBetween(1.0, 9.0);
        assertThat(ruleOf(onlyTable(clipped.document()).getRow(0))).isEqualTo(STHeightRule.EXACT);
    }

    @Test
    void aClipThatCutsItsTextHoldsThePanelExactlyAtItsOutline() throws Exception {
        Exported exported = export(page -> page
                .addParagraph("Above")
                .add(clippedBox(ClipPolicy.CLIP_PATH).build())
                .addParagraph("Below"));

        XWPFTableRow row = onlyTable(exported.document()).getRow(0);
        // Word and LibreOffice add one border to an exact height: the panel stands its outline's 60pt.
        assertThat(ruleOf(row)).isEqualTo(STHeightRule.EXACT);
        assertThat(heightOf(row)).isEqualTo((60 - 1) * 20);
        assertThat(detailOf(exported.report(), "clipped shape container"))
                .isEqualTo("its clip is written as its panel's exact height, its outline's: Word cuts what its cell "
                           + "holds past the foot of the panel, as the page cuts it at the outline, but square, not "
                           + "round its rounded corners, and text added to it in an editor is hidden too, the panel no "
                           + "taller");
        // Its text is set from the outline's top, with nothing above its first line to take the border;
        // the cell sets it from its top as the page does, though the layer is centred.
        assertThat(detailOf(exported.report(), "ShapeContainerNode"))
                .isEqualTo("its content stands 1pt lower than the page sets it: Word sets it below the whole of its "
                           + "top border, where the page sets its layers from its outline's top, and nothing above "
                           + "its first line takes the border");
        assertThat(row.getCell(0).getVerticalAlignment()).isNull();
        // The paragraph under it is measured against lines written inside the box, as the page sets them.
        assertThat(exported.report().bySubject()).doesNotContainKey("space above");
    }

    @Test
    void aSquareBoxsClipIsNamedWithoutRoundCorners() throws Exception {
        Exported exported = export(page -> page
                .addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Box").rectangle(200, 60).fillColor(SURFACE).stroke(BORDER)
                        .clipPolicy(ClipPolicy.CLIP_BOUNDS).center(new ParagraphBuilder().text(LONG).build()).build())
                .addParagraph("Below"));

        assertThat(ruleOf(onlyTable(exported.document()).getRow(0))).isEqualTo(STHeightRule.EXACT);
        assertThat(detailOf(exported.report(), "clipped shape container"))
                .isEqualTo("its clip is written as its panel's exact height, its outline's: Word cuts what its cell "
                           + "holds past the foot of the panel, as the page cuts it at the outline, and text added to "
                           + "it in an editor is hidden too, the panel no taller");
        assertThat(exported.report().bySubject()).doesNotContainKey("corner radius");
    }

    @Test
    void aClipThatCutsOnlyAboveTheFootLetsThePanelGrowAndKeepsItsNote() throws Exception {
        // A line moved up past the outline's top: the page cuts its top, and nothing runs past the foot.
        Exported exported = export(page -> page
                .addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Box").roundedRect(200, 60, 12).fillColor(SURFACE).stroke(BORDER)
                        .clipPolicy(ClipPolicy.CLIP_PATH)
                        .position(new ParagraphBuilder().text("Moved up past the top").build(), 10, -8,
                                LayerAlign.TOP_LEFT)
                        .build())
                .addParagraph("Below"));

        assertThat(ruleOf(onlyTable(exported.document()).getRow(0))).isEqualTo(STHeightRule.AT_LEAST);
        assertThat(detailOf(exported.report(), "clipped shape container"))
                .isEqualTo("its clip is not in the file, so what its layers paint past its outline is written whole");
    }

    @Test
    void aBoxSettingItsOneWrittenLayerBesideDrawingStaysABand() throws Exception {
        // A section heading beside its badge: the band writes the title on the badge's line.
        Exported exported = export(page -> page
                .addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Box").roundedRect(300, 30, 6).fillColor(SURFACE)
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .centerLeft(new ShapeBuilder().size(24, 24).fillColor(INK).build())
                        .position(new ParagraphBuilder().text("EXPERIENCE").build(), 34, 0, LayerAlign.CENTER_LEFT)
                        .build())
                .addParagraph("Below"));

        assertThat(exported.document().getTables()).isEmpty();
        assertThat(exported.document().getParagraphs()).extracting(XWPFParagraph::getText).contains("EXPERIENCE");
        // Its outline is drawn, its corners round.
        assertThat(exported.body()).contains("<wp:anchor");
        assertThat(exported.report().bySubject()).doesNotContainKey("corner radius");
    }

    @Test
    void aBoxHoldingNoTextInsideAPanelOrARowsCellOrGivenNoRoomIsNotAPanel() throws Exception {
        // A picture contained in a frame taller than it is: drawn, as before.
        Exported framed = export(page -> page.addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Box").roundedRect(60, 60, 8).fillColor(SURFACE)
                        .clipPolicy(ClipPolicy.CLIP_BOUNDS)
                        .center(new ImageBuilder().source(DocumentImageData.fromBytes(png()))
                                .size(60, 70).fitMode(DocumentImageFitMode.CONTAIN).build())
                        .build())
                .addParagraph("Below"));
        // In a painted section's cell and a row's: written in that cell, as before.
        Exported inAPanel = export(page -> page.addParagraph("Above")
                .addSection(section -> section.fillColor(DocumentColor.rgb(250, 250, 250)).add(narrowBox().build()))
                .addParagraph("Below"));
        Exported inARow = export(page -> page.addParagraph("Above")
                .addRow(row -> row.add(narrowBox().build()).add(new ParagraphBuilder().text("Beside").build()))
                .addParagraph("Below"));
        // Its margins take back its height, its text linked, so the page lays it over the flow and
        // gives it no room: not a panel, which would take its outline's.
        Exported noRoom = export(page -> page.addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Box").roundedRect(220, 40, 10).fillColor(SURFACE)
                        .margin(new DocumentInsets(0, 0, -40, 0))
                        .center(new ParagraphBuilder().text("Linked").linkTo("target").build()).build())
                .addParagraph(p -> p.text("Below").anchor("target")));

        assertThat(framed.document().getTables()).isEmpty();
        assertThat(onlyTable(inAPanel.document()).getRow(0).getCell(0).getTables()).isEmpty();
        assertThat(onlyTable(inARow.document()).getRow(0).getCell(0).getTables()).isEmpty();
        assertThat(noRoom.document().getTables()).isEmpty();
    }

    @Test
    void aBoxWithNoClipOrNothingToCutGrowsWithItsText() throws Exception {
        Exported card = export(page -> page
                .addParagraph("Above")
                .add(card(ClipPolicy.CLIP_PATH).build())
                .addParagraph("Below"));
        Exported overflowing = export(page -> page
                .addParagraph("Above")
                .add(clippedBox(ClipPolicy.OVERFLOW_VISIBLE).build())
                .addParagraph("Below"));

        assertThat(ruleOf(onlyTable(card.document()).getRow(0))).isEqualTo(STHeightRule.AT_LEAST);
        assertThat(card.report().bySubject()).doesNotContainKeys("clipped shape container", "shape container");
        // Unclipped, its text runs past the outline over what follows on the page; the panel holds it.
        assertThat(ruleOf(onlyTable(overflowing.document()).getRow(0))).isEqualTo(STHeightRule.AT_LEAST);
        assertThat(overflowing.report().bySubject()).doesNotContainKey("clipped shape container");
        assertThat(detailOf(overflowing.report(), "shape container"))
                .matches("its layers run [0-9.]+pt past its outline's foot: written as a panel, it grows to hold "
                         + "them, and what follows stands up to that much lower");
    }

    @Test
    void aCardItsTextFillsGrowsWithItsTextAsOneWithRoomToSpare() throws Exception {
        // Its default clip has nothing to cut, though its text fills it. Were its row held exactly at
        // its outline's height, every line in Word would stand where it stood, and in LibreOffice 24.2
        // the paragraph under it would stand nearer the page; the card would not grow when text is
        // added to it. The fidelity gates, which measure lines, would not see that.
        // The card just tall enough to hold its text, as the page lays it out.
        double outline = export(page -> page.add(pageWideCard(200).build())).placed("Text").placementHeight() + 0.25;
        Exported exported = export(page -> page
                .addParagraph("Above")
                .add(pageWideCard(outline).build())
                .addParagraph("Below"));

        PlacedNode box = exported.placed("Box");
        PlacedNode text = exported.placed("Text");
        // The page leaves less than half a point between the foot of its text's padding and the outline's.
        assertThat(text.placementY() - box.placementY()).isBetween(0.0, 0.5);
        XWPFTableRow row = onlyTable(exported.document()).getRow(0);
        assertThat(ruleOf(row)).isEqualTo(STHeightRule.AT_LEAST);
        assertThat(heightOf(row)).isEqualTo(Math.round(outline * 20) - 2 * 20);
        assertThat(exported.report().bySubject()).doesNotContainKeys("clipped shape container", "shape container");
    }

    @Test
    void itsPaddingStandsRoundTheOutline() throws Exception {
        Exported exported = export(page -> page
                .addParagraph("Above")
                .add(narrowBox().padding(DocumentInsets.of(10)).build())
                .addParagraph("Below"));

        XWPFTable panel = onlyTable(exported.document());
        // The page pads round the outline: the table is the outline's size, set in by the padding.
        assertThat(widthOf(panel.getRow(0).getCell(0))).isEqualTo(Math.round((220 + 0.5 + 1) * 20));
        assertThat(heightOf(panel.getRow(0))).isEqualTo((90 - 2) * 20);
        assertThat(twipsOf(panel.getCTTbl().getTblPr().getTblInd().getW())).isEqualTo(10 * 20);
        // Its layer is held in from the outline, inside the padding, not from the padded box.
        PlacedNode box = exported.placed("Box");
        PlacedNode note = exported.placed("Note");
        XWPFParagraph written = panel.getRow(0).getCell(0).getParagraphs().get(0);
        assertThat(indentOf(written, true)).isEqualTo(Math.round((note.placementX() - box.placementX() - 10) * 20));
    }

    @Test
    void aTurnedBoxNamesTheTransformItsPanelDoesNotCarry() throws Exception {
        Exported exported = export(page -> page
                .addParagraph("Above")
                .add(narrowBox().transform(DocumentTransform.rotate(10)).build())
                .addParagraph("Below"));

        assertThat(detailOf(exported.report(), "shape container"))
                .isEqualTo("written as a panel, which carries no transform, so it stands upright at its size");
    }

    @Test
    void anEllipseAnUnpaintedBoxATileAndABoxLaidOverAnotherAreNotPanels() throws Exception {
        Exported ellipse = export(page -> page.addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Box").ellipse(220, 90).fillColor(SURFACE)
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE).center(note()).build())
                .addParagraph("Below"));
        Exported unpainted = export(page -> page.addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Box").roundedRect(220, 90, 10)
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE).center(note()).build())
                .addParagraph("Below"));
        Exported tile = export(page -> page.addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Box").roundedRect(40, 40, 6).fillColor(SURFACE)
                        .center(new ShapeBuilder().size(20, 20).fillColor(INK).build()).build())
                .addParagraph("Below"));
        Exported stacked = export(page -> page.addParagraph("Above")
                .addLayerStack(stack -> stack.layer(narrowBox().build(), LayerAlign.TOP_LEFT, 0)
                        .layer(new ParagraphBuilder().text("Over").build(), LayerAlign.TOP_RIGHT, 1))
                .addParagraph("Below"));

        assertThat(ellipse.document().getTables()).isEmpty();
        assertThat(ellipse.report().bySubject()).containsKey("shape container");
        assertThat(unpainted.document().getTables()).isEmpty();
        assertThat(tile.document().getTables()).isEmpty();
        assertThat(stacked.document().getTables()).isEmpty();
    }

    private static ShapeContainerBuilder narrowBox() {
        return new ShapeContainerBuilder().name("Box").roundedRect(220, 90, 10).fillColor(SURFACE).stroke(BORDER)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE).center(note());
    }

    private static ShapeContainerBuilder clippedBox(ClipPolicy clip) {
        return new ShapeContainerBuilder().name("Box").roundedRect(200, 60, 12).fillColor(SURFACE).stroke(BORDER)
                .clipPolicy(clip).center(new ParagraphBuilder().text(LONG).build());
    }

    private static ShapeContainerBuilder card(ClipPolicy clip) {
        return new ShapeContainerBuilder().name("Box").roundedRect(328, 80, 12).fillColor(SURFACE).stroke(BORDER)
                .clipPolicy(clip)
                // Lines far shorter than the card: the section's box is theirs, not the card's.
                .topLeft(new SectionBuilder().padding(DocumentInsets.of(12)).spacing(4)
                        .addParagraph(p -> p.text("A rounded card"))
                        .addParagraph(p -> p.text("Its text is short."))
                        .build());
    }

    /** A card as wide as the page between its margins, holding a heading and a paragraph. */
    private static ShapeContainerBuilder pageWideCard(double height) {
        return new ShapeContainerBuilder().name("Box").roundedRect(328, height, 12).fillColor(SURFACE).stroke(BORDER)
                .topLeft(new SectionBuilder().name("Text").padding(DocumentInsets.of(12)).spacing(4)
                        .addParagraph(p -> p.text("A rounded card").textStyle(DocumentTextStyle.builder().size(12).build()))
                        .addParagraph(p -> p.text("Its text runs over three lines from the card's top left corner, "
                                                  + "and the card is just tall enough to hold them with its padding "
                                                  + "round them: a line more, and the card needs to be taller.")
                                .textStyle(DocumentTextStyle.builder().size(10).build()))
                        .build());
    }

    private static DocumentNode note() {
        return new ParagraphBuilder().name("Note").text(NOTE).build();
    }

    private record Exported(XWPFDocument document, String body, DocxExportReport report, List<PlacedNode> placements) {
        PlacedNode placed(String name) {
            return placements.stream().filter(node -> name.equals(node.semanticName())).findFirst().orElseThrow();
        }
    }

    private static Exported export(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document().pageSize(400, 500).margin(DocumentInsets.of(36)).create()) {
            session.pageFlow(content::accept);
            List<PlacedNode> placements = session.layoutGraph().nodes();
            XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(
                    session.export(new DocxSemanticBackend(report::set))));
            return new Exported(document, document.getDocument().getBody().xmlText(), report.get(), placements);
        }
    }

    private static XWPFTable onlyTable(XWPFDocument document) {
        assertThat(document.getTables()).hasSize(1);
        return document.getTables().get(0);
    }

    private static String detailOf(DocxExportReport report, String subject) {
        List<DocxExportReport.Note> notes = report.bySubject().get(subject);
        assertThat(notes).as("the note on the %s", subject).isNotNull().hasSize(1);
        return notes.get(0).detail();
    }

    private static long widthOf(XWPFTableCell cell) {
        return twipsOf(cell.getCTTc().getTcPr().getTcW().getW());
    }

    private static long heightOf(XWPFTableRow row) {
        return twipsOf(row.getCtRow().getTrPr().getTrHeightArray(0).getVal());
    }

    private static STHeightRule.Enum ruleOf(XWPFTableRow row) {
        return row.getCtRow().getTrPr().getTrHeightArray(0).getHRule();
    }

    private static long indentOf(XWPFParagraph paragraph, boolean left) {
        CTPPr properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetInd()) {
            return 0;
        }
        Object twips = left ? properties.getInd().getLeft() : properties.getInd().getRight();
        return twips == null ? 0 : twipsOf(twips);
    }

    private static long spacingBefore(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        return properties == null || !properties.isSetSpacing() || properties.getSpacing().getBefore() == null
                ? 0 : twipsOf(properties.getSpacing().getBefore());
    }

    private static long twipsOf(Object value) {
        return DocxTwips.of(value);
    }

    private static byte[] png() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(100, 20, BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
