package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.ShapeContainerNode;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.ShapeOutline;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for {@link ShapeContainerNode}.
 *
 * <p>Reads as: <em>"container is a [shape], inside it I'm composing layers"</em>.
 * The outline is mandatory — call {@link #rectangle(double, double)},
 * {@link #roundedRect(double, double, double)}, {@link #ellipse(double, double)},
 * or {@link #circle(double)} before {@link #build()}. Layers are appended in
 * source order (first behind, last in front) and each layer carries one of the
 * nine {@link LayerAlign} anchors plus optional on-screen offset.</p>
 *
 * @author Artem Demchyshyn
 */
public final class ShapeContainerBuilder {
    private String name = "";
    private ShapeOutline outline;
    private final List<LayerStackNode.Layer> layers = new ArrayList<>();
    // Default to CLIP_PATH per ADR §Decision — see ShapeContainerNode for
    // the rationale.
    private ClipPolicy clipPolicy = ClipPolicy.CLIP_PATH;
    private DocumentColor fillColor;
    private DocumentStroke stroke;
    private DocumentInsets padding = DocumentInsets.zero();
    private DocumentInsets margin = DocumentInsets.zero();

    /**
     * Creates a shape-container builder with no outline configured yet.
     */
    public ShapeContainerBuilder() {
    }

    /**
     * Sets the semantic node name.
     *
     * @param name name used in snapshots and layout graph paths
     * @return this builder
     */
    public ShapeContainerBuilder name(String name) {
        this.name = name == null ? "" : name;
        return this;
    }

    /**
     * Sets a plain rectangle outline.
     *
     * @param width outline width in points
     * @param height outline height in points
     * @return this builder
     */
    public ShapeContainerBuilder rectangle(double width, double height) {
        this.outline = new ShapeOutline.Rectangle(width, height);
        return this;
    }

    /**
     * Sets a rounded-rectangle outline.
     *
     * @param width outline width in points
     * @param height outline height in points
     * @param cornerRadius corner radius in points
     * @return this builder
     */
    public ShapeContainerBuilder roundedRect(double width, double height, double cornerRadius) {
        this.outline = new ShapeOutline.RoundedRectangle(width, height, cornerRadius);
        return this;
    }

    /**
     * Sets an ellipse outline.
     *
     * @param width outline width in points
     * @param height outline height in points
     * @return this builder
     */
    public ShapeContainerBuilder ellipse(double width, double height) {
        this.outline = new ShapeOutline.Ellipse(width, height);
        return this;
    }

    /**
     * Sets a circular outline (ellipse with equal width and height).
     *
     * @param diameter diameter in points
     * @return this builder
     */
    public ShapeContainerBuilder circle(double diameter) {
        this.outline = ShapeOutline.circle(diameter);
        return this;
    }

    /**
     * Replaces the outline with a pre-built {@link ShapeOutline} value.
     *
     * @param outline outline value
     * @return this builder
     */
    public ShapeContainerBuilder outline(ShapeOutline outline) {
        this.outline = Objects.requireNonNull(outline, "outline");
        return this;
    }

    /**
     * Sets the clipping policy applied to child layers.
     *
     * @param clipPolicy clip policy
     * @return this builder
     */
    public ShapeContainerBuilder clipPolicy(ClipPolicy clipPolicy) {
        this.clipPolicy = clipPolicy == null ? ClipPolicy.CLIP_PATH : clipPolicy;
        return this;
    }

    /**
     * Sets outline fill colour with a public canonical value.
     *
     * @param fillColor fill colour, or {@code null} for no fill
     * @return this builder
     */
    public ShapeContainerBuilder fillColor(DocumentColor fillColor) {
        this.fillColor = fillColor;
        return this;
    }

    /**
     * Sets outline fill colour from an AWT colour value.
     *
     * @param fillColor fill colour, or {@code null} for no fill
     * @return this builder
     */
    public ShapeContainerBuilder fillColor(Color fillColor) {
        this.fillColor = fillColor == null ? null : DocumentColor.of(fillColor);
        return this;
    }

    /**
     * Sets outline stroke.
     *
     * @param stroke stroke descriptor, or {@code null} for no stroke
     * @return this builder
     */
    public ShapeContainerBuilder stroke(DocumentStroke stroke) {
        this.stroke = stroke;
        return this;
    }

    /**
     * Sets uniform inner padding around all layers.
     *
     * @param padding padding in points
     * @return this builder
     */
    public ShapeContainerBuilder padding(double padding) {
        return padding(DocumentInsets.of(padding));
    }

    /**
     * Sets inner padding around all layers.
     *
     * @param padding padding insets
     * @return this builder
     */
    public ShapeContainerBuilder padding(DocumentInsets padding) {
        this.padding = padding == null ? DocumentInsets.zero() : padding;
        return this;
    }

    /**
     * Sets uniform outer margin around the container.
     *
     * @param margin margin in points
     * @return this builder
     */
    public ShapeContainerBuilder margin(double margin) {
        return margin(DocumentInsets.of(margin));
    }

    /**
     * Sets outer margin around the container.
     *
     * @param margin margin insets
     * @return this builder
     */
    public ShapeContainerBuilder margin(DocumentInsets margin) {
        this.margin = margin == null ? DocumentInsets.zero() : margin;
        return this;
    }

    /**
     * Appends a layer with explicit alignment.
     *
     * @param node child node
     * @param align anchor inside the inner box
     * @return this builder
     */
    public ShapeContainerBuilder layer(DocumentNode node, LayerAlign align) {
        layers.add(new LayerStackNode.Layer(Objects.requireNonNull(node, "node"), align));
        return this;
    }

    /**
     * Appends a top-left aligned layer.
     *
     * @param node child node
     * @return this builder
     */
    public ShapeContainerBuilder layer(DocumentNode node) {
        return layer(node, LayerAlign.TOP_LEFT);
    }

    /**
     * Appends a layer anchored to {@code align} and shifted by an on-screen
     * offset (positive {@code offsetX} = right, positive {@code offsetY} = down).
     *
     * @param node child node
     * @param offsetX horizontal offset in points
     * @param offsetY vertical offset in points
     * @param align anchor inside the inner box
     * @return this builder
     */
    public ShapeContainerBuilder position(DocumentNode node,
                                          double offsetX,
                                          double offsetY,
                                          LayerAlign align) {
        layers.add(new LayerStackNode.Layer(
                Objects.requireNonNull(node, "node"), align, offsetX, offsetY));
        return this;
    }

    /**
     * Appends a back layer (top-left aligned, drawn first / behind).
     *
     * @param node child node
     * @return this builder
     */
    public ShapeContainerBuilder back(DocumentNode node) {
        return layer(node, LayerAlign.TOP_LEFT);
    }

    // 9-point alignment shortcuts mirror LayerStackBuilder so the two builders
    // read the same way. Keeping the surface aligned helps autocomplete:
    // "I started with addCircle, the same vocabulary works."

    /** Anchors a layer to the top-left corner. */
    public ShapeContainerBuilder topLeft(DocumentNode node) {
        return layer(node, LayerAlign.TOP_LEFT);
    }

    /** Anchors a layer to the top edge, centred horizontally. */
    public ShapeContainerBuilder topCenter(DocumentNode node) {
        return layer(node, LayerAlign.TOP_CENTER);
    }

    /** Anchors a layer to the top-right corner. */
    public ShapeContainerBuilder topRight(DocumentNode node) {
        return layer(node, LayerAlign.TOP_RIGHT);
    }

    /** Anchors a layer to the left edge, centred vertically. */
    public ShapeContainerBuilder centerLeft(DocumentNode node) {
        return layer(node, LayerAlign.CENTER_LEFT);
    }

    /** Centred layer (the typical foreground content above an outline fill). */
    public ShapeContainerBuilder center(DocumentNode node) {
        return layer(node, LayerAlign.CENTER);
    }

    /** Anchors a layer to the right edge, centred vertically. */
    public ShapeContainerBuilder centerRight(DocumentNode node) {
        return layer(node, LayerAlign.CENTER_RIGHT);
    }

    /** Anchors a layer to the bottom-left corner. */
    public ShapeContainerBuilder bottomLeft(DocumentNode node) {
        return layer(node, LayerAlign.BOTTOM_LEFT);
    }

    /** Anchors a layer to the bottom edge, centred horizontally. */
    public ShapeContainerBuilder bottomCenter(DocumentNode node) {
        return layer(node, LayerAlign.BOTTOM_CENTER);
    }

    /** Anchors a layer to the bottom-right corner. */
    public ShapeContainerBuilder bottomRight(DocumentNode node) {
        return layer(node, LayerAlign.BOTTOM_RIGHT);
    }

    /**
     * Builds the immutable {@link ShapeContainerNode}.
     *
     * @return the configured container node
     * @throws IllegalStateException if no outline was configured
     */
    public ShapeContainerNode build() {
        if (outline == null) {
            throw new IllegalStateException(
                    "ShapeContainerBuilder '" + name + "' requires an outline; "
                            + "call rectangle/roundedRect/ellipse/circle before build().");
        }
        return new ShapeContainerNode(name, outline, layers, clipPolicy, fillColor, stroke, padding, margin);
    }
}
