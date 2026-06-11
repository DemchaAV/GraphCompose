package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.*;
import com.demcha.compose.document.layout.payloads.LineFragmentPayload;
import com.demcha.compose.document.node.LineNode;

import java.util.List;

import static com.demcha.compose.document.layout.NodeDefinitionSupport.*;

/**
 * Layout definition for {@link LineNode}: a fixed-size atomic line fragment
 * rendered inside the node's padding-adjusted box.
 *
 * @author Artem Demchyshyn
 */
public final class LineDefinition implements NodeDefinition<LineNode> {

    /**
     * Creates the line layout definition.
     */
    public LineDefinition() {
    }

    @Override
    public Class<LineNode> nodeType() {
        return LineNode.class;
    }

    @Override
    public PreparedNode<LineNode> prepare(LineNode node, PrepareContext ctx, BoxConstraints constraints) {
        return PreparedNode.leaf(node, new MeasureResult(
                node.width() + node.padding().horizontal(),
                node.height() + node.padding().vertical()));
    }

    @Override
    public PaginationPolicy paginationPolicy(LineNode node) {
        return PaginationPolicy.ATOMIC;
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<LineNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        LineNode node = prepared.node();
        double width = Math.max(0.0, placement.width() - node.padding().horizontal());
        double height = Math.max(0.0, placement.height() - node.padding().vertical());
        if (width <= EPS && height <= EPS) {
            return List.of();
        }
        LayoutFragment leaf = new LayoutFragment(
                placement.path(),
                0,
                node.padding().left(),
                node.padding().bottom(),
                width,
                height,
                new LineFragmentPayload(
                        toStroke(node.stroke()),
                        node.startX(),
                        node.startY(),
                        node.endX(),
                        node.endY(),
                        node.linkOptions(),
                        node.bookmarkOptions(),
                        node.dashPattern()));
        return wrapAtomicWithTransform(leaf, placement, node.transform());
    }
}
