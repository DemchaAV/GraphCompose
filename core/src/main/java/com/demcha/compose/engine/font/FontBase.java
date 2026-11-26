package com.demcha.compose.engine.font;

public abstract class FontBase<T> implements Font<T> {
    private final T defaultFont;
    private final T bold;
    private final T italic;
    private final T boldItalic;
    private final T underline;
    private final T strikethrough;

    protected FontBase(T defaultFont, T bold, T italic, T boldItalic, T underline, T strikethrough) {
        this.defaultFont = defaultFont;
        this.bold = bold;
        this.italic = italic;
        this.boldItalic = boldItalic;
        this.underline = underline;
        this.strikethrough = strikethrough;
    }

    protected FontBase(T defaultFont, T bold, T italic, T boldItalic) {
        this.defaultFont = defaultFont;
        this.bold = bold;
        this.italic = italic;
        this.boldItalic = boldItalic;
        this.underline = defaultFont;
        this.strikethrough = defaultFont;
    }

    /**
     * Returns the regular face.
     *
     * @return the regular face
     */
    @Override
    public T defaultFont() {
        return defaultFont;
    }

    /**
     * Returns the bold face.
     *
     * @return the bold face
     */
    @Override
    public T bold() {
        return bold;
    }

    /**
     * Returns the italic face.
     *
     * @return the italic face
     */
    @Override
    public T italic() {
        return italic;
    }

    /**
     * Returns the bold italic face.
     *
     * @return the bold italic face
     */
    @Override
    public T boldItalic() {
        return boldItalic;
    }

    /**
     * Returns the face underlined text is set in.
     *
     * @return the underline face; the regular face unless one was given
     */
    @Override
    public T underline() {
        return underline;
    }

    /**
     * Returns the face struck-through text is set in.
     *
     * @return the strikethrough face; the regular face unless one was given
     */
    @Override
    public T strikethrough() {
        return strikethrough;
    }
}
