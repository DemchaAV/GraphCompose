package com.demcha.compose.layout_core.core;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;

import java.io.Closeable;

/**
 * Backend-neutral document composition contract.
 * <p>
 * A {@code DocumentComposer} owns the composition lifecycle for one document.
 * Callers use it to obtain builders, inspect the canvas, toggle document-wide
 * parsing and debug settings, and finally trigger layout and rendering.
 * </p>
 *
 * <p>The interface deliberately separates two phases:</p>
 * <ol>
 *   <li>describe the document by building entities</li>
 *   <li>materialize the document by calling {@link #build()} or {@link #toBytes()}</li>
 * </ol>
 *
 * <h3>Engine flow</h3>
 * <pre>
 * try (DocumentComposer composer = GraphCompose.pdf(outputPath).create()) {
 *     var cb = composer.componentBuilder();
 *     cb.text()
 *             .textWithAutoSize("Hello GraphCompose")
 *             .textStyle(TextStyle.DEFAULT_STYLE)
 *             .anchor(Anchor.topLeft())
 *             .build();
 *
 *     composer.build();
 * }
 * </pre>
 *
 * <h3>Template flow</h3>
 * <pre>
 * try (var composer = GraphCompose.pdf().create()) {
 *     var template = TemplateBuilder.from(
 *             composer.componentBuilder(),
 *             CvTheme.defaultTheme());
 *
 *     template.moduleBuilder("Profile", composer.canvas())
 *             .addChild(template.blockText(
 *                     "Analytical engineer focused on reliable platform design.",
 *                     composer.canvas().innerWidth()))
 *             .build();
 *
 *     byte[] pdfBytes = composer.toBytes();
 * }
 * </pre>
 */
public interface DocumentComposer extends Closeable {

    /**
     * Returns the root builder facade used to create document entities.
     *
     * <p>Builders created from this facade register entities into the composer's
     * underlying {@code EntityManager}. They describe the document but do not
     * execute layout immediately.</p>
     *
     * @return the document's shared {@link ComponentBuilder}
     */
    ComponentBuilder componentBuilder();

    /**
     * Returns the canvas that defines the current document's base page geometry.
     *
     * <p>Root-level entities are ultimately resolved relative to this canvas and
     * its margin-adjusted inner box.</p>
     *
     * @return the backend-neutral {@link Canvas}
     */
    Canvas canvas();

    /**
     * Toggles markdown parsing for text-oriented builders used by this composer.
     *
     * @param enabled {@code true} to enable markdown parsing
     */
    void markdown(boolean enabled);

    /**
     * Toggles visual guide lines used to inspect resolved layout geometry.
     *
     * @param enabled {@code true} to render guide lines
     */
    void guideLines(boolean enabled);

    /**
     * Runs the full document pipeline and persists the result if the backend was
     * configured with an output target.
     *
     * <p>Implementations typically ensure builders are materialized, run layout,
     * then invoke one or more rendering/output systems.</p>
     *
     * @throws Exception if layout, rendering, or output fails
     */
    void build() throws Exception;

    /**
     * Runs the full document pipeline and returns the rendered document as bytes.
     *
     * <p>This path is useful for HTTP responses, tests, or pipelines that need an
     * in-memory representation instead of a file on disk.</p>
     *
     * @return the rendered document bytes
     * @throws Exception if layout or rendering fails
     */
    byte[] toBytes() throws Exception;
}

