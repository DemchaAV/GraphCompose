package com.demcha.compose.engine.components.content.text;

/**
 * Internal text indentation strategy used by paragraph layout preparation.
 *
 * <p>This is a V2 engine model type, not an authoring builder contract. Public
 * document APIs should expose semantic list/paragraph options and convert to
 * this value only inside the canonical layout pipeline.</p>
 *
 * @author Artem Demchyshyn
 */
public enum TextIndentStrategy {
    NONE(false, false),
    FIRST_LINE(true, false),
    FROM_SECOND_LINE(false, true),
    ALL_LINES(true, true);

    private final boolean indentFirstLine;
    private final boolean indentWrappedLines;

    TextIndentStrategy(boolean indentFirstLine, boolean indentWrappedLines) {
        this.indentFirstLine = indentFirstLine;
        this.indentWrappedLines = indentWrappedLines;
    }

    /**
     * Returns whether the first visual line should receive the indent marker.
     *
     * @return true when first-line indentation is enabled
     */
    public boolean indentFirstLine() {
        return indentFirstLine;
    }

    /**
     * Returns whether wrapped continuation lines should receive the indent.
     *
     * @return true when wrapped-line indentation is enabled
     */
    public boolean indentWrappedLines() {
        return indentWrappedLines;
    }
}
