package com.demcha;

import com.demcha.components.components_builders.*;
import com.demcha.components.content.link.LinkUrl;
import com.demcha.components.content.shape.CornerRadius;
import com.demcha.components.content.text.TextDecoration;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.Anchor;
import com.demcha.components.style.ComponentColor;
import com.demcha.core.EntityManager;
import com.demcha.system.LayoutSystem;
import com.demcha.system.PdfFileManagerSystem;
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


        entityManager.addSystem(new LayoutSystem(doc.getPage(0)));
        entityManager.addSystem(new PdfRenderingSystem(doc));
        entityManager.addSystem(new PdfFileManagerSystem(target, doc));

//
//        var button = new ButtonBuilder(entityManager).create()
//                .createABottom("MyButton", new double[]{90, 30}, new CornerRadius(5), "Hello")
//                .anchor(Anchor.center())
//                .build();
//        var button2 = new ButtonBuilder(entityManager).create()
//                .createABottom("MyButton2", new double[]{90, 30}, new CornerRadius(5), "Gooogle")
//                .anchor(Anchor.center())
//                .build();
//        ;
//        var row = new RowBuilder(entityManager).create(Align.middle(15))
//                .anchor(Anchor.center())
//                .addChild(button)
//                .addChild(button2)
//                .build();

        var link = new LinkBuilder(entityManager).create()
                .linkUrl(new LinkUrl("demchaav@gmail.com"))
                .anchor(Anchor.center())
                .displayText(new DisplayTextBuilder(entityManager).create()
                        .textWithAutoSize("Email")
                )
                .build();


        entityManager.processSystems();


    }


}


