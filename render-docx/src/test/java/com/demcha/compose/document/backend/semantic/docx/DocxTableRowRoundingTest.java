package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.TableRowFragmentPayload;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A table's rows written taller than the page's by rounding stand where the page's do, row after
 * row, give or take half a twip: what each is written taller by, rounded to the twip, is carried
 * to the rows under it and settled in whole twips in a row's bottom margin. A row written shorter
 * carries nothing: it does not stand shorter in Word or LibreOffice.
 *
 * <p>Word sets a row at its written height or its tallest cell's content, whichever is more, and
 * adds the cells' largest top and bottom margins. {@code ConsultingInvoice}'s item rows, each
 * written 607 twips where the page makes them 606.6, stood a fiftieth of a point lower with every
 * row in Word and LibreOffice: its long variant's 26 rows came to 0.56pt in Word.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxTableRowRoundingTest {

    /** A face whose lines are rounded up to the twip: 212.64 twips, written 213. */
    private static final DocumentTextStyle BODY = DocumentTextStyle.builder().fontName(FontName.LATO).size(8.86).build();

    @Test
    void aLongTablesRowsWrittenTallerStandWhereThePagesDoOnEachPage() throws Exception {
        // Two lines to a cell, as ConsultingInvoice's quantity and its unit. At 8.86pt a line is
        // 212.64 twips, written 213, and the row 0.72 twip taller than the page's; at 9.41pt, 0.32.
        // At 8.89pt a line is 213.36 twips, written 213, but the row's height is rounded up, and
        // that sets it 0.28 twip taller. 40 rows over two pages.
        for (double size : new double[] {8.86, 8.89, 9.41}) {
            try (Exported exported = export(lato(size), 3.95, 0, rows(40, "10.00", "hrs"))) {
                assertTheRowsStandWhereThePagesDo(exported, size + "pt");
                long most = mostBottom(exported.table());
                assertThat(bottoms(exported.table()).stream().distinct())
                        .as("at %spt, some rows take a twip off their bottom margin", size)
                        .containsExactlyInAnyOrder(most, most - 1);
            }
        }
    }

    @Test
    void aRowWrittenTallerByMoreThanATwipTakesOffWhatItComesTo() throws Exception {
        // Three lines of one paragraph at 8.86pt, a cell with no space between its lines: each line
        // 0.36 twip taller than the page's, 1.08 a row, all rounding.
        try (Exported exported = export(BODY, 3.95, 0, 0, rows(30, "10.00", "hrs", "each"))) {
            assertThat(exported.table().getRow(0).getCell(1).getParagraphs()).as("one paragraph of three lines").hasSize(1);
            // Left alone past a twip a row, the rows would stand 1.08 twips lower each.
            assertTheRowsStandWhereThePagesDo(exported, "three lines");
            assertThat(bottoms(exported.table()).stream().distinct()).as("rows take one twip off, or two").hasSize(2);
        }
    }

    @Test
    void aRuledTablesRowsTakeOffWhatTheSameRowsUnruledDo() throws Exception {
        // The rules' share comes off the cells' padding, and the row is held less the rules it
        // holds besides: ruled at 0.75pt or not at all, the rows are as far off the page's.
        try (Exported unruled = export(BODY, 3.95, 0, rows(30, "10.00", "hrs"));
             Exported ruled = export(BODY, 3.95, 0.75, rows(30, "10.00", "hrs"))) {
            List<Long> plain = bottoms(unruled.table());
            List<Long> withRules = bottoms(ruled.table());
            assertThat(plain.stream().distinct()).as("the unruled rows take twips off").hasSize(2);
            // The last row holds the rule below the table whole, its margin less than the others'.
            long mostRuled = withRules.subList(0, withRules.size() - 1).stream().mapToLong(Long::longValue).max().orElseThrow();
            for (int i = 0; i < plain.size() - 1; i++) {
                assertThat(mostRuled - withRules.get(i)).as("row %s", i).isEqualTo(mostBottom(unruled.table()) - plain.get(i));
            }
        }
    }

    private static void assertTheRowsStandWhereThePagesDo(Exported exported, String label) {
        List<PlacedFragment> rows = exported.rows();
        assertThat(rows.get(rows.size() - 1).pageIndex()).as("%s: the table runs onto a second page", label).isPositive();
        XWPFTable table = exported.table();
        double off = 0;
        int page = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).pageIndex() != page) {
                page = rows.get(i).pageIndex();
                off = 0;
            }
            off += inWord(table.getRow(i)) - rows.get(i).height() * 20;
            assertThat(off).as("%s, row %s: the rows down to it, off the page's", label, i)
                    .isBetween(-0.5 - 1e-6, 0.5 + 1e-6);
        }
    }

    private static Consumer<com.demcha.compose.document.dsl.TableBuilder> rows(int count, String... lines) {
        return table -> {
            for (int row = 0; row < count; row++) {
                table.rowCells(DocumentTableCell.text(String.valueOf(row + 1)), DocumentTableCell.lines(lines));
            }
        };
    }

    private static List<Long> bottoms(XWPFTable table) {
        return table.getRows().stream().map(row -> bottom(row.getCell(0))).toList();
    }

    private static long mostBottom(XWPFTable table) {
        return bottoms(table).stream().mapToLong(Long::longValue).max().orElseThrow();
    }

    private static DocumentTextStyle lato(double size) {
        return DocumentTextStyle.builder().fontName(FontName.LATO).size(size).build();
    }

    @Test
    void rowsWrittenShorterThanThePagesOrToTheTwipAreLeftAsTheyAre() throws Exception {
        // At 8.88pt each line is 213.12 twips, written 213: a row 0.24 twip short of the page's,
        // which stands no shorter in Word. At 8.5pt each is 204 twips to the twip.
        for (double size : new double[] {8.88, 8.5}) {
            try (Exported exported = export(lato(size), 3.95, 0, table -> {
                for (int row = 0; row < 12; row++) {
                    table.rowCells(DocumentTableCell.text(String.valueOf(row + 1)), DocumentTableCell.lines("10.00", "hrs"));
                }
            })) {
                XWPFTable table = exported.table();
                assertThat(inWord(table.getRow(0))).as("at %spt, the row no taller than the page's", size)
                        .isLessThanOrEqualTo(Math.round(exported.rows().get(0).height() * 20));
                assertThat(table.getRows().stream().map(row -> bottom(row.getCell(0))).distinct())
                        .as("at %spt, every row's bottom margin as the page's padding writes it", size).hasSize(1);
            }
        }
    }

    @Test
    void aRowHoldingACellBuiltFromANodeTallerThanItsTextIsLeftAsItIs() throws Exception {
        // A cell built from a node is not read; its row is not read where no text cell sets it,
        // so nothing is carried and no margin moves.
        try (Exported exported = export(3.95, table -> {
            for (int row = 0; row < 12; row++) {
                table.rowCells(DocumentTableCell.text(String.valueOf(row + 1)),
                        DocumentTableCell.node(new ParagraphBuilder().text("Leadership alignment workshops with every stakeholder group, each one held twice")
                                .textStyle(BODY).build()));
            }
        })) {
            XWPFTable table = exported.table();
            assertThat(table.getRows().stream().map(row -> bottom(row.getCell(0))).distinct())
                    .as("every row's bottom margin as the page's padding writes it").hasSize(1);
        }
    }

    @Test
    void aRowInAVerticalMergeIsLeftAsItIs() throws Exception {
        try (Exported exported = export(3.95, table -> {
            table.rowCells(DocumentTableCell.text("Span").rowSpan(12), DocumentTableCell.lines("10.00", "hrs"));
            for (int row = 1; row < 12; row++) {
                table.rowCells(DocumentTableCell.lines("10.00", "hrs"));
            }
        })) {
            XWPFTable table = exported.table();
            assertThat(table.getRows().stream().map(row -> bottom(row.getCell(row.getTableCells().size() - 1))).distinct())
                    .as("every row's bottom margin as the page's padding writes it").hasSize(1);
        }
    }

    /** How tall Word sets a row, in twips: its written height or its tallest cell's content, and its margins. */
    private static long inWord(XWPFTableRow row) {
        long content = row.getHeight();
        long margins = 0;
        for (XWPFTableCell cell : row.getTableCells()) {
            long cellContent = 0;
            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                CTSpacing spacing = paragraph.getCTP().getPPr().getSpacing();
                long lines = 1 + paragraph.getRuns().stream().mapToLong(run -> run.getCTR().sizeOfBrArray()).sum();
                cellContent += lines * twips(spacing.getLine()) + twips(spacing.isSetBefore() ? spacing.getBefore() : null)
                               + twips(spacing.isSetAfter() ? spacing.getAfter() : null);
            }
            content = Math.max(content, cellContent);
            margins = Math.max(margins, top(cell) + bottom(cell));
        }
        return content + margins;
    }

    private static long twips(Object value) {
        return value instanceof BigInteger number ? number.longValue() : 0;
    }

    private static long top(XWPFTableCell cell) {
        CTTcMar margins = cell.getCTTc().getTcPr().getTcMar();
        return margins == null || !margins.isSetTop() ? 0 : twips(margins.getTop().getW());
    }

    private static long bottom(XWPFTableCell cell) {
        CTTcMar margins = cell.getCTTc().getTcPr().getTcMar();
        return margins == null || !margins.isSetBottom() ? 0 : twips(margins.getBottom().getW());
    }

    private static Exported export(double padding,
                                   Consumer<com.demcha.compose.document.dsl.TableBuilder> rows) throws Exception {
        return export(BODY, padding, 0, rows);
    }

    private static Exported export(DocumentTextStyle text, double padding, double rule,
                                   Consumer<com.demcha.compose.document.dsl.TableBuilder> rows) throws Exception {
        return export(text, padding, rule, 1.15, rows);
    }

    private static Exported export(DocumentTextStyle text, double padding, double rule, double lineSpacing,
                                   Consumer<com.demcha.compose.document.dsl.TableBuilder> rows) throws Exception {
        DocumentSession session = GraphCompose.document()
                .pageSize(300, 500)
                .margin(DocumentInsets.of(20))
                .create();
        session.pageFlow(page -> page.addTable(t -> {
            t.name("Items")
                    .columns(DocumentTableColumn.fixed(60), DocumentTableColumn.fixed(120))
                    .defaultCellStyle(DocumentTableStyle.builder()
                            .padding(DocumentInsets.of(padding))
                            .stroke(DocumentStroke.of(DocumentColor.BLACK, rule))
                            .textStyle(text)
                            .lineSpacing(lineSpacing)
                            .build());
            rows.accept(t);
        }));
        List<PlacedFragment> placed = session.layoutGraph().fragments().stream()
                .filter(fragment -> fragment.payload() instanceof TableRowFragmentPayload)
                .toList();
        XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(session.export(new DocxSemanticBackend())));
        return new Exported(session, document, placed);
    }

    private record Exported(DocumentSession session, XWPFDocument document, List<PlacedFragment> rows)
            implements AutoCloseable {

        XWPFTable table() {
            return document.getTables().get(0);
        }

        @Override
        public void close() throws Exception {
            document.close();
            session.close();
        }
    }
}
