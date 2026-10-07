package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A paragraph the page reads as markdown is written as the page sets it, and not named
 * ({@link DocxSessionMarkdownTest}); where the page's lines do not tell how it sets it — with no
 * layout — it is written as authored and named. A list item the page reads as markdown is named in
 * the report: the page sets the text its marks style and drops the marks, and the Word file holds
 * the text as authored, marks and all — on the list's note.
 *
 * <p>A session reads markdown unless it is told not to ({@code markdown(false)}), in a paragraph
 * or a list item of plain text holding a mark of emphasis or code. Text the page sets as authored —
 * markdown off, no mark, a mark it keeps, a marker typed before an item, or runs, which it never
 * reads — is not named.</p>
 */
class DocxMarkdownReportTest {

    private static final String UNMEASURED = "markdown marks are written as letters — whether the page reads them is "
                                             + "not measured";
    private static final String ITEMS = "its items' markdown marks are written as letters, where the page sets the "
                                        + "text they mark and drops them";

    @Test
    void aParagraphThePageReadsAsMarkdownIsWrittenSoAndNotNamed() throws Exception {
        assertThat(paragraphNotes(true, page -> page.addParagraph("Some **bold** and `code` text"))).isEmpty();
        assertThat(paragraphNotes(true, page -> page.addParagraph("# Title *now*"))).as("a heading, past its line")
                .containsExactly("written as a paragraph; its markdown heading is written at the size the page sets it, "
                                 + "28pt, in a line only as tall as the paragraph's own: the page draws its letters past "
                                 + "the line, and Word cuts their tops on screen");
        assertThat(paragraphNotes(true, page -> page.addParagraph("`x`"))).as("a code span alone").isEmpty();
        // A prefix with a mark of its own is laid out with it, and leads the letters the page sets.
        assertThat(paragraphNotes(true, page -> page.addParagraph(p -> p.text("Some *emphasis* here")
                .bulletOffset("* ").indentStrategy(DocumentTextIndent.FIRST_LINE))))
                .containsExactly("written as a paragraph; its bulletOffset's letters, \"*\", are not written before "
                                 + "its first line");
        assertThat(paragraphNotes(true, page -> page.addParagraph(p -> p.text("*x*")
                .bulletOffset("** ").indentStrategy(DocumentTextIndent.FIRST_LINE))))
                .as("as many marks in the prefix as the text drops")
                .containsExactly("written as a paragraph; its bulletOffset's letters, \"**\", are not written before "
                                 + "its first line");
    }

    @Test
    void aParagraphThePageSetsAsAuthoredIsNotNamed() throws Exception {
        assertThat(paragraphNotes(false, page -> page.addParagraph("Some **bold** and `code` text")))
                .as("markdown off").isEmpty();
        assertThat(paragraphNotes(true, page -> page.addParagraph("Plain text, no marks")))
                .as("no mark").isEmpty();
        assertThat(paragraphNotes(true, page -> page.addParagraph("Read the file_name field")))
                .as("an underscore inside a word, which markdown keeps").isEmpty();
        assertThat(paragraphNotes(true, page -> page.addParagraph(p -> p.inlineText("Some **bold** text"))))
                .as("runs, which the page never reads as markdown").isEmpty();
        assertThat(paragraphNotes(false, page -> page.addParagraph(p -> p.text("Some *emphasis* here")
                .padding(new DocumentInsets(0, 0, 0, 240)))))
                .as("given no width, the page lays out lines with no text").isEmpty();
        assertThat(paragraphNotes(false, page -> page.addParagraph(p -> p.text("Some *emphasis* here")
                .bulletOffset("* ").indentStrategy(DocumentTextIndent.FIRST_LINE))))
                .as("markdown off, the prefix's mark aside")
                .containsExactly("written as a paragraph; its bulletOffset's letters, \"*\", are not written before its "
                                 + "first line");
    }

    @Test
    void aListsItemsThePageReadsAsMarkdownAreNamed() throws Exception {
        assertThat(listNotes(true, page -> page.addList(list -> list.name("Skills").items("**Java** lead", "Kotlin"))))
                .containsExactly("written as a Word list; " + ITEMS);
        assertThat(listNotes(true, page -> page.addList(list -> list.name("Skills").hangingIndent(true)
                .items("**Java** lead", "Kotlin"))))
                .as("markers in a column of their own").containsExactly("written as a Word list; " + ITEMS);
        assertThat(listNotes(true, page -> page.addList(list -> list.name("Skills")
                .addItem("Languages", child -> child.addItem("**Java**").addItem("Kotlin")))))
                .as("a tree of items").singleElement().asString().endsWith("; " + ITEMS);
        // As the page lays an item out: a marker typed before it is taken off, and none is lost.
        assertThat(listNotes(false, page -> page.addList(list -> list.name("Skills").items("* Java", "* Kotlin"))))
                .as("a marker typed before an item, markdown off").isEmpty();
        assertThat(listNotes(true, page -> page.addList(list -> list.name("Skills").items("* Java", "* Kotlin"))))
                .as("a marker typed before an item").isEmpty();
        assertThat(listNotes(false, page -> page.addList(list -> list.name("Skills").items("**Java** lead", "Kotlin"))))
                .as("markdown off").isEmpty();
        assertThat(listNotes(true, page -> page.addList(list -> list.name("Skills").items("Java", "Kotlin"))))
                .as("no mark").isEmpty();
    }

