package com.demcha.compose.layout_core.system.interfaces;

import java.io.IOException;

/**
 * Factory for backend-specific render-pass sessions.
 *
 * <p>The engine does not ask the backend to open a fresh surface per entity.
 * Instead it opens one session per render pass, and the backend can decide how
 * to reuse page-local resources inside that pass.</p>
 *
 * @param <T> backend-specific surface type
 */
public interface RenderStream<T extends AutoCloseable> {
    RenderPassSession<T> openRenderPass() throws IOException;
}
