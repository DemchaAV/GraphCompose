package com.demcha.compose.document.backend.semantic;

import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.layout.LayoutGraph;

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
 * @param layoutGraph the same document's compiled layout, or {@code null} — present only
 *                    for a backend that asked for it through
 *                    {@link SemanticBackend#requiresResolvedLayout()}, and always
 *                    compiled from the graph handed to the same {@code export} call
 * @param layoutFailure why a layout the backend asked for is missing, or {@code null} — the
 *                      message of the failure that stopped the document being laid out, as
 *                      the engine words it (its class name when it carries no message),
 *                      present only when {@code layoutGraph} is {@code null} because
 *                      compiling it failed, so a backend that exports without it can say why;
 *                      a blank one is read as none
 * @since 2.5.0 carries {@code layoutGraph} and {@code layoutFailure}
 */
public record SemanticExportContext(
        LayoutCanvas canvas,
        Collection<FontFamilyDefinition> customFontFamilies,
        Path outputFile,
        DocumentOutputOptions outputOptions,
        @com.demcha.compose.document.api.Beta LayoutGraph layoutGraph,
        @com.demcha.compose.document.api.Beta String layoutFailure
) {
    /**
     * Normalizes the custom font collection into an immutable snapshot,
     * defaults the output options to the empty bundle when the caller passes
     * {@code null}, and reads a blank layout failure as none.
     *
     * @throws IllegalArgumentException if both a layout and a failure to compile one are given
     */
    public SemanticExportContext {
        customFontFamilies = List.copyOf(customFontFamilies);
        outputOptions = outputOptions == null ? DocumentOutputOptions.EMPTY : outputOptions;
        layoutFailure = layoutFailure == null || layoutFailure.isBlank() ? null : layoutFailure;
        if (layoutGraph != null && layoutFailure != null) {
            throw new IllegalArgumentException(
                    "A context carries a layout or the reason there is none, not both: " + layoutFailure);
        }
    }

    /**
     * Constructor without a resolved layout, for a backend that reads the graph alone.
     *
     * <p>Written out rather than left to the record because adding the layout components moved
     * the canonical constructor to six arguments, and this four-argument descriptor is
     * published: callers compiled against it would stop linking.</p>
     *
     * @param canvas physical page canvas for semantic export
     * @param customFontFamilies document-local font families available to the backend
     * @param outputFile optional export output file
     * @param outputOptions backend-neutral document output options
     */
    public SemanticExportContext(LayoutCanvas canvas,
                                 Collection<FontFamilyDefinition> customFontFamilies,
                                 Path outputFile,
                                 DocumentOutputOptions outputOptions) {
        this(canvas, customFontFamilies, outputFile, outputOptions, null, null);
    }

    /**
     * Backwards-compatible constructor without explicit output options.
     *
     * @param canvas physical page canvas for semantic export
     * @param customFontFamilies document-local font families available to the backend
     * @param outputFile optional export output file
     */
    public SemanticExportContext(LayoutCanvas canvas,
                                 Collection<FontFamilyDefinition> customFontFamilies,
                                 Path outputFile) {
        this(canvas, customFontFamilies, outputFile, DocumentOutputOptions.EMPTY, null, null);
    }

    /**
     * The compiled layout, for a backend that asked for one.
     *
     * @return the resolved layout
     * @throws IllegalStateException if no layout was supplied — which means the backend
     *                               did not ask for one, the document could not be laid out
     *                               (the message then says why), or the context was built
     *                               by hand without it
     * @since 2.5.0
     */
    @com.demcha.compose.document.api.Beta
    public LayoutGraph requireLayoutGraph() {
        if (layoutGraph == null) {
            return throwMissingLayout(layoutFailure);
        }
        return layoutGraph;
    }

    private static LayoutGraph throwMissingLayout(String failure) {
        if (failure != null) {
            throw new IllegalStateException("No resolved layout in this export context: the document "
                    + "could not be laid out (" + failure + ")");
        }
        throw new IllegalStateException(
                "No resolved layout in this export context. A backend is given one only "
                + "when it returns true from SemanticBackend.requiresResolvedLayout(); a "
                + "context built directly must be given the layout compiled from the same "
                + "document graph.");
    }
}



