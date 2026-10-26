package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.xwpf.usermodel.IBodyElement;
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
import java.util.stream.Stream;

/**
 * Splits a table of the body in two at the row a page starts with, so that a Word section can
 * start there: a section ends in a paragraph, which stands between blocks; and cuts a panel there
 * at a row of the table in its cell, which Word would otherwise break as its lines run out, the
 * table's header rows not repeated (see {@link #splitInItsPanel}).
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

    // A drawing, whose id names one drawing: a panel's copy of the header rows leaves it out
    // (splitInItsPanel).
    private static final List<String> DRAWINGS = List.of("drawing", "pict", "object");

    // A bookmark, whose name marks one place: a copy of the header rows leaves it out (split,
    // splitInItsPanel), its end with it. A bookmark that closes in a header row opened in one: a
    // block holding the table closes at its last row or after it.
    private static final List<String> BOOKMARKS = List.of("bookmarkStart", "bookmarkEnd");

    // What a header row's copy cannot hold again: a bookmark's name marks one place, a drawing's id
    // names one drawing, and a list item's number is counted on from the item before it.
    private static final List<String> NOT_REPEATED =
            Stream.concat(Stream.of("bookmarkStart", "numPr"), DRAWINGS.stream()).toList();

    // A field, which the export settles once the body is written, in the paragraph it wrote it in: a
    // row moved to the second part leaves that paragraph behind, and a copy of one is never settled.
    private static final List<String> FIELDS = List.of("fldChar", "instrText", "fldSimple");

    private DocxTableSplit() {
    }

    /**
     * The row of a table the layout starts a page with, where the table can be split there.
     *
     * <p>It cannot be where a cell spans rows across that row, where only the header rows stand
     * above it, or where a header row holds what its copy in the part after it cannot hold again —
     * a bookmark or a drawing, which the file holds once, or a numbered list item, which the copy
     * would number on from the one above. Nor where a header row or a row
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
        if (holds(table, 0, header, NOT_REPEATED) || holds(table, 0, header, FIELDS)) {
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
     * <p>The copy of the header rows holds no bookmark. Where the split is decided, they hold none
     * ({@link #rowStarting}); one the export puts in them after that, at their section's end — a
     * block of drawings alone, marked in the paragraph its first drawing is anchored in — marks the
     * table's first page, and stays in the first part. Copied with its name and its id, both copies
     * went when the export kept one bookmark of each name.</p>
     *
     * @param document the document the table is in the body of
     * @param table    the table
     * @param row      the row the second part starts with, after the header rows
     * @return the paragraph between the two parts
     */
    static XWPFParagraph split(XWPFDocument document, XWPFTable table, int row) {
        XWPFTable second = copyAfter(document, table);
        keepTheRowsBefore(table, row);
        keepTheRowsFrom(second, row);
        drop(second, headerRows(second), BOOKMARKS);
        try (XmlCursor before = second.getCTTbl().newCursor()) {
            return document.insertNewParagraph(before);
        }
    }

    /**
     * Cuts a panel — a table of one cell in the body — in two at a row of a table its cell holds,
     * with a page break between the two parts.
     *
     * <p>The first part keeps what its cell holds up to that table and the table's rows before
     * that row; the second, a copy of the first, holds the table's header rows, its rows from that
     * row on and what follows the table in the cell. Each part is the panel's table, its fill and
     * its four borders. The paragraph put between them breaks the page before itself.</p>
     *
     * <p>The copy of the header rows holds no drawing. Where the cut is decided, they hold none
     * ({@link #rowStarting}); one the export anchors in them after that, at their section's end — a
     * shape of the table's first page standing beside a header's text, or one a page with no other
     * paragraph to carry it leaves in its first cell's — is that page's, and stays in the first
     * part. So does a bookmark the export puts there, for a block of drawings alone, as where a
     * table of the body is split ({@link #split}).</p>
     *
     * @param document the document the panel is in the body of
     * @param panel    the panel
     * @param table    the table in its cell
     * @param row      the row the second part starts with, after the header rows
     * @return the paragraph closing the first part's cell, the paragraph between the two parts and
     *         the second part, the table in its cell the first thing it holds
     */
    static PanelPart splitInItsPanel(XWPFDocument document, XWPFTable panel, XWPFTable table, int row) {
        XWPFTableCell cell = panel.getRow(0).getCell(0);
        int nested = cell.getTables().indexOf(table);
        XWPFTable second = copyAfter(document, panel);
        XWPFTableCell continued = second.getRow(0).getCell(0);
        XWPFTable rest = continued.getTables().get(nested);
        List<IBodyElement> held = cell.getBodyElements();
        for (int index = held.size() - 1; index > held.indexOf(table); index--) {
            remove(cell, held.get(index));
        }
        // A cell ends in a paragraph: one after the table now ending the first part's.
        XWPFParagraph closer = cell.addParagraph();
        keepTheRowsBefore(table, row);
        List<IBodyElement> after = continued.getBodyElements();
        for (int index = after.indexOf(rest) - 1; index >= 0; index--) {
            remove(continued, after.get(index));
        }
        keepTheRowsFrom(rest, row);
        drop(rest, headerRows(rest), DRAWINGS);
        drop(rest, headerRows(rest), BOOKMARKS);
        try (XmlCursor before = second.getCTTbl().newCursor()) {
            XWPFParagraph between = document.insertNewParagraph(before);
            between.setPageBreak(true);
            return new PanelPart(closer, between, second);
        }
    }

    /**
     * What cutting a panel puts in and after its first part.
     *
     * @param closer  the empty paragraph that ends the first part's cell, after its table
     * @param between the paragraph between the two parts, which opens the next page: a break before
     *                it, which a paragraph that opens a page anyway does not add, where a break in it
     *                would put a page of its own after a part that fills its page
     * @param second  the second part
     */
    record PanelPart(XWPFParagraph closer, XWPFParagraph between, XWPFTable second) {
    }

    /** A copy of a table of the body, put right after it. */
    private static XWPFTable copyAfter(XWPFDocument document, XWPFTable table) {
        CTBody body = document.getDocument().getBody();
        int written = List.of(body.getTblArray()).indexOf(table.getCTTbl());
        try (XmlCursor from = table.getCTTbl().newCursor(); XmlCursor after = table.getCTTbl().newCursor()) {
            after.toEndToken();
            after.toNextToken();
            from.copyXml(after);
        }
        XWPFTable copy = new XWPFTable(body.getTblArray(written + 1), document);
        document.insertTable(document.getBodyElements().indexOf(table) + 1, copy);
        return copy;
    }

    /** Takes a table's rows from one on away. */
    private static void keepTheRowsBefore(XWPFTable table, int row) {
        for (int index = table.getNumberOfRows() - 1; index >= row; index--) {
            table.removeRow(index);
        }
    }

    /** Takes a table's rows between its header rows and one away. */
    private static void keepTheRowsFrom(XWPFTable table, int row) {
        int header = headerRows(table);
        for (int index = row - 1; index >= header; index--) {
            table.removeRow(index);
        }
    }

    /** Takes every element of the given names out of a table's first rows. */
    private static void drop(XWPFTable table, int rows, List<String> elements) {
        for (int row = 0; row < rows; row++) {
            for (String element : elements) {
                for (org.apache.xmlbeans.XmlObject found : table.getRow(row).getCtRow().selectPath(W + ".//w:" + element)) {
                    try (XmlCursor cursor = found.newCursor()) {
                        cursor.removeXml();
                    }
                }
            }
        }
    }

    /** Takes a paragraph or a table out of a cell. */
    private static void remove(XWPFTableCell cell, IBodyElement element) {
        if (element instanceof XWPFParagraph paragraph) {
            cell.removeParagraph(cell.getParagraphs().indexOf(paragraph));
        } else if (element instanceof XWPFTable table) {
            cell.removeTable(cell.getTables().indexOf(table));
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
