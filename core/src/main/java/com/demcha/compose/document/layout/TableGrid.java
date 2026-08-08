package com.demcha.compose.document.layout;

import com.demcha.compose.document.api.Internal;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.table.DocumentTableCell;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves a {@link TableNode}'s authored rows into the grid positions they occupy.
 *
 * <p>A table is authored sparsely: a cell with {@code rowSpan} covers positions in the rows
 * below it, and those rows do not repeat the covered cells. So a row's list of
 * {@link DocumentTableCell} records is not a list of columns, and the number of records in
 * the first row is not the table's column count either — a {@code colSpan} makes the two
 * differ. Reading either as if it were is how a table comes out with its rows shifted, or
 * with the cells past the end of a too-narrow grid dropped in silence.</p>
 *
 * <p>This is the one place that walks the occupancy matrix and decides where each authored
 * cell lands. It exists because the layout pipeline is not the only consumer: a semantic
 * backend writes the same grid into a format with its own merge markup, and a second
 * implementation of these rules would drift from this one without any test noticing.</p>
 *
 * <p>Malformed grids are rejected here rather than being drawn wrong: a cell that would
 * overrun the columns or rows, one that overlaps a position an earlier span already took,
 * a row that runs out of cells before the grid is full, and a row that still has cells
 * after it is. Each names the position, because the author's row indices and the grid's do
 * not line up once a span is involved.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.1.2
 */
@Internal
public final class TableGrid {

    private TableGrid() {
    }

    /**
     * An authored cell and the grid rectangle it occupies.
     *
     * @param row     grid row the cell starts in
     * @param column  grid column the cell starts in
     * @param colSpan columns the cell occupies, at least 1
     * @param rowSpan rows the cell occupies, at least 1
     * @param cell    the authored cell
     */
    public record Placement(int row, int column, int colSpan, int rowSpan, DocumentTableCell cell) {
    }

    /**
     * The table's column count.
     *
     * <p>The first row by definition has no rowSpan-occupied slots from earlier rows, so its
     * colSpan sum equals the column count. Subsequent rows may have fewer source cells when
     * a prior rowSpan covers some of their columns, so they must not be used to derive it.
     * A declared column spec wins when it asks for more.</p>
     *
     * @param node the table
     * @return the number of grid columns
     */
    public static int columnCount(TableNode node) {
        int firstRowColSpanSum = 0;
        if (!node.rows().isEmpty()) {
            for (DocumentTableCell cell : node.rows().get(0)) {
                firstRowColSpanSum += cell.colSpan();
            }
        }
        return Math.max(node.columns().size(), firstRowColSpanSum);
    }

    /**
     * Places every authored cell on the grid, one list per authored row, in column order.
     *
     * @param node the table
     * @return the placements, row by row
     * @throws IllegalStateException if the authored rows do not describe a rectangular grid
     */
    public static List<List<Placement>> resolve(TableNode node) {
        int columnCount = columnCount(node);
        int rowCount = node.rows().size();
        boolean[][] occupied = new boolean[rowCount][columnCount];
        List<List<Placement>> result = new ArrayList<>(rowCount);

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            List<DocumentTableCell> source = node.rows().get(rowIndex);
            List<Placement> placements = new ArrayList<>(source.size());
            int sourceIdx = 0;
            int col = 0;
            while (col < columnCount) {
                if (occupied[rowIndex][col]) {
                    col++;
                    continue;
                }
                if (sourceIdx >= source.size()) {
                    throw new IllegalStateException("Row " + rowIndex
                                                    + " is missing a cell for column " + col
                                                    + " (table has " + columnCount + " columns; source row provides "
                                                    + source.size() + " cells, prior rowSpan covers some columns).");
                }
                DocumentTableCell cell = source.get(sourceIdx++);
                if (col + cell.colSpan() > columnCount) {
                    throw new IllegalStateException("Cell at row " + rowIndex
                                                    + " column " + col + " has colSpan " + cell.colSpan()
                                                    + " but only " + (columnCount - col) + " columns remain.");
                }
                if (rowIndex + cell.rowSpan() > rowCount) {
                    throw new IllegalStateException("Cell at row " + rowIndex
                                                    + " column " + col + " has rowSpan " + cell.rowSpan()
                                                    + " but only " + (rowCount - rowIndex) + " rows remain.");
                }
                for (int r = rowIndex; r < rowIndex + cell.rowSpan(); r++) {
                    for (int c = col; c < col + cell.colSpan(); c++) {
                        if (occupied[r][c]) {
                            throw new IllegalStateException("Cell at row " + rowIndex
                                                            + " column " + col + " (colSpan=" + cell.colSpan()
                                                            + ", rowSpan=" + cell.rowSpan()
                                                            + ") overlaps an already-spanned position (" + r + ", " + c + ").");
                        }
                        occupied[r][c] = true;
                    }
                }
                placements.add(new Placement(rowIndex, col, cell.colSpan(), cell.rowSpan(), cell));
                col += cell.colSpan();
            }
            if (sourceIdx < source.size()) {
                throw new IllegalStateException("Row " + rowIndex
                                                + " has " + (source.size() - sourceIdx) + " extra source cell(s) "
                                                + "after the grid was already filled — column slots are accounted for "
                                                + "by colSpan plus rowSpan from earlier rows.");
            }
            result.add(List.copyOf(placements));
        }
        return List.copyOf(result);
    }
}
