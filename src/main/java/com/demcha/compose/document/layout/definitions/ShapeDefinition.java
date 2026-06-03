package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.BoxConstraints;
import com.demcha.compose.document.layout.FragmentContext;
import com.demcha.compose.document.layout.FragmentPlacement;
import com.demcha.compose.document.layout.LayoutFragment;
import com.demcha.compose.document.layout.MeasureResult;
import com.demcha.compose.document.layout.NodeDefinition;
import com.demcha.compose.document.layout.PaginationPolicy;
import com.demcha.compose.document.layout.PrepareContext;
import com.demcha.compose.document.layout.PreparedNode;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.node.ShapeNode;

import java.util.List;

import static com.demcha.compose.document.layout.NodeDefinitionSupport.EPS;
import static com.demcha.compose.document.layout.NodeDefinitionSupport.toStroke;
import static com.demcha.compose.document.layout.NodeDefinitionSupport.wrapAtomicWithTransform;

/**
 * Layout definition for {@link ShapeNode}: a fixed-size atomic rectangle shape
 * whose visual fragment carries fill, stroke, corner radius, and semantic PDF
 * metadata.
 *
 * @author Artem Demchyshyn
 */
public final class ShapeDefinition implements NodeDefinition<ShapeNode> {

    /**
     * Creates the shape layout definition.
     */
    public ShapeDefinition() {
    }

    @Override
    public Class<ShapeNode> nodeType() {
        return ShapeNode.class;
    }

    @Override
    public PreparedNode<ShapeNode> prepare(ShapeNode node, PrepareContext ctx, BoxConstraints constraints) {
        return PreparedNode.leaf(node, new MeasureResult(
                node.width() + node.padding().horizontal(),
                node.height() + node.padding().vertical()));
    }

    @Override
    public PaginationPolicy paginationPolicy(ShapeNode node) {
        return PaginationPolicy.ATOMIC;
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<ShapeNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        ShapeNode node = prepared.node();
        double width = Math.max(0.0, placement.width() - node.padding().horizontal());
        double height = Math.max(0.0, placement.height() - node.padding().vertical());
        if (width <= EPS || height <= EPS) {
            return List.of();
        }
        LayoutFragment leaf = new LayoutFragment(
                placement.path(),
                0,
                node.padding().left(),
                node.padding().bottom(),
                width,
                height,
                new ShapeFragmentPayload(
                        node.fillColor() == null ? null : node.fillColor().color(),
                        toStroke(node.stroke()),
                        node.cornerRadius(),
                        node.linkOptions(),
                        node.bookmarkOptions(),
                        null));
        return wrapAtomicWithTransform(leaf, placement, node.transform());
    }
}
