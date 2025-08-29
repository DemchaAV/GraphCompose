package com.demcha.components.containers.moduls;


import com.demcha.components.containers.abstract_builders.ShapeBuilderBase;
import com.demcha.components.content.Element;
import com.demcha.components.content.Stroke;
import com.demcha.components.content.components_builders.TextBuilder;
import com.demcha.components.content.rectangle.Radius;
import com.demcha.components.content.rectangle.Rectangle;
import com.demcha.components.content.text.TextDecoration;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.Anchor;
import com.demcha.components.layout.ParentComponent;
import com.demcha.core.PdfDocument;
import lombok.AllArgsConstructor;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
public class Button {
    private final PdfDocument pdfDocument;
    private Map<UUID, Entity> buttons;


    private Entity createABottom(String nameButton, double[] size, Radius cornerRadius, String textButton) {
        if (size == null) {
            size = new double[]{100, 30};
        } else {
            if (size.length > 2 || size.length < 2) {
                throw new IllegalStateException("Array  must be have 2 elements size[0]== width, size[1]== height");
            }
        }
        var text3 = new TextBuilder(pdfDocument).create()
                .entityName(new EntityName("TextComponent"))
                .textWithAutoSize(textButton)
                .textStyle(TextStyle.builder()
                        .font(TextStyle.HELVETICA)
                        .size(12)
                        .decoration(TextDecoration.DEFAULT)
                        .color(new Color(19, 19, 19))
                        .build()
                )
                .anchor(Anchor.center());


        var button = new BodyButtonBuilder(pdfDocument).create()
                .entityName(new EntityName(nameButton))
                .rectangle(new Rectangle())
                .cornerRadius(cornerRadius)
                .size(new ContentSize(size[0], size[1]))
                .stroke(new Stroke(12.0));

        return buildButton(button, text3);
    }

    private Entity buildButton(BodyButtonBuilder button, TextBuilder buttonText) {
        if (buttons == null) buttons = new HashMap<>();

        var buttonBody = button
                .build();

        var buttonTextDecorated = buttonText
                .addComponent(new ParentComponent(buttonBody))
                .buildInto();

        return buttonBody;
    }
}


class BodyButtonBuilder extends ShapeBuilderBase<BodyButtonBuilder> {
    public BodyButtonBuilder(PdfDocument document) {
        super(document);
    }


    public BodyButtonBuilder rectangle(Rectangle rectangle) {
        return addComponent(rectangle);
    }

    @Override
    public Entity build() {
        return entity;
    }

    @Override
    public void initialize() {
        entity.addComponent(new Element());
    }
}
