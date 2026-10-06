package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.style.DocumentTransform;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.font.FontName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The clip of a layer stack or a shape container, and the transform of a container whose outline
 * draws nothing, are in the report on each path the export writes them by.
 *
 * <p>A Word file has no clip a container can set round its layers: what the page cut away was
 * written whole, and the report named a container's clip on one path only — and there of every
 * container, cut or not. A clip that cuts nothing loses nothing, and is not named.</p>
 */
class DocxClipReportTest {

    private static final DocumentColor INK = DocumentColor.rgb(26, 86, 148);
    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final String PAST_ITS_BOX =
            "its clip is not in the file, so what its layers paint past its box is written whole";
    private static final String PAST_ITS_OUTLINE =
            "its clip is not in the file, so what its layers paint past its outline is written whole";

    @Test
    void aLayerStackNamesTheClipThatCutsItsLayers() throws Exception {
        // A dot set past the stack's right side: the page cuts it at the box, the file does not.
        DocxExportReport report = reportOf(page -> page
                .addParagraph("Above")
                .addLayerStack(stack -> stack.name("Stack").clipToBounds()
                        .layer(box(100, 40), LayerAlign.TOP_LEFT, 0)
                        .position(box(30, 30), 90, 5, LayerAlign.TOP_LEFT, 1))
                .addParagraph("Below"));

        assertThat(detailOf(report, "clipped layer stack")).isEqualTo(PAST_ITS_BOX);
    }

    @Test
    void aClipThatCutsNothingOrNoClipAtAllIsNotNamed() throws Exception {
        DocxExportReport inside = reportOf(page -> page
                .addParagraph("Above")
                .addLayerStack(stack -> stack.name("Stack").clipToBounds()
                        .layer(box(100, 40), LayerAlign.TOP_LEFT, 0)
                        .position(box(30, 30), 60, 5, LayerAlign.TOP_LEFT, 1))
                .addParagraph("Below"));
        DocxExportReport notClipping = reportOf(page -> page
                .addParagraph("Above")
                .addLayerStack(stack -> stack.name("Stack")
                        .layer(box(100, 40), LayerAlign.TOP_LEFT, 0)
                        .position(box(30, 30), 90, 5, LayerAlign.TOP_LEFT, 1))
                .addParagraph("Below"));
        DocxExportReport overflowVisible = reportOf(page -> page
                .addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Disc").circle(40).fillColor(INK)
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .center(box(40, 40)).build())
                .addParagraph("Below"));

