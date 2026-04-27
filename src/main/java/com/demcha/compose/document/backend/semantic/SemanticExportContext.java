package com.demcha.compose.document.backend.semantic;

import com.demcha.compose.document.layout.LayoutCanvas;

import com.demcha.compose.document.output.DocumentOutputOptions;
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
 * @param outputOptions backend-neutral document output options (metadata,
 *                      watermark, headers/footers, protection)
 */
public record SemanticExportContext(
        LayoutCanvas canvas,
        Collection<FontFamilyDefinition> customFontFamilies,
        Path outputFile,
        DocumentOutputOptions outputOptions
) {
    /**
     * Normalizes the custom font collection into an immutable snapshot and
     * defaults the output options to the empty bundle when the caller passes
     * {@code null}.
     */
    public SemanticExportContext {
        customFontFamilies = List.copyOf(customFontFamilies);
        outputOptions = outputOptions == null ? DocumentOutputOptions.EMPTY : outputOptions;
    }

    /**
     * Backwards-compatible constructor without explicit output options.
     */
    public SemanticExportContext(LayoutCanvas canvas,
                                 Collection<FontFamilyDefinition> customFontFamilies,
                                 Path outputFile) {
        this(canvas, customFontFamilies, outputFile, DocumentOutputOptions.EMPTY);
    }
}



