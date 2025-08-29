package com.demcha.components.content.components_builders;

import com.demcha.components.containers.abstract_builders.EmptyBox;
import com.demcha.components.content.text.Text;
import com.demcha.components.content.text.TextComponent;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;
import lombok.extern.slf4j.Slf4j;

//TODO has to be finish with adding essential data type for building b Box
@Slf4j
public final class TextBuilder extends EmptyBox<TextBuilder> {
    private String text;
    private TextStyle style;
    private boolean autosize;

    public TextBuilder(EntityManager document) {
        super(document);
    }

    public TextBuilder text(Text textComponent) {
        return addComponent(textComponent);
    }

    public TextBuilder text(String text) {
        return addComponent(new Text(text));
    }

    public TextBuilder textStyle(TextStyle textStyle) {
        return addComponent(textStyle);
    }


    public TextBuilder textWithAutoSize(Text textComponent) {
        autosize = true;
        return addComponent(textComponent);

    }
    public TextBuilder textWithAutoSize(String text) {
        autosize = true;
        return addComponent(new Text(text));

    }

    @Override
    public void initialize() {
        entity.addComponent(new TextComponent());
    }

    @Override
    public Entity build() {
        if (entity.has(TextComponent.class)) {
            if (autosize) {
                TextStyle style = entity.getComponent(TextStyle.class).orElseThrow();
                Text textValue = entity.getComponent(Text.class).orElseThrow();
                Padding padding = entity.getComponent(Padding.class).orElse(Padding.zero());
                double textHeight = style.getTextHeight(textValue.value()) + padding.vertical();
                double textWidth = style.getTextWidth(textValue.value()) + padding.horizontal();
                entity.addComponent(new ContentSize(textWidth, textHeight));
            }
            return entity;
        } else {
            log.error("TextComponent Component  has not been initialized");
            throw new RuntimeException(TextComponent.class + " Component  has not been initialized");
        }
    }
}
