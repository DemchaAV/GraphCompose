package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;

/**
 * A page number the consuming format resolves for itself.
 *
 * <p>Exists because the two export lanes learn the number at different moments.
 * A fixed-layout backend is handed a paginated document, so a page zone can put
 * the number in as text. A semantic backend is handed a node tree and no page
 * count at all — Word paginates, not GraphCompose — so a number written as text
 * would be right on one page and wrong on the rest. This node carries the
 * <em>question</em> instead of an answer, and each lane answers it: a
 * fixed-layout zone never sees one (its {@code PageContext} hands out resolved
 * text), and a semantic export maps it onto the format's own field — {@code
 * PAGE} and {@code NUMPAGES} in Word — which stays live when the reader edits
 * the document.</p>
 *
 * <p>Authors do not construct this directly; a zone's {@code PageContext}
 * produces it through {@code pageNumber()} / {@code pageTotal()}, which return
 * resolved text or this marker depending on which lane is asking.</p>
 *
 * @param name      node name used in snapshots and layout graph paths
 * @param kind      which of the two numbers this field carries
 * @param textStyle text style for the number
 * @param align     horizontal alignment within the node box
 * @param padding   inner padding
 * @param margin    outer margin
 * @author Artem Demchyshyn
 * @since 2.2.3
 */
public record PageFieldNode(
        String name,
        PageFieldKind kind,
        DocumentTextStyle textStyle,
        TextAlign align,
        DocumentInsets padding,
        DocumentInsets margin
) implements DocumentNode {

    /**
     * Normalizes defaults and requires a kind.
     */
    public PageFieldNode {
        name = name == null ? "" : name;
        if (kind == null) {
            throw new IllegalArgumentException("PageFieldNode kind must not be null.");
        }
        textStyle = textStyle == null ? DocumentTextStyle.DEFAULT : textStyle;
        align = align == null ? TextAlign.LEFT : align;
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
    }

    /**
     * A field of the given kind with default spacing and alignment.
     *
     * @param kind      which of the two numbers this field carries
     * @param textStyle text style for the number
     */
    public PageFieldNode(PageFieldKind kind, DocumentTextStyle textStyle) {
        this("", kind, textStyle, TextAlign.LEFT, DocumentInsets.zero(), DocumentInsets.zero());
    }
}
