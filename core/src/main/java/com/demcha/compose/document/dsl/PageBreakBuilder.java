package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.style.DocumentInsets;

/**
 * Builder for explicit page-break control nodes.
 *
 * @since 1.0.0
 */
public final class PageBreakBuilder {
    private String name = "";
    private DocumentInsets margin = DocumentInsets.zero();

    /**
     * Creates a page-break builder.
     */
    public PageBreakBuilder() {
    }

    /**
     * Sets the page-break node name.
     *
     * @param name name used in snapshots and layout graph paths
     * @return this builder
     */
    public PageBreakBuilder name(String name) {
        this.name = name == null ? "" : name;
        return this;
    }

    /**
     * Sets page-break margin with the public canonical spacing value.
     *
     * @param margin margin in points
     * @return this builder
     */
    public PageBreakBuilder margin(DocumentInsets margin) {
        this.margin = margin == null ? DocumentInsets.zero() : margin;
        return this;
    }

    /**
     * Builds the semantic page-break node.
     *
     * @return page-break node
     */
    public PageBreakNode build() {
        return new PageBreakNode(name, margin);
    }
}
