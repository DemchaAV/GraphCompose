package com.demcha.compose.document.node;

import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Atomic composite node that places its children at explicit
 * {@code (x, y)} pixel coordinates inside a fixed-size bounding
 * box.
 *
 * <p>Use this when standard flow layout, alignment-based
 * {@link LayerStackNode}, or shape-as-container clipping cannot
 * express the placement an author wants — diploma seals,
 * pixel-perfect cover-page badges, custom diagrams, marketing
 * blocks where specific dots must land at specific coordinates.
 * For everything else prefer the regular flow / row / list /
 * stack primitives.</p>
 *
 * <p>Coordinates use the on-screen convention: {@code (0, 0)} is
 * the canvas's top-left corner, positive {@code x} is right, and
 * positive {@code y} is down. The canvas itself has a fixed
 * {@code width}/{@code height} (independent of its children) so
 * the surrounding flow can reserve a stable rectangle for it.</p>
 *
 * <p>Pagination is atomic — the entire canvas moves to the next
 * page when its measured height does not fit on the current
 * page. Children must therefore stay short enough to live on a
 * single page; oversized canvases throw at layout time.</p>
 *
 * @param name       canvas name used in snapshots and layout graph paths
 * @param width      canvas inner width in points (must be {@code > 0})
 * @param height     canvas inner height in points (must be {@code > 0})
 * @param placements per-child {@link CanvasChild} entries (node + position)
 * @param clipPolicy clipping mode applied to children that overflow
 *                   the canvas's bounding box
 * @param padding    inner padding between the canvas's outer rectangle
 *                   and the placement coordinate space
 * @param margin     outer margin around the canvas
 * @author Artem Demchyshyn
 */
public record CanvasLayerNode(
        String name,
        double width,
        double height,
        List<CanvasChild> placements,
        ClipPolicy clipPolicy,
        DocumentInsets padding,
        DocumentInsets margin
) implements DocumentNode {

    /**
     * Validates the canvas dimensions, copy-protects the placement
     * list, and normalizes nullable fields.
     */
    public CanvasLayerNode {
        name = name == null ? "" : name;
        if (width <= 0 || Double.isNaN(width) || Double.isInfinite(width)) {
            throw new IllegalArgumentException(
                    "CanvasLayerNode '" + name + "' width must be finite and positive: " + width);
        }
        if (height <= 0 || Double.isNaN(height) || Double.isInfinite(height)) {
            throw new IllegalArgumentException(
                    "CanvasLayerNode '" + name + "' height must be finite and positive: " + height);
        }
        Objects.requireNonNull(placements, "placements");
        List<CanvasChild> normalized = new ArrayList<>(placements.size());
        for (CanvasChild child : placements) {
            normalized.add(Objects.requireNonNull(child, "placement"));
        }
        placements = List.copyOf(normalized);
        clipPolicy = clipPolicy == null ? ClipPolicy.CLIP_BOUNDS : clipPolicy;
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
    }

    /**
     * Returns the placed children in source order. Override of
     * {@link DocumentNode#children()} so the layout framework can
     * walk the canvas's child sub-trees without reaching for the
     * {@code placements} accessor (which carries position metadata
     * the framework does not consume).
     *
     * @return ordered list of canvas children
     */
    @Override
    public List<DocumentNode> children() {
        List<DocumentNode> nodes = new ArrayList<>(placements.size());
        for (CanvasChild child : placements) {
            nodes.add(child.node());
        }
        return List.copyOf(nodes);
    }
}
