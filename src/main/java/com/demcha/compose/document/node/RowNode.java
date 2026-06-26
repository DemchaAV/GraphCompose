package com.demcha.compose.document.node;

import com.demcha.compose.document.style.*;

import java.util.List;
import java.util.Objects;

/**
 * Horizontal flow semantic container that arranges its children left-to-right
 * inside a single row band.
 *
 * <p>The row is treated as an atomic block by the canonical paginator: the
 * whole row moves to the next page when its measured height does not fit on
 * the current page. Child nodes share the row width using either explicit
 * weights or an even split when no weights are configured.</p>
 *
 * <p>Children may be atomic primitives (paragraph, image, shape, line, ellipse,
 * spacer, barcode) or vertical containers ({@link SectionNode},
 * {@link ContainerNode}) acting as columns. Nested rows and tables are
 * disallowed at the DSL boundary.</p>
 *
 * @param name         node name used in snapshots and layout graph paths
 * @param children     child semantic nodes in source order
 * @param weights      optional per-child weights (length must match children, or be empty)
 * @param gap          horizontal gap between children
 * @param padding      inner padding
 * @param margin       outer margin
 * @param fillColor    optional background fill
 * @param stroke       optional border stroke
 * @param cornerRadius optional render-only corner radius
 * @param borders      optional per-side border strokes overriding the uniform stroke
 * @param columns      optional per-child column widths (fixed / intrinsic / weight);
 *                     length must match children, or be empty. Mutually exclusive
 *                     with {@code weights}.
 * @param verticalAlign cross-axis placement of children within the row band
 *                      (defaults to {@link RowVerticalAlign#TOP})
 * @author Artem Demchyshyn
 */
public record RowNode(
        String name,
        List<DocumentNode> children,
        List<Double> weights,
        double gap,
        DocumentInsets padding,
        DocumentInsets margin,
        DocumentColor fillColor,
        DocumentStroke stroke,
        DocumentCornerRadius cornerRadius,
        DocumentBorders borders,
        List<DocumentRowColumn> columns,
        RowVerticalAlign verticalAlign
) implements DocumentNode {
    /**
     * Creates a normalized horizontal row container.
     */
    public RowNode {
        name = name == null ? "" : name;
        Objects.requireNonNull(children, "children");
        children = List.copyOf(children);
        weights = weights == null ? List.of() : List.copyOf(weights);
        if (!weights.isEmpty() && weights.size() != children.size()) {
            throw new IllegalArgumentException("RowNode weights size " + weights.size()
                                               + " does not match children size " + children.size());
        }
        for (Double weight : weights) {
            if (weight == null || Double.isNaN(weight) || Double.isInfinite(weight) || weight <= 0.0) {
                throw new IllegalArgumentException("RowNode weights must be positive finite numbers, got: " + weights);
            }
        }
        columns = columns == null ? List.of() : List.copyOf(columns);
        if (!columns.isEmpty()) {
            if (columns.size() != children.size()) {
                throw new IllegalArgumentException("RowNode columns size " + columns.size()
                                                   + " does not match children size " + children.size());
            }
            for (DocumentRowColumn column : columns) {
                if (column == null) {
                    throw new IllegalArgumentException("RowNode columns must not contain null.");
                }
            }
            if (!weights.isEmpty()) {
                throw new IllegalArgumentException("RowNode cannot set both weights and columns; use one.");
            }
        }
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
        cornerRadius = cornerRadius == null ? DocumentCornerRadius.ZERO : cornerRadius;
        borders = borders == null ? DocumentBorders.NONE : borders;
        verticalAlign = verticalAlign == null ? RowVerticalAlign.TOP : verticalAlign;
        if (gap < 0 || Double.isNaN(gap) || Double.isInfinite(gap)) {
            throw new IllegalArgumentException("gap must be finite and non-negative: " + gap);
        }
    }

    /**
     * Backwards-compatible constructor without a cross-axis vertical alignment —
     * defaults to {@link RowVerticalAlign#TOP}.
     *
     * @param name         node name used in snapshots and layout graph paths
     * @param children     child semantic nodes in source order
     * @param weights      optional per-child weights (length must match children, or be empty)
     * @param gap          horizontal gap between children
     * @param padding      inner padding
     * @param margin       outer margin
     * @param fillColor    optional background fill
     * @param stroke       optional border stroke
     * @param cornerRadius optional render-only corner radius
     * @param borders      optional per-side border strokes
     * @param columns      optional per-child column widths
     */
    public RowNode(String name,
                   List<DocumentNode> children,
                   List<Double> weights,
                   double gap,
                   DocumentInsets padding,
                   DocumentInsets margin,
                   DocumentColor fillColor,
                   DocumentStroke stroke,
                   DocumentCornerRadius cornerRadius,
                   DocumentBorders borders,
                   List<DocumentRowColumn> columns) {
        this(name, children, weights, gap, padding, margin, fillColor, stroke, cornerRadius, borders, columns,
                RowVerticalAlign.TOP);
    }

    /**
     * Backwards-compatible constructor without per-child columns — defaults to an
     * empty column list (weights / even split).
     *
     * @param name         node name used in snapshots and layout graph paths
     * @param children     child semantic nodes in source order
     * @param weights      optional per-child weights (length must match children, or be empty)
     * @param gap          horizontal gap between children
     * @param padding      inner padding
     * @param margin       outer margin
     * @param fillColor    optional background fill
     * @param stroke       optional border stroke
     * @param cornerRadius optional render-only corner radius
     * @param borders      optional per-side border strokes
     */
    public RowNode(String name,
                   List<DocumentNode> children,
                   List<Double> weights,
                   double gap,
                   DocumentInsets padding,
                   DocumentInsets margin,
                   DocumentColor fillColor,
                   DocumentStroke stroke,
                   DocumentCornerRadius cornerRadius,
                   DocumentBorders borders) {
        this(name, children, weights, gap, padding, margin, fillColor, stroke, cornerRadius, borders, List.of(),
                RowVerticalAlign.TOP);
    }

    /**
     * Backwards-compatible constructor without per-side borders.
     *
     * @param name         node name used in snapshots and layout graph paths
     * @param children     child semantic nodes in source order
     * @param weights      optional per-child weights (length must match children, or be empty)
     * @param gap          horizontal gap between children
     * @param padding      inner padding
     * @param margin       outer margin
     * @param fillColor    optional background fill
     * @param stroke       optional border stroke
     * @param cornerRadius optional render-only corner radius
     */
    public RowNode(String name,
                   List<DocumentNode> children,
                   List<Double> weights,
                   double gap,
                   DocumentInsets padding,
                   DocumentInsets margin,
                   DocumentColor fillColor,
                   DocumentStroke stroke,
                   DocumentCornerRadius cornerRadius) {
        this(name, children, weights, gap, padding, margin, fillColor, stroke, cornerRadius, DocumentBorders.NONE,
                List.of(), RowVerticalAlign.TOP);
    }
}
