package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.*;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.node.ShapeNode;

import java.util.List;

import static com.demcha.compose.document.layout.NodeDefinitionSupport.*;

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
        // Solid paints normalise to a plain fill colour so the render path (and
        // its byte output) is identical to a fillColor; only true gradients
        // travel as fillPaint.
        com.demcha.compose.document.style.DocumentPaint paint = node.fillPaint();
        java.awt.Color fill;
        com.demcha.compose.document.style.DocumentPaint gradient = null;
        if (paint instanceof com.demcha.compose.document.style.DocumentPaint.Solid solid) {
            fill = solid.color().color();
        } else if (paint != null) {
            gradient = paint;
            fill = null;
        } else {
            fill = node.fillColor() == null ? null : node.fillColor().color();
        }
        LayoutFragment leaf = new LayoutFragment(
                placement.path(),
                0,
                node.padding().left(),
                node.padding().bottom(),
                width,
                height,
                new ShapeFragmentPayload(
                        fill,
                        toStroke(node.stroke()),
                        node.cornerRadius(),
                        node.linkOptions(),
                        node.bookmarkOptions(),
                        null,
                        gradient));
        return wrapAtomicWithTransform(leaf, placement, node.transform());
    }
}
