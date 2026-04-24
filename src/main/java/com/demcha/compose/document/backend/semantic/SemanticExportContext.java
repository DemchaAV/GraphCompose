package com.demcha.compose.document.backend.semantic;

import com.demcha.compose.document.layout.LayoutCanvas;

import com.demcha.compose.font.FontFamilyDefinition;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Shared immutable context passed to semantic export backends.
 *
 * @param canvas physical page canvas for semantic export
 * @param customFontFamilies document-local font families available to the backend
 * @param outputFile optional export output file
 */
public record SemanticExportContext(
        LayoutCanvas canvas,
        Collection<FontFamilyDefinition> customFontFamilies,
        Path outputFile
) {
    /**
     * Normalizes the custom font collection into an immutable snapshot.
     */
    public SemanticExportContext {
        customFontFamilies = List.copyOf(customFontFamilies);
    }
}



