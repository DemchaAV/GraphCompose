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
 * Builder for semantic list nodes with marker and spacing controls.
 */
public final class ListBuilder {
    private String name = "";
    private final List<String> items = new ArrayList<>();
    private ListMarker marker = ListMarker.bullet();
    private DocumentTextStyle textStyle = DocumentTextStyle.DEFAULT;
    private TextAlign align = TextAlign.LEFT;
    private double lineSpacing = 0.0;
    private double itemSpacing = 0.0;
    private String continuationIndent = "";
    private boolean normalizeMarkers = true;
    private DocumentInsets padding = DocumentInsets.zero();
    private DocumentInsets margin = DocumentInsets.zero();

    /**
     * Creates a list builder.
     */
    public ListBuilder() {
    }

    /**
     * Sets the list node name.
     *
     * @param name name used in snapshots and layout graph paths
     * @return this builder
     */
    public ListBuilder name(String name) {
        this.name = name == null ? "" : name;
        return this;
    }

    /**
     * Replaces list items from varargs.
     *
     * @param items item texts
     * @return this builder
     */
    public ListBuilder items(String... items) {
        this.items.clear();
        if (items != null) {
            this.items.addAll(List.of(items));
        }
        return this;
    }

    /**
     * Replaces list items from a collection.
     *
     * @param items item texts
     * @return this builder
     */
    public ListBuilder items(List<String> items) {
        this.items.clear();
        if (items != null) {
            this.items.addAll(items);
        }
        return this;
    }

    /**
     * Appends one list item.
     *
     * @param item item text
     * @return this builder
     */
    public ListBuilder addItem(String item) {
        this.items.add(item);
        return this;
    }

    /**
     * Sets the list marker.
     *
     * @param marker list marker
     * @return this builder
     */
    public ListBuilder marker(ListMarker marker) {
        this.marker = marker == null ? ListMarker.bullet() : marker;
        return this;
    }

    /**
     * Sets a custom list marker.
     *
     * @param marker marker text
     * @return this builder
     */
    public ListBuilder marker(String marker) {
        return marker(ListMarker.custom(marker));
    }

    /**
     * Uses bullet markers.
     *
     * @return this builder
     */
    public ListBuilder bullet() {
        return marker(ListMarker.bullet());
    }

    /**
     * Uses dash markers.
     *
     * @return this builder
     */
    public ListBuilder dash() {
        return marker(ListMarker.dash());
    }

    /**
     * Uses markerless rows.
     *
     * @return this builder
     */
    public ListBuilder noMarker() {
        return marker(ListMarker.none());
    }

    /**
     * Sets list text style with the public canonical style value.
     *
     * @param textStyle list text style
     * @return this builder
     */
    public ListBuilder textStyle(DocumentTextStyle textStyle) {
        this.textStyle = textStyle == null ? DocumentTextStyle.DEFAULT : textStyle;
        return this;
    }

    /**
     * Sets list item alignment.
     *
     * @param align item text alignment
     * @return this builder
     */
    public ListBuilder align(TextAlign align) {
        this.align = align == null ? TextAlign.LEFT : align;
        return this;
    }

    /**
     * Sets spacing between wrapped lines within one item.
     *
     * @param lineSpacing line spacing in points
     * @return this builder
     */
    public ListBuilder lineSpacing(double lineSpacing) {
        this.lineSpacing = lineSpacing;
        return this;
    }

    /**
     * Sets spacing between list items.
     *
     * @param itemSpacing item spacing in points
     * @return this builder
     */
    public ListBuilder itemSpacing(double itemSpacing) {
        this.itemSpacing = itemSpacing;
        return this;
    }

    /**
     * Sets the prefix used only for wrapped continuation lines when the list
     * has no visible marker.
     *
     * @param continuationIndent continuation-line prefix, often a few spaces
     * @return this builder
     */
    public ListBuilder continuationIndent(String continuationIndent) {
        this.continuationIndent = continuationIndent == null ? "" : continuationIndent;
        return this;
    }

    /**
     * Sets whether leading raw markers should be stripped from input items.
     *
     * @param normalizeMarkers whether input markers are normalized
     * @return this builder
     */
    public ListBuilder normalizeMarkers(boolean normalizeMarkers) {
        this.normalizeMarkers = normalizeMarkers;
        return this;
    }

    /**
     * Sets list padding with the public canonical spacing value.
     *
     * @param padding padding in points
     * @return this builder
     */
    public ListBuilder padding(DocumentInsets padding) {
        this.padding = padding == null ? DocumentInsets.zero() : padding;
        return this;
    }

    /**
     * Sets list padding from explicit side values.
     *
     * @param top top padding
     * @param right right padding
     * @param bottom bottom padding
     * @param left left padding
     * @return this builder
     */
    public ListBuilder padding(float top, float right, float bottom, float left) {
        return padding(new DocumentInsets(top, right, bottom, left));
    }

    /**
     * Sets list margin with the public canonical spacing value.
     *
     * @param margin margin in points
     * @return this builder
     */
    public ListBuilder margin(DocumentInsets margin) {
        this.margin = margin == null ? DocumentInsets.zero() : margin;
        return this;
    }

    /**
     * Sets list margin from explicit side values.
     *
     * @param top top margin
     * @param right right margin
     * @param bottom bottom margin
     * @param left left margin
     * @return this builder
     */
    public ListBuilder margin(float top, float right, float bottom, float left) {
        return margin(new DocumentInsets(top, right, bottom, left));
    }

    /**
     * Builds the semantic list node.
     *
     * @return list node
     */
    public ListNode build() {
        return new ListNode(
                name,
                List.copyOf(items),
                marker,
                textStyle,
                align,
                lineSpacing,
                itemSpacing,
                continuationIndent,
                normalizeMarkers,
                padding,
                margin);
    }
}

/**
 * Builder for semantic images.
 */
