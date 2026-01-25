package com.demcha;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.font_library.FontName;
import com.demcha.compose.loyaut_core.components.components_builders.Canvas;
import com.demcha.compose.loyaut_core.components.components_builders.HContainerBuilder;
import com.demcha.compose.loyaut_core.components.components_builders.TextBuilder;
import com.demcha.compose.loyaut_core.components.content.link.Email;
import com.demcha.compose.loyaut_core.components.content.link.LinkUrl;
import com.demcha.compose.loyaut_core.components.content.shape.Stroke;
import com.demcha.compose.loyaut_core.components.content.text.TextDecoration;
import com.demcha.compose.loyaut_core.components.content.text.TextStyle;
import com.demcha.compose.loyaut_core.components.core.Entity;
import com.demcha.compose.loyaut_core.components.geometry.ContentSize;
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static Anchor MAIN_ANCHOR = Anchor.topRight();
    static String textBlockData = "**Custom GeminiConnection (API Client Library)** – *Portfolio Project*\\nCreated a custom Java client to **integrate with a third-party solution** (Google's Gemini API). The library supports streaming responses and structured JSON parsing, demonstrating the ability to work effectively with external services and APIs.";

    public static void main(String[] args) throws Exception {
        // 1. Setup Phase

        Path target = Paths.get("output.pdf");
        GraphCompose compose = new GraphCompose(target);

        blockTextBuilder(compose, textBlockData, 400, 1);

        compose.build();



    }

    /**
     * Initializes the EntityManager and registers all necessary systems.
     *
     * @return A configured EntityManager instance.
     */
    private static EntityManager setupEntityManager(boolean guidLines) throws Exception {
        Path target = Paths.get("output.pdf");
        EntityManager entityManager = new EntityManager();
        PDDocument doc = new PDDocument();
        Canvas canvas = new PdfCanvas(PDRectangle.A4, 0.0f);
        canvas.addMargin(Margin.of(20));

        entityManager.setGuideLines(guidLines);

        PdfRenderingSystemECS renderingSystemECS = new PdfRenderingSystemECS(doc, canvas);
        entityManager.getSystems().addSystem(new LayoutSystem(canvas, renderingSystemECS));
        entityManager.getSystems().addSystem(renderingSystemECS);
        entityManager.getSystems().addSystem(new PdfFileManagerSystem(target, doc));

        return entityManager;
    }


    private static Entity createATableLayout(GraphCompose compose, String name) {


        var rows = List.of(
                createLinksColumn(compose, "leftColumn"),
                createLinksColumn(compose, "rightColumn")
        );
        var row1 = createVContainer(compose, "row1", rows)
                .addComponent(Anchor.center())
                .addComponent(Margin.zero());//delete a Margin from rows

        var rows2 = List.of(
                createLinksColumn(compose, "leftColumn"),
                createLinksColumn(compose, "rightColumn")
        );

        var row2 = createHContainer(compose, "row2", rows2)
                .addComponent(Anchor.center())
                .addComponent(Margin.zero()); //delete a Margin from rows

        var buttons = createButtonsVContainer(compose, "buttons");
        Entity blockTextBuilder = blockTextBuilder(compose, textBlockData, 400, 1);


        return createVContainer(compose, name, row1, row2, buttons, blockTextBuilder)
                .addComponent(MAIN_ANCHOR);
    }

    private static Entity createASingleObject(GraphCompose compose, String name) {
        var buttons = List.of(
                button(compose, "Button1"),
                button(compose, "Button2"),
                button(compose, "Button3"),
                button(compose, "Button4"));
        return createVContainer(compose, name, buttons);

    }

    private static Entity createButtonsVContainer(GraphCompose compose, String name) {
        var buttons = List.of(
                button(compose, "Button1"),
                button(compose, "Button2"));
        return createHContainer(compose, name, buttons);
    }

    /**
     * Creates a vertical container with a "Google" link and an "Email" link.
     *
     * @param compose The entity manager to use for creating entities.
     * @return The built container (Entity).
     */
    private static Entity createLinksColumn(GraphCompose compose, String name) {
        var googleLink = compose.componentBuilder().link()
                .linkUrl(new LinkUrl("https://www.google.com/"))
                .anchor(Anchor.center())
                .entityName("google")
                .displayText(compose.componentBuilder().displayUrlText()
                        .textWithAutoSize("Google")
                )
                .build();

        var emailLink = compose.componentBuilder().link()
                .linkUrl(new Email("demchaav@gmail.com", "Job Info hiring", "Dear Artem "))
                .anchor(Anchor.center())
                .entityName("email")
                .displayText(compose.componentBuilder().displayUrlText()
                        .textWithAutoSize("Email")
                )
                .build();

        // The method name is more descriptive now
        return createVContainer(compose, name, googleLink, emailLink);
    }

    // in this specific layout. You can call it from createPageLayout if needed.
    private static Entity button(GraphCompose compose, String buttonText) {
        return compose.componentBuilder().button()
                .text(compose.componentBuilder().text()
                        .textWithAutoSize(buttonText)
                        .textStyle(new TextStyle(FontName.DEFAULT, 10, TextDecoration.DEFAULT, ComponentColor.BLACK))
                        .anchor(Anchor.center())
                )
                .fillColor(ComponentColor.ROYAL_BLUE)
                .stroke(new Stroke(ComponentColor.MODULE_TITLE, 2.0))
                .size(new ContentSize(90, 30))
                .cornerRadius(5)
                .anchor(Anchor.center())
                .margin(Margin.of(5))
                .build();
    }

    private static Entity createVContainer(GraphCompose compose, String name, List<Entity> entities) {
        var vContainerBuilder = compose.componentBuilder().vContainer(Align.middle(5))
                .entityName(name)
                .margin(new Margin(10, 10, 5, 5))
                .padding(new Padding(5, 5, 10, 10))
                .anchor(Anchor.bottomLeft());
        entities.forEach(vContainerBuilder::addChild);
        return vContainerBuilder
                .build();
    }

    private static Entity createVContainer(GraphCompose compose, String name, Entity... entities) {
        return createVContainer(compose, name, Arrays.asList(entities));
    }

    private static Entity createHContainer(GraphCompose compose, String name, List<Entity> entities) {
        HContainerBuilder hContainerBuilder = compose.componentBuilder().hContainer(Align.middle(10))
                .entityName(name)
                .margin(new Margin(10, 20, 5, 15))
                .padding(Padding.of(5))
                .anchor(Anchor.bottomLeft());
        entities.forEach(hContainerBuilder::addChild);
        return hContainerBuilder
                .build();
    }

    private static Entity createHContainer(GraphCompose compose, String name, Entity... entities) {
        return createHContainer(compose, name, Arrays.asList(entities));
    }

    static Entity blockTextBuilder(GraphCompose compose, String text, double width, double spacing, String name) {
        TextBuilder textBuilder = compose.componentBuilder().text()
                .textWithAutoSize(text)
                .textStyle(TextStyle.builder()
                        .size(9)
                        .color(ComponentColor.TITLE)
                        .fontName(FontName.HELVETICA)
                        .decoration(TextDecoration.DEFAULT)
                        .build());


        var blockText = compose.componentBuilder().blockText(Align.middle(spacing), TextStyle.builder().build())
                .size(width, 2)
                .anchor(Anchor.center())
                .padding(0, 5, 0, 25)
                .margin(Margin.of(5))
                .text(textBuilder);

        if (name != null && !name.isEmpty()) {
            blockText.entityName(name);
        }

        return blockText.build();
    }

    static Entity blockTextBuilder(GraphCompose compose, String text, double width, double spacing) {
        return blockTextBuilder(compose, text, width, spacing, null);


    }


}