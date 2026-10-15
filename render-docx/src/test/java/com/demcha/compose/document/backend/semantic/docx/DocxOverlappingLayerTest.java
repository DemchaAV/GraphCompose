package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A layer the page starts above where the layers before it in its stack end is named: Word writes
 * one block under another, so what it writes in the flow is written under them. A caption set
 * over the foot of its photo came out under the photo, a label in a card's corner under the card,
 * and the report said nothing. A stack with no place of its own in the layout, composed in a
 * table cell, is named too, its layers not measured.
 */
class DocxOverlappingLayerTest {

    private static final String SUBJECT = "overlapping layer";
    private static final Pattern DETAIL = Pattern.compile("the page starts it ([0-9.]+)pt above where the layers "
            + "before it in its (stack|column) end, and Word writes one block under another: what it writes in the "
            + "flow is written under them");
    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final DocumentColor INK = DocumentColor.rgb(26, 86, 148);

    @Test
    void aCaptionSetOverTheFootOfItsPhotoIsNamedAsWrittenUnderIt() throws Exception {
        Export export = export(page -> page.addLayerStack(stack -> stack
                .layer(new ImageBuilder().name("Photo").source(png()).size(200, 100).build(), LayerAlign.TOP_LEFT)
                .layer(new ParagraphBuilder().name("Caption").text("Caption over the photo").build(),
                        LayerAlign.BOTTOM_LEFT)));

        assertThat(export.notedAbove("Caption", "stack")).as("from the caption's top down to the photo's foot")
                .isCloseTo(top(export.placed("Caption")) - export.placed("Photo").placementY(), within(0.01));
        assertThat(export.report().bySubject().get(SUBJECT)).as("the photo opens its stack").hasSize(1);
    }

    @Test
    void aLabelOverACardsCornerIsMeasuredFromWhereTheCardsPanelEnds() throws Exception {
        // The card is a panel, a table writing its own padding below its text: the layers before
        // the label end at the panel's foot, not at its last line.
        Export export = export(page -> page.addLayerStack(stack -> stack
                .layer(new SectionBuilder().name("Card").fillColor(SURFACE).padding(DocumentInsets.of(8))
                        .addParagraph("First line of the card")
                        .addParagraph("Second line of the card").build(), LayerAlign.TOP_LEFT)
                .layer(new ParagraphBuilder().name("Label").text("NEW").build(), LayerAlign.TOP_RIGHT)));

        assertThat(export.notedAbove("Label", "stack"))
                .isCloseTo(top(export.placed("Label")) - export.placed("Card").placementY(), within(0.01));
    }

    @Test
    void aTitleWhosePlaceAnEarlierLayerHoldsIsNamedAsWrittenUnderTheBody() throws Exception {
        // The place is held in the layer before the title, so the body is written first and the
        // title after it: under the body, not over its place.
        double line = lineHeight();
        Export export = export(page -> page.addLayerStack(stack -> stack
                .layer(new SectionBuilder().name("Body").addSpacer(spacer -> spacer.height(line))
                        .addParagraph(paragraph -> paragraph.name("BodyText").text("Body text")).build(),
                        LayerAlign.TOP_LEFT)
                .layer(new ParagraphBuilder().name("Title").text("Title").build(), LayerAlign.TOP_LEFT)));

        assertThat(export.notedAbove("Title", "stack"))
                .isCloseTo(top(export.placed("Title")) - export.placed("BodyText").placementY(), within(0.01));
    }

