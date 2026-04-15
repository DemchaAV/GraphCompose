package com.demcha.compose.v2;

import com.demcha.compose.font_library.FontFamilyDefinition;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Shared context passed to fixed-layout backends.
 */
public record FixedLayoutRenderContext(
        LayoutCanvas canvas,
        Collection<FontFamilyDefinition> customFontFamilies,
        Path outputFile
) {
    public FixedLayoutRenderContext {
        customFontFamilies = List.copyOf(customFontFamilies);
    }
}
