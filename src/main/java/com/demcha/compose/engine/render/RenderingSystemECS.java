package com.demcha.compose.engine.render;

import com.demcha.compose.engine.core.Canvas;
import com.demcha.compose.engine.components.content.shape.Side;
import com.demcha.compose.engine.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.compose.engine.core.SystemECS;
import com.demcha.compose.engine.render.guides.GuidLineSettings;
import com.demcha.compose.engine.render.guides.GuidesRenderer;

import java.awt.*;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * Backend-neutral rendering system contract.
 *
 * <p>The shared engine knows how to orchestrate a render pass, but not how a
 * concrete backend materializes page surfaces. Implementations expose the
 * active {@link RenderPassSession} during {@code process(...)} so handlers can
 * reuse backend-local page resources without leaking PDF/DOCX/PPTX details into
 * engine/template code.</p>
 */
public interface RenderingSystemECS<S extends AutoCloseable> extends SystemECS {
    <T extends Canvas> T canvas();

    GuidesRenderer<S> guidesRenderer();
    RenderHandlerRegistry renderHandlers();

    <T extends RenderStream<S>> T stream();
    Optional<RenderPassSession<S>> activeRenderSession();

    /**
     * Returns the current render-pass session.
     *
     * <p>This is only valid while a rendering system is actively processing a
     * document. Callers that need ad-hoc rendering outside a normal pass should
     * open their own short-lived session from {@link #stream()}.</p>
     */
    default RenderPassSession<S> renderSession() {
        return activeRenderSession()
                .orElseThrow(() -> new IllegalStateException("No active render session is available"));
    }

    boolean renderBorder(S stream, RenderCoordinateContext context,
                         boolean lineDash,
                         Set<Side> sides) throws IOException;

    GuidLineSettings guidLineSettings();

    boolean renderRectangle(S stream, RenderCoordinateContext context, boolean lineDash) throws IOException;

    void fillCircle(S stream, float cx, float cy, float r, Color fill) throws IOException;

}

