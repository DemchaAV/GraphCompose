package com.demcha;

import com.demcha.components.content.components_builders.BodyBoxBuilder;
import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import com.demcha.components.geometry.Size;
import com.demcha.components.layout.ParentComponent;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.core.PdfDocument;
import com.demcha.system.LayoutSystem;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

public class Main {
    public static void main(String[] args) throws Exception {
        String closingBatFile = "close-adobe.bat";
        Path target = Paths.get("output.pdf");


        PdfDocument document = new PdfDocument();
        document.setPathOut(target);
        document.addSystem(new LayoutSystem());
//        document.addSystem(new RenderingSystem());


//        var rectangle = RectangleBuilder.create()
//                .rectangle(new Rectangle(50, 150))
//                .entityName(new EntityName("Rectangle1"))
//                .stroke(new Stroke(5))
//                .position(new Position(0, 0))
//                .margin(Margin.all(50))
////                .anchor(new Anchor(HAnchor.CENTER,VAnchor.MIDDLE))
//                .strokeColor(Color.DARK_GRAY)
////                .parentComponent(new ParentComponent(box.getId()))
//                .buildInto(document);

//        var rectangle2 = RectangleBuilder.create()
//                .rectangle(new Rectangle(10, 50))
//                .entityName(new EntityName("Rectangle2"))
//                .parentComponent(new ParentComponent(rectangle.getId()))
//                .stroke(new Stroke(5))
////                .anchor(new Anchor(HAnchor.CENTER,VAnchor.MIDDLE))
//                .margin(Margin.all(5))
//                .anchor(new Anchor(HAnchor.CENTER,VAnchor.MIDDLE))
//                .strokeColor(Color.DARK_GRAY)
////                .parentComponent(new ParentComponent(box.getId()))
//                .buildInto(document);
        var box = BodyBoxBuilder.create()
                .entityName(new EntityName("BoxContainer"))
                .size(new Size(200, 200))
                .padding(Padding.of(10))
                .margin(new Margin(5, 5, 5, 5))
                .buildInto(document);

        var childBox = BodyBoxBuilder.create()
                .entityName(new EntityName("ChildBox"))
                .size(new Size(80, 40))
                .parentComponent(new ParentComponent(box.getId()))
                .padding(Padding.of(10))
                .margin(Margin.all(4))
                .buildInto(document);


//        PdfRebuildOpenInAdobe.closeAdobeFromResources(closingBatFile, 3, TimeUnit.SECONDS);
        for (Map.Entry<UUID, Entity> entry : document.getEntities().entrySet()) {
            Entity value = entry.getValue();
            System.out.println(entry.getValue());
            Map<Class<? extends Component>, Component> view = value.view();
            for (Component component : view.values()) {
                System.out.println(component);
            }
        }

            document.processSystems();

            for (Map.Entry<UUID, Entity> entry : document.getEntities().entrySet()) {
                Entity value = entry.getValue();
                System.out.println(entry.getValue());
                Map<Class<? extends Component>, Component> view = value.view();
                for (Component component : view.values()) {
                    System.out.println(component);
                }

            }


//        PdfRebuildOpenInAdobe.openInAdobe(target);

        }
    }


