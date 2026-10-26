package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A block that writes no paragraph, and draws nothing a paragraph carries as its own, names the
 * anchor it declares, where no bookmark marks it.
 *
 * <p>A bookmark goes round the paragraphs a block writes, or, for a block of drawings alone, at the
 * start of the paragraph its first drawing is anchored in ({@code DocxDrawnBlockBookmarkTest}). A
 * block that draws nothing has neither, and nor has one composed in a table's cell, whose drawings its
 * table draws as its own. A link to its anchor is written all the same, to a bookmark the file lacks:
 * measured in Word 16 and LibreOffice on Windows, the page's link went to the block's place, and in
 * both editors' PDFs the text carried no link at all, with nothing in the report. A page reference to
 * it was already named, as the number it is written as.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxAnchorWithoutABookmarkTest {

    private static final String LOST = "its anchor \"target\" has no bookmark in the Word file: a link to it points "
                                       + "at none, and a page reference to it is a fixed number rather than a field";
    private static final String WHY = "writes no paragraph, and no paragraph carries the first drawing of its own, so ";

    @Test
    void aSectionThatDrawsNothingNamesItsAnchor() throws Exception {
        Exported exported = export(page -> page.addParagraph(p -> p.text("Go there").linkTo("target"))
                .add(nothing("target"))
                .addParagraph("After"));

        assertThat(notesOn(exported.report(), "SectionNode")).singleElement().isEqualTo(WHY + LOST);
        String xml = exported.document().getDocument().xmlText();
        assertThat(xml).as("the link, written all the same").contains("w:anchor=\"target\"");
        assertThat(xml).as("no bookmark for it to go to").doesNotContain("w:name=\"target\"");
    }

    @Test
    void aContainerThatDrawsNothingNamesItsAnchor() throws Exception {
        Exported exported = export(page -> page.addParagraph(p -> p.text("Go there").linkTo("target"))
                .add(new com.demcha.compose.document.node.ContainerNode("Box", List.of(), 0,
                        DocumentInsets.zero(), DocumentInsets.zero(), null, null, null, null, "target"))
                .addParagraph("After"));

        assertThat(notesOn(exported.report(), "ContainerNode")).singleElement().isEqualTo(WHY + LOST);
    }

    @Test
    void aSectionWithANoteOfItsOwnNamesItsAnchorThere() throws Exception {
        // One node, one note: its outline entry and its anchor together.
        Exported exported = export(page -> page.addParagraph(p -> p.text("Go there").linkTo("target"))
                .add(new SectionBuilder().anchor("target").bookmark(new DocumentBookmarkOptions("Entry")).build())
                .addParagraph("After"));

        assertThat(notesOn(exported.report(), "SectionNode")).singleElement()
                .isEqualTo("written as its contents; its outline entry is not written; it " + WHY + LOST);
    }

    @Test
    void aSectionComposedInACellWithANoteOfItsOwnNamesItsAnchorThere() throws Exception {
        // Its drawing is its table's, anchored as the table's cell drawing. Told by its path under the
        // table, it has one note, as in the flow.
        Exported exported = export(page -> page.addParagraph(p -> p.text("Go there").linkTo("target"))
                .addTable(table -> table.autoColumns(1).rowCells(DocumentTableCell.node(new SectionBuilder()
                        .anchor("target").bookmark(new DocumentBookmarkOptions("Entry")).add(drawing()).build()))));

        assertThat(notesOn(exported.report(), "SectionNode")).singleElement().asString()
                .contains("its outline entry is not written")
                .endsWith("it " + WHY + LOST);
        assertThat(exported.document().getDocument().xmlText()).doesNotContain("w:name=\"target\"");
    }

    @Test
    void aSectionInAPanelsCellNamesItsAnchor() throws Exception {
        // What it writes lands in the panel's cell, which holds the panel's paragraph and none of its own.
        Exported exported = export(page -> page.addParagraph(p -> p.text("Go there").linkTo("target"))
                .addSection(panel -> panel.fillColor(DocumentColor.LIGHT_GRAY).padding(DocumentInsets.of(6))
                        .addParagraph("Inside")
                        .add(nothing("target"))));

        assertThat(notesOn(exported.report(), "SectionNode")).contains(WHY + LOST);
    }

    @Test
    void aSectionInATablesComposedCellHasANoteOfItsOwn() throws Exception {
        // Each node in a composed cell is told by its own path under the table: the section inside,
        // with its outline entry, keeps its note, and the anchored one round it has its own.
        DocumentNode inner = new SectionBuilder().bookmark(new DocumentBookmarkOptions("Entry")).add(drawing()).build();
        Exported exported = export(page -> page.addParagraph(p -> p.text("Go there").linkTo("target"))
                .addTable(table -> table.autoColumns(1).rowCells(DocumentTableCell.node(
                        new SectionBuilder().anchor("target").add(inner).build()))));

        List<String> notes = notesOn(exported.report(), "SectionNode");
        assertThat(notes).contains(WHY + LOST);
        assertThat(notes).filteredOn(note -> note.contains("outline entry")).as("the inner section's note")
                .isNotEmpty()
                .allSatisfy(note -> assertThat(note).as("its anchor not added").doesNotContain("\"target\""));
    }

    @Test
    void aBlockWithNothingToMarkTakesNoBookmarkName() throws Exception {
        // Names are kept once given, without case: one given to the section, written before anything
        // asks for "Intro", would have put the paragraph's onto a suffix.
        Exported exported = export(page -> page.add(nothing("intro"))
                .addParagraph(p -> p.text("Go there").linkTo("Intro"))
                .addParagraph(p -> p.text("Here").anchor("Intro")));

        assertThat(exported.document().getDocument().xmlText()).contains("w:name=\"Intro\"")
                .doesNotContainIgnoringCase("Intro_2");
    }

    @Test
    void anAnchorAnotherNodeMarksIsNotNamedOnTheBlock() throws Exception {
        // The paragraph's bookmark holds the name: a link goes there, and a page reference reads it.
        Exported exported = export(page -> page.addParagraph(p -> p.text("Go there").linkTo("target"))
                .add(nothing("target"))
                .addParagraph(p -> p.text("Here").anchor("target")));

        assertThat(exported.report().bySubject()).doesNotContainKey("SectionNode");
        assertThat(notesOn(exported.report(), "anchor")).singleElement().asString()
                .startsWith("\"target\" is the anchor of 2 nodes");
    }

    @Test
    void aSectionThatWritesAParagraphIsBookmarkedAndNamesNothing() throws Exception {
        Exported exported = export(page -> page.addParagraph(p -> p.text("Go there").linkTo("target"))
                .addSection(section -> section.anchor("target").add(drawing()).addParagraph("Caption"))
                .addParagraph("After"));

        assertThat(exported.document().getDocument().xmlText()).contains("w:name=\"target\"");
        assertThat(exported.report().bySubject()).doesNotContainKey("SectionNode");
    }

    @Test
    void anAnchorWithNothingLinkingToItIsNamedAllTheSame() throws Exception {
        // Word's own "go to" a bookmark finds none either.
        Exported exported = export(page -> page.addParagraph("Before")
                .add(nothing("target"))
                .addParagraph("After"));

        assertThat(notesOn(exported.report(), "SectionNode")).singleElement().isEqualTo(WHY + LOST);
    }

    @Test
    void aLaterSectionsNodeAtTheSamePathKeepsItsOwnNote() throws Exception {
        // A path is its section's own: the second section's first block, with its outline entry, has
        // the path the first section's anchored block has.
        List<DocxExportReport.Note> notes = exportSections(
                page -> page.add(nothing("cover")).addParagraph("Cover"),
                page -> page.addSection(section -> section.bookmark(new DocumentBookmarkOptions("Entry")).add(drawing())));

        assertThat(notes).filteredOn(note -> note.subject().equals("SectionNode")).extracting(DocxExportReport.Note::detail)
                .containsExactlyInAnyOrder("written as its contents; its outline entry is not written",
                        WHY + LOST.replace("\"target\"", "\"cover\""));
    }

    @Test
    void blocksAtOnePathInTwoSectionsHaveANoteEach() throws Exception {
        List<DocxExportReport.Note> notes = exportSections(
                page -> page.add(nothing("one")).addParagraph("One"),
                page -> page.add(nothing("two")).addParagraph("Two"));

        assertThat(notes).filteredOn(note -> note.subject().equals("SectionNode")).extracting(DocxExportReport.Note::detail)
                .containsExactlyInAnyOrder(WHY + LOST.replace("\"target\"", "\"one\""),
                        WHY + LOST.replace("\"target\"", "\"two\""));
    }

    @Test
    void aPageReferenceAfterTheBlockKeepsItsNoteInItsPlace() throws Exception {
        // The reference's note is put in where it was counted while writing, after the block's is
        // named: a note put in among the others before it would move that place onto another note.
        Exported exported = export(page -> page.add(nothing("target"))
                .add(drawing())
                .addParagraph("Page:").addPageReference("target")
                .addParagraph("After"));

        assertThat(notesOn(exported.report(), "ShapeNode")).singleElement().asString().startsWith("drawn as a shape");
        assertThat(notesOn(exported.report(), "PageReferenceNode")).singleElement().asString()
                .contains("its anchor has no bookmark in the Word file");
        assertThat(notesOn(exported.report(), "SectionNode")).singleElement().isEqualTo(WHY + LOST);
        assertThat(DocxExports.besidesTheExactLines(exported.report())).extracting(DocxExportReport.Note::subject)
                .as("each where its node was written, the block's own after them")
                .containsExactly("ShapeNode", "PageReferenceNode", "SectionNode");
    }

    /** The notes of a document of two sections, each a page of its own content. */
    @SafeVarargs
    private static List<DocxExportReport.Note> exportSections(Consumer<PageFlowBuilder>... contents) throws Exception {
        List<DocxExportReport.Note> notes = new java.util.ArrayList<>();
        var documents = GraphCompose.documents();
        for (Consumer<PageFlowBuilder> content : contents) {
            DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create();
            session.pageFlow(content::accept);
            documents = documents.section(session);
        }
        try (com.demcha.compose.document.api.MultiSectionDocument document = documents.create()) {
            document.export(DocxSemanticBackend.builder().reportSink(report -> notes.addAll(report.notes())).build());
        }
        return notes;
    }

    /** An anchored section with nothing in it: it writes no paragraph and draws nothing. */
    private static DocumentNode nothing(String anchor) {
        return new SectionBuilder().anchor(anchor).build();
    }

    private static DocumentNode drawing() {
        return new ShapeBuilder().size(120, 30).fillColor(DocumentColor.LIGHT_GRAY).build();
    }

    private static List<String> notesOn(DocxExportReport report, String subject) {
        return report.notes().stream().filter(note -> note.subject().equals(subject))
                .map(DocxExportReport.Note::detail).toList();
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
