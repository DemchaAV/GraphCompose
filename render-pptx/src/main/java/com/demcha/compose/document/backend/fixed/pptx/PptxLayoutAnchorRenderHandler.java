package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.LayoutAnchorPayload;

/**
 * Draws nothing for a {@link LayoutAnchorPayload} fragment.
 *
 * <p>The payload reports where an anchored subtree landed so a resolved-layout pass can
 * read it; the ink is drawn by whatever the pass contributes, not here. A handler is
 * required all the same — {@link PptxFixedLayoutBackend}'s {@code handlerFor} throws on a
 * payload class it does not know, so an unhandled anchor would fail every render that
 * contains one.</p>
 *
 * <p>Package-private, and here rather than in {@code ..pptx.handlers} for that reason: the
 * resolved-layout seam is internal, and a public class is a permanent one. The siblings in
 * that package are public because a caller can register them; nothing registers this.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
final class PptxLayoutAnchorRenderHandler implements PptxFragmentRenderHandler<LayoutAnchorPayload> {

    PptxLayoutAnchorRenderHandler() {
    }

    @Override
    public Class<LayoutAnchorPayload> payloadType() {
        return LayoutAnchorPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       LayoutAnchorPayload payload,
                       PptxRenderEnvironment environment) {
        // Intentionally empty: an anchor is metadata, not ink.
    }
}
