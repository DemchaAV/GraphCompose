package com.demcha;

import com.demcha.components.components_builders.ButtonBuilder;
import com.demcha.components.components_builders.RectangleBuilder;
import com.demcha.components.components_builders.RowBuilder;
import com.demcha.components.components_builders.TextBuilder;
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


        var button = new ButtonBuilder(entityManager).create()
                .createABottom("MyButton", new double[]{90, 30}, new CornerRadius(5), "Hello")
//                .padding(new Padding(2, 0, 0, 0))
                .anchor(Anchor.center())
                .build();
        var button2 = new ButtonBuilder(entityManager).create()
                .createABottom("MyButton2", new double[]{90, 30}, new CornerRadius(5), "Gooogle")
//                .padding(new Padding(2, 0, 0, 0))
                .anchor(Anchor.center())
                .build();
        ;
        var row = new RowBuilder(entityManager).create(Align.middle(15))
                .anchor(Anchor.center())
                .addChild(button)
                .addChild(button2)
                .build();


        entityManager.processSystems();


    }

    public static Entity createABottom(String nameButton, String TextButton, EntityManager document) {

        var text3 = new TextBuilder(document).create()
                .entityName(new EntityName("TextComponent"))
                .textWithAutoSize(TextButton)
                .textStyle((new TextStyle(TextStyle.HELVETICA, 12, TextDecoration.DEFAULT, ComponentColor.MODULE_LINE_TEXT)))
                .anchor(Anchor.center())
                .build();

        var button = new RectangleBuilder(document).create()
                .entityName(new EntityName(nameButton))
                .cornerRadius(new CornerRadius(5))
                .size(new ContentSize(80, 30))
                .fillColor(ComponentColor.ROYAL_BLUE)
                .addChild(text3)
                .anchor(Anchor.topLeft())
                .build();

        return button;
    }
}


