package com.demcha.compose.layout_core.system.interfaces;

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public abstract class FontBase<T> implements Font<T> {
    private final T defaultFont;
    private final T bold;
    private final T italic;
    private final T boldItalic;
    private final T underline;
    private final T strikethrough;
    protected static final java.util.regex.Pattern CONTROL_CHARS_PATTERN = java.util.regex.Pattern.compile("[\\p{Cntrl}&&[^\\t]]");
    protected static final java.util.regex.Pattern MULTIPLE_SPACES_PATTERN = java.util.regex.Pattern.compile(" +");

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
}


