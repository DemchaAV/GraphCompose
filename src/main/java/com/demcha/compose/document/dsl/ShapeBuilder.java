package com.demcha.compose.document.dsl;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.internal.BuilderSupport;
import com.demcha.compose.document.dsl.internal.SemanticNameNormalizer;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.BarcodeNode;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentBarcodeOptions;
import com.demcha.compose.document.node.DocumentBarcodeType;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;

import java.awt.Color;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Builder for rectangle-like semantic shape nodes.
 */
public class ShapeBuilder {
    protected String name = "";
    protected double width;
    protected double height;
    protected DocumentColor fillColor;
    protected DocumentStroke stroke;
    protected DocumentLinkOptions linkOptions;
    protected DocumentBookmarkOptions bookmarkOptions;
    protected DocumentInsets padding = DocumentInsets.zero();
    protected DocumentInsets margin = DocumentInsets.zero();

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
     * @param width width in points
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
     * Builds the semantic shape node.
     *
     * @return shape node
     */
    public ShapeNode build() {
        return new ShapeNode(name, width, height, fillColor, stroke, linkOptions, bookmarkOptions, padding, margin);
    }
}

/**
 * Builder for semantic barcode and QR-code nodes.
 */
