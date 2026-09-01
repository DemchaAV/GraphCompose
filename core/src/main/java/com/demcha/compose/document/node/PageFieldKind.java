package com.demcha.compose.document.node;

/**
 * Which page number a {@link PageFieldNode} carries.
 *
 * @author Artem Demchyshyn
 * @since 2.3.0
 */
public enum PageFieldKind {

    /** The page the field is drawn on — Word's {@code PAGE} field. */
    NUMBER,

    /** The document's page count — Word's {@code NUMPAGES} field. */
    TOTAL
}
