package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ListBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.node.ListItem;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a list's items lose in the Word file is in the report: its alignment, its lineSpacing
 * where the layout's items are not its own, its continuationIndent, and the column of every
 * item that does not stand where the page sets it.
 *
 * <p>Each was written by the page and left out of the file without a note. A list that keeps
 * them — its lineSpacing put between its laid-out lines, its items tabbed to the page's column
 * or set where the layout set them — is not named.</p>
 */
class DocxListReportTest {

    private static final String LONG = "A highlight long enough to run past the end of its line, "
                                       + "and on past the end of the next one, in a narrow page";
    private static final String TWO_LINES = "One highlight that wraps onto exactly two lines here";
    private static final String STATED = " stand at a stated column — 9pt in, 6pt more a level — or a space past "
                                         + "their marker or two spaces a level in, not where the page sets them";

    @Test
    void aCentredOrARightAlignedListIsNamed() throws Exception {
        assertThat(listNote(page -> page.addList(list -> list.name("Steps").items("One", "Two")
                .align(TextAlign.CENTER))))
                .isEqualTo("written as a Word list; its items are written flush left, where the page sets them centred");
        assertThat(listNote(page -> page.addList(list -> list.name("Steps").items("One", "Two")
                .align(TextAlign.RIGHT))))
                .isEqualTo("written as a Word list; its items are written flush left, where the page sets them right-aligned");
        assertThat(listNotes(page -> page.addList(list -> list.name("Steps").items("One", "Two")
                .align(TextAlign.LEFT)))).isEmpty();
    }

    @Test
    void aListsLineSpacingIsNamedWhereTheLayoutsItemsAreNotItsOwn() throws Exception {
        // An item run onto the next page is laid out as two: no item is matched to the list's,
        // and none is given the gap between its lines.
        assertThat(listNote(200, page -> page.addList(list -> list.name("Highlights").lineSpacing(4)
                .items(LONG, String.join(" ", LONG, LONG, LONG, LONG, LONG)))))
                .isEqualTo("written as a Word list; its lineSpacing is not written between a wrapped item's lines");
        // Split a line apiece, an item still wraps: one line at the foot of the page, one on the next.
        assertThat(listNote(200, page -> page
                .addSpacer(spacer -> spacer.height(125))
                .addList(list -> list.name("Highlights").lineSpacing(4).textStyle(DocumentTextStyle.DEFAULT.withSize(10))
                        .items(TWO_LINES, "Short"))))
                .isEqualTo("written as a Word list; its lineSpacing is not written between a wrapped item's lines");
        // Composed in a table cell, it has no lines of its own to match or to read, nor a column.
        assertThat(listNote(page -> page.addTable(table -> table.name("Facts")
                .columns(DocumentTableColumn.fixed(160))
                .rowCells(DocumentTableCell.node(new ListBuilder().name("Highlights").lineSpacing(4)
                        .items(LONG).build())))))
                .isEqualTo("written as a Word list; its lineSpacing is not written between a wrapped item's lines"
                           + " — whether an item wraps is not measured; 1 of its 1 items" + STATED);

        // On one page, every item's own lines are given the gap.
        assertThat(listNotes(page -> page.addList(list -> list.name("Highlights").lineSpacing(4)
                .items(LONG, LONG)))).isEmpty();
    }

    @Test
    void aContinuationIndentIsNamedWhereAnItemWraps() throws Exception {
        // With no marker drawn before them, the page sets a wrapped item's lines after it.
        assertThat(listNote(page -> page.addList(list -> list.name("Notes").noMarker()
                .continuationIndent("    ").items(LONG))))
                .isEqualTo("written as a paragraph per item; its continuationIndent is not written before a "
                           + "wrapped item's lines");

        assertThat(listNotes(page -> page.addList(list -> list.name("Notes").noMarker()
                .continuationIndent("    ").items("Short", "Lines"))))
                .as("no item wraps").isEmpty();
        assertThat(listNotes(page -> page.addList(list -> list.name("Notes").bullet()
                .continuationIndent("    ").items(LONG))))
                .as("a marker the page sets before the wrapped lines instead").isEmpty();
        assertThat(listNotes(page -> page.addList(list -> list.name("Notes").noMarker().hangingIndent(true)
                .continuationIndent("    ").items(LONG))))
                .as("its items set in a column of their own, which the page applies no prefix to").isEmpty();

        // A tree of items is laid out flattened into its labels, with no marker before a wrapped line.
        assertThat(listNote(page -> page.addList(list -> list.name("Notes").continuationIndent("    ")
                .addItem(LONG, child -> { }))))
                .isEqualTo("written as a Word list; its continuationIndent is not written before a wrapped "
                           + "item's lines; 1 of its 1 items" + STATED);
    }

