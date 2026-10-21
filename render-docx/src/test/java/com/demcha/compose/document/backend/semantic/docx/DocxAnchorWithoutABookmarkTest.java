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
 * A block that writes no paragraph names the anchor it declares, where no bookmark marks it.
 *
 * <p>A bookmark goes round the paragraphs a block writes. A section of drawings alone writes none:
 * its drawings stand in the paragraph beside them. A link to its anchor is written all the same, to
 * a bookmark the file lacks. Measured in Word 16 and LibreOffice on Windows: the page's link went to
 * the section's place on page 1, and in both editors' PDFs the text carried no link at all, with
 * nothing in the report. A page reference to it was already named, as the number it is written
 * as.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxAnchorWithoutABookmarkTest {

    private static final String LOST = "its anchor \"target\" has no bookmark in the Word file: a link to it points "
                                       + "at none, and a page reference to it is a fixed number rather than a field";

    @Test
    void aSectionOfDrawingsAloneNamesItsAnchor() throws Exception {
        Exported exported = export(page -> page.addParagraph(p -> p.text("Go there").linkTo("target"))
                .addSection(section -> section.anchor("target").add(drawing()))
                .addParagraph("After"));

        assertThat(notesOn(exported.report(), "SectionNode")).singleElement()
                .isEqualTo("writes no paragraph, so " + LOST);
        String xml = exported.document().getDocument().xmlText();
        assertThat(xml).as("the link, written all the same").contains("w:anchor=\"target\"");
        assertThat(xml).as("no bookmark for it to go to").doesNotContain("w:name=\"target\"");
    }

    @Test
    void aContainerOfDrawingsAloneNamesItsAnchor() throws Exception {
        Exported exported = export(page -> page.addParagraph(p -> p.text("Go there").linkTo("target"))
                .add(new com.demcha.compose.document.node.ContainerNode("Box", List.of(drawing()), 0,
                        DocumentInsets.zero(), DocumentInsets.zero(), null, null, null, null, "target"))
                .addParagraph("After"));

        assertThat(notesOn(exported.report(), "ContainerNode")).singleElement()
                .isEqualTo("writes no paragraph, so " + LOST);
    }

    @Test
    void aSectionWithANoteOfItsOwnNamesItsAnchorThere() throws Exception {
        // One node, one note: its outline entry and its anchor together.
        Exported exported = export(page -> page.addParagraph(p -> p.text("Go there").linkTo("target"))
                .addSection(section -> section.anchor("target").bookmark(new DocumentBookmarkOptions("Entry"))
                        .add(drawing())));

        assertThat(notesOn(exported.report(), "SectionNode")).singleElement()
                .isEqualTo("written as its contents; its outline entry is not written; it writes no paragraph, so "
                           + LOST);
    }

    @Test
    void aSectionInAPanelsCellNamesItsAnchor() throws Exception {
        // What it writes lands in the panel's cell, which holds the panel's paragraph and none of its own.
        Exported exported = export(page -> page.addParagraph(p -> p.text("Go there").linkTo("target"))
                .addSection(panel -> panel.fillColor(DocumentColor.LIGHT_GRAY).padding(DocumentInsets.of(6))
                        .addParagraph("Inside")
                        .add(new SectionBuilder().anchor("target").add(drawing()).build())));

        assertThat(notesOn(exported.report(), "SectionNode")).contains("writes no paragraph, so " + LOST);
    }

    @Test
    void aSectionInATablesComposedCellHasANoteOfItsOwn() throws Exception {
        // Nothing in a composed cell has a path of its own to tell it by: the section inside, with its
        // outline entry, keeps its note, and the anchored one round it has its own.
        DocumentNode inner = new SectionBuilder().bookmark(new DocumentBookmarkOptions("Entry")).add(drawing()).build();
        Exported exported = export(page -> page.addParagraph(p -> p.text("Go there").linkTo("target"))
                .addTable(table -> table.autoColumns(1).rowCells(DocumentTableCell.node(
                        new SectionBuilder().anchor("target").add(inner).build()))));

        List<String> notes = notesOn(exported.report(), "SectionNode");
        assertThat(notes).contains("writes no paragraph, so " + LOST);
        assertThat(notes).filteredOn(note -> note.contains("outline entry")).as("the inner section's note")
                .isNotEmpty()
                .allSatisfy(note -> assertThat(note).as("its anchor not added").doesNotContain("\"target\""));
    }

    @Test
    void aBlockWithNothingToMarkTakesNoBookmarkName() throws Exception {
        // Names are kept once given, without case: one given to the section, written before anything
        // asks for "Intro", would have put the paragraph's onto a suffix.
        Exported exported = export(page -> page.addSection(section -> section.anchor("intro").add(drawing()))
                .addParagraph(p -> p.text("Go there").linkTo("Intro"))
                .addParagraph(p -> p.text("Here").anchor("Intro")));

        assertThat(exported.document().getDocument().xmlText()).contains("w:name=\"Intro\"")
                .doesNotContain("Intro_2");
    }

    @Test
    void anAnchorAnotherNodeMarksIsNotNamedOnTheBlock() throws Exception {
        // The paragraph's bookmark holds the name: a link goes there, and a page reference reads it.
        Exported exported = export(page -> page.addParagraph(p -> p.text("Go there").linkTo("target"))
                .addSection(section -> section.anchor("target").add(drawing()))
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
                .addSection(section -> section.anchor("target").add(drawing())));

        assertThat(notesOn(exported.report(), "SectionNode")).singleElement()
                .isEqualTo("writes no paragraph, so " + LOST);
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
