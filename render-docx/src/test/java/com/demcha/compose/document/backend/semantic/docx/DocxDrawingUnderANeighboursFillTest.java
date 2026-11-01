package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A shaded cell's fill is part of the text layer in both Word and LibreOffice, and both paint it
 * over whatever is drawn behind that layer. So a drawing that stands beside a painted panel —
 * not inside it — and whose box reaches into that panel is covered by the panel's shading, and
 * the reader never sees that much of it.
 *
 * <p>Measured on the fidelity corpus in Word 16.0.20527 and LibreOffice 24.2, at 300 dpi: the one
 * document of the seventy-one that holds the case lays an 18pt icon over a card's top right
 * corner through a layer stack. The engine's PDF draws 4308 pixels of the icon's own ink; Word's
 * draws none, and LibreOffice's draws none — not one pixel anywhere on the page, the icon's whole
 * box coming back the card's colour. Until the stack's order is carried into the document, the
 * export says so rather than losing it in silence.</p>
 *
 * <p>A drawing standing <em>inside</em> a painted panel is a different case with its own note:
 * there the export already puts it in front wherever it frames no text of the flow. Measured the
 * same way, that case has no instance at all in the corpus — its branch is taken 55 times over 14
 * documents and goes in front every one of them.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxDrawingUnderANeighboursFillTest {

    private static final DocumentColor BAND = DocumentColor.rgb(90, 120, 200);
    private static final DocumentColor ACCENT = DocumentColor.rgb(26, 86, 148);

    private static final String COVERED = "a panel painted beside it";

    @Test
    void aShapeOverAPaintedNeighbourIsNamedAsCoveredByIt() throws Exception {
        DocxExportReport report = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Band").fillColor(BAND).fixedWidth(200)
                                .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new ShapeBuilder().name("Pin").size(30, 14).fillColor(ACCENT).build(), 150, 2, LayerAlign.TOP_LEFT, 1)
                        .build())));

        assertThat(covered(report)).singleElement().asString()
                .as("the pin, and the panel that covers it, both named")
                .contains("Pin").contains("Band[0]").contains(COVERED);
    }

    @Test
    void aShapeOverAnUnpaintedNeighbourIsNotNamed() throws Exception {
        // The one difference from the fixture above. Without the fill there is nothing to paint
        // over the pin, and Word draws all 3237 pixels of it against the engine's 3237.
        DocxExportReport report = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Band").fixedWidth(200)
                                .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new ShapeBuilder().name("Pin").size(30, 14).fillColor(ACCENT).build(), 150, 2, LayerAlign.TOP_LEFT, 1)
                        .build())));

        assertThat(covered(report)).isEmpty();
    }

    @Test
    void aShapeBesideAPaintedNeighbourItDoesNotReachIsNotNamed() throws Exception {
        // Set past the band's right edge: the two share a line and no area, and nothing of the
        // pin is under the band's shading. This is the geometry doing the deciding — the pin is
        // drawn behind the text here exactly as it is in the fixture that is named.
        DocxExportReport report = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Band").fillColor(BAND).fixedWidth(200)
                                .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new ShapeBuilder().name("Pin").size(30, 14).fillColor(ACCENT).build(), 210, 2, LayerAlign.TOP_LEFT, 1)
                        .build())));

        assertThat(covered(report)).isEmpty();
    }

    @Test
    void aShapeInsideAPaintedPanelAndBehindItsTextIsNotNamedAsCoveredByIt() throws Exception {
        // Inside the panel, not beside it. The track lies under the label laid over it, so it is
        // one of the few shapes a painted panel keeps behind the text — the export's own case,
        // which has a note of its own — and the box it lies in is that same panel's. Only the
        // test of which box holds which keeps it from being named here as well, so that test is
        // what this fixture exercises: a shape the panel puts in front would never reach it.
        DocxExportReport report = export(session -> session.pageFlow(page -> page
                .addSection("Card", card -> card.fillColor(BAND)
                        .addLayerStack(stack -> stack
                                .layer(new ShapeBuilder().name("Track").size(200, 20)
                                        .fillColor(ACCENT).build(), LayerAlign.TOP_LEFT)
                                .layer(new ParagraphBuilder().name("Label").text("80%").build(),
                                        LayerAlign.CENTER)))));

        assertThat(report.notes())
                .as("the track is behind the text, so it does reach the test of which box holds it")
                .anyMatch(note -> note.detail() != null
                                  && note.detail().contains("behind the text it frames"));
        assertThat(covered(report)).isEmpty();
    }

    @Test
    void aBadgeInFrontOverAPaintedNeighbourIsNotNamedAsCoveredByIt() throws Exception {
        // A badge holding its initials is handed to the queue already in front, with the flag for
        // the call saying otherwise. Standing over a painted neighbour it is seen plainly, so it
        // must not be named as hidden under it.
        DocxExportReport report = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Band").fillColor(BAND).fixedWidth(200)
                                .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new ShapeContainerBuilder().name("Badge").circle(28)
                                        .fillColor(ACCENT)
                                        .center(new ParagraphBuilder().text("JR").build()).build(),
                                150, 2, LayerAlign.TOP_LEFT, 1)
                        .build())));

        assertThat(covered(report)).isEmpty();
    }

    @Test
    void aShapeOverANeighbourBearingBordersAloneIsNotNamed() throws Exception {
        // A panel is named as covering only where a fill was actually written for it. One bearing
        // borders alone writes no shading, so there is nothing to paint over the pin, and saying
        // the pin is hidden under it would be false. This is why the panels are taken where the
        // export writes their fill rather than read off the tree, where a stroke and a fill look
        // alike.
        DocxExportReport report = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Band")
                                .stroke(DocumentStroke.of(ACCENT, 1)).fixedWidth(200)
                                .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new ShapeBuilder().name("Pin").size(30, 14).fillColor(ACCENT).build(),
                                150, 2, LayerAlign.TOP_LEFT, 1)
                        .build())));

        assertThat(covered(report)).isEmpty();
    }

    @Test
    void aShapeOverAPaintedNeighbourInAnEarlierSectionIsNamedToo() throws Exception {
        // A section's pages are counted from its own first, and a page's height is its section's,
        // so a drawing and a panel are only ever judged against each other within one section. The
        // pin of each section stands over that section's band; neither may be missed, and neither
        // may be matched against the other section's band.
        List<DocxExportReport.Note> notes = exportSections(
                page -> page.add(stack("Band one", "Pin one")),
                page -> page.addParagraph(p -> p.text("A section of plain text, no panel in it.")),
                page -> page.add(stack("Band two", "Pin two")));

        assertThat(notes.stream()
                .filter(note -> note.detail() != null && note.detail().contains(COVERED))
                .map(note -> note.path() + " -> " + note.detail().replaceAll(".*box of ([^,]+),.*", "$1")))
                .as("each section's pin named against its own section's band")
                .hasSize(2)
                .anySatisfy(named -> assertThat(named).contains("Pin one").contains("Band one"))
                .anySatisfy(named -> assertThat(named).contains("Pin two").contains("Band two"));
    }

    /** A band painted on the lower layer with a pin set over it on the layer above. */
    private static com.demcha.compose.document.node.DocumentNode stack(String band, String pin) {
        return new LayerStackBuilder().name("Stack")
                .layer(new SectionBuilder().name(band).fillColor(BAND).fixedWidth(200)
                        .addParagraph(p -> p.text("Stage")).build(), LayerAlign.TOP_LEFT, 0)
                .position(new ShapeBuilder().name(pin).size(30, 14).fillColor(ACCENT).build(),
                        150, 2, LayerAlign.TOP_LEFT, 1)
                .build();
    }

    /** The notes of a document of several sections, each a page of its own content. */
    @SafeVarargs
    private static List<DocxExportReport.Note> exportSections(
            Consumer<com.demcha.compose.document.dsl.PageFlowBuilder>... contents) throws Exception {
        List<DocxExportReport.Note> notes = new java.util.ArrayList<>();
        var documents = GraphCompose.documents();
        for (Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content : contents) {
            DocumentSession session = GraphCompose.document().pageSize(400, 400)
                    .margin(DocumentInsets.of(20)).create();
            session.pageFlow(content::accept);
            documents = documents.section(session);
        }
        try (com.demcha.compose.document.api.MultiSectionDocument document = documents.create()) {
            document.export(DocxSemanticBackend.builder()
                    .reportSink(report -> notes.addAll(report.notes())).build());
        }
        return notes;
    }

    /** The notes naming a drawing a panel beside it covers. */
    private static List<String> covered(DocxExportReport report) {
        return report.notes().stream()
                .filter(note -> note.detail() != null && note.detail().contains(COVERED))
                .map(note -> note.subject() + " " + note.path() + ": " + note.detail())
                .toList();
    }

    private static DocxExportReport export(Consumer<DocumentSession> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400)
                .margin(DocumentInsets.of(20)).create()) {
            content.accept(session);
            session.export(new DocxSemanticBackend(report::set));
        }
        return report.get();
    }
}
