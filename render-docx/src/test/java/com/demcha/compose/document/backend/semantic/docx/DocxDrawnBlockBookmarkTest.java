package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageMarginRule;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFldChar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHyperlink;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTMarkupRange;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A block of drawings alone is bookmarked at the start of the paragraph its first drawing is
 * anchored in.
 *
 * <p>A section or a container holding nothing but drawings writes no paragraph for a bookmark to go
 * round: its drawings are anchored in the paragraphs beside them, or in one written on their page,
 * when the section ends. Its anchor had no bookmark: a link to it named a bookmark the file did not
 * hold, and a page reference to it was written as the number it read. Word keeps a drawing on the
 * page of the paragraph it is anchored in, and moves it with that paragraph. Measured in Word 16
 * (fields updated) and LibreOffice 24.2, on such a block on page 2 and the same file with a page put
 * in before it: before, both showed 2 in each and carried no link; with the bookmark in the
 * paragraph holding its first drawing, both show 2 and then 3, and the link goes to the block's page
 * — a section, a container, one opening its page, one alone on it, one at a page's foot, one in a
 * panel and an anchor named with spaces alike.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxDrawnBlockBookmarkTest {

    @Test
    void aSectionOfDrawingsAloneIsMarkedWhereItsFirstDrawingIsAnchored() throws Exception {
        // Its shape, the higher of its drawings, stands beside the paragraph above it; its circle,
        // beside the one under it.
        Exported exported = export(page -> {
            referTo("drawn").accept(page);
            page.addPageBreak(b -> { });
            lines(page, "Top of page two", 3);
            page.add(drawings("drawn"));
            page.addParagraph("After the drawings");
        });

        XWPFParagraph holder = placeMarked(exported.document(), "drawn");
        assertThat(holder.getText()).isEqualTo("Top of page two 3");
        assertThat(drawingsIn(holder)).as("it carries the block's first drawing").isPositive();
        assertThat(placeMarksIn(paragraphWith(exported.document(), "After the drawings")))
                .as("the paragraph carrying its circle holds no mark").isEmpty();
        assertThat(pageReferences(exported.document())).containsExactly(Map.entry("PAGEREF drawn \\h", "2"));
        assertThat(internalLinks(exported.document())).containsExactly("drawn");
        assertThat(exported.report().notes()).noneMatch(note -> note.detail().contains("bookmark"));
    }

    @Test
    void aContainerOfDrawingsAloneIsMarkedTheSameWay() throws Exception {
        Exported exported = export(page -> {
            referTo("box").accept(page);
            page.addPageBreak(b -> { });
            lines(page, "Top of page two", 3);
            page.add(new ContainerNode("Box", List.of(drawings(null)), 0, DocumentInsets.zero(),
                    DocumentInsets.zero(), null, null, null, null, "box"));
            page.addParagraph("After the drawings");
        });

        assertThat(placeMarked(exported.document(), "box").getText()).isEqualTo("Top of page two 3");
        assertThat(pageReferences(exported.document())).containsExactly(Map.entry("PAGEREF box \\h", "2"));
        assertThat(internalLinks(exported.document())).containsExactly("box");
        assertThat(exported.report().notes()).noneMatch(note -> note.detail().contains("bookmark"));
    }

    @Test
    void aBlockCarriedByARowATableIsSplitAtForItsPagesMarginsIsMarkedInThatRow() throws Exception {
        // Page 2 has other margins, so the table is split where page 2 starts. The block's shape stands
        // at page 2's foot, which no paragraph of the body is written on: a row of the table on page 2
        // carries it, and that row goes to the second part. Marked once the document was written, the
        // row's paragraph was the first part's, taken out of the file by then: the export threw.
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 300).margin(DocumentInsets.of(20))
                .create()) {
            session.pageMargins(List.of(PageMarginRule.from(2, new DocumentInsets(45, 20, 34, 20))));
            session.pageFlow(page -> {
                page.addParagraph("See page").addPageReference("drawn");
                page.addTable(table -> {
                    table.columns(DocumentTableColumn.fixed(200));
                    for (int row = 1; row <= 19; row++) {
                        table.rowCells(DocumentTableCell.text("Row " + row));
                    }
                });
                page.add(drawings("drawn"));
                page.addParagraph("After the drawings");
            });
            assertThat(session.layoutGraph().nodes()).as("the premise: its shape at page 2's foot, its circle on 3")
                    .filteredOn(node -> node.path().endsWith("/SectionNode[3]"))
                    .singleElement()
                    .satisfies(node -> assertThat(new int[]{node.startPage(), node.endPage()}).containsExactly(1, 2));
            docx = session.export(new DocxSemanticBackend());
        }
        XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx));

        XWPFParagraph holder = placeMarked(document, "drawn");
        assertThat(holder.getBody()).as("in a row of the table").isInstanceOf(XWPFTableCell.class);
        assertThat(document.getTables()).as("the premise: the table split in two").hasSize(2);
        assertThat(((XWPFTableCell) holder.getBody()).getTableRow().getTable())
                .as("the row went to the second part").isSameAs(document.getTables().get(1));
        assertThat(pageReferences(document)).containsExactly(Map.entry("PAGEREF drawn \\h", "2"));
    }

    @Test
    void aBlockAboveATableSplitForItsPagesMarginsIsMarkedOnce() throws Exception {
        // The block opens page 2 above a table with a repeated header row; page 3 has other margins,
        // so the table is split where it starts, under a copy of the header row. The paragraph holding
        // the block's room above the table carries its drawings and its bookmark, written once.
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 300).margin(DocumentInsets.of(20))
                .create()) {
            session.pageMargins(List.of(PageMarginRule.from(3, new DocumentInsets(45, 20, 34, 20))));
            session.pageFlow(page -> {
                referTo("drawn").accept(page);
                page.addPageBreak(b -> { });
                page.add(drawings("drawn"));
                page.addTable(table -> {
                    table.columns(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(100))
                            .header("Item", "Amount").repeatHeader();
                    for (int row = 1; row <= 20; row++) {
                        table.rowCells(DocumentTableCell.text("Row " + row), DocumentTableCell.text(String.valueOf(row)));
                    }
                });
                page.addParagraph("After the table");
            });
            docx = session.export(new DocxSemanticBackend());
        }
        XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx));

        assertThat(document.getTables()).as("the premise: the table split where page 3 starts").hasSize(2);
        XWPFParagraph holder = placeMarked(document, "drawn");
        List<IBodyElement> body = document.getBodyElements();
        assertThat(body.get(body.indexOf(holder) + 1)).as("the paragraph right above the table")
                .isSameAs(document.getTables().get(0));
        assertThat(drawingsIn(holder)).isEqualTo(2);
        for (XWPFTable part : document.getTables()) {
            assertThat(part.getRow(0).getCtRow().xmlText()).as("each part's header row").doesNotContain("bookmark");
        }
        assertThat(pageReferences(document)).containsExactly(Map.entry("PAGEREF drawn \\h", "2"));
    }

    @Test
    void aBlocksNameIsGivenAfterTheBodyNamedItsAnchors() throws Exception {
        // Word reads a bookmark's name without case. The block's bookmark is written once its section's
        // drawings are anchored, after the paragraph's: the paragraph keeps the name its anchor cleans to.
        Exported exported = export(page -> {
            lines(page, "Top", 1);
            page.add(drawings("intro"));
            page.addParagraph(p -> p.text("Here").anchor("Intro"));
            page.addParagraph(p -> p.text("Go there").linkTo("Intro"));
        });

        assertThat(paragraphWith(exported.document(), "Here").getCTP().getBookmarkStartList())
                .extracting(CTBookmark::getName).containsExactly("Intro");
        assertThat(placeMarked(exported.document(), "intro_2").getText()).isEqualTo("Top 1");
        assertThat(internalLinks(exported.document())).containsExactly("Intro");
    }

    @Test
    void aBlockRunningOntoTheNextPageIsMarkedOnThePageItStartsOn() throws Exception {
        // The layout starts it at the foot of page 1, its shape there and its circle on page 2.
        Exported exported = export(page -> {
            referTo("drawn").accept(page);
            lines(page, "Filler", 27);
            page.add(drawings("drawn"));
            lines(page, "Next page", 2);
        });

        assertThat(placeMarked(exported.document(), "drawn").getText()).isEqualTo("Filler 27");
        assertThat(drawingsIn(paragraphWith(exported.document(), "Next page 1")))
                .as("the premise: its circle is on page 2").isPositive();
        assertThat(pageReferences(exported.document())).containsExactly(Map.entry("PAGEREF drawn \\h", "1"));
    }

    @Test
    void aBlockOpeningItsPageIsMarkedInTheParagraphUnderIt() throws Exception {
        Exported exported = export(page -> {
            referTo("drawn").accept(page);
            page.addPageBreak(b -> { });
            page.add(drawings("drawn"));
            page.addParagraph("After the drawings");
        });

        assertThat(placeMarked(exported.document(), "drawn").getText()).isEqualTo("After the drawings");
        assertThat(pageReferences(exported.document())).containsExactly(Map.entry("PAGEREF drawn \\h", "2"));
    }

    @Test
    void aBlockAloneOnItsPageIsMarkedInTheParagraphClosingTheSection() throws Exception {
        // No paragraph is written on its page: the one closing the section carries its drawings.
        Exported exported = export(page -> {
            referTo("drawn").accept(page);
            page.addPageBreak(b -> { });
            page.add(drawings("drawn"));
        });

        List<IBodyElement> body = exported.document().getBodyElements();
        XWPFParagraph holder = placeMarked(exported.document(), "drawn");
        assertThat(holder.getText()).isEmpty();
        assertThat(body.get(body.size() - 1)).as("the section's closing paragraph").isSameAs(holder);
        assertThat(drawingsIn(holder)).isEqualTo(2);
        assertThat(pageReferences(exported.document())).containsExactly(Map.entry("PAGEREF drawn \\h", "2"));
    }

    @Test
    void aBlockInAPanelIsMarkedInThePanelsCell() throws Exception {
        Exported exported = export(page -> {
            referTo("inpanel").accept(page);
            page.addSection(panel -> panel.fillColor(DocumentColor.rgb(238, 242, 247)).padding(DocumentInsets.of(10))
                    .addParagraph("Inside the panel")
                    .add(drawings("inpanel"))
                    .addParagraph("Under the drawings"));
        });

        XWPFParagraph holder = placeMarked(exported.document(), "inpanel");
        assertThat(holder.getBody()).as("in the panel's cell").isInstanceOf(XWPFTableCell.class);
        assertThat(holder.getText()).isEqualTo("Inside the panel");
        assertThat(pageReferences(exported.document())).containsExactly(Map.entry("PAGEREF inpanel \\h", "1"));
    }

    @Test
    void anAnchorNamedWithSpacesIsOneNameForItsBookmarkItsLinkAndItsReference() throws Exception {
        // Word keeps 40 characters of a name, letters, digits and underscores: the name is made once
        // for the anchor, so the field and the link name the bookmark it is written under.
        String spaced = "Terms and conditions of the agreement between both parties";
        String name = "Terms_and_conditions_of_the_agreement_be";
        for (boolean drawn : new boolean[]{true, false}) {
            Exported exported = export(page -> {
                referTo(spaced).accept(page);
                page.add(drawn ? drawings(spaced)
                        : new SectionBuilder().anchor(spaced).addParagraph("Anchored text").build());
            });

            assertThat(name).hasSize(40);
            assertThat(bookmarkNames(exported.document())).as(drawn ? "drawings alone" : "text").containsExactly(name);
            assertThat(internalLinks(exported.document())).containsExactly(name);
            assertThat(pageReferences(exported.document())).containsExactly(Map.entry("PAGEREF " + name + " \\h", "1"));
        }
    }

    @Test
    void twoBlocksMarkedInOneParagraphKeepAMarkEach() throws Exception {
        // Both open their page, and the paragraph under them carries the drawings of each.
        Exported exported = export(page -> {
            page.addParagraph("See page").addPageReference("one").addPageReference("two");
            page.addPageBreak(b -> { });
            page.add(drawings("one"));
            page.add(drawings("two"));
            page.addParagraph("After the drawings");
        });

        XWPFParagraph holder = paragraphWith(exported.document(), "After the drawings");
        assertThat(placeMarksIn(holder)).containsExactlyInAnyOrder("one", "two");
        assertThat(pageReferences(exported.document())).containsExactly(
                Map.entry("PAGEREF one \\h", "2"), Map.entry("PAGEREF two \\h", "2"));
    }

    @Test
    void anotherNodeDeclaringTheAnchorAfterTheBlockKeepsTheBookmark() throws Exception {
        // A link to an anchor two nodes declare goes, on the page, to the last; the file keeps the
        // last bookmark it holds of the name, the paragraph's, and names the anchor.
        Exported exported = export(page -> {
            lines(page, "Top", 1);
            page.add(drawings("target"));
            page.addParagraph(p -> p.text("Here").anchor("target"));
        });

        assertThat(bookmarkNames(exported.document())).containsExactly("target");
        assertThat(paragraphWith(exported.document(), "Here").getCTP().getBookmarkStartList())
                .extracting(CTBookmark::getName).containsExactly("target");
        assertThat(placeMarksIn(paragraphWith(exported.document(), "Top 1"))).as("the block's taken out").isEmpty();
        assertThat(exported.report().notes()).filteredOn(note -> note.subject().equals("anchor")).singleElement()
                .satisfies(note -> assertThat(note.detail()).startsWith("\"target\" is the anchor of 2 nodes"));
    }

    @Test
    void aBlockThatDrawsNothingIsNotNamedWhereALaterBlockOfDrawingsMarksItsAnchor() throws Exception {
        // The empty one is written first; whether the anchor is marked is known once every block of
        // drawings has its mark.
        Exported exported = export(page -> {
            lines(page, "Top", 1);
            page.add(new SectionBuilder().anchor("target").build());
            page.add(drawings("target"));
            page.addParagraph("After the drawings");
        });

        assertThat(bookmarkNames(exported.document())).containsExactly("target");
        assertThat(exported.report().notes()).filteredOn(note -> note.subject().equals("SectionNode")).isEmpty();
    }

    @Test
    void aBlockThatDrawsNothingIsNotNamedWhereALaterSectionsBlockOfDrawingsMarksItsAnchor() throws Exception {
        // Named once every section's blocks of drawings alone are marked, not at its own section's end.
        List<DocxExportReport.Note> notes = new ArrayList<>();
        byte[] docx;
        DocumentSession first = GraphCompose.document().pageSize(400, 500).margin(DocumentInsets.of(30)).create();
        first.pageFlow(page -> page.add(new SectionBuilder().anchor("target").build()).addParagraph("Cover"));
        DocumentSession second = GraphCompose.document().pageSize(400, 500).margin(DocumentInsets.of(30)).create();
        second.pageFlow(page -> {
            lines(page, "Top", 1);
            page.add(drawings("target"));
            page.addParagraph("After the drawings");
        });
        try (com.demcha.compose.document.api.MultiSectionDocument sections =
                     GraphCompose.documents().section(first).section(second).create()) {
            docx = sections.export(DocxSemanticBackend.builder().reportSink(report -> notes.addAll(report.notes()))
                    .build());
        }

        assertThat(bookmarkNames(new XWPFDocument(new ByteArrayInputStream(docx)))).containsExactly("target");
        assertThat(notes).filteredOn(note -> note.subject().equals("SectionNode")).isEmpty();
    }

    private static DocumentNode drawings(String anchor) {
        SectionBuilder section = new SectionBuilder()
                .addShape(120, 30, DocumentColor.rgb(26, 86, 148))
                .addCircle(24, DocumentColor.rgb(200, 60, 60));
        if (anchor != null) {
            section.anchor(anchor);
        }
        return section.build();
    }

    private static Consumer<PageFlowBuilder> referTo(String anchor) {
        return page -> page
                .addParagraph(p -> p.text("See page"))
                .addPageReference(anchor)
                .addParagraph(p -> p.text("Go to the anchor").linkTo(anchor));
    }

    private static void lines(PageFlowBuilder page, String prefix, int count) {
        for (int line = 1; line <= count; line++) {
            page.addParagraph(prefix + " " + line);
        }
    }

    private record Exported(XWPFDocument document, DocxExportReport report) {
    }

    private static Exported export(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 500).margin(DocumentInsets.of(30)).create()) {
            session.pageFlow(content::accept);
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        return new Exported(new XWPFDocument(new ByteArrayInputStream(docx)), report.get());
    }

    /** The paragraph whose place marks hold the bookmark of a name, which must be there and only there. */
    private static XWPFParagraph placeMarked(XWPFDocument document, String name) {
        List<XWPFParagraph> holders = paragraphs(document).stream()
                .filter(paragraph -> placeMarksIn(paragraph).contains(name)).toList();
        assertThat(holders).as("one paragraph marked " + name).hasSize(1);
        assertThat(bookmarkNames(document)).as("no other bookmark of the name").containsOnlyOnce(name);
        return holders.get(0);
    }

    /**
     * The names of the bookmarks a paragraph opens with that mark a place: each start followed by its
     * own end, before any of the paragraph's runs.
     */
    private static List<String> placeMarksIn(XWPFParagraph paragraph) {
        List<String> names = new ArrayList<>();
        try (XmlCursor cursor = paragraph.getCTP().newCursor()) {
            if (!cursor.toFirstChild()) {
                return names;
            }
            do {
                String element = cursor.getName().getLocalPart();
                if (element.equals("pPr")) {
                    continue;
                }
                if (!element.equals("bookmarkStart")) {
                    break;
                }
                CTBookmark start = (CTBookmark) cursor.getObject();
                String name = start.getName();
                java.math.BigInteger id = start.getId();
                if (!cursor.toNextSibling() || !cursor.getName().getLocalPart().equals("bookmarkEnd")
                    || !id.equals(((CTMarkupRange) cursor.getObject()).getId())) {
                    break;
                }
                names.add(name);
            } while (cursor.toNextSibling());
        }
        return names;
    }

    private static int drawingsIn(XWPFParagraph paragraph) {
        return paragraph.getCTP().xmlText().split("<wp:anchor ", -1).length - 1;
    }

    private static XWPFParagraph paragraphWith(XWPFDocument document, String text) {
        return paragraphs(document).stream().filter(paragraph -> paragraph.getText().equals(text)).findFirst()
                .orElseThrow(() -> new AssertionError("no paragraph " + text));
    }

    private static List<String> bookmarkNames(XWPFDocument document) {
        List<String> names = new ArrayList<>();
        for (XWPFParagraph paragraph : paragraphs(document)) {
            for (CTBookmark bookmark : paragraph.getCTP().getBookmarkStartList()) {
                names.add(bookmark.getName());
            }
        }
        return names;
    }

    private static List<String> internalLinks(XWPFDocument document) {
        List<String> anchors = new ArrayList<>();
        for (XWPFParagraph paragraph : paragraphs(document)) {
            for (CTHyperlink link : paragraph.getCTP().getHyperlinkList()) {
                if (link.getAnchor() != null) {
                    anchors.add(link.getAnchor());
                }
            }
        }
        return anchors;
    }

    /** Every PAGEREF field, its instruction to the text it reads. */
    private static Map<String, String> pageReferences(XWPFDocument document) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (XWPFParagraph paragraph : paragraphs(document)) {
            String instruction = null;
            boolean reading = false;
            StringBuilder text = new StringBuilder();
            List<CTR> runs = new ArrayList<>(paragraph.getCTP().getRList());
            for (CTHyperlink link : paragraph.getCTP().getHyperlinkList()) {
                runs.addAll(link.getRList());
            }
            for (CTR run : runs) {
                for (CTText code : run.getInstrTextList()) {
                    instruction = code.getStringValue().trim();
                }
                for (CTFldChar mark : run.getFldCharList()) {
                    if (mark.getFldCharType() == STFldCharType.SEPARATE) {
                        reading = true;
                    } else if (mark.getFldCharType() == STFldCharType.END) {
                        if (instruction != null && instruction.startsWith("PAGEREF")) {
                            fields.put(instruction, text.toString());
                        }
                        instruction = null;
                        reading = false;
                        text.setLength(0);
                    }
                }
                if (reading) {
                    run.getTList().forEach(t -> text.append(t.getStringValue()));
                }
            }
        }
        return fields;
    }

    /** Every paragraph of the body, in the file's order, those of tables nested in tables included. */
    private static List<XWPFParagraph> paragraphs(XWPFDocument document) {
        List<XWPFParagraph> paragraphs = new ArrayList<>();
        collect(document.getBodyElements(), paragraphs);
        return paragraphs;
    }

    private static void collect(List<IBodyElement> elements, List<XWPFParagraph> paragraphs) {
        for (IBodyElement element : elements) {
            if (element instanceof XWPFParagraph paragraph) {
                paragraphs.add(paragraph);
            } else if (element instanceof XWPFTable table) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        collect(cell.getBodyElements(), paragraphs);
                    }
                }
            }
        }
    }
}
