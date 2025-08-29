package com.demcha;

import com.demcha.components.containers.HContainerBuilder;
import com.demcha.components.containers.VContainerBuilder;
import com.demcha.components.content.Stroke;
import com.demcha.components.content.components_builders.ElementBuilder;
import com.demcha.components.content.components_builders.RectangleBuilder;
import com.demcha.components.content.components_builders.TextBuilder;
import com.demcha.components.content.rectangle.Radius;
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
import com.demcha.system.PdfRenderingSystem;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        Path target = Paths.get("output.pdf");

        EntityManager document = new EntityManager();
        document.setPathOut(target);
        document.addSystem(new LayoutSystem());
        document.addSystem(new PdfRenderingSystem());

        var text3 = new TextBuilder(document).create()
                .entityName(new EntityName("TextComponent"))
                .textWithAutoSize("Artem Demchyshyn")
                .textStyle(new TextStyle(TextStyle.HELVETICA, 32, TextDecoration.DEFAULT, ComponentColor.TITLE))
                .anchor(Anchor.center())
                .margin(Margin.of(6))
                .padding(Padding.of(5))
                .buildInto();


        var rectangle = new RectangleBuilder(document).create()
                .entityName(new EntityName("Rectangle"))
                .size(new ContentSize(400, 200))
                .cornerRadius(new Radius(5))
                .stroke(new Stroke(ComponentColor.ROYAL_BLUE,5))

                .padding(Padding.of(15))
                .anchor(Anchor.center())
//                .position(new Position(25, 25))
                .margin(new Margin(5, 5, 5, 5))
                .buildInto();

        var box = ElementBuilder.create()
                .entityName(new EntityName("BodyBox"))
                .fillHorizontal(document.getPage(), 100)
                .padding(Padding.zero())
                .position(new Position(0, 200))
                .anchor(Anchor.centerTop())
                .margin(Margin.of(0))
                .buildInto(document);
        var button1 = createABottom("button1", "Arteme", document);
        var button2 = createABottom("button1", "Arteme", document);
        var button3 = createABottom("button1", "Arteme", document);
        var button4 = createABottom("button1", "Arteme", document);

        var hContainer1 = new HContainerBuilder(document).create()
                .anchor(Anchor.centerBottom())
                .padding(Padding.of(5))
                .margin(Margin.of(6))
                .anchor(Anchor.center())
                .addChild(button1)
                .addChild(button2)
                .build();

        var hContainer2 = new HContainerBuilder(document).create()
                .anchor(Anchor.centerBottom())
                .padding(Padding.of(5))
                .margin(Margin.of(6))
                .anchor(Anchor.center())
                .addChild(button3)
                .addChild(button4)
                .build();
        document.putEntity(hContainer2);


        var VContainer = new VContainerBuilder(document)
                .create(Align.middle(5))
                .entityName(new EntityName("VContainer"))
                .margin(Margin.of(15))
                .anchor(Anchor.centerBottom())
                .addChild(hContainer1)
                .addChild(hContainer2)
                .build();


        document.setGuideLines(true);
        document.printEntities();
        document.processSystems();
        document.printEntities();


    }

    public static Entity createABottom(String nameButton, String TextButton, EntityManager document) {

        var button = new RectangleBuilder(document).create()
                .entityName(new EntityName(nameButton))
                .cornerRadius(new Radius(5))
                .size(new ContentSize(80, 30))
                .fillColor(ComponentColor.ROYAL_BLUE)
                .anchor(Anchor.topLeft())
                .build();
        var text3 = new TextBuilder(document).create()
                .entityName(new EntityName("TextComponent"))
                .textWithAutoSize(TextButton)
                .textStyle((new TextStyle(TextStyle.HELVETICA, 12, TextDecoration.DEFAULT, ComponentColor.MODULE_LINE_TEXT)))
                .anchor(Anchor.center())
                .addComponent(new ParentComponent(button))
                .buildInto();
        return button;
    }
}


