package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.SectionNode;

/**
 * Builder for semantic sections inside document flows.
 *
 * @author Artem Demchyshyn
 * @since 1.0.0
 */
public final class SectionBuilder extends AbstractFlowBuilder<SectionBuilder, SectionNode> {
    private boolean keepTogether = false;

    /**
     * Creates a section builder.
     */
    public SectionBuilder() {
    }

    @Override
    protected SectionBuilder self() {
        return this;
    }

    /**
     * Keeps the whole section on one page: when it does not fit in the remaining
     * page space but fits on a fresh page, it relocates whole to the next page
     * instead of orphaning its leading children (e.g. a heading) from the content
     * below. Sections taller than a page still flow.
     *
     * @return this builder
     * @since 1.8.0
     */
    public SectionBuilder keepTogether() {
        this.keepTogether = true;
        return this;
    }

    /**
     * Sets whether the section keeps together on one page.
     *
     * @param value true to keep the section whole
     * @return this builder
     * @since 1.8.0
     */
    public SectionBuilder keepTogether(boolean value) {
        this.keepTogether = value;
        return this;
    }

    @Override
    protected SectionNode buildNode() {
        return new SectionNode(name(), children(), spacing(), padding(), margin(), fillColor(),
                stroke(), cornerRadius(), borders(), keepTogether);
    }

    /**
     * Builds the detached section node.
     *
     * @return the built section node
     */
    public SectionNode build() {
        return buildNode();
    }
}
