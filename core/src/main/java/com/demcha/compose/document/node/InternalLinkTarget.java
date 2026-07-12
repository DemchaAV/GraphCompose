package com.demcha.compose.document.node;

import java.util.Objects;

/**
 * A {@link DocumentLinkTarget} that jumps to a named anchor in the same
 * document.
 *
 * <p>The anchor name must match a destination declared elsewhere with
 * {@code anchor(name)} on a flow or leaf builder. Resolution is deferred to the
 * end of the render pass so a link may reference an anchor that appears later in
 * the document (a forward reference). When no matching anchor exists, supporting
 * backends emit no navigation action and render the run as ordinary styled
 * text.</p>
 *
 * @param anchor non-blank anchor name to navigate to
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public record InternalLinkTarget(String anchor) implements DocumentLinkTarget {
    /**
     * Validates and normalizes the anchor name.
     *
     * @param anchor anchor name to navigate to
     */
    public InternalLinkTarget {
        anchor = Objects.requireNonNullElse(anchor, "").trim();
        if (anchor.isEmpty()) {
            throw new IllegalArgumentException("Internal link anchor must not be blank.");
        }
    }
}
