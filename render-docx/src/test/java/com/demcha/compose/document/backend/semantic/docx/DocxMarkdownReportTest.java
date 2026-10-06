package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A paragraph the page reads as markdown is named in the report: the page sets the text its marks
 * style and drops the marks, and the Word file holds the text as authored, marks and all.
 *
 * <p>A session reads markdown unless it is told not to ({@code markdown(false)}), in a paragraph
 * of plain text holding a mark of emphasis or code. A paragraph the page sets as authored — markdown
 * off, no mark, a mark it keeps, or runs, which it never reads — is not named.</p>
 */
class DocxMarkdownReportTest {

    private static final String MARKS = "its markdown marks are written as letters, where the page sets the text "
                                        + "they mark and drops them";

    @Test
    void aParagraphThePageReadsAsMarkdownIsNamed() throws Exception {
        assertThat(paragraphNotes(true, page -> page.addParagraph("Some **bold** and `code` text")))
                .containsExactly("written as a paragraph; " + MARKS);
        // A prefix with a mark of its own is laid out with it; the paragraph's marks still go.
        assertThat(paragraphNotes(true, page -> page.addParagraph(p -> p.text("Some *emphasis* here")
                .bulletOffset("* ").indentStrategy(DocumentTextIndent.FIRST_LINE))))
                .containsExactly("written as a paragraph; " + MARKS + "; its bulletOffset's letters, \"*\", are not "
                                 + "written before its first line");
        assertThat(paragraphNotes(true, page -> page.addParagraph(p -> p.text("*x*")
                .bulletOffset("** ").indentStrategy(DocumentTextIndent.FIRST_LINE))))
                .as("as many marks in the prefix as the text drops")
                .containsExactly("written as a paragraph; " + MARKS + "; its bulletOffset's letters, \"**\", are not "
                                 + "written before its first line");
        assertThat(paragraphNotes(false, page -> page.addParagraph(p -> p.text("Some *emphasis* here")
                .bulletOffset("* ").indentStrategy(DocumentTextIndent.FIRST_LINE))))
                .as("markdown off, the prefix's mark aside")
                .containsExactly("written as a paragraph; its bulletOffset's letters, \"*\", are not written before its "
                                 + "first line");
    }

    @Test
    void aListsItemsThePageReadsAsMarkdownAreNamed() throws Exception {
        assertThat(listNotes(true, page -> page.addList(list -> list.name("Skills").items("**Java** lead", "Kotlin"))))
                .containsExactly("written as a Word list; its items' markdown marks are written as letters, where the "
                                 + "page sets the text they mark and drops them");
        assertThat(listNotes(false, page -> page.addList(list -> list.name("Skills").items("**Java** lead", "Kotlin"))))
                .as("markdown off").isEmpty();
        assertThat(listNotes(true, page -> page.addList(list -> list.name("Skills").items("Java", "Kotlin"))))
                .as("no mark").isEmpty();
    }

    @Test
    void aParagraphThePageSetsAsAuthoredIsNotNamed() throws Exception {
        assertThat(paragraphNotes(false, page -> page.addParagraph("Some **bold** and `code` text")))
                .as("markdown off").isEmpty();
        assertThat(paragraphNotes(true, page -> page.addParagraph("Plain text, no marks")))
                .as("no mark").isEmpty();
        assertThat(paragraphNotes(true, page -> page.addParagraph("Read the file_name field")))
                .as("a mark markdown keeps, inside a word").isEmpty();
        assertThat(paragraphNotes(true, page -> page.addParagraph(p -> p.inlineText("Some **bold** text"))))
                .as("runs, which the page never reads as markdown").isEmpty();
    }

    @Test
    void whereALinesOwnTextIsNotReadWhetherThePageReadsItsMarksIsNotMeasured() throws Exception {
        String unmeasured = "markdown marks are written as letters — whether the page reads them is not measured";
        // With no layout, nothing tells; a session reads markdown unless told not to.
        assertThat(DocxExports.reportWithoutLayout(300, 400, 30, page -> page.addParagraph("Some **bold** text"))
                .bySubject().get("ParagraphNode")).extracting(DocxExportReport.Note::detail)
                .containsExactly("written as a paragraph; its " + unmeasured);
        assertThat(DocxExports.reportWithoutLayout(300, 400, 30, page -> page.addParagraph("Plain text"))
                .bySubject()).as("no mark").doesNotContainKey("ParagraphNode");
        // Composed in a table cell, a paragraph is matched to its lines by its text, which the page
        // set otherwise than authored.
        DocxExportReport cell = report(true, page -> page.addTable(table -> table.name("Rota")
                .columns(com.demcha.compose.document.table.DocumentTableColumn.fixed(120))
                .rowCells(com.demcha.compose.document.table.DocumentTableCell.node(
                        new ParagraphBuilder().name("Note").text("Some **bold** text").build()))));
        assertThat(cell.bySubject().get("ParagraphNode")).extracting(DocxExportReport.Note::detail)
                .containsExactly("written as a paragraph; its " + unmeasured);
    }

    @Test
    void aZoneParagraphThePageReadsAsMarkdownIsNamedOnTheZone() throws Exception {
        AtomicReference<DocxExportReport> captured = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document().pageSize(300, 400).margin(DocumentInsets.of(36))
                .markdown(true).create()) {
            session.chrome().zone(DocumentPageZone.footer(30, page -> new ParagraphBuilder().name("ZoneLine")
                    .text("**Confidential**").textStyle(DocumentTextStyle.DEFAULT.withSize(8)).build()));
            session.pageFlow(page -> page.addParagraph("Body"));
            session.export(new DocxSemanticBackend(captured::set));
        }
        assertThat(captured.get().bySubject().get("page zone")).extracting(DocxExportReport.Note::detail)
                .containsExactly("a footer written as one line of Word's footer; a paragraph's markdown marks are "
                                 + "written as letters, where the page sets the text they mark and drops them");
    }

    private static List<String> paragraphNotes(boolean markdown, Consumer<PageFlowBuilder> content) throws Exception {
        return notes(markdown, content, "ParagraphNode");
    }

    private static List<String> listNotes(boolean markdown, Consumer<PageFlowBuilder> content) throws Exception {
        return notes(markdown, content, "ListNode");
    }

    private static List<String> notes(boolean markdown, Consumer<PageFlowBuilder> content, String subject) throws Exception {
        return report(markdown, content).bySubject().getOrDefault(subject, List.of()).stream()
                .map(DocxExportReport.Note::detail).toList();
    }

    private static DocxExportReport report(boolean markdown, Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> captured = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document().pageSize(300, 400).margin(DocumentInsets.of(30))
                .markdown(markdown).create()) {
            session.pageFlow(content::accept);
            session.export(new DocxSemanticBackend(captured::set));
        }
        assertThat(captured.get()).as("the sink is called once the bytes exist").isNotNull();
        return captured.get();
    }
}
