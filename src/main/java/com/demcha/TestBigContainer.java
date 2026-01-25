package com.demcha;

import com.demcha.compose.loyaut_core.components.components_builders.Canvas;
import com.demcha.compose.loyaut_core.components.components_builders.VContainerBuilder;
import com.demcha.compose.loyaut_core.components.layout.Align;
import com.demcha.compose.loyaut_core.components.layout.Anchor;
import com.demcha.compose.loyaut_core.components.style.Margin;
import com.demcha.compose.loyaut_core.components.style.Padding;
import com.demcha.compose.loyaut_core.core.EntityManager;
import com.demcha.compose.loyaut_core.system.LayoutSystem;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfCanvas;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfFileManagerSystem;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestBigContainer {

    public static Anchor MAIN_ANCHOR = Anchor.topRight();
    public static Margin MARGIN = Margin.of(10);
    public static Padding PADDING = Padding.of(5);

    public static void main(String[] args) throws Exception {

        EntityManager entityManager = setupEntityManager(true);
        container(entityManager, 500, 2100);
        entityManager.processSystems();


    }

    private static void container(EntityManager entityManager, double width, double height) {
        VContainerBuilder containerBuilder = new VContainerBuilder(entityManager, Align.middle(2));
        containerBuilder
                .size(width, height)
                .anchor(Anchor.topCenter()).build()
                .addComponent(MARGIN)
                .addComponent(PADDING)
        ;
    }

    private static EntityManager setupEntityManager(boolean guidLines) throws Exception {
        Path target = Paths.get("output_break.pdf");
        PDDocument doc = new PDDocument();
        Canvas canvas = new PdfCanvas(PDRectangle.A4, 0.0f);

        canvas.addMargin(MARGIN);

        EntityManager entityManager = new EntityManager();
        entityManager.setGuideLines(guidLines);

        PdfRenderingSystemECS renderingSystemECS = new PdfRenderingSystemECS(doc, canvas);
        entityManager.getSystems().addSystem(new LayoutSystem(canvas, renderingSystemECS));
        entityManager.getSystems().addSystem(renderingSystemECS);
        entityManager.getSystems().addSystem(new PdfFileManagerSystem(target, doc));

        return entityManager;
    }
}
