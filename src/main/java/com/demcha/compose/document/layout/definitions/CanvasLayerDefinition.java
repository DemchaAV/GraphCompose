package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.*;
import com.demcha.compose.document.layout.payloads.PreparedStackLayout;
import com.demcha.compose.document.node.CanvasChild;
import com.demcha.compose.document.node.CanvasLayerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout definition for {@link CanvasLayerNode}: an atomic
 * composite that places children at explicit {@code (x, y)}
 * pixel coordinates inside a fixed-size bounding box.
 *
 * <p>The definition reuses the existing
 * {@link PreparedStackLayout} payload by anchoring every child
 * at {@link LayerAlign#TOP_LEFT} and converting the canvas
 * child's {@code (x, y)} into the stack layout's
 * {@code offsetX} / {@code offsetY}. Stack offsets follow the
 * on-screen convention (positive {@code y} = down), which
 * matches the canvas's coordinate system one-to-one — no
 * conversion arithmetic needed. The framework's STACK
 * placement code path positions each child accordingly.</p>
 *
 * <p>Pagination is atomic: when the canvas's measured height
 * doesn't fit on the current page, the framework moves the
 * whole canvas to the next page.</p>
 *
 * @author Artem Demchyshyn
 */
public final class CanvasLayerDefinition implements NodeDefinition<CanvasLayerNode> {

    /**
     * Creates the canvas-layer layout definition.
     */
    public CanvasLayerDefinition() {
    }

    @Override
    public Class<CanvasLayerNode> nodeType() {
        return CanvasLayerNode.class;
    }

    @Override
    public PreparedNode<CanvasLayerNode> prepare(CanvasLayerNode node,
                                                 PrepareContext ctx,
                                                 BoxConstraints constraints) {
        int n = node.placements().size();
        List<LayerAlign> alignments = new ArrayList<>(n);
        List<Double> offsetsX = new ArrayList<>(n);
        List<Double> offsetsY = new ArrayList<>(n);
        List<Integer> zIndices = new ArrayList<>(n);
        for (CanvasChild child : node.placements()) {
            alignments.add(LayerAlign.TOP_LEFT);
            offsetsX.add(child.x());
            offsetsY.add(child.y());
            // zIndex 0 — source order is the rendering order. Authors
            // who need explicit z-stacking inside a canvas should
            // wrap the canvas in a LayerStackNode with explicit
            // zIndex per layer.
            zIndices.add(0);
        }
        // Canvas dimensions are explicit and independent of children's
        // measures: the canvas reserves a fixed (width, height) box in
        // the surrounding flow regardless of where children are placed.
        double outerWidth = node.width() + node.padding().horizontal();
        double outerHeight = node.height() + node.padding().vertical();
        return PreparedNode.composite(
                node,
                new MeasureResult(outerWidth, outerHeight),
                new PreparedStackLayout(alignments, offsetsX, offsetsY, zIndices),
                new CompositeLayoutSpec(0.0, CompositeLayoutSpec.Axis.STACK));
    }

    @Override
    public PaginationPolicy paginationPolicy(CanvasLayerNode node) {
        return PaginationPolicy.ATOMIC;
    }

    @Override
    public List<DocumentNode> children(CanvasLayerNode node) {
        return node.children();
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<CanvasLayerNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        // The canvas itself emits no body fragment — children are
        // placed by the framework via PreparedStackLayout, exactly
        // as LayerStackNode does. Decoration fragments (background
        // fill, border, clip) are emitted via the standard
        // composite-decoration overlay path when the canvas's
        // ClipPolicy or styling needs them.
        return List.of();
    }
}
