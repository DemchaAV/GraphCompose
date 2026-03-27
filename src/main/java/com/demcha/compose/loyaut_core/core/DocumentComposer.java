package com.demcha.compose.loyaut_core.core;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.loyaut_core.components.components_builders.ComponentBuilder;

import java.io.Closeable;

/**
 * Base interface for document composers supporting different output formats
 * (PDF, Word, etc.).
 * <p>
 * Implementations handle the lifecycle of creating, building, and exporting
 * documents. Use {@link GraphCompose} to obtain instances.
 * </p>
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
     * Returns the component builder for creating entities.
     *
     * @return The {@link ComponentBuilder} instance.
     */
    ComponentBuilder componentBuilder();

    /**
     * Returns the underlying Entity Manager.
     *
     * @return The {@link EntityManager} instance.
     */
    EntityManager entityManager();

    /**
     * Toggles Markdown parsing for text entities.
     *
     * @param enabled true to enable markdown, false to disable.
     */
    void markdown(boolean enabled);

    /**
     * Toggles visual guide lines for debugging layout.
     *
     * @param enabled true to render guide lines, false to hide them.
     */
    void guideLines(boolean enabled);

    /**
     * Processes all systems to layout, render, and save the document.
     *
     * @throws Exception if an error occurs during processing.
     */
    void build() throws Exception;

    /**
     * Builds the document and returns it as a byte array.
     * Does NOT save to file.
     *
     * @return The document content as bytes.
     * @throws Exception if an error occurs during processing.
     */
    byte[] toBytes() throws Exception;
}

