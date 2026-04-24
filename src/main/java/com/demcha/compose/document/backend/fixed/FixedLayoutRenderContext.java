package com.demcha.compose.document.backend.fixed;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfMetadataOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfProtectionOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkOptions;
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
 * @param guideLines whether debug guide overlays should be drawn
 * @param metadataOptions optional PDF metadata options
 * @param watermarkOptions optional PDF watermark options
 * @param protectionOptions optional PDF protection options
 * @param headerFooterOptions immutable page header/footer options
 */
public record FixedLayoutRenderContext(
        LayoutCanvas canvas,
        Collection<FontFamilyDefinition> customFontFamilies,
        Path outputFile,
        OutputStream outputStream,
        boolean guideLines,
        PdfMetadataOptions metadataOptions,
        PdfWatermarkOptions watermarkOptions,
        PdfProtectionOptions protectionOptions,
        Collection<PdfHeaderFooterOptions> headerFooterOptions
) {
    /**
     * Normalizes the custom font collection into an immutable snapshot.
     */
    public FixedLayoutRenderContext {
        customFontFamilies = List.copyOf(customFontFamilies);
        headerFooterOptions = List.copyOf(headerFooterOptions);
    }
}



