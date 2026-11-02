package com.demcha.compose.document.node;

import com.demcha.compose.document.style.*;

import java.awt.*;

/**
 * Atomic rectangle-like semantic shape.
 *
 * @param name            node name used in snapshots and layout graph paths
 * @param width           resolved shape width
 * @param height          resolved shape height
 * @param fillColor       optional fill color
 * @param stroke          optional stroke descriptor
 * @param cornerRadius    optional render-only corner radius
 * @param linkTarget      optional node-level link target (external URI or internal anchor)
 * @param bookmarkOptions optional node-level bookmark metadata
 * @param padding         inner padding
 * @param margin          outer margin
 * @param transform       render-time affine transform (rotation around the
 *                        placement centre and/or scaling); defaults to
 *                        {@link DocumentTransform#NONE}. Layout snapshots stay
 *                        deterministic regardless of rotation/scale — backends
 *                        apply the transform during render only.
 * @param fillPaint       optional paint fill; when set it wins over
 *                        {@code fillColor}. Gradients render as native shadings in
 *                        the PDF backend; backends without shading support fall
 *                        back to {@link DocumentPaint#primaryColor()}.
 * @param anchor          optional in-document navigation anchor name at the shape's
 *                        top-left, or {@code null} for none
 * @param divider         whether the box was authored as a divider — a rule across
 *                        the flow, which the DSL's divider builder
 *                        ({@link com.demcha.compose.document.dsl.DividerBuilder}),
 *                        and the templates module's {@code Divider} widget, build —
 *                        rather than as a box of a colour placed where the
 *                        page puts it. Nothing is drawn differently for it: on the
 *                        page both are a filled box, and layout, measurement and
 *                        pagination do not read it. It is there for a backend that
 *                        has a rule of its own and must know which box asked to be
 *                        one — the DOCX export writes a divider in the flow as
 *                        Word's horizontal rule, a bordered paragraph that flows
 *                        with the text, and leaves any other box of the same size
 *                        to be drawn where the page puts it, which no geometry of
 *                        the box distinguishes. Defaults to
 *                        {@code false}. ({@code @since 2.5.0})
 * @author Artem Demchyshyn
 */
