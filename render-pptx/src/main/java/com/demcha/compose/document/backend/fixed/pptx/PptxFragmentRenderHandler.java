package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.layout.PlacedFragment;

/**
 * Public extension point for painting one PPTX fragment payload type.
 *
 * <p>Each handler owns exactly one payload type and materializes it as shapes
 * on the slide supplied by the surrounding {@link PptxRenderEnvironment}. The
 * contract mirrors the PDF backend's handler seam: handlers are small,
 * stateless, and reused across slides and documents — per-render mutable state
 * belongs in {@link PptxRenderEnvironment}, never in handler fields.</p>
 *
 * <p><b>Adding a custom handler.</b> Register through
 * {@link PptxFixedLayoutBackend.Builder#addHandler(PptxFragmentRenderHandler)}.
 * A custom handler reporting the same {@link #payloadType()} as a built-in
 * default replaces that default for the backend instance.</p>
 *
 * @param <T> payload type supported by the handler
 * @since 2.1.0
 */
public interface PptxFragmentRenderHandler<T> {

    /**
     * Returns the payload class accepted by this handler.
     *
     * @return concrete payload type supported by the handler
     */
    Class<T> payloadType();

    /**
     * Renders one resolved fragment.
     *
     * @param fragment    resolved fragment placement (PDF-native page space —
     *                    convert through the environment's canvas height)
     * @param payload     fragment payload data already validated by the layout layer
     * @param environment shared PPTX render environment for the current document pass
     * @throws Exception if the drawing operation fails
     */
    void render(PlacedFragment fragment, T payload, PptxRenderEnvironment environment) throws Exception;
}
