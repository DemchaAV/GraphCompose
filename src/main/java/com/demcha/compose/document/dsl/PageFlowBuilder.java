package com.demcha.compose.document.dsl;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.ContainerNode;

/**
 * Builder for root page-flow containers that attach to a document session.
 *
 * @author Artem Demchyshyn
 * @since 1.0.0
 */
public final class PageFlowBuilder extends AbstractFlowBuilder<PageFlowBuilder, ContainerNode> {
    private final DocumentSession session;

    PageFlowBuilder(DocumentSession session) {
        this.session = session;
    }

    @Override
    protected PageFlowBuilder self() {
        return this;
    }

    @Override
    protected ContainerNode buildNode() {
        return new ContainerNode(name(), children(), spacing(), padding(), margin(), fillColor(), stroke(), cornerRadius(), borders());
    }

    /**
     * Builds the root flow and attaches it to the bound session.
     *
     * @return the built root node
     */
    public ContainerNode build() {
        ContainerNode root = buildNode();
        session.add(root);
        return root;
    }
}
