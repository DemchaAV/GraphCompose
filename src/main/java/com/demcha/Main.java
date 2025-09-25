package com.demcha;

import com.demcha.components.components_builders.*;
import com.demcha.components.content.link.Email;
import com.demcha.components.content.link.LinkUrl;
import com.demcha.components.content.shape.Stroke;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.Anchor;
import com.demcha.components.style.ComponentColor;
import com.demcha.components.style.Margin;
import com.demcha.core.EntityManager;
import com.demcha.system.pdf_systems.PdfFileManagerSystem;
import com.demcha.system.pdf_systems.PdfLayoutSystem;
import com.demcha.system.pdf_systems.PdfRenderingSystem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) throws Exception {
        // 1. Setup Phase
        EntityManager entityManager = setupEntityManager(true);

        // 2. Content Creation and Layout
        createATableLayout(entityManager);

        // 3. Final Processing
        entityManager.processSystems();
    }

    /**
     * Initializes the EntityManager and registers all necessary systems.
     *
     * @return A configured EntityManager instance.
     */
    private static EntityManager setupEntityManager(boolean guidLines) throws Exception {
        Path target = Paths.get("output.pdf");
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage(PDRectangle.A4));

        EntityManager entityManager = new EntityManager();
        entityManager.setGuideLines(guidLines);

        entityManager.addSystem(new PdfLayoutSystem(doc.getPage(0)));
        entityManager.addSystem(new PdfRenderingSystem(doc));
        entityManager.addSystem(new PdfFileManagerSystem(target, doc));

        return entityManager;
    }

    /**
     * Creates and assembles the main layout of the PDF page.
     *
     * @param entityManager The entity manager to use for creating entities.
     */
    private static Entity createPageLayout(EntityManager entityManager) {
        // Create two vertical columns of links
        Entity leftColumn = createLinksColumn(entityManager);
        Entity rightColumn = createLinksColumn(entityManager);

        // Assemble them into a horizontal container
        return new HContainerBuilder(entityManager, Align.middle(20))
                .entityName("Horizontal Container")
                .anchor(Anchor.center())
                .addChild(leftColumn)
                .addChild(rightColumn)
                .build();
    }

    private static Entity createATableLayout(EntityManager entityManager) {
        var row1 = createPageLayout(entityManager);
        var row2 = createPageLayout(entityManager);
        return new VContainerBuilder(entityManager, Align.middle(20))
                .entityName("Main Vertical Container")
                .anchor(Anchor.center())
                .addChild(row1)
                .addChild(row2)
                .build();
    }

    /**
     * Creates a vertical container with a "Google" link and an "Email" link.
     *
     * @param entityManager The entity manager to use for creating entities.
     * @return The built container (Entity).
     */
    private static Entity createLinksColumn(EntityManager entityManager) {
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
        return new VContainerBuilder(entityManager, Align.middle(20))
                .margin(Margin.of(5))
                .anchor(Anchor.topCenter())
                .addChild(googleLink)
                .addChild(emailLink)
                .build();
    }

    // Note: The 'button' method is kept as it is well-defined, but it is not used
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
                .position(100, 100)
                .size(new ContentSize(90, 30))
                .build();
    }
}