package com.demcha.compose.document.templates.coverletter.layouts;

import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentInsets;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Templates v2 cover-letter layout.
 *
 * <p>Single-column layout with generous paragraph spacing — header
 * sits at the top, then greeting, body paragraphs, and closing
 * stack vertically beneath it. There is no slot map: a cover
 * letter is structurally simpler than a CV (one continuous reading
 * flow), so the layout takes the rendered nodes in source order and
 * emits one container.</p>
 */
public final class LetterFormat {

    private static final String LAYOUT_NAME = "layout.letterFormat";

    private double moduleGap = 0.0;

    private LetterFormat() {
    }

    /**
     * Returns a fresh layout with zero gap (caller drives spacing
     * through node margins).
     *
     * @return new layout instance
     */
    public static LetterFormat layout() {
        return new LetterFormat();
    }

    /**
     * Sets the vertical gap between top-level letter blocks (header,
     * greeting, body, closing).
     *
     * @param value non-negative finite gap in points
     * @return this layout (for chaining)
     * @throws IllegalArgumentException if {@code value} is negative,
     *         {@code NaN}, or infinite
     */
    public LetterFormat moduleGap(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            throw new IllegalArgumentException("moduleGap must be finite and non-negative: " + value);
        }
        this.moduleGap = value;
        return this;
    }

    /**
     * Composes the final {@link DocumentNode} from a header plus
     * ordered list of letter blocks (greeting, body, closing).
     *
     * @param header non-null header node
     * @param blocks non-null ordered list of letter blocks
     * @return container node holding the header and blocks in order
     * @throws NullPointerException if either argument is null
     */
    public DocumentNode compose(DocumentNode header, List<DocumentNode> blocks) {
        Objects.requireNonNull(header, "header");
        Objects.requireNonNull(blocks, "blocks");

        List<DocumentNode> children = new ArrayList<>(blocks.size() + 1);
        children.add(header);
        children.addAll(blocks);

        return new ContainerNode(
                LAYOUT_NAME,
                children,
                moduleGap,
                DocumentInsets.zero(),
                DocumentInsets.zero(),
                /* fillColor    */ null,
                /* stroke       */ null,
                /* cornerRadius */ null,
                /* borders      */ null);
    }
}
