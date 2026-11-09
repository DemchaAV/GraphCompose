package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.ListBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Where the export writes a node off the page's and knows it, the report says so: a list item's
 * pictures placed by the list's first line, the one layer of a stack composed in a table cell, a
 * panel wider than its cell, the padding a cell keeps and Word gives its whole row, and, with no
 * layout, what the section's note names.
 */
class DocxUnmeasuredPlacementNotesTest {

    private static final DocumentColor RED = DocumentColor.rgb(255, 0, 0);
    private static final DocumentColor PALE = DocumentColor.rgb(255, 200, 200);
    private static final DocumentTextStyle NINE = DocumentTextStyle.builder().size(9).build();
    private static final SvgIcon ICON = SvgIcon.parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'>"
            + "<circle cx='12' cy='12' r='10' fill='#1A5694'/></svg>");

    @Test
    void anItemInAnotherSizeThanTheListsFirstLineHasItsPicturesNamed() throws Exception {
        // Placed by the list's 9pt first line, the 20pt item's dot stands lower than the page's:
        // measured, Word 16 2.8pt, LibreOffice 24.2 2.9pt.
        List<String> bigger = details("list picture", page -> page.addList(l -> l.hangingIndent(true).textStyle(NINE)
                .addItem(i -> i.plain("Small item ").dot(6, RED))
                .addItem(i -> i.size("Large item ", 20).dot(6, RED))));
        List<String> same = details("list picture", page -> page.addList(l -> l.hangingIndent(true).textStyle(NINE)
                .addItem(i -> i.plain("First item ").dot(6, RED))
                .addItem(i -> i.plain("Second item ").dot(6, RED))));

        assertThat(bigger).singleElement().asString()
                .startsWith("its item's pictures are placed by the list's first line, not their own: up to ")
                .endsWith("pt lower than the page sets them");
        assertThat(same).as("every item in the list's size: its own line is the list's").isEmpty();
    }

    @Test
    void anItemAfterALargerFirstOneHasItsPicturesNamedHigher() throws Exception {
        assertThat(details("list picture", page -> page.addList(l -> l.hangingIndent(true).textStyle(NINE)
                .addItem(i -> i.size("Large item ", 20).dot(6, RED))
                .addItem(i -> i.plain("Small item ").dot(6, RED)))))
                .singleElement().asString().endsWith("pt higher than the page sets them");
    }

    @Test
    void aPictureMarkerIsPlacedByTheListsFirstLineToo() throws Exception {
        // The page sets a marker on its item's first line, in that line's metrics.
        assertThat(details("list picture", page -> page.addList(l -> l.hangingIndent(true).textStyle(NINE)
                .marker(m -> m.dot(6, RED))
                .addItem(i -> i.plain("Small item"))
                .addItem(i -> i.size("Large item", 20)))))
                .singleElement().asString().endsWith("pt lower than the page sets them");
    }

    @Test
    void aRaiseTheItemsOwnLineWritesAlikeIsNotNamed() throws Exception {
        // Word moves a picture in half points: a size this near the list's rounds to the same raise.
        assertThat(details("list picture", page -> page.addList(l -> l.hangingIndent(true).textStyle(NINE)
                .addItem(i -> i.plain("First item ").dot(6, RED))
                .addItem(i -> i.size("Second item ", 9.2).dot(6, RED))))).isEmpty();
    }

    @Test
    void aPictureOnALaterLineOfAnotherHeightIsNamed() throws Exception {
        // The item's first line is in the list's size; its dot stands on a second, 20pt line.
        assertThat(details("list picture", page -> page.addList(l -> l.hangingIndent(true).textStyle(NINE)
                .addItem(i -> i.plain("A first line of nine point text running on past the width ")
                        .size("Large ", 20).dot(6, RED)))))
                .singleElement().asString().endsWith("pt lower than the page sets them");
    }

    @Test
    void aLineOfPicturesAloneInTheListsMetricsIsNotNamed() throws Exception {
        // Its line keeps the list's text metrics, raised to the picture: the raise is the same.
        assertThat(details("list picture", page -> page.addList(l -> l.hangingIndent(true).textStyle(NINE)
                .addItem(i -> i.plain("First item"))
                .addItem(i -> i.dot(20, RED))))).isEmpty();
    }

    @Test
    void anItemTheLayoutRunsOntoTheNextPageHasItsPicturesNamedAsNotMeasured() throws Exception {
        // Split a line apiece, the item is laid out as two: no item of the layout's is the list's own,
        // and no item's lines are known.
        Consumer<PageFlowBuilder> overPages = page -> page.addSpacer(spacer -> spacer.height(125))
                .addList(l -> l.hangingIndent(true).textStyle(DocumentTextStyle.DEFAULT.withSize(10))
                        .addItem(i -> i.plain("One highlight that wraps onto exactly two lines here ").dot(4, RED))
                        .addItem(i -> i.plain("Short")));
        List<String> notes = DocxExports.report(240, 200, 30, overPages).bySubject()
                .getOrDefault("list picture", List.of()).stream().map(DocxExportReport.Note::detail).toList();

        assertThat(notes).contains("its item's pictures are placed by the list's first line, and where the page sets "
                                   + "them by the item's own lines is not measured");
    }

    @Test
    void aListInACellWhoseLinesAreNotFoundHasItsPicturesNamedOnTheBaseline() throws Exception {
        // An item of runs is not matched to the table's lines: the list has no line to place by.
        Consumer<PageFlowBuilder> unmatched = beside(new ListBuilder().name("Items").noMarker().hangingIndent(true)
                .textStyle(NINE).addItem(i -> i.plain("Support ").dot(4, RED)).build());
        Consumer<PageFlowBuilder> onTheBaseline = beside(new ListBuilder().name("Items").noMarker().hangingIndent(true)
                .textStyle(NINE).addItem(i -> i.plain("Support ").svgIcon(ICON, 8, InlineImageAlignment.BASELINE))
                .build());
        Consumer<PageFlowBuilder> raised = beside(new ListBuilder().name("Items").noMarker().hangingIndent(true)
                .textStyle(NINE).addItem(i -> i.plain("Support ")
                        .svgIcon(ICON, 8, InlineImageAlignment.BASELINE, 3, null)).build());
        Consumer<PageFlowBuilder> alone = beside(new ListBuilder().name("Items").noMarker().hangingIndent(true)
                .textStyle(NINE).addItem(i -> i.plain("Support ")).addItem(i -> i.dot(4, RED)).build());
        Consumer<PageFlowBuilder> pictureless = beside(new ListBuilder().name("Items").noMarker().hangingIndent(true)
                .textStyle(NINE).addItem(i -> i.plain("Support")).build());

        assertThat(details("list picture", unmatched)).containsExactly(
                "its item's pictures stand on the text's baseline, where the page sets them by their alignment: no "
                + "line the layout set carries the list");
        assertThat(details("list picture", raised)).containsExactly("its item's pictures stand on the text's "
                + "baseline, up to 3pt from where the page sets them: no line the layout set carries the list");
        assertThat(details("list picture", onTheBaseline)).as("where the page sets it too").isEmpty();
        assertThat(details("list picture", alone)).as("pictures alone, set at their size").isEmpty();
        assertThat(details("list picture", pictureless)).as("no picture to place").isEmpty();
    }

    @Test
    void theOneLayerAStackComposedInACellWritesIsNamed() throws Exception {
        String note = "the one layer it writes beside what it draws is set as its table cell's text, as its text's "
                      + "alignment and the cell's set it: composed in a table cell, it has no place of its own in the "
                      + "layout, and where the page sets that layer, by its alignment and offset, is not measured";
        Consumer<PageFlowBuilder> labelled = inACell(new LayerStackBuilder().name("Labelled")
                .back(new ShapeBuilder().size(200, 40).fillColor(PALE).build())
                .center(new ParagraphBuilder().text("Label").build()).build());
        Consumer<PageFlowBuilder> drawn = inACell(new LayerStackBuilder().name("Icon")
                .back(new ShapeBuilder().size(20, 20).fillColor(RED).build())
                .center(new ShapeBuilder().size(10, 10).fillColor(PALE).build()).build());
        Consumer<PageFlowBuilder> placed = page -> page.add(new LayerStackBuilder().name("Labelled")
                .back(new ShapeBuilder().size(200, 40).fillColor(PALE).build())
                .center(new ParagraphBuilder().text("Label").build()).build());

        assertThat(details("overlapping layer", labelled)).containsExactly(note);
        assertThat(details("overlapping layer", drawn)).as("nothing written").isEmpty();
        assertThat(details("overlapping layer", placed)).as("the layout places it").isEmpty();
    }

    @Test
    void aPanelWiderThanItsCellIsNamed() throws Exception {
        // The cell leaves 92pt inside its 4pt margins: a 160pt panel is held to it.
        assertThat(details("panel width", panelInACell(160))).containsExactly(
                "composed in a table cell narrower than it, it is held to the cell, 68pt narrower than the page draws "
                + "it past the cell's edge, its text set in what the cell leaves");
        assertThat(details("panel width", panelInACell(92.09))).as("under a tenth of a point past").isEmpty();
        assertThat(details("panel width", panelInACell(80))).as("within the cell").isEmpty();
    }

    @Test
    void aCellOpeningWithATableNamesWhatItsPaddingCostsTheRow() throws Exception {
        assertThat(details("cell padding", page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder().padding(DocumentInsets.of(2))
                        .stroke(DocumentStroke.of(DocumentColor.BLACK, 0)).build())
                .rowCells(DocumentTableCell.text("tight"),
                        DocumentTableCell.node(new TableBuilder().columns(DocumentTableColumn.auto()).row("inner")
                                .build()).withStyle(DocumentTableStyle.builder().padding(DocumentInsets.of(12))
                                .build())))))
                .containsExactly("a cell opening with a table or spanning rows keeps its padding, which Word gives "
                                 + "every cell of its row: the other cells' text up to 10pt lower than the page sets it, "
                                 + "the row as much taller where one of them is its tallest");
    }

    @Test
    void aKeptMarginUnderATenthOfAPointPastTheRowsIsNotNamed() throws Exception {
        // 2.05pt is written as 41 twips beside 40: a twentieth of a point.
        assertThat(details("cell padding", page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder().padding(DocumentInsets.of(2))
                        .stroke(DocumentStroke.of(DocumentColor.BLACK, 0)).build())
                .rowCells(DocumentTableCell.text("tight"),
                        DocumentTableCell.node(new TableBuilder().columns(DocumentTableColumn.auto()).row("inner")
                                .build()).withStyle(DocumentTableStyle.builder().padding(DocumentInsets.of(2.05))
                                .build()))))).isEmpty();
    }

    @Test
    void aCellSpanningRowsNamesItsTopAndBottom() throws Exception {
        assertThat(details("cell padding", page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder().padding(DocumentInsets.of(2))
                        .stroke(DocumentStroke.of(DocumentColor.BLACK, 0)).build())
                .rowCells(DocumentTableCell.text("spans").rowSpan(2).withStyle(DocumentTableStyle.builder()
                                .padding(new DocumentInsets(12, 2, 8, 2)).build()),
                        DocumentTableCell.text("six"))
                .rowCells(DocumentTableCell.text("second")))))
                .containsExactly("a cell opening with a table or spanning rows keeps its padding, which Word gives "
                                 + "every cell of its row: the other cells' text up to 10pt lower than the page sets "
                                 + "it, the row as much taller where one of them is its tallest, and the row up to 6pt "
                                 + "taller");
    }

    @Test
    void aRowWhosePaddingIsAllWrittenInItsParagraphsIsNotNamed() throws Exception {
        assertThat(details("cell padding", page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder().padding(DocumentInsets.of(2))
                        .stroke(DocumentStroke.of(DocumentColor.BLACK, 0)).build())
                .rowCells(DocumentTableCell.text("tight"), DocumentTableCell.text("loose")
                        .withStyle(DocumentTableStyle.builder().padding(DocumentInsets.of(12)).build()))))).isEmpty();
    }

    @Test
    void withNoLayoutTheSectionsNoteNamesWhatItCannotPlace() throws Exception {
        assertThat(sectionNote(page -> page.add(new LayerStackBuilder().position(
                new ParagraphBuilder().text("Moved").build(), 0, 24, LayerAlign.TOP_LEFT).build())))
                .contains("; a stack's layer its offset moves up or down is written where the stack starts");
        assertThat(sectionNote(page -> page.add(new LayerStackBuilder().position(
                new ParagraphBuilder().text("Moved").build(), 0, -12, LayerAlign.TOP_LEFT).build())))
                .as("moved up").contains("; a stack's layer its offset moves up or down is written where the stack starts");
        assertThat(sectionNote(page -> page.addRow(r -> r.columns(DocumentRowColumn.fixed(150), DocumentRowColumn.auto())
                .add(new ParagraphBuilder().text("Fixed").build()).add(new ParagraphBuilder().text("Auto").build()))))
                .contains("; a row's fixed or weighted column beside an auto one is written with no width");
        assertThat(sectionNote(page -> page.addRow(r -> r.columns(DocumentRowColumn.weight(1), DocumentRowColumn.auto())
                .add(new ParagraphBuilder().text("Weighted").build()).add(new ParagraphBuilder().text("Auto").build()))))
                .contains("; a row's fixed or weighted column beside an auto one is written with no width");
        assertThat(sectionNote(page -> page.addParagraph(p -> p.text("Seated").verticalAlign(TextVerticalAlign.BOTTOM))))
                .contains("; a paragraph's vertical alignment in its line is not written");
        assertThat(sectionNote(page -> page.addList(l -> l.hangingIndent(true).addItem(i -> i.plain("Call ").dot(4, RED)))))
                .contains("; a list item's pictures stand on its text's baseline");
        assertThat(sectionNote(page -> page.addList(l -> l.hangingIndent(true)
                .marker(m -> m.dot(4, RED)).items("Call"))))
                .as("a marker's picture").contains("; a list item's pictures stand on its text's baseline");
    }

    @Test
    void withNoLayoutTheSectionsNoteNamesNoneOfThemWhereNoneIs() throws Exception {
        String note = sectionNote(page -> {
            page.add(new LayerStackBuilder().position(new ParagraphBuilder().text("Across").build(), 12, 0,
                    LayerAlign.TOP_LEFT).build());
            page.addRow(r -> r.columns(DocumentRowColumn.fixed(150), DocumentRowColumn.fixed(100))
                    .add(new ParagraphBuilder().text("Fixed").build()).add(new ParagraphBuilder().text("Too").build()));
            page.addRow(r -> r.columns(DocumentRowColumn.auto(), DocumentRowColumn.auto())
                    .add(new ParagraphBuilder().text("Auto").build()).add(new ParagraphBuilder().text("Auto").build()));
            page.addParagraph(p -> p.text(" ").verticalAlign(TextVerticalAlign.TOP));
            page.addParagraph(p -> p.text("Default"));
            page.addList(l -> l.hangingIndent(true).items("Plain"));
            // A picture on the baseline stands there in Word too.
            page.addList(l -> l.hangingIndent(true).addItem(i -> i.plain("Call ")
                    .svgIcon(ICON, 8, InlineImageAlignment.BASELINE)));
            // A list whose author declared depth writes its items' markers, never its own.
            page.addList(l -> l.marker(m -> m.dot(4, RED)).addItem("Parent", children -> children.items("Child")));
        });

        assertThat(note).doesNotContain("moves up or down", "a row's fixed", "vertical alignment",
                "list item's pictures");
    }

    private static String sectionNote(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.reportWithoutLayout(300, 400, 20, content).bySubject().get("measured geometry")
                .get(0).detail();
    }

    private static Consumer<PageFlowBuilder> panelInACell(double width) {
        return page -> page.add(new TableBuilder().columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.node(new ShapeContainerBuilder().rectangle(width, 40).fillColor(RED)
                                .center(new ParagraphBuilder().text("Wide").build()).build()),
                        DocumentTableCell.text("next"))
                .build());
    }

    private static Consumer<PageFlowBuilder> inACell(DocumentNode content) {
        return page -> page.add(new TableBuilder().columns(DocumentTableColumn.fixed(240))
                .rowCells(DocumentTableCell.node(content)).build());
    }

    private static Consumer<PageFlowBuilder> beside(DocumentNode composed) {
        return page -> page.addTable(table -> table
                .columns(DocumentTableColumn.fixed(40), DocumentTableColumn.fixed(160))
                .rowCells(DocumentTableCell.text("Mon"), DocumentTableCell.node(composed)));
    }

    private static List<String> details(String subject, Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.report(300, 400, 20, content).bySubject().getOrDefault(subject, List.of()).stream()
                .map(DocxExportReport.Note::detail).toList();
    }
}
