package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.style.*;

import java.awt.*;

/**
 * Builder for rectangle-like semantic shape nodes.
 *
 * @author Artem Demchyshyn
 * @since 1.0.0
 */
public class ShapeBuilder implements Transformable<ShapeBuilder> {
    protected String name = "";
    protected double width;
    protected double height;
    protected DocumentColor fillColor;
    protected com.demcha.compose.document.style.DocumentPaint fillPaint;
    protected DocumentStroke stroke;
    protected DocumentCornerRadius cornerRadius = DocumentCornerRadius.ZERO;
    protected DocumentLinkOptions linkOptions;
    protected DocumentBookmarkOptions bookmarkOptions;
    protected DocumentInsets padding = DocumentInsets.zero();
    protected DocumentInsets margin = DocumentInsets.zero();
    protected DocumentTransform transform = DocumentTransform.NONE;

    /**
     * Creates a shape builder.
     */
    public ShapeBuilder() {
    }

    /**
     * Sets the shape node name.
     *
     * @param name name used in snapshots and layout graph paths
     * @return this builder
     */
    public ShapeBuilder name(String name) {
        this.name = name == null ? "" : name;
        return this;
    }

    /**
     * Sets the shape width.
     *
     * @param width width in points
     * @return this builder
     */
    public ShapeBuilder width(double width) {
        this.width = width;
        return this;
    }

    /**
     * Sets the shape height.
     *
     * @param height height in points
     * @return this builder
     */
    public ShapeBuilder height(double height) {
        this.height = height;
        return this;
    }

    /**
     * Sets shape width and height.
     *
     * @param width  width in points
     * @param height height in points
     * @return this builder
     */
    public ShapeBuilder size(double width, double height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * Sets shape fill color.
     *
     * @param fillColor fill color
     * @return this builder
     */
    public ShapeBuilder fillColor(Color fillColor) {
        this.fillColor = fillColor == null ? null : DocumentColor.of(fillColor);
        return this;
    }

    /**
     * Sets shape fill with a public canonical color.
     *
     * @param fillColor fill color
     * @return this builder
     */
    public ShapeBuilder fillColor(DocumentColor fillColor) {
        this.fillColor = fillColor;
        return this;
    }

    /**
     * Sets the shape fill with a {@link com.demcha.compose.document.style.DocumentPaint}
     * — a solid colour or a gradient. When set, the paint wins over
     * {@link #fillColor(DocumentColor)}; gradients render as native shadings in
     * the PDF backend.
     *
     * @param paint fill paint, or {@code null} to clear
     * @return this builder
     * @since 1.8.0
     */
    public ShapeBuilder fill(com.demcha.compose.document.style.DocumentPaint paint) {
        this.fillPaint = paint;
        return this;
    }

    /**
     * Sets shape stroke with the public canonical stroke value.
     *
     * @param stroke shape stroke, or {@code null} for no stroke
     * @return this builder
     */
    public ShapeBuilder stroke(DocumentStroke stroke) {
        this.stroke = stroke;
        return this;
    }

    /**
     * Sets the rectangle corner radius in points.
     *
     * @param radius corner radius in points
     * @return this builder
     */
    public ShapeBuilder cornerRadius(double radius) {
        return cornerRadius(DocumentCornerRadius.of(radius));
    }

    /**
     * Sets the rectangle corner radius with the public canonical value.
     *
     * @param cornerRadius corner radius, or {@code null} for square corners
     * @return this builder
     */
    public ShapeBuilder cornerRadius(DocumentCornerRadius cornerRadius) {
        this.cornerRadius = cornerRadius == null ? DocumentCornerRadius.ZERO : cornerRadius;
        return this;
    }

    /**
     * Attaches link metadata to the shape.
     *
     * @param linkOptions link metadata
     * @return this builder
     */
    public ShapeBuilder link(DocumentLinkOptions linkOptions) {
        this.linkOptions = linkOptions;
        return this;
    }

    /**
     * Attaches bookmark metadata to the shape.
     *
     * @param bookmarkOptions bookmark metadata
     * @return this builder
     */
    public ShapeBuilder bookmark(DocumentBookmarkOptions bookmarkOptions) {
        this.bookmarkOptions = bookmarkOptions;
        return this;
    }

    /**
     * Sets shape padding with the public canonical spacing value.
     *
     * @param padding padding in points
     * @return this builder
     */
    public ShapeBuilder padding(DocumentInsets padding) {
        this.padding = padding == null ? DocumentInsets.zero() : padding;
        return this;
    }

    /**
     * Sets shape margin with the public canonical spacing value.
     *
     * @param margin margin in points
     * @return this builder
     */
    public ShapeBuilder margin(DocumentInsets margin) {
        this.margin = margin == null ? DocumentInsets.zero() : margin;
        return this;
    }

    /**
     * Sets the render-time affine transform (rotation around the placement
     * centre and/or scaling). The {@link Transformable#rotate(double)},
     * {@link Transformable#scale(double)}, and
     * {@link Transformable#scale(double, double)} shortcuts delegate
     * through this setter.
     */
    @Override
    public ShapeBuilder transform(DocumentTransform transform) {
        this.transform = transform == null ? DocumentTransform.NONE : transform;
        return this;
    }

    @Override
    public DocumentTransform currentTransform() {
        return transform;
    }

    /**
     * Builds the semantic shape node.
     *
     * @return shape node
     */
    public ShapeNode build() {
        return new ShapeNode(name, width, height, fillColor, stroke, cornerRadius, linkOptions,
                bookmarkOptions, padding, margin, transform, fillPaint);
    }
}
