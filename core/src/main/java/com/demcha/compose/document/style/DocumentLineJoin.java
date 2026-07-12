package com.demcha.compose.document.style;

/**
 * Line-join style for stroke corners, mirroring the PDF join vocabulary
 * (and SVG's {@code stroke-linejoin}).
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public enum DocumentLineJoin {
    /** Sharp mitred corner (PDF join 0, the default). */
    MITER(0),
    /** Rounded corner (PDF join 1). */
    ROUND(1),
    /** Flattened corner (PDF join 2). */
    BEVEL(2);

    private final int pdfCode;

    DocumentLineJoin(int pdfCode) {
        this.pdfCode = pdfCode;
    }

    /**
     * Returns the PDF line-join operator value.
     *
     * @return PDF join style code (0–2)
     */
    public int pdfCode() {
        return pdfCode;
    }
}
