package com.demcha.compose.document.node;

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
 * @param stroke optional border stroke
 * @param cornerRadius optional render-only corner radius
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
        DocumentCornerRadius cornerRadius
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
        if (spacing < 0 || Double.isNaN(spacing) || Double.isInfinite(spacing)) {
            throw new IllegalArgumentException("spacing must be finite and non-negative: " + spacing);
        }
    }

    /**
     * Creates a vertical semantic section with square corners.
     */
    public SectionNode(String name,
                       List<DocumentNode> children,
                       double spacing,
                       DocumentInsets padding,
                       DocumentInsets margin,
                       DocumentColor fillColor,
                       DocumentStroke stroke) {
        this(name, children, spacing, padding, margin, fillColor, stroke, DocumentCornerRadius.ZERO);
    }
}


