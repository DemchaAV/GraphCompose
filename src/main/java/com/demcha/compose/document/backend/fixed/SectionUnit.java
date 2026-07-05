package com.demcha.compose.document.backend.fixed;

import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.font.FontFamilyDefinition;

import java.util.List;
import java.util.Objects;

/**
 * One section of a multi-section fixed-layout render: its resolved layout graph,
 * canvas, document-local fonts, and the configured {@link FixedLayoutRenderer}
 * that carries the section's own chrome (page size, headers, footers, numbering).
 *
 * <p>Sections are concatenated inside a single backend render so navigation and
 * outlines resolve across section boundaries. This type is backend-neutral —
 * {@code chrome} is a {@link FixedLayoutRenderer}, not a concrete backend — so
 * the document API can assemble a multi-section document without naming a
 * rendering backend.</p>
 *
 * @param graph       resolved layout graph for this section
 * @param canvas      resolved canvas geometry for this section
 * @param customFonts document-local font families for this section (never
 *                    {@code null}; defensively copied)
 * @param chrome      configured backend supplying this section's chrome
 * @since 2.0.0
 */
public record SectionUnit(LayoutGraph graph,
                          LayoutCanvas canvas,
                          List<FontFamilyDefinition> customFonts,
                          FixedLayoutRenderer chrome) {

    /**
     * Validates required members and defensively copies the font list.
     */
    public SectionUnit {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(canvas, "canvas");
        Objects.requireNonNull(chrome, "chrome");
        customFonts = customFonts == null ? List.of() : List.copyOf(customFonts);
    }
}
