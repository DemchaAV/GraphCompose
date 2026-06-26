package com.demcha.compose.document.node;

import com.demcha.compose.document.style.*;

import java.util.List;
import java.util.Objects;

/**
 * Vertical flow semantic container.
 *
 * @param name         node name used in snapshots and layout graph paths
 * @param children     child semantic nodes in source order
 * @param spacing      vertical spacing between children
 * @param padding      inner padding
 * @param margin       outer margin
 * @param fillColor    optional background fill
 * @param stroke       optional uniform border stroke
 * @param cornerRadius optional render-only corner radius
 * @param borders      optional per-side border strokes overriding the uniform stroke
 * @param anchor       optional in-document navigation anchor name; renders a named
 *                     destination at the container's top-left, or {@code null} for none
 * @param bookmarkOptions optional PDF outline entry placed at the container's top on
 *                        its start page, or {@code null} for none
 * @author Artem Demchyshyn
 */
public record ContainerNode(
        String name,
        List<DocumentNode> children,
        double spacing,
        DocumentInsets padding,
        DocumentInsets margin,
        DocumentColor fillColor,
        DocumentStroke stroke,
        DocumentCornerRadius cornerRadius,
        DocumentBorders borders,
        String anchor,
        DocumentBookmarkOptions bookmarkOptions
) implements DocumentNode {
    /**
     * Creates a normalized vertical flow container.
     */
    public ContainerNode {
        name = name == null ? "" : name;
        Objects.requireNonNull(children, "children");
        children = List.copyOf(children);
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
        cornerRadius = cornerRadius == null ? DocumentCornerRadius.ZERO : cornerRadius;
        borders = borders == null ? DocumentBorders.NONE : borders;
        anchor = anchor == null || anchor.isBlank() ? null : anchor.trim();
        if (spacing < 0 || Double.isNaN(spacing) || Double.isInfinite(spacing)) {
            throw new IllegalArgumentException("spacing must be finite and non-negative: " + spacing);
        }
    }

    /**
     * Backward-compatible constructor without a bookmark (defaults to none).
     *
     * @param name         node name used in snapshots and layout graph paths
     * @param children     child semantic nodes in source order
     * @param spacing      vertical spacing between children
     * @param padding      inner padding
     * @param margin       outer margin
     * @param fillColor    optional background fill
     * @param stroke       optional uniform border stroke
     * @param cornerRadius optional render-only corner radius
     * @param borders      optional per-side border strokes overriding the uniform stroke
     * @param anchor       optional navigation anchor name
     */
    public ContainerNode(String name,
                         List<DocumentNode> children,
                         double spacing,
                         DocumentInsets padding,
                         DocumentInsets margin,
                         DocumentColor fillColor,
                         DocumentStroke stroke,
                         DocumentCornerRadius cornerRadius,
                         DocumentBorders borders,
                         String anchor) {
        this(name, children, spacing, padding, margin, fillColor, stroke, cornerRadius, borders, anchor, null);
    }

    /**
     * Backward-compatible constructor without the navigation anchor (defaults to
     * no anchor).
     *
     * @param name         node name used in snapshots and layout graph paths
     * @param children     child semantic nodes in source order
     * @param spacing      vertical spacing between children
     * @param padding      inner padding
     * @param margin       outer margin
     * @param fillColor    optional background fill
     * @param stroke       optional uniform border stroke
     * @param cornerRadius optional render-only corner radius
     * @param borders      optional per-side border strokes overriding the uniform stroke
     */
    public ContainerNode(String name,
                         List<DocumentNode> children,
                         double spacing,
                         DocumentInsets padding,
                         DocumentInsets margin,
                         DocumentColor fillColor,
                         DocumentStroke stroke,
                         DocumentCornerRadius cornerRadius,
                         DocumentBorders borders) {
        this(name, children, spacing, padding, margin, fillColor, stroke, cornerRadius, borders, null, null);
    }

    /**
     * Creates a vertical flow container without per-side borders.
     *
     * @param name         node name used in snapshots and layout graph paths
     * @param children     child semantic nodes in source order
     * @param spacing      vertical spacing between children
     * @param padding      inner padding
     * @param margin       outer margin
     * @param fillColor    optional background fill
     * @param stroke       optional uniform border stroke
     * @param cornerRadius optional render-only corner radius
     */
    public ContainerNode(String name,
                         List<DocumentNode> children,
                         double spacing,
                         DocumentInsets padding,
                         DocumentInsets margin,
                         DocumentColor fillColor,
                         DocumentStroke stroke,
                         DocumentCornerRadius cornerRadius) {
        this(name, children, spacing, padding, margin, fillColor, stroke, cornerRadius, DocumentBorders.NONE);
    }

    /**
     * Creates a vertical flow container with square corners and no per-side borders.
     *
     * @param name      node name used in snapshots and layout graph paths
     * @param children  child semantic nodes in source order
     * @param spacing   vertical spacing between children
     * @param padding   inner padding
     * @param margin    outer margin
     * @param fillColor optional background fill
     * @param stroke    optional uniform border stroke
     */
    public ContainerNode(String name,
                         List<DocumentNode> children,
                         double spacing,
                         DocumentInsets padding,
                         DocumentInsets margin,
                         DocumentColor fillColor,
                         DocumentStroke stroke) {
        this(name, children, spacing, padding, margin, fillColor, stroke, DocumentCornerRadius.ZERO, DocumentBorders.NONE);
    }
}


