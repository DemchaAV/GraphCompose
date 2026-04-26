package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.LineNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;

/**
 * Builder for fixed-size semantic line nodes.
 *
 * @author Artem Demchyshyn
 */
public final class LineBuilder {
    private String name = "";
    private double width = 1.0;
    private double height = 1.0;
    private Double startX;
    private Double startY;
    private Double endX;
    private Double endY;
    private DocumentStroke stroke = DocumentStroke.of(DocumentColor.BLACK, 1.0);
    private DocumentLinkOptions linkOptions;
    private DocumentBookmarkOptions bookmarkOptions;
    private DocumentInsets padding = DocumentInsets.zero();
    private DocumentInsets margin = DocumentInsets.zero();

    /**
     * Creates a line builder.
     */
    public LineBuilder() {
    }

    /**
     * Sets the line node name.
     *
     * @param name name used in snapshots and layout graph paths
     * @return this builder
     */
    public LineBuilder name(String name) {
        this.name = name == null ? "" : name;
        return this;
    }

    /**
     * Sets the line box width.
     *
     * @param width width in points
     * @return this builder
     */
    public LineBuilder width(double width) {
        this.width = width;
        return this;
    }

    /**
     * Sets the line box height.
     *
     * @param height height in points
     * @return this builder
     */
    public LineBuilder height(double height) {
        this.height = height;
        return this;
    }

    /**
     * Sets the line box width and height.
     *
     * @param width width in points
     * @param height height in points
     * @return this builder
     */
    public LineBuilder size(double width, double height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * Configures a horizontal line.
     *
     * @param width line width in points
     * @return this builder
     */
    public LineBuilder horizontal(double width) {
        this.width = width;
        this.height = Math.max(1.0, stroke == null ? 1.0 : stroke.width());
        this.startX = 0.0;
        this.startY = this.height / 2.0;
        this.endX = width;
        this.endY = this.height / 2.0;
        return this;
    }

    /**
     * Configures a vertical line.
     *
     * @param height line height in points
     * @return this builder
     */
    public LineBuilder vertical(double height) {
        this.width = Math.max(1.0, stroke == null ? 1.0 : stroke.width());
        this.height = height;
        this.startX = this.width / 2.0;
        this.startY = 0.0;
        this.endX = this.width / 2.0;
        this.endY = height;
        return this;
    }

    /**
     * Configures a diagonal line across the line box.
     *
     * @param width line box width
     * @param height line box height
     * @return this builder
     */
    public LineBuilder diagonal(double width, double height) {
        this.width = width;
        this.height = height;
        this.startX = 0.0;
        this.startY = 0.0;
        this.endX = width;
        this.endY = height;
        return this;
    }

    /**
     * Sets the custom line start point inside the line box.
     *
     * @param x x offset in points
     * @param y y offset in points
     * @return this builder
     */
    public LineBuilder from(double x, double y) {
        this.startX = x;
        this.startY = y;
        return this;
    }

    /**
     * Sets the custom line end point inside the line box.
     *
     * @param x x offset in points
     * @param y y offset in points
     * @return this builder
     */
    public LineBuilder to(double x, double y) {
        this.endX = x;
        this.endY = y;
        return this;
    }

    /**
     * Sets line stroke.
     *
     * @param stroke stroke value
     * @return this builder
     */
    public LineBuilder stroke(DocumentStroke stroke) {
        this.stroke = stroke;
        return this;
    }

    /**
     * Sets line color while preserving the current stroke width.
     *
     * @param color stroke color
     * @return this builder
     */
    public LineBuilder color(DocumentColor color) {
        double width = stroke == null ? 1.0 : stroke.width();
        this.stroke = DocumentStroke.of(color, width);
        return this;
    }

    /**
     * Sets line thickness while preserving the current stroke color.
     *
     * @param thickness stroke width in points
     * @return this builder
     */
    public LineBuilder thickness(double thickness) {
        DocumentColor color = stroke == null ? DocumentColor.BLACK : stroke.color();
        this.stroke = DocumentStroke.of(color, thickness);
        if (isHorizontalLine()) {
            this.height = Math.max(this.height, thickness);
            this.startY = this.height / 2.0;
            this.endY = this.height / 2.0;
        } else if (isVerticalLine()) {
            this.width = Math.max(this.width, thickness);
            this.startX = this.width / 2.0;
            this.endX = this.width / 2.0;
        }
        return this;
    }

    /**
     * Attaches line-level link metadata.
     *
     * @param linkOptions link metadata
     * @return this builder
     */
    public LineBuilder link(DocumentLinkOptions linkOptions) {
        this.linkOptions = linkOptions;
        return this;
    }

    /**
     * Attaches line-level bookmark metadata.
     *
     * @param bookmarkOptions bookmark metadata
     * @return this builder
     */
    public LineBuilder bookmark(DocumentBookmarkOptions bookmarkOptions) {
        this.bookmarkOptions = bookmarkOptions;
        return this;
    }

    /**
     * Sets line padding.
     *
     * @param padding padding in points
     * @return this builder
     */
    public LineBuilder padding(DocumentInsets padding) {
        this.padding = padding == null ? DocumentInsets.zero() : padding;
        return this;
    }

    /**
     * Sets line margin.
     *
     * @param margin margin in points
     * @return this builder
     */
    public LineBuilder margin(DocumentInsets margin) {
        this.margin = margin == null ? DocumentInsets.zero() : margin;
        return this;
    }

    /**
     * Builds the line node.
     *
     * @return line node
     */
    public LineNode build() {
        double resolvedStartX = startX == null ? 0.0 : startX;
        double resolvedStartY = startY == null ? height / 2.0 : startY;
        double resolvedEndX = endX == null ? width : endX;
        double resolvedEndY = endY == null ? height / 2.0 : endY;
        return new LineNode(
                name,
                width,
                height,
                resolvedStartX,
                resolvedStartY,
                resolvedEndX,
                resolvedEndY,
                stroke,
                linkOptions,
                bookmarkOptions,
                padding,
                margin);
    }

    private boolean isHorizontalLine() {
        return startX != null
                && endX != null
                && startY != null
                && endY != null
                && Double.compare(startX, 0.0) == 0
                && Double.compare(endX, width) == 0
                && Double.compare(startY, endY) == 0;
    }

    private boolean isVerticalLine() {
        return startX != null
                && endX != null
                && startY != null
                && endY != null
                && Double.compare(startY, 0.0) == 0
                && Double.compare(endY, height) == 0
                && Double.compare(startX, endX) == 0;
    }
}