    @Test
    void aBadgeOverTheLayersBeforeItIsNamedForTheLineItsInitialsHold() throws Exception {
        // The badge and its initials are drawn where the page draws them; what the band writes in
        // the flow for it is a line as tall as the initials, under the name beside it.
        Export export = export(page -> page.addLayerStack(stack -> stack
                .layer(new SectionBuilder().name("Name")
                        .addParagraph("Jordan")
                        .addParagraph(paragraph -> paragraph.name("Surname").text("Rivera")).build(), LayerAlign.TOP_LEFT)
                .layer(new ShapeContainerBuilder().name("Badge").circle(30).fillColor(INK)
                        .center(new ParagraphBuilder().name("Initials").text("JR").build()).build(),
                        LayerAlign.TOP_RIGHT)));

        assertThat(export.notedAbove("Badge", "stack"))
                .isCloseTo(top(export.placed("Initials")) - export.placed("Surname").placementY(), within(0.01));
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(export.bytes()))) {
            assertThat(document.getParagraphs()).extracting(XWPFParagraph::getText)
                    .as("the initials are the badge's text, not a paragraph").doesNotContain("JR");
        }
    }

    @Test
    void aLayerIsMeasuredFromItsHighestBlockNotItsFirst() throws Exception {
        // The row centres its label beside a taller column: the label is its first block, and
        // starts under the intro; the column beside it starts over the intro's last two lines.
        Export export = export(page -> page.addLayerStack(stack -> stack
                .layer(new SectionBuilder().name("Intro")
                        .addParagraph("Intro, first line")
                        .addParagraph("Intro, second line")
                        .addParagraph(paragraph -> paragraph.name("IntroEnd").text("Intro, third line")).build(),
                        LayerAlign.TOP_LEFT)
                .position(new RowBuilder().name("Row").weights(1, 1).verticalAlign(RowVerticalAlign.CENTER)
                        .addParagraph(paragraph -> paragraph.name("Label").text("Label"))
                        .addSection("Column", column -> {
                            column.addParagraph(paragraph -> paragraph.name("ColumnTop").text("Column, line one"));
                            for (int line = 2; line <= 5; line++) {
                                column.addParagraph("Column, line " + line);
                            }
                        }).build(), 0, 20, LayerAlign.TOP_LEFT)));

        double intro = export.placed("IntroEnd").placementY();
        assertThat(top(export.placed("Label"))).as("premise: its first block starts under the intro").isLessThan(intro);
        assertThat(export.notedAbove("Row", "stack"))
                .isCloseTo(top(export.placed("ColumnTop")) - intro, within(0.01));
    }

    @Test
    void aLayerOverAnotherInItsColumnIsNamed() throws Exception {
        Export export = export(page -> page.addLayerStack(stack -> stack
                .layer(new SectionBuilder().name("LeftFirst").margin(new DocumentInsets(0, 200, 0, 0))
                        .addParagraph(paragraph -> paragraph.name("LeftFirstText").text("Left column, first layer"))
                        .build(), LayerAlign.TOP_LEFT)
                .layer(new SectionBuilder().name("Right").margin(new DocumentInsets(0, 0, 0, 200))
                        .addParagraph("Right column").build(), LayerAlign.TOP_LEFT)
                .layer(new SectionBuilder().name("LeftOver").margin(new DocumentInsets(0, 200, 0, 0))
                        .addParagraph(paragraph -> paragraph.name("LeftOverText").text("Left column, second layer"))
                        .build(), LayerAlign.TOP_LEFT)));

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(export.bytes()))) {
            assertThat(document.getTables()).as("the stack is written as columns").hasSize(1);
            assertThat(document.getTables().get(0).getRow(0).getCell(0).getText())
                    .isEqualTo("Left column, first layerLeft column, second layer");
        }
        assertThat(export.notedAbove("LeftOver", "column"))
                .isCloseTo(top(export.placed("LeftOverText")) - export.placed("LeftFirstText").placementY(),
                        within(0.01));
        assertThat(export.report().bySubject().get(SUBJECT)).hasSize(1);
    }

    @Test
    void aLayerOverACardInItsColumnIsMeasuredFromWhereTheCardsPanelEnds() throws Exception {
        Export export = export(page -> page.addLayerStack(stack -> stack
                .layer(new SectionBuilder().name("LeftFirst").margin(new DocumentInsets(0, 200, 0, 0))
                        .addSection(card -> card.name("Card").fillColor(SURFACE).padding(DocumentInsets.of(8))
                                .addParagraph("A card in the left column")).build(), LayerAlign.TOP_LEFT)
                .layer(new SectionBuilder().name("Right").margin(new DocumentInsets(0, 0, 0, 200))
                        .addParagraph("Right column").build(), LayerAlign.TOP_LEFT)
                .layer(new SectionBuilder().name("LeftOver").margin(new DocumentInsets(0, 200, 0, 0))
                        .addParagraph("Left column, second layer").build(), LayerAlign.TOP_LEFT)));

        assertThat(export.notedAbove("LeftOver", "column"))
                .isCloseTo(top(export.placed("LeftOver")) - export.placed("Card").placementY(), within(0.01));
    }

    @Test
    void aLayerIsMeasuredFromItsOwnBoxNotFromTheMarginAboveIt() throws Exception {
        Export export = export(page -> page.addLayerStack(stack -> stack
                .layer(new ParagraphBuilder().name("Behind").text("BEHIND").build(), LayerAlign.TOP_LEFT)
                .layer(new ParagraphBuilder().name("Front").text("In front").margin(new DocumentInsets(4, 0, 0, 0))
                        .build(), LayerAlign.TOP_LEFT)));

        assertThat(export.notedAbove("Front", "stack"))
                .isCloseTo(top(export.placed("Front")) - export.placed("Behind").placementY(), within(0.01));
    }

    @Test
    void aStackComposedInATableCellIsNamedAsNotMeasured() throws Exception {
        // A layer stack composed in a cell has no place of its own: it is no band, and its
        // layers are written one after another however the page lays them.
        Export captioned = export(page -> page.add(new TableBuilder().name("Gallery")
                .columns(DocumentTableColumn.fixed(240))
                .rowCells(DocumentTableCell.node(new LayerStackBuilder().name("Captioned")
                        .layer(new ImageBuilder().name("Photo").source(png()).size(200, 100).build(), LayerAlign.TOP_LEFT)
                        .layer(new ParagraphBuilder().name("Caption").text("Caption over the photo").build(),
                                LayerAlign.BOTTOM_LEFT)
                        .build()))
                .build()));
        Export drawn = export(page -> page.add(new TableBuilder().name("Icons")
                .columns(DocumentTableColumn.fixed(240))
                .rowCells(DocumentTableCell.node(new LayerStackBuilder().name("Icon")
                        .layer(new ShapeBuilder().size(20, 20).fillColor(INK).build(), LayerAlign.TOP_LEFT)
                        .layer(new ShapeBuilder().size(10, 10).fillColor(SURFACE).build(), LayerAlign.CENTER)
                        .build()))
                .build()));

        assertThat(captioned.report().bySubject().get(SUBJECT)).extracting(DocxExportReport.Note::detail)
                .containsExactly("its layers are written one after another, and with no place of its own in the "
                                 + "layout — composed in a table cell, or with no layout behind the export — whether "
                                 + "the page lays one over another is not measured");
        assertThat(drawn.report().bySubject()).as("drawing alone writes nothing in the flow")
                .doesNotContainKey(SUBJECT);
    }

    @Test
    void layersThePageSetsUnderTheOnesBeforeThemAreNotNamed() throws Exception {
        // A later layer holding the place of an earlier one with a spacer starts its own content
        // under it, as a layer set at the stack's foot does under one at its top.
        double line = lineHeight();
        Export standIn = export(page -> page.addLayerStack(stack -> stack
                .layer(new ParagraphBuilder().name("Title").text("Title").build(), LayerAlign.TOP_LEFT)
                .layer(new SectionBuilder().name("Body").addSpacer(spacer -> spacer.height(line))
                        .addParagraph("Body text").build(), LayerAlign.TOP_LEFT)));
        Export apart = export(page -> page.addLayerStack(stack -> stack
                .layer(new ParagraphBuilder().name("Top").text("Top line").build(), LayerAlign.TOP_LEFT)
                .layer(new ParagraphBuilder().name("Bottom").text("Bottom line").build(), LayerAlign.BOTTOM_LEFT)
                .layer(new SectionBuilder().name("Height").addSpacer(spacer -> spacer.height(60)).build(),
                        LayerAlign.TOP_LEFT)));

        assertThat(standIn.report().bySubject()).doesNotContainKey(SUBJECT);
        assertThat(apart.report().bySubject()).doesNotContainKey(SUBJECT);
    }

    @Test
    void aLinePairAndTextOverADrawingAreNotNamed() throws Exception {
        // A pair is one line split by a tab, and a drawing is drawn where the page draws it: Word
        // writes neither layer under the other.
        Export pair = export(page -> page.addLayerStack(stack -> stack
                .layer(new ParagraphBuilder().name("Title").text("Title").build(), LayerAlign.TOP_LEFT)
                .position(new ParagraphBuilder().name("Dates").text("2024 - 2026").build(), 120, 2,
                        LayerAlign.TOP_LEFT)));
        Export overADrawing = export(page -> page.addLayerStack(stack -> stack
                .back(new ShapeBuilder().size(200, 40).fillColor(SURFACE).build())
                .center(new ParagraphBuilder().name("Over").text("Text over a drawing").build())));

        assertThat(pair.report().bySubject()).doesNotContainKey(SUBJECT);
        assertThat(overADrawing.report().bySubject()).doesNotContainKey(SUBJECT);
    }

    @Test
    void aLayerTouchingTheOneBeforeItIsNotNamed() throws Exception {
        double line = lineHeight();
        Export touching = stackedAt(line - 0.3);
        Export over = stackedAt(line - 1);

        assertThat(touching.report().bySubject()).as("0.3pt over is the same edge").doesNotContainKey(SUBJECT);
        assertThat(over.notedAbove("Second", "stack")).isCloseTo(1, within(0.01));
    }

    private static Export stackedAt(double offset) throws Exception {
        return export(page -> page.addLayerStack(stack -> stack
                .layer(new ParagraphBuilder().name("First").text("First").build(), LayerAlign.TOP_LEFT)
                .position(new ParagraphBuilder().name("Second").text("Second").build(), 0, offset,
                        LayerAlign.TOP_LEFT)));
    }

    /** The height the page gives a line of the default text. */
    private static double lineHeight() throws Exception {
        return export(page -> page.addParagraph(paragraph -> paragraph.name("Line").text("Line")))
                .placed("Line").placementHeight();
    }

    private static double top(PlacedNode node) {
        return node.placementY() + node.placementHeight();
    }

    private record Export(DocxExportReport report, List<PlacedNode> placements, byte[] bytes) {

        PlacedNode placed(String name) {
            return placements.stream().filter(node -> node.path().matches("(.*/)?" + name + "\\[\\d+]"))
                    .findFirst().orElseThrow(() -> new AssertionError("no placement named " + name));
        }

        /**
         * How far above where the layers before it end the note on the named layer says the page
         * starts it, the note naming what those layers are measured in.
         */
        double notedAbove(String name, String among) {
            List<DocxExportReport.Note> notes = report.bySubject().get(SUBJECT);
            assertThat(notes).as("the notes on overlapping layers").isNotNull();
            DocxExportReport.Note note = notes.stream().filter(candidate -> candidate.path().equals(placed(name).path()))
                    .findFirst().orElseThrow(() -> new AssertionError("no note on " + name + " in " + notes));
            Matcher matcher = DETAIL.matcher(note.detail());
            assertThat(matcher.matches()).as(note.detail()).isTrue();
            assertThat(matcher.group(2)).isEqualTo(among);
            return Double.parseDouble(matcher.group(1));
        }
    }

    private static Export export(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 600)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(content::accept);
            byte[] bytes = session.export(new DocxSemanticBackend(report::set));
            return new Export(report.get(), session.layoutGraph().nodes(), bytes);
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
