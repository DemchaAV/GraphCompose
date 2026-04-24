package com.demcha.compose.document.backend.fixed.pdf.options;

/**
 * Barcode and QR-code symbologies supported by the canonical PDF API.
 */
public enum PdfBarcodeType {
    /**
     * QR Code matrix barcode.
     */
    QR_CODE,
    /**
     * Code 128 linear barcode.
     */
    CODE_128,
    /**
     * Code 39 linear barcode.
     */
    CODE_39,
    /**
     * EAN-13 retail barcode.
     */
    EAN_13,
    /**
     * EAN-8 retail barcode.
     */
    EAN_8,
    /**
     * UPC-A retail barcode.
     */
    UPC_A,
    /**
     * PDF417 stacked barcode.
     */
    PDF_417,
    /**
     * Data Matrix 2D barcode.
     */
    DATA_MATRIX
}
