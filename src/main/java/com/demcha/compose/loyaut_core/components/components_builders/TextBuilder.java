package com.demcha.compose.loyaut_core.components.components_builders;

import com.demcha.compose.loyaut_core.components.containers.abstract_builders.EmptyBox;
import com.demcha.compose.loyaut_core.components.content.text.Text;
import com.demcha.compose.loyaut_core.components.content.text.TextStyle;
import com.demcha.compose.loyaut_core.components.core.Entity;
import com.demcha.compose.loyaut_core.components.geometry.ContentSize;
import com.demcha.compose.loyaut_core.components.renderable.TextComponent;
import com.demcha.compose.loyaut_core.components.style.Padding;
import com.demcha.compose.loyaut_core.core.EntityManager;
import com.demcha.compose.loyaut_core.exceptions.TextComponentException;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

//TODO has to be finish with adding essential data type for building b Box
@Slf4j
@Accessors(fluent = true)
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

    @SneakyThrows
    @Override
    @SuppressWarnings("unchecked")
    public Entity build() {
        if (entity.hasAssignable(TextComponent.class)) {
            if (autosize) {
                Padding padding = entity.getComponent(Padding.class).orElse(Padding.zero());
                ContentSize measuredText = TextComponent.autoMeasureText(entity, entityManager);
                double textHeight = measuredText.height() + padding.vertical();
                double textWidth = measuredText.width() + padding.horizontal();

                log.debug("Autosized single-line text entity {} -> measuredText={} padding={} finalSize=({}, {})",
                        entity, measuredText, padding, textWidth, textHeight);

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
