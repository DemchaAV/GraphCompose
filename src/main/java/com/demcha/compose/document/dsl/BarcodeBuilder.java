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
 * Builder for semantic barcode and QR-code nodes.
 */
public final class BarcodeBuilder {
    private String name = "";
    private String content = "";
    private DocumentBarcodeType type = DocumentBarcodeType.QR_CODE;
    private DocumentColor foreground = DocumentColor.BLACK;
    private DocumentColor background = DocumentColor.WHITE;
    private int quietZoneMargin = 0;
    private double width;
    private double height;
    private DocumentLinkOptions linkOptions;
    private DocumentBookmarkOptions bookmarkOptions;
    private DocumentInsets padding = DocumentInsets.zero();
    private DocumentInsets margin = DocumentInsets.zero();

    /**
     * Creates a barcode builder.
     */
    public BarcodeBuilder() {
    }

    /**
     * Sets the barcode node name.
     *
     * @param name name used in snapshots and layout graph paths
     * @return this builder
     */
    public BarcodeBuilder name(String name) {
        this.name = name == null ? "" : name;
        return this;
    }

    /**
     * Replaces the full barcode options object.
     *
     * @param options canonical barcode options
     * @return this builder
     */
    public BarcodeBuilder options(DocumentBarcodeOptions options) {
        DocumentBarcodeOptions safe = Objects.requireNonNull(options, "options");
        this.content = safe.getContent();
        this.type = safe.getType() == null ? DocumentBarcodeType.QR_CODE : safe.getType();
        this.foreground = safe.getForeground() == null ? DocumentColor.BLACK : safe.getForeground();
        this.background = safe.getBackground() == null ? DocumentColor.WHITE : safe.getBackground();
        this.quietZoneMargin = safe.getQuietZoneMargin();
        return this;
    }

    /**
     * Sets the barcode content.
     *
     * @param content encoded content
     * @return this builder
     */
    public BarcodeBuilder data(String content) {
        this.content = content == null ? "" : content;
        return this;
    }

    /**
     * Sets the barcode type.
     *
     * @param type barcode type
     * @return this builder
     */
    public BarcodeBuilder type(DocumentBarcodeType type) {
        this.type = type == null ? DocumentBarcodeType.QR_CODE : type;
        return this;
    }

    /**
     * Uses QR-code rendering.
     *
     * @return this builder
     */
    public BarcodeBuilder qrCode() {
        return type(DocumentBarcodeType.QR_CODE);
    }

    /**
     * Uses Code 128 barcode rendering.
     *
     * @return this builder
     */
    public BarcodeBuilder code128() {
        return type(DocumentBarcodeType.CODE_128);
    }

    /**
     * Uses Code 39 barcode rendering.
     *
     * @return this builder
     */
    public BarcodeBuilder code39() {
        return type(DocumentBarcodeType.CODE_39);
    }

    /**
     * Uses EAN-13 barcode rendering.
     *
     * @return this builder
     */
    public BarcodeBuilder ean13() {
        return type(DocumentBarcodeType.EAN_13);
    }

    /**
     * Uses EAN-8 barcode rendering.
     *
     * @return this builder
     */
    public BarcodeBuilder ean8() {
        return type(DocumentBarcodeType.EAN_8);
    }

    /**
     * Sets barcode foreground color.
     *
     * @param foreground foreground color
     * @return this builder
     */
    public BarcodeBuilder foreground(Color foreground) {
        return foreground(foreground == null ? null : DocumentColor.of(foreground));
    }

    /**
     * Sets barcode foreground with a public canonical color.
     *
     * @param foreground foreground color
     * @return this builder
     */
    public BarcodeBuilder foreground(DocumentColor foreground) {
        this.foreground = foreground == null ? DocumentColor.BLACK : foreground;
        return this;
    }

    /**
     * Sets barcode background color.
     *
     * @param background background color
     * @return this builder
     */
    public BarcodeBuilder background(Color background) {
        return background(background == null ? null : DocumentColor.of(background));
    }

    /**
     * Sets barcode background with a public canonical color.
     *
     * @param background background color
     * @return this builder
     */
    public BarcodeBuilder background(DocumentColor background) {
        this.background = background == null ? DocumentColor.WHITE : background;
        return this;
    }

    /**
     * Sets barcode quiet-zone margin.
     *
     * @param quietZoneMargin quiet-zone margin in barcode modules
     * @return this builder
     */
    public BarcodeBuilder quietZone(int quietZoneMargin) {
        this.quietZoneMargin = Math.max(0, quietZoneMargin);
        return this;
    }

    /**
     * Sets barcode width.
     *
     * @param width width in points
     * @return this builder
     */
    public BarcodeBuilder width(double width) {
        this.width = width;
        return this;
    }

    /**
     * Sets barcode height.
     *
     * @param height height in points
     * @return this builder
     */
    public BarcodeBuilder height(double height) {
        this.height = height;
        return this;
    }

    /**
     * Sets barcode width and height.
     *
     * @param width width in points
     * @param height height in points
     * @return this builder
     */
    public BarcodeBuilder size(double width, double height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * Attaches link metadata to the barcode.
     *
     * @param linkOptions link metadata
     * @return this builder
     */
    public BarcodeBuilder link(DocumentLinkOptions linkOptions) {
        this.linkOptions = linkOptions;
        return this;
    }

    /**
     * Attaches bookmark metadata to the barcode.
     *
     * @param bookmarkOptions bookmark metadata
     * @return this builder
     */
    public BarcodeBuilder bookmark(DocumentBookmarkOptions bookmarkOptions) {
        this.bookmarkOptions = bookmarkOptions;
        return this;
    }

    /**
     * Sets barcode padding with the public canonical spacing value.
     *
     * @param padding padding in points
     * @return this builder
     */
    public BarcodeBuilder padding(DocumentInsets padding) {
        this.padding = padding == null ? DocumentInsets.zero() : padding;
        return this;
    }

    /**
     * Sets barcode margin with the public canonical spacing value.
     *
     * @param margin margin in points
     * @return this builder
     */
    public BarcodeBuilder margin(DocumentInsets margin) {
        this.margin = margin == null ? DocumentInsets.zero() : margin;
        return this;
    }

    /**
     * Builds the semantic barcode node.
     *
     * @return barcode node
     */
    public BarcodeNode build() {
        DocumentBarcodeOptions options = DocumentBarcodeOptions.builder()
                .content(content)
                .type(type)
                .foreground(foreground)
                .background(background)
                .quietZoneMargin(quietZoneMargin)
                .build();
        return new BarcodeNode(name, options, width, height, linkOptions, bookmarkOptions, padding, margin);
    }
}

/**
 * Builder for thin horizontal dividers.
 */
