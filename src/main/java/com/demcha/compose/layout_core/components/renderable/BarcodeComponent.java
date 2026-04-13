package com.demcha.compose.layout_core.components.renderable;

import com.demcha.compose.layout_core.system.interfaces.Render;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Render marker for a barcode/QR-code leaf entity.
 *
 * <p>Like all render markers in the engine, this class describes intent
 * without performing any drawing. The actual bitmap generation and PDF
 * rendering is handled by {@code PdfBarcodeRenderHandler}.</p>
 *
 * @author Artem Demchyshyn
 */
@Slf4j
@EqualsAndHashCode
@NoArgsConstructor
public class BarcodeComponent implements Render {
}
