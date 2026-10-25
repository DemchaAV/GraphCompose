package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageBreakBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
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
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A page break the page does not break at is not written.
 *
 * <p>The page breaks a page only in the flow it lays out page by page. In a row's column, a layer
 * stack's layer and a table's composed cell it places a break as a box of no height, the spacing
 * of the container it stands in on either side of it, and goes on on the same page. Written there
 * — in the body after the row's, the columns' or the table's table, or between a stack's layers
 * — the break set everything after it on a page the page does not have: measured in Word 16 and
 * LibreOffice 24.2, a section of "Part A", a break and "Part B" in each of them came out on two
 * pages where the page has one, and in a cell "Part B" stood 6pt high, the spacing above it
 * once.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxPageBreakOutsideTheFlowTest {

    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    /** The spacing of the section the break stands in, in points. */
    private static final double SPACING = 6;
    /**
     * What a line of the default text is raised into the space above it, in points
     * ({@link DocxExports#DEFAULT_LINE_RAISE}).
     */
    private static final double RAISE = DocxExports.DEFAULT_LINE_RAISE / 20.0;
    private static final Pattern PAGE_BREAK = Pattern.compile("<w:br\\b[^>]*w:type=\"page\"|<w:pageBreakBefore\\b");

    @Test
    void aBreakInARowsColumnIsNotWritten() throws Exception {
        Exported exported = export(page -> page.addParagraph("Above")
                .addRow(row -> row.add(parts()).add(paragraph("Right")))
                .addParagraph("After the row"));

        assertThat(onThePageOfPartA(exported.layout(), "Part B")).as("the premise: the page breaks no page").isTrue();
        assertThat(pageBreaks(exported.document())).as("the page breaks no page in a row").isZero();
        assertThat(bodyText(exported.document())).as("no paragraph after the row's table but what follows it")
                .containsExactly("Above", "After the row");
        assertThat(gapInWord(exported.document(), "Part A", "Part B")).as("the section's spacing on either side")
                .isCloseTo(gapOnThePage(exported.layout(), "Part A", "Part B") - RAISE, within(0.05))
                .isCloseTo(2 * SPACING - RAISE, within(0.05));
    }

    @Test
    void aBreakInARowsColumnInACardIsNotWrittenAfterTheCard() throws Exception {
        Exported exported = export(page -> page.addParagraph("Above")
                .addSection(card -> card.fillColor(SURFACE).padding(DocumentInsets.of(8))
                        .addRow(row -> row.add(parts()).add(paragraph("Right"))))
                .addParagraph("After the card"));

        assertThat(pageBreaks(exported.document())).isZero();
        assertThat(exported.document().getTables()).as("one card").hasSize(1);
        assertThat(bodyText(exported.document())).containsExactly("Above", "After the card");
    }

    @Test
    void aBreakInAStacksColumnIsNotWritten() throws Exception {
        Exported exported = export(page -> page.addParagraph("Above")
                .addLayerStack(stack -> stack
                        .topLeft(new SectionBuilder().spacing(SPACING).padding(new DocumentInsets(0, 180, 0, 0))
                                .add(paragraph("Part A")).add(new PageBreakBuilder().build()).add(paragraph("Part B"))
                                .build())
                        .topLeft(new SectionBuilder().padding(new DocumentInsets(0, 0, 0, 200))
                                .add(paragraph("Beside")).build()))
                .addParagraph("After the stack"));

        assertThat(onThePageOfPartA(exported.layout(), "Part B")).as("the premise: the page breaks no page").isTrue();
        assertThat(exported.document().getTables()).as("the premise: written as columns").hasSize(1);
        assertThat(exported.document().getTables().get(0).getRow(0).getTableCells()).hasSize(2);
        assertThat(pageBreaks(exported.document())).isZero();
        assertThat(bodyText(exported.document())).containsExactly("Above", "After the stack");
        assertThat(gapInWord(exported.document(), "Part A", "Part B"))
                .isCloseTo(gapOnThePage(exported.layout(), "Part A", "Part B") - RAISE, within(0.05));
    }

    @Test
    void aBreakInATablesComposedCellIsNotWritten() throws Exception {
        Exported exported = export(page -> page.addParagraph("Above")
                .addTable(table -> table.columns(DocumentTableColumn.fixed(200))
                        .rowCells(DocumentTableCell.node(parts())))
                .addParagraph("After the table"));

        assertThat(pageBreaks(exported.document())).isZero();
        assertThat(bodyText(exported.document())).containsExactly("Above", "After the table");
        // The page places no node of a composed cell: its spacing is the section's, on either side.
        assertThat(gapInWord(exported.document(), "Part A", "Part B")).as("the section's spacing on either side")
                .isCloseTo(2 * SPACING, within(0.05));
    }

    @Test
    void aBreakInABandIsNotWritten() throws Exception {
        // A stack laying one layer over another is written as its layers one after the other.
        Exported exported = export(page -> page.addParagraph("Above")
                .addLayerStack(stack -> stack.topLeft(parts()).bottomRight(paragraph("Over")))
                .addParagraph("After the stack"));

        assertThat(exported.document().getTables()).as("the premise: written as a band, not as columns").isEmpty();
        assertThat(onThePageOfPartA(exported.layout(), "Over")).isTrue();
        assertThat(pageBreaks(exported.document())).isZero();
        assertThat(gapInWord(exported.document(), "Part A", "Part B"))
                .isCloseTo(gapOnThePage(exported.layout(), "Part A", "Part B") - RAISE, within(0.05));
    }

    @Test
    void aBreakInAStackOfOneLayerIsNotWritten() throws Exception {
        Exported exported = export(page -> page.addParagraph("Above")
                .addLayerStack(stack -> stack.topLeft(parts()))
                .addParagraph("After the stack"));

        assertThat(onThePageOfPartA(exported.layout(), "Part B")).as("the premise: the page breaks no page").isTrue();
        assertThat(pageBreaks(exported.document())).isZero();
        assertThat(gapInWord(exported.document(), "Part A", "Part B"))
                .isCloseTo(gapOnThePage(exported.layout(), "Part A", "Part B") - RAISE, within(0.05));
    }

    @Test
    void aCardInAStackOfOneLayerIsNotCutAtItsBreak() throws Exception {
        // The page draws the card whole on one page: one table, both parts in its cell.
        Exported exported = export(page -> page.addParagraph("Above")
                .addLayerStack(stack -> stack.topLeft(new SectionBuilder().fillColor(SURFACE)
                        .padding(DocumentInsets.of(8)).add(parts()).build()))
                .addParagraph("After the stack"));

        assertThat(pageBreaks(exported.document())).isZero();
        assertThat(exported.document().getTables()).as("one card").hasSize(1);
        assertThat(exported.document().getTables().get(0).getRow(0).getCell(0).getText())
                .contains("Part A").contains("Part B");
    }

    @Test
    void aBreakInAShapeContainersLayerIsNotWritten() throws Exception {
        Exported exported = export(page -> page.addParagraph("Above")
                .add(new ShapeContainerBuilder().rectangle(300, 80).fillColor(SURFACE)
                        .layer(parts(), LayerAlign.TOP_LEFT).build())
                .addParagraph("After the shape"));

        assertThat(onThePageOfPartA(exported.layout(), "Part B")).as("the premise: the page breaks no page").isTrue();
        assertThat(pageBreaks(exported.document())).isZero();
        assertThat(gapInWord(exported.document(), "Part A", "Part B")).as("the section's spacing on either side")
                .isCloseTo(2 * SPACING, within(RAISE + 0.05));
    }

    @Test
    void aBreakInACanvasIsNotWritten() throws Exception {
        Exported exported = export(page -> page.addParagraph("Above")
                .addCanvas(300, 80, canvas -> canvas.position(parts(), 0, 0))
                .addParagraph("After the canvas"));

        assertThat(onThePageOfPartA(exported.layout(), "Part B")).as("the premise: the page breaks no page").isTrue();
        assertThat(pageBreaks(exported.document())).isZero();
        // A line in a canvas is not raised into the space above it.
        assertThat(gapInWord(exported.document(), "Part A", "Part B")).as("the section's spacing on either side")
                .isCloseTo(gapOnThePage(exported.layout(), "Part A", "Part B"), within(0.05))
                .isCloseTo(2 * SPACING, within(0.05));
    }

    @Test
    void aBreakInTheFlowIsStillWrittenWithNoSpacingAboveIt() throws Exception {
        Exported exported = export(page -> page.addParagraph("Above")
                .add(parts())
                .addParagraph("After the section"));

        assertThat(placed(exported.layout(), "Part B").startPage()).as("the premise: the page breaks there")
                .isEqualTo(1);
        assertThat(pageBreaks(exported.document())).isEqualTo(1);
        assertThat(bodyText(exported.document())).as("the break's paragraph between the parts")
                .containsExactly("Above", "Part A", "\n", "Part B", "After the section");
        assertThat(after(paragraphOf(exported.document(), "Part A"))).as("the spacing stays on the next page")
                .isZero();
    }

    /** A section of "Part A", a page break and "Part B", its children {@link #SPACING} apart. */
    private static DocumentNode parts() {
        return new SectionBuilder().spacing(SPACING)
                .add(paragraph("Part A")).add(new PageBreakBuilder().build()).add(paragraph("Part B")).build();
    }

    private static DocumentNode paragraph(String text) {
        return new ParagraphBuilder().name(text.replace(' ', '_')).text(text).build();
    }

    private record Exported(XWPFDocument document, LayoutGraph layout) {
    }

    private static Exported export(Consumer<PageFlowBuilder> content) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(content::accept);
            LayoutGraph layout = session.layoutGraph();
            byte[] docx = session.export(new DocxSemanticBackend());
            return new Exported(new XWPFDocument(new ByteArrayInputStream(docx)), layout);
        }
    }

    /** How many page breaks the file writes, in the body and in every table, nested ones included. */
    private static int pageBreaks(XWPFDocument document) {
        Matcher matcher = PAGE_BREAK.matcher(document.getDocument().xmlText());
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /** The text of each paragraph of the body itself, not in a table. */
    private static List<String> bodyText(XWPFDocument document) {
        return document.getParagraphs().stream().map(XWPFParagraph::getText).toList();
    }

    /** The space Word writes between two paragraphs written one after the other, in points. */
    private static double gapInWord(XWPFDocument document, String above, String below) {
        XWPFParagraph first = paragraphOf(document, above);
        XWPFParagraph second = paragraphOf(document, below);
        List<IBodyElement> elements = first.getBody().getBodyElements();
        assertThat(second.getBody()).as("%s and %s in one body", above, below).isSameAs(first.getBody());
        assertThat(elements.indexOf(second)).as("%s written right after %s", below, above)
                .isEqualTo(elements.indexOf(first) + 1);
        return after(first) + before(second);
    }

    /** The space the page leaves between two placed blocks, in points. */
    private static double gapOnThePage(LayoutGraph layout, String above, String below) {
        PlacedNode first = placed(layout, above);
        PlacedNode second = placed(layout, below);
        assertThat(second.startPage()).as("%s on the page of %s", below, above).isEqualTo(first.startPage());
        return first.placementY() - (second.placementY() + second.placementHeight());
    }

    /** Whether the page sets a block on the page it sets "Part A" on. */
    private static boolean onThePageOfPartA(LayoutGraph layout, String text) {
        return placed(layout, text).startPage() == placed(layout, "Part A").startPage();
    }

    private static PlacedNode placed(LayoutGraph layout, String text) {
        String name = text.replace(' ', '_');
        return layout.nodes().stream().filter(node -> name.equals(node.semanticName())).findFirst()
                .orElseThrow(() -> new AssertionError("no placed node named " + name));
    }

    private static XWPFParagraph paragraphOf(XWPFDocument document, String text) {
        List<XWPFParagraph> all = new ArrayList<>(document.getParagraphs());
        for (XWPFTable table : document.getTables()) {
            collect(table, all);
        }
        return all.stream().filter(paragraph -> paragraph.getText().equals(text)).findFirst()
                .orElseThrow(() -> new AssertionError("no paragraph reading " + text));
    }

    private static void collect(XWPFTable table, List<XWPFParagraph> into) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                into.addAll(cell.getParagraphs());
                for (XWPFTable nested : cell.getTables()) {
                    collect(nested, into);
                }
            }
        }
    }

    private static double before(XWPFParagraph paragraph) {
        CTSpacing spacing = spacingOf(paragraph);
        return spacing == null || !spacing.isSetBefore() ? 0 : DocxTwips.of(spacing.getBefore()) / 20.0;
    }

    private static double after(XWPFParagraph paragraph) {
        CTSpacing spacing = spacingOf(paragraph);
        return spacing == null || !spacing.isSetAfter() ? 0 : DocxTwips.of(spacing.getAfter()) / 20.0;
    }

    private static CTSpacing spacingOf(XWPFParagraph paragraph) {
        return paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr().getSpacing() : null;
    }
}
