package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.ListMarker;

import java.util.Objects;

/**
 * One list item under
 * {@link com.demcha.compose.document.layout.ListItemLayout#MARKER_CONTENT},
 * with its three parts kept apart.
 *
 * <p>The legacy path concatenates these into a single label — depth becomes a
 * run of non-breaking spaces, the marker becomes a text prefix, and what is left
 * is measured as one paragraph. That is exactly what makes marker geometry
 * impossible: by the time anything is measured there is no marker any more, only
 * characters at the front of a string.</p>
 *
 * <p>This record is the opposite arrangement. It is produced by one structural
 * walk of the authored tree and keeps {@code depth}, {@code marker} and
 * {@code content} independent, so the next phase can measure the marker on its
 * own and derive {@code markerX} / {@code contentX} / {@code contentWidth} from
 * the depth rather than from a count of spaces.</p>
 *
 * <p>{@code markerGap} is deliberately <b>not</b> here: it is one value for the
 * whole list, carried on the node, and duplicating it per item would let items
 * of one list disagree.</p>
 *
 * @param depth   zero-based nesting depth; 0 for every item of a flat list
 * @param marker  the item's resolved marker — the per-item override when it has
 *                one, otherwise the list's marker at depth 0 or the per-depth
 *                cascade below it. Markerless items carry
 *                {@link ListMarker#none()}, which measures 0 wide and takes no
 *                gap
 * @param content the item's text, with author-typed markers already normalized
 *                away, and with no depth indent and no marker glued to it
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public record ListItemSpec(int depth, ListMarker marker, String content) {

    /**
     * Normalizes nullable inputs and rejects a negative depth.
     *
     * @throws IllegalArgumentException when {@code depth} is negative
     */
    public ListItemSpec {
        if (depth < 0) {
            throw new IllegalArgumentException("depth must be non-negative: " + depth);
        }
        marker = marker == null ? ListMarker.none() : marker;
        content = content == null ? "" : content;
    }

    /**
     * Returns the marker text to measure — the marker's value without the
     * synthetic trailing separator {@link ListMarker} appends for the legacy
     * prefix path.
     *
     * <p>That separator exists so {@code "•" + text} does not render as
     * {@code "•text"}; under {@code MARKER_CONTENT} the space between marker and
     * content is {@code markerGap}, so measuring the separator too would count
     * the gap twice. Only that one appended separator is removed: whitespace the
     * author put inside their own marker is theirs and is measured with it.</p>
     *
     * @return marker text for measurement, empty for a markerless item
     */
    public String markerText() {
        String value = marker.value();
        return value.endsWith(" ") ? value.substring(0, value.length() - 1) : value;
    }

    /**
     * Returns whether this item shows a marker at all. A markerless item takes
     * marker width 0 <em>and</em> gap 0, so it starts flush with the depth
     * indent rather than at an unexplained inset.
     *
     * @return {@code true} when the marker has visible content
     */
    public boolean hasMarker() {
        return marker.isVisible();
    }

    @Override
    public String toString() {
        return "ListItemSpec[depth=" + depth
               + ", marker=" + Objects.toString(marker.value(), "")
               + ", content=" + content + "]";
    }
}
