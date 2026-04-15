package com.demcha.compose.document.backend.semantic;

import com.demcha.compose.document.layout.LayoutCanvas;

import com.demcha.compose.font_library.FontFamilyDefinition;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Shared immutable context passed to semantic export backends.
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



