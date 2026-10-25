package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.MultiSectionDocument;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.PageReferenceNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
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
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A panel the layout breaks over pages inside the table it holds is a table a page.
 *
 * <p>The layout breaks such a table between rows, repeats its header rows at the top of the next
 * page and goes on with the panel there, no padding above its text. Written as one table, Word
 * broke the panel's row where its lines ran out and repeated no header row of the table nested in
 * its cell: measured in Word 16, {@code ObsidianInvoice}'s and {@code SubscriptionInvoice}'s line
 * items set over two pages stood 24 to 30pt high on the second, and over three one row stood a
 * page early, in LibreOffice 24.2 too. Cut at the row each page starts with, every line stands on
 * the page the layout sets it on in both.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxPanelOverPagesTest {

    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final DocumentColor INK = DocumentColor.rgb(26, 86, 148);
    private static final String CUT = "written as a table a page";

    @Test
    void aPanelWhoseTableRunsOverAPageIsATableAPageCutAtTheRowsThePagesStartWith() throws Exception {
        Exported exported = export(card(20, true, null, null));

        List<XWPFTable> parts = parts(exported.document());
        assertThat(exported.pages()).as("the premise: the card runs onto a second page").hasSize(2);
        assertThat(parts).hasSize(2);
        assertThat(rowsOf(parts)).as("each part the rows the page sets on its page, under the header")
                .isEqualTo(exported.pages());
    }

    @Test
    void aTableOverThreePagesIsCutTwiceEachPartUnderItsHeader() throws Exception {
        Exported exported = export(card(30, true, null, null));

        assertThat(exported.pages()).as("the premise: three pages").hasSize(3);
        assertThat(rowsOf(parts(exported.document()))).isEqualTo(exported.pages());
    }

    @Test
    void aTableWithNoHeaderRowsIsCutWithNoneAndItsNoteSaysNothingOfOne() throws Exception {
        Exported exported = export(card(20, false, null, null));

        List<List<String>> rows = rowsOf(parts(exported.document()));
        assertThat(rows).hasSize(2);
        assertThat(rows.get(1).get(0)).as("the second part opens with its first row").startsWith("Row ");
        assertThat(cutNote(exported.report())).doesNotContain("header");
    }

    @Test
    void thePartsStandApartByAShortParagraphThatBreaksThePageBeforeItself() throws Exception {
        XWPFDocument document = export(card(20, true, null, null)).document();

        List<IBodyElement> body = document.getBodyElements();
        int first = body.indexOf(parts(document).get(0));
        XWPFParagraph between = (XWPFParagraph) body.get(first + 1);
        assertThat(between.isPageBreak()).as("a break before it, which adds no page where it opens one").isTrue();
        assertThat(between.getRuns()).as("no break in it, which would").isEmpty();
        assertThat(between.getCTP().getPPr().getSpacing().getLine().toString()).as("a tenth of a point").isEqualTo("2");
        assertThat(body.get(first + 2)).isSameAs(parts(document).get(1));
    }

    @Test
    void aPartAfterACutHasNoTopPaddingAndOneBeforeACutNoBottomPadding() throws Exception {
        // The page sets the card's rows down to the page's foot and closes it on its last page only.
        List<XWPFTable> parts = parts(export(card(30, true, null, null)).document());

        assertThat(margin(parts.get(0), true)).as("the card's padding, its border's room taken").isPositive();
        assertThat(margin(parts.get(0), false)).isZero();
        assertThat(margin(parts.get(1), true)).isZero();
        assertThat(margin(parts.get(1), false)).isZero();
        assertThat(margin(parts.get(2), true)).isZero();
        assertThat(margin(parts.get(2), false)).as("the last closes the card").isPositive();
    }

    @Test
    void whatTheCardHoldsBeforeAndAfterItsTableStaysWithTheFirstAndTheLastPart() throws Exception {
        List<XWPFTable> parts = parts(export(card(20, true,
                above -> above.addParagraph("Heading"), below -> below.addParagraph("Closing"))).document());

        XWPFTableCell first = parts.get(0).getRow(0).getCell(0);
        XWPFTableCell last = parts.get(parts.size() - 1).getRow(0).getCell(0);
        assertThat(texts(first)).contains("Heading").doesNotContain("Closing");
        assertThat(texts(last)).contains("Closing").doesNotContain("Heading");
        IBodyElement closer = first.getBodyElements().get(first.getBodyElements().size() - 1);
        assertThat(closer).as("the first part's cell, ending with its table, ends with a paragraph")
                .isInstanceOf(XWPFParagraph.class);
        assertThat(((XWPFParagraph) closer).getCTP().getPPr().getSpacing().getLine().toString())
                .as("a tenth of a point, as one closing a table in a cell").isEqualTo("2");
    }

    @Test
    void theCutIsNamedWithItsPagesItsFixedBreaksAndTheTopBorderItsPartsStandLowerBy() throws Exception {
        String note = cutNote(export(card(30, true, null, null)).report());

        assertThat(note).contains("cut at the rows pages 2 and 3 start with");
        assertThat(note).contains("opening with the table's header rows");
        assertThat(note).contains("page breaks fixed there");
        assertThat(note).contains("in Word a part after a cut stands about its top border's width, 1pt, lower");
    }

    @Test
    void aPageWhoseRowsHoldAFieldIsLeftWholeAndTheNextOneIsStillCut() throws Exception {
        // A page reference in a row on page 2: the table is not split where page 2 starts, a
        // field's copy being one the export does not settle, and is where page 3 does.
        Exported exported = export(page -> page.addSection(card -> card.fillColor(SURFACE)
                .stroke(DocumentStroke.of(INK, 1)).padding(DocumentInsets.of(6))
                .addParagraph(p -> p.text("Heading").anchor("top"))
                .addTable(table -> {
                    table.columns(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(100))
                            .header("Item", "Amount").repeatHeader();
                    for (int row = 1; row <= 30; row++) {
                        table.rowCells(DocumentTableCell.text("Row " + row), row == 15
                                ? DocumentTableCell.node(new PageReferenceNode("top", DocumentTextStyle.DEFAULT,
                                        TextAlign.LEFT))
                                : DocumentTableCell.text(String.valueOf(row)));
                    }
                })));

        assertThat(exported.pages()).as("the premise: three pages").hasSize(3);
        assertThat(exported.pages().get(1)).as("the premise: row 15 on page 2").contains("Row 15");
        assertThat(parts(exported.document())).hasSize(2);
        assertThat(cutNote(exported.report())).contains("cut at the row page 3 starts with")
                .contains("page 2, whose first row its table cannot be split at, is left to Word");
    }

    @Test
    void aPanelInAnEarlierSectionIsCutByItsOwnSectionsLayout() throws Exception {
        // Each section is laid out on its own: the cut reads the card's pages off its section's.
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        try (MultiSectionDocument sections = GraphCompose.documents()
                .section(session(card(20, true, null, null)))
                .section(session(page -> page.addParagraph("Second section")))
                .create()) {
            docx = sections.export(DocxSemanticBackend.builder().reportSink(report::set).build());
        }

        assertThat(parts(new XWPFDocument(new ByteArrayInputStream(docx)))).hasSize(2);
        assertThat(cutNote(report.get())).contains("cut at the row page 2 starts with");
    }

    @Test
    void aPanelWhoseTableFitsItsPageIsOneTable() throws Exception {
        Exported exported = export(card(8, true, null, null));

        assertThat(exported.pages()).hasSize(1);
        assertThat(parts(exported.document())).hasSize(1);
        assertThat(exported.report().notes()).noneMatch(note -> note.detail().contains(CUT));
    }

    @Test
    void aPanelTheLayoutBreaksBetweenParagraphsIsNotCut() throws Exception {
        // Only a table the layout breaks over pages is cut: blocks it moves or breaks otherwise
        // inside a panel are left to Word.
        Exported exported = export(page -> page.addSection(card -> {
            card.fillColor(SURFACE).padding(DocumentInsets.of(6));
            for (int line = 1; line <= 30; line++) {
                card.addParagraph("Line " + line);
            }
        }));

        assertThat(exported.cardPages()).as("the premise: the card runs onto a second page").isGreaterThan(1);
        assertThat(exported.document().getTables()).hasSize(1);
        assertThat(exported.document().getParagraphs()).noneMatch(XWPFParagraph::isPageBreak);
    }

    @Test
    void aPanelWithAnotherBlockOverPagesBesideItsTableIsNotCut() throws Exception {
        // A paragraph the layout breaks over pages above the table: the table is not the only one.
        Exported exported = export(card(20, true, above -> above.addParagraph("Long " + "word ".repeat(400)), null));

        assertThat(exported.cardPages()).as("the premise: the card runs over pages").isGreaterThan(2);
        assertThat(parts(exported.document())).hasSize(1);
        assertThat(exported.report().notes()).noneMatch(note -> note.detail().contains(CUT));
    }

    @Test
    void aPanelWithTwoTablesOverPagesIsNotCut() throws Exception {
        Exported exported = export(page -> page.addSection(card -> {
            card.fillColor(SURFACE).stroke(DocumentStroke.of(INK, 1)).padding(DocumentInsets.of(6));
            card.addTable(table -> rows(table, 20, true));
            card.addTable(table -> rows(table, 20, true));
        }));

        assertThat(exported.cardPages()).as("the premise: the card runs over pages").isGreaterThan(2);
        assertThat(exported.document().getParagraphs()).noneMatch(XWPFParagraph::isPageBreak);
    }

    @Test
    void aTableOverPagesOutsideAPanelIsNotCut() throws Exception {
        // In the body Word repeats a table's header rows itself.
        Exported exported = export(page -> page.addTable(table -> rows(table, 20, true)));

        assertThat(exported.pages()).as("the premise: the table runs onto a second page").hasSize(2);
        assertThat(exported.document().getTables()).hasSize(1);
        assertThat(exported.document().getParagraphs()).noneMatch(XWPFParagraph::isPageBreak);
    }

    /** A card with a 1pt border and 6pt of padding holding a table of {@code count} rows. */
    private static Consumer<PageFlowBuilder> card(int count, boolean header, Consumer<SectionBuilder> above,
                                                  Consumer<SectionBuilder> below) {
        return page -> page.addSection(card -> {
            card.fillColor(SURFACE).stroke(DocumentStroke.of(INK, 1)).padding(DocumentInsets.of(6));
            if (above != null) {
                above.accept(card);
            }
            card.addTable(table -> rows(table, count, header));
            if (below != null) {
                below.accept(card);
            }
        });
    }

    private static void rows(TableBuilder table, int count, boolean header) {
        table.columns(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(100));
        if (header) {
            table.header("Item", "Amount").repeatHeader();
        }
        for (int row = 1; row <= count; row++) {
            table.row("Row " + row, String.valueOf(row));
        }
    }

    /**
     * An export, its report, the rows of its first table the layout sets on each page — header and
     * data rows by their first cell's text — and the pages its first section runs over.
     */
    private record Exported(XWPFDocument document, DocxExportReport report, List<List<String>> pages, int cardPages) {
    }

    private static DocumentSession session(Consumer<PageFlowBuilder> content) {
        DocumentSession session = GraphCompose.document().pageSize(400, 300).margin(DocumentInsets.of(20)).create();
        session.pageFlow(content::accept);
        return session;
    }

    private static Exported export(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = session(content)) {
            byte[] docx = session.export(new DocxSemanticBackend(report::set));
            DocxLayoutMetrics layout = DocxLayoutMetrics.of(session.documentGraph(), session.layoutGraph());
            List<List<String>> pages = new ArrayList<>();
            TableNode table = first(session.documentGraph().roots(), TableNode.class);
            if (table != null) {
                boolean header = table.repeatedHeaderRowCount() > 0;
                for (int row = header ? 1 : 0; row < table.rows().size(); row++) {
                    int page = layout.rowPage(table, row);
                    while (pages.size() <= page) {
                        pages.add(new ArrayList<>(header ? List.of("Item") : List.of()));
                    }
                    pages.get(page).add("Row " + (header ? row : row + 1));
                }
            }
            SectionNode card = first(session.documentGraph().roots(), SectionNode.class);
            int cardPages = card == null || layout.placement(card) == null ? 0
                    : layout.placement(card).endPage() - layout.placement(card).startPage() + 1;
            return new Exported(new XWPFDocument(new ByteArrayInputStream(docx)), report.get(), pages, cardPages);
        }
    }

    private static <T extends DocumentNode> T first(List<DocumentNode> nodes, Class<T> kind) {
        for (DocumentNode node : nodes) {
            if (kind.isInstance(node)) {
                return kind.cast(node);
            }
            T inside = first(node.children(), kind);
            if (inside != null) {
                return inside;
            }
        }
        return null;
    }

    private static String cutNote(DocxExportReport report) {
        return report.notes().stream().filter(note -> note.detail().contains(CUT)).findFirst().orElseThrow().detail();
    }

    /** The panel's tables in the body, those that hold a table in their one cell. */
    private static List<XWPFTable> parts(XWPFDocument document) {
        return document.getTables().stream()
                .filter(table -> table.getNumberOfRows() == 1 && !table.getRow(0).getCell(0).getTables().isEmpty())
                .toList();
    }

    /** Each part's table's rows, by their first cell's text. */
    private static List<List<String>> rowsOf(List<XWPFTable> parts) {
        List<List<String>> rows = new ArrayList<>();
        for (XWPFTable part : parts) {
            List<String> texts = new ArrayList<>();
            for (XWPFTableRow row : part.getRow(0).getCell(0).getTables().get(0).getRows()) {
                texts.add(row.getCell(0).getText());
            }
            rows.add(texts);
        }
        return rows;
    }

    private static List<String> texts(XWPFTableCell cell) {
        return cell.getParagraphs().stream().map(XWPFParagraph::getText).toList();
    }

    private static long margin(XWPFTable part, boolean top) {
        CTTcMar margins = part.getRow(0).getCell(0).getCTTc().getTcPr().getTcMar();
        if (margins == null || !(top ? margins.isSetTop() : margins.isSetBottom())) {
            return 0;
        }
        return DocxTwips.of((top ? margins.getTop() : margins.getBottom()).getW());
    }
}
