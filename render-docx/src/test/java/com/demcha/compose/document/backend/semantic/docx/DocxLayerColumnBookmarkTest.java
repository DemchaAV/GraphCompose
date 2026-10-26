package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFldChar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHyperlink;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTMarkupRange;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A layer stack's column is bookmarked round what it writes in its cell.
 *
 * <p>A stack whose layers stand side by side is written as a one-row table, each layer's content in
 * its column's cell; the layer itself is never written as a block. Its anchor had no bookmark: a link
 * to it named a bookmark the file did not hold, and a page reference to it was written as the number
 * it read. Measured in Word 16 (fields updated) and LibreOffice 24.2, on a column on page 2 and the
 * same file with a page put in before it: before, both showed 2 in each and carried no link; with the
 * bookmark, both show 2 and then 3, and the link goes to the column's page — a left or right column,
 * a container, a later layer of a column, a column opening with a table, a stack in a panel, and a
 * table of contents' entry alike.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxLayerColumnBookmarkTest {

    private static final DocumentInsets LEFT_BAND = new DocumentInsets(0, 190, 0, 0);
    private static final DocumentInsets RIGHT_BAND = new DocumentInsets(0, 0, 0, 170);
    private static final DocumentTextStyle TEXT = DocumentTextStyle.DEFAULT.withSize(12);
    private static final String NAME = "Jordan Alexander Rivera";

    @Test
    void eachAnchoredColumnIsBookmarkedRoundWhatItWroteInItsCell() throws Exception {
        DocumentNode right = new ContainerNode("Right", List.of(paragraph("Right one"), paragraph("Right two")),
                0, DocumentInsets.zero(), RIGHT_BAND, null, null, null, null, "right");
        Exported exported = export(page -> page.add(columns(column("left", LEFT_BAND, "Left one", "Left two"), right)));

        XWPFTable table = exported.document().getTables().get(0);
        Marked left = marked(exported.document(), "left");
        assertThat(left.start().getText()).isEqualTo("Left one");
        assertThat(left.end().getText()).isEqualTo("Left two");
        assertThat(left.start().getBody()).isSameAs(left.end().getBody()).isSameAs(table.getRow(0).getCell(0));
        Marked marked = marked(exported.document(), "right");
        assertThat(marked.start().getText()).isEqualTo("Right one");
        assertThat(marked.end().getText()).isEqualTo("Right two");
        assertThat(marked.start().getBody()).isSameAs(marked.end().getBody()).isSameAs(table.getRow(0).getCell(1));
        assertThat(exported.report().notes()).noneMatch(note -> note.detail().contains("bookmark"));
    }

    @Test
    void aLinkAndAPageReferenceToAColumnNameItsBookmark() throws Exception {
        Exported exported = export(page -> page
                .addParagraph(p -> p.text("Go to the column").linkTo("left"))
                .addPageReference("left")
                .addPageBreak(b -> { })
                .add(columns(column("left", LEFT_BAND, "Left one"), column(null, RIGHT_BAND, "Right one"))));

        assertThat(bookmarkNames(exported.document())).containsExactly("left");
        assertThat(internalLinks(exported.document())).containsExactly("left");
        assertThat(pageReferences(exported.document())).containsExactly(Map.entry("PAGEREF left \\h", "2"));
        assertThat(exported.report().notes()).noneMatch(note -> note.detail().contains("bookmark"));
    }

    @Test
    void aTableOfContentsEntryToAColumnIsAField() throws Exception {
        Exported exported = export(page -> page
                .addTableOfContents(toc -> toc.title("Contents").entry("The left column", "left"))
                .addPageBreak(b -> { })
                .add(columns(column("left", LEFT_BAND, "Left one"), column(null, RIGHT_BAND, "Right one"))));

        assertThat(pageReferences(exported.document())).containsExactly(Map.entry("PAGEREF left \\h", "2"));
    }

    @Test
    void aLaterLayerOfAColumnIsBookmarkedFromItsOwnFirstParagraph() throws Exception {
        DocumentNode lower = new SectionBuilder().anchor("lower").margin(new DocumentInsets(120, 190, 0, 0))
                .addParagraph("Lower in the band").build();
        Exported exported = export(page -> page.add(new LayerStackBuilder()
                .layer(column(null, LEFT_BAND, "Top of the band"), LayerAlign.TOP_LEFT)
                .layer(lower, LayerAlign.TOP_LEFT)
                .layer(column(null, RIGHT_BAND, "Right one"), LayerAlign.TOP_LEFT).build()));

        Marked marked = marked(exported.document(), "lower");
        assertThat(marked.start().getText()).isEqualTo("Lower in the band");
        assertThat(marked.end()).isSameAs(marked.start());
    }

    @Test
    void aLayerAfterATableInItsColumnIsBookmarkedFromItsOwnFirstParagraph() throws Exception {
        // The paragraph closing the table above is the one the next layer writes its first line in.
        DocumentNode top = new SectionBuilder().margin(LEFT_BAND)
                .addTable(t -> t.columns(DocumentTableColumn.fixed(70), DocumentTableColumn.fixed(70))
                        .rowCells(DocumentTableCell.text("A1"), DocumentTableCell.text("B1")))
                .build();
        DocumentNode lower = new SectionBuilder().anchor("lower").margin(new DocumentInsets(60, 190, 0, 0))
                .addParagraph("Under the table").build();
        Exported exported = export(page -> page.add(new LayerStackBuilder()
                .layer(top, LayerAlign.TOP_LEFT)
                .layer(lower, LayerAlign.TOP_LEFT)
                .layer(column(null, RIGHT_BAND, "Right one"), LayerAlign.TOP_LEFT).build()));

        Marked marked = marked(exported.document(), "lower");
        assertThat(marked.start().getText()).isEqualTo("Under the table");
        assertThat(marked.end()).isSameAs(marked.start());
    }

    @Test
    void aColumnOpeningWithATableIsBookmarkedFromItsFirstCell() throws Exception {
        DocumentNode left = new SectionBuilder().anchor("table").margin(LEFT_BAND)
                .addTable(t -> t.columns(DocumentTableColumn.fixed(70), DocumentTableColumn.fixed(70))
                        .rowCells(DocumentTableCell.text("A1"), DocumentTableCell.text("B1"))
                        .rowCells(DocumentTableCell.text("A2"), DocumentTableCell.text("B2")))
                .addParagraph("After the table").build();
        Exported exported = export(page -> page.add(columns(left, column(null, RIGHT_BAND, "Right one"))));

        Marked marked = marked(exported.document(), "table");
        XWPFTableCell column = exported.document().getTables().get(0).getRow(0).getCell(0);
        assertThat(marked.start().getText()).isEqualTo("A1");
        assertThat(tableOf(marked.start())).as("the column's own table, inside its cell")
                .isSameAs(column.getTables().get(0));
        assertThat(marked.end().getText()).isEqualTo("After the table");
        assertThat(marked.end().getBody()).isSameAs(column);
    }

    @Test
    void aColumnEndingWithATableIsBookmarkedPastItsTable() throws Exception {
        // A cell ends with a paragraph after its last table, which the bookmark closes in.
        DocumentNode left = new SectionBuilder().anchor("table").margin(LEFT_BAND)
                .addParagraph("Before the table")
                .addTable(t -> t.columns(DocumentTableColumn.fixed(70), DocumentTableColumn.fixed(70))
                        .rowCells(DocumentTableCell.text("A1"), DocumentTableCell.text("B1")))
                .build();
        Exported exported = export(page -> page.add(columns(left, column(null, RIGHT_BAND, "Right one"))));

        Marked marked = marked(exported.document(), "table");
        XWPFTableCell column = exported.document().getTables().get(0).getRow(0).getCell(0);
        assertThat(marked.start().getText()).isEqualTo("Before the table");
        assertThat(marked.end().getBody()).isSameAs(column);
        assertThat(column.getBodyElements().get(column.getBodyElements().size() - 1)).isSameAs(marked.end());
    }

    @Test
    void aColumnOfAStackInAPanelIsBookmarkedInsideThePanel() throws Exception {
        Exported exported = export(page -> page
                .addPageReference("inside")
                .addSection(panel -> panel.fillColor(DocumentColor.rgb(238, 242, 247)).padding(DocumentInsets.of(10))
                        .add(columns(column("inside", LEFT_BAND, "Panel left"), column(null, RIGHT_BAND, "Panel right")))));

        Marked marked = marked(exported.document(), "inside");
        XWPFTableCell panel = exported.document().getTables().get(0).getRow(0).getCell(0);
        assertThat(marked.start().getText()).isEqualTo("Panel left");
        assertThat(tableOf(marked.start())).as("the columns' table, inside the panel's cell")
                .isSameAs(panel.getTables().get(0));
        assertThat(pageReferences(exported.document())).containsExactly(Map.entry("PAGEREF inside \\h", "1"));
    }

    @Test
    void aReferenceToAColumnReadsItsOwnBookmarkWhereAnotherCleansToItsName() throws Exception {
        // The column's first paragraph is anchored "a" and written first, so it is named "a"; the
        // column's anchor "a." cleans to that name and is pushed onto a suffix.
        DocumentNode left = new SectionBuilder().anchor("a.").margin(LEFT_BAND)
                .addParagraph(p -> p.text("Left one").anchor("a")).build();
        Exported exported = export(page -> page
                .addPageReference("a.")
                .add(columns(left, column(null, RIGHT_BAND, "Right one"))));

        assertThat(bookmarkNames(exported.document())).containsExactlyInAnyOrder("a", "a_2");
        assertThat(pageReferences(exported.document())).containsExactly(Map.entry("PAGEREF a_2 \\h", "1"));
    }

    @Test
    void anUnanchoredColumnWritesNoBookmark() throws Exception {
        Exported exported = export(page -> page
                .add(columns(column(null, LEFT_BAND, "Left one"), column(null, RIGHT_BAND, "Right one"))));

        assertThat(bookmarkNames(exported.document())).isEmpty();
    }

    @Test
    void aColumnOfDrawingsAloneIsNamedAndAReferenceToItStaysItsNumber() throws Exception {
        // Its drawing stands in the paragraph beside it: the column writes none to put a bookmark round.
        DocumentNode drawn = new SectionBuilder().anchor("drawn").margin(LEFT_BAND)
                .addCircle(40, DocumentColor.rgb(26, 86, 148)).build();
        Exported exported = export(page -> page
                .addPageReference("drawn")
                .add(columns(drawn, column(null, RIGHT_BAND, "Right one"))));

        assertThat(bookmarkNames(exported.document())).isEmpty();
        assertThat(pageReferences(exported.document())).isEmpty();
        assertThat(exported.document().getParagraphs().get(0).getText()).as("the number it reads, as text")
                .isEqualTo("1");
        assertThat(notesOn(exported.report(), "PageReferenceNode")).singleElement().asString()
                .contains("its page number is written as text");
        assertThat(notesOn(exported.report(), "SectionNode")).containsExactly("writes no paragraph, so its anchor "
                + "\"drawn\" has no bookmark in the Word file: a link to it points at none, and a page reference to "
                + "it is a fixed number rather than a field");
    }

    @Test
    void aColumnThatWritesNothingNamesItsAnchorInItsOwnNote() throws Exception {
        // The column has a note of its own already — its outline entry — and the anchor joins it.
        DocumentNode drawn = new SectionBuilder().anchor("drawn").bookmark(new DocumentBookmarkOptions("Drawn"))
                .margin(LEFT_BAND).addCircle(40, DocumentColor.rgb(26, 86, 148)).build();
        Exported exported = export(page -> page.add(columns(drawn, column(null, RIGHT_BAND, "Right one"))));

        assertThat(notesOn(exported.report(), "SectionNode")).containsExactly("written as a column of its layer "
                + "stack; its outline entry is not written; it writes no paragraph, so its anchor \"drawn\" has no "
                + "bookmark in the Word file: a link to it points at none, and a page reference to it is a fixed "
                + "number rather than a field");
    }

    @Test
    void aLayerWhoseBlockAnEarlierLayersStandInHoldsIsBookmarkedFromThere() throws Exception {
        // The subtitle is the main layer's, drawn over the strip the layer above paints: it is
        // written in the strip, before the rest of its layer.
        DocumentNode stack = strip(340, "main", true);
        Exported exported = export(page -> page.add(stack));

        Marked marked = marked(exported.document(), "main");
        assertThat(marked.start().getText()).isEqualTo("Subtitle");
        assertThat(cellOf(marked.start()).getText()).as("the premise: written in the strip").contains(NAME);
        assertThat(marked.end().getText()).isEqualTo("Body");
        assertThat(exported.report().notes()).noneMatch(note -> note.detail().contains("bookmark"));
    }

    @Test
    void aLayerAllOfWhichAnEarlierLayersStandInHoldsIsBookmarkedThere() throws Exception {
        DocumentNode stack = strip(340, "main", false);
        Exported exported = export(page -> page.addPageReference("main").add(stack));

        Marked marked = marked(exported.document(), "main");
        assertThat(marked.start().getText()).isEqualTo("Subtitle");
        assertThat(marked.end()).isSameAs(marked.start());
        assertThat(pageReferences(exported.document())).containsExactly(Map.entry("PAGEREF main \\h", "1"));
        assertThat(exported.report().notes()).noneMatch(note -> note.detail().contains("bookmark"));
    }

    @Test
    void aColumnHoldingAStackOfItsOwnIsBookmarkedFromItsOwnFirstLine() throws Exception {
        // The stack inside writes its subtitle in its strip, after the column's heading: that is
        // the inner stack's, inside what the column writes, not something written before it.
        DocumentNode outer = new SectionBuilder().anchor("outer").margin(new DocumentInsets(0, 20, 0, 0))
                .addParagraph("Heading").add(strip(320, null, true)).build();
        DocumentNode right = column(null, new DocumentInsets(0, 0, 0, 325), "R");
        Exported exported = export(page -> page.add(columns(outer, right)));

        Marked marked = marked(exported.document(), "outer");
        assertThat(cellOf(paragraphHolding(exported.document(), "Subtitle")).getText())
                .as("the premise: the inner stack wrote its subtitle in its strip").contains(NAME);
        assertThat(marked.start().getText()).isEqualTo("Heading");
    }

    private static DocumentNode column(String anchor, DocumentInsets band, String... lines) {
        SectionBuilder section = new SectionBuilder().margin(band);
        if (anchor != null) {
            section.anchor(anchor);
        }
        for (String line : lines) {
            section.addParagraph(line);
        }
        return section.build();
    }

    /**
     * A sidebar beside a main column whose strip is drawn in two layers, as {@code ReadingOrderColumns}
     * draws one (see {@code DocxStandInMoveTest}): the first paints the strip with the name and a
     * stand-in under it, the second holds the name's place with a spacer and writes the subtitle,
     * which lies inside the stand-in, then, with {@code body}, a body line under the strip.
     *
     * @param width  the width the stack is offered, in points
     * @param anchor the main layer's anchor, or {@code null}
     */
    private static DocumentNode strip(double width, String anchor, boolean body) throws Exception {
        double[] name = new double[2];
        double[] subtitle = new double[2];
        try (DocumentSession probe = GraphCompose.document().pageSize(400, 500).margin(DocumentInsets.of(20))
                .create()) {
            probe.pageFlow(page -> page.addSection("Probe", strip -> strip.padding(DocumentInsets.of(10))
                    .addParagraph(p -> p.name("Name").text(NAME).textStyle(TEXT))
                    .addParagraph(p -> p.name("Subtitle").text("Subtitle").textStyle(TEXT))));
            for (com.demcha.compose.document.layout.PlacedNode node : probe.layoutGraph().nodes()) {
                double[] box = "Name".equals(node.semanticName()) ? name
                        : "Subtitle".equals(node.semanticName()) ? subtitle : null;
                if (box != null) {
                    box[0] = node.placementWidth();
                    box[1] = node.placementHeight();
                }
            }
        }
        DocumentNode main = layer("MainLayer", 100, 0, anchor, content -> {
            content.addSection("Hero", hero -> hero.padding(new DocumentInsets(10, 10, 10, 10))
                    .addSpacer(spacer -> spacer.name("NamePlace").width(name[0]).height(name[1]))
                    .addParagraph(p -> p.name("Subtitle").text("Subtitle").textStyle(TEXT)));
            if (body) {
                content.addParagraph(p -> p.name("Body").text("Body").textStyle(TEXT));
            }
        });
        return new LayerStackBuilder()
                .layer(layer("Sidebar", 0, width - 100, null, sidebar -> sidebar
                        .addParagraph(p -> p.text("Contact").textStyle(TEXT))), LayerAlign.TOP_LEFT)
                .layer(layer("StripLayer", 100, 0, null, content -> content.addSection("Strip", strip -> strip
                        .fillColor(DocumentColor.rgb(240, 240, 236))
                        .padding(DocumentInsets.of(10))
                        .addParagraph(p -> p.name("Name").text(NAME).textStyle(TEXT))
                        .addSpacer(spacer -> spacer.name("SubtitlePlace").width(subtitle[0]).height(subtitle[1])))),
                        LayerAlign.TOP_LEFT)
                .layer(main, LayerAlign.TOP_LEFT)
                .build();
    }

    private static DocumentNode layer(String name, double left, double right, String anchor,
                                      Consumer<SectionBuilder> content) {
        SectionBuilder layer = new SectionBuilder();
        layer.name(name).spacing(0).padding(new DocumentInsets(0, right, 0, left));
        if (anchor != null) {
            layer.anchor(anchor);
        }
        layer.addSection(name + "Content", content);
        return layer.build();
    }

    private static XWPFTableCell cellOf(XWPFParagraph paragraph) {
        assertThat(paragraph.getBody()).isInstanceOf(XWPFTableCell.class);
        return (XWPFTableCell) paragraph.getBody();
    }

    private static XWPFTable tableOf(XWPFParagraph paragraph) {
        return cellOf(paragraph).getTableRow().getTable();
    }

    private static XWPFParagraph paragraphHolding(XWPFDocument document, String text) {
        return paragraphs(document).stream().filter(paragraph -> paragraph.getText().equals(text)).findFirst()
                .orElseThrow();
    }

    private static DocumentNode paragraph(String text) {
        return new ParagraphBuilder().text(text).build();
    }

    private static DocumentNode columns(DocumentNode left, DocumentNode right) {
        return new LayerStackBuilder().layer(left, LayerAlign.TOP_LEFT).layer(right, LayerAlign.TOP_LEFT).build();
    }

    private record Exported(XWPFDocument document, DocxExportReport report) {
    }

    private static Exported export(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 500).margin(DocumentInsets.of(30))
                .create()) {
            session.pageFlow(content::accept);
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        return new Exported(new XWPFDocument(new ByteArrayInputStream(docx)), report.get());
    }

    private static List<String> notesOn(DocxExportReport report, String subject) {
        return report.notes().stream().filter(note -> note.subject().equals(subject))
                .map(DocxExportReport.Note::detail).toList();
    }

    /** Where a bookmark opens and where it closes. */
    private record Marked(XWPFParagraph start, XWPFParagraph end) {
    }

    private static Marked marked(XWPFDocument document, String name) {
        XWPFParagraph start = null;
        BigInteger id = null;
        for (XWPFParagraph paragraph : paragraphs(document)) {
            for (CTBookmark bookmark : paragraph.getCTP().getBookmarkStartList()) {
                if (name.equals(bookmark.getName())) {
                    start = paragraph;
                    id = bookmark.getId();
                }
            }
            for (CTMarkupRange end : paragraph.getCTP().getBookmarkEndList()) {
                if (id != null && id.equals(end.getId())) {
                    return new Marked(start, paragraph);
                }
            }
        }
        throw new AssertionError("no bookmark " + name + " opened and closed in the file");
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
