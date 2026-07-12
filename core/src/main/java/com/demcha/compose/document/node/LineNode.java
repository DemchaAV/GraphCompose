package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentDashPattern;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTransform;

/**
 * Atomic semantic line drawn inside a fixed-size box.
 *
 * @param name            node name used in snapshots and layout graph paths
 * @param width           resolved line box width
 * @param height          resolved line box height
 * @param startX          line start x offset inside the box
 * @param startY          line start y offset inside the box
 * @param endX            line end x offset inside the box
 * @param endY            line end y offset inside the box
 * @param stroke          line stroke descriptor
 * @param linkTarget      optional node-level link target (external URI or internal anchor)
 * @param bookmarkOptions optional node-level bookmark metadata
 * @param padding         inner padding
 * @param margin          outer margin
 * @param transform       render-time affine transform; defaults to
 *                        {@link DocumentTransform#NONE}.
 * @param dashPattern     dash pattern for the stroke; defaults to
 *                        {@link DocumentDashPattern#NONE} (solid)
 * @param anchor          optional in-document navigation anchor name at the line box's
 *                        top-left, or {@code null} for none
 * @param lineCap         end-cap style for the stroke; defaults to
 *                        {@link DocumentLineCap#BUTT}. {@code ROUND} turns a
 *                        dashed stroke into a dotted line
 * @param fillWidth       when {@code true}, the line stretches to the width
 *                        available where it is placed (its row slot, or the
 *                        content width) instead of {@code width}; the flex line
 *                        behind a dot leader. Defaults to {@code false}.
 * @author Artem Demchyshyn
 */
