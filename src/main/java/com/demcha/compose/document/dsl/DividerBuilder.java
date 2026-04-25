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
 * Builder for thin horizontal divider nodes.
 */
public final class DividerBuilder extends ShapeBuilder {
    DividerBuilder() {
        height = 1.0;
        fillColor = DocumentColor.LIGHT_GRAY;
    }

    /**
     * Sets divider width.
     *
     * @param width width in points
     * @return this builder
     */
    public DividerBuilder width(double width) {
        super.width(width);
        return this;
    }

    /**
     * Sets divider height.
     *
     * @param height height in points
     * @return this builder
     */
    public DividerBuilder height(double height) {
        super.height(height);
        return this;
    }

    /**
     * Sets divider thickness.
     *
     * @param height thickness in points
     * @return this builder
     */
    public DividerBuilder thickness(double height) {
        return height(height);
    }

    /**
     * Sets divider color.
     *
     * @param color divider color
     * @return this builder
     */
    public DividerBuilder color(Color color) {
        super.fillColor(color);
        return this;
    }

    /**
     * Sets divider color with a public canonical color.
     *
     * @param color divider color
     * @return this builder
     */
    public DividerBuilder color(DocumentColor color) {
        return color(color == null ? null : color.color());
    }

    /**
     * Sets divider node name.
     *
     * @param name name used in snapshots and layout graph paths
     * @return this builder
     */
    @Override
    public DividerBuilder name(String name) {
        super.name(name);
        return this;
    }

    /**
     * Sets divider padding with the public canonical spacing value.
     *
     * @param padding padding in points
     * @return this builder
     */
    @Override
    public DividerBuilder padding(DocumentInsets padding) {
        super.padding(padding);
        return this;
    }

    /**
     * Sets divider margin with the public canonical spacing value.
     *
     * @param margin margin in points
     * @return this builder
     */
    @Override
    public DividerBuilder margin(DocumentInsets margin) {
        super.margin(margin);
        return this;
    }

    /**
     * Builds the divider as a thin shape node.
     *
     * @return divider shape node
     */
    @Override
    public ShapeNode build() {
        return new ShapeNode(name, width, height, fillColor, stroke, null, null, padding, margin);
    }
}

/**
 * Builder for semantic tables with row-atomic pagination.
 */
