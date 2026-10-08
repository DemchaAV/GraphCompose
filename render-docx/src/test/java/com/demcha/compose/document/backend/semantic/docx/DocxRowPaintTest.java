package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A row that paints its box — a fill, an outline, a side's border — is written as a panel holding
 * its columns: Word holds the paint on a table cell, the row's padding in the cell's margins, and
 * the columns in a table nested in the cell, as a container's panel holds its children. The page
 * paints a row's box as it paints a container's.
 */
class DocxRowPaintTest {

    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final DocumentStroke RULE = DocumentStroke.of(DocumentColor.rgb(26, 86, 148), 1);
    private static final double CONTENT = 360;

    @Test
    void aRowsFillIsItsPanelsShadingRoundItsColumns() throws Exception {
        Exported exported = export(page -> page.addRow(row -> row.name("Totals").fillColor(SURFACE)
                .padding(DocumentInsets.of(8)).spacing(12).addParagraph("Subtotal").addParagraph("120.00")));

        XWPFTable panel = exported.document().getTables().get(0);
        assertThat(panel.getRows()).hasSize(1);
        XWPFTableCell cell = panel.getRow(0).getCell(0);
        assertThat(panel.getRow(0).getTableCells()).as("one cell, the panel's").hasSize(1);
        assertThat(cell.getColor()).isEqualToIgnoringCase("EEF3F9");
        CTTcMar margins = cell.getCTTc().getTcPr().getTcMar();
        assertThat(twips(margins.getTop().getW())).as("the row's padding, as the cell's margins").isEqualTo(160);
        assertThat(twips(margins.getBottom().getW())).isEqualTo(160);
        assertThat(twips(margins.getLeft().getW())).isEqualTo(160);
        assertThat(twips(margins.getRight().getW())).isEqualTo(160);

        // The columns, nested in the cell, unshaded and without the padding the margins hold.
        assertThat(cell.getTables()).hasSize(1);
        XWPFTable columns = cell.getTables().get(0);
        assertThat(columns.getRow(0).getTableCells()).hasSize(2)
                .allSatisfy(column -> assertThat(column.getColor()).isNull());
        assertThat(columns.getRow(0).getCell(0).getText()).isEqualTo("Subtotal");
        assertThat(columns.getRow(0).getCell(1).getText()).isEqualTo("120.00");
        long grid = columns.getCTTbl().getTblGrid().getGridColList().stream().mapToLong(col -> twips(col.getW())).sum();
        assertThat(grid).as("the row's width less its padding").isEqualTo(Math.round((CONTENT - 16) * 20));
        assertThat(twips(columns.getRow(0).getCell(0).getCTTc().getTcPr().getTcMar().getLeft().getW()))
                .as("the first column starts at the cell's text").isZero();
        assertThat(twips(columns.getRow(0).getCell(0).getCTTc().getTcPr().getTcMar().getRight().getW()))
                .as("the gap after it").isEqualTo(240);
        assertThat(twips(columns.getRow(0).getCell(1).getCTTc().getTcPr().getTcMar().getRight().getW()))
                .as("the last column ends at the cell's text").isZero();

        assertThat(exported.report().bySubject()).doesNotContainKey("row paint").doesNotContainKey("RowNode");
        // Laid out as one piece, never across a page break: its panel is kept whole too.
        assertThat(panel.getRow(0).isCantSplitRow()).isTrue();
        // The panel holds the row's height; the columns, held as well, would grow it.
        var columnsRow = columns.getRow(0).getCtRow();
        assertThat(columnsRow.isSetTrPr() && columnsRow.getTrPr().sizeOfTrHeightArray() > 0)
                .as("the columns are not held").isFalse();
    }

