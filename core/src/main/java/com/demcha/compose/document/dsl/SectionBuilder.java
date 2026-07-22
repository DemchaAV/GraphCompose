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
    private boolean keepWithNext = false;

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

    /**
     * Keeps this section with the block that follows it: when the section plus the
     * first line of the next block would not fit in the remaining page space (but
     * fit on a fresh page), the section relocates to the next page rather than
     * stranding at a page bottom apart from the content it introduces. This is the
     * orphaned-heading fix for a boxed section title — it keeps the title with only
     * the first line of a long, page-spanning body, unlike {@link #keepTogether()}
     * which would try to keep the whole body together. The rule is inert when
     * nothing follows the section on the page.
     *
     * @return this builder
     * @since 2.0.0
     */
    public SectionBuilder keepWithNext() {
        this.keepWithNext = true;
        return this;
    }

    /**
     * Sets whether the section stays with the block that follows it.
     *
     * @param value true to keep the section with the first line of the next block
     * @return this builder
     * @since 2.0.0
     */
    public SectionBuilder keepWithNext(boolean value) {
        this.keepWithNext = value;
        return this;
    }

    @Override
    protected SectionNode buildNode() {
        return new SectionNode(name(), children(), spacing(), padding(), margin(), fillColor(),
                stroke(), cornerRadius(), borders(), keepTogether, anchor(), bleed(), bookmarkOptions(),
                keepWithNext);
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
