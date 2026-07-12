package com.demcha.compose.document.style;

/**
 * Line-cap style for open stroke ends, mirroring the PDF cap vocabulary
 * (and SVG's {@code stroke-linecap}).
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public enum DocumentLineCap {
    /** Squared-off end exactly at the endpoint (PDF cap 0, the default). */
    BUTT(0),
    /** Semicircular end centred on the endpoint (PDF cap 1). */
    ROUND(1),
    /** Squared-off end projecting half a line width past the endpoint (PDF cap 2). */
    SQUARE(2);

    private final int pdfCode;

    DocumentLineCap(int pdfCode) {
        this.pdfCode = pdfCode;
    }

    /**
     * Returns the PDF line-cap operator value.
     *
     * @return PDF cap style code (0–2)
     */
    public int pdfCode() {
        return pdfCode;
    }
}
