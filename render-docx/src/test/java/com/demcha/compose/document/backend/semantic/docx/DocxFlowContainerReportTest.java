package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.SpacerBuilder;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentEdge;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a container written as its contents leaves of its own layout is in its report note: a
 * canvas's places and width, a panel's bleed, the fixed width of a layer stack's column, and the
 * keep of a line drawn in the flow.
 *
 * <p>Each was left out in silence: a canvas's caption set at its middle came out at its top, a
 * band bled to the page's edges stopped at its box, and a column fixed narrower than its
 * neighbours left it ran its text their way. A canvas's room is held in Word
 * ({@code DocxCanvasRoomTest}).</p>
 */
class DocxFlowContainerReportTest {

    private static final DocumentColor INK = DocumentColor.rgb(26, 86, 148);
    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);

    @Test
    void aCanvasNamesThePlacesAndWidthItIsWrittenWithout() throws Exception {
        // Its room is held under what it writes (DocxCanvasRoomTest).
        DocxExportReport report = reportOf(page -> page
                .addCanvas(200, 120, canvas -> canvas.position(new ParagraphBuilder()
                        .text("Set forty points in and sixty down, wrapping inside two hundred").build(), 40, 60))
                .addParagraph("Below"));

        assertThat(detailOf(report, "CanvasLayerNode")).isEqualTo("written as its contents; "
                + "what it writes is written from its corner, one block after another, not where it places it; "
                + "its width is not in the file, so its paragraphs and lists run the width of the column it "
                + "stands in");
    }

    @Test
    void aCanvasWritingOneBlockAtItsCornerAsTallAsItLosesNothing() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addCanvas(360, 40, canvas -> canvas.position(new SpacerBuilder().height(40).build(), 0, 0))
                .addParagraph("Below"));

        assertThat(report.bySubject()).doesNotContainKey("CanvasLayerNode");
    }

    @Test
    void aCanvasThatOnlyDrawsAtTheEndOfTheFlowIsNotNamed() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addParagraph("Above")
                .addCanvas(100, 80, canvas -> canvas.position(new ShapeBuilder().size(40, 40).fillColor(INK).build(),
                        20, 20)));

        assertThat(report.bySubject()).doesNotContainKey("CanvasLayerNode");
    }

    @Test
    void aCanvasInAStackOfOneLayerStandsInTheFlowToo() throws Exception {
        // A stack of one layer lays nothing over anything; a template wraps a block in one.
        DocxExportReport report = reportOf(page -> page
                .addLayerStack(stack -> stack.layer(new com.demcha.compose.document.dsl.CanvasLayerBuilder(360, 80)
                        .position(new ParagraphBuilder().text("Caption").build(), 10, 30)
                        .build(), LayerAlign.TOP_LEFT))
                .addParagraph("Below"));

        assertThat(detailOf(report, "CanvasLayerNode")).isEqualTo("written as its contents; "
                + "what it writes is written from its corner, one block after another, not where it places it");
    }

    @Test
    void aTimelinesMarkersAreNotNamed() throws Exception {
        // Each marker is a canvas that only draws, alone in its row's cell, which holds its room.
        DocxExportReport report = reportOf(page -> page
                .addTimeline(timeline -> timeline
                        .entry(com.demcha.compose.document.dsl.TimelineMarker.dot(8, INK), entry -> entry
                                .title("Senior Engineer").meta("2023 - Present"))
                        .entry(com.demcha.compose.document.dsl.TimelineMarker.dot(8, INK), entry -> entry
                                .title("Engineer").meta("2021 - 2023")))
                .addParagraph("Below"));

        assertThat(report.bySubject()).doesNotContainKey("CanvasLayerNode");
    }

    @Test
    void aPanelBledToThePagesEdgesNamesItsBleed() throws Exception {
        Consumer<PageFlowBuilder> bled = page -> page.addSection(band -> band.fillColor(SURFACE)
                .bleedToEdge(DocumentEdge.LEFT, DocumentEdge.RIGHT).addParagraph("A band to the edges"));
        DocxExportReport report = reportOf(bled);

        assertThat(detailOf(report, "SectionNode")).isEqualTo("written as a panel; its bleed is not in the file, "
                + "so its fill and borders stop at its box, not at the page's edge");
        assertThat(bodyOf(bled)).as("the file does not carry it").isEqualTo(bodyOf(page -> page
                .addSection(band -> band.fillColor(SURFACE).addParagraph("A band to the edges"))));
    }

    @Test
    void aPanelInALayerIsNotBledByThePageEither() throws Exception {
        // The page bleeds a section only in the flow it pages.
        DocxExportReport report = reportOf(page -> page.addLayerStack(stack -> stack
                .layer(new SectionBuilder().fillColor(SURFACE).bleedToEdge(DocumentEdge.LEFT, DocumentEdge.RIGHT)
                        .addParagraph("Card").build(), LayerAlign.TOP_LEFT)));

        assertThat(report.bySubject()).doesNotContainKey("SectionNode");
    }

    @Test
    void anUnpaintedSectionsBleedPaintsNothing() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addSection(section -> section.bleedToEdge(DocumentEdge.LEFT).addParagraph("Nothing to bleed")));

        assertThat(report.bySubject()).doesNotContainKey("SectionNode");
    }

    @Test
    void aLineDrawnInTheFlowNamesTheKeepItIsWrittenWithout() throws Exception {
        // Its drawing is anchored in a paragraph near it, and nothing keeps that paragraph with
        // the next block: a page can end between them before a table, a paragraph, in a panel.
        String lost = "; its keep with the next block is not carried, so a page can end between the two";
        DocxExportReport beforeATable = reportOf(page -> page
                .addParagraph("Above")
                .addLine(line -> line.vertical(30).thickness(1).color(INK).keepWithNext())
                .addTable(table -> table.columns(DocumentTableColumn.fixed(200)).row("Kept")));
        DocxExportReport beforeAParagraph = reportOf(page -> page
                .addParagraph("Above")
                .addLine(line -> line.vertical(30).thickness(1).color(INK).keepWithNext())
                .addParagraph("Kept"));
        DocxExportReport inAPanel = reportOf(page -> page
                .addSection(panel -> panel.fillColor(SURFACE)
                        .addLine(line -> line.vertical(30).thickness(1).color(INK).keepWithNext())
                        .addParagraph("Kept")));

        assertThat(detailOf(beforeATable, "LineNode")).startsWith("drawn as a shape").endsWith(lost);
        assertThat(detailOf(beforeAParagraph, "LineNode")).startsWith("drawn as a shape").endsWith(lost);
        assertThat(detailOf(inAPanel, "LineNode")).startsWith("drawn as a shape").endsWith(lost);
    }

    @Test
    void aLineKeptWhereThePageKeepsNothingNamesNoKeep() throws Exception {
        // A row's cell is a box the page places, not its flow; a page break ends the page anyway.
        DocxExportReport inARowCell = reportOf(page -> page.addRow(row -> row.weights(1, 1)
                .addSection(section -> section
                        .addLine(line -> line.vertical(30).thickness(1).color(INK).keepWithNext())
                        .addTable(table -> table.columns(DocumentTableColumn.fixed(100)).row("Kept")))
                .addParagraph("Beside")));
        DocxExportReport beforeABreak = reportOf(page -> page
                .addParagraph("Above")
                .addLine(line -> line.vertical(30).thickness(1).color(INK).keepWithNext())
                .addPageBreak(pageBreak -> { })
                .addParagraph("Next page"));

        assertThat(detailOf(inARowCell, "LineNode")).doesNotContain("keep");
        assertThat(detailOf(beforeABreak, "LineNode")).doesNotContain("keep");
    }

    @Test
    void aPanelBledAcrossPagesNamesItsBleed() throws Exception {
        // The page bleeds it toward the top of every page it stands on.
        DocxExportReport report = reportOf(page -> page.addSection(band -> {
            band.fillColor(SURFACE).bleedToEdge(DocumentEdge.TOP);
            for (int line = 0; line < 60; line++) {
                band.addParagraph("Line " + line + " of a band long enough to run onto a second page");
            }
        }));

        assertThat(report.bySubject().get("SectionNode")).extracting(DocxExportReport.Note::detail)
                .contains("written as a panel; its bleed is not in the file, so its fill and borders stop at its "
                          + "box, not at the page's edge");
    }

    @Test
    void aCanvasThatOnlyDrawsBeforeAPageBreakIsNotNamed() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addCanvas(100, 80, canvas -> canvas.position(new ShapeBuilder().size(40, 40).fillColor(INK).build(),
                        20, 20))
                .addPageBreak(pageBreak -> { })
                .addParagraph("Next page"));

        assertThat(report.bySubject()).doesNotContainKey("CanvasLayerNode");
    }

    @Test
    void aCanvasStackingWhatItWritesFromItsCornerKeepsItsPlaces() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addCanvas(360, 50, canvas -> canvas
                        .position(new SpacerBuilder().height(20).build(), 0, 0)
                        .position(new SpacerBuilder().height(30).build(), 0, 20))
                .addParagraph("Below"));

        assertThat(report.bySubject()).doesNotContainKey("CanvasLayerNode");
    }

    @Test
    void aColumnFixedNarrowerThanItsBandNamesItsWidth() throws Exception {
        // The left column's text runs to its margin, so the stack spans the page; the right
        // column's band runs from its margin to the stack's edge, wider than it is fixed at.
        DocxExportReport report = reportOf(600, page -> page.addLayerStack(stack -> stack
                .layer(new SectionBuilder().margin(new DocumentInsets(0, 300, 0, 0))
                        .addParagraph("A left column whose text is long enough to run to the margin that "
                                      + "keeps it clear of the right one").build(), LayerAlign.TOP_LEFT)
                .layer(new SectionBuilder().name("Right").margin(new DocumentInsets(0, 0, 0, 280)).fixedWidth(150)
                        .addParagraph("A right column long enough to wrap at its fixed width of one hundred and "
                                      + "fifty points").build(), LayerAlign.TOP_LEFT)));

        assertThat(detailOf(report, "SectionNode")).isEqualTo("written as a column of its layer stack; its fixed "
                + "width is not in the file, so its paragraphs and lists run the width of its column");
    }

    private static String detailOf(DocxExportReport report, String subject) {
        List<DocxExportReport.Note> notes = report.bySubject().get(subject);
        assertThat(notes).as("the note on the %s", subject).isNotNull().hasSize(1);
        return notes.get(0).detail();
    }

    private static DocxExportReport reportOf(Consumer<PageFlowBuilder> content) throws Exception {
        return reportOf(400, content);
    }

    private static DocxExportReport reportOf(double pageWidth, Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> captured = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document()
                .pageSize(pageWidth, 600)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(content::accept);
            session.export(new DocxSemanticBackend(captured::set));
        }
        assertThat(captured.get()).as("the sink is called once the bytes exist").isNotNull();
        return captured.get();
    }

    private static String bodyOf(Consumer<PageFlowBuilder> content) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(content::accept);
            try (XWPFDocument document = new XWPFDocument(
                    new ByteArrayInputStream(session.export(new DocxSemanticBackend())))) {
                return document.getDocument().getBody().xmlText();
            }
        }
    }
}
