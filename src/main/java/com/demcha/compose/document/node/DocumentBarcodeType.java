package com.demcha.compose.document.node;

/**
 * Backend-neutral barcode and QR-code symbologies supported by GraphCompose.
 *
 * @author Artem Demchyshyn
 */
public enum DocumentBarcodeType {
    /** QR code. */
    QR_CODE,
    /** Code 128 barcode. */
    CODE_128,
    /** Code 39 barcode. */
    CODE_39,
    /** EAN-13 barcode. */
    EAN_13,
    /** EAN-8 barcode. */
    EAN_8,
    /** UPC-A barcode. */
    UPC_A,
    /** PDF417 barcode. */
    PDF_417,
    /** Data Matrix barcode. */
    DATA_MATRIX
}
