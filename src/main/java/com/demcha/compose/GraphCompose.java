package com.demcha.compose;

import com.demcha.compose.loyaut_core.components.ComponentBuilder;
import com.demcha.compose.loyaut_core.components.components_builders.Canvas;
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
import java.util.Objects;

public final class GraphCompose implements AutoCloseable {

    private final EntityManager entityManager;
    private final ComponentBuilder componentBuilder;

    private final PDDocument doc;
    private final Canvas canvas;
    private final PdfRenderingSystemECS renderingSystem;
    private final Path outputFile;

    public GraphCompose(Path outputFile) {
        this(outputFile, true, false, PDRectangle.A4);
    }

    public GraphCompose(Path outputFile, boolean markdown, boolean guideLines, PDRectangle pageSize) {
        this.outputFile = Objects.requireNonNull(outputFile, "outputFile");

        this.entityManager = new EntityManager(markdown);
        this.entityManager.setGuideLines(guideLines);

        this.doc = new PDDocument();
        this.canvas = new PdfCanvas(pageSize, 0.0f, 0.0f);

        this.renderingSystem = new PdfRenderingSystemECS(doc, canvas);

        setupSystems(this.renderingSystem, this.outputFile);

        this.componentBuilder = ComponentBuilder.builder(entityManager);
    }

    public void markdown(boolean setMarkdown) {
        this.entityManager.setMarkdown(setMarkdown);
    }

    public void guideLines(boolean setGuideLines) {
        this.entityManager.setGuideLines(setGuideLines);
    }

    public void margin(Margin margin) {
        canvas.addMargin(margin);
    }

    public ComponentBuilder componentBuilder() {
        return this.componentBuilder;
    }

    public EntityManager entityManager() {
        return this.entityManager;
    }

    private void setupSystems(PdfRenderingSystemECS renderingSystemECS, Path outputFile) {
        entityManager.getSystems().addSystem(new LayoutSystem<>(canvas, renderingSystemECS));
        entityManager.getSystems().addSystem(renderingSystemECS);
        entityManager.getSystems().addSystem(new PdfFileManagerSystem(outputFile, renderingSystemECS.doc()));
    }
    public void build() throws Exception {
        entityManager.processSystems();
    }


    @Override
    public void close() throws IOException {
        doc.close();
    }
}
