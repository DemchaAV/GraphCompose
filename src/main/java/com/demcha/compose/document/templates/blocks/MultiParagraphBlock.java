package com.demcha.compose.document.templates.blocks;

import java.util.List;
import java.util.Objects;

/**
 * A {@link Block} that renders as several paragraphs separated by
 * paragraph spacing.
 *
 * <p>Use this for cover-letter bodies, multi-paragraph experience
 * descriptions, or any module whose body is a sequence of discrete
 * paragraphs rather than a list of enumerated items. The surrounding
 * Module composer applies the active {@code Spacing.paragraphSpacing}
 * between adjacent paragraphs.</p>
 *
 * @param paragraphs paragraph texts in source order (must not be null;
 *                   may be empty; individual paragraphs must not be null;
 *                   blank paragraphs are kept as-is to preserve the
 *                   user's intended whitespace)
 */
public record MultiParagraphBlock(List<String> paragraphs) implements Block {

    /**
     * Compact constructor that defensively copies the supplied list and
     * validates that no paragraph reference is null.
     *
     * @throws NullPointerException if {@code paragraphs} or any element
     *                              is null
     */
    public MultiParagraphBlock {
        Objects.requireNonNull(paragraphs, "paragraphs");
        paragraphs = List.copyOf(paragraphs);
    }
}