public record ShapeNode(
        String name,
        double width,
        double height,
        DocumentColor fillColor,
        DocumentStroke stroke,
        DocumentCornerRadius cornerRadius,
        DocumentLinkTarget linkTarget,
        DocumentBookmarkOptions bookmarkOptions,
        DocumentInsets padding,
        DocumentInsets margin,
        DocumentTransform transform,
        DocumentPaint fillPaint,
        String anchor,
        boolean divider
) implements DocumentNode {
    /**
     * Backwards-compatible canonical constructor without the divider signal —
     * defaults to {@code false}, the box of a colour it has always built.
     *
     * @param name            node name used in snapshots and layout graph paths
     * @param width           resolved shape width
     * @param height          resolved shape height
     * @param fillColor       optional fill color
     * @param stroke          optional stroke descriptor
     * @param cornerRadius    optional render-only corner radius
     * @param linkTarget      optional node-level link target
     * @param bookmarkOptions optional node-level bookmark metadata
     * @param padding         inner padding
     * @param margin          outer margin
     * @param transform       render-time affine transform
     * @param fillPaint       optional paint fill
     * @param anchor          optional navigation anchor name
     */
    public ShapeNode(String name,
                     double width,
                     double height,
                     DocumentColor fillColor,
                     DocumentStroke stroke,
                     DocumentCornerRadius cornerRadius,
                     DocumentLinkTarget linkTarget,
                     DocumentBookmarkOptions bookmarkOptions,
                     DocumentInsets padding,
                     DocumentInsets margin,
                     DocumentTransform transform,
                     DocumentPaint fillPaint,
                     String anchor) {
        this(name, width, height, fillColor, stroke, cornerRadius, linkTarget, bookmarkOptions,
                padding, margin, transform, fillPaint, anchor, false);
    }

    /**
     * Backwards-compatible canonical constructor taking external
     * {@link DocumentLinkOptions} (wrapped) with a paint fill and no navigation
     * anchor.
     *
     * @param name            node name used in snapshots and layout graph paths
     * @param width           resolved shape width
     * @param height          resolved shape height
     * @param fillColor       optional fill color
     * @param stroke          optional stroke descriptor
     * @param cornerRadius    optional render-only corner radius
     * @param linkOptions     optional external link metadata
     * @param bookmarkOptions optional node-level bookmark metadata
     * @param padding         inner padding
     * @param margin          outer margin
     * @param transform       render-time affine transform
     * @param fillPaint       optional paint fill
     */
    public ShapeNode(String name,
                     double width,
                     double height,
                     DocumentColor fillColor,
                     DocumentStroke stroke,
                     DocumentCornerRadius cornerRadius,
                     DocumentLinkOptions linkOptions,
                     DocumentBookmarkOptions bookmarkOptions,
                     DocumentInsets padding,
                     DocumentInsets margin,
                     DocumentTransform transform,
                     DocumentPaint fillPaint) {
        this(name, width, height, fillColor, stroke, cornerRadius,
                linkOptions == null ? null : new ExternalLinkTarget(linkOptions),
                bookmarkOptions, padding, margin, transform, fillPaint, null);
    }

    /**
     * Backwards-compatible canonical constructor without a paint fill.
     *
     * @param name            node name used in snapshots and layout graph paths
     * @param width           resolved shape width
     * @param height          resolved shape height
     * @param fillColor       optional fill color
     * @param stroke          optional stroke descriptor
     * @param cornerRadius    optional render-only corner radius
     * @param linkOptions     optional node-level link metadata
     * @param bookmarkOptions optional node-level bookmark metadata
     * @param padding         inner padding
     * @param margin          outer margin
     * @param transform       render-time affine transform
     */
    public ShapeNode(String name,
                     double width,
                     double height,
                     DocumentColor fillColor,
                     DocumentStroke stroke,
                     DocumentCornerRadius cornerRadius,
                     DocumentLinkOptions linkOptions,
                     DocumentBookmarkOptions bookmarkOptions,
                     DocumentInsets padding,
                     DocumentInsets margin,
                     DocumentTransform transform) {
        this(name, width, height, fillColor, stroke, cornerRadius, linkOptions, bookmarkOptions,
                padding, margin, transform, null);
    }

    /**
     * Normalizes spacing defaults and validates explicit shape dimensions.
     */
    public ShapeNode {
        name = name == null ? "" : name;
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
        cornerRadius = cornerRadius == null ? DocumentCornerRadius.ZERO : cornerRadius;
        transform = transform == null ? DocumentTransform.NONE : transform;
        anchor = anchor == null || anchor.isBlank() ? null : anchor.trim();
        if (width <= 0 || Double.isNaN(width) || Double.isInfinite(width)) {
            throw new IllegalArgumentException("width must be finite and positive: " + width);
        }
        if (height <= 0 || Double.isNaN(height) || Double.isInfinite(height)) {
            throw new IllegalArgumentException("height must be finite and positive: " + height);
        }
    }

    /**
     * Backward-compatible convenience constructor without link/bookmark metadata.
     *
     * @param name      node name used in snapshots and layout graph paths
     * @param width     resolved shape width
     * @param height    resolved shape height
     * @param fillColor optional fill color
     * @param stroke    optional stroke descriptor
     * @param padding   inner padding
     * @param margin    outer margin
     */
    public ShapeNode(String name,
                     double width,
                     double height,
                     Color fillColor,
                     DocumentStroke stroke,
                     DocumentInsets padding,
                     DocumentInsets margin) {
        this(name, width, height, fillColor == null ? null : DocumentColor.of(fillColor), stroke,
                DocumentCornerRadius.ZERO, null, null, padding, margin, DocumentTransform.NONE);
    }

    /**
     * Backward-compatible convenience constructor without corner radius.
     *
     * @param name            node name used in snapshots and layout graph paths
     * @param width           resolved shape width
     * @param height          resolved shape height
     * @param fillColor       optional fill color
     * @param stroke          optional stroke descriptor
     * @param linkOptions     optional node-level link metadata
     * @param bookmarkOptions optional node-level bookmark metadata
     * @param padding         inner padding
     * @param margin          outer margin
     */
    public ShapeNode(String name,
                     double width,
                     double height,
                     DocumentColor fillColor,
                     DocumentStroke stroke,
                     DocumentLinkOptions linkOptions,
                     DocumentBookmarkOptions bookmarkOptions,
                     DocumentInsets padding,
                     DocumentInsets margin) {
        this(name, width, height, fillColor, stroke, DocumentCornerRadius.ZERO, linkOptions, bookmarkOptions, padding, margin, DocumentTransform.NONE);
    }

    /**
     * Backward-compatible convenience constructor without transform — defaults
     * to {@link DocumentTransform#NONE}.
     *
     * @param name            node name used in snapshots and layout graph paths
     * @param width           resolved shape width
     * @param height          resolved shape height
     * @param fillColor       optional fill color
     * @param stroke          optional stroke descriptor
     * @param cornerRadius    optional render-only corner radius
     * @param linkOptions     optional node-level link metadata
     * @param bookmarkOptions optional node-level bookmark metadata
     * @param padding         inner padding
     * @param margin          outer margin
     */
    public ShapeNode(String name,
                     double width,
                     double height,
                     DocumentColor fillColor,
                     DocumentStroke stroke,
                     DocumentCornerRadius cornerRadius,
                     DocumentLinkOptions linkOptions,
                     DocumentBookmarkOptions bookmarkOptions,
                     DocumentInsets padding,
                     DocumentInsets margin) {
        this(name, width, height, fillColor, stroke, cornerRadius, linkOptions, bookmarkOptions, padding, margin, DocumentTransform.NONE);
    }

}


