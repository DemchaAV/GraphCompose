package com.demcha.components.components_builders;

import com.demcha.components.containers.abstract_builders.EmptyBox;
import com.demcha.components.content.text.Text;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.renderable.TextComponent;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;
import com.demcha.exceptions.TextComponentException;
import lombok.extern.slf4j.Slf4j;

//TODO has to be finish with adding essential data type for building b Box
@Slf4j
public class TextBuilder extends EmptyBox<TextBuilder> {
    private boolean autosize;

    public TextBuilder(EntityManager entityManager) {
        super(entityManager);
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
        if (entity.hasAssignable(TextComponent.class)) {
            if (autosize) {
                TextStyle style = entity.getComponent(TextStyle.class).orElse(TextStyle.defaultStyle());
                Text textValue = entity.getComponent(Text.class).orElseThrow(() -> new TextComponentException("TextComponent Component  has not been initialized"));
                Padding padding = entity.getComponent(Padding.class).orElse(Padding.zero());
                double textHeight = style.getLineHeight() + padding.vertical();
                double textWidth = style.getTextWidth(textValue.value()) + padding.horizontal();
                entity.addComponent(new ContentSize(textWidth, textHeight));
            }
            manager().putEntity(entity);
            return entity;
        } else {
            log.error("TextComponent Component  has not been initialized");
            throw new TextComponentException(TextComponent.class + " Component  has not been initialized");
        }
    }
}
