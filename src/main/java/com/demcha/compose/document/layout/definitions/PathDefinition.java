package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.*;
import com.demcha.compose.document.layout.payloads.PathFragmentPayload;
import com.demcha.compose.document.node.PathNode;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentStroke;

import java.awt.Color;
import java.util.List;

import static com.demcha.compose.document.layout.NodeDefinitionSupport.EPS;
import static com.demcha.compose.document.layout.NodeDefinitionSupport.toStroke;

/**
 * Layout definition for {@link PathNode}: a fixed-size atomic vector-path
 * fragment rendered through the path fragment pipeline with native curve
 * operators.
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public final class PathDefinition implements NodeDefinition<PathNode> {

    /**
     * Creates the path layout definition.
     */
    public PathDefinition() {
    }

    @Override
    public Class<PathNode> nodeType() {
        return PathNode.class;
    }

    @Override
    public PreparedNode<PathNode> prepare(PathNode node, PrepareContext ctx, BoxConstraints constraints) {
        return PreparedNode.leaf(node, new MeasureResult(
                node.width() + node.padding().horizontal(),
                node.height() + node.padding().vertical()));
    }

    @Override
    public PaginationPolicy paginationPolicy(PathNode node) {
        return PaginationPolicy.ATOMIC;
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<PathNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        PathNode node = prepared.node();
        double width = Math.max(0.0, placement.width() - node.padding().horizontal());
        double height = Math.max(0.0, placement.height() - node.padding().vertical());
        if (width <= EPS || height <= EPS) {
            return List.of();
        }
        // Solid paints normalise to flat colours so the render path (and its
        // byte output) is identical to plain fillColor/stroke; only true
        // gradients travel as paints. Mirrors ShapeDefinition.
        Color fill;
        DocumentPaint fillGradient = null;
        if (node.fillPaint() instanceof DocumentPaint.Solid solid) {
            fill = solid.color().color();
        } else if (node.fillPaint() != null) {
            fillGradient = node.fillPaint();
            fill = null;
        } else {
            fill = node.fillColor() == null ? null : node.fillColor().color();
        }
        DocumentStroke stroke = node.stroke();
        DocumentPaint strokeGradient = null;
        if (node.strokePaint() instanceof DocumentPaint.Solid solid) {
            stroke = DocumentStroke.of(solid.color(), stroke.width());
        } else if (node.strokePaint() != null) {
            strokeGradient = node.strokePaint();
        }
        return List.of(new LayoutFragment(
                placement.path(),
                0,
                node.padding().left(),
                node.padding().bottom(),
                width,
                height,
                new PathFragmentPayload(
                        node.segments(),
                        fill,
                        fillGradient,
                        toStroke(stroke),
                        strokeGradient,
                        null,
                        null,
                        node.dashPattern())));
    }
}
