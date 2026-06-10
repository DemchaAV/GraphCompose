package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;

import java.util.List;
import java.util.Objects;

/**
 * Vertical semantic section node.
 *
 * @param name node name used in snapshots and layout graph paths
 * @param children child semantic nodes in source order
 * @param spacing vertical spacing between children
 * @param padding inner padding
 * @param margin outer margin
 * @param fillColor optional background fill
 * @param stroke optional uniform border stroke
 * @param cornerRadius optional render-only corner radius
 * @param borders optional per-side border strokes overriding the uniform stroke
 * @param keepTogether when {@code true}, the section relocates whole to the next
 *                     page instead of orphaning its leading children when it does
 *                     not fit in the remaining page space (and fits on a fresh page)
 *
 * @author Artem Demchyshyn
 */
public record SectionNode(
        String name,
        List<DocumentNode> children,
        double spacing,
        DocumentInsets padding,
        DocumentInsets margin,
        DocumentColor fillColor,
        DocumentStroke stroke,
        DocumentCornerRadius cornerRadius,
        DocumentBorders borders,
        boolean keepTogether
) implements DocumentNode {
    /**
     * Normalizes optional section fields and validates child spacing.
     */
    public SectionNode {
        name = name == null ? "" : name;
        Objects.requireNonNull(children, "children");
        children = List.copyOf(children);
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
        cornerRadius = cornerRadius == null ? DocumentCornerRadius.ZERO : cornerRadius;
        borders = borders == null ? DocumentBorders.NONE : borders;
        if (spacing < 0 || Double.isNaN(spacing) || Double.isInfinite(spacing)) {
            throw new IllegalArgumentException("spacing must be finite and non-negative: " + spacing);
        }
    }

    /**
     * Backward-compatible constructor without the keep-together flag (defaults to
     * normal flow).
     *
     * @param name node name
     * @param children child nodes
     * @param spacing vertical spacing
     * @param padding inner padding
     * @param margin outer margin
     * @param fillColor optional background fill
     * @param stroke optional uniform border stroke
     * @param cornerRadius optional render-only corner radius
     * @param borders optional per-side borders
     */
    public SectionNode(String name,
                       List<DocumentNode> children,
                       double spacing,
                       DocumentInsets padding,
                       DocumentInsets margin,
                       DocumentColor fillColor,
                       DocumentStroke stroke,
                       DocumentCornerRadius cornerRadius,
                       DocumentBorders borders) {
        this(name, children, spacing, padding, margin, fillColor, stroke, cornerRadius, borders, false);
    }

    /**
     * Creates a vertical semantic section without per-side borders.
     *
     * @param name node name used in snapshots and layout graph paths
     * @param children child semantic nodes in source order
     * @param spacing vertical spacing between children
     * @param padding inner padding
     * @param margin outer margin
     * @param fillColor optional background fill
     * @param stroke optional uniform border stroke
     * @param cornerRadius optional render-only corner radius
     */
    public SectionNode(String name,
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
     * Creates a vertical semantic section with square corners and no per-side borders.
     *
     * @param name node name used in snapshots and layout graph paths
     * @param children child semantic nodes in source order
     * @param spacing vertical spacing between children
     * @param padding inner padding
     * @param margin outer margin
     * @param fillColor optional background fill
     * @param stroke optional uniform border stroke
     */
    public SectionNode(String name,
                       List<DocumentNode> children,
                       double spacing,
                       DocumentInsets padding,
                       DocumentInsets margin,
                       DocumentColor fillColor,
                       DocumentStroke stroke) {
        this(name, children, spacing, padding, margin, fillColor, stroke, DocumentCornerRadius.ZERO, DocumentBorders.NONE);
    }
}


