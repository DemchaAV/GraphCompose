package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentRowColumn;

import java.util.List;

/**
 * Shared row slot-width helpers for the compile and measure phases.
 *
 * <p>Centralises the {@link IllegalArgumentException} contract used by both
 * {@link LayoutCompiler#distributeRowSlotWidths(List, List, double, double) compile-phase}
 * and {@link NodeDefinitionSupport#measureRow measure-phase} row distribution,
 * and owns the explicit-column ({@code fixed / intrinsic / weight}) distribution
 * so both phases compute the same slot widths from a single source.</p>
 *
 * <p>Package-private intentionally — engine surface, not public API.</p>
 *
 * @author Artem Demchyshyn
 */
final class RowSlots {

    private static final double EPS = 1e-6;

    private RowSlots() {
        // Utility class, no instantiation.
    }

    /**
     * Inner width left for the slots after subtracting the inter-child gaps.
     *
     * @param innerWidth  the row's content width
     * @param gap         gap between children
     * @param childCount  number of children
     * @return the width available to distribute across the slots
     */
    static double rowAvailableWidth(double innerWidth, double gap, int childCount) {
        return Math.max(0.0, innerWidth - gap * Math.max(0, childCount - 1));
    }

    /**
     * Distributes the row width across explicit columns: fixed columns take their
     * point width, intrinsic columns take their measured natural width, and the
     * remainder is shared across the weight columns. Called identically by the
     * compile and measure phases (each supplies the same intrinsic widths), so a
     * weight-only column list reduces to the same {@code available * (w / total)}
     * split as plain weights.
     *
     * @param columns         one width spec per child
     * @param intrinsicWidths measured natural width per child (read only for
     *                        intrinsic columns; other entries are ignored)
     * @param gap             gap between children
     * @param innerWidth      the row's content width
     * @param rowName         row name for the over-constrained error message
     * @return resolved slot width per child
     * @throws IllegalArgumentException if the fixed + intrinsic columns exceed the
     *                                  available width
     */
    static double[] distributeColumns(List<DocumentRowColumn> columns,
                                      double[] intrinsicWidths,
                                      double gap,
                                      double innerWidth,
                                      String rowName) {
        int n = columns.size();
        double available = rowAvailableWidth(innerWidth, gap, n);
        double[] slots = new double[n];
        double used = 0.0;
        double totalWeight = 0.0;
        for (int i = 0; i < n; i++) {
            DocumentRowColumn column = columns.get(i);
            switch (column.type()) {
                case FIXED -> {
                    slots[i] = column.value();
                    used += slots[i];
                }
                case AUTO -> {
                    slots[i] = Math.max(0.0, intrinsicWidths[i]);
                    used += slots[i];
                }
                case WEIGHT -> totalWeight += column.value();
            }
        }
        if (used > available + EPS) {
            throw new IllegalArgumentException("Row '" + rowName + "' fixed and auto columns need "
                                               + used + "pt but only " + available
                                               + "pt is available. Reduce the fixed widths or the row content.");
        }
        double remaining = Math.max(0.0, available - used);
        if (totalWeight > 0.0) {
            for (int i = 0; i < n; i++) {
                if (columns.get(i).type() == DocumentRowColumn.Type.WEIGHT) {
                    slots[i] = remaining * (columns.get(i).value() / totalWeight);
                }
            }
        }
        return slots;
    }

    /**
     * Measures the natural (content) outer width of each intrinsic column, for
     * {@link #distributeColumns}. Both the compile and measure phases call this
     * with the same {@code available} width and prepare context, so the resolved
     * intrinsic widths — and therefore the slot widths — match.
     *
     * @param children  the row's children
     * @param columns   one width spec per child
     * @param available the row width available to the slots (after gaps)
     * @param ctx       the prepare context
     * @return natural outer width per child (zero for non-intrinsic columns)
     */
    static double[] intrinsicColumnWidths(List<DocumentNode> children,
                                          List<DocumentRowColumn> columns,
                                          double available,
                                          PrepareContext ctx) {
        double[] intrinsic = new double[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).type() == DocumentRowColumn.Type.AUTO) {
                DocumentNode child = children.get(i);
                double childInner = Math.max(0.0, available - child.margin().horizontal());
                double natural = ctx.prepare(child, BoxConstraints.natural(childInner)).measureResult().width();
                intrinsic[i] = natural + child.margin().horizontal();
            }
        }
        return intrinsic;
    }

    /**
     * Asserts that an explicit {@code weights} list matches the row's
     * children count. Callers must skip this check when {@code weights}
     * is null or empty — the even-split fallback applies there instead.
     *
     * @param weights    non-null, non-empty weights list
     * @param childCount number of row children
     * @throws IllegalArgumentException if {@code weights.size() != childCount}
     */
    static void validateWeightsMatchChildren(List<Double> weights, int childCount) {
        if (weights.size() != childCount) {
            throw new IllegalArgumentException(
                    "Row weights size (" + weights.size() + ") must match children size ("
                    + childCount + "). Pass exactly " + childCount
                    + " weight(s) or leave weights empty for an even split.");
        }
    }
}
