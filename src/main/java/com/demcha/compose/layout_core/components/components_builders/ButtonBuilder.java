package com.demcha.compose.layout_core.components.components_builders;

import com.demcha.compose.font_library.FontName;
import com.demcha.compose.layout_core.components.containers.abstract_builders.ShapeBuilderBase;
import com.demcha.compose.layout_core.components.content.shape.CornerRadius;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.content.text.TextDecoration;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.core.EntityName;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.renderable.Button;
import com.demcha.compose.layout_core.components.renderable.Rectangle;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.core.EntityManager;

import java.awt.*;


public class ButtonBuilder extends ShapeBuilderBase<ButtonBuilder> {
    public ButtonBuilder(EntityManager entityManager) {
        super(entityManager);
    }


    public ButtonBuilder rectangle(Rectangle rectangle) {
        return addComponent(rectangle);
    }


    public ButtonBuilder text(TextBuilder textBuilder) {
        return addChild(textBuilder.build());
    }

    public ButtonBuilder createABottom(String nameButton, double[] size, CornerRadius cornerRadius, String textButton) {
        if (size == null) {
            size = new double[]{100, 30};
        } else {
            if (size.length != 2) {
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
        return new TextBuilder(entityManager)
                .entityName(new EntityName("TextComponent"))
                .textWithAutoSize(textButton)
                .anchor(Anchor.center())
                .textStyle(TextStyle.builder()
                        .fontName(FontName.HELVETICA)
                        .size(14)
                        .decoration(TextDecoration.DEFAULT)
                        .color(new Color(19, 19, 19))
                        .build());

    }

    private void setDefaultButtonDecoration(String nameButton, double[] size, CornerRadius cornerRadius) {
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
