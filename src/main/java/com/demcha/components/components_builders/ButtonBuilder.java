package com.demcha.components.components_builders;

import com.demcha.components.containers.abstract_builders.ShapeBuilderBase;
import com.demcha.components.content.shape.CornerRadius;
import com.demcha.components.content.shape.Stroke;
import com.demcha.components.content.text.TextDecoration;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.EntityName;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.Anchor;
import com.demcha.components.renderable.Button;
import com.demcha.components.renderable.Rectangle;
import com.demcha.components.style.ComponentColor;
import com.demcha.core.EntityManager;

import java.awt.*;

public class ButtonBuilder extends ShapeBuilderBase<ButtonBuilder> {
    public ButtonBuilder(EntityManager document) {
        super(document);
    }


    public ButtonBuilder rectangle(Rectangle rectangle) {
        return addComponent(rectangle);
    }

//    @Override
//    public Entity build() {
//        return entity;
//    }

    public ButtonBuilder text(TextBuilder textBuilder) {
        return addChild(textBuilder.build());
    }

    public ButtonBuilder createABottom(String nameButton, double[] size, CornerRadius cornerRadius, String textButton) {
        if (size == null) {
            size = new double[]{100, 30};
        } else {
            if (size.length > 2 || size.length < 2) {
                throw new IllegalStateException("Array  must be have 2 elements size[0]== width, size[1]== height");
            }
        }
        setDefaultButtonDecoration(nameButton, size, cornerRadius);
        var buttonText = defaultTextDecoration(textButton);
        text(buttonText)
                .build();

        return self();
    }

    private TextBuilder defaultTextDecoration(String textButton) {
        return new TextBuilder(entityManager).create()
                .entityName(new EntityName("TextComponent"))
                .textWithAutoSize(textButton)
                .anchor(Anchor.center())
                .textStyle(TextStyle.builder()
                        .font(TextStyle.HELVETICA)
                        .size(14)
                        .decoration(TextDecoration.DEFAULT)
                        .color(new Color(19, 19, 19))
                        .build());

    }

    private void setDefaultButtonDecoration(String nameButton, double[] size, CornerRadius cornerRadius) {
        create();
        entityName(new EntityName(nameButton));
        cornerRadius(cornerRadius);
        size(new ContentSize(size[0], size[1]));
        stroke(new Stroke(ComponentColor.TITLE));
        fillColor(ComponentColor.ROYAL_BLUE);

    }

    @Override
    public void initialize() {
        entity.addComponent(new Button());
    }
}
