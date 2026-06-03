package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.BoxConstraints;
import com.demcha.compose.document.layout.CompositeLayoutSpec;
import com.demcha.compose.document.layout.FragmentContext;
import com.demcha.compose.document.layout.FragmentPlacement;
import com.demcha.compose.document.layout.LayoutFragment;
import com.demcha.compose.document.layout.NodeDefinition;
import com.demcha.compose.document.layout.PaginationPolicy;
import com.demcha.compose.document.layout.PrepareContext;
import com.demcha.compose.document.layout.PreparedNode;
import com.demcha.compose.document.layout.payloads.PreparedStackLayout;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.LayerStackNode;

import java.util.List;

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
        return List.of();
    }
}