        // A rota's shift chip in CobaltRota's face: its label's line two points taller than
        // the chip, its digits inside it.
        DocxExportReport chip = reportOf(page -> page
                .addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Chip").roundedRect(90, 8, 4).fillColor(SURFACE)
                        .clipPolicy(ClipPolicy.CLIP_PATH)
                        .center(new ParagraphBuilder().text("08:00-16:00")
                                .textStyle(DocumentTextStyle.builder().fontName(FontName.CARLITO).size(8.2).build())
                                .build())
                        .build())
                .addParagraph("Below"));

        // The same label in Helvetica, which the PDF does not embed: its line fits the chip, and
        // it is measured by its line, not by outlines read through a stand-in in other units.
        DocxExportReport standard = reportOf(page -> page
                .addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Chip").roundedRect(90, 12, 4).fillColor(SURFACE)
                        .clipPolicy(ClipPolicy.CLIP_PATH)
                        .center(new ParagraphBuilder().text("08:00-16:00")
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(8.2)).build())
                        .build())
                .addParagraph("Below"));

        assertThat(inside.bySubject()).doesNotContainKeys("clipped layer stack", "clipped shape container");
        assertThat(chip.bySubject()).doesNotContainKeys("clipped layer stack", "clipped shape container");
        assertThat(standard.bySubject()).doesNotContainKeys("clipped layer stack", "clipped shape container");
        assertThat(notClipping.bySubject()).doesNotContainKeys("clipped layer stack", "clipped shape container");
        assertThat(overflowVisible.bySubject()).doesNotContainKeys("clipped layer stack", "clipped shape container");
    }

    @Test
    void aDrawingPastItsCircleOrItsBoxNamesTheClip() throws Exception {
        // A square filling a disc: the page rounds its corners off, the file draws them.
        DocxExportReport report = reportOf(page -> page
                .addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Disc").circle(40).fillColor(SURFACE)
                        .clipPolicy(ClipPolicy.CLIP_PATH)
                        .center(box(40, 40)).build())
                .addParagraph("Below"));
        DocxExportReport toItsBox = reportOf(page -> page
                .addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Disc").circle(40).fillColor(SURFACE)
                        .clipPolicy(ClipPolicy.CLIP_BOUNDS)
                        .position(box(40, 40), 10, 0, LayerAlign.TOP_LEFT).build())
                .addParagraph("Below"));

        assertThat(detailOf(report, "clipped shape container")).isEqualTo(PAST_ITS_OUTLINE);
        assertThat(detailOf(toItsBox, "clipped shape container")).isEqualTo(PAST_ITS_BOX);
    }

    @Test
    void aPhotoFillingItsCircleIsCroppedToIt() throws Exception {
        // Written with the ellipse as its geometry, the picture shows only what the page shows.
        Consumer<PageFlowBuilder> avatar = page -> page
                .addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Avatar").circle(60).fillColor(INK)
                        .clipPolicy(ClipPolicy.CLIP_PATH)
                        .center(new ImageBuilder().name("Photo").source(DocumentImageData.fromBytes(png()))
                                .size(60, 60).build())
                        .build())
                .addParagraph("Below");
        DocxExportReport report = reportOf(avatar);

        assertThat(bodyOf(avatar)).as("the picture written with the ellipse as its geometry")
                .containsPattern("<pic:pic.*prst=\"ellipse\".*</pic:pic>");
        assertThat(report.bySubject()).doesNotContainKey("clipped shape container");
        assertThat(detailOf(report, "shape container")).as("its layers, with no word of a clip")
                .isEqualTo("its layers are written inline, one after another in source order");
    }

    @Test
    void aBadgeNamesTheInitialsItsCircleCuts() throws Exception {
        // Held in the badge's shape, a letter taller than its disc runs past it; the page cuts it.
        Consumer<PageFlowBuilder> badge = page -> page
                .addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Badge").circle(20).fillColor(INK)
                        .center(new ParagraphBuilder().text("W")
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(30)).build())
                        .build())
                .addParagraph("Below");
        DocxExportReport report = reportOf(badge);
        DocxExportReport fitting = reportOf(page -> page
                .addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Badge").circle(40).fillColor(INK)
                        .center(new ParagraphBuilder().text("JR").build())
                        .build())
                .addParagraph("Below"));

        assertThat(bodyOf(badge)).as("written as a badge holding its letters")
                .containsPattern("<w:txbxContent>.*>W<");
        assertThat(detailOf(report, "clipped shape container")).isEqualTo(PAST_ITS_OUTLINE);
        assertThat(fitting.bySubject()).doesNotContainKey("clipped shape container");
    }

    @Test
    void aTitleAndItsDatesInAPillNameTheClipThatCutsTheTitle() throws Exception {
        // Written as one line with a tab, set from the pill's left: its first letters, set past it,
        // show.
        Consumer<PageFlowBuilder> pill = page -> page
                .addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Pill").roundedRect(240, 20, 10).fillColor(SURFACE)
                        .clipPolicy(ClipPolicy.CLIP_PATH)
                        .position(new ParagraphBuilder().name("Title").text("Senior engineer").build(),
                                -20, 0, LayerAlign.CENTER_LEFT)
                        .position(new ParagraphBuilder().name("Dates").text("2020 - 2024").build(),
                                -8, 0, LayerAlign.CENTER_RIGHT)
                        .build())
                .addParagraph("Below");
        DocxExportReport report = reportOf(pill);

        assertThat(bodyOf(pill)).as("written as one line, the dates after a tab")
                .containsPattern(">Senior engineer<.*<w:tab/>.*>2020 - 2024<");
        assertThat(detailOf(report, "clipped shape container")).isEqualTo(PAST_ITS_OUTLINE);
    }

    @Test
    void aContainerLaidOverTheFlowNamesTheClipThatCutsItsOrnament() throws Exception {
        // A sidebar the page gives no room, its ornament set past its side: LumaStudioInvoice's.
        double stub = 120;
        DocumentNode sidebar = new ShapeContainerBuilder().name("Sidebar")
                .rectangle(100, stub).clipPolicy(ClipPolicy.CLIP_BOUNDS)
                .margin(new DocumentInsets(-40, 0, 40 - stub, -40))
                .position(new ShapeBuilder().name("BrandBlock").size(100, stub).fillColor(INK).build(),
                        0, 0, LayerAlign.TOP_LEFT, 0)
                .position(new ShapeBuilder().name("Ornament").size(80, 80).fillColor(SURFACE).build(),
                        60, 20, LayerAlign.TOP_LEFT, 1)
                .position(new ParagraphBuilder().name("Monogram").text("L").build(), 10, 10, LayerAlign.TOP_LEFT, 2)
                .build();
        DocxExportReport report = reportOf(page -> page.add(sidebar).addParagraph("Masthead"));

        assertThat(detailOf(report, "clipped shape container")).isEqualTo(PAST_ITS_BOX);
        assertThat(report.bySubject().get("ParagraphNode")).extracting(DocxExportReport.Note::detail)
                .as("written over the flow").anyMatch(detail -> detail.startsWith("laid over the flow"));
    }

    @Test
    void aClipComposedInATableCellIsNamedOnTheTable() throws Exception {
        // Composed in a cell, a chip has no place of its own: its clip is among its table's
        // fragments. A label wrapping below the chip runs past it.
        DocxExportReport report = reportOf(page -> page.addTable(table -> table.name("Rota")
                .columns(DocumentTableColumn.fixed(200))
                .rowCells(DocumentTableCell.node(chip("A long label run past its chip")))));
        DocxExportReport fitting = reportOf(page -> page.addTable(table -> table.name("Rota")
                .columns(DocumentTableColumn.fixed(200))
                .rowCells(DocumentTableCell.node(chip("Early")))));

        assertThat(detailOf(report, "clipped cell content"))
                .isEqualTo("a clip composed in its cells is not in the file, so what is painted past it is written whole");
        assertThat(report.bySubject().get("clipped cell content").get(0).path()).endsWith("Rota[0]");
        assertThat(fitting.bySubject()).doesNotContainKey("clipped cell content");
    }

    @Test
    void aContainedPictureIsMeasuredWhereThePageDrawsIt() throws Exception {
        // A wide picture contained in a box taller than its tile: the page draws it a strip
        // across the tile's middle, and the file writes it that size.
        DocxExportReport report = reportOf(page -> page
                .addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Tile").rectangle(60, 60).fillColor(SURFACE)
                        .clipPolicy(ClipPolicy.CLIP_BOUNDS)
                        .center(new ImageBuilder().name("Logo").source(DocumentImageData.fromBytes(png(100, 20)))
                                .size(60, 70).fitMode(DocumentImageFitMode.CONTAIN).build())
                        .build())
                .addParagraph("Below"));

        assertThat(report.bySubject()).doesNotContainKey("clipped shape container");
    }

    @Test
    void anOuterClipIsNotNamedForWhatAClipInsideItCutsAway() throws Exception {
        // A disc in a card holds a bar running past both: the disc cuts it inside the card, and
        // only the disc's clip loses anything.
        DocxExportReport report = reportOf(page -> page
                .addParagraph("Above")
                .addLayerStack(card -> card.name("Card").clipToBounds()
                        .layer(box(100, 100), LayerAlign.TOP_LEFT, 0)
                        .layer(new ShapeContainerBuilder().name("Disc").circle(40).fillColor(SURFACE)
                                .clipPolicy(ClipPolicy.CLIP_PATH).center(box(40, 160)).build(),
                                LayerAlign.CENTER, 1))
                .addParagraph("Below"));

        assertThat(report.bySubject()).doesNotContainKey("clipped layer stack");
        assertThat(detailOf(report, "clipped shape container")).isEqualTo(PAST_ITS_OUTLINE);
    }

    @Test
    void withNoLayoutEveryClipIsNamedItsCutNotMeasured() throws Exception {
        // A bare export has nothing to measure by: a clip is named whether it cuts or not.
        DocxExportReport report = DocxExports.reportWithoutLayout(400, 600, 40, page -> page
                .addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Badge").circle(40).fillColor(INK)
                        .center(new ParagraphBuilder().text("JR").build()).build())
                .addLayerStack(stack -> stack.name("Icon").clipToBounds().layer(box(20, 20)))
                .add(new ShapeContainerBuilder().name("Free").circle(40).fillColor(INK)
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE).center(box(10, 10)).build())
                .addParagraph("Below"));

        assertThat(detailOf(report, "clipped shape container")).isEqualTo(PAST_ITS_OUTLINE
                + " — with no layout behind the export, whether they do is not measured");
        assertThat(detailOf(report, "clipped layer stack")).isEqualTo(PAST_ITS_BOX
                + " — with no layout behind the export, whether they do is not measured");
    }

    @Test
    void aTurnedContainerWhoseOutlineDrawsNothingNamesItsTransform() throws Exception {
        DocxExportReport unpainted = reportOf(page -> page
                .addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Turned").rectangle(120, 30)
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE).transform(DocumentTransform.rotate(30))
                        .center(new ParagraphBuilder().text("Turned").build()).build())
                .addParagraph("Below"));
        DocxExportReport painted = reportOf(page -> page
                .addParagraph("Above")
                .add(new ShapeContainerBuilder().name("Turned").rectangle(120, 30).fillColor(SURFACE)
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE).transform(DocumentTransform.rotate(30))
                        .center(new ParagraphBuilder().text("Turned").build()).build())
                .addParagraph("Below"));

        assertThat(detailOf(unpainted, "ShapeContainerNode"))
                .isEqualTo("its transform is not carried, so what it holds stands upright at its size");
        // Its drawn outline's note names the transform; nothing more is said of it.
        assertThat(detailOf(painted, "ShapeContainerNode"))
                .startsWith("drawn as a shape").contains("its transform is not carried, so it is drawn upright");
    }

    private static DocumentNode chip(String label) {
        return new ShapeContainerBuilder().name("Chip").roundedRect(60, 10, 5).fillColor(SURFACE)
                .clipPolicy(ClipPolicy.CLIP_PATH)
                .center(new ParagraphBuilder().text(label).textStyle(DocumentTextStyle.DEFAULT.withSize(7)).build())
                .build();
    }

    private static DocumentNode box(double width, double height) {
        return new ShapeBuilder().size(width, height).fillColor(INK).build();
    }

    private static String detailOf(DocxExportReport report, String subject) {
        List<DocxExportReport.Note> notes = report.bySubject().get(subject);
        assertThat(notes).as("the note on the %s", subject).isNotNull().hasSize(1);
        return notes.get(0).detail();
    }

    private static DocxExportReport reportOf(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> captured = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 600)
                .margin(DocumentInsets.of(40))
                .create()) {
            session.pageFlow(content::accept);
            session.export(new DocxSemanticBackend(captured::set));
        }
        assertThat(captured.get()).as("the sink is called once the bytes exist").isNotNull();
        return captured.get();
    }

    private static String bodyOf(Consumer<PageFlowBuilder> content) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(40)).create()) {
            session.pageFlow(content::accept);
            try (org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument(
                    new java.io.ByteArrayInputStream(session.export(new DocxSemanticBackend())))) {
                return document.getDocument().getBody().xmlText();
            }
        }
    }

    private static byte[] png() {
        return png(20, 20);
    }

    private static byte[] png(int width, int height) {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(width, height,
                    java.awt.image.BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }
}
