package com.demcha.compose.layout_core.components.components_builders;

import com.demcha.compose.layout_core.components.containers.abstract_builders.EmptyBox;
import com.demcha.compose.layout_core.components.content.text.Text;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.renderable.TextComponent;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.exceptions.TextComponentException;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * Builder for single text entities.
 * <p>
 * {@code TextBuilder} produces a leaf entity rendered by {@code TextComponent}.
 * It is the simplest text path in the engine and is a good fit for labels,
 * short headings, metadata fields, and any text that should stay as one entity.
 * </p>
 *
 * <p>If auto-size is enabled, the builder measures the text before registration
 * and writes a {@code ContentSize} component that later drives layout.</p>
 */
@Slf4j
@Accessors(fluent = true)
public class TextBuilder extends EmptyBox<TextBuilder> {
    private boolean autosize;

    TextBuilder(EntityManager entityManager) {
        super(entityManager);
    }

    /**
     * Sets the text payload explicitly.
     */
    public TextBuilder text(Text textComponent) {
        return addComponent(textComponent);
    }

    /**
     * Sets the text payload from a raw string.
     */
    public TextBuilder text(String text) {
        return addComponent(new Text(text));
    }

    /**
     * Sets the text style consumed later by measurement and rendering.
     */
    public TextBuilder textStyle(TextStyle textStyle) {
        return addComponent(textStyle);
    }


    /**
     * Sets the text payload and asks the builder to measure the final content size.
     */
    public TextBuilder textWithAutoSize(Text textComponent) {
        autosize = true;
        return addComponent(textComponent);

    }

    /**
     * Sets the text payload from a string and asks the builder to measure the
     * final content size.
     */
    public TextBuilder textWithAutoSize(String text) {
        autosize = true;
        return addComponent(new Text(text));

    }

    @Override
    public void initialize() {
        entity.addComponent(new TextComponent());
    }

    /**
     * Finalizes the entity and optionally measures text before registration.
     *
     * <p>After this call the entity is ready for layout. The layout system will
     * later compute placement from the measured size, margin, padding, anchor,
     * and parent context.</p>
     */
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
            return registerBuiltEntity();
        } else {
            log.error("TextComponent Component  has not been initialized");
            throw new TextComponentException(TextComponent.class + " Component  has not been initialized");
        }
    }
}
