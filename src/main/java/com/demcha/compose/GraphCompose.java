package com.demcha.compose;

import com.demcha.compose.loyaut_core.components.ComponentBuilder;
import com.demcha.compose.loyaut_core.components.components_builders.Canvas;
import com.demcha.compose.loyaut_core.components.containers.abstract_builders.EntityBuilderBase;
import com.demcha.compose.loyaut_core.components.style.Margin;
import com.demcha.compose.loyaut_core.core.EntityManager;
import com.demcha.compose.loyaut_core.system.LayoutSystem;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfCanvas;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfFileManagerSystem;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The main entry point for composing and generating PDF documents using the GraphCompose framework.
 * <p>
 * This class orchestrates the Entity Component System (ECS), manages the PDF document lifecycle,
 * and configures the layout and rendering systems. It implements {@link AutoCloseable} to ensure
 * the underlying {@link PDDocument} is closed properly.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>
 * try (GraphCompose compose = new GraphCompose(outputPath)) {
 *     compose.componentBuilder().addText("Hello World");
 *     compose.build();
 * }
 * </pre>
 */
public final class GraphCompose implements AutoCloseable {

    private final EntityManager entityManager;
    private final ComponentBuilder componentBuilder;


    private final PDDocument doc;
    private final Canvas canvas;
    private final PdfRenderingSystemECS renderingSystem;
    private final Path outputFile;

    /**
     * Creates a new GraphCompose instance with default settings:
     * A4 page size, Markdown enabled, and Guide Lines disabled.
     *
     * @param outputFile The path where the generated PDF will be saved.
     */
    public GraphCompose(Path outputFile) {
        this(outputFile, true, false, PDRectangle.A4);
    }

    /**
     * Initializes a new GraphCompose instance with specific configuration.
     *
     * @param outputFile The path where the generated PDF will be saved.
     * @param markdown   If true, enables Markdown parsing for text components.
     * @param guideLines If true, renders visual guide lines for debugging layout boundaries.
     * @param pageSize   The size of the PDF pages (e.g., {@link PDRectangle#A4}).
     */
    public GraphCompose(Path outputFile, boolean markdown, boolean guideLines, PDRectangle pageSize) {
        this.outputFile = Objects.requireNonNull(outputFile, "outputFile");

        this.entityManager = new EntityManager(markdown);
        this.entityManager.setGuideLines(guideLines);

        this.doc = new PDDocument();
        this.canvas = new PdfCanvas(pageSize, 0.0f, 0.0f);

        this.renderingSystem = new PdfRenderingSystemECS(doc, canvas);

        setupPdfSystems(this.renderingSystem, this.outputFile);

        this.componentBuilder = ComponentBuilder.builder(entityManager);
    }

    /**
     * Toggles Markdown parsing for text entities.
     *
     * @param setMarkdown true to enable markdown, false to disable.
     */
    public void markdown(boolean setMarkdown) {
        this.entityManager.setMarkdown(setMarkdown);
    }

    /**
     * Toggles visual guide lines for debugging layout.
     *
     * @param setGuideLines true to render guide lines, false to hide them.
     */
    public void guideLines(boolean setGuideLines) {
        this.entityManager.setGuideLines(setGuideLines);
    }

    /**
     * Adds a margin to the document canvas.
     *
     * @param margin The margin configuration to apply.
     */
    public void margin(Margin margin) {
        canvas.addMargin(margin);
    }

    /**
     * Returns the builder used to create and attach components to the document.
     *
     * @return The {@link ComponentBuilder} instance.
     */
    public ComponentBuilder componentBuilder() {
        return this.componentBuilder;
    }

    /**
     * Returns the underlying Entity Manager.
     *
     * @return The {@link EntityManager} instance.
     */
    public EntityManager entityManager() {
        return this.entityManager;
    }

    private void setupPdfSystems(PdfRenderingSystemECS renderingSystemECS, Path outputFile) {
        entityManager.getSystems().addSystem(new LayoutSystem<>(canvas, renderingSystemECS));
        entityManager.getSystems().addSystem(renderingSystemECS);
        entityManager.getSystems().addSystem(new PdfFileManagerSystem(outputFile, renderingSystemECS.doc()));
    }

    /**
     * Processes all registered ECS systems to layout, render, and save the PDF document.
     *
     * @throws Exception if an error occurs during layout, rendering, or file I/O.
     */
    public void build() throws Exception {
        componentBuilder.buildsComponents();
        entityManager.processSystems();
    }


    @Override
    /**
     * Closes the underlying {@link PDDocument} to release resources.
     *
     * @throws IOException if an error occurs while closing the document.
     */
    public void close() throws IOException {
        doc.close();
    }
}
