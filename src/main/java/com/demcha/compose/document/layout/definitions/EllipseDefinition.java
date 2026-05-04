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
import com.demcha.compose.document.layout.payloads.EllipseFragmentPayload;
import com.demcha.compose.document.node.EllipseNode;

import java.util.List;

import static com.demcha.compose.document.layout.NodeDefinitionSupport.EPS;
import static com.demcha.compose.document.layout.NodeDefinitionSupport.toStroke;
import static com.demcha.compose.document.layout.NodeDefinitionSupport.wrapAtomicWithTransform;

/**
 * Layout definition for {@link EllipseNode}: a fixed-size atomic ellipse or
 * circle fragment.
 *
 * @author Artem Demchyshyn
 */
public final class EllipseDefinition implements NodeDefinition<EllipseNode> {
    @Override
    public Class<EllipseNode> nodeType() {
        return EllipseNode.class;
    }

    @Override
    public PreparedNode<EllipseNode> prepare(EllipseNode node, PrepareContext ctx, BoxConstraints constraints) {
        return PreparedNode.leaf(node, new MeasureResult(
                node.width() + node.padding().horizontal(),
                node.height() + node.padding().vertical()));
    }

    @Override
    public PaginationPolicy paginationPolicy(EllipseNode node) {
        return PaginationPolicy.ATOMIC;
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<EllipseNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        EllipseNode node = prepared.node();
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
                new EllipseFragmentPayload(
                        node.fillColor() == null ? null : node.fillColor().color(),
                        toStroke(node.stroke()),
                        node.linkOptions(),
                        node.bookmarkOptions()));
        return wrapAtomicWithTransform(leaf, placement, node.transform());
    }
}
