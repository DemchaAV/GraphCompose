package com.demcha.compose.font;

import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * Typed font registration tuple.
 *
 * @param name logical font family
 * @param fontClass backend font type
 * @param font backend font instance
 * @param <F> backend font type
 */
@Accessors(fluent = true)
public record FontSet<F>(FontName name, Class<F> fontClass, F font) {
    /**
     * Creates a tuple using the font object's runtime class.
     *
     * @param name logical font family
     * @param font backend font instance
     */
    public FontSet(FontName name, F font) {
        this(name, runtimeClass(font), font);
    }

    /**
     * Creates a validated font tuple.
     */
    public FontSet {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(fontClass, "fontClass");
        Objects.requireNonNull(font, "font");
        if (!fontClass.isInstance(font)) {
            throw new IllegalArgumentException("Font instance does not match the provided class type.");
        }
    }

    @SuppressWarnings("unchecked")
    private static <F> Class<F> runtimeClass(F font) {
        Objects.requireNonNull(font, "font");
        return (Class<F>) font.getClass();
    }
}
