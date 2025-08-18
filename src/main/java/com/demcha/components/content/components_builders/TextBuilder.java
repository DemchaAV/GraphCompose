package com.demcha.components.content.components_builders;

import com.demcha.components.content.Text;
//TODO has to be finish with adding essential data type for building b Box
public final class TextBuilder extends ComponentBoxBuilder<TextBuilder> {

    private TextBuilder() {
    }

    public static TextBuilder create() {
        return new TextBuilder();
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
        return put(text);
    }
}
