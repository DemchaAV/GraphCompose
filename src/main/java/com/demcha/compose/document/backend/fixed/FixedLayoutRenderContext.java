package com.demcha.compose.document.backend.fixed;

import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.font.FontFamilyDefinition;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Shared immutable context passed to fixed-layout backends.
 *
 * <p>The context carries only document-wide render configuration. Backends must
 * treat it as read-only and derive any mutable render-pass state from it.</p>
 *
 * @param canvas physical page canvas used by the resolved layout graph
 * @param customFontFamilies document-local font families available to the backend
 * @param outputFile optional file target for backends that persist artifacts to disk
 * @param outputStream optional caller-owned stream target for streaming render output
 */
public record FixedLayoutRenderContext(
        LayoutCanvas canvas,
        Collection<FontFamilyDefinition> customFontFamilies,
        Path outputFile,
        OutputStream outputStream
) {
    /**
     * Normalizes the custom font collection into an immutable snapshot.
     */
    public FixedLayoutRenderContext {
        customFontFamilies = List.copyOf(customFontFamilies);
    }
}



