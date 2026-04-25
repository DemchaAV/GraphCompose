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
 * Builder for semantic image nodes.
 */
public final class ImageBuilder {
    private String name = "";
    private DocumentImageData imageData;
    private Double width;
    private Double height;
    private DocumentLinkOptions linkOptions;
    private DocumentBookmarkOptions bookmarkOptions;
    private DocumentInsets padding = DocumentInsets.zero();
    private DocumentInsets margin = DocumentInsets.zero();

    /**
     * Creates an image builder.
     */
    public ImageBuilder() {
    }

    /**
     * Sets the image node name.
     *
     * @param name name used in snapshots and layout graph paths
     * @return this builder
     */
    public ImageBuilder name(String name) {
        this.name = name == null ? "" : name;
        return this;
    }

    /**
     * Sets image source data.
     *
     * @param imageData image data
     * @return this builder
     */
    public ImageBuilder source(DocumentImageData imageData) {
        this.imageData = Objects.requireNonNull(imageData, "imageData");
        return this;
    }

    /**
     * Sets image source from an in-memory byte array.
     *
     * @param bytes image bytes
     * @return this builder
     */
    public ImageBuilder source(byte[] bytes) {
        return source(DocumentImageData.fromBytes(bytes));
    }

    /**
     * Sets image source from a filesystem path.
     *
     * @param path image path
     * @return this builder
     */
    public ImageBuilder source(Path path) {
        return source(DocumentImageData.fromPath(path));
    }

    /**
     * Sets image source from a filesystem path string.
     *
     * @param path image path
     * @return this builder
     */
    public ImageBuilder source(String path) {
        return source(DocumentImageData.fromPath(path));
    }

    /**
     * Sets image width.
     *
     * @param width width in points
     * @return this builder
     */
    public ImageBuilder width(double width) {
        this.width = width;
        return this;
    }

    /**
     * Sets image height.
     *
     * @param height height in points
     * @return this builder
     */
    public ImageBuilder height(double height) {
        this.height = height;
        return this;
    }

    /**
     * Sets image width and height.
     *
     * @param width width in points
     * @param height height in points
     * @return this builder
     */
    public ImageBuilder size(double width, double height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * Attaches image-level link metadata.
     *
     * @param linkOptions link metadata
     * @return this builder
     */
    public ImageBuilder link(DocumentLinkOptions linkOptions) {
        this.linkOptions = linkOptions;
        return this;
    }

    /**
     * Attaches image-level bookmark metadata.
     *
     * @param bookmarkOptions bookmark metadata
     * @return this builder
     */
    public ImageBuilder bookmark(DocumentBookmarkOptions bookmarkOptions) {
        this.bookmarkOptions = bookmarkOptions;
        return this;
    }

    /**
     * Sets image padding with the public canonical spacing value.
     *
     * @param padding padding in points
     * @return this builder
     */
    public ImageBuilder padding(DocumentInsets padding) {
        this.padding = padding == null ? DocumentInsets.zero() : padding;
        return this;
    }

    /**
     * Sets image margin with the public canonical spacing value.
     *
     * @param margin margin in points
     * @return this builder
     */
    public ImageBuilder margin(DocumentInsets margin) {
        this.margin = margin == null ? DocumentInsets.zero() : margin;
        return this;
    }

    /**
     * Builds the semantic image node.
     *
     * @return image node
     */
    public ImageNode build() {
        return new ImageNode(
                name,
                Objects.requireNonNull(imageData, "imageData"),
                width,
                height,
                linkOptions,
                bookmarkOptions,
                padding,
                margin);
    }
}

/**
 * Builder for simple rectangle-like shapes.
 */