    @Test
    void withNoLayoutTheColumnsAreTheRowsOwnArithmeticLessItsPadding() throws Exception {
        XWPFDocument document = DocxExports.withoutLayout(400, 600, 20, page -> page.addRow(row -> row
                .fillColor(SURFACE).padding(DocumentInsets.of(8)).spacing(12).addParagraph("Left").addParagraph("Right")));

        XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);
        assertThat(cell.getColor()).isEqualToIgnoringCase("EEF3F9");
        XWPFTable columns = cell.getTables().get(0);
        long grid = columns.getCTTbl().getTblGrid().getGridColList().stream().mapToLong(col -> twips(col.getW())).sum();
        assertThat(grid).as("the room the panel's margins leave").isEqualTo(Math.round((CONTENT - 16) * 20));
        assertThat(twips(columns.getRow(0).getCell(0).getCTTc().getTcPr().getTcMar().getLeft().getW())).isZero();
    }

    @Test
    void aRowThePageGivesNoHeightIsNotWritten() throws Exception {
        // The page paints a row's box only where the layout gives it height.
        Exported exported = export(page -> page.addParagraph("Before").addRow(row -> row.fillColor(SURFACE))
                .addParagraph("After"));

        assertThat(exported.document().getTables()).isEmpty();
        assertThat(exported.report().bySubject()).doesNotContainKey("row paint");
    }

    @Test
    void anOutlineAndASidesBorderAreItsPanelsBorders() throws Exception {
        Exported outlinedRow = export(page -> page.addRow(row -> row.name("Outlined").stroke(RULE)
                .addParagraph("Left").addParagraph("Right")));
        CTTcBorders outlined = panelCell(outlinedRow).getCTTc().getTcPr().getTcBorders();
        assertThat(List.of(outlined.getTop(), outlined.getLeft(), outlined.getBottom(), outlined.getRight()))
                .allSatisfy(side -> assertThat(side.xgetColor().getStringValue()).isEqualToIgnoringCase("1A5694"));
        // Word sets the columns below the whole border above, where the page strokes it on the
        // row's edge: with no padding and no space above to take what reaches past the row's box,
        // the columns stand that much lower.
        assertThat(outlinedRow.report().bySubject().get("RowNode")).singleElement().satisfies(note -> {
            assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.APPROXIMATED);
            assertThat(note.path()).contains("Outlined");
            assertThat(note.detail()).isEqualTo("its content stands 1pt lower than the page sets it, and what follows "
                                                + "up to as much: Word sets it below the whole of its top border, and "
                                                + "neither the space above it nor its padding takes what of the border "
                                                + "reaches past the panel's box");
        });
        Exported padded = export(page -> page.addRow(row -> row.stroke(RULE).padding(DocumentInsets.of(4))
                .addParagraph("Left").addParagraph("Right")));
        assertThat(padded.report().bySubject()).as("its padding takes its top border").doesNotContainKey("RowNode");

        // The border below reaches past the row's box too: the block below stands that much lower
        // where neither the space between them nor the row's padding takes it, and says so.
        Exported underlined = export(page -> page.addRow(row -> row.borders(DocumentBorders.bottom(RULE))
                .addParagraph("Left").addParagraph("Right")).addParagraph("After"));
        CTTcBorders bottom = panelCell(underlined).getCTTc().getTcPr().getTcBorders();
        assertThat(bottom.getBottom().xgetColor().getStringValue()).isEqualToIgnoringCase("1A5694");
        assertThat(bottom.isSetTop() && bottom.getTop().getVal() != STBorder.NONE && bottom.getTop().getVal() != STBorder.NIL)
                .as("no top border").isFalse();
        assertThat(underlined.report().bySubject()).doesNotContainKey("row paint").doesNotContainKey("RowNode");
        assertThat(underlined.report().bySubject().get("space above")).singleElement()
                .extracting(DocxExportReport.Note::detail).isEqualTo("it stands 1pt lower than the page sets it, and "
                        + "what follows with it: the space the page leaves above it does not hold the border Word draws "
                        + "past the box of the panel above it (1pt)");
        Exported spaced = export(page -> page.addRow(row -> row.borders(DocumentBorders.bottom(RULE))
                .addParagraph("Left").addParagraph("Right")).addParagraph(p -> p.text("After")
                .margin(new DocumentInsets(4, 0, 0, 0))));
        assertThat(spaced.report().bySubject()).as("the space between them takes it").doesNotContainKey("space above");
    }

    @Test
    void aRoundedRowKeepsItsFillAndNamesItsSquaredCorners() throws Exception {
        Exported exported = export(page -> page.addRow(row -> row.fillColor(SURFACE).cornerRadius(6)
                .addParagraph("Left").addParagraph("Right")));

        assertThat(panelCell(exported).getColor()).isEqualToIgnoringCase("EEF3F9");
        assertThat(exported.report().bySubject().get("corner radius")).singleElement().satisfies(note -> {
            assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.APPROXIMATED);
            assertThat(note.detail()).isEqualTo("a Word table cell is rectangular, so the panel keeps its fill and "
                                                + "loses its rounded corners");
        });
        assertThat(exported.report().bySubject()).doesNotContainKey("row paint");
    }

    @Test
    void aTranslucentFillIsFlattenedAgainstThePageAndNamed() throws Exception {
        Exported exported = export(page -> page.addRow(row -> row.name("Tinted")
                .fillColor(DocumentColor.rgba(26, 86, 148, 128)).addParagraph("Left").addParagraph("Right")));

        // Half of 1A5694 over the white page.
        assertThat(panelCell(exported).getColor()).isEqualToIgnoringCase("8CAAC9");
        assertThat(exported.report().notes()).anySatisfy(note -> {
            assertThat(note.path()).contains("Tinted");
            assertThat(note.detail()).contains("its fill").contains("flattened");
        });
    }

    @Test
    void aTranslucentPanelInAFilledRowIsFlattenedAgainstTheRowsFill() throws Exception {
        // Word shows the row's fill under the panel, as the page does: white at half over navy.
        Exported exported = export(page -> page.addRow(row -> row.fillColor(DocumentColor.rgb(30, 50, 90))
                .padding(DocumentInsets.of(6)).addSection("Tile", tile -> tile
                        .fillColor(DocumentColor.rgba(255, 255, 255, 128)).addParagraph("Inside"))
                .addParagraph("Beside")));

        XWPFTableCell rowCell = panelCell(exported);
        assertThat(rowCell.getColor()).isEqualToIgnoringCase("1E325A");
        XWPFTableCell tile = rowCell.getTables().get(0).getRow(0).getCell(0).getTables().get(0).getRow(0).getCell(0);
        assertThat(tile.getColor()).isEqualToIgnoringCase("8F99AD");
    }

    @Test
    void aFilledRowInABandOfLayersKeepsItsNoteAndNothingIsSetOnItsFill() throws Exception {
        // A band measures the space round its first block to the text inside it, which a panel's
        // margins would hold again: the row is written as its columns alone, its paint named, and
        // the panel the page lays over it, written after it, stands on what Word shows there.
        Exported exported = export(page -> page.addLayerStack(stack -> stack
                .back(new com.demcha.compose.document.dsl.RowBuilder().name("Under").fillColor(DocumentColor.rgb(30, 50, 90))
                        .padding(DocumentInsets.of(20)).addParagraph("Under").addParagraph("Row").build())
                .center(new com.demcha.compose.document.dsl.SectionBuilder().name("Over")
                        .fillColor(DocumentColor.rgba(255, 255, 255, 128)).addParagraph("Over").build())));

        assertThat(cellsShaded(exported.document())).as("the row's navy is not written; white over white")
                .containsOnly("FFFFFF");
        assertThat(exported.report().bySubject().get("row paint")).singleElement().satisfies(note -> {
            assertThat(note.path()).contains("Under");
            assertThat(note.detail()).isEqualTo("the row's fill is not written: a band of layers or a stack's "
                                                + "column measures the space round it to its text, which its "
                                                + "panel's margins would hold again");
        });

        // So in the middle of a band's layer, between blocks the band measures.
        Exported middle = export(page -> page.addLayerStack(stack -> stack
                .back(new com.demcha.compose.document.dsl.SectionBuilder().addParagraph("Top")
                        .addRow(row -> row.name("Middle").fillColor(SURFACE).padding(DocumentInsets.of(6))
                                .addParagraph("Left").addParagraph("Right"))
                        .addParagraph("Bottom").build())
                .center(new com.demcha.compose.document.dsl.SectionBuilder().addParagraph("Tag").build())));
        assertThat(middle.report().bySubject().get("row paint")).extracting(DocxExportReport.Note::path)
                .singleElement().asString().contains("Middle");
    }

    @Test
    void aRowWhosePaddingOrColumnHangsPastItsEdgeKeepsItsNote() throws Exception {
        String kept = "the row's fill is not written: its padding, or a column's margin into it, is below zero, "
                      + "which a Word table cell's margins do not hold";
        Exported hanging = export(page -> page.addRow(row -> row.fillColor(SURFACE).padding(DocumentInsets.of(8))
                .addParagraph(p -> p.text("Left").margin(new DocumentInsets(0, 0, 0, -4))).addParagraph("Right")));
        assertThat(hanging.report().bySubject().get("row paint")).extracting(DocxExportReport.Note::detail)
                .containsExactly(kept);
        assertThat(hanging.document().getTables().get(0).getRow(0).getTableCells()).as("its columns alone").hasSize(2);

        Exported negative = export(page -> page.addRow(row -> row.fillColor(SURFACE)
                .padding(new DocumentInsets(0, 0, 0, -10)).addParagraph("Left").addParagraph("Right")));
        assertThat(negative.report().bySubject().get("row paint")).extracting(DocxExportReport.Note::detail)
                .containsExactly(kept);
    }

    @Test
    void aRowWhoseMarginIsBelowZeroAboveBelowOrAtASideInACellKeepsItsNote() throws Exception {
        String kept = "the row's fill is not written: its margin is below zero above or below it, or at a side in a "
                      + "cell, which its panel's place in Word does not take";
        // Its columns' table nets a margin pulling it up against its padding; a panel owes the
        // space round it on its own, where a pull is nothing.
        Exported pulled = export(page -> page.addParagraph(p -> p.text("Before").margin(DocumentInsets.bottom(10)))
                .addRow(row -> row.fillColor(SURFACE).margin(new DocumentInsets(-8, 0, -8, 0))
                        .padding(DocumentInsets.of(8)).addParagraph("Left").addParagraph("Right"))
                .addParagraph("After"));
        assertThat(pulled.report().bySubject().get("row paint")).extracting(DocxExportReport.Note::detail)
                .containsExactly(kept);
        assertThat(cellsShaded(pulled.document())).as("its columns alone").isEmpty();

        // Word starts a table no further left than its cell's text.
        Exported bled = export(page -> page.addSection("Card", card -> card.fillColor(SURFACE)
                .padding(DocumentInsets.of(12)).addRow(row -> row.fillColor(DocumentColor.rgb(30, 50, 90))
                        .margin(new DocumentInsets(0, -12, 0, -12)).padding(DocumentInsets.of(12))
                        .addParagraph("Left").addParagraph("Right"))));
        assertThat(bled.report().bySubject().get("row paint")).extracting(DocxExportReport.Note::detail)
                .containsExactly(kept);

        // In the body Word holds a table's edge past the margin: the row bleeds as its panel.
        Exported bleeding = export(page -> page.addRow(row -> row.fillColor(SURFACE)
                .margin(new DocumentInsets(0, -20, 0, -20)).padding(DocumentInsets.of(20))
                .addParagraph("Left").addParagraph("Right")));
        assertThat(bleeding.report().bySubject()).doesNotContainKey("row paint");
        assertThat(panelCell(bleeding).getColor()).isEqualToIgnoringCase("EEF3F9");
    }

    @Test
    void aFilledRowAStacksColumnMeasuresRoundIsWrittenAsItsColumns() throws Exception {
        String kept = "the row's fill is not written: a band of layers or a stack's column measures the space round "
                      + "it to its text, which its panel's margins would hold again";
        // The main layer goes on below the name layer after a stand-in: its first block opens
        // where the layer resumes, measured to the text inside it.
        Exported opening = export(page -> page.addLayerStack(stack -> stack
                .layer(column("NameLayer", 120, 0, name -> name.addParagraph("Ada Lovelace")), LayerAlign.TOP_LEFT)
                .layer(column("Sidebar", 0, 240, side -> side.addParagraph("Contact")), LayerAlign.TOP_LEFT)
                .layer(column("MainLayer", 120, 0, main -> main
                        .addSpacer(spacer -> spacer.width(100).height(20))
                        .addRow(row -> row.name("Opening").fillColor(SURFACE).padding(DocumentInsets.of(6))
                                .addParagraph("Engineer").addParagraph("2020")))
                        , LayerAlign.TOP_LEFT)));
        assertThat(opening.report().bySubject().get("row paint")).singleElement().satisfies(note -> {
            assertThat(note.path()).contains("Opening");
            assertThat(note.detail()).isEqualTo(kept);
        });

        // The name layer ends with a row the main layer resumes after, measured from its text.
        Exported closing = export(page -> page.addLayerStack(stack -> stack
                .layer(column("NameLayer", 120, 0, name -> name.addParagraph("Ada Lovelace")
                        .addRow(row -> row.name("Closing").fillColor(SURFACE).padding(DocumentInsets.of(6))
                                .addParagraph("Engineer").addParagraph("2020"))), LayerAlign.TOP_LEFT)
                .layer(column("Sidebar", 0, 240, side -> side.addParagraph("Contact")), LayerAlign.TOP_LEFT)
                .layer(column("MainLayer", 120, 0, main -> main
                        .addSpacer(spacer -> spacer.width(100).height(40))
                        .addParagraph("Experience")), LayerAlign.TOP_LEFT)));
        assertThat(closing.report().bySubject().get("row paint")).singleElement().satisfies(note -> {
            assertThat(note.path()).contains("Closing");
            assertThat(note.detail()).isEqualTo(kept);
        });
    }

    /** One column as a full-width layer, inset to its band, as a two-column CV lays it out. */
    private static com.demcha.compose.document.node.DocumentNode column(
            String name, double insetLeft, double insetRight,
            Consumer<com.demcha.compose.document.dsl.SectionBuilder> content) {
        com.demcha.compose.document.dsl.SectionBuilder layer = new com.demcha.compose.document.dsl.SectionBuilder();
        layer.name(name).spacing(0).padding(new DocumentInsets(0, insetRight, 0, insetLeft));
        layer.addSection(name + "Content", section -> content.accept(section.spacing(0)));
        return layer.build();
    }

    @Test
    void aPaintedRowTheLayoutMovesToANewPageKeepsTheSpaceAboveItThere() throws Exception {
        // The layout keeps the row's top margin on the new page; Word drops a paragraph's space
        // above at a page's top and keeps a line's height, so a line kept with the panel holds it.
        // The space below the paragraph before stays on the page above, below that paragraph.
        Exported exported = export(page -> page.addSpacer(spacer -> spacer.height(500))
                .addParagraph(p -> p.text("Before").margin(DocumentInsets.bottom(10)))
                .addRow(row -> row.name("Moved").fillColor(SURFACE).margin(new DocumentInsets(24, 0, 0, 0))
                        .padding(DocumentInsets.of(6)).addParagraph("Left").addParagraph("Right")));

        List<org.apache.poi.xwpf.usermodel.IBodyElement> body = exported.document().getBodyElements();
        int panel = body.indexOf(exported.document().getTables().get(0));
        assertThat(body.get(panel - 1)).isInstanceOf(org.apache.poi.xwpf.usermodel.XWPFParagraph.class)
                .satisfies(element -> {
                    var line = ((org.apache.poi.xwpf.usermodel.XWPFParagraph) element).getCTP().getPPr();
                    assertThat(line.isSetKeepNext()).as("kept with the panel").isTrue();
                    assertThat(twips(line.getSpacing().getLine())).as("the margin, 24pt").isEqualTo(480);
                });
        assertThat(body.get(panel - 2)).isInstanceOf(org.apache.poi.xwpf.usermodel.XWPFParagraph.class)
                .satisfies(element -> {
                    var before = (org.apache.poi.xwpf.usermodel.XWPFParagraph) element;
                    assertThat(before.getText()).isEqualTo("Before");
                    assertThat(twips(before.getCTP().getPPr().getSpacing().getAfter()))
                            .as("its own 10pt, on the page above").isEqualTo(200);
                });
        assertThat(exported.report().bySubject()).doesNotContainKey("row paint");
    }

    /** Every cell shading in the document, nested tables included. */
    private static List<String> cellsShaded(XWPFDocument document) {
        List<String> fills = new java.util.ArrayList<>();
        for (XWPFTable table : document.getTables()) {
            collectShading(table, fills);
        }
        return fills;
    }

    private static void collectShading(XWPFTable table, List<String> into) {
        for (var row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                if (cell.getColor() != null) {
                    into.add(cell.getColor().toUpperCase(java.util.Locale.ROOT));
                }
                for (XWPFTable nested : cell.getTables()) {
                    collectShading(nested, into);
                }
            }
        }
    }

    @Test
    void aRowOfNoColumnsItPaintsIsAPanelAsTallAsThePageMakesIt() throws Exception {
        Exported exported = export(page -> page.addParagraph("Before")
                .addRow(row -> row.fillColor(SURFACE).padding(DocumentInsets.of(8))).addParagraph("After"));

        // Its padding is the cell's margins, 16pt together, round a paragraph a hairline tall.
        XWPFTableCell cell = exported.document().getTables().get(0).getRow(0).getCell(0);
        assertThat(cell.getColor()).isEqualToIgnoringCase("EEF3F9");
        assertThat(twips(cell.getCTTc().getTcPr().getTcMar().getTop().getW())).isEqualTo(160);
        assertThat(twips(cell.getCTTc().getTcPr().getTcMar().getBottom().getW())).isEqualTo(160);
        assertThat(cell.getText()).isEmpty();
        assertThat(cell.getTables()).isEmpty();
        assertThat(exported.report().bySubject()).doesNotContainKey("row paint");
    }

    @Test
    void aRowThatPaintsNothingIsItsColumnsAlone() throws Exception {
        Exported exported = export(page -> page.addRow(row -> row.padding(DocumentInsets.of(8))
                .addParagraph("Left").addParagraph("Right")));

        XWPFTable columns = exported.document().getTables().get(0);
        assertThat(columns.getRow(0).getTableCells()).hasSize(2)
                .allSatisfy(cell -> assertThat(cell.getTables()).isEmpty());
        assertThat(twips(columns.getRow(0).getCell(0).getCTTc().getTcPr().getTcMar().getLeft().getW()))
                .as("its padding rides in its first column's margin").isEqualTo(160);
    }

    private static XWPFTableCell panelCell(Exported exported) {
        return exported.document().getTables().get(0).getRow(0).getCell(0);
    }

    private static long twips(Object value) {
        return DocxTwips.of(value);
    }

    private record Exported(XWPFDocument document, DocxExportReport report) {
    }

    private static Exported export(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(content::accept);
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        return new Exported(new XWPFDocument(new ByteArrayInputStream(docx)), report.get());
    }
}
