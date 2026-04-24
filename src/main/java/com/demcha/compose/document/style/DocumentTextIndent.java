package com.demcha.compose.document.style;

/**
 * Public text indentation mode for semantic paragraphs.
 *
 * <p>This value describes author intent in the canonical document API. The
 * layout engine converts it to its internal indentation strategy when text is
 * measured and split across pages.</p>
 *
 * @author Artem Demchyshyn
 */
public enum DocumentTextIndent {
    /**
     * No generated indentation is applied to paragraph lines.
     */
    NONE,

    /**
     * Only the first visual line receives the configured prefix.
     */
    FIRST_LINE,

    /**
     * Wrapped continuation lines receive the configured prefix.
     */
    FROM_SECOND_LINE,

    /**
     * Every visual line receives the configured prefix.
     */
    ALL_LINES
}
