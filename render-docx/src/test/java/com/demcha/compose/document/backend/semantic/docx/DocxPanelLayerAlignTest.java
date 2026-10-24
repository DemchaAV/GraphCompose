package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A shape written as a panel in a table cell sets its layers where their alignment sets them.
 *
 * <p>Measured in Word 16 and LibreOffice on Windows, in a 100×60 panel: a label set at the
 * top-left corner stood at the middle, 22.5pt low, and one set at the bottom-right corner at the
 * middle and at the left, 46pt left of the page's — the panel's cell was always written centred.
 * Set at the top, the foot or the middle as their layer is, and held in across to their box, both
 * stood where the page sets them, in both.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxPanelLayerAlignTest {

    private static final DocumentColor LIGHT = DocumentColor.rgb(220, 230, 245);
    private static final double ROOM = 100;

    @Test
    void aLayerAtTheTopSetsThePanelsCellAtTheTop() throws Exception {
        Exported exported = export(panel(text("TopLeft"), LayerAlign.TOP_LEFT, 0, 0));

        assertThat(exported.vertical()).as("Word's own default, not written").isNull();
        assertThat(exported.left("TopLeft")).isZero();
        assertThat(exported.right("TopLeft")).as("set from the left as its layer is, not held to its box").isZero();
        assertThat(exported.offsetNotes()).isEmpty();
    }

    @Test
    void aLayerAtTheFootSetsThePanelsCellAtTheFootAndItsBoxAtItsSide() throws Exception {
        Exported exported = export(panel(text("BotRight"), LayerAlign.BOTTOM_RIGHT, 0, 0));

        assertThat(exported.vertical()).isEqualTo("bottom");
        assertThat(exported.left("BotRight")).as("held in to its box at the right")
                .isCloseTo(Math.round((ROOM - exported.widthOf("BotRight")) * 20), within(2L));
        assertThat(exported.offsetNotes()).isEmpty();
    }

    @Test
    void aCentredLayerSetsThePanelsCellAtTheMiddleAndALeftParagraphInItsMiddle() throws Exception {
        Exported exported = export(panel(text("Middle"), LayerAlign.CENTER, 0, 0));

        assertThat(exported.vertical()).isEqualTo("center");
        double width = exported.widthOf("Middle");
        assertThat(exported.left("Middle")).as("its box centred, its text set from the box's left")
                .isCloseTo(Math.round((ROOM - width) / 2 * 20), within(2L));
        // On the right, the room to spare a line set from its left is given, and what Word's face
        // asks of its measure.
        assertThat(exported.right("Middle")).as("held in on the right to its box too")
                .isCloseTo(Math.round(((ROOM - width) / 2 - Math.max(2, width * 0.05)) * 20), within(30L));
    }

    @Test
    void aParagraphLeaningAsItsLayerIsNotHeldIn() throws Exception {
        Exported exported = export(panel(new ParagraphBuilder().text("Middle").align(TextAlign.CENTER).build(),
                LayerAlign.CENTER, 0, 0));

        assertThat(exported.left("Middle")).as("centred by its own alignment").isZero();
    }

    @Test
    void anOffsetAcrossMovesTheBox() throws Exception {
        Exported exported = export(panel(text("Moved"), LayerAlign.TOP_LEFT, 10, 0));

        assertThat(exported.left("Moved")).isCloseTo(200L, within(2L));
        assertThat(exported.offsetNotes()).isEmpty();
    }

    @Test
    void anOffsetLeftOfThePanelIsWrittenAtItsEdgeAndNamed() throws Exception {
        Exported exported = export(panel(text("Moved"), LayerAlign.TOP_LEFT, -6, 0));

        assertThat(exported.left("Moved")).isZero();
        assertThat(exported.offsetNotes()).singleElement().extracting(DocxExportReport.Note::detail)
                .isEqualTo("the page sets it 6pt left of the edge it is written from; in its table cell it is "
                           + "written at that edge");
    }

    @Test
    void anOffsetDownIsNamed() throws Exception {
        Exported exported = export(panel(text("Moved"), LayerAlign.TOP_LEFT, 0, 6));

        assertThat(exported.offsetNotes()).singleElement().extracting(DocxExportReport.Note::detail)
                .isEqualTo("written as a panel's content: its offset moves it 6pt down, which its cell does not write");
    }

    @Test
    void layersAtDifferentHeightsAreSetAtTheFirstsAndTheOthersNamed() throws Exception {
        DocumentNode panel = new ShapeContainerBuilder().rectangle(ROOM, 60).fillColor(LIGHT)
                .layer(text("Top"), LayerAlign.TOP_LEFT)
                .layer(text("Foot"), LayerAlign.BOTTOM_LEFT)
                .build();
        Exported exported = export(panel);

        assertThat(exported.vertical()).as("the first's").isNull();
        assertThat(exported.offsetNotes()).singleElement().extracting(DocxExportReport.Note::detail)
                .isEqualTo("written as a panel's content: the page sets it at the foot of the shape, and its cell "
                           + "sets all it holds at the top, it under the layers before it");
        assertThat(exported.offsetNotes()).singleElement().extracting(DocxExportReport.Note::path)
                .as("the layer, composed in a table cell, named under its table by its cell").asString()
                .endsWith("TableNode[1]/row[0]/cell[0]/ShapeContainerNode[0]/ParagraphNode[1]");
    }

    @Test
    void layersAtOneHeightAreWrittenOneUnderTheOtherAndNamed() throws Exception {
        DocumentNode panel = new ShapeContainerBuilder().rectangle(ROOM, 60).fillColor(LIGHT)
                .layer(text("Title"), LayerAlign.TOP_LEFT)
                .layer(text("$9"), LayerAlign.TOP_RIGHT)
                .build();
        Exported exported = export(panel);

        assertThat(exported.left("$9")).as("held in to the right still").isPositive();
        assertThat(exported.offsetNotes()).singleElement().extracting(DocxExportReport.Note::detail)
                .isEqualTo("written as a panel's content: the page sets it at the height of the layers before it, "
                           + "and its cell writes it under them");
    }

    @Test
    void aCentredParagraphMovedAcrossPastTheShapesEdgeIsNamed() throws Exception {
        // A centred paragraph is as wide as the shape: moved 10pt right, it runs 10pt past it.
        Exported exported = export(panel(new ParagraphBuilder().text("Hi").align(TextAlign.CENTER).build(),
                LayerAlign.CENTER, 10, 0));

        assertThat(exported.left("Hi")).as("moved in 10pt, less half the room to spare a centred line is given")
                .isCloseTo(Math.round((10 - Math.max(2, ROOM * 0.05) / 2) * 20), within(2L));
        assertThat(exported.offsetNotes()).singleElement().extracting(DocxExportReport.Note::detail)
                .isEqualTo("written as a panel's content: the page sets it 10pt past the shape's right edge, where "
                           + "its cell's text ends");
    }

    @Test
    void aPaddedShapesLayerIsHeldInItsOutline() throws Exception {
        // The page sets the padding round the outline and the layers in the outline itself.
        DocumentNode panel = new ShapeContainerBuilder().rectangle(ROOM, 60).fillColor(LIGHT).padding(5)
                .position(text("BotRight"), 0, 0, LayerAlign.BOTTOM_RIGHT).build();
        Exported exported = export(panel);

        assertThat(exported.left("BotRight")).as("at the outline's right")
                .isCloseTo(Math.round((ROOM - exported.widthOf("BotRight")) * 20), within(2L));
    }

    @Test
    void aSectionSetAcrossIsWrittenFromTheLeftAndNamed() throws Exception {
        DocumentNode section = new SectionBuilder().addParagraph("Inside").build();
        Exported exported = export(panel(section, LayerAlign.TOP_RIGHT, 0, 0));

        assertThat(exported.offsetNotes()).singleElement().extracting(DocxExportReport.Note::detail)
                .isEqualTo("written as a panel's content: the page sets it across by a box the layout does not give "
                           + "where it is composed in a table cell (SectionNode), and it is written from the shape's "
                           + "left");
    }

    private static DocumentNode panel(DocumentNode layer, LayerAlign align, double dx, double dy) {
        return new ShapeContainerBuilder().rectangle(ROOM, 60).fillColor(LIGHT).position(layer, dx, dy, align).build();
    }

    private static DocumentNode text(String text) {
        return new ParagraphBuilder().text(text).build();
    }

    private record Exported(XWPFDocument document, List<DocxExportReport.Note> notes, List<PlacedFragment> fragments) {

        XWPFTableCell panelCell() {
            return document.getTables().get(0).getRow(0).getCell(0).getTables().get(0).getRow(0).getCell(0);
        }

        /** The panel's cell's vertical alignment, or {@code null} where none is written. */
        String vertical() {
            CTTcPr properties = panelCell().getCTTc().getTcPr();
            return properties == null || !properties.isSetVAlign() ? null : properties.getVAlign().getVal().toString();
        }

        long left(String text) {
            CTInd indent = indentOf(text);
            return indent == null || !indent.isSetLeft() ? 0L : DocxTwips.of(indent.getLeft());
        }

        long right(String text) {
            CTInd indent = indentOf(text);
            return indent == null || !indent.isSetRight() ? 0L : DocxTwips.of(indent.getRight());
        }

        private CTInd indentOf(String text) {
            XWPFParagraph paragraph = panelCell().getParagraphs().stream()
                    .filter(candidate -> candidate.getText().equals(text)).findFirst().orElseThrow();
            return paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr().getInd() : null;
        }

        /** How wide the page lays the paragraph holding some letters out, in points. */
        double widthOf(String letters) {
            return fragments.stream()
                    .filter(fragment -> fragment.payload() instanceof ParagraphFragmentPayload paragraph
                                        && paragraph.lines().stream().flatMap(line -> line.spans().stream())
                                                .anyMatch(span -> span instanceof ParagraphTextSpan run
                                                                  && run.text() != null && run.text().contains(letters)))
                    .findFirst().orElseThrow().width();
        }

        List<DocxExportReport.Note> offsetNotes() {
            return notes.stream().filter(note -> note.subject().equals("layer offset")).toList();
        }
    }

    private static Exported export(DocumentNode panel) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        List<PlacedFragment> fragments;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(72)).create()) {
            session.pageFlow(page -> page
                    .addParagraph("Before")
                    .addTable(table -> table
                            .columns(DocumentTableColumn.fixed(120), DocumentTableColumn.fixed(120))
                            .rowCells(DocumentTableCell.node(panel), DocumentTableCell.node(text("Beside"))))
                    .addParagraph("After"));
            docx = session.export(new DocxSemanticBackend(report::set));
            fragments = session.layoutGraph().fragments();
        }
        assertThat(report.get().notes()).noneMatch(note -> note.subject().equals("measured geometry"));
        return new Exported(new XWPFDocument(new ByteArrayInputStream(docx)), report.get().notes(), fragments);
    }
}
