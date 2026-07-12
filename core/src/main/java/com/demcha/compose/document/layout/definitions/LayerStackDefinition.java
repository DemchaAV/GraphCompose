package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.*;
import com.demcha.compose.document.layout.payloads.PreparedStackLayout;
import com.demcha.compose.document.layout.payloads.ShapeClipBeginPayload;
import com.demcha.compose.document.layout.payloads.ShapeClipEndPayload;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.ShapeOutline;

import java.util.List;

import static com.demcha.compose.document.layout.NodeDefinitionSupport.EPS;
import static com.demcha.compose.document.layout.NodeDefinitionSupport.measureStack;
import static com.demcha.compose.document.layout.NodeDefinitionSupport.toPadding;

/**
 * Layout definition for {@link LayerStackNode}: an atomic stacked composite
 * whose child layers carry alignment, offset, and z-index metadata.
 *
 * @author Artem Demchyshyn
 */
public final class LayerStackDefinition implements NodeDefinition<LayerStackNode> {

    /**
     * Creates the layer-stack layout definition.
     */
    public LayerStackDefinition() {
    }

    @Override
    public Class<LayerStackNode> nodeType() {
        return LayerStackNode.class;
    }

    @Override
    public PreparedNode<LayerStackNode> prepare(LayerStackNode node, PrepareContext ctx, BoxConstraints constraints) {
        int n = node.layers().size();
        List<LayerAlign> alignments = new java.util.ArrayList<>(n);
        List<Double> offsetsX = new java.util.ArrayList<>(n);
        List<Double> offsetsY = new java.util.ArrayList<>(n);
        List<Integer> zIndices = new java.util.ArrayList<>(n);
        for (LayerStackNode.Layer layer : node.layers()) {
            alignments.add(layer.align());
            offsetsX.add(layer.offsetX());
            offsetsY.add(layer.offsetY());
            zIndices.add(layer.zIndex());
        }
        return PreparedNode.composite(
                node,
                measureStack(node, toPadding(node.padding()), ctx, constraints),
                new PreparedStackLayout(alignments, offsetsX, offsetsY, zIndices),
                new CompositeLayoutSpec(0.0, CompositeLayoutSpec.Axis.STACK));
    }

    @Override
    public PaginationPolicy paginationPolicy(LayerStackNode node) {
        return PaginationPolicy.ATOMIC;
    }

    @Override
    public List<DocumentNode> children(LayerStackNode node) {
        return node.children();
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<LayerStackNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        if (!emitsBoundsClip(prepared)) {
            return List.of();
        }
        LayerStackNode node = prepared.node();
        double width = prepared.measureResult().width() - node.padding().horizontal();
        double height = prepared.measureResult().height() - node.padding().vertical();
        // Open a bounds clip before the layers (the "before the body" half of
        // the paired marker; the matching restore is emitted in
        // emitOverlayFragments after the layers). This reuses the
        // ShapeContainer clip pipeline so the block icon honours SVG viewBox
        // semantics — off-canvas layer geometry is cut to the box, the
        // block-path mirror of the inline glyph-box clip in
        // PdfParagraphFragmentRenderHandler.
        return List.of(new LayoutFragment(
                placement.path(),
                0,
                node.padding().left(),
                node.padding().bottom(),
                width,
                height,
                new ShapeClipBeginPayload(
                        new ShapeOutline.Rectangle(width, height),
                        ClipPolicy.CLIP_BOUNDS,
                        placement.path())));
    }

    @Override
    public List<LayoutFragment> emitOverlayFragments(PreparedNode<LayerStackNode> prepared,
                                                     FragmentContext ctx,
                                                     FragmentPlacement placement) {
        if (!emitsBoundsClip(prepared)) {
            return List.of();
        }
        // Close the bounds clip after the layers — restores the graphics
        // state saved by the begin marker on the same page (the stack is
        // atomic, so begin and end never straddle a page break). Gated by the
        // same condition as the begin so the save/restore pair always balances.
        return List.of(new LayoutFragment(
                placement.path(),
                0,
                0.0,
                0.0,
                0.0,
                0.0,
                new ShapeClipEndPayload(placement.path())));
    }

    /**
     * Whether this stack emits a viewBox bounds-clip around its layers. True
     * only for an opted-in stack ({@code clipToBounds}) whose content box is
     * non-degenerate — the single source of truth shared by the begin marker
     * ({@link #emitFragments}) and the end marker ({@link #emitOverlayFragments})
     * so the graphics-state save/restore pair can never go unbalanced.
     */
    private static boolean emitsBoundsClip(PreparedNode<LayerStackNode> prepared) {
        LayerStackNode node = prepared.node();
        if (!node.clipToBounds()) {
            return false;
        }
        double width = prepared.measureResult().width() - node.padding().horizontal();
        double height = prepared.measureResult().height() - node.padding().vertical();
        return width > EPS && height > EPS;
    }
}
