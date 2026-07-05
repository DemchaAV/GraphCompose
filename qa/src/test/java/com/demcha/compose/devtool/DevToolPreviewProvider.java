package com.demcha.compose.devtool;

import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * Contract for GraphCompose live preview providers.
 * <p>
 * Implementations build a fully in-memory {@link PDDocument}. Ownership of the
 * returned document is transferred to the dev tool, which will close it after
 * rendering.
 * </p>
 */
@FunctionalInterface
public interface DevToolPreviewProvider {

    PDDocument buildPreview() throws Exception;
}
