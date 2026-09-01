package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;

/**
 * A page number the consuming format resolves for itself.
 *
 * <p>Exists because the two export lanes learn the number at different moments.
 * A fixed-layout backend paginates before a page zone is drawn; a semantic
 * backend hands the node tree to Word and no page count exists at all — Word
 * paginates, not GraphCompose — so a number written as text would be right on
 * one page and wrong on the rest. This node carries the <em>question</em>
 * instead of an answer, and each lane answers it: a paginated render resolves
 * it through the field's own layout definition, which reads the page the zone
 * publishes while compiling the band, and a semantic export maps it onto the
 * format's own field — {@code PAGE} and {@code NUMPAGES} in Word — which stays
 * live when the reader edits the document.</p>
 *
 * <p>A zone's {@code PageContext} produces one through {@code pageNumber()} /
 * {@code pageTotal()} in both lanes. Constructed anywhere else it is still a
 * node like any other; where nothing publishes a page number it renders blank
 * rather than failing the document.</p>
 *
 * @param name      node name used in snapshots and layout graph paths
 * @param kind      which of the two numbers this field carries
 * @param textStyle text style for the number
 * @param align     horizontal alignment within the node box
 * @param padding   inner padding
 * @param margin    outer margin
 * @author Artem Demchyshyn
 * @since 2.3.0
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
