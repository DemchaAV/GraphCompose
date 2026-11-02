package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a node was written as, where that is not the kind of thing the node is, is in the report.
 *
 * <p>Which Word object a node becomes is settled as it is written, and the writer's phrase for it
 * reached the report only as the lead-in to a list of what the node went without — so a divider
 * written as a line, layers written side by side or one after another, a box written as drawing
 * alone, each said nothing at all unless the same node also lost a link or an outline entry. The
 * caller the report is for cannot see any of it in its own tree.</p>
 *
 * <p>A node written as the kind of thing it is keeps the old gate: a paragraph, a table, a picture
 * and a box are named for what they lose and not for their form, and so is a {@code LineNode}
 * written as a paragraph border, which is a line either way.</p>
 */
class DocxRewrittenFormReportTest {

    private static final DocumentColor INK = DocumentColor.rgb(26, 86, 148);
    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);

    private static final String AS_A_RULE = "written as a paragraph border rather than as a drawing";

    @Test
    void aDividerIsNamedForTheRuleItBecomesThoughItLosesNothingElse() throws Exception {
        Consumer<PageFlowBuilder> content = page -> page.addDivider(divider -> divider.name("Bar")
                .width(200).thickness(2).color(INK));
        DocxExportReport report = reportOf(content);

        assertThat(detailOf(report, "ShapeNode")).startsWith(AS_A_RULE)
                .as("what the note names is the divider, the one box written as a rule")
                .contains("a divider, no taller than Word's thickest border")
                .contains("flows with the text")
                .as("no loss is appended where there is none").doesNotContain("; its")
                .as("not the words a drawing's note says of itself: DocxNeighbourFillGuardTest "
                    + "holds every note carrying them to its ledger of placeable drawings")
                .doesNotContain("drawn as a shape");
        assertThat(pathOf(report, "ShapeNode")).as("the authored node's own path")
                .isEqualTo("ContainerNode[0]/Bar[0]");
        assertThat(noteOn(report, "ShapeNode").severity())
                .isEqualTo(DocxExportReport.Severity.APPROXIMATED);
        // The note's own claim, read off the file: a bordered paragraph and no drawing.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, content)) {
            assertThat(document.getParagraphs().get(0).getCTP().getPPr().getPBdr().getBottom())
                    .as("a paragraph of its own, bordered below").isNotNull()
                    .matches(border -> border.isSetSz() && border.isSetColor());
            assertThat(document.getDocument().xmlText()).doesNotContain("<wp:anchor");
        }
    }

    @Test
    void aDividerWhoseFillShowsNothingIsNamedWithoutPromisingABorder() throws Exception {
        // A border is painted only where the fill shows (writeRule), and a plain fill of no
        // opacity is a rule all the same: the paragraph is written, and the note says the border
        // is there where the fill shows rather than promising one.
        DocxExportReport report = reportOf(page -> page.addDivider(divider -> divider.name("Clear")
                .width(200).thickness(2).color(DocumentColor.rgba(26, 86, 148, 0))));

        assertThat(detailOf(report, "ShapeNode")).startsWith(AS_A_RULE)
                .contains("bordered below where the fill shows");
    }

    @Test
    void aBoxOfAColourIsNotNamedAsARuleBecauseItIsNotWrittenAsOne() throws Exception {
        // addDivider and addShape build the same ShapeNode but for one signal (DividerBuilder):
        // the same thickness, the same fill, no stroke. Only the divider is written as Word's
        // rule, so only the divider is named for that form; the swatch is drawn, and its
        // drawing's own notes say what a drawing says.
        Consumer<PageFlowBuilder> content = page -> page.addShape(shape -> shape.name("Swatch")
                .size(200, 1).fillColor(INK));

        assertThat(notesOn(reportOf(content), "ShapeNode"))
                .noneMatch(detail -> detail.startsWith(AS_A_RULE));
        // The absence of the note is held to the file, not taken on trust: the swatch really is
        // a drawing there, and no paragraph carries a border for it.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, content)) {
            assertThat(document.getDocument().xmlText()).contains("<wp:anchor");
            assertThat(document.getParagraphs()).allSatisfy(paragraph ->
                    assertThat(paragraph.getCTP().getPPr())
                            .matches(properties -> properties == null || properties.getPBdr() == null));
        }
    }

    @Test
    void aLineWrittenAsARuleIsNotNamedForItsForm() throws Exception {
        // A line written as a paragraph border is a line either way; what it loses is still named
        // (DocxCarriedWithoutReportTest).
        DocxExportReport report = reportOf(page -> page
                .addLine(line -> line.name("Rail").horizontal(200).thickness(1).color(INK)));

        assertThat(report.bySubject()).doesNotContainKey("LineNode");
    }

    @Test
    void layersSideBySideAreNamedAsTheColumnsTheyBecome() throws Exception {
        Consumer<PageFlowBuilder> content = page -> page.addLayerStack(stack -> stack.name("Grid")
                .layer(new SectionBuilder().name("Left").margin(new DocumentInsets(0, 220, 0, 0))
                        .addParagraph("Left column").build(), LayerAlign.TOP_LEFT)
                .layer(new SectionBuilder().name("Right").margin(new DocumentInsets(0, 0, 0, 200))
                        .addParagraph("Right column").build(), LayerAlign.TOP_LEFT));
        DocxExportReport report = reportOf(content);

        assertThat(detailOf(report, "LayerStackNode"))
                .startsWith("written as the columns of a table: the layers the page sets side by side "
                            + "stand in a cell each");
        assertThat(pathOf(report, "LayerStackNode")).isEqualTo("ContainerNode[0]/Grid[0]");
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, content)) {
            assertThat(document.getTables()).hasSize(1);
            assertThat(document.getTables().get(0).getRow(0).getTableCells())
                    .as("a cell each, as the note says").extracting(XWPFTableCell::getText)
                    .containsExactly("Left column", "Right column");
        }
    }

    @Test
    void layersThePageGivesOneBandToAreNamedAsFollowingOneAnotherInItsCell() throws Exception {
        // Two layers in the right-hand band: DocxLayerColumns puts both in that column, so "a cell
        // each" is not the whole truth and the note says what the file holds.
        Consumer<PageFlowBuilder> content = page -> page.addLayerStack(stack -> stack.name("Grid")
                .layer(new SectionBuilder().name("Left").margin(new DocumentInsets(0, 220, 0, 0))
                        .addParagraph("Left column").build(), LayerAlign.TOP_LEFT)
                .layer(new SectionBuilder().name("RightTop").margin(new DocumentInsets(0, 0, 0, 200))
                        .addParagraph("Name").build(), LayerAlign.TOP_LEFT)
                .layer(new SectionBuilder().name("RightUnder").margin(new DocumentInsets(0, 0, 0, 200))
                        .addParagraph("Role").build(), LayerAlign.TOP_LEFT));
        DocxExportReport report = reportOf(content);

        assertThat(detailOf(report, "LayerStackNode"))
                .contains("those the page gives one band to share its cell");
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, content)) {
            List<XWPFTableCell> cells = document.getTables().get(0).getRow(0).getTableCells();
            assertThat(cells).as("three layers, two cells").hasSize(2);
            assertThat(cells.get(1).getParagraphs()).extracting(XWPFParagraph::getText)
                    .as("the band's two layers one after another in its cell")
                    .containsExactly("Name", "Role");
        }
    }

    @Test
    void aLayerOfNothingButDrawingOverTheColumnsIsNamedAsStandingOverThem() throws Exception {
        // A mark laid across the boundary of two cards forms no column of its own: it is drawn
        // over them, which is why the note cannot say nothing stands over anything.
        Consumer<PageFlowBuilder> content = page -> page.addLayerStack(stack -> stack.name("Cards")
                .layer(new SectionBuilder().name("First").margin(new DocumentInsets(0, 200, 0, 0))
                        .addParagraph("AWS Certified").build(), LayerAlign.TOP_LEFT)
                .layer(new SectionBuilder().name("Second").margin(new DocumentInsets(0, 0, 0, 180))
                        .addParagraph("Docker Certified").build(), LayerAlign.TOP_LEFT)
                .layer(new SectionBuilder().name("Mark").margin(new DocumentInsets(0, 160, 0, 170))
                        .add(new ShapeBuilder().name("Medal").size(16, 16).fillColor(INK).build())
                        .build(), LayerAlign.TOP_LEFT));
        DocxExportReport report = reportOf(content);

        assertThat(detailOf(report, "LayerStackNode"))
                .contains("a layer holding only drawing or pictures may have no cell at all and be "
                          + "drawn where the page draws it — behind the columns' text");
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, content)) {
            assertThat(document.getTables().get(0).getRow(0).getTableCells())
                    .as("three layers, two cells: the mark has none")
                    .extracting(XWPFTableCell::getText)
                    .containsExactly("AWS Certified", "Docker Certified");
            assertThat(document.getDocument().xmlText())
                    .as("drawn where the page draws it, not written in a cell, and behind the text "
                        + "as the note says — no panel's shading stands under it here")
                    .contains("<wp:anchor").contains("behindDoc=\"1\"");
        }
    }

    @Test
    void layersOneAfterAnotherAreNamedAsTheBandTheyBecome() throws Exception {
        // A backdrop behind two paragraph layers: what the band writes goes one after another, and
        // the backdrop is drawn behind it — which is why the note cannot claim nothing in the stack
        // is laid over anything.
        Consumer<PageFlowBuilder> content = page -> page.addLayerStack(stack -> stack.name("Band")
                .back(new ShapeBuilder().name("Backdrop").size(200, 160).fillColor(SURFACE).build())
                .layer(new ParagraphBuilder().name("First").text("First").build(), LayerAlign.TOP_LEFT)
                .layer(new ParagraphBuilder().name("Second").text("Second").build(), LayerAlign.BOTTOM_LEFT));
        DocxExportReport report = reportOf(content);

        assertThat(detailOf(report, "LayerStackNode"))
                .startsWith("written as a band of its layers: what they write goes one after another")
                .contains("a layer of nothing but drawing is drawn where the page draws it rather "
                          + "than written in the flow");
        assertThat(pathOf(report, "LayerStackNode")).isEqualTo("ContainerNode[0]/Band[0]");
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, content)) {
            assertThat(document.getParagraphs()).extracting(XWPFParagraph::getText)
                    .as("what the layers write, one after another").contains("First", "Second");
            assertThat(document.getDocument().xmlText())
                    .as("and the backdrop drawn, not written in the flow").contains("<wp:anchor");
        }
    }

    @Test
    void aTitleAndItsDatesAtEitherEndAreNamedAsTheLineTheyShare() throws Exception {
        // A rule under the pair is the "whatever else the layers hold" of the note: it is drawn
        // where the page draws it, not written as a third line.
        Consumer<PageFlowBuilder> content = page -> page.add(new ShapeContainerBuilder().name("Head")
                .rectangle(360, 14).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(new ParagraphBuilder().name("Title").text("MARKETING MANAGER")
                        .align(TextAlign.LEFT).build(), 0, 0, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder().name("Dates").text("Jan 2022 - Present")
                        .align(TextAlign.LEFT).build(), 0, 0, LayerAlign.CENTER_RIGHT)
                .position(new ShapeBuilder().name("Pill").size(360, 2).fillColor(SURFACE).build(),
                        0, 0, LayerAlign.BOTTOM_LEFT)
                .build());
        DocxExportReport report = reportOf(content);

        assertThat(detailOf(report, "ShapeContainerNode"))
                .isEqualTo("written as one line that two paragraphs share, the one further along it "
                           + "set where the page sets it — at the line's end, or at a tab where the "
                           + "page starts it — and whatever else the layers hold drawn where the page "
                           + "draws it");
        assertThat(pathOf(report, "ShapeContainerNode")).isEqualTo("ContainerNode[0]/Head[0]");
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, content)) {
            assertThat(document.getParagraphs()).extracting(XWPFParagraph::getText)
                    .as("one line, both texts on it")
                    .contains("MARKETING MANAGER\tJan 2022 - Present");
            assertThat(document.getDocument().xmlText())
                    .as("and the pill drawn beside it").contains("<wp:anchor");
        }
    }

    @Test
    void aBoxOfNothingButDrawingIsNamedAsDrawingAlone() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addLayerStack(stack -> stack.name("Ornament")
                        .layer(new ShapeBuilder().name("Disc").size(40, 40).fillColor(SURFACE).build(),
                                LayerAlign.TOP_LEFT)
                        .layer(new ShapeBuilder().name("Dot").size(12, 12).fillColor(INK).build(),
                                LayerAlign.CENTER))
                .addParagraph("Below"));

        assertThat(detailOf(report, "LayerStackNode")).startsWith("written as drawing alone")
                .contains("the room it took on the page is owed as space to the next block written");
        assertThat(pathOf(report, "LayerStackNode")).isEqualTo("ContainerNode[0]/Ornament[0]");
    }

    @Test
    void aDrawnTileNamesBothWhereItsDrawingGoesAndThatItHasNoPlaceInTheFlow() throws Exception {
        // Two notes on one node, which answer different questions: the drawing's says what it is
        // anchored in and so what editing the text above moves, and the form's says the node
        // itself is nowhere in the flow. 21 nodes of the fidelity corpus carry both.
        DocxExportReport report = reportOf(page -> page
                .add(new ShapeContainerBuilder().name("Plate")
                        .rectangle(40, 40).fillColor(SURFACE)
                        .center(new ShapeBuilder().name("Dot").size(12, 12).fillColor(INK).build())
                        .build())
                .addParagraph("Below"));

        assertThat(notesOn(report, "ShapeContainerNode")).hasSize(2)
                .anyMatch(detail -> detail.startsWith("written as drawing alone"))
                .anyMatch(detail -> detail.startsWith("drawn as a shape"));
    }

    @Test
    void aNodeLaidOverTheFlowIsNamedAsWrittenOverIt() throws Exception {
        Consumer<PageFlowBuilder> content = page -> page
                .add(DocxExports.laidOverTheFlow(
                        new SectionBuilder().name("Studio").addParagraph("Studio").build()))
                .addParagraph("Masthead");
        DocxExportReport report = reportOf(content);

        assertThat(report.notes())
                .filteredOn(note -> note.detail().startsWith("written over the flow"))
                .singleElement()
                .satisfies(note -> {
                    assertThat(note.subject()).isEqualTo("ShapeContainerNode");
                    assertThat(note.path()).isEqualTo("ContainerNode[0]/Sidebar[0]");
                    assertThat(note.detail()).contains("a paragraph in a text box");
                });
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, content)) {
            assertThat(document.getDocument().xmlText())
                    .as("its text in a text box, as the note says").contains("txbxContent")
                    .contains("Studio");
            assertThat(document.getParagraphs()).extracting(XWPFParagraph::getText)
                    .as("and nothing of it in the flow").doesNotContain("Studio");
        }
    }

    @Test
    void aBadgeOverTheFlowHasItsLettersInItsShapeRatherThanATextBox() throws Exception {
        // The badge's guard is the first of writeNodeContent's, so a badge inside a node laid over
        // the flow never reaches the text-box path: its letters go into the shape drawn for it,
        // which is why the note cannot promise a text box for every paragraph.
        Consumer<PageFlowBuilder> content = page -> page
                .add(DocxExports.laidOverTheFlow(new ShapeContainerBuilder().name("Monogram")
                        .roundedRect(40, 40, com.demcha.compose.document.style.DocumentCornerRadius.of(20))
                        .fillColor(INK)
                        .center(new ParagraphBuilder().name("Initials").text("AD").build())
                        .build()))
                .addParagraph("Masthead");
        DocxExportReport report = reportOf(content);

        assertThat(report.notes())
                .filteredOn(note -> note.detail().startsWith("written over the flow"))
                .singleElement().extracting(DocxExportReport.Note::detail).asString()
                .contains("a badge's letters in the shape drawn for them");
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, content)) {
            // Both a text box and a badge hold their text in a wps:txbx; what tells them apart is
            // the shape round it — the badge's carries its own fill, a text box draws nothing.
            String body = document.getDocument().xmlText();
            int letters = body.indexOf(">AD<");
            assertThat(letters).as("the letters are in the file").isGreaterThan(0);
            int shape = body.lastIndexOf("<wps:wsp", letters);
            assertThat(shape).as("inside a drawn shape").isGreaterThan(0);
            assertThat(body.substring(shape, letters))
                    .as("the shape drawn for the badge carries them: its own fill is on it")
                    .containsIgnoringCase("1A5694");
            assertThat(document.getParagraphs()).extracting(XWPFParagraph::getText)
                    .as("and nothing of the badge in the flow").doesNotContain("AD");
        }
    }

    @Test
    void aBadgeIsNamedAsTheShapeItsLettersAreSetIn() throws Exception {
        // A container of a few letters is drawn as one shape carrying them, and holds its room the
        // way a box of nothing but drawing does — its guard one branch earlier (writeTextBadge).
        DocxExportReport report = reportOf(page -> page
                .add(new ShapeContainerBuilder().name("Monogram")
                        .roundedRect(40, 40, com.demcha.compose.document.style.DocumentCornerRadius.of(20))
                        .fillColor(INK)
                        .center(new ParagraphBuilder().name("Initials").text("AD").build())
                        .build())
                .addParagraph("Below"));

        assertThat(notesOn(report, "ShapeContainerNode"))
                .filteredOn(detail -> detail.startsWith("written as one drawn shape holding its text"))
                .singleElement().asString()
                .contains("its letters are set in the shape rather than written as a paragraph of "
                          + "the flow")
                .contains("the room it took on the page is owed as space to the next block written");
    }

    @Test
    void aShapeContainerWhoseLayersAreWrittenInTheFlowIsNamedOnceByItsOwnWriter() throws Exception {
        // Its own writer says it (reportLayersWrittenInline), so where such a container is written
        // as a band of its layers nothing adds a second note of the same thing — in the fidelity
        // corpus that is 83 of the 91 bands.
        DocxExportReport report = reportOf(page -> page.add(new ShapeContainerBuilder().name("Card")
                .rectangle(300, 120).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(new ShapeBuilder().name("Backdrop").size(300, 120).fillColor(SURFACE).build(),
                        0, 0, LayerAlign.TOP_LEFT)
                .position(new SectionBuilder().name("Body").addParagraph("One").addParagraph("Two").build(),
                        0, 10, LayerAlign.TOP_LEFT)
                .build()));

        assertThat(notesOn(report, "shape container"))
                .containsExactly("its layers are written inline, one after another in source order");
        assertThat(notesOn(report, "ShapeContainerNode"))
                .as("not named a second time for the same thing")
                .noneMatch(detail -> detail.startsWith("written as a band of its layers"));
    }

    @Test
    void aNodeWrittenAsTheKindOfThingItIsIsNotNamedForItsForm() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addParagraph("Plain")
                .addImage(image -> image.source(DocumentImageData.fromBytes(png())).size(80, 40))
                .addTable(table -> table.columns(DocumentTableColumn.fixed(200)).row("Cell"))
                .addSection(section -> section.addParagraph("Inside"))
                .addSection(panel -> panel.fillColor(SURFACE).addParagraph("In a panel")));

        assertThat(report.bySubject())
                .doesNotContainKeys("ParagraphNode", "ImageNode", "TableNode", "SectionNode");
    }

    private static List<String> notesOn(DocxExportReport report, String subject) {
        return report.bySubject().getOrDefault(subject, List.of()).stream()
                .map(DocxExportReport.Note::detail).toList();
    }

    private static String detailOf(DocxExportReport report, String subject) {
        return noteOn(report, subject).detail();
    }

    private static String pathOf(DocxExportReport report, String subject) {
        return noteOn(report, subject).path();
    }

    private static DocxExportReport.Note noteOn(DocxExportReport report, String subject) {
        List<DocxExportReport.Note> notes = report.bySubject().get(subject);
        assertThat(notes).as("the note on the %s", subject).isNotNull().hasSize(1);
        return notes.get(0);
    }

    private static DocxExportReport reportOf(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.report(400, 600, 20, content);
    }

    private static byte[] png() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