public record LineNode(
        String name,
        double width,
        double height,
        double startX,
        double startY,
        double endX,
        double endY,
        DocumentStroke stroke,
        DocumentLinkTarget linkTarget,
        DocumentBookmarkOptions bookmarkOptions,
        DocumentInsets padding,
        DocumentInsets margin,
        DocumentTransform transform,
        DocumentDashPattern dashPattern,
        String anchor,
        DocumentLineCap lineCap,
        boolean fillWidth
) implements DocumentNode {
    /**
     * Normalizes spacing defaults and validates explicit line geometry.
     */
    public LineNode {
        name = name == null ? "" : name;
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
        transform = transform == null ? DocumentTransform.NONE : transform;
        dashPattern = dashPattern == null ? DocumentDashPattern.NONE : dashPattern;
        anchor = anchor == null || anchor.isBlank() ? null : anchor.trim();
        lineCap = lineCap == null ? DocumentLineCap.BUTT : lineCap;
        requireNonNegativeFinite(width, "width");
        requireNonNegativeFinite(height, "height");
        requireFinite(startX, "startX");
        requireFinite(startY, "startY");
        requireFinite(endX, "endX");
        requireFinite(endY, "endY");
    }

    /**
     * Backward-compatible canonical constructor without the fill flag — defaults
     * to {@code false} (a fixed-width, byte-identical line).
     *
     * @param name            node name used in snapshots and layout graph paths
     * @param width           resolved line box width
     * @param height          resolved line box height
     * @param startX          line start x offset inside the box
     * @param startY          line start y offset inside the box
     * @param endX            line end x offset inside the box
     * @param endY            line end y offset inside the box
     * @param stroke          line stroke descriptor
     * @param linkTarget      optional node-level link target
     * @param bookmarkOptions optional node-level bookmark metadata
     * @param padding         inner padding
     * @param margin          outer margin
     * @param transform       render-time affine transform
     * @param dashPattern     dash pattern for the stroke
     * @param anchor          optional navigation anchor name
     * @param lineCap         end-cap style for the stroke
     */
    public LineNode(String name,
                    double width,
                    double height,
                    double startX,
                    double startY,
                    double endX,
                    double endY,
                    DocumentStroke stroke,
                    DocumentLinkTarget linkTarget,
                    DocumentBookmarkOptions bookmarkOptions,
                    DocumentInsets padding,
                    DocumentInsets margin,
                    DocumentTransform transform,
                    DocumentDashPattern dashPattern,
                    String anchor,
                    DocumentLineCap lineCap) {
        this(name, width, height, startX, startY, endX, endY, stroke, linkTarget, bookmarkOptions,
                padding, margin, transform, dashPattern, anchor, lineCap, false);
    }

    /**
     * Backward-compatible canonical constructor without the line cap — defaults
     * to {@link DocumentLineCap#BUTT} (a squared, byte-identical end).
     *
     * @param name            node name used in snapshots and layout graph paths
     * @param width           resolved line box width
     * @param height          resolved line box height
     * @param startX          line start x offset inside the box
     * @param startY          line start y offset inside the box
     * @param endX            line end x offset inside the box
     * @param endY            line end y offset inside the box
     * @param stroke          line stroke descriptor
     * @param linkTarget      optional node-level link target
     * @param bookmarkOptions optional node-level bookmark metadata
     * @param padding         inner padding
     * @param margin          outer margin
     * @param transform       render-time affine transform
     * @param dashPattern     dash pattern for the stroke
     * @param anchor          optional navigation anchor name
     */
    public LineNode(String name,
                    double width,
                    double height,
                    double startX,
                    double startY,
                    double endX,
                    double endY,
                    DocumentStroke stroke,
                    DocumentLinkTarget linkTarget,
                    DocumentBookmarkOptions bookmarkOptions,
                    DocumentInsets padding,
                    DocumentInsets margin,
                    DocumentTransform transform,
                    DocumentDashPattern dashPattern,
                    String anchor) {
        this(name, width, height, startX, startY, endX, endY, stroke, linkTarget, bookmarkOptions,
                padding, margin, transform, dashPattern, anchor, DocumentLineCap.BUTT, false);
    }

    /**
     * Backwards-compatible canonical constructor taking external
     * {@link DocumentLinkOptions} (wrapped) and no navigation anchor.
     *
     * @param name            node name used in snapshots and layout graph paths
     * @param width           resolved line box width
     * @param height          resolved line box height
     * @param startX          line start x offset inside the box
     * @param startY          line start y offset inside the box
     * @param endX            line end x offset inside the box
     * @param endY            line end y offset inside the box
     * @param stroke          line stroke descriptor
     * @param linkOptions     optional external link metadata
     * @param bookmarkOptions optional node-level bookmark metadata
     * @param padding         inner padding
     * @param margin          outer margin
     * @param transform       render-time affine transform
     * @param dashPattern     dash pattern for the stroke
     */
    public LineNode(String name,
                    double width,
                    double height,
                    double startX,
                    double startY,
                    double endX,
                    double endY,
                    DocumentStroke stroke,
                    DocumentLinkOptions linkOptions,
                    DocumentBookmarkOptions bookmarkOptions,
                    DocumentInsets padding,
                    DocumentInsets margin,
                    DocumentTransform transform,
                    DocumentDashPattern dashPattern) {
        this(name, width, height, startX, startY, endX, endY, stroke,
                linkOptions == null ? null : new ExternalLinkTarget(linkOptions),
                bookmarkOptions, padding, margin, transform, dashPattern, null);
    }

    /**
     * Backward-compatible constructor without a dash pattern — defaults to a
     * solid stroke ({@link DocumentDashPattern#NONE}).
     *
     * @param name            node name used in snapshots and layout graph paths
     * @param width           resolved line box width
     * @param height          resolved line box height
     * @param startX          line start x offset inside the box
     * @param startY          line start y offset inside the box
     * @param endX            line end x offset inside the box
     * @param endY            line end y offset inside the box
     * @param stroke          line stroke descriptor
     * @param linkOptions     optional node-level link metadata
     * @param bookmarkOptions optional node-level bookmark metadata
     * @param padding         inner padding
     * @param margin          outer margin
     * @param transform       render-time affine transform
     */
    public LineNode(String name,
                    double width,
                    double height,
                    double startX,
                    double startY,
                    double endX,
                    double endY,
                    DocumentStroke stroke,
                    DocumentLinkOptions linkOptions,
                    DocumentBookmarkOptions bookmarkOptions,
                    DocumentInsets padding,
                    DocumentInsets margin,
                    DocumentTransform transform) {
        this(name, width, height, startX, startY, endX, endY, stroke, linkOptions, bookmarkOptions, padding, margin, transform, DocumentDashPattern.NONE);
    }

    /**
     * Backward-compatible convenience constructor without transform — defaults
     * to {@link DocumentTransform#NONE}.
     *
     * @param name            node name used in snapshots and layout graph paths
     * @param width           resolved line box width
     * @param height          resolved line box height
     * @param startX          line start x offset inside the box
     * @param startY          line start y offset inside the box
     * @param endX            line end x offset inside the box
     * @param endY            line end y offset inside the box
     * @param stroke          line stroke descriptor
     * @param linkOptions     optional node-level link metadata
     * @param bookmarkOptions optional node-level bookmark metadata
     * @param padding         inner padding
     * @param margin          outer margin
     */
    public LineNode(String name,
                    double width,
                    double height,
                    double startX,
                    double startY,
                    double endX,
                    double endY,
                    DocumentStroke stroke,
                    DocumentLinkOptions linkOptions,
                    DocumentBookmarkOptions bookmarkOptions,
                    DocumentInsets padding,
                    DocumentInsets margin) {
        this(name, width, height, startX, startY, endX, endY, stroke, linkOptions, bookmarkOptions, padding, margin, DocumentTransform.NONE);
    }

    private static void requireNonNegativeFinite(double value, String name) {
        if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be finite and non-negative: " + value);
        }
    }

    private static void requireFinite(double value, String name) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be finite: " + value);
        }
    }
}
