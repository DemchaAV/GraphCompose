package com.demcha.compose.document.node;

import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.ShapeOutline;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Composite node whose bounding box is dictated by a {@link ShapeOutline}
 * (rectangle, rounded rectangle, or ellipse) and that hosts one or more child
 * layers positioned inside that outline.
 *
 * <p>The container differs from {@link LayerStackNode} in two ways. First,
 * the bounding box is derived from {@code outline.width()} /
 * {@code outline.height()} rather than from {@code max(child outer size)} —
 * so the outline is the authoritative size, not the children. Second, a
 * {@link ClipPolicy} declares whether children clip to the outline path,
 * to the bounding box, or are allowed to overflow.</p>
 *
 * <p>Children are reused from {@link LayerStackNode.Layer} so the same
 * alignment + on-screen offset semantics apply: the first layer is painted
 * behind, the last layer is painted in front, and each layer is anchored
 * inside the inner box (outline minus {@code padding}) by its
 * {@link LayerAlign} corner / edge plus its {@code offsetX} / {@code offsetY}.</p>
 *
 * <p>Pagination is atomic: the outline plus all of its layers stays on the
 * same page or moves to the next as one unit.</p>
 *
 * @param name node name used in snapshots and layout graph paths
 * @param outline geometric outline that drives the bounding box
 * @param layers child layers in back-to-front order; must contain at least one
 * @param clipPolicy how children are clipped relative to the outline
 * @param fillColor optional outline fill colour
 * @param stroke optional outline stroke
 * @param padding inner padding applied around all layers (subtracted from outline)
 * @param margin outer margin around the container
 *
 * @author Artem Demchyshyn
 */
public record ShapeContainerNode(
        String name,
        ShapeOutline outline,
        List<LayerStackNode.Layer> layers,
        ClipPolicy clipPolicy,
        DocumentColor fillColor,
        DocumentStroke stroke,
        DocumentInsets padding,
        DocumentInsets margin
) implements DocumentNode {

    /**
     * Normalizes optional values, copies the layer list defensively, and
     * validates that at least one layer exists.
     */
    public ShapeContainerNode {
        name = name == null ? "" : name;
        Objects.requireNonNull(outline, "outline");
        Objects.requireNonNull(layers, "layers");
        if (layers.isEmpty()) {
            throw new IllegalArgumentException(
                    "ShapeContainerNode '" + name + "' must have at least one layer.");
        }
        List<LayerStackNode.Layer> normalized = new ArrayList<>(layers.size());
        for (LayerStackNode.Layer layer : layers) {
            normalized.add(Objects.requireNonNull(layer, "layer"));
        }
        layers = List.copyOf(normalized);
        // Default to CLIP_PATH per ADR §Decision: a ShapeContainerNode is
        // *the* shape-with-children primitive, and the natural reading of
        // "add a circle with a label inside" is that the label is clipped
        // by the circle's outline. Callers who explicitly want
        // axis-aligned bbox clipping or no clipping at all set the policy
        // through the builder.
        clipPolicy = clipPolicy == null ? ClipPolicy.CLIP_PATH : clipPolicy;
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
    }

    /**
     * @return ordered child node references — equivalent to
     *         {@code layers.stream().map(Layer::node)}, kept aligned with
     *         {@link DocumentNode#children()}'s contract
     */
    @Override
    public List<DocumentNode> children() {
        List<DocumentNode> nodes = new ArrayList<>(layers.size());
        for (LayerStackNode.Layer layer : layers) {
            nodes.add(layer.node());
        }
        return List.copyOf(nodes);
    }
}
