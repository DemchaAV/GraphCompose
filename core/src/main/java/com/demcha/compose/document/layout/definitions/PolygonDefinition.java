package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.*;
import com.demcha.compose.document.layout.payloads.PolygonFragmentPayload;
import com.demcha.compose.document.node.PolygonNode;

import java.util.List;

import static com.demcha.compose.document.layout.NodeDefinitionSupport.EPS;
import static com.demcha.compose.document.layout.NodeDefinitionSupport.toStroke;

/**
 * Layout definition for {@link PolygonNode}: a fixed-size atomic polygon
 * fragment rendered through the existing polygon fragment pipeline.
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public final class PolygonDefinition implements NodeDefinition<PolygonNode> {

    /**
     * Creates the polygon layout definition.
     */
    public PolygonDefinition() {
    }

    @Override
    public Class<PolygonNode> nodeType() {
        return PolygonNode.class;
    }

    @Override
    public PreparedNode<PolygonNode> prepare(PolygonNode node, PrepareContext ctx, BoxConstraints constraints) {
        return PreparedNode.leaf(node, new MeasureResult(
                node.width() + node.padding().horizontal(),
                node.height() + node.padding().vertical()));
    }

    @Override
    public PaginationPolicy paginationPolicy(PolygonNode node) {
        return PaginationPolicy.ATOMIC;
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<PolygonNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        PolygonNode node = prepared.node();
        double width = Math.max(0.0, placement.width() - node.padding().horizontal());
        double height = Math.max(0.0, placement.height() - node.padding().vertical());
        if (width <= EPS || height <= EPS) {
            return List.of();
        }
        return List.of(new LayoutFragment(
                placement.path(),
                0,
                node.padding().left(),
                node.padding().bottom(),
                width,
                height,
                new PolygonFragmentPayload(
                        node.points(),
                        node.fillColor() == null ? null : node.fillColor().color(),
                        toStroke(node.stroke()),
                        null,
                        null)));
    }
}
