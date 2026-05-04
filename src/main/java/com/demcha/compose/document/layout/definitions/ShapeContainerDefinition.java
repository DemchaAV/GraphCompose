package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.BoxConstraints;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.CompositeLayoutSpec;
import com.demcha.compose.document.layout.payloads.EllipseFragmentPayload;
import com.demcha.compose.document.layout.payloads.PreparedStackLayout;
import com.demcha.compose.document.layout.payloads.ShapeClipBeginPayload;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.layout.payloads.ShapeClipEndPayload;
import com.demcha.compose.document.layout.payloads.TransformBeginPayload;
import com.demcha.compose.document.layout.payloads.TransformEndPayload;
import com.demcha.compose.document.layout.FragmentContext;
import com.demcha.compose.document.layout.FragmentPlacement;
import com.demcha.compose.document.layout.LayoutFragment;
import com.demcha.compose.document.layout.MeasureResult;
import com.demcha.compose.document.layout.NodeDefinition;
import com.demcha.compose.document.layout.PaginationPolicy;
import com.demcha.compose.document.layout.PrepareContext;
import com.demcha.compose.document.layout.PreparedNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.ShapeContainerNode;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.style.Padding;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.layout.NodeDefinitionSupport.EPS;
import static com.demcha.compose.document.layout.NodeDefinitionSupport.toPadding;
import static com.demcha.compose.document.layout.NodeDefinitionSupport.toStroke;

/**
 * Layout definition for {@link ShapeContainerNode}: an atomic shape-backed
 * stack whose layers may be clipped and transformed as a single composite.
 *
 * @author Artem Demchyshyn
 */
public final class ShapeContainerDefinition implements NodeDefinition<ShapeContainerNode> {
    @Override
    public Class<ShapeContainerNode> nodeType() {
        return ShapeContainerNode.class;
    }

    @Override
    public PreparedNode<ShapeContainerNode> prepare(ShapeContainerNode node,
                                                    PrepareContext ctx,
                                                    BoxConstraints constraints) {
        ShapeOutline outline = node.outline();
        Padding padding = toPadding(node.padding());
        double measureWidth = outline.width() + padding.horizontal();
        double measureHeight = outline.height() + padding.vertical();
        double innerWidthForChildren = outline.width();
        for (LayerStackNode.Layer layer : node.layers()) {
            DocumentNode child = layer.node();
            double childInner = Math.max(0.0, innerWidthForChildren - child.margin().horizontal());
            ctx.prepare(child, BoxConstraints.natural(childInner));
        }

        int n = node.layers().size();
        List<LayerAlign> alignments = new ArrayList<>(n);
        List<Double> offsetsX = new ArrayList<>(n);
        List<Double> offsetsY = new ArrayList<>(n);
        List<Integer> zIndices = new ArrayList<>(n);
        for (LayerStackNode.Layer layer : node.layers()) {
            alignments.add(layer.align());
            offsetsX.add(layer.offsetX());
            offsetsY.add(layer.offsetY());
            zIndices.add(layer.zIndex());
        }

        return PreparedNode.composite(
                node,
                new MeasureResult(measureWidth, measureHeight),
                new PreparedStackLayout(alignments, offsetsX, offsetsY, zIndices),
                new CompositeLayoutSpec(0.0, CompositeLayoutSpec.Axis.STACK));
    }

    @Override
    public PaginationPolicy paginationPolicy(ShapeContainerNode node) {
        return PaginationPolicy.SHAPE_ATOMIC;
    }

    @Override
    public List<DocumentNode> children(ShapeContainerNode node) {
        return node.children();
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<ShapeContainerNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        ShapeContainerNode node = prepared.node();
        ShapeOutline outline = node.outline();
        double padLeft = node.padding().left();
        double padBottom = node.padding().bottom();
        double width = outline.width();
        double height = outline.height();
        if (width <= EPS || height <= EPS) {
            return List.of();
        }
        Color awtFill = node.fillColor() == null ? null : node.fillColor().color();
        Stroke stroke = toStroke(node.stroke());
        LayoutFragment outlineFragment = switch (outline) {
            case ShapeOutline.Ellipse ignored -> new LayoutFragment(
                    placement.path(),
                    0,
                    padLeft,
                    padBottom,
                    width,
                    height,
                    new EllipseFragmentPayload(awtFill, stroke, null, null));
            case ShapeOutline.Rectangle ignored -> new LayoutFragment(
                    placement.path(),
                    0,
                    padLeft,
                    padBottom,
                    width,
                    height,
                    new ShapeFragmentPayload(awtFill, stroke, 0.0, null, null, null));
            case ShapeOutline.RoundedRectangle r -> new LayoutFragment(
                    placement.path(),
                    0,
                    padLeft,
                    padBottom,
                    width,
                    height,
                    new ShapeFragmentPayload(awtFill, stroke, r.cornerRadius(), null, null, null));
        };

        List<LayoutFragment> opening = new ArrayList<>(4);
        boolean hasTransform = !node.transform().isIdentity();
        if (hasTransform) {
            opening.add(new LayoutFragment(
                    placement.path(),
                    0,
                    padLeft,
                    padBottom,
                    width,
                    height,
                    new TransformBeginPayload(node.transform(), placement.path())));
        }
        opening.add(outlineFragment);
        if (node.clipPolicy() != ClipPolicy.OVERFLOW_VISIBLE) {
            opening.add(new LayoutFragment(
                    placement.path(),
                    1,
                    padLeft,
                    padBottom,
                    width,
                    height,
                    new ShapeClipBeginPayload(outline, node.clipPolicy(), placement.path())));
        }
        return List.copyOf(opening);
    }

    @Override
    public List<LayoutFragment> emitOverlayFragments(PreparedNode<ShapeContainerNode> prepared,
                                                     FragmentContext ctx,
                                                     FragmentPlacement placement) {
        ShapeContainerNode node = prepared.node();
        boolean hasClip = node.clipPolicy() != ClipPolicy.OVERFLOW_VISIBLE;
        boolean hasTransform = !node.transform().isIdentity();
        if (!hasClip && !hasTransform) {
            return List.of();
        }
        List<LayoutFragment> closing = new ArrayList<>(2);
        if (hasClip) {
            closing.add(new LayoutFragment(
                    placement.path(),
                    2,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    new ShapeClipEndPayload(placement.path())));
        }
        if (hasTransform) {
            closing.add(new LayoutFragment(
                    placement.path(),
                    3,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    new TransformEndPayload(placement.path())));
        }
        return List.copyOf(closing);
    }
}
