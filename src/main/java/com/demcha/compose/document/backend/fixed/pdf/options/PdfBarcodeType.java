package com.demcha.compose.document.backend.fixed.pdf.options;

/**
 * Barcode and QR-code symbologies supported by the canonical PDF API.
 */
public enum PdfBarcodeType {
    QR_CODE,
    CODE_128,
    CODE_39,
    EAN_13,
    EAN_8,
    UPC_A,
    PDF_417,
    DATA_MATRIX
}
