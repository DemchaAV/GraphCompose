package com.demcha;

import com.demcha.components.content.Stroke;
import com.demcha.components.content.components_builders.RectangleBuilder;
import com.demcha.components.content.components_builders.TextBuilder;
import com.demcha.components.content.rectangle.Radius;
import com.demcha.components.content.rectangle.Rectangle;
import com.demcha.components.content.text.Text;
import com.demcha.components.content.text.TextDecoration;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.EntityName;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.Anchor;
import com.demcha.components.layout.coordinator.Position;
import com.demcha.components.style.ColorComponent;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.core.PdfDocument;
import com.demcha.system.LayoutSystem;
import com.demcha.system.RenderingSystem;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        Path target = Paths.get("output.pdf");

        PdfDocument document = new PdfDocument();
        document.setPathOut(target);
        document.addSystem(new LayoutSystem());
        document.addSystem(new RenderingSystem());


        var rectangle = RectangleBuilder.create()
                .entityName(new EntityName("Rectangle"))
                .rectangle(new Rectangle())
                .size(new ContentSize(400, 200))
                .padding(Padding.of(15))
                .strokeColor(Color.BLACK)
                .anchor(Anchor.center())
//                .position(new Position(25, 25))
                .margin(new Margin(5, 5, 5, 5))
                .buildInto(document);


        var rectangle2 = RectangleBuilder.create()
                .entityName(new EntityName("Rectangle2"))
                .rectangle(new Rectangle(new Radius(12)))
                .size(new ContentSize(94, 50))
                .padding(Padding.of(10))
                .stroke(new Stroke(12.0))
                .parentComponent(rectangle)
                .strokeColor(new Color(133, 198, 198, 255))
                .anchor(Anchor.topLeft())
                .margin(new Margin(5, 5, 5, 5))
                .buildInto(document);
        var rectangle3 = RectangleBuilder.create()
                .entityName(new EntityName("Rectangle2"))
                .rectangle(new Rectangle(new Radius(12)))
                .size(new ContentSize(94, 50))
                .padding(Padding.of(10))
                .stroke(new Stroke(12.0))
                .parentComponent(rectangle)
                .strokeColor(new Color(128, 128, 255, 255))
                .anchor(Anchor.topRight())
                .margin(new Margin(5, 5, 5, 5))
                .buildInto(document);

        var text = TextBuilder.create()
                .entityName(new EntityName("Text"))
                .parentComponent(rectangle2)
                .text(new Text("Hello World!!", new TextStyle(TextStyle.HELVETICA, 12, TextDecoration.DEFAULT, ColorComponent.LINK_VISITED)))
//                .position(new Position(25, 400))
                .anchor(Anchor.center())
                .margin(Margin.all(15))
                .buildInto(document);

        var text2 = TextBuilder.create()
                .entityName(new EntityName("Text"))
                .parentComponent(rectangle3)
                .text(new Text("Artem!!", new TextStyle(TextStyle.HELVETICA, 12, TextDecoration.DEFAULT, ColorComponent.LINK_VISITED)))
//                .position(new Position(25, 400))
                .anchor(Anchor.center())
                .margin(Margin.all(15))
                .buildInto(document);

        document.setGuideLines(false);
        document.printEntities();
        document.processSystems();
        document.printEntities();

    }
}


