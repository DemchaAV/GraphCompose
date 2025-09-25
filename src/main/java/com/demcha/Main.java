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
import com.demcha.core.EntityManager;
import com.demcha.system.PdfFileManagerSystem;
import com.demcha.system.PdfLayoutSystem;
import com.demcha.system.PdfRenderingSystem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        Path target = Paths.get("output.pdf");

        EntityManager entityManager = new EntityManager();
        entityManager.setGuideLines(true);
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage(PDRectangle.A4));


        entityManager.addSystem(new PdfLayoutSystem(doc.getPage(0)));
        entityManager.addSystem(new PdfRenderingSystem(doc));
        entityManager.addSystem(new PdfFileManagerSystem(target, doc));


        Entity links = links_mail(entityManager);
//        Entity links2= links_mail(entityManager).addComponent(Anchor.topCenter());
//        Entity entity = blockTextBuilder(entityManager);
//        Entity blockText = textTest(entityManager);
        Entity button1 = button(entityManager, "Button1");
        Entity button2 = button(entityManager, "Button2");
                var vBox = new VContainerBuilder(entityManager, Align.middle(20))
                .entityName("V Main Container")
                .anchor(Anchor.center())
                .addChild(button1)
                .addChild(button2)
                .build();

//
//
//        var vBox = new VContainerBuilder(entityManager, Align.middle(20))
//                .entityName("V Main Container")
//                .anchor(Anchor.center())
//                .addChild(blockText)
//                .addChild(links)
//                .addChild(links2)
//                .addChild(entity)
//                .addChild(button)
//                .build();


        entityManager.processSystems();


    }

    private static Entity button(EntityManager entityManager, String buttonText) {
        var button = new ButtonBuilder(entityManager)
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
        return button;
    }

    private static Entity links_mail(EntityManager entityManager) {
        var google = new LinkBuilder(entityManager)
                .linkUrl(new LinkUrl("https://www.google.com/"))
                .anchor(Anchor.center())
                .entityName("google")
                .displayText(new DisplayUrlTextBuilder(entityManager)
                        .textWithAutoSize("Google")
                )
                .build();
        var email = new LinkBuilder(entityManager)
                .linkUrl(new Email("demchaav@gmail.com", "Job Info hiring", "Dear Artem "))
                .anchor(Anchor.center())
                .entityName("email")
                .displayText(new DisplayUrlTextBuilder(entityManager)
                        .textWithAutoSize("Email")
                )
                .build();

        var row = new VContainerBuilder(entityManager, Align.middle(20))
                .margin(Margin.of(5))
                .anchor(Anchor.center())
                .addChild(google)
                .addChild(email)
                .build();
        return row;
    }

    private static void ChunkTextBuilder(EntityManager entityManager) {
        TextBuilder textBuilder = new TextBuilder(entityManager)
                .textWithAutoSize("Trigger the long word warning. Integer nec odio. Praesent libero. lectus risus, iaculis vel, suscipit quis, " +
                                  "luctus non, massa. Fusce Vestibulum facilisis, nunc in hendrerit posuere, sapien magna Vestibulum lacinia arcu eget " +
                                  "nulla. Class aptent taciti sociosqu Sed cursus ante dapibus diam. Sed nisi. Nulla quis sem at nibh elementum imperdiet. " +
                                  "Duis sagittis ipsum. Praesent mauris. metus, ullamcorper vel, tincidunt sed, euismod in, nibh. Curabitur tortor. " +
                                  "Pellentesque nibh. Aenean quam. In ad litora torquent per conubia nostra, per inceptos himenaeos. " +
                                  "Fusce nec tellus sed augue semper porta. Mauris massa. pellentesque nunc, ac vehicula eros elit vitae nisl. t" +
                                  "ristique sem. Proin ut ligula vel nunc egestas porttitor. Morbi Supercalifragilisticexpialidocious is a very long " +
                                  "word that should ac turpis quis ligula lacinia aliquet. Mauris ipsum. Nulla metus scelerisque sem at dolor. Maecenas " +
                                  "mattis. Sed convallis Curabitur sodales ligula in libero. Sed dignissim lacinia nunc. Lorem ipsum dolor sit amet," +
                                  " consectetur adipiscing elit.")
                .textStyle(TextStyle.builder()
                        .size(12)
                        .color(ComponentColor.TITLE)
                        .font(TextStyle.HELVETICA)
                        .decoration(TextDecoration.DEFAULT)
                        .build());


        Entity blockText = new ChunkTextBuilder(entityManager, Align.left(10))
                .size(500, 650)
                .margin(Margin.of(0))
                .anchor(Anchor.center())
                .padding(0, 0, 0, 20)
                .text(textBuilder)
                .build();
    }

    private static Entity blockTextBuilder(EntityManager entityManager) {
        TextBuilder textBuilder = new TextBuilder(entityManager)
                .textWithAutoSize("Trigger the long word warning. Integer nec odio. Praesent libero. lectus risus, iaculis vel, suscipit quis, " +
                                  "luctus non, massa. Fusce Vestibulum facilisis, nunc in hendrerit posuere, sapien magna Vestibulum lacinia arcu eget " +
                                  "nulla. Class aptent taciti sociosqu Sed cursus ante dapibus diam. Sed nisi. Nulla quis sem at nibh elementum imperdiet. " +
                                  "Duis sagittis ipsum. Praesent mauris. metus, ullamcorper vel, tincidunt sed, euismod in, nibh. Curabitur tortor. " +
                                  "Pellentesque nibh. Aenean quam. In ad litora torquent per conubia nostra, per inceptos himenaeos. " +
                                  "Fusce nec tellus sed augue semper porta. Mauris massa. pellentesque nunc, ac vehicula eros elit vitae nisl. t" +
                                  "ristique sem. Proin ut ligula vel nunc egestas porttitor. Morbi Supercalifragilisticexpialidocious is a very long " +
                                  "word that should ac turpis quis ligula lacinia aliquet. Mauris ipsum. Nulla metus scelerisque sem at dolor. Maecenas " +
                                  "mattis. Sed convallis Curabitur sodales ligula in libero. Sed dignissim lacinia nunc. Lorem ipsum dolor sit amet," +
                                  " consectetur adipiscing elit.")
                .textStyle(TextStyle.builder()
                        .size(12)
                        .color(ComponentColor.TITLE)
                        .font(TextStyle.HELVETICA)
                        .decoration(TextDecoration.DEFAULT)
                        .build());


        Entity blockText = new BlockTextBuilder(entityManager).create(Align.middle(0))
                .size(500, 2)
                .margin(Margin.of(20))
                .anchor(Anchor.bottomCenter())
                .padding(0, 5, 0, 20)
                .text(textBuilder)
                .build();
        return blockText;
    }

    private static Entity textTest(EntityManager entityManager) {
        var text = new TextBuilder(entityManager)
                .textWithAutoSize("Hello world")
                .textStyle(TextStyle.defaultStyle())
                .anchor(Anchor.center())
                .build();
        return text;

    }


}


