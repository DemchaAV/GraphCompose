package com.demcha.compose.loyaut_core.core;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.loyaut_core.components.ComponentBuilder;

import java.io.Closeable;

/**
 * Base interface for document composers supporting different output formats
 * (PDF, Word, etc.).
 * <p>
 * Implementations handle the lifecycle of creating, building, and saving
 * documents.
 * Use {@link GraphCompose} factory to obtain instances.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * 
 * <pre>
 * try (DocumentComposer composer = GraphCompose.pdf(outputPath).create()) {
 *     var builder = composer.componentBuilder();
 *     // ... build components
 *     composer.build();
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
