package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.RowArrangement;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.DocumentRowColumn;

import java.util.List;

/**
 * Shared row slot-width helpers for the compile and measure phases.
 *
 * <p>Centralises the {@link IllegalArgumentException} contract used by both
 * {@link #distributeRowSlotWidths(List, List, double, double) compile-phase}
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

    /**
     * Whether a row uses the flex distribution path — a grow spacer absorbs the
     * leftover width, or a non-{@link RowArrangement#START START} arrangement
     * justifies it. Only this returning {@code true} routes a row through
     * {@link #distributeFlex}; every other row keeps the unchanged
     * columns / weights / even split.
     *
     * @param node the node being distributed
     * @return {@code true} when flex distribution applies
     */
    static boolean hasFlexLayout(DocumentNode node) {
        if (!(node instanceof RowNode row)) {
            return false;
        }
        if (row.arrangement() != RowArrangement.START) {
            return true;
        }
        return hasGrowChild(row.children());
    }

    private static boolean hasGrowChild(List<DocumentNode> children) {
        for (DocumentNode child : children) {
            if (child instanceof SpacerNode spacer && spacer.grow() > 0.0) {
                return true;
            }
        }
        return false;
    }

    private static double growOf(DocumentNode child) {
        return child instanceof SpacerNode spacer ? spacer.grow() : 0.0;
    }

    /**
     * Distributes the row width for the flex path: every non-grow child takes its
     * intrinsic (natural content) width, and the leftover is shared across the
     * grow children in proportion to their grow factors. With no grow child the
     * children keep their intrinsic widths and the leftover is handed to
     * {@link #flexJustify} for main-axis arrangement. Called identically by the
     * compile and measure phases so the slot widths match.
     *
     * @param children   the row's children
     * @param gap        gap between children
     * @param innerWidth the row's content width
     * @param ctx        the prepare context (for intrinsic widths)
     * @return resolved slot width per child
     */
    static double[] distributeFlex(List<DocumentNode> children,
                                   double gap,
                                   double innerWidth,
                                   PrepareContext ctx) {
        int n = children.size();
        double available = rowAvailableWidth(innerWidth, gap, n);
        double[] slots = new double[n];
        double totalGrow = 0.0;
        double used = 0.0;
        for (int i = 0; i < n; i++) {
            DocumentNode child = children.get(i);
            double grow = growOf(child);
            if (grow > 0.0) {
                totalGrow += grow;
            } else {
                double childInner = Math.max(0.0, available - child.margin().horizontal());
                double natural = ctx.prepare(child, BoxConstraints.natural(childInner)).measureResult().width();
                slots[i] = natural + child.margin().horizontal();
                used += slots[i];
            }
        }
        double remaining = Math.max(0.0, available - used);
        if (totalGrow > 0.0) {
            for (int i = 0; i < n; i++) {
                double grow = growOf(children.get(i));
                if (grow > 0.0) {
                    slots[i] = remaining * (grow / totalGrow);
                }
            }
        }
        return slots;
    }

    /**
     * Resolves a non-START {@link RowArrangement} into a leading offset and an
     * extra inter-child gap that justify {@code leftover} width across {@code n}
     * content-sized children. The compile phase applies these to the cursor; the
     * measure phase does not need them (they shift x only, not height).
     *
     * @param arrangement the row arrangement (must not be START)
     * @param leftover    the unused width to distribute
     * @param n           number of children
     * @return {@code {leading, extraGap}} — leading offset before the first child,
     *         and extra gap inserted between adjacent children
     */
    static double[] flexJustify(RowArrangement arrangement, double leftover, int n) {
        double leading = 0.0;
        double extraGap = 0.0;
        switch (arrangement) {
            case START -> {
            }
            case CENTER -> leading = leftover / 2.0;
            case END -> leading = leftover;
            case SPACE_BETWEEN -> {
                if (n > 1) {
                    extraGap = leftover / (n - 1);
                }
            }
            case SPACE_AROUND -> {
                extraGap = n > 0 ? leftover / n : 0.0;
                leading = extraGap / 2.0;
            }
            case SPACE_EVENLY -> {
                leading = leftover / (n + 1);
                extraGap = leading;
            }
        }
        return new double[]{leading, extraGap};
    }

    /**
     * Distributes the row width across weighted slots. With no weights every
     * child receives an equal share of the width left after the inter-child
     * gaps; with weights each child receives a share proportional to its weight
     * (a non-positive weight total falls back to the even split).
     *
     * @param children   the row children (only the count is read)
     * @param weights    per-child weights, or {@code null} / empty for an even split
     * @param gap        gap between children
     * @param innerWidth the row's content width
     * @return resolved slot width per child
     * @throws IllegalArgumentException if a non-empty weight list size does not
     *                                  match the child count
     */
    static double[] distributeRowSlotWidths(List<DocumentNode> children,
                                            List<Double> weights,
                                            double gap,
                                            double innerWidth) {
        int n = children.size();
        double available = Math.max(0.0, innerWidth - gap * Math.max(0, n - 1));
        double[] slots = new double[n];
        if (weights == null || weights.isEmpty()) {
            double share = n > 0 ? available / n : 0.0;
            for (int i = 0; i < n; i++) {
                slots[i] = share;
            }
            return slots;
        }
        validateWeightsMatchChildren(weights, n);
        double total = 0.0;
        for (double w : weights) {
            total += w;
        }
        if (total <= 0.0) {
            double share = n > 0 ? available / n : 0.0;
            for (int i = 0; i < n; i++) {
                slots[i] = share;
            }
            return slots;
        }
        for (int i = 0; i < n; i++) {
            slots[i] = available * (weights.get(i) / total);
        }
        return slots;
    }

    /**
     * Resolves a horizontal row's slot layout from its arrangement mode: flex
     * (with optional non-START justification), explicit columns, or plain
     * weights. Reads no compiler state and reuses the same primitives the
     * measure phase uses, so the compile phase seats children on identical
     * widths.
     *
     * @param node             the row node (read for its columns / arrangement)
     * @param children         the row children
     * @param layoutSpec       the resolved composite layout spec (spacing / weights)
     * @param childRegionWidth the row's inner content width
     * @param prepareContext   preparation context for intrinsic child measurement
     * @param semanticName     row name for the over-constrained column error
     * @return the slot widths plus the flex leading offset and extra inter-child gap
     */
    static SlotLayout resolveLayout(DocumentNode node,
                                    List<DocumentNode> children,
                                    CompositeLayoutSpec layoutSpec,
                                    double childRegionWidth,
                                    PrepareContext prepareContext,
                                    String semanticName) {
        List<DocumentRowColumn> columns = node instanceof RowNode row ? row.columns() : List.of();
        double[] slotWidths;
        // flexLeading / flexExtraGap stay 0.0 for every non-flex row, so the
        // caller's cursor math is byte-identical to the pre-flex placement.
        double flexLeading = 0.0;
        double flexExtraGap = 0.0;
        if (hasFlexLayout(node)) {
            slotWidths = distributeFlex(children, layoutSpec.spacing(), childRegionWidth, prepareContext);
            RowArrangement arrangement = node instanceof RowNode flexRow
                    ? flexRow.arrangement() : RowArrangement.START;
            if (arrangement != RowArrangement.START) {
                double available = rowAvailableWidth(
                        childRegionWidth, layoutSpec.spacing(), children.size());
                double used = 0.0;
                for (double slot : slotWidths) {
                    used += slot;
                }
                double[] justify = flexJustify(
                        arrangement, Math.max(0.0, available - used), children.size());
                flexLeading = justify[0];
                flexExtraGap = justify[1];
            }
        } else if (!columns.isEmpty()) {
            double available = rowAvailableWidth(childRegionWidth, layoutSpec.spacing(), children.size());
            double[] intrinsic = intrinsicColumnWidths(children, columns, available, prepareContext);
            slotWidths = distributeColumns(columns, intrinsic, layoutSpec.spacing(), childRegionWidth, semanticName);
        } else {
            slotWidths = distributeRowSlotWidths(children, layoutSpec.weights(),
                    layoutSpec.spacing(), childRegionWidth);
        }
        return new SlotLayout(slotWidths, flexLeading, flexExtraGap);
    }

    /**
     * Resolved slot geometry for one horizontal row: the per-child slot widths
     * plus the flex justification offsets applied to the placement cursor. A
     * transient return holder — the {@code widths} array is freshly allocated
     * per call and not shared.
     *
     * @param widths   resolved slot width per child
     * @param leading  x offset before the first child (non-START flex only)
     * @param extraGap extra gap inserted between adjacent children (non-START flex only)
     */
    record SlotLayout(double[] widths, double leading, double extraGap) {
    }
}
