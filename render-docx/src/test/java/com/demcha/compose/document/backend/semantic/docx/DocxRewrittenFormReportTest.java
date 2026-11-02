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
 * reached the report only as the lead-in to a list of what the node went without — so a filled bar
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
    void aFilledBarIsNamedForTheRuleItBecomesThoughItLosesNothingElse() throws Exception {
        DocxExportReport report = reportOf(page -> page.addShape(shape -> shape.name("Bar")
                .size(200, 2).fillColor(INK)));

        assertThat(detailOf(report, "ShapeNode")).startsWith(AS_A_RULE)
                .contains("flows with the text")
                .as("no loss is appended where there is none").doesNotContain("; its")
                .as("not the words a drawing's note says of itself: DocxNeighbourFillGuardTest "
                    + "holds every note carrying them to its ledger of placeable drawings")
                .doesNotContain("drawn as a shape");
        assertThat(pathOf(report, "ShapeNode")).as("the authored node's own path")
                .isEqualTo("ContainerNode[0]/Bar[0]");
        assertThat(noteOn(report, "ShapeNode").severity())
                .isEqualTo(DocxExportReport.Severity.APPROXIMATED);
    }

    @Test
    void aBoxWhoseFillShowsNothingIsNamedWithoutPromisingABorder() throws Exception {
        // A border is painted only where the fill shows (writeRule), and a plain fill of no
        // opacity is a rule all the same: the paragraph is written, and the note says the border
        // is there where the fill shows rather than promising one.
        DocxExportReport report = reportOf(page -> page.addShape(shape -> shape.name("Clear")
                .size(200, 2).fillColor(DocumentColor.rgba(26, 86, 148, 0))));

        assertThat(detailOf(report, "ShapeNode")).startsWith(AS_A_RULE)
                .contains("bordered below where the fill shows");
    }

    @Test
    void aDividerBuiltByTheDslIsNamedAsAnyOtherFilledBoxIs() throws Exception {
        // addDivider builds a ShapeNode whose thickness is its height, with a fill and no stroke
        // (DividerBuilder), so the DSL's own divider and a colour swatch of the same height are one
        // node: the report names both, having nothing to tell them apart by.
        DocxExportReport report = reportOf(page -> page
                .addDivider(divider -> divider.name("Divider").width(200).thickness(1).color(INK)));

        assertThat(detailOf(report, "ShapeNode")).startsWith(AS_A_RULE);
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
        DocxExportReport report = reportOf(page -> page.addLayerStack(stack -> stack.name("Grid")
                .layer(new SectionBuilder().name("Left").margin(new DocumentInsets(0, 220, 0, 0))
                        .addParagraph("Left column").build(), LayerAlign.TOP_LEFT)
                .layer(new SectionBuilder().name("Right").margin(new DocumentInsets(0, 0, 0, 200))
                        .addParagraph("Right column").build(), LayerAlign.TOP_LEFT)));

        assertThat(detailOf(report, "LayerStackNode"))
                .isEqualTo("written as the columns of a table: its layers stand side by side, a cell "
                           + "each, as the page places them, rather than one over another");
        assertThat(pathOf(report, "LayerStackNode")).isEqualTo("ContainerNode[0]/Grid[0]");
    }

    @Test
    void layersOneAfterAnotherAreNamedAsTheBandTheyBecome() throws Exception {
        DocxExportReport report = reportOf(page -> page.addLayerStack(stack -> stack.name("Band")
                .back(new ShapeBuilder().name("Backdrop").size(200, 160).build())
                .layer(new ParagraphBuilder().name("First").text("First").build(), LayerAlign.TOP_LEFT)
                .layer(new ParagraphBuilder().name("Second").text("Second").build(), LayerAlign.BOTTOM_LEFT)));

        assertThat(detailOf(report, "LayerStackNode"))
                .startsWith("written as its layers one after another")
                .contains("none of the stack's layers is laid over another in the file");
        assertThat(pathOf(report, "LayerStackNode")).isEqualTo("ContainerNode[0]/Band[0]");
    }

    @Test
    void aTitleAndItsDatesAtEitherEndAreNamedAsTheLineTheyShare() throws Exception {
        DocxExportReport report = reportOf(page -> page.add(new ShapeContainerBuilder().name("Head")
                .rectangle(360, 14).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(new ParagraphBuilder().name("Title").text("MARKETING MANAGER")
                        .align(TextAlign.LEFT).build(), 0, 0, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder().name("Dates").text("Jan 2022 - Present")
                        .align(TextAlign.LEFT).build(), 0, 0, LayerAlign.CENTER_RIGHT)
                .build()));

        assertThat(detailOf(report, "ShapeContainerNode"))
                .isEqualTo("written as one line that two paragraphs share, its layers set at either "
                           + "end of it, rather than one over another");
        assertThat(pathOf(report, "ShapeContainerNode")).isEqualTo("ContainerNode[0]/Head[0]");
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
        DocxExportReport report = reportOf(page -> page
                .add(DocxExports.laidOverTheFlow(
                        new SectionBuilder().name("Studio").addParagraph("Studio").build()))
                .addParagraph("Masthead"));

        assertThat(report.notes())
                .filteredOn(note -> note.detail().startsWith("written over the flow"))
                .singleElement()
                .satisfies(note -> {
                    assertThat(note.subject()).isEqualTo("ShapeContainerNode");
                    assertThat(note.path()).isEqualTo("ContainerNode[0]/Sidebar[0]");
                    assertThat(note.detail())
                            .contains("each of its paragraphs in a text box where the page sets it");
                });
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
                .noneMatch(detail -> detail.startsWith("written as its layers one after another"));
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
