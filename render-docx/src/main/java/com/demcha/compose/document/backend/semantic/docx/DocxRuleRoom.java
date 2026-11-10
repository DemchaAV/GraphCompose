package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.TableGrid;
import com.demcha.compose.document.style.DocumentInsets;

import java.util.OptionalDouble;

/**
 * How much taller Word sets a table than the page for the room it makes in its rows for the
 * table's horizontal rules, row by row.
 *
 * <p>The page draws a cell's rules over its edge; Word makes room for them in the row, and the
 * export takes that room off the cell's padding. Word sets each cell at its content and, on each
 * side, its padding or its share of the rules there where that is more, and the row at its
 * tallest cell: a row stands taller by what that comes to past the page's height — not by a short
 * cell's padding falling short where a taller cell sets the row. Measured in Word and LibreOffice
 * on Windows, to a tenth of a point (see {@code DocxSemanticBackend.nameTheRoomForTheRules}).</p>
 *
 * <p>A cell of text that shows is counted where the layout tells its row's height on the page;
 * a blank one there is not, its line being cut to the room its row leaves it. Where it does not
 * tell — a table composed in a cell, or no layout, whose rows are not held — or does not measure a
 * counted cell, the row is counted at the most any of its text cells' padding falls short of
 * taking, blank ones included: no more than that. A cell built from a node, and one spanning rows,
 * is not counted. A header row Word repeats on every page counts on each.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxRuleRoom {

    private final double[] tallestInWord;
    private final double[] mostNotTaken;
    private final double[] mostNotTakenAny;
    private final boolean[] unmeasured;

    DocxRuleRoom(int rows) {
        tallestInWord = new double[rows];
        mostNotTaken = new double[rows];
        mostNotTakenAny = new double[rows];
        unmeasured = new boolean[rows];
    }

    /**
     * Counts one text cell of a row.
     *
     * @param row        the row's index
     * @param placement  the cell on the table's grid
     * @param padding    its padding on the page
     * @param round      what Word holds round its content, its rules' shares included, in points
     * @param textHeight its lines' height on the page, or {@code NaN} where the layout does not tell
     */
    void count(int row, TableGrid.Placement placement, DocumentInsets padding, double round, double textHeight) {
        double shortfall = round - Math.max(0, padding.top()) - Math.max(0, padding.bottom());
        mostNotTakenAny[row] = Math.max(mostNotTakenAny[row], shortfall);
        if (!showsText(placement)) {
            return;
        }
        mostNotTaken[row] = Math.max(mostNotTaken[row], shortfall);
        if (Double.isNaN(textHeight)) {
            unmeasured[row] = true;
        } else {
            tallestInWord[row] = Math.max(tallestInWord[row], textHeight + round);
        }
    }

    /**
     * How much taller Word sets the table, and whether that is only the most it can be.
     *
     * @param pageHeight the page's height of each row, empty where the layout does not tell
     * @param times      how many times the page places each row: a repeated header's once a page
     */
    Taller taller(java.util.function.IntFunction<OptionalDouble> pageHeight,
                  java.util.function.IntUnaryOperator times) {
        double taller = 0;
        boolean atMost = false;
        for (int row = 0; row < tallestInWord.length; row++) {
            OptionalDouble page = pageHeight.apply(row);
            double rowTaller;
            if (page.isPresent() && !unmeasured[row]) {
                rowTaller = Math.max(0, tallestInWord[row] - page.getAsDouble());
            } else {
                rowTaller = Math.max(0, page.isPresent() ? mostNotTaken[row] : mostNotTakenAny[row]);
                atMost |= rowTaller > 0;
            }
            taller += rowTaller * Math.max(1, times.applyAsInt(row));
        }
        return new Taller(taller, atMost);
    }

    /** Whether a cell's text shows anything: one that is empty is written as an empty paragraph. */
    static boolean showsText(TableGrid.Placement placement) {
        return placement.cell() != null && placement.cell().content() == null
               && placement.cell().lines().stream().anyMatch(line -> line != null && !line.isEmpty());
    }

    /**
     * How much taller Word sets a table.
     *
     * @param points the height, in points
     * @param atMost whether a row of it is counted at the most it can be
     */
    record Taller(double points, boolean atMost) {
    }
}
