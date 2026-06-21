package com.demcha.compose.document.layout.payloads;

/**
 * Non-visual marker fragment payload that declares a named in-document
 * navigation destination (an anchor) at the fragment's top-left.
 *
 * <p>An anchored leaf or container node emits one of these at its resolved
 * top-left. The PDF backend records each anchor's resolved page and position so
 * that internal links ({@code InternalLinkTarget}) resolve to a go-to action in
 * a deferred post-pass — supporting forward references. The marker itself draws
 * nothing.</p>
 *
 * @param anchor non-blank anchor name
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public record AnchorMarkerPayload(String anchor) {
    /**
     * Normalizes the anchor name.
     */
    public AnchorMarkerPayload {
        anchor = anchor == null ? "" : anchor.trim();
    }
}
