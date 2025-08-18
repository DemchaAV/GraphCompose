package com.demcha;

import com.demcha.components.content.components_builders.BodyBoxBuilder;
import com.demcha.components.core.EntityName;
import com.demcha.components.geometry.BoxSize;
import com.demcha.components.layout.ComputedPosition;
import com.demcha.components.layout.ParentComponent;
import com.demcha.components.layout.Position;
import com.demcha.components.style.ColorComponent;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.core.PdfDocument;
import com.demcha.system.LayoutSystem;

public class Main {
    public static void main(String[] args) {
        PdfDocument document = new PdfDocument();
        LayoutSystem layoutSystem = new LayoutSystem();
        document.addSystem(layoutSystem);

        var box = BodyBoxBuilder.create()
                .entityName(new EntityName("Box"))
                .size(new BoxSize(500.0, 500.0))
                .margin(new Margin(5, 9, 8, 0))
                .padding(new Padding(8, 8, 8, 8))
                .buildInto(document);
//
//        var component = TextBuilder.create()
//                .parentComponent(new ParentComponent(box))
//                .entityName(new EntityName("Module"))
//                .text(new Text("Artem Demchyshyn"))
//                .size(new BoxSize(200.0, 40.0))
//                .position(new Position(24, 24))
//                .margin(new Margin(5.5, 3, 0, 0))
//                .buildInto(document);
//
//        var component2 = TextBuilder.create()
//                .parentComponent(new ParentComponent(component))
//                .entityName(new EntityName("Module2_test"))
//                .text(new Text("Mark Loyd"))
//                .size(new BoxSize(150., 30.0))
//                .position(new Position(24, 24))
//                .margin(new Margin(5.5, 3, 0, 0))
//                .buildInto(document);

//        var rectangle = RectangleBuilder.create()
//                .parentComponent(new ParentComponent(box))
//                .entityName(new EntityName("Rectangle"))
//                .rectangle(new RectangleComponent(200, 50, Color.BLACK))
//                .size(new BoxSize(300, 90))
//                .position(new Position(24, 24))
//                .margin(new Margin(5.5, 3, 0, 0))
//                .buildInto(document);
//        var rectangle2 = RectangleBuilder.create()
//                .parentComponent(new ParentComponent(rectangle))
//                .entityName(new EntityName("Rectangle2"))
//                .rectangle(new RectangleComponent(200, 50, Color.YELLOW))
//                .size(new BoxSize(75, 15))
//                .position(new Position(24, 24))
//                .margin(new Margin(5.5, 3, 0, 0))
//                .buildInto(document);
//
//
        PdfDocument doc = new PdfDocument();
        doc.addSystem(new LayoutSystem());

// Create a root with padding & margin
        var root = doc.createEntity("Box");
        root.addComponent(new BoxSize(500, 500));
        root.addComponent(new Padding(8, 8, 8, 8));
        root.addComponent(new Margin(5, 9, 8, 0));

        System.out.println("///////////////////");
        System.out.println(root.getComponent(BoxSize.class));
        root.addComponentIfAbsent(new BoxSize(200, 2055));
        System.out.println(root.getComponent(BoxSize.class));

// Create a child positioned inside the root
        var child = doc.createEntity("Rectangle");
        child.addComponent(new ParentComponent(root.getId()));
        child.addComponent(new Position(24, 24));
        child.addComponent(new Margin(5.5, 3, 0, 0));
        child.addComponent(new BoxSize(300, 90));
        child.addComponent(new ColorComponent(ColorComponent.LINK_DEFAULT));

// Run systems
        doc.processSystems();

// Read computed layout
        ComputedPosition cp = doc.getEntities().get(child.getId())
                .getComponent(ComputedPosition.class)
                .orElseThrow();
        System.out.println(cp.x() + ", " + cp.y());


        document.processSystems();

    }
}
