package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentInsets;

import java.util.ArrayList;
import java.util.List;

/**
 * Columns placed side by side, each flowing down its own column and
 * continuing on the next page.
 *
 * <p>A {@link RowNode} puts children beside each other in one band and is
 * atomic: the whole band must fit on the page it starts on, and content that
 * does not fit has nowhere to go. That is right for a row of cells and wrong
 * for a two-column document body, which is why layouts built that way cap
 * their content at a page and truncate the rest.</p>
 *
 * <p>This is the other shape. Each child is a column, and each column is an
 * ordinary vertical flow: it breaks where it runs out of page and resumes at
 * the top of the next one, independently of its neighbours. The node ends on
 * the last page any column reached, so whatever follows continues below the
 * longest column.</p>
 *
 * <pre>
 *   page 1                    page 2
 *   ┌────────┬─────────┐      ┌────────┬─────────┐
 *   │ side   │ main    │      │ side   │ main    │
 *   │ …      │ …       │  →   │ …cont. │ …cont.  │
 *   └────────┴─────────┘      └────────┴─────────┘
 * </pre>
 *
 * <p>Widths are resolved once, from {@code weights} (or evenly when none are
 * given), and every page uses the same ones — a column that changed width
 * halfway down a document would not read as one column.</p>
 *
 * <p>The node carries no fill or border of its own: a column that wants a
 * panel is a section with a fill, and the engine already repeats a section's
 * fill on each page it spans. Chrome that must reach the page edge belongs in
 * a page background, which paints on every page by definition.</p>
 *
 * @param name     diagnostic name for layout paths and snapshots
 * @param children one node per column, left to right
 * @param weights  relative column widths; empty distributes evenly
 * @param gap      horizontal gap between columns
 * @param padding  inner padding applied to the whole flow
 * @param margin   outer margin applied to the whole flow
 * @since 2.3.0
 */
public record ColumnFlowNode(
        String name,
        List<DocumentNode> children,
        List<Double> weights,
        double gap,
        DocumentInsets padding,
        DocumentInsets margin
) implements DocumentNode {

    /**
     * Creates a normalized multi-column flow container.
     */
    public ColumnFlowNode {
        name = name == null ? "" : name;
        children = children == null ? List.of() : List.copyOf(children);
        for (DocumentNode column : children) {
            // A column is a vertical container because that is what paginates.
            // Checked here rather than only in the builder so every route in —
            // the record, the factory, an import layer — gets the same answer.
            if (!(column instanceof SectionNode) && !(column instanceof ContainerNode)) {
                throw new IllegalArgumentException(
                        "A column flow's children are columns, and a column is a vertical "
                                + "container (section or container) because that is what "
                                + "paginates. Received: " + column.nodeKind()
                                + ". Wrap it in a column instead.");
            }
        }
        weights = weights == null ? List.of() : List.copyOf(weights);
        if (!weights.isEmpty() && weights.size() != children.size()) {
            throw new IllegalArgumentException(
                    "Column flow weights size (" + weights.size() + ") must match children size ("
                            + children.size() + "). Pass exactly " + children.size()
                            + " weight(s) or leave weights empty for an even split.");
        }
        for (Double weight : weights) {
            if (weight == null || Double.isNaN(weight) || Double.isInfinite(weight) || weight <= 0.0) {
                throw new IllegalArgumentException(
                        "Column flow weights must be positive finite numbers: " + weights);
            }
        }
        if (Double.isNaN(gap) || Double.isInfinite(gap) || gap < 0.0) {
            throw new IllegalArgumentException("gap must be finite and non-negative: " + gap);
        }
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String nodeKind() {
        return "columnFlow";
    }

    /**
     * The columns, left to right.
     *
     * @return one node per column
     */
    public List<DocumentNode> columns() {
        return children;
    }

    /**
     * A flow whose columns share the width evenly.
     *
     * @param name    diagnostic name
     * @param gap     horizontal gap between columns
     * @param columns one node per column
     * @return a column flow with even widths
     */
    public static ColumnFlowNode of(String name, double gap, DocumentNode... columns) {
        List<DocumentNode> children = columns == null ? List.of() : new ArrayList<>(List.of(columns));
        return new ColumnFlowNode(name, children, List.of(), gap,
                DocumentInsets.zero(), DocumentInsets.zero());
    }
}
