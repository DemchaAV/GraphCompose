package com.demcha;

import com.demcha.components.content.Stroke;
import com.demcha.components.content.components_builders.RectangleBuilder;
import com.demcha.components.content.components_builders.TextBuilder;
import com.demcha.components.content.rectangle.Rectangle;
import com.demcha.components.content.text.Text;
import com.demcha.components.core.EntityName;
import com.demcha.components.layout.*;
import com.demcha.components.style.Margin;
import com.demcha.core.PdfDocument;
import com.demcha.helper.PdfRebuildOpenInAdobe;
import com.demcha.system.LayoutSystem;
import com.demcha.system.RenderingSystem;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws Exception {
        String closingBatFile = "close-adobe.bat";
        Path target = Paths.get("output.pdf");


        PdfDocument document = new PdfDocument();
        document.setPathOut(target);
        document.addSystem(new LayoutSystem());
        document.addSystem(new RenderingSystem());


//        var box = BodyBoxBuilder.create()
//                .fillPageSize(document.getPage())
//                .entityName(new EntityName("BoxContainer"))
////                .padding(Padding.of(100))
//                .buildComponents();
//        document.putEntity(box);
//
        var rectangle = RectangleBuilder.create()
                .rectangle(new Rectangle(50, 150))
                .entityName(new EntityName("Rectangle1"))
                .stroke(new Stroke(5))
                .position(new Position(200, 200))
//                .anchor(new Anchor(HAnchor.CENTER,VAnchor.MIDDLE))
                .strokeColor(Color.DARK_GRAY)
//                .parentComponent(new ParentComponent(box.getId()))
                .buildInto(document);

        var rectangle2 = RectangleBuilder.create()
                .rectangle(new Rectangle(10, 50))
                .entityName(new EntityName("Rectangle2"))
                .parentComponent(new ParentComponent(rectangle.getId()))
                .stroke(new Stroke(5))
//                .anchor(new Anchor(HAnchor.CENTER,VAnchor.MIDDLE))
                .margin(Margin.all(5))
                .anchor(new Anchor(HAnchor.CENTER,VAnchor.MIDDLE))
                .strokeColor(Color.DARK_GRAY)
//                .parentComponent(new ParentComponent(box.getId()))
                .buildInto(document);

//        var textEntity = TextBuilder.create()
//                .parentComponent(new ParentComponent(rectangle.getId()))
//                .textWithAutoSize(Text.of("Hello World!"))
//                .margin(Margin.all(5))
//                .anchor(new Anchor(HAnchor.CENTER, VAnchor.TOP))
//                .buildInto(document);


//        PdfRebuildOpenInAdobe.closeAdobeFromResources(closingBatFile, 3, TimeUnit.SECONDS);


        document.processSystems();
//        System.out.println(box.getComponent(BoundingBox.class));
//        System.out.println(textEntity.getComponent(BoundingBox.class));
//        System.out.println(textEntity.getComponent(ComputedPosition.class));

        PdfRebuildOpenInAdobe.openInAdobe(target);

    }

}
