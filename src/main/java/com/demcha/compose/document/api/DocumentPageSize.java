package com.demcha.compose.document.api;

/**
 * Backend-neutral physical page size expressed in PDF points.
 *
 * <p>The values are deliberately plain geometry. PDFBox-specific page objects
 * are created only by the PDF backend when a document is rendered.</p>
 *
 * @param width page width in points
 * @param height page height in points
 */
public record DocumentPageSize(double width, double height) {
    /**
     * ISO A4 portrait page size in points.
     */
    public static final DocumentPageSize A4 = new DocumentPageSize(595.27563, 841.88977);

    /**
     * US Letter portrait page size in points.
     */
    public static final DocumentPageSize LETTER = new DocumentPageSize(612.0, 792.0);

    /**
     * US Legal portrait page size in points.
     */
    public static final DocumentPageSize LEGAL = new DocumentPageSize(612.0, 1008.0);

    /**
     * Creates a page size from point dimensions.
     *
     * @param width page width in points
     * @param height page height in points
     */
    public DocumentPageSize {
        if (!Double.isFinite(width) || width <= 0.0) {
            throw new IllegalArgumentException("Page width must be a positive finite value.");
        }
        if (!Double.isFinite(height) || height <= 0.0) {
            throw new IllegalArgumentException("Page height must be a positive finite value.");
        }
    }

    /**
     * Creates a page size from point dimensions.
     *
     * @param width page width in points
     * @param height page height in points
     * @return page size value
     */
    public static DocumentPageSize of(double width, double height) {
        return new DocumentPageSize(width, height);
    }

    /**
     * Returns this size with the longer side used as width.
     *
     * @return landscape-oriented page size
     */
    public DocumentPageSize landscape() {
        return width >= height ? this : new DocumentPageSize(height, width);
    }

    /**
     * Returns this size with the shorter side used as width.
     *
     * @return portrait-oriented page size
     */
    public DocumentPageSize portrait() {
        return width <= height ? this : new DocumentPageSize(height, width);
    }
}
