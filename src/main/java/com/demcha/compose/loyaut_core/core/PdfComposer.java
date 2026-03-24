package com.demcha.compose.loyaut_core.core;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.font_library.DefaultFonts;
import com.demcha.compose.font_library.FontFamilyDefinition;
import com.demcha.compose.loyaut_core.components.ComponentBuilder;
import com.demcha.compose.loyaut_core.components.components_builders.Canvas;
import com.demcha.compose.loyaut_core.components.style.Margin;
import com.demcha.compose.loyaut_core.system.LayoutSystem;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfCanvas;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfFileManagerSystem;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * PDF implementation of {@link DocumentComposer}.
 * <p>
 * Orchestrates the Entity Component System (ECS), manages the PDF document
 * lifecycle, and configures the layout and rendering systems.
 * </p>
 *
 * <h3>Build to file</h3>
 * <pre>
 * try (var composer = GraphCompose.pdf(outputPath).create()) {
 *     composer.componentBuilder()
 *             .text()
 *             .textWithAutoSize("Hello GraphCompose")
 *             .textStyle(TextStyle.DEFAULT_STYLE)
 *             .anchor(Anchor.topLeft())
 *             .build();
 *
 *     composer.build();
 * }
 * </pre>
 *
 * <h3>Or get bytes directly</h3>
 * <pre>
 * try (var composer = GraphCompose.pdf()
 *         .pageSize(PDRectangle.A4)
 *         .create()) {
 *
 *     composer.componentBuilder()
 *             .text()
 *             .textWithAutoSize("In-memory PDF")
 *             .textStyle(TextStyle.DEFAULT_STYLE)
 *             .anchor(Anchor.topLeft())
 *             .build();
 *
 *     byte[] pdfBytes = composer.toBytes();
 * }
 * </pre>
 */
public final class PdfComposer implements DocumentComposer {

    private final EntityManager entityManager;
    private final ComponentBuilder componentBuilder;
    private final PDDocument doc;
    private final Canvas canvas;
    private final PdfRenderingSystemECS renderingSystem;
    private final Path outputFile; // null if output to bytes only

    /**
     * Package-private constructor. Use {@link GraphCompose#pdf(Path)} to create
     * instances.
     */
    public PdfComposer(Path outputFile, boolean markdown, boolean guideLines, PDRectangle pageSize, Margin margin,
            Collection<FontFamilyDefinition> customFontFamilies) {
        this.outputFile = outputFile;

        this.doc = new PDDocument();
        this.entityManager = new EntityManager(DefaultFonts.library(doc, customFontFamilies), markdown);
        this.entityManager.setGuideLines(guideLines);
        this.canvas = new PdfCanvas(pageSize, 0.0f, 0.0f);

        if (margin != null) {
            this.canvas.addMargin(margin);
        }

        this.renderingSystem = new PdfRenderingSystemECS(doc, canvas);
        setupPdfSystems();

        this.componentBuilder = ComponentBuilder.builder(entityManager);
    }

    private void setupPdfSystems() {
        entityManager.getSystems().addSystem(new LayoutSystem<>(canvas, renderingSystem));
        entityManager.getSystems().addSystem(renderingSystem);

        if (outputFile != null) {
            entityManager.getSystems().addSystem(new PdfFileManagerSystem(outputFile, doc));
        }
    }

    @Override
    public ComponentBuilder componentBuilder() {
        return this.componentBuilder;
    }

    @Override
    public EntityManager entityManager() {
        return this.entityManager;
    }

    @Override
    public void markdown(boolean enabled) {
        this.entityManager.setMarkdown(enabled);
    }

    @Override
    public void guideLines(boolean enabled) {
        this.entityManager.setGuideLines(enabled);
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
     * Returns the canvas for this PDF document.
     *
     * @return The {@link Canvas} instance.
     */
    public Canvas canvas() {
        return this.canvas;
    }

    public List<com.demcha.compose.font_library.FontName> availableFonts() {
        return List.copyOf(entityManager.getFonts().availableFonts());
    }

    @Override
    public void build() throws Exception {
        componentBuilder.buildsComponents();
        entityManager.processSystems();
    }

    @Override
    public byte[] toBytes() throws Exception {
        toPDDocument(); // Reuse common processing logic

        // Write PDF to byte array
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Builds the document and returns the underlying {@link PDDocument}.
     * <p>
     * Use this when you need direct access to the PDDocument for further
     * manipulation without writing to the file system.
     * </p>
     * <p>
     * <b>Important:</b> The caller is responsible for closing the document
     * when done, or use this within a try-with-resources on the PdfComposer.
     * </p>
     *
     * @return The processed {@link PDDocument} instance.
     * @throws Exception if an error occurs during processing.
     */
    public PDDocument toPDDocument() throws Exception {
        componentBuilder.buildsComponents();

        // Process layout and rendering systems (but not file manager)
        entityManager.getSystems().getSystem(LayoutSystem.class)
                .ifPresent(sys -> sys.process(entityManager));
        renderingSystem.process(entityManager);

        return doc;
    }

    @Override
    public void close() throws IOException {
        doc.close();
    }
}
