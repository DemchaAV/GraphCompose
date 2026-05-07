package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.CanvasChild;
import com.demcha.compose.document.node.CanvasLayerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for the v1.6 {@link CanvasLayerNode} — places child
 * nodes at explicit {@code (x, y)} pixel coordinates inside a
 * fixed-size bounding box.
 *
 * <p>Coordinates use the on-screen convention: {@code (0, 0)}
 * is the canvas's top-left corner, positive {@code x} is right,
 * positive {@code y} is down.</p>
 *
 * @author Artem Demchyshyn
 */
public final class CanvasLayerBuilder {

    private String name = "";
    private double width;
    private double height;
    private final List<CanvasChild> placements = new ArrayList<>();
    private ClipPolicy clipPolicy = ClipPolicy.CLIP_BOUNDS;
    private DocumentInsets padding = DocumentInsets.zero();
    private DocumentInsets margin = DocumentInsets.zero();

    /**
     * Creates a canvas builder with explicit dimensions. The
     * canvas reserves a fixed {@code width × height} rectangle
     * regardless of where children are placed.
     *
     * @param width canvas width in points (must be {@code > 0})
     * @param height canvas height in points (must be {@code > 0})
     */
    public CanvasLayerBuilder(double width, double height) {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be > 0: " + width);
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be > 0: " + height);
        }
        this.width = width;
        this.height = height;
    }

    /**
     * Sets the canvas name used in snapshots and layout graph
     * paths.
     *
     * @param name canvas name
     * @return this builder
     */
    public CanvasLayerBuilder name(String name) {
        this.name = name == null ? "" : name;
        return this;
    }

    /**
     * Resizes the canvas's bounding box. Existing placements are
     * preserved at their original {@code (x, y)} but may now
     * overflow if {@code width} or {@code height} shrinks past
     * a child's coordinate.
     *
     * @param width canvas width in points (must be {@code > 0})
     * @param height canvas height in points (must be {@code > 0})
     * @return this builder
     */
    public CanvasLayerBuilder size(double width, double height) {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be > 0: " + width);
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be > 0: " + height);
        }
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * Places a child node at the given canvas-local coordinates.
     * The child's bounding-box top-left anchors at
     * {@code (x, y)} relative to the canvas's top-left origin.
     *
     * @param child child node to place
     * @param x offset from the canvas's left edge
     * @param y offset from the canvas's top edge (positive = down)
     * @return this builder
     */
    public CanvasLayerBuilder position(DocumentNode child, double x, double y) {
        Objects.requireNonNull(child, "child");
        placements.add(new CanvasChild(child, x, y));
        return this;
    }

    /**
     * Sets the clipping policy applied to children that overflow
     * the canvas's bounding box. Defaults to
     * {@link ClipPolicy#CLIP_BOUNDS}.
     *
     * @param clipPolicy clipping mode (must not be {@code null})
     * @return this builder
     */
    public CanvasLayerBuilder clipPolicy(ClipPolicy clipPolicy) {
        this.clipPolicy = clipPolicy == null ? ClipPolicy.CLIP_BOUNDS : clipPolicy;
        return this;
    }

    /**
     * Sets the canvas's inner padding.
     *
     * @param padding padding (defaults to zero when {@code null})
     * @return this builder
     */
    public CanvasLayerBuilder padding(DocumentInsets padding) {
        this.padding = padding == null ? DocumentInsets.zero() : padding;
        return this;
    }

    /**
     * Sets the canvas's outer margin.
     *
     * @param margin margin (defaults to zero when {@code null})
     * @return this builder
     */
    public CanvasLayerBuilder margin(DocumentInsets margin) {
        this.margin = margin == null ? DocumentInsets.zero() : margin;
        return this;
    }

    /**
     * Builds the canvas layer node from the current
     * configuration.
     *
     * @return canvas layer node
     */
    public CanvasLayerNode build() {
        return new CanvasLayerNode(
                name,
                width,
                height,
                List.copyOf(placements),
                clipPolicy,
                padding,
                margin);
    }
}
