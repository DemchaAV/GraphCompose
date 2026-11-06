package com.demcha.compose.document.style;

/**
 * How a {@code ShapeContainerNode} clips the children laid inside its outline.
 *
 * <p>The semantics are independent of the chosen outline kind: the same enum
 * applies whether the container is a rectangle, a rounded rectangle, or an
 * ellipse. Backends decide how to enforce the policy — the PDF backend uses
 * graphics-state clip paths. A Word file has no clip a container can set, so
 * the DOCX backend writes most of a clipped container's content whole, crops a
 * picture that fills an elliptical outline, and names in its export report
 * each clip that cuts what the container holds.</p>
 *
 * @author Artem Demchyshyn
 */
public enum ClipPolicy {
    /**
     * Clip children to the axis-aligned bounding box of the outline only.
     * Equivalent to today's {@code LayerStackNode} behaviour and the safest
     * default — every backend can express it.
     */
    CLIP_BOUNDS,

    /**
     * Clip children to the outline's geometric path (e.g. the circle for an
     * {@link com.demcha.compose.document.style.ShapeOutline.Ellipse}).
     * The PDF backend uses the path as a graphics-state clip; backends that
     * cannot express path clipping fall back to {@link #CLIP_BOUNDS} and
     * record a capability warning.
     */
    CLIP_PATH,

    /**
     * Do not clip — children may render outside the outline. Useful when the
     * outline is decorative and the container is acting as an anchor for
     * floating overlays such as badges.
     */
    OVERFLOW_VISIBLE
}