    @Test
    void whereTheLinesAreNotReadWhetherThePageReadsTheMarksIsNotMeasured() throws Exception {
        // With no layout, nothing tells; a session reads markdown unless told not to.
        assertThat(DocxExports.reportWithoutLayout(300, 400, 30, page -> page.addParagraph("Some **bold** text"))
                .bySubject().get("ParagraphNode")).extracting(DocxExportReport.Note::detail)
                .containsExactly("written as a paragraph; its " + UNMEASURED);
        assertThat(DocxExports.reportWithoutLayout(300, 400, 30, page -> page.addParagraph("Plain text"))
                .bySubject()).as("no mark").doesNotContainKey("ParagraphNode");
        // Read line by line, as the page does: a list marker opening a line is kept.
        assertThat(DocxExports.reportWithoutLayout(300, 400, 30, page -> page.addParagraph("* a_b"))
                .bySubject()).as("a marker the page keeps, a mark it keeps").doesNotContainKey("ParagraphNode");
        // Composed in a table cell, a paragraph is matched to its lines by its text as the page reads
        // it, and written so; a list's lines are not matched at all.
        assertThat(paragraphNotes(true, page -> page.add(cell(new ParagraphBuilder().name("Note")
                .text("Some **bold** text").build())))).isEmpty();
        assertThat(paragraphNotes(true, page -> page.add(cell(new ParagraphBuilder().name("Note")
                .text("Install node_js first").build())))).as("a mark the parser keeps").isEmpty();
        assertThat(listNotes(true, page -> page.add(cell(new com.demcha.compose.document.dsl.ListBuilder()
                .name("Skills").items("**Java** lead", "Kotlin").build()))))
                .singleElement().asString().endsWith("; its items' " + UNMEASURED);
        assertThat(listNotes(true, page -> page.add(cell(new com.demcha.compose.document.dsl.ListBuilder()
                .name("Skills").items("node_js", "Kotlin").build())))).as("a mark the parser keeps")
                .noneMatch(note -> note.contains("markdown"));
    }

    @Test
    void aZoneParagraphThePageReadsAsMarkdownIsWrittenSoAndNotNamed() throws Exception {
        assertThat(zoneNotes(true)).isEmpty();
        assertThat(zoneNotes(false)).as("markdown off").isEmpty();
    }

    private static com.demcha.compose.document.node.DocumentNode cell(com.demcha.compose.document.node.DocumentNode content) {
        return new com.demcha.compose.document.dsl.TableBuilder().name("Rota").columns(DocumentTableColumn.fixed(200))
                .rowCells(DocumentTableCell.node(content)).build();
    }

    private static List<String> zoneNotes(boolean markdown) throws Exception {
        AtomicReference<DocxExportReport> captured = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document().pageSize(300, 400).margin(DocumentInsets.of(36))
                .markdown(markdown).create()) {
            session.chrome().zone(DocumentPageZone.footer(30, page -> new ParagraphBuilder().name("ZoneLine")
                    .text("**Confidential**").textStyle(DocumentTextStyle.DEFAULT.withSize(8)).build()));
            session.pageFlow(page -> page.addParagraph("Body"));
            session.export(new DocxSemanticBackend(captured::set));
        }
        return captured.get().bySubject().getOrDefault("page zone", List.of()).stream()
                .map(DocxExportReport.Note::detail).toList();
    }

    private static List<String> paragraphNotes(boolean markdown, Consumer<PageFlowBuilder> content) throws Exception {
        return notes(markdown, content, "ParagraphNode");
    }

    private static List<String> listNotes(boolean markdown, Consumer<PageFlowBuilder> content) throws Exception {
        return notes(markdown, content, "ListNode");
    }

    private static List<String> notes(boolean markdown, Consumer<PageFlowBuilder> content, String subject) throws Exception {
        AtomicReference<DocxExportReport> captured = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document().pageSize(300, 400).margin(DocumentInsets.of(30))
                .markdown(markdown).create()) {
            session.pageFlow(content::accept);
            session.export(new DocxSemanticBackend(captured::set));
        }
        assertThat(captured.get()).as("the sink is called once the bytes exist").isNotNull();
        return captured.get().bySubject().getOrDefault(subject, List.of()).stream()
                .map(DocxExportReport.Note::detail).toList();
    }
}
