package com.demcha.components.containers.moduls;


import com.demcha.components.components_builders.BodyButtonBuilder;
import com.demcha.components.content.shape.Stroke;
import com.demcha.components.components_builders.TextBuilder;
import com.demcha.components.content.shape.CornerRadius;
import com.demcha.components.renderable.Rectangle;
import com.demcha.components.content.text.TextDecoration;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.Anchor;
import com.demcha.components.layout.ParentComponent;
import com.demcha.core.EntityManager;
import lombok.AllArgsConstructor;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
public class Button {
    private final EntityManager entityManager;
    private Map<UUID, Entity> buttons;


    private Entity createABottom(String nameButton, double[] size, CornerRadius cornerRadius, String textButton) {
        if (size == null) {
            size = new double[]{100, 30};
        } else {
            if (size.length > 2 || size.length < 2) {
                throw new IllegalStateException("Array  must be have 2 elements size[0]== width, size[1]== height");
            }
        }
        var text3 = new TextBuilder(entityManager).create()
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


        var button = new BodyButtonBuilder(entityManager).create()
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
                .build();

        return buttonBody;
    }
}


