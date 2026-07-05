package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.layout.PlacedFragment;

/**
 * Public extension point for painting one PDF fragment payload type.
 *
 * <p>Each handler owns exactly one payload type and renders it onto a page
 * surface supplied by the surrounding {@link PdfRenderEnvironment}. Handlers
 * are intentionally small and stateless so the backend can extend feature
 * coverage without turning into one monolithic renderer.</p>
 *
 * <p><b>Adding a custom handler.</b> Library users register custom handlers
 * through {@link PdfFixedLayoutBackend.Builder#addHandler(PdfFragmentRenderHandler)}.
 * If a custom handler reports the same {@link #payloadType()} as a built-in
 * default, it replaces the default for that backend instance — useful when you
 * want to override how a built-in payload (e.g. {@code ShapeFragmentPayload})
 * is rendered without forking the backend.</p>
 *
 * <p>Handlers are stateless from the engine's point of view and are reused
 * across pages and documents; do not mutate handler-instance fields per
 * fragment. Per-render mutable state belongs in {@link PdfRenderEnvironment}
 * caches or in the fragment payload itself.</p>
 *
 * @param <T> payload type supported by the handler
 * @since 1.6.0
 */
public interface PdfFragmentRenderHandler<T> {

    /**
     * Returns the payload class accepted by this handler.
     *
     * @return concrete payload type supported by the handler
     */
    Class<T> payloadType();

    /**
     * Renders one resolved fragment.
     *
     * @param fragment    resolved fragment placement
     * @param payload     fragment payload data already validated by the layout layer
     * @param environment shared PDF render environment for the current document pass
     * @throws Exception if the PDF drawing operation fails
     */
    void render(PlacedFragment fragment, T payload, PdfRenderEnvironment environment) throws Exception;
}
