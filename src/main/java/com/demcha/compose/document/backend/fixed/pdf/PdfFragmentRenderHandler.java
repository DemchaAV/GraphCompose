package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.layout.PlacedFragment;

/**
 * Package-private PDF fragment painter contract.
 *
 * <p>Each handler owns one payload type and renders it onto a page surface
 * supplied by the surrounding {@link PdfRenderEnvironment}. Handlers are
 * intentionally small and stateless so the backend can extend feature coverage
 * without turning into one monolithic renderer.</p>
 *
 * @param <T> payload type supported by the handler
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
     * @param fragment resolved fragment placement
     * @param payload fragment payload data already validated by the layout layer
     * @param environment shared PDF render environment for the current document pass
     * @throws Exception if the PDF drawing operation fails
     */
    void render(PlacedFragment fragment, T payload, PdfRenderEnvironment environment) throws Exception;
}
