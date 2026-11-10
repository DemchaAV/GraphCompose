package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A layer stands across where its offset moves it.
 *
 * <p>Measured in Word 16 and LibreOffice on Windows: a stack's one layer moved 40pt in stood at
 * the stack's left edge in both, and a band's layer the page sets 20pt left of the stack stood at
 * the stack's edge, the page margin, in both. Held to its box, in by the offset and out past an
 * edge by an indent below zero, each stood where the page sets it, in both, and a layer of three
 * lines moved 40pt in broke at the page's words. An offset down or up moves a layer without making
 * room for it, which the flow Word writes cannot do: that is named.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxLayerOffsetTest {

    private static final DocumentColor LIGHT = DocumentColor.rgb(220, 230, 245);
    private static final String WRAPPING = "An offset layer of several words that wraps onto a second and a third line";

    @Test
    void aStacksOneLayerMovedAcrossKeepsThePagesMeasure() throws Exception {
        Exported exported = export(page -> page.addLayerStack(stack -> stack
                .position(text(WRAPPING), 40, 0, LayerAlign.TOP_LEFT)));

        XWPFParagraph moved = exported.paragraph(WRAPPING);
        assertThat(left(moved)).as("held in 40pt").isCloseTo(800L, within(2L));
        assertThat(right(moved)).as("and out 40pt past the right edge, as wide as the page sets it")
                .isCloseTo(-800L, within(2L));
        assertThat(exported.offsetNotes()).as("set as the page sets it").isEmpty();
    }

    @Test
    void aStacksOneLayerTheOffsetMovesDownIsNamedAndWrittenWhereTheStackStarts() throws Exception {
        Exported exported = export(page -> page.addLayerStack(stack -> stack
                .position(text("Moved"), 40, 10, LayerAlign.TOP_LEFT)));

        assertThat(left(exported.paragraph("Moved"))).as("held in 40pt still").isCloseTo(800L, within(2L));
        assertThat(exported.paragraph("Moved").getSpacingBefore()).as("written where the stack starts")
                .isLessThanOrEqualTo(0);
        assertThat(exported.offsetNotes()).singleElement().satisfies(note -> {
            assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.APPROXIMATED);
            assertThat(note.path()).endsWith("ParagraphNode[0]");
            assertThat(note.detail()).isEqualTo("the page sets it 10pt lower than its stack starts, without making "
                                                + "room for it; Word writes it where the stack starts");
        });
    }

    @Test
    void aStacksOneLayerTheOffsetMovesUpIsNamedHigher() throws Exception {
        Exported exported = export(page -> page.addLayerStack(stack -> stack
                .position(text("Moved"), 0, -6, LayerAlign.TOP_LEFT)));

        assertThat(exported.paragraph("Moved").getCTP().getPPr().getInd()).as("not moved across").isNull();
        assertThat(exported.offsetNotes()).singleElement().extracting(DocxExportReport.Note::detail)
                .asString().startsWith("the page sets it 6pt higher than its stack starts");
    }

    @Test
    void aStacksOneLayerSetInByItsMarginAndTheStacksPaddingIsNotNamed() throws Exception {
        Exported exported = export(page -> page.addLayerStack(stack -> stack.padding(6)
                .layer(new ParagraphBuilder().text("Set in").margin(DocumentInsets.top(8)).build(), LayerAlign.TOP_LEFT)));

        assertThat(exported.paragraph("Set in").getText()).as("the premise: written").isEqualTo("Set in");
        assertThat(exported.offsetNotes()).isEmpty();
    }

    @Test
    void aStacksOneLayerThatOnlyDrawsIsDrawnWhereThePageDrawsItUnnamed() throws Exception {
        Exported exported = export(page -> page.addLayerStack(stack -> stack
                .position(new ShapeBuilder().size(40, 10).fillColor(LIGHT).build(), 30, 10, LayerAlign.TOP_LEFT)));

        assertThat(exported.offsetNotes()).isEmpty();
    }

    @Test
    void aStacksOneLayerThatOnlyDrawsInABandIsDrawnWhereThePageDrawsItUnnamed() throws Exception {
        // In a band it is written with the band's layer, not held as room in the flow.
        DocumentNode mark = new LayerStackBuilder()
                .position(new ShapeBuilder().size(40, 10).fillColor(LIGHT).build(), 30, 10, LayerAlign.TOP_LEFT)
                .build();
        Exported exported = export(page -> page.addLayerStack(stack -> stack
                .layer(new ShapeBuilder().size(200, 60).fillColor(LIGHT).build(), LayerAlign.TOP_LEFT)
                .layer(new SectionBuilder().addParagraph("Title").add(mark).build(), LayerAlign.TOP_LEFT)));

        assertThat(exported.paragraph("Title")).as("the premise: the band is written").isNotNull();
        assertThat(exported.offsetNotes()).isEmpty();
    }

    @Test
    void aStacksOneLayerABandWritesFirstIsSetDownByTheBandUnnamed() throws Exception {
        // The band sets the page's distance down to its first block, the offset in it.
        DocumentNode label = new LayerStackBuilder().position(text("Label"), 0, 10, LayerAlign.TOP_LEFT).build();
        Exported exported = export(page -> page.addLayerStack(stack -> stack
                .layer(new ShapeBuilder().size(200, 60).fillColor(LIGHT).build(), LayerAlign.TOP_LEFT)
                .layer(label, LayerAlign.TOP_LEFT)));

        assertThat(exported.paragraph("Label").getSpacingBefore()).as("set the offset down").isGreaterThan(150);
        assertThat(exported.offsetNotes()).isEmpty();
    }

    @Test
    void aBandsLayerSetLeftOfTheStackStandsOutPastItsEdge() throws Exception {
        Exported exported = export(page -> page.addLayerStack(stack -> stack
                .layer(new ShapeBuilder().size(200, 60).fillColor(LIGHT).build(), LayerAlign.TOP_LEFT)
                .position(text("Left"), -20, 5, LayerAlign.TOP_LEFT)
                .position(text("Right"), 120, 30, LayerAlign.TOP_LEFT)
                .layer(new ParagraphBuilder().text("Flush").align(TextAlign.RIGHT).build(), LayerAlign.BOTTOM_LEFT)));

        assertThat(left(exported.paragraph("Left"))).as("20pt out into the margin").isCloseTo(-400L, within(2L));
        assertThat(left(exported.paragraph("Right"))).as("and the one inside held in").isCloseTo(2400L, within(2L));
        CTInd flush = exported.paragraph("Flush").getCTP().getPPr().getInd();
        assertThat(flush == null || !flush.isSetLeft() ? 0L : DocxTwips.of(flush.getLeft()))
                .as("one set at the edge stays inside it, its room to spare on its left").isGreaterThanOrEqualTo(0L);
        assertThat(exported.offsetNotes()).isEmpty();
    }

    @Test
    void aBandsLayerSetLeftOfItsEdgeInARowsCellIsWrittenAtTheEdgeAndNamed() throws Exception {
        Exported exported = export(page -> page.addRow(row -> row.add(leftBand()).addParagraph("Beside")));

        assertThat(indentOf(exported.cellParagraph("Left"))).as("the cell's text starts at the cell's own").isNull();
        assertThat(exported.offsetNotes()).singleElement().satisfies(note -> {
            assertThat(note.path()).endsWith("ParagraphNode[1]");
            assertThat(note.detail()).isEqualTo("the page sets it 20pt left of the edge it is written from; in its "
                                                + "table cell it is written at that edge");
        });
    }

    @Test
    void aBandsLayerSetLeftOfItsEdgeInARowsCellStandsAsFarLeftAsItsIndentGoes() throws Exception {
        Exported exported = export(page -> page.addRow(row -> row
                .add(new SectionBuilder().addParagraph("Head").add(new SectionBuilder()
                        .padding(new DocumentInsets(0, 0, 0, 12)).add(leftBand()).build()).build())
                .addParagraph("Beside")));

        assertThat(indentOf(exported.cellParagraph("Left"))).as("12pt in, all of it given back").isNull();
        assertThat(exported.offsetNotes()).singleElement().extracting(DocxExportReport.Note::detail)
                .isEqualTo("the page sets it 20pt left of the edge it is written from; in its table cell it stands "
                           + "only 12pt left of that edge");
    }

    @Test
    void aBandsLayerSetLeftOfItsEdgeWithinTheIndentInARowsCellStandsWhereThePageSetsIt() throws Exception {
        Exported exported = export(page -> page.addRow(row -> row
                .add(new SectionBuilder().addParagraph("Head").add(new SectionBuilder()
                        .padding(new DocumentInsets(0, 0, 0, 24)).add(leftBand()).build()).build())
                .addParagraph("Beside")));

        assertThat(indentOf(exported.cellParagraph("Left"))).as("24pt in, 20pt of it given back")
                .isCloseTo(80L, within(2L));
        assertThat(exported.offsetNotes()).isEmpty();
    }

    @Test
    void aBandsDrawingSetLeftOfItsEdgeInARowsCellIsDrawnWhereThePageDrawsItUnnamed() throws Exception {
        DocumentNode band = new LayerStackBuilder()
                .layer(new ShapeBuilder().size(120, 40).fillColor(LIGHT).build(), LayerAlign.TOP_LEFT)
                .position(new ShapeBuilder().size(4, 40).fillColor(DocumentColor.BLACK).build(), -8, 0, LayerAlign.TOP_LEFT)
                .position(text("Text"), 10, 5, LayerAlign.TOP_LEFT)
                .build();
        Exported exported = export(page -> page.addRow(row -> row.add(band).addParagraph("Beside")));

        assertThat(exported.cellParagraph("Text")).as("the premise: the band is written").isNotNull();
        assertThat(exported.offsetNotes()).isEmpty();
    }

    @Test
    void aStacksOneLayerMovedAcrossInARowsCellIsWrittenWhereTheStackStartsAndNamed() throws Exception {
        DocumentNode moved = new LayerStackBuilder().position(text("Moved"), 30, 0, LayerAlign.TOP_LEFT).build();
        Exported exported = export(page -> page.addRow(row -> row.add(moved).addParagraph("Beside")));

        assertThat(indentOf(exported.cellParagraph("Moved"))).as("not held in").isNull();
        assertThat(exported.offsetNotes()).singleElement().extracting(DocxExportReport.Note::detail)
                .isEqualTo("the page sets it 30pt right of the stack's edge, in a table cell, whose text Word holds "
                           + "inside it; Word writes it where the stack starts");
    }

    @Test
    void aStacksOneLayerMovedInATablesComposedCellIsNamedByItsOffset() throws Exception {
        // Composed in a table cell, the stack has no place of its own in the layout.
        DocumentNode moved = new LayerStackBuilder().position(text("Moved"), 30, 6, LayerAlign.TOP_LEFT).build();
        Exported exported = export(page -> page.addTable(table -> table
                .columns(com.demcha.compose.document.table.DocumentTableColumn.fixed(120),
                        com.demcha.compose.document.table.DocumentTableColumn.fixed(120))
                .rowCells(com.demcha.compose.document.table.DocumentTableCell.node(moved),
                        com.demcha.compose.document.table.DocumentTableCell.node(text("Beside")))));

        assertThat(indentOf(exported.cellParagraph("Moved"))).as("not held in").isNull();
        assertThat(exported.offsetNotes()).singleElement().extracting(DocxExportReport.Note::detail)
                .isEqualTo("with no place of its own in the layout, it is written where the stack starts, its offset "
                           + "of 30pt across and 6pt down not written");
    }

    private static DocumentNode leftBand() {
        return new LayerStackBuilder()
                .layer(new ShapeBuilder().size(120, 40).fillColor(LIGHT).build(), LayerAlign.TOP_LEFT)
                .position(text("Left"), -20, 5, LayerAlign.TOP_LEFT)
                .position(text("Under"), 0, 25, LayerAlign.TOP_LEFT)
                .build();
    }

    private static DocumentNode text(String text) {
        return new ParagraphBuilder().text(text).build();
    }

    private static long left(XWPFParagraph paragraph) {
        Long left = indentOf(paragraph);
        assertThat(left).as("%s is indented", paragraph.getText()).isNotNull();
        return left;
    }

    private static long right(XWPFParagraph paragraph) {
        CTInd indent = paragraph.getCTP().getPPr().getInd();
        assertThat(indent != null && indent.isSetRight()).as("%s has a right indent", paragraph.getText()).isTrue();
        return DocxTwips.of(indent.getRight());
    }

    /** The paragraph's left indent in twips, or {@code null} where none is written or it is zero. */
    private static Long indentOf(XWPFParagraph paragraph) {
        CTInd indent = paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr().getInd() : null;
        if (indent == null || !indent.isSetLeft()) {
            return null;
        }
        long left = DocxTwips.of(indent.getLeft());
        return left == 0 ? null : left;
    }

    private record Exported(XWPFDocument document, List<DocxExportReport.Note> notes) {

        XWPFParagraph paragraph(String text) {
            return document.getParagraphs().stream().filter(paragraph -> paragraph.getText().equals(text))
                    .findFirst().orElseThrow();
        }

        XWPFParagraph cellParagraph(String text) {
            return document.getTables().stream()
                    .flatMap(table -> table.getRows().stream())
                    .flatMap(row -> row.getTableCells().stream())
                    .flatMap(cell -> cell.getParagraphs().stream())
                    .filter(paragraph -> paragraph.getText().equals(text))
                    .findFirst().orElseThrow();
        }

        List<DocxExportReport.Note> offsetNotes() {
            return notes.stream().filter(note -> note.subject().equals("layer offset")).toList();
        }
    }

    private static Exported export(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(72)).create()) {
            session.pageFlow(page -> {
                page.addParagraph("Before");
                content.accept(page);
                page.addParagraph("After");
            });
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        assertThat(report.get().notes()).noneMatch(note -> note.subject().equals("measured geometry"));
        return new Exported(new XWPFDocument(new ByteArrayInputStream(docx)), report.get().notes());
    }
}
