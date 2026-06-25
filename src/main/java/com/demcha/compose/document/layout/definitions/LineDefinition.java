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
        // A horizontal fill line claims the width available where it is placed (its
        // row slot, or the content width) rather than its authored fixed width.
        double outerWidth = isFill(node)
                ? constraints.availableWidth()
                : node.width() + node.padding().horizontal();
        return PreparedNode.leaf(node, new MeasureResult(
                outerWidth,
                node.height() + node.padding().vertical()));
    }

    /**
     * Fill applies only to horizontal lines — stretching the end point of a
     * vertical or diagonal line would silently change its geometry, so fill is a
     * no-op there.
     */
    private static boolean isFill(LineNode node) {
        return node.fillWidth() && Math.abs(node.startY() - node.endY()) <= EPS;
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
        // A fill line is stretched to its resolved box, so it ends at the box's
        // right content edge instead of the authored endX.
        double endX = isFill(node) ? width : node.endX();
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
                        endX,
                        node.endY(),
                        node.linkTarget(),
                        node.bookmarkOptions(),
                        node.dashPattern(),
                        node.lineCap()));
        return withAnchorMarker(
                wrapAtomicWithTransform(leaf, placement, node.transform()),
                node.anchor(),
                placement);
    }
}
