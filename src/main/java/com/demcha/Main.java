package com.demcha;

import com.demcha.components.components_builders.*;
import com.demcha.components.content.shape.Stroke;
import com.demcha.components.content.shape.CornerRadius;
import com.demcha.components.content.text.TextDecoration;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.Anchor;
import com.demcha.components.layout.ParentComponent;
import com.demcha.components.layout.coordinator.Position;
import com.demcha.components.style.ComponentColor;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
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
        entityManager.setGuideLines(false);
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage(PDRectangle.A4));


        entityManager.addSystem(new LayoutSystem(doc.getPage(0)));
        entityManager.addSystem(new PdfRenderingSystem(doc));
        entityManager.addSystem(new PdfFileManagerSystem(target, doc));




        var rectangle = new RectangleBuilder(entityManager).create()
                .entityName(new EntityName("Rectangle"))
                .size(new ContentSize(400, 200))
                .cornerRadius(new CornerRadius(15))
                .stroke(new Stroke(ComponentColor.ROYAL_BLUE, 5))
                .padding(Padding.of(15))
                .anchor(Anchor.center())
                .margin(new Margin(5, 5, 5, 5))
                .build();

        var rectangleChild = new RectangleBuilder(entityManager).create()
                .size(new ContentSize(390, 190))
                .cornerRadius(new CornerRadius(12))
                .addParent(rectangle)
                .stroke(new Stroke(ComponentColor.MODULE_TITLE, 2))
                .anchor(Anchor.center())
                .build();

        var text3 = new TextBuilder(entityManager).create()
                .textWithAutoSize("Artem Demchyshyn")
                .textStyle((new TextStyle(TextStyle.HELVETICA, 32, TextDecoration.DEFAULT, ComponentColor.MODULE_LINE_TEXT)))
                .anchor(Anchor.center())
                .addParent(rectangleChild)
                .build();


        var root = new ElementBuilder(entityManager).create()
                .entityName(new EntityName("Root"))
                .fillPageSize(doc.getPage(0))
                .padding(Padding.of(25))
                .addChildAndFit(rectangle)
                .margin(Margin.of(0))
                .build();

        var button1 = createABottom("button1", "Arteme", entityManager);
        var button2 = createABottom("button1", "Arteme", entityManager);
        var button3 = createABottom("button1", "Arteme", entityManager);
        var button4 = createABottom("button1", "Arteme", entityManager);

        var hContainer1 = new HContainerBuilder(entityManager).create(Align.middle(0))
                .anchor(Anchor.center())
                .addChild(button1)
                .addChild(button2)
                .build();

        var hContainer2 = new HContainerBuilder(entityManager).create(Align.middle(0))
                .anchor(Anchor.centerBottom())
//                .padding(Padding.of(5))
                .anchor(Anchor.center())
                .addChild(button3)
                .addChild(button4)
                .build();
        entityManager.putEntity(hContainer2);


        var VContainer = new VContainerBuilder(entityManager)
                .create(Align.middle(1))
                .entityName(new EntityName("VContainer"))
                .margin(Margin.of(15))
                .anchor(Anchor.center())
                .addChild(hContainer1)
                .addChild(hContainer2)
                .addParent(root)
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


