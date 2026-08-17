package com.demcha.compose.document.dsl;

import com.demcha.compose.document.dsl.internal.BuilderSupport;
import com.demcha.compose.document.node.ColumnFlowNode;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.style.DocumentInsets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builder for a multi-column flow: columns side by side, each continuing on
 * the next page.
 *
 * <p>Use this where a {@link RowBuilder} would cap the document at a page. A
 * row places one band and is atomic — the whole band must fit where it starts,
 * and a two-column body built that way can hold only as much as one page. A
 * column flow places the same two columns and lets each break where it runs
 * out of room:</p>
 *
 * <pre>{@code
 * flow.addColumnFlow("Body", body -> body
 *         .gap(18)
 *         .weights(0.72, 1.28)
 *         .addColumn(side -> side.spacing(6).addParagraph(...))
 *         .addColumn(main -> main.spacing(8).addParagraph(...)));
 * }</pre>
 *
 * <p>Every column is a vertical container, because a column <em>is</em> a
 * vertical flow — that is what lets its contents paginate. Anything else
 * (a paragraph, an image, a nested row) belongs inside one of those columns
 * rather than beside them.</p>
 *
 * <p>The flow has no fill or border of its own. A column that wants a panel is
 * a section with a fill, and the engine repeats a section's fill on every page
 * it spans; chrome that must reach the page edge belongs in a page background,
 * which paints on every page by definition.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.3.0
 */
public final class ColumnFlowBuilder {
    private final List<DocumentNode> columns = new ArrayList<>();
    private final List<Double> weights = new ArrayList<>();
    private String name = "";
    private double gap;
    private DocumentInsets padding = DocumentInsets.zero();
    private DocumentInsets margin = DocumentInsets.zero();

    /**
     * Creates a column-flow builder.
     */
    public ColumnFlowBuilder() {
    }

    /**
     * Sets the diagnostic name used in snapshots and layout-graph paths.
     *
     * @param value flow name
     * @return this builder
     */
    public ColumnFlowBuilder name(String value) {
        this.name = value == null ? "" : value;
        return this;
    }

    /**
     * Sets the horizontal gap between columns.
     *
     * @param value gap in points; must be finite and non-negative
     * @return this builder
     */
    public ColumnFlowBuilder gap(double value) {
        this.gap = value;
        return this;
    }

    /**
     * Sets the relative column widths. Omit to split the width evenly.
     *
     * @param values one positive weight per column
     * @return this builder
     */
    public ColumnFlowBuilder weights(double... values) {
        this.weights.clear();
        if (values != null) {
            for (double value : values) {
                this.weights.add(value);
            }
        }
        return this;
    }

    /**
     * Sets the inner padding of the whole flow.
     *
     * @param value padding insets
     * @return this builder
     */
    public ColumnFlowBuilder padding(DocumentInsets value) {
        this.padding = value == null ? DocumentInsets.zero() : value;
        return this;
    }

    /**
     * Sets the outer margin of the whole flow.
     *
     * @param value margin insets
     * @return this builder
     */
    public ColumnFlowBuilder margin(DocumentInsets value) {
        this.margin = value == null ? DocumentInsets.zero() : value;
        return this;
    }

    /**
     * Appends a column configured through a nested section builder.
     *
     * @param spec column builder callback
     * @return this builder
     */
    public ColumnFlowBuilder addColumn(Consumer<SectionBuilder> spec) {
        columns.add(BuilderSupport.configure(new SectionBuilder(), spec).build());
        return this;
    }

    /**
     * Appends a named column configured through a nested section builder.
     *
     * @param name column name used in snapshots and layout-graph paths
     * @param spec column builder callback
     * @return this builder
     */
    public ColumnFlowBuilder addColumn(String name, Consumer<SectionBuilder> spec) {
        columns.add(BuilderSupport.configure(new SectionBuilder().name(name), spec).build());
        return this;
    }

    /**
     * Appends a pre-built column.
     *
     * @param column a vertical container — a section or a container
     * @return this builder
     * @throws IllegalArgumentException if the node is not a vertical container
     */
    public ColumnFlowBuilder addColumn(DocumentNode column) {
        if (!(column instanceof SectionNode) && !(column instanceof ContainerNode)) {
            throw new IllegalArgumentException(
                    "A column flow's children are columns, and a column is a vertical container "
                            + "(section or container) because that is what paginates. Received: "
                            + (column == null ? "null" : column.nodeKind())
                            + ". Wrap it in a column instead.");
        }
        columns.add(column);
        return this;
    }

    /**
     * Builds the immutable flow node.
     *
     * @return the assembled column flow
     */
    public ColumnFlowNode build() {
        return new ColumnFlowNode(name, List.copyOf(columns), List.copyOf(weights),
                gap, padding, margin);
    }
}
