package com.demcha.compose.layout_core.core;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.font_library.DefaultFonts;
import com.demcha.compose.font_library.FontFamilyDefinition;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.system.LayoutSystem;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfCanvas;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfFileManagerSystem;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.jetbrains.annotations.Nullable;

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
public final class PdfComposer extends AbstractDocumentComposer {

    private final PDDocument doc;
    private final PdfRenderingSystemECS renderingSystem;
    @Nullable
    private final Path outputFile; // null if output to bytes only

    /**
     * Package-private constructor. Use {@link GraphCompose#pdf(Path)} to create
     * instances.
     */
    public PdfComposer(Path outputFile, boolean markdown, boolean guideLines, PDRectangle pageSize, Margin margin,
            Collection<FontFamilyDefinition> customFontFamilies) {
        this(createState(markdown, guideLines, pageSize, margin, customFontFamilies), outputFile);
    }

    private PdfComposer(PdfComposerState state, @Nullable Path outputFile) {
        super(state.entityManager(), state.canvas());
        this.doc = state.doc();
        this.renderingSystem = state.renderingSystem();
        this.outputFile = outputFile;
        setupPdfSystems();
    }

    private static PdfComposerState createState(boolean markdown,
                                                boolean guideLines,
                                                PDRectangle pageSize,
                                                Margin margin,
                                                Collection<FontFamilyDefinition> customFontFamilies) {
        PDDocument doc = new PDDocument();
        EntityManager entityManager = new EntityManager(DefaultFonts.library(doc, customFontFamilies), markdown);
        entityManager.setGuideLines(guideLines);

        Canvas canvas = new PdfCanvas(pageSize, 0.0f, 0.0f);
        if (margin != null) {
            canvas.addMargin(margin);
        }

        PdfRenderingSystemECS renderingSystem = new PdfRenderingSystemECS(doc, canvas);
        return new PdfComposerState(doc, entityManager, canvas, renderingSystem);
    }

    private void setupPdfSystems() {
        entityManager().getSystems().addSystem(new LayoutSystem<>(canvas(), renderingSystem));
        entityManager().getSystems().addSystem(renderingSystem);

        if (outputFile != null) {
            entityManager().getSystems().addSystem(new PdfFileManagerSystem(outputFile, doc));
        }
    }

    /**
     * Adds a margin to the document canvas.
     *
     * @param margin The margin configuration to apply.
     */
    public void margin(Margin margin) {
        canvas().addMargin(margin);
    }

    public List<com.demcha.compose.font_library.FontName> availableFonts() {
        return List.copyOf(entityManager().getFonts().availableFonts());
    }

    @Override
    protected void buildDocument() throws Exception {
        entityManager().processSystems();
    }

    @Override
    protected byte[] exportBytes() throws Exception {
        processInMemoryRender();
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
        buildComponents();
        processInMemoryRender();

        return doc;
    }

    private void processInMemoryRender() throws Exception {
        entityManager().getSystems().getSystem(LayoutSystem.class)
                .ifPresent(sys -> sys.process(entityManager()));
        renderingSystem.process(entityManager());
    }

    @Override
    public void close() throws IOException {
        doc.close();
    }

    private record PdfComposerState(
            PDDocument doc,
            EntityManager entityManager,
            Canvas canvas,
            PdfRenderingSystemECS renderingSystem) {
    }
}


