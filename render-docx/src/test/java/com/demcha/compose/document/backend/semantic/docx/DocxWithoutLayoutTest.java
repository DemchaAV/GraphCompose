package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SpacerBuilder;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.RowArrangement;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What an export with no layout behind it says it leaves to the editor, and what it does with
 * the drawings and the rows only a layout places.
 */
class DocxWithoutLayoutTest {

    private static final String BASE = "this document could not be laid out, so line heights, the space between "
                                       + "lines and auto column widths are the editor's rather than the engine's";

    @Test
    void aDocumentOfParagraphsIsToldWhatItLeavesAndNoMore() throws Exception {
        assertThat(sectionNote(page -> page.addParagraph("Plain text"))).isEqualTo(BASE);
    }

    @Test
    void aRowItsArrangementOrAGrowingSpacerDividesIsNamed() throws Exception {
        String arranged = sectionNote(page -> page.addRow(row -> row.arrangement(RowArrangement.SPACE_BETWEEN)
                .add(paragraph("Total")).add(paragraph("$120"))));
        String grown = sectionNote(page -> page.addRow(row -> row
                .add(paragraph("Total")).add(new SpacerBuilder().grow(1).build()).add(paragraph("$120"))));
        String weighed = sectionNote(page -> page.addRow(row -> row.add(paragraph("Total")).add(paragraph("$120"))));

        String divided = "line heights, the space between lines, auto column widths and the columns of a row its "
                         + "arrangement or a growing spacer divides are the editor's";
        assertThat(arranged).contains(divided);
        assertThat(grown).contains(divided);
        assertThat(weighed).doesNotContain("divides")
                .as("its columns are its weights, written as they are")
                .startsWith(BASE);
    }

    @Test
    void anAlignmentOrAStacksLayerIsNamedAsWrittenFromTheLeftAndALeftOneIsNot() throws Exception {
        String across = BASE + "; what an alignment or a stack's layer is set at across the width is written from the left";
        assertThat(sectionNote(page -> page.addAligned(HorizontalAlign.CENTER, paragraph("Centred"))))
                .isEqualTo(across);
        assertThat(sectionNote(page -> page.addLayerStack(stack -> stack.center(paragraph("Centred")))))
                .isEqualTo(across);
        assertThat(sectionNote(page -> page.addLayerStack(stack -> stack.position(paragraph("Moved"), 12, 0,
                com.demcha.compose.document.node.LayerAlign.TOP_LEFT))))
                .as("set in by its offset").isEqualTo(across);
        assertThat(sectionNote(page -> page.addCircle(40, DocumentColor.BLACK, circle -> circle.center(paragraph("A")))))
                .as("a shape container's layer").startsWith(across);
        assertThat(sectionNote(page -> page.addAligned(HorizontalAlign.LEFT, paragraph("Left"))))
                .isEqualTo(BASE);
        assertThat(sectionNote(page -> page.addLayerStack(stack -> stack.centerLeft(paragraph("Left")))))
                .isEqualTo(BASE);
    }

    @Test
    void whatThePageKeepsFromAPageBreakIsNamedKindByKind() throws Exception {
        String keep = "; Word is not told to keep what the page keeps from a page break: ";
        assertThat(sectionNote(page -> page.addSection(s -> s.keepTogether().addParagraph("A").addParagraph("B"))))
                .isEqualTo(BASE + keep + "a block kept together or with the next");
        assertThat(sectionNote(page -> page.addSection(s -> s.keepWithNext().addParagraph("Heading"))
                .addParagraph("Body")))
                .isEqualTo(BASE + keep + "a block kept together or with the next");
        assertThat(sectionNote(page -> page.addTable(t -> t.autoColumns(2).row("Net", "100"))))
                .isEqualTo(BASE + keep + "a table's row in one piece");
        assertThat(sectionNote(page -> page.addRow(row -> row.add(paragraph("A")).add(paragraph("B")))))
                .isEqualTo(BASE + keep + "a row of columns");
        assertThat(sectionNote(page -> page.addRow(row -> { })))
                .as("a row with nothing in it writes nothing").isEqualTo(BASE);
        assertThat(sectionNote(page -> page.addLine(l -> l.horizontal(100).keepWithNext()).addParagraph("Below")
                .addTable(t -> t.autoColumns(2).row("Net", "100"))
                .addRow(row -> row.add(paragraph("A")).add(paragraph("B")))))
                .isEqualTo(BASE + keep + "a block kept together or with the next, a table's row in one piece, "
                           + "a row of columns");
    }

    @Test
    void aRowOrABlockComposedInATableCellIsKeptByItsTablesRowAndNotNamed() throws Exception {
        String note = sectionNote(page -> page.addTable(t -> t.columns(DocumentTableColumn.fixed(300))
                .rowCells(DocumentTableCell.node(new RowBuilder().add(paragraph("A")).add(paragraph("B")).build()))
                .rowCells(DocumentTableCell.node(new com.demcha.compose.document.dsl.SectionBuilder().keepTogether()
                        .addParagraph("C").addParagraph("D").build()))));

        assertThat(note).endsWith("a page break: a table's row in one piece");
    }

