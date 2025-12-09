package com.demcha;

import com.demcha.font_library.FontName;
import com.demcha.loyaut_core.components.components_builders.*;
import com.demcha.loyaut_core.components.content.link.Email;
import com.demcha.loyaut_core.components.content.link.LinkUrl;
import com.demcha.loyaut_core.components.content.shape.Stroke;
import com.demcha.loyaut_core.components.content.text.TextDecoration;
import com.demcha.loyaut_core.components.content.text.TextStyle;
import com.demcha.loyaut_core.components.core.Entity;
import com.demcha.loyaut_core.components.geometry.ContentSize;
import com.demcha.loyaut_core.components.layout.Align;
import com.demcha.loyaut_core.components.layout.Anchor;
import com.demcha.loyaut_core.components.style.ComponentColor;
import com.demcha.loyaut_core.components.style.Margin;
import com.demcha.loyaut_core.components.style.Padding;
import com.demcha.loyaut_core.core.EntityManager;
import com.demcha.loyaut_core.system.LayoutSystem;
import com.demcha.loyaut_core.system.implemented_systems.pdf_systems.PdfCanvas;
import com.demcha.loyaut_core.system.implemented_systems.pdf_systems.PdfFileManagerSystem;
import com.demcha.loyaut_core.system.implemented_systems.pdf_systems.PdfFont;
import com.demcha.loyaut_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import com.demcha.loyaut_core.system.utils.page_breaker.EntitySorter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static Anchor MAIN_ANCHOR = Anchor.topRight();
    static String textBlockData = "Junior Java Backend Developer with hands-on experience building REST APIs using Spring Boot, Spring " +
                                  "    Data JPA, and JWT-based Security. Transitioning from an engineering leadership background with a " +
                                  "    strong foundation in Core/Advanced Java, SQL, and object-oriented design. Passionate about clean " +
                                  "    code, robust architecture, and solving real-world problems. Ready to contribute to modern backend " +
                                  "    development projects and grow within a collaborative team. ";

    public static void main(String[] args) throws Exception {
        // 1. Setup Phase
        EntityManager entityManager = setupEntityManager(true);


        // 2. Content Creation and Layout
//        createATableLayout(entityManager, "table");
        blockTextBuilder(entityManager, textBlockData, 400, 1);
//
//        createASingleObject(entityManager, "Hello");
//        createButtonsVContainer(entityManager, "buttons");


        // 3. Final Processing
        entityManager.processSystems();
        var pageBreaker = EntitySorter.sortByYPosition(entityManager.getEntities());
    }

    /**
     * Initializes the EntityManager and registers all necessary systems.
     *
     * @return A configured EntityManager instance.
     */
    private static EntityManager setupEntityManager(boolean guidLines) throws Exception {
        Path target = Paths.get("output.pdf");
        PDDocument doc = new PDDocument();
        Canvas canvas = new PdfCanvas(PDRectangle.A4, 0.0f);
        canvas.addMargin(Margin.of(20));

        EntityManager entityManager = new EntityManager();
        entityManager.setGuideLines(guidLines);

        PdfRenderingSystemECS renderingSystemECS = new PdfRenderingSystemECS(doc, canvas);
        entityManager.getSystems().addSystem(new LayoutSystem(canvas, renderingSystemECS));
        entityManager.getSystems().addSystem(renderingSystemECS);
        entityManager.getSystems().addSystem(new PdfFileManagerSystem(target, doc));

        return entityManager;
    }


    private static Entity createATableLayout(EntityManager entityManager, String name) {


        var rows = List.of(
                createLinksColumn(entityManager, "leftColumn"),
                createLinksColumn(entityManager, "rightColumn")
        );
        var row1 = createVContainer(entityManager, "row1", rows)
                .addComponent(Anchor.center())
                .addComponent(Margin.zero());//delete a Margin from rows

        var rows2 = List.of(
                createLinksColumn(entityManager, "leftColumn"),
                createLinksColumn(entityManager, "rightColumn")
        );

        var row2 = createHContainer(entityManager, "row2", rows2)
                .addComponent(Anchor.center())
                .addComponent(Margin.zero()); //delete a Margin from rows

        var buttons = createButtonsVContainer(entityManager, "buttons");
        Entity blockTextBuilder = blockTextBuilder(entityManager, textBlockData, 400, 1);


        return createVContainer(entityManager, name, row1, row2, buttons, blockTextBuilder)
                .addComponent(MAIN_ANCHOR);
    }

    private static Entity createASingleObject(EntityManager entityManager, String name) {
        var buttons = List.of(
                button(entityManager, "Button1"),
                button(entityManager, "Button2"),
                button(entityManager, "Button3"),
                button(entityManager, "Button4"));
        return createVContainer(entityManager, name, buttons);

    }

    private static Entity createButtonsVContainer(EntityManager entityManager, String name) {
        var buttons = List.of(
                button(entityManager, "Button1"),
                button(entityManager, "Button2"));
        return createHContainer(entityManager, name, buttons);
    }

    /**
     * Creates a vertical container with a "Google" link and an "Email" link.
     *
     * @param entityManager The entity manager to use for creating entities.
     * @return The built container (Entity).
     */
    private static Entity createLinksColumn(EntityManager entityManager, String name) {
        var googleLink = new LinkBuilder(entityManager)
                .linkUrl(new LinkUrl("https://www.google.com/"))
                .anchor(Anchor.center())
                .entityName("google")
                .displayText(new DisplayUrlTextBuilder(entityManager)
                        .textWithAutoSize("Google")
                )
                .build();

        var emailLink = new LinkBuilder(entityManager)
                .linkUrl(new Email("demchaav@gmail.com", "Job Info hiring", "Dear Artem "))
                .anchor(Anchor.center())
                .entityName("email")
                .displayText(new DisplayUrlTextBuilder(entityManager)
                        .textWithAutoSize("Email")
                )
                .build();

        // The method name is more descriptive now
        return createVContainer(entityManager, name, googleLink, emailLink);
    }

    // in this specific layout. You can call it from createPageLayout if needed.
    private static Entity button(EntityManager entityManager, String buttonText) {
        return new ButtonBuilder(entityManager)
                .text(new TextBuilder(entityManager)
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

    private static Entity createVContainer(EntityManager entityManager, String name, List<Entity> entities) {
        var vContainerBuilder = new VContainerBuilder(entityManager, Align.middle(5))
                .entityName(name)
                .margin(new Margin(10, 10, 5, 5))
                .padding(new Padding(5, 5, 10, 10))
                .anchor(Anchor.bottomLeft());
        entities.forEach(vContainerBuilder::addChild);
        return vContainerBuilder
                .build();
    }

    private static Entity createVContainer(EntityManager entityManager, String name, Entity... entities) {
        return createVContainer(entityManager, name, Arrays.asList(entities));
    }

    private static Entity createHContainer(EntityManager entityManager, String name, List<Entity> entities) {
        HContainerBuilder hContainerBuilder = new HContainerBuilder(entityManager, Align.middle(10))
                .entityName(name)
                .margin(new Margin(10, 20, 5, 15))
                .padding(Padding.of(5))
                .anchor(Anchor.bottomLeft());
        entities.forEach(hContainerBuilder::addChild);
        return hContainerBuilder
                .build();
    }

    private static Entity createHContainer(EntityManager entityManager, String name, Entity... entities) {
        return createHContainer(entityManager, name, Arrays.asList(entities));
    }

    static Entity blockTextBuilder(EntityManager entityManager, String text, double width, double spacing, String name) {
        TextBuilder textBuilder = new TextBuilder(entityManager)
                .textWithAutoSize(text)
                .textStyle(TextStyle.builder()
                        .size(9)
                        .color(ComponentColor.TITLE)
                        .fontName(FontName.HELVETICA)
                        .decoration(TextDecoration.UNDERLINE)
                        .build());


        var blockText = new BlockTextBuilder(entityManager, Align.middle(spacing), TextStyle.builder().build() ){
        }                .size(width, 2)
                .anchor(Anchor.center())
                .padding(0, 5, 0, 25)
                .margin(Margin.of(5))
                .text(textBuilder);
        if (name!=null && !name.isEmpty()) {
            blockText.entityName(name);
        }

        return blockText.build();
    }

    static Entity blockTextBuilder(EntityManager entityManager, String text, double width, double spacing) {
        return blockTextBuilder(entityManager, text, width, spacing, null);


    }


}