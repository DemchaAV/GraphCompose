package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;

import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Splits a table of the body in two at the row a page starts with, so that a Word section can
 * start there: a section ends in a paragraph, which stands between blocks.
 *
 * <p>The rows from that row on go to a table of their own after it, which opens with copies of
 * the table's header rows: the layout repeats them at the top of each page the table runs onto,
 * as Word repeats a table's leading header rows. A paragraph between the two parts ends the
 * first.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxTableSplit {

    private static final String W =
            "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' ";

    // What a file holds once: a bookmark's name marks one place, and a drawing's id names one drawing.
    private static final List<String> HELD_ONCE = List.of("bookmarkStart", "drawing", "pict", "object");

    // A field, which the export settles once the body is written, in the paragraph it wrote it in: a
    // row moved to the second part leaves that paragraph behind, and a copy of one is never settled.
    private static final List<String> FIELDS = List.of("fldChar", "instrText", "fldSimple");

    private DocxTableSplit() {
    }

    /**
     * The row of a table the layout starts a page with, where the table can be split there.
     *
     * <p>It cannot be where a cell spans rows across that row, where only the header rows stand
     * above it, or where a header row holds what the file can hold only once — a bookmark or a
     * drawing — as the part after it would hold that row again. Nor where a header row or a row
     * from that one on holds a field, a page reference's: the export settles it once the body is
     * written, in the paragraph it wrote it in.</p>
     *
     * @param table  the table as written
     * @param page   the page, from 0
     * @param pageOf the page the layout placed each of its rows on, by index; -1 for one it did not
     * @return the row's index, or -1 where no row starts the page or the table cannot be split there
     */
    static int rowStarting(XWPFTable table, int page, IntUnaryOperator pageOf) {
        int rows = table.getNumberOfRows();
        int header = headerRows(table);
        if (holds(table, 0, header, HELD_ONCE) || holds(table, 0, header, FIELDS)) {
            return -1;
        }
        for (int row = header; row < rows; row++) {
            int on = pageOf.applyAsInt(row);
            if (on < 0) {
                return -1;
            }
            if (on == page) {
                return row > header && !continuesAMerge(table.getRow(row)) && !holds(table, row, rows, FIELDS)
                        ? row : -1;
            }
        }
        return -1;
    }

    /**
     * How many of a table's leading rows Word repeats at the top of each page it runs onto.
     *
     * @param table the table
     * @return the number of its header rows
     */
    static int headerRows(XWPFTable table) {
        int header = 0;
        while (header < table.getNumberOfRows() && table.getRow(header).isRepeatHeader()) {
            header++;
        }
        return header;
    }

    /**
     * Splits a table of the body before one of its rows: that row and the rows after it go to a
     * table of their own after it, under copies of its header rows, and a paragraph is put between
     * the two.
     *
     * @param document the document the table is in the body of
     * @param table    the table
     * @param row      the row the second part starts with, after the header rows
     * @return the paragraph between the two parts
     */
    static XWPFParagraph split(XWPFDocument document, XWPFTable table, int row) {
        int header = headerRows(table);
        CTBody body = document.getDocument().getBody();
        int written = List.of(body.getTblArray()).indexOf(table.getCTTbl());
        try (XmlCursor from = table.getCTTbl().newCursor(); XmlCursor after = table.getCTTbl().newCursor()) {
            after.toEndToken();
            after.toNextToken();
            from.copyXml(after);
        }
        CTTbl copied = body.getTblArray(written + 1);
        XWPFTable second = new XWPFTable(copied, document);
        document.insertTable(document.getBodyElements().indexOf(table) + 1, second);
        for (int index = row - 1; index >= header; index--) {
            second.removeRow(index);
        }
        for (int index = table.getNumberOfRows() - 1; index >= row; index--) {
            table.removeRow(index);
        }
        try (XmlCursor before = copied.newCursor()) {
            return document.insertNewParagraph(before);
        }
    }

    /** Whether a table's rows from one to another, that one left out, hold any of some elements. */
    private static boolean holds(XWPFTable table, int from, int to, List<String> elements) {
        for (int row = from; row < to; row++) {
            for (String element : elements) {
                if (table.getRow(row).getCtRow().selectPath(W + ".//w:" + element).length > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether a cell of the row continues one merged down from the row above. */
    private static boolean continuesAMerge(XWPFTableRow row) {
        for (XWPFTableCell cell : row.getTableCells()) {
            CTTcPr properties = cell.getCTTc().getTcPr();
            if (properties != null && properties.isSetVMerge()
                && properties.getVMerge().getVal() != STMerge.RESTART) {
                return true;
            }
        }
        return false;
    }
}
