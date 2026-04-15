package com.demcha.compose.v2;

import com.demcha.compose.font_library.FontFamilyDefinition;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Shared context passed to semantic export backends.
 */
public record SemanticExportContext(
        LayoutCanvas canvas,
        Collection<FontFamilyDefinition> customFontFamilies,
        Path outputFile
) {
    public SemanticExportContext {
        customFontFamilies = List.copyOf(customFontFamilies);
    }
}
