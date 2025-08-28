package com.demcha.components.content.components_builders;

import com.demcha.components.content.text.Text;
import com.demcha.components.core.EntityName;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.Align;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Optional;

//TODO has to be finish with adding essential data type for building b Box
@Slf4j
public final class TextBuilder extends ComponentBoxBuilder<TextBuilder> {

    public TextBuilder() {
    }

    public static TextBuilder create() {
        TextBuilder textBuilder = new TextBuilder();
        textBuilder.entity.addComponent(new EntityName("Text Box"));
        return textBuilder;
    }


    @Override
    protected TextBuilder self() {
        return this;
    }

    /**
     * Add ar replace {@link Text} component
     *
     * @param text the Text component
     * @return this builder
     */
    public TextBuilder text(Text text) {
        return  textWithAutoSize(text);
    }

    public TextBuilder textWithAutoSize(Text text) {
        Optional<ContentSize> size;
        try {
            size = text.autoMeasureText();
        } catch (IOException e) {
            log.error("Error while trying to add a {}", text.getClass(), e);
            log.info("Element {} have not been added", text);
            return this;
        }
        log.debug("Element {} has been added", text);
        if (size.isPresent()) {
            addComponent(text);
            addComponent(size.get());
        }

        return this;
    }

    public TextBuilder align(Align align) {
        addComponent(align);
        return this;
    }
}