    @Test
    void aDrawingWithNoLayoutIsDroppedForTheLayoutItLacks() throws Exception {
        String why = "with no layout behind the export, nothing says where the page draws it, so it is not in the document";
        assertThat(dropped(page -> page.addLine(l -> l.vertical(40).thickness(2).color(DocumentColor.BLACK))))
                .containsExactly(why);
        assertThat(dropped(page -> page.addShape(80, 40, DocumentColor.BLACK))).containsExactly(why);
        assertThat(dropped(page -> page.addEllipse(80, 40, DocumentColor.BLACK))).containsExactly(why);
        assertThat(dropped(page -> page.addPath(path -> path.size(40, 40).moveTo(0, 0).lineTo(40, 40)
                .stroke(new DocumentStroke(DocumentColor.BLACK, 1))))).containsExactly(why);
        assertThat(dropped(page -> page.addPath(path -> path.size(40, 40).moveTo(0, 0).lineTo(40, 0).lineTo(20, 40)
                .closePath().fillColor(DocumentColor.BLACK)))).containsExactly(why);
        assertThat(dropped(page -> page.addShape(s -> s.size(40, 20)
                .fill(com.demcha.compose.document.style.DocumentPaint.solid(DocumentColor.BLACK)))))
                .as("a paint is a fill").containsExactly(why);
        assertThat(dropped(page -> page.addLine(l -> l.horizontal(100).thickness(2).color(DocumentColor.BLACK))))
                .as("a rule is written as one, with a layout or without")
                .isEmpty();
    }

    @Test
    void aDrawingThatPaintsNothingIsSaidToPaintNothing() throws Exception {
        String why = "it paints nothing a shape could show, so it is not in the document";
        List<String> laidOut = new ArrayList<>();
        for (DocxExportReport.Note note : DocxExports.report(400, 600, 20, page -> page.addCircle(60)).notes()) {
            if (note.severity() == DocxExportReport.Severity.DROPPED) {
                laidOut.add(note.detail());
            }
        }
        assertThat(laidOut).containsExactly(why);
        assertThat(dropped(page -> page.addCircle(60))).containsExactly(why);
        assertThat(dropped(page -> page.addShape(s -> s.size(40, 20).stroke(new DocumentStroke(DocumentColor.BLACK, 0)))))
                .as("a stroke no wider than nothing draws no line")
                .containsExactly(why);
        assertThat(dropped(page -> page.addPath(path -> path.size(40, 40).moveTo(0, 0).lineTo(40, 40))))
                .containsExactly(why);
        assertThat(dropped(page -> page.addLine(l -> l.vertical(40).stroke(null)))).containsExactly(why);
    }

    @Test
    void everyTableHasAGridARowLeftToWordAnEmptyOne() throws Exception {
        Consumer<PageFlowBuilder> content = page -> page
                .addRow(row -> row.arrangement(RowArrangement.SPACE_BETWEEN).add(paragraph("Total")).add(paragraph("$120")))
                .addTable(t -> t.columns(DocumentTableColumn.fixed(300))
                        .rowCells(DocumentTableCell.node(new RowBuilder().arrangement(RowArrangement.SPACE_BETWEEN)
                                .add(paragraph("Net")).add(paragraph("$99")).build())));
        try (XWPFDocument document = DocxExports.withoutLayout(400, 600, 20, content)) {
            List<XWPFTable> tables = new ArrayList<>(document.getTables());
            tables.add(document.getTables().get(1).getRow(0).getCell(0).getTables().get(0));

            assertThat(tables).hasSize(3).allSatisfy(table -> assertThat(table.getCTTbl().getTblGrid()).isNotNull());
            assertThat(tables.get(0).getCTTbl().getTblGrid().sizeOfGridColArray()).isZero();
            assertThat(tables.get(2).getCTTbl().getTblGrid().sizeOfGridColArray()).isZero();
            assertThat(tables.get(1).getCTTbl().getTblGrid().sizeOfGridColArray()).isEqualTo(1);
            assertThat(tables).allSatisfy(table -> assertThat(table.getCTTbl().xmlText())
                    .as("in the order a table's schema sets: properties, grid, rows")
                    .containsPattern("</w:tblPr><w:tblGrid(/>|>)")
                    .matches("(?s).*<w:tblGrid.*?<w:tr[ >].*"));
        }
    }

    private static String sectionNote(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.reportWithoutLayout(400, 600, 20, content).notes().stream()
                .filter(note -> note.subject().equals("measured geometry"))
                .map(DocxExportReport.Note::detail)
                .reduce((first, second) -> {
                    throw new AssertionError("two section notes");
                })
                .orElseThrow();
    }

    private static List<String> dropped(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.reportWithoutLayout(400, 600, 20, content).notes().stream()
                .filter(note -> note.severity() == DocxExportReport.Severity.DROPPED)
                .map(DocxExportReport.Note::detail)
                .toList();
    }

    private static com.demcha.compose.document.node.ParagraphNode paragraph(String text) {
        return new ParagraphBuilder().text(text).build();
    }
}
