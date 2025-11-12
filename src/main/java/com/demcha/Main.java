package com.demcha;

import com.demcha.components.components_builders.*;
import com.demcha.components.content.link.Email;
import com.demcha.components.content.link.LinkUrl;
import com.demcha.components.content.shape.Stroke;
import com.demcha.components.content.text.TextDecoration;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.Anchor;
import com.demcha.components.style.ComponentColor;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;
import com.demcha.system.LayoutSystemImpl;
import com.demcha.system.pdf_systems.PdfFileManagerSystem;
import com.demcha.system.pdf_systems.PdfRenderingSystemECS;
import com.demcha.utils.page_brecker.PageBreaker;
import com.demcha.utils.page_brecker.PdfCanvas;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class Main {
    static String textBlockData = "Junior Java Backend Developer with hands-on experience building REST APIs using Spring Boot, Spring " +
                                  "    Data JPA, and JWT-based Security. Transitioning from an engineering leadership background with a " +
                                  "    strong foundation in Core/Advanced Java, SQL, and object-oriented design. Passionate about clean " +
                                  "    code, robust architecture, and solving real-world problems. Ready to contribute to modern backend " +
                                  "    development projects and grow within a collaborative team. ";

    public static void main(String[] args) throws Exception {
        // 1. Setup Phase
        EntityManager entityManager = setupEntityManager(true);

        // 2. Content Creation and Layout
        createATableLayout(entityManager, "table");
//        createButtonsVContainer(entityManager, "buttons");


        // 3. Final Processing
        entityManager.processSystems();
        var pageBreaker = PageBreaker.sortByYPosition(entityManager.getEntities());
    }

    /**
     * Initializes the EntityManager and registers all necessary systems.
     *
     * @return A configured EntityManager instance.
     */
    private static EntityManager setupEntityManager(boolean guidLines) throws Exception {
        Path target = Paths.get("output.pdf");
        PDDocument doc = new PDDocument();
        Canvas canvasSize = new PdfCanvas(PDRectangle.A4, 0.0f);
        canvasSize.addMargin(Margin.of(10));

        EntityManager entityManager = new EntityManager();
        entityManager.setGuideLines(guidLines);

        entityManager.addSystem(new LayoutSystemImpl(canvasSize));
        entityManager.addSystem(new PdfRenderingSystemECS(doc, canvasSize));
        entityManager.addSystem(new PdfFileManagerSystem(target, doc));

        return entityManager;
    }


    private static Entity createATableLayout(EntityManager entityManager, String name) {

        Entity row1LeftColumn = createLinksColumn(entityManager, "leftColumn");
        Entity row1RightColumn = createLinksColumn(entityManager, "rightColumn");
        var row1 = createVContainer(entityManager, "row1", row1LeftColumn, row1RightColumn)
                .addComponent(Margin.zero());//delete a Margin from rows

        Entity row2LeftColumn = createLinksColumn(entityManager, "leftColumn");
        Entity row2RightColumn = createLinksColumn(entityManager, "rightColumn");
        var row2 = createHContainer(entityManager, "row2", row2LeftColumn, row2RightColumn)
                .addComponent(Margin.zero()); //delete a Margin from rows

        var buttons = createButtonsVContainer(entityManager, "buttons");
        Entity blockTextBuilder = blockTextBuilder(entityManager, textBlockData, 400, 1);


        return createVContainer(entityManager, name, row1, row2, buttons, blockTextBuilder)
                .addComponent(Anchor.topRight()) ;
    }

    private static Entity createButtonsVContainer(EntityManager entityManager, String name) {
        var button1 = button(entityManager, "button1");
        var button2 = button(entityManager, "button2");
        return createVContainer(entityManager, name, button1, button2);
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
                        .textStyle(TextStyle.defaultStyle())
                        .anchor(Anchor.center())
                )
                .fillColor(ComponentColor.ROYAL_BLUE)
                .stroke(new Stroke(ComponentColor.MODULE_TITLE, 2.0))
                .size(new ContentSize(90, 30))
                .cornerRadius(5)
                .build();
    }

    private static Entity createVContainer(EntityManager entityManager, String name, Entity... entities) {
        VContainerBuilder vContainerBuilder = new VContainerBuilder(entityManager, Align.middle(10))
                .entityName(name)
                .margin(Margin.of(5))
                .padding(Padding.of(5))
                .anchor(Anchor.center());
        Arrays.stream(entities).forEach(vContainerBuilder::addChild);
        return vContainerBuilder
                .build();
    }

    private static Entity createHContainer(EntityManager entityManager, String name, Entity... entities) {
        HContainerBuilder hContainerBuilder = new HContainerBuilder(entityManager, Align.middle(10))
                .entityName(name)
                .margin(Margin.of(5))
                .padding(Padding.of(5))
                .anchor(Anchor.center());
        Arrays.stream(entities).forEach(hContainerBuilder::addChild);
        return hContainerBuilder
                .build();
    }

    private static Entity blockTextBuilder(EntityManager entityManager, String text, double width, double spacing) {
        TextBuilder textBuilder = new TextBuilder(entityManager)
                .textWithAutoSize(text)
                .textStyle(TextStyle.builder()
                        .size(9)
                        .color(ComponentColor.TITLE)
                        .font(new PDType1Font(Standard14Fonts.FontName.HELVETICA))
                        .decoration(TextDecoration.UNDERLINE)
                        .build());


        var blockText = new BlockTextBuilder(entityManager, Align.middle(spacing))
                .size(width, 2)
                .anchor(Anchor.center())
                .padding(0, 5, 0, 25)
                .margin(Margin.of(5))
                .text(textBuilder);

        return blockText.build();

    }


}