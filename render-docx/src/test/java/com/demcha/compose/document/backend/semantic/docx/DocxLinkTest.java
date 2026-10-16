package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.node.DocumentLinkTarget;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A link in the document is a link in the file.
 *
 * <p>Every one was dropped: a reader opened an exported document, found the text of a link
 * with nothing behind it, and a reference to another section that went nowhere. For a
 * document handed over to be edited rather than read, that is most of the point of handing
 * it over.</p>
 *
 * <p>Word owns both kinds — {@code w:hyperlink} with a relationship for an address, or with
 * {@code w:anchor} for a bookmark in the same document — so what these pin is a mapping,
 * not an approximation.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxLinkTest {

    @Test
    void aParagraphLinkBecomesAHyperlinkWithItsAddress() throws Exception {
        try (XWPFDocument document = exported(page -> page.addParagraph(p -> p
                .text("The licence")
                .linkTarget(DocumentLinkTarget.external("https://example.org/licence"))))) {

            XWPFHyperlinkRun run = onlyHyperlink(document);
            assertThat(run.getText(0)).isEqualTo("The licence");
            assertThat(addressOf(document, run))
                    .as("the address the document asked for, through a real relationship")
                    .isEqualTo("https://example.org/licence");
        }
    }

    @Test
    void oneLinkedPhraseLeavesTheRestOfTheSentenceAlone() throws Exception {
        try (XWPFDocument document = exported(page -> page.addParagraph(p -> p
                .inlineText("See ")
                .inlineLink("the licence",
                        new com.demcha.compose.document.node.DocumentLinkOptions("https://example.org/licence"))
                .inlineText(" for the terms.")))) {

            XWPFParagraph para = document.getParagraphs().get(0);
            assertThat(para.getText()).isEqualTo("See the licence for the terms.");
            List<XWPFHyperlinkRun> links = para.getRuns().stream()
                    .filter(XWPFHyperlinkRun.class::isInstance)
                    .map(XWPFHyperlinkRun.class::cast)
                    .toList();
            assertThat(links).hasSize(1);
            assertThat(links.get(0).getText(0)).isEqualTo("the licence");
        }
    }

    @Test
    void anInternalLinkPointsAtTheBookmarkItsAnchorBecame() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text("Back to terms")
                        .linkTarget(DocumentLinkTarget.anchor("Terms & conditions")))
                .addParagraph(p -> p.text("Terms and conditions")
                        .anchor("Terms & conditions")))) {

            String anchor = onlyHyperlink(document).getAnchor();
            assertThat(anchor)
                    .as("cleaned to what Word accepts: letters, digits and underscores")
                    .isEqualTo("Terms_conditions");
            assertThat(bookmarkNames(document))
                    .as("and the bookmark carries the same name, or the link goes nowhere")
                    .containsExactly(anchor);
        }
    }

    @Test
    void aForwardReferenceResolvesJustAsWell() throws Exception {
        // The link is written before the anchor it points at exists in the file. Nothing
        // resolves anything at write time — both sides go through the same naming — so the
        // order the document happens to be in does not matter.
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text("Jump ahead").linkTarget(DocumentLinkTarget.anchor("appendix")))
                .addParagraph(p -> p.text("Appendix").anchor("appendix")))) {

            assertThat(onlyHyperlink(document).getAnchor()).isEqualTo("appendix");
            assertThat(bookmarkNames(document)).containsExactly("appendix");
        }
    }

    @Test
    void theBookmarkWrapsTheTextRatherThanSittingBeforeIt() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text("Appendix").anchor("appendix")))) {

            String xml = document.getParagraphs().get(0).getCTP().xmlText();
            assertThat(xml.indexOf("bookmarkStart"))
                    .as("opened before the text")
                    .isLessThan(xml.indexOf("<w:r>"));
            assertThat(xml.indexOf("bookmarkEnd"))
                    .as("and closed after it, so the reader lands on the paragraph")
                    .isGreaterThan(xml.indexOf("<w:r>"));
        }
    }

    @Test
    void twoAnchorsThatCleanToTheSameTextStayApart() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text("One").anchor("Section 1"))
                .addParagraph(p -> p.text("Two").anchor("Section-1")))) {

            assertThat(bookmarkNames(document))
                    .as("two anchors are two bookmarks, whatever they clean to")
                    .doesNotHaveDuplicates()
                    .hasSize(2);
        }
    }

    @Test
    void aNameASuffixMakesIsNoOtherAnchorsName() throws Exception {
        // "Section-1" takes "Section_1_2" after "Section 1"; "Section 1 2" cleans to that name too.
        // Named in the order written: the anchors first, the links to them after.
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text("One").anchor("Section 1"))
                .addParagraph(p -> p.text("Two").anchor("Section-1"))
                .addParagraph(p -> p.text("Three").anchor("Section 1 2"))
                .addParagraph(p -> p.text("To two").linkTo("Section-1"))
                .addParagraph(p -> p.text("To three").linkTo("Section 1 2")))) {

            assertThat(bookmarkNames(document)).doesNotHaveDuplicates().hasSize(3);
            assertThat(bookmarkIn(document, "Two")).isEqualTo("Section_1_2");
            assertThat(bookmarkIn(document, "Three")).isEqualTo("Section_1_2_2");
            assertThat(linkAnchor(document.getParagraphs().get(3))).isEqualTo(bookmarkIn(document, "Two"));
            assertThat(linkAnchor(document.getParagraphs().get(4))).isEqualTo(bookmarkIn(document, "Three"));
        }
    }

    @Test
    void anAnchorTwoNodesDeclareIsMarkedOnTheLastAlone() throws Exception {
        // The page sends a link to the last node declaring it; Word goes to a name's first bookmark.
        List<DocxExportReport.Note> notes = new java.util.ArrayList<>();
        byte[] docx;
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(com.demcha.compose.document.style.DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page
                    .addParagraph(p -> p.text("To it").linkTo("dup"))
                    .addParagraph(p -> p.text("First").anchor("dup"))
                    .addParagraph(p -> p.text("Second").anchor("dup")));
            docx = session.export(DocxSemanticBackend.builder().reportSink(report -> notes.addAll(report.notes())).build());
        }
        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(docx))) {
            assertThat(bookmarkNames(document)).containsExactly("dup");
            assertThat(bookmarkIn(document, "Second")).isEqualTo("dup");
            assertThat(document.getParagraphs()).allSatisfy(paragraph -> assertThat(
                    paragraph.getCTP().getBookmarkEndList().size()).isEqualTo(paragraph.getCTP().getBookmarkStartList().size()));
        }
        assertThat(notes).filteredOn(note -> note.subject().equals("anchor")).singleElement()
                .satisfies(note -> assertThat(note.detail()).startsWith("\"dup\" is the anchor of 2 nodes"));
    }

    @Test
    void aBlocksBookmarkBeforeTheLastDeclarationIsTakenOutWhole() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addSection(block -> block.anchor("dup").addParagraph("In the block").addParagraph("Still in it"))
                .addParagraph(p -> p.text("Later").anchor("dup")))) {
            String xml = document.getDocument().xmlText();

            assertThat(bookmarkNames(document)).containsExactly("dup");
            assertThat(bookmarkIn(document, "Later")).isEqualTo("dup");
            assertThat(count(xml, "<w:bookmarkEnd ")).as("its end with it").isEqualTo(count(xml, "<w:bookmarkStart "));
        }
    }

    @Test
    void aBlockAndANodeInsideItSharingAnAnchorKeepTheInnersBookmark() throws Exception {
        // The page registers the block before what it holds, and goes to the last: the paragraph
        // inside. The block's bookmark opens before it in the file, though it is written after.
        try (XWPFDocument document = exported(page -> page
                .addSection(block -> block.anchor("x").addParagraph("Opening").addParagraph(p -> p.text("Inner").anchor("x"))))) {
            assertThat(bookmarkNames(document)).containsExactly("x");
            assertThat(bookmarkIn(document, "Inner")).isEqualTo("x");
        }
    }

    @Test
    void anAnchoredPanelRunOntoTheNextPageIsMarkedWhereItStarts() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(300, 200, 20, page -> page.addSection(card -> {
            card.anchor("card").fillColor(com.demcha.compose.document.style.DocumentColor.LIGHT_GRAY);
            for (int line = 0; line < 30; line++) {
                card.addParagraph("Line " + line);
            }
        }))) {
            List<String> names = document.getTables().stream()
                    .flatMap(table -> table.getRows().stream()).flatMap(row -> row.getTableCells().stream())
                    .flatMap(cell -> cell.getParagraphs().stream())
                    .flatMap(p -> p.getCTP().getBookmarkStartList().stream()).map(bookmark -> bookmark.getName())
                    .toList();
            String first = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0).getCTP()
                    .getBookmarkStartList().stream().map(bookmark -> bookmark.getName()).findFirst().orElse(null);

            assertThat(names).containsExactly("card");
            assertThat(first).as("on the panel's first line").isEqualTo("card");
        }
    }

    @Test
    void anAnchorWhoseLastDeclarationHasNoBookmarkIsNamed() throws Exception {
        // A drawn shape is no paragraph a bookmark could mark: the page goes to it, Word to the text.
        List<DocxExportReport.Note> notes = new java.util.ArrayList<>();
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(com.demcha.compose.document.style.DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page
                    .addParagraph(p -> p.text("First").anchor("dup"))
                    .addShape(shape -> shape.size(20, 20).anchor("dup")));
            session.export(DocxSemanticBackend.builder().reportSink(report -> notes.addAll(report.notes())).build());
        }
        assertThat(notes).filteredOn(note -> note.subject().equals("anchor")).singleElement()
                .satisfies(note -> assertThat(note.detail()).startsWith("\"dup\" is the anchor of 2 nodes"));
    }

    @Test
    void twoCellsOfOneTableDeclaringAnAnchorAreTwoNodes() throws Exception {
        // The layout registers both under the table's path: counted off it, they were one node.
        List<DocxExportReport.Note> notes = new java.util.ArrayList<>();
        byte[] docx;
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(com.demcha.compose.document.style.DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addTable(t -> t
                    .columns(com.demcha.compose.document.table.DocumentTableColumn.fixed(200))
                    .rowCells(com.demcha.compose.document.table.DocumentTableCell.node(
                            new com.demcha.compose.document.dsl.ParagraphBuilder().text("One").anchor("dup").build()))
                    .rowCells(com.demcha.compose.document.table.DocumentTableCell.node(
                            new com.demcha.compose.document.dsl.ParagraphBuilder().text("Two").anchor("dup").build()))));
            docx = session.export(DocxSemanticBackend.builder().reportSink(report -> notes.addAll(report.notes())).build());
        }
        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(docx))) {
            String xml = document.getDocument().xmlText();
            assertThat(count(xml, "w:bookmarkStart")).isEqualTo(1);
            assertThat(xml.substring(xml.indexOf("w:bookmarkStart"), xml.indexOf("w:bookmarkEnd")))
                    .as("the last cell's").contains(">Two<").doesNotContain(">One<");
        }
        assertThat(notes).filteredOn(note -> note.subject().equals("anchor")).singleElement()
                .satisfies(note -> assertThat(note.detail()).startsWith("\"dup\" is the anchor of 2 nodes"));

        // The second a drawn circle, the file writes one bookmark: only the nodes say there are two.
        List<DocxExportReport.Note> drawn = new java.util.ArrayList<>();
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(com.demcha.compose.document.style.DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addTable(t -> t
                    .columns(com.demcha.compose.document.table.DocumentTableColumn.fixed(200))
                    .rowCells(com.demcha.compose.document.table.DocumentTableCell.node(
                            new com.demcha.compose.document.dsl.ParagraphBuilder().text("One").anchor("dup").build()))
                    .rowCells(com.demcha.compose.document.table.DocumentTableCell.node(
                            new com.demcha.compose.document.dsl.EllipseBuilder().circle(10)
                                    .fillColor(com.demcha.compose.document.style.DocumentColor.BLACK)
                                    .anchor("dup").build()))));
            session.export(DocxSemanticBackend.builder().reportSink(report -> drawn.addAll(report.notes())).build());
        }
        assertThat(drawn).filteredOn(note -> note.subject().equals("anchor")).singleElement()
                .satisfies(note -> assertThat(note.detail()).startsWith("\"dup\" is the anchor of 2 nodes"));
    }

    @Test
    void anAnchorInATableHeaderThePageRepeatsIsNamed() throws Exception {
        // Drawn again on every page the header repeats on, the page goes to the last; the file holds
        // the row once, where the table starts (measured: Word puts the bookmark on page 1, and a page
        // reference reading 5 reads 1 once updated).
        String named = "\"hdr\" is in a table's header row, which the page repeats on 5 pages: a link to it goes "
                       + "to the last of them, and a page reference reads that one's page; the Word file holds the "
                       + "row once, where the table starts, which its link goes to and a page reference reads once "
                       + "Word updates it";
        com.demcha.compose.document.node.DocumentNode item =
                new com.demcha.compose.document.dsl.ParagraphBuilder().text("Item").anchor("hdr").build();
        assertThat(headerAnchorNotes(item, 30)).singleElement().isEqualTo(named);
        assertThat(headerAnchorNotes(new com.demcha.compose.document.dsl.SectionBuilder().addParagraph(
                p -> p.text("Item").anchor("hdr")).build(), 30))
                .as("declared inside a block the header cell is built from").singleElement().isEqualTo(named);
        assertThat(headerAnchorNotes(item, 3)).as("a header drawn on one page is drawn once").isEmpty();
        assertThat(headerAnchorNotes(new com.demcha.compose.document.dsl.EllipseBuilder().circle(10)
                .fillColor(com.demcha.compose.document.style.DocumentColor.BLACK).anchor("hdr").build(), 30))
                .as("a drawn circle has no bookmark for a link to go to, which the page reference's note says")
                .isEmpty();
    }

    private static List<String> headerAnchorNotes(com.demcha.compose.document.node.DocumentNode item, int rows)
            throws Exception {
        List<DocxExportReport.Note> notes = new java.util.ArrayList<>();
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(300, 200).margin(com.demcha.compose.document.style.DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addTable(table -> {
                table.autoColumns(2).repeatHeader().headerCells(
                        com.demcha.compose.document.table.DocumentTableCell.node(item),
                        com.demcha.compose.document.table.DocumentTableCell.text("Amount"));
                for (int row = 0; row < rows; row++) {
                    table.row("Row " + row, "1");
                }
            }).addPageReference("hdr"));
            session.export(DocxSemanticBackend.builder().reportSink(report -> notes.addAll(report.notes())).build());
        }
        return notes.stream().filter(note -> note.subject().equals("anchor")).map(DocxExportReport.Note::detail).toList();
    }

    @Test
    void anAnchorStartingWithADigitIsStillAName() throws Exception {
        // Word will not take a bookmark whose name starts with anything but a letter, and
        // refuses the file rather than the bookmark.
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text("Clause").anchor("2024-terms")))) {

            assertThat(bookmarkNames(document)).hasSize(1);
            assertThat(bookmarkNames(document).get(0)).matches("^[A-Za-z][A-Za-z0-9_]*$");
        }
    }

    @Test
    void aParagraphWithNoLinkCarriesNoHyperlink() throws Exception {
        try (XWPFDocument document = exported(page -> page.addParagraph(p -> p.text("Plain")))) {
            assertThat(document.getParagraphs().get(0).getCTP().sizeOfHyperlinkArray()).isZero();
            assertThat(bookmarkNames(document)).isEmpty();
        }
    }

    @Test
    void aLinkInsideAListItemIsStillALink() throws Exception {
        // A list item made of runs is written by its own path, which learned styles and
        // chips but not links: the same phrase was a link in a sentence and dead text in a
        // bullet. hangingIndent, because an item made of runs is laid out only with it.
        try (XWPFDocument document = exported(page -> page
                .addList(list -> list
                        .bullet()
                        .hangingIndent(true)
                        .addItem(rich -> rich.plain("Read the ")
                                .link("guide", "https://graphcompose.dev/guide")
                                .plain(" first"))))) {

            XWPFHyperlinkRun link = onlyHyperlink(document);
            assertThat(link.text()).isEqualTo("guide");
            assertThat(addressOf(document, link)).isEqualTo("https://graphcompose.dev/guide");
            assertThat(document.getParagraphs().get(0).getText())
                    .as("the rest of the item is still there, around the link")
                    .contains("Read the guide first");
        }
    }

    @Test
    void anInternalLinkInsideAListItemPointsAtItsBookmark() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addList(list -> list
                        .bullet()
                        .hangingIndent(true)
                        .addItem(rich -> rich.plain("See ").linkTo("terms", "terms")))
                .addParagraph(p -> p.text("Terms").anchor("terms")))) {

            List<String> anchors = document.getParagraphs().stream()
                    .flatMap(p -> p.getCTP().getHyperlinkList().stream())
                    .map(hyperlink -> hyperlink.getAnchor())
                    .toList();
            assertThat(anchors).containsExactly("terms");
            assertThat(bookmarkNames(document)).contains("terms");
        }
    }

    private static XWPFHyperlinkRun onlyHyperlink(XWPFDocument document) {
        List<XWPFHyperlinkRun> links = document.getParagraphs().stream()
                .flatMap(p -> p.getRuns().stream())
                .filter(XWPFHyperlinkRun.class::isInstance)
                .map(XWPFHyperlinkRun.class::cast)
                .toList();
        assertThat(links).hasSize(1);
        return links.get(0);
    }

    /** The address behind a hyperlink run, read through the part's own relationship. */
    private static String addressOf(XWPFDocument document, XWPFHyperlinkRun run) {
        PackageRelationship relationship = document.getPackagePart()
                .getRelationship(run.getHyperlinkId());
        assertThat(relationship).as("the run points at a relationship that exists").isNotNull();
        return relationship.getTargetURI().toString();
    }

    private static List<String> bookmarkNames(XWPFDocument document) {
        return document.getParagraphs().stream()
                .flatMap(p -> p.getCTP().getBookmarkStartList().stream())
                .map(bookmark -> bookmark.getName())
                .toList();
    }

    /** The name of the bookmark in the paragraph reading a text, or null where it holds none. */
    private static String bookmarkIn(XWPFDocument document, String text) {
        return document.getParagraphs().stream().filter(p -> p.getText().equals(text))
                .flatMap(p -> p.getCTP().getBookmarkStartList().stream()).map(bookmark -> bookmark.getName())
                .findFirst().orElse(null);
    }

    private static int count(String xml, String element) {
        return xml.split(java.util.regex.Pattern.quote(element), -1).length - 1;
    }

    /** The bookmark an internal link in a paragraph points at. */
    private static String linkAnchor(XWPFParagraph paragraph) {
        return paragraph.getCTP().getHyperlinkList().get(0).getAnchor();
    }

    private static XWPFDocument exported(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.withLayout(400, 600, 20, content);
    }
}
