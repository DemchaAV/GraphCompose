package com.demcha.compose.document.templates.blocks;

import com.demcha.compose.document.node.TextAlign;

import java.util.Objects;

/**
 * A {@link Block} that renders as a single paragraph of body text.
 *
 * <p>Use this for short prose modules such as Professional Summary, or
 * for any module whose body is one continuous block of text rather than
 * an enumerated list.</p>
 *
 * @param text  paragraph text content (must not be null; may be empty)
 * @param align horizontal alignment for the paragraph (defaults to
 *              {@link TextAlign#LEFT} when null)
 */
public record ParagraphBlock(String text, TextAlign align) implements Block {

    /**
     * Compact constructor that normalises the alignment default and
     * rejects a null text reference.
     *
     * @throws NullPointerException if {@code text} is null
     */
    public ParagraphBlock {
        Objects.requireNonNull(text, "text");
        align = align == null ? TextAlign.LEFT : align;
    }

    /**
     * Convenience constructor that defaults alignment to left.
     *
     * @param text paragraph text content
     */
    public ParagraphBlock(String text) {
        this(text, TextAlign.LEFT);
    }
}