    @Test
    void aListThatWritesNoItemIsNotNamed() throws Exception {
        assertThat(listNotes(page -> page.addList(list -> list.name("Empty").items("", "  ")
                .align(TextAlign.CENTER).noMarker().continuationIndent("    ")))).isEmpty();
    }

    @Test
    void aMarkerThePageDrawsAloneForABlankItemIsNamed() throws Exception {
        // With hangingIndent the page keeps a blank item whose marker shows as a row of its own,
        // a marker before nothing; the export writes no paragraph for a blank item. Its row is no
        // item run onto the next page, and the list's lineSpacing is not named for it.
        assertThat(listNote(page -> page.addList(list -> list.name("Skills").hangingIndent(true).lineSpacing(4)
                .items("Java", "", "Kotlin"))))
                .isEqualTo("written as a Word list; 1 row the page draws as a marker alone, for a blank item, "
                           + "is not written");
        assertThat(listNote(page -> page.addList(list -> list.name("Skills").hangingIndent(true)
                .items("", " "))))
                .isEqualTo("writes no paragraph; 2 rows the page draws as a marker alone, for blank items, "
                           + "are not written");

        assertThat(listNotes(page -> page.addList(list -> list.name("Skills").items("Java", "", "Kotlin"))))
                .as("without hangingIndent the page skips a blank item too").isEmpty();
        assertThat(listNotes(page -> page.addList(list -> list.name("Skills").hangingIndent(true).noMarker()
                .items("Java", "", "Kotlin")))).as("a blank item with no marker draws nothing").isEmpty();
    }

    @Test
    void anItemThatDoesNotStandWhereThePageSetsItIsCounted() throws Exception {
        // A hangingIndent list that nests keeps the stated columns: its levels are not measured.
        assertThat(listNote(page -> page.addList(list -> list.name("Skills").hangingIndent(true)
                .addItem("Languages", child -> child.addItem("Java").addItem("Kotlin")))))
                .isEqualTo("written as a Word list; 3 of its 3 items" + STATED);
        // A gap too narrow to clear what Word may set the marker wider in.
        assertThat(listNote(page -> page.addList(list -> list.name("Skills").hangingIndent(true).markerGap(0)
                .items("Java", "Kotlin"))))
                .isEqualTo("written as a Word list; 2 of its 2 items" + STATED);
        // A tree of items without hangingIndent is laid out flattened, its markers in its text.
        assertThat(listNote(page -> page.addList(list -> list.name("Skills")
                .addItem("Java", child -> { }).addItem("Kotlin", child -> { }))))
                .isEqualTo("written as a Word list; 2 of its 2 items" + STATED);

        assertThat(listNotes(page -> page.addList(list -> list.name("Skills").hangingIndent(true)
                .items("Java", "Kotlin")))).as("its marker column the page's").isEmpty();
        assertThat(listNotes(page -> page.addList(list -> list.name("Skills").items("Java", "Kotlin"))))
                .as("its column the spaces the page sets its wrapped lines after").isEmpty();
        // A name and the description set under it, a tab and the layout's own place.
        assertThat(listNotes(page -> page.addList(list -> list.name("Projects").marker("•")
                .markerFor(1, ListMarker.none()).hangingIndent(true)
                .addItem(rich -> rich.bold("Magazine"), child -> child.addItem(rich -> rich.plain(LONG))))))
                .isEmpty();
    }

