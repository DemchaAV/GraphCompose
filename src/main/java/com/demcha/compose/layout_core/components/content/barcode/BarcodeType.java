package com.demcha.compose.layout_core.components.content.barcode;

/**
 * Supported barcode/QR symbology types.
 *
 * <p>Each constant maps to a ZXing {@code BarcodeFormat} used during image
 * generation. The engine treats the generated bitmap as a fixed-size leaf,
 * similar to {@code ImageComponent}.</p>
 *
 * @author Artem Demchyshyn
 */
public enum BarcodeType {

    /** 2-D matrix code — the most popular format for URLs and short payloads. */
    QR_CODE,

    /** High-density linear barcode — common in logistics and inventory. */
    CODE_128,

    /** Alphanumeric linear barcode — established systems and US DoD. */
    CODE_39,

    /** European Article Number — retail product identification (13 digits). */
    EAN_13,

    /** Short-form EAN — small packaging (8 digits). */
    EAN_8,

    /** Universal Product Code — North American retail (12 digits). */
    UPC_A,

    /** Stacked 2-D barcode — high-capacity data encoding. */
    PDF_417,

    /** Compact 2-D matrix — industrial and postal applications. */
    DATA_MATRIX
}
