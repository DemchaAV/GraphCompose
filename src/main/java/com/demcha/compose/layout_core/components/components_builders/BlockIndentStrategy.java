package com.demcha.compose.layout_core.components.components_builders;

public enum BlockIndentStrategy {
    NONE(false, false),
    FIRST_LINE(true, false),
    FROM_SECOND_LINE(false, true),
    ALL_LINES(true, true);

    private final boolean indentFirstLine;
    private final boolean indentWrappedLines;

    BlockIndentStrategy(boolean indentFirstLine, boolean indentWrappedLines) {
        this.indentFirstLine = indentFirstLine;
        this.indentWrappedLines = indentWrappedLines;
    }

    public boolean indentFirstLine() {
        return indentFirstLine;
    }

    public boolean indentWrappedLines() {
        return indentWrappedLines;
    }
}

