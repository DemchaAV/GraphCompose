package com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.handlers;

import com.demcha.compose.layout_core.components.content.barcode.BarcodeData;
import com.demcha.compose.layout_core.components.content.barcode.BarcodeType;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.components.renderable.BarcodeComponent;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import com.demcha.compose.layout_core.system.interfaces.guides.GuidesRenderer;
import com.demcha.compose.layout_core.system.rendering.RenderHandler;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.oned.Code39Writer;
import com.google.zxing.oned.EAN13Writer;
import com.google.zxing.oned.EAN8Writer;
import com.google.zxing.oned.UPCAWriter;
import com.google.zxing.pdf417.PDF417Writer;
import com.google.zxing.datamatrix.DataMatrixWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * PDF render handler for barcode/QR-code entities.
 *
 * <p>Generates a barcode bitmap using ZXing, converts it to a
 * {@link PDImageXObject}, and draws it into the resolved placement box.
 * The barcode image is generated at draw time — no intermediate file is
 * needed.</p>
 *
 * @author Artem Demchyshyn
 */
@Slf4j
public final class PdfBarcodeRenderHandler implements RenderHandler<BarcodeComponent, PdfRenderingSystemECS> {

    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING, GuidesRenderer.Guide.BOX);

    @Override
    public Class<BarcodeComponent> renderType() {
        return BarcodeComponent.class;
    }

    @Override
    public boolean render(EntityManager manager,
                          Entity entity,
                          BarcodeComponent renderComponent,
                          PdfRenderingSystemECS renderingSystem,
                          boolean guideLines) throws IOException {
        BarcodeData barcodeData = entity.getComponent(BarcodeData.class).orElse(null);
        Placement placement = entity.getComponent(Placement.class).orElse(null);

        if (barcodeData == null || placement == null) {
            log.warn("Skipping barcode render because BarcodeData or Placement is missing for {}", entity);
            return false;
        }

        Padding padding = entity.getComponent(Padding.class).orElse(Padding.zero());
        double x = placement.x() + padding.left();
        double y = placement.y() + padding.bottom();
        double width = Math.max(0.0, placement.width() - padding.horizontal());
        double height = Math.max(0.0, placement.height() - padding.vertical());

        PDPageContentStream stream = renderingSystem.pageSurface(entity);
        if (width > 0.0 && height > 0.0) {
            BufferedImage barcodeImage = generateBarcodeImage(barcodeData, (int) width, (int) height);
            PDImageXObject xObject = createXObject(renderingSystem, barcodeImage);
            stream.drawImage(xObject, (float) x, (float) y, (float) width, (float) height);
        }

        if (guideLines) {
            renderingSystem.guidesRenderer().guidesRender(entity, stream, DEFAULT_GUIDES);
        }

        return width > 0.0 && height > 0.0;
    }

    private BufferedImage generateBarcodeImage(BarcodeData data, int width, int height) throws IOException {
        try {
            BarcodeFormat format = mapFormat(data.getType());

            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            if (data.getMargin() >= 0) {
                hints.put(EncodeHintType.MARGIN, data.getMargin());
            }

            // Use at least 2x resolution for sharpness, then the PDF handler scales
            int renderWidth = Math.max(width * 2, 200);
            int renderHeight = Math.max(height * 2, 200);

            BitMatrix matrix = createWriter(data.getType()).encode(data.getContent(), format, renderWidth, renderHeight, hints);

            int matrixWidth = matrix.getWidth();
            int matrixHeight = matrix.getHeight();
            BufferedImage image = new BufferedImage(matrixWidth, matrixHeight, BufferedImage.TYPE_INT_ARGB);

            int fgRgb = data.getForeground().getRGB();
            int bgRgb = data.getBackground().getRGB();

            for (int py = 0; py < matrixHeight; py++) {
                for (int px = 0; px < matrixWidth; px++) {
                    image.setRGB(px, py, matrix.get(px, py) ? fgRgb : bgRgb);
                }
            }

            return image;
        } catch (WriterException e) {
            throw new IOException("Failed to generate barcode for type " + data.getType() + ": " + e.getMessage(), e);
        }
    }

    private PDImageXObject createXObject(PdfRenderingSystemECS renderingSystem, BufferedImage image) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", baos);
            return PDImageXObject.createFromByteArray(renderingSystem.doc(), baos.toByteArray(), "barcode");
        }
    }

    private com.google.zxing.Writer createWriter(BarcodeType type) {
        return switch (type) {
            case QR_CODE -> new QRCodeWriter();
            case CODE_128 -> new Code128Writer();
            case CODE_39 -> new Code39Writer();
            case EAN_13 -> new EAN13Writer();
            case EAN_8 -> new EAN8Writer();
            case UPC_A -> new UPCAWriter();
            case PDF_417 -> new PDF417Writer();
            case DATA_MATRIX -> new DataMatrixWriter();
        };
    }

    private BarcodeFormat mapFormat(BarcodeType type) {
        return switch (type) {
            case QR_CODE -> BarcodeFormat.QR_CODE;
            case CODE_128 -> BarcodeFormat.CODE_128;
            case CODE_39 -> BarcodeFormat.CODE_39;
            case EAN_13 -> BarcodeFormat.EAN_13;
            case EAN_8 -> BarcodeFormat.EAN_8;
            case UPC_A -> BarcodeFormat.UPC_A;
            case PDF_417 -> BarcodeFormat.PDF_417;
            case DATA_MATRIX -> BarcodeFormat.DATA_MATRIX;
        };
    }
}