    @Test
    void anItemWrittenAsTextOrInRunsIsCountedWhereItLeavesThePagesColumn() throws Exception {
        // Siblings that disagree on the marker stay text, the marker and a space before the item.
        // Without hangingIndent those are the letters the page sets; with it, a column past the marker.
        List<ListItem> mixed = List.of(new ListItem("alpha", ListMarker.custom("✓"), List.of()),
                new ListItem("beta", ListMarker.custom("✗"), List.of()));
        assertThat(listNotes(page -> page.add(new ListNode("Mixed", List.of(), mixed, ListMarker.bullet(),
                DocumentTextStyle.DEFAULT, TextAlign.LEFT, 0, 0, "", true, DocumentInsets.zero(),
                DocumentInsets.zero())))).isEmpty();
        assertThat(listNote(page -> page.add(new ListNode("Mixed", List.of(), mixed, ListMarker.bullet(),
                DocumentTextStyle.DEFAULT, TextAlign.LEFT, 0, 0, "", true, DocumentInsets.zero(),
                DocumentInsets.zero(), true, ListNode.DEFAULT_MARKER_GAP))))
                .isEqualTo("written as a paragraph per item; 2 of its 2 items" + STATED);

        // Rich items nesting one with a marker: it keeps its spaces a level in, and the top level
        // a space after its marker.
        assertThat(listNote(page -> page.addList(list -> list.name("Skills").hangingIndent(true)
                .addItem(rich -> rich.bold("Java"), child -> child.addItem(rich -> rich.plain("Spring"))))))
                .isEqualTo("written as a paragraph per item; 2 of its 2 items" + STATED);
        assertThat(listNotes(page -> page.addList(list -> list.name("Skills").noMarker().hangingIndent(true)
                .addItem(rich -> rich.bold("Java"))))).as("no marker before it").isEmpty();

        // A marker that draws a disc alone: a tab to the page's column where its gap clears the
        // picture, a space where it does not.
        assertThat(listNotes(page -> page.addList(list -> list.name("Skills").hangingIndent(true)
                .marker(marker -> marker.dot(5, DocumentColor.rgb(0, 128, 128))).items("Java", "Kotlin"))))
                .isEmpty();
        assertThat(listNote(page -> page.addList(list -> list.name("Skills").hangingIndent(true).markerGap(0)
                .marker(marker -> marker.dot(5, DocumentColor.rgb(0, 128, 128))).items("Java", "Kotlin"))))
                .isEqualTo("written as a paragraph per item; 2 of its 2 items" + STATED);
    }

    @Test
    void withNoLayoutAListAtTheStatedColumnIsNamedAndTheSectionsNoteNamesTheSpaceBetweenLines() throws Exception {
        DocxExportReport report = DocxExports.reportWithoutLayout(400, 600, 40, page -> page
                .addList(list -> list.name("Skills").hangingIndent(true).lineSpacing(4).items("Java", "Kotlin")));

        assertThat(report.bySubject().get("ListNode")).extracting(DocxExportReport.Note::detail)
                .containsExactly("written as a Word list; 2 of its 2 items" + STATED);
        assertThat(report.bySubject().get("measured geometry")).extracting(DocxExportReport.Note::detail)
                .containsExactly("this document could not be laid out, so line heights, the space between lines "
                                 + "and auto column widths are the editor's rather than the engine's");

        // With no lines of its own to read, a continuationIndent is named whether or not an item wraps.
        DocxExportReport continued = DocxExports.reportWithoutLayout(400, 600, 40, page -> page
                .addList(list -> list.name("Notes").noMarker().continuationIndent("    ").items("Short")));
        assertThat(continued.bySubject().get("ListNode")).extracting(DocxExportReport.Note::detail)
                .containsExactly("written as a paragraph per item; its continuationIndent is not written before a "
                                 + "wrapped item's lines — whether an item wraps is not measured");
    }

    private static String listNote(Consumer<PageFlowBuilder> content) throws Exception {
        return listNote(600, content);
    }

    private static String listNote(double pageHeight, Consumer<PageFlowBuilder> content) throws Exception {
        List<String> notes = listNotes(pageHeight, content);
        assertThat(notes).as("the list's note").hasSize(1);
        return notes.get(0);
    }

    private static List<String> listNotes(Consumer<PageFlowBuilder> content) throws Exception {
        return listNotes(600, content);
    }

    private static List<String> listNotes(double pageHeight, Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> captured = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document()
                .pageSize(240, pageHeight)
                .margin(DocumentInsets.of(30))
                .create()) {
            session.pageFlow(content::accept);
            session.export(new DocxSemanticBackend(captured::set));
        }
        assertThat(captured.get()).as("the sink is called once the bytes exist").isNotNull();
        return captured.get().bySubject().getOrDefault("ListNode", List.of()).stream()
                .map(DocxExportReport.Note::detail).toList();
    }
}
