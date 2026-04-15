package com.demcha.compose.document.backend.fixed;

import com.demcha.compose.document.layout.LayoutCanvas;

import com.demcha.compose.font_library.FontFamilyDefinition;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Shared immutable context passed to fixed-layout backends.
 *
 * <p>The context carries only document-wide render configuration. Backends must
 * treat it as read-only and derive any mutable render-pass state from it.</p>
 */
public record FixedLayoutRenderContext(
        LayoutCanvas canvas,
        Collection<FontFamilyDefinition> customFontFamilies,
        Path outputFile
) {
    /**
     * Normalizes the custom font collection into an immutable snapshot.
     */
    public FixedLayoutRenderContext {
        customFontFamilies = List.copyOf(customFontFamilies);
    }
}



