package com.demcha;

import com.demcha.components.content.Stroke;
import com.demcha.components.content.components_builders.RectangleBuilder;
import com.demcha.components.content.components_builders.TextBuilder;
import com.demcha.components.content.rectangle.Radius;
import com.demcha.components.content.rectangle.Rectangle;
import com.demcha.components.content.text.Text;
import com.demcha.components.core.EntityName;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.Anchor;
import com.demcha.components.layout.Position;
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
        String closingBatFile = "close-adobe.bat";
        Path target = Paths.get("output.pdf");

        PdfDocument document = new PdfDocument();
        document.setPathOut(target);
        document.addSystem(new LayoutSystem());
        document.addSystem(new RenderingSystem());




        var rectangle = RectangleBuilder.create()
                .entityName(new EntityName("Rectangle"))
                .rectangle(new Rectangle())
                .size(new ContentSize(200, 200))
                .padding(new Padding(5,3,10,6))
                .strokeColor(Color.BLACK)
                .anchor(Anchor.center())
                .position(new Position(25, 25))
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
                .position(Position.zero())
                .anchor(Anchor.center())
                .margin(new Margin(5, 5, 5, 5))
                .buildInto(document);

        var text = TextBuilder.create()
                .entityName(new EntityName("Text"))
                .text(Text.of("Helloo world"))
                .position(new Position(25, 400))
                .buildInto(document);


        document.printEntities();
        document.processSystems();
        document.printEntities();

    }
}


