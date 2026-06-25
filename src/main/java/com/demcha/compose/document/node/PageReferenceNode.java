package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;

/**
 * Leaf that prints the resolved page number of a named {@code anchor(...)} — the
 * "see page N" cross-reference. The number is resolved from the laid-out
 * document on a second layout pass; until then (or when the anchor is unknown)
 * it renders {@code placeholderText}.
 *
 * <p>Because the number is resolved after layout, a document containing a
 * page reference is laid out twice; the reference reserves a single text line in
 * both passes, so its own footprint does not shift the pages it reports.</p>
 *
 * <p>Resolved in document flows and rows (the table-of-contents row). A reference
 * nested inside a table cell is not resolved in this release.</p>
 *
 * @param name           node name used in snapshots and layout graph paths
 * @param anchor         the anchor whose page number is printed
 * @param textStyle      text style for the number
 * @param align          horizontal alignment within the node box
 * @param placeholderText text shown before the number is resolved, or when the
 *                       anchor is unknown
 * @param padding        inner padding
 * @param margin         outer margin
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public record PageReferenceNode(
        String name,
        String anchor,
        DocumentTextStyle textStyle,
        TextAlign align,
        String placeholderText,
        DocumentInsets padding,
        DocumentInsets margin
) implements DocumentNode {

    /**
     * Normalizes defaults and requires a non-blank anchor.
     */
    public PageReferenceNode {
        name = name == null ? "" : name;
        anchor = anchor == null ? "" : anchor.trim();
        if (anchor.isBlank()) {
            throw new IllegalArgumentException("PageReferenceNode anchor must not be blank.");
        }
        textStyle = textStyle == null ? DocumentTextStyle.DEFAULT : textStyle;
        align = align == null ? TextAlign.LEFT : align;
        placeholderText = placeholderText == null ? "" : placeholderText;
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
    }

    /**
     * Convenience constructor for an unnamed reference with default spacing and no
     * placeholder text.
     *
     * @param anchor    the anchor whose page number is printed
     * @param textStyle text style for the number
     * @param align     horizontal alignment within the node box
     */
    public PageReferenceNode(String anchor, DocumentTextStyle textStyle, TextAlign align) {
        this("", anchor, textStyle, align, "", DocumentInsets.zero(), DocumentInsets.zero());
    }
}
