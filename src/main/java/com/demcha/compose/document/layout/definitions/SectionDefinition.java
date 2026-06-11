package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.*;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.SectionNode;

import java.util.List;

import static com.demcha.compose.document.layout.NodeDefinitionSupport.*;

/**
 * Layout definition for {@link SectionNode}: a vertical atomic composite with
 * optional background and border decoration.
 *
 * @author Artem Demchyshyn
 */
public final class SectionDefinition implements NodeDefinition<SectionNode> {

    /**
     * Creates the section layout definition.
     */
    public SectionDefinition() {
    }

    @Override
    public Class<SectionNode> nodeType() {
        return SectionNode.class;
    }

    @Override
    public PreparedNode<SectionNode> prepare(SectionNode node, PrepareContext ctx, BoxConstraints constraints) {
        return PreparedNode.composite(
                node,
                measureComposite(node.children(), node.spacing(), toPadding(node.padding()), ctx, constraints),
                new CompositeLayoutSpec(node.spacing()));
    }

    @Override
    public PaginationPolicy paginationPolicy(SectionNode node) {
        return PaginationPolicy.ATOMIC;
    }

    @Override
    public List<DocumentNode> children(SectionNode node) {
        return node.children();
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<SectionNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        SectionNode node = prepared.node();
        return emitDecorationFragment(
                node.fillColor() == null ? null : node.fillColor().color(),
                toStroke(node.stroke()),
                node.cornerRadius(),
                toSideBorders(node.borders()),
                placement);
    }
}
