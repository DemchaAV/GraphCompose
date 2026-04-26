package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;

import java.awt.Color;

/**
 * Builder for semantic circle and ellipse nodes.
 *
 * @author Artem Demchyshyn
 */
public final class EllipseBuilder {
    private String name = "";
    private double width;
    private double height;
    private DocumentColor fillColor;
    private DocumentStroke stroke;
    private DocumentLinkOptions linkOptions;
    private DocumentBookmarkOptions bookmarkOptions;
    private DocumentInsets padding = DocumentInsets.zero();
    private DocumentInsets margin = DocumentInsets.zero();

    /**
     * Creates an ellipse builder.
     */
    public EllipseBuilder() {
    }

    /**
     * Sets the ellipse node name.
     *
     * @param name name used in snapshots and layout graph paths
     * @return this builder
     */
    public EllipseBuilder name(String name) {
        this.name = name == null ? "" : name;
        return this;
    }

    /**
     * Sets ellipse width.
     *
     * @param width width in points
     * @return this builder
     */
    public EllipseBuilder width(double width) {
        this.width = width;
        return this;
    }

    /**
     * Sets ellipse height.
     *
     * @param height height in points
     * @return this builder
     */
    public EllipseBuilder height(double height) {
        this.height = height;
        return this;
    }

    /**
     * Sets ellipse width and height.
     *
     * @param width width in points
     * @param height height in points
     * @return this builder
     */
    public EllipseBuilder size(double width, double height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * Configures a circle with equal width and height.
     *
     * @param diameter circle diameter in points
     * @return this builder
     */
    public EllipseBuilder circle(double diameter) {
        this.width = diameter;
        this.height = diameter;
        return this;
    }

    /**
     * Sets ellipse fill color.
     *
     * @param fillColor fill color
     * @return this builder
     */
    public EllipseBuilder fillColor(Color fillColor) {
        this.fillColor = fillColor == null ? null : DocumentColor.of(fillColor);
        return this;
    }

    /**
     * Sets ellipse fill color.
     *
     * @param fillColor fill color
     * @return this builder
     */
    public EllipseBuilder fillColor(DocumentColor fillColor) {
        this.fillColor = fillColor;
        return this;
    }

    /**
     * Sets ellipse stroke.
     *
     * @param stroke stroke descriptor, or {@code null} for no stroke
     * @return this builder
     */
    public EllipseBuilder stroke(DocumentStroke stroke) {
        this.stroke = stroke;
        return this;
    }

    /**
     * Attaches ellipse-level link metadata.
     *
     * @param linkOptions link metadata
     * @return this builder
     */
    public EllipseBuilder link(DocumentLinkOptions linkOptions) {
        this.linkOptions = linkOptions;
        return this;
    }

    /**
     * Attaches ellipse-level bookmark metadata.
     *
     * @param bookmarkOptions bookmark metadata
     * @return this builder
     */
    public EllipseBuilder bookmark(DocumentBookmarkOptions bookmarkOptions) {
        this.bookmarkOptions = bookmarkOptions;
        return this;
    }

    /**
     * Sets ellipse padding.
     *
     * @param padding padding in points
     * @return this builder
     */
    public EllipseBuilder padding(DocumentInsets padding) {
        this.padding = padding == null ? DocumentInsets.zero() : padding;
        return this;
    }

    /**
     * Sets ellipse margin.
     *
     * @param margin margin in points
     * @return this builder
     */
    public EllipseBuilder margin(DocumentInsets margin) {
        this.margin = margin == null ? DocumentInsets.zero() : margin;
        return this;
    }

    /**
     * Builds the ellipse node.
     *
     * @return ellipse node
     */
    public EllipseNode build() {
        return new EllipseNode(name, width, height, fillColor, stroke, linkOptions, bookmarkOptions, padding, margin);
    }
}
