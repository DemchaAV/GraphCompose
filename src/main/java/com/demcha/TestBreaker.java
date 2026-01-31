package com.demcha;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.PdfComposer;
import com.demcha.compose.loyaut_core.components.components_builders.Canvas;
import com.demcha.compose.loyaut_core.components.components_builders.HContainerBuilder;
import com.demcha.compose.loyaut_core.components.content.shape.Stroke;
import com.demcha.compose.loyaut_core.components.core.Entity;
import com.demcha.compose.loyaut_core.components.layout.Align;
import com.demcha.compose.loyaut_core.components.layout.Anchor;
import com.demcha.compose.loyaut_core.components.style.ComponentColor;
import com.demcha.compose.loyaut_core.components.style.Margin;
import com.demcha.compose.loyaut_core.components.style.Padding;
import com.demcha.compose.loyaut_core.core.EntityManager;
import com.demcha.compose.loyaut_core.system.LayoutSystem;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfCanvas;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfFileManagerSystem;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.demcha.Main.blockTextBuilder;

public class TestBreaker {

    public static Anchor MAIN_ANCHOR = Anchor.topCenter();
    public static Margin MARGIN = Margin.of(50);

    public static void main(String[] args) throws Exception {
        Path target = Paths.get("output.pdf");
        PdfComposer compose = GraphCompose.pdf(target).create();
        // EntityManager entityManager = setupEntityManager_16_16(true);

        boolean testText = true;
        // testText = false;

        List<Entity> data;
        if (testText) {
            data = textBuilding(compose, 95, 3);

        } else {
            data = colorObjectBuilding(compose);
        }

        var container = createVContainer(compose, "mainContainer V",
                data);

        // 3. Final Processing
        compose.build();

    }

    @NotNull
    private static List<Entity> colorObjectBuilding(PdfComposer compose) {
        List<Entity> data = new ArrayList<>();
        var colors = List.of(
                Color.BLUE,
                Color.darkGray,
                Color.green,
                Color.red,
                Color.yellow

        );

        for (int i = 0; i < 5; i++) {
            var entity = compose.componentBuilder().rectangle()
                    .size(300, 300)
                    .stroke(new Stroke(ComponentColor.PURPLE, 2))
                    .fillColor(colors.get(i))
                    .entityName("Rectangle " + i).build();
            data.add(entity);

        }
        return data;
    }

    @NotNull
    private static List<Entity> textBuilding(PdfComposer entityManager, int rows, double spacing) {
        double w = 302;
        List<Entity> data = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= rows; i++) {
            sb.append(i);
            sb.append(". ");
            sb.append("Test text line  We will se how we break our text in to the pages\n");

            if (i > 0 && i % 20 == 0) {
                String string = sb.toString();
                data.add(blockTextBuilder(entityManager, string, w, spacing, "textBlockData" + i / 20));
                sb = new StringBuilder();
            }
            if (i == rows && !sb.isEmpty()) {
                String string = sb.toString();
                data.add(blockTextBuilder(entityManager, string, w, spacing, "textBlockData" + i / 20));
                sb = new StringBuilder();
            }

        }
        return data;
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

    private static EntityManager setupEntityManager_16_16(boolean guidLines) throws Exception {
        Path target = Paths.get("output_break.pdf");
        PDDocument doc = new PDDocument();
        Canvas canvas = new PdfCanvas(new PDRectangle(0.0f, 0.0f, 160.0f, 160.0f), 0.0f);

        canvas.addMargin(MARGIN);

        EntityManager entityManager = new EntityManager();
        entityManager.setGuideLines(guidLines);

        PdfRenderingSystemECS renderingSystemECS = new PdfRenderingSystemECS(doc, canvas);
        entityManager.getSystems().addSystem(new LayoutSystem(canvas, renderingSystemECS));
        entityManager.getSystems().addSystem(renderingSystemECS);
        entityManager.getSystems().addSystem(new PdfFileManagerSystem(target, doc));

        return entityManager;
    }

    private static Entity createVContainer(PdfComposer compose, String name, List<Entity> entities) {
        var vContainerBuilder = compose.componentBuilder().vContainer(Align.middle(10))
                .entityName(name)
                .margin(new Margin(10, 10, 5, 5))
                .padding(new Padding(5, 5, 10, 10))
                .anchor(MAIN_ANCHOR);
        entities.forEach(vContainerBuilder::addChild);
        return vContainerBuilder
                .build();
    }

    private static Entity createVContainer(PdfComposer compose, String name, Entity... entities) {
        return createVContainer(compose, name, Arrays.asList(entities));
    }

    private static Entity createHContainer(PdfComposer compose, String name, List<Entity> entities) {
        HContainerBuilder hContainerBuilder = compose.componentBuilder().hContainer(Align.middle(10))
                .entityName(name)
                .margin(new Margin(10, 20, 5, 15))
                .padding(Padding.of(5))
                .anchor(Anchor.bottomLeft());
        entities.forEach(hContainerBuilder::addChild);
        return hContainerBuilder
                .build();
    }

    private static Entity createHContainer(PdfComposer compose, String name, Entity... entities) {
        return createHContainer(compose, name, Arrays.asList(entities));
    }
}
