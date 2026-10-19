package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.MultiSectionDocument;
import com.demcha.compose.document.backend.semantic.SemanticBackend;
import com.demcha.compose.document.backend.semantic.SemanticExportContext;
import com.demcha.compose.document.backend.semantic.SemanticSection;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A section Word starts after a blank page of its own is named.
 *
 * <p>Every section numbers its pages from 1, and where the document states different even and
 * odd pages — for a page zone on some of them — Word starts every section on an odd page, adding
 * a blank one before a section whose section before it runs to an odd number of pages. Measured
 * in Word on Windows: sections of one, one and one page ran to five pages, of two, one and one to
 * five, of one, two and one to five, of one, three and two to eight; LibreOffice adds none.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxBlankPageBeforeSectionTest {

    private static final String BLANK_PAGE = "Word adds a blank page before this section";

    @Test
    void aSectionAfterAnOddNumberOfPagesIsNamed() throws Exception {
        List<DocxExportReport.Note> notes = notes(1, 1, 1);

        assertThat(blankPagesBefore(notes)).containsExactly("section 2", "section 3");
        assertThat(notes).anySatisfy(note -> assertThat(note.detail())
                .startsWith(BLANK_PAGE + ": the section before it runs to one page, and")
                .contains("different even and odd pages")
                .endsWith("LibreOffice adds none"));
    }

    @Test
    void aSectionAfterAnyOddNumberOfPagesIsNamedWithIt() throws Exception {
        List<DocxExportReport.Note> notes = notes(1, 3, 2);

        assertThat(blankPagesBefore(notes)).containsExactly("section 2", "section 3");
        assertThat(notes).anySatisfy(note -> assertThat(note.detail())
                .startsWith(BLANK_PAGE + ": the section before it runs to 3 pages, and"));
    }

    @Test
    void aSectionAfterAnEvenNumberOfPagesIsNot() throws Exception {
        assertThat(blankPagesBefore(notes(2, 1, 1))).containsExactly("section 3");
        assertThat(blankPagesBefore(notes(1, 2, 1))).containsExactly("section 2");
        assertThat(blankPagesBefore(notes(2, 2))).isEmpty();
    }

    @Test
    void aDocumentWithoutEvenAndOddPagesAddsNone() throws Exception {
        List<DocxExportReport.Note> notes = new ArrayList<>();
        try (MultiSectionDocument document = GraphCompose.documents()
                .section(section(1, false))
                .section(section(1, false))
                .create()) {
            document.export(DocxSemanticBackend.builder().reportSink(report -> notes.addAll(report.notes())).build());
        }

        assertThat(blankPagesBefore(notes)).isEmpty();
    }

    @Test
    void aSectionAfterOneTheLayoutDoesNotCountIsNamedWhereItRunsOdd() throws Exception {
        Captured captured = new Captured();
        try (MultiSectionDocument document = GraphCompose.documents()
                .section(section(2, true))
                .section(section(1, false))
                .create()) {
            document.export(captured);
        }
        SemanticSection first = captured.sections.get(0);
        SemanticExportContext laidOut = first.context();
        List<SemanticSection> sections = List.of(
                new SemanticSection(first.graph(), new SemanticExportContext(laidOut.canvas(),
                        laidOut.customFontFamilies(), laidOut.outputFile(), laidOut.outputOptions(), null)),
                captured.sections.get(1));
        List<DocxExportReport.Note> notes = new ArrayList<>();
        DocxSemanticBackend.builder().reportSink(report -> notes.addAll(report.notes())).build()
                .exportSections(sections);

        assertThat(blankPagesBefore(notes)).containsExactly("section 2");
        assertThat(notes).anySatisfy(note -> assertThat(note.detail())
                .startsWith(BLANK_PAGE + " where the section before it runs to an odd number of pages:"));
    }

    private static List<DocxExportReport.Note> notes(int... pages) throws Exception {
        List<DocxExportReport.Note> notes = new ArrayList<>();
        var documents = GraphCompose.documents();
        for (int index = 0; index < pages.length; index++) {
            documents = documents.section(section(pages[index], index == 0));
        }
        try (MultiSectionDocument document = documents.create()) {
            document.export(DocxSemanticBackend.builder().reportSink(report -> notes.addAll(report.notes())).build());
        }
        return notes;
    }

    /** The sections the report names a blank page before, in order. */
    private static List<String> blankPagesBefore(List<DocxExportReport.Note> notes) {
        return notes.stream()
                .filter(note -> note.detail().startsWith(BLANK_PAGE))
                .map(note -> {
                    assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.APPROXIMATED);
                    assertThat(note.subject()).isEqualTo("page zone");
                    return note.path();
                })
                .toList();
    }

    /** A section of so many pages, with a footer on its even pages only where asked. */
    private static DocumentSession section(int pages, boolean evenFooter) {
        DocumentSession session = GraphCompose.document()
                .pageSize(300, 200)
                .margin(DocumentInsets.of(30))
                .create();
        if (evenFooter) {
            session.chrome().zone(DocumentPageZone.footer(20, page -> new ParagraphBuilder().text("Verso").build())
                    .toBuilder().appliesTo(page -> page.number() % 2 == 0).build());
        }
        session.pageFlow(page -> {
            page.addParagraph(p -> p.text("Page 1"));
            for (int next = 2; next <= pages; next++) {
                int number = next;
                page.addPageBreak(pageBreak -> { });
                page.addParagraph(p -> p.text("Page " + number));
            }
        });
        return session;
    }

    /** Takes the sections a multi-section document hands a backend. */
    private static final class Captured implements SemanticBackend<byte[]> {

        private List<SemanticSection> sections;

        @Override
        public String name() {
            return "capture";
        }

        @Override
        public byte[] export(DocumentGraph documentGraph, SemanticExportContext context) {
            return new byte[0];
        }

        @Override
        public byte[] exportSections(List<SemanticSection> sections) {
            this.sections = List.copyOf(sections);
            return new byte[0];
        }
    }
}
