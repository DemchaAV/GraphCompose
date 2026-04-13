package com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.helpers;

import com.demcha.compose.layout_core.components.content.watermark.WatermarkConfig;
import com.demcha.compose.layout_core.components.content.watermark.WatermarkPosition;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

import java.io.IOException;
import java.nio.file.Files;

/**
 * PDF-specific helper that renders a watermark on every page of a document.
 *
 * <p>This is a post-processing helper called after all content has been rendered.
 * It is not an ECS entity — watermarks are document-level concerns.</p>
 *
 * @author Artem Demchyshyn
 */
@Slf4j
public final class PdfWatermarkRenderer {

    private PdfWatermarkRenderer() {
        // static helper
    }

    /**
     * Applies the given watermark configuration to every page in the document.
     *
     * @param doc    the target PDF document
     * @param config the watermark configuration
     * @throws IOException if writing to the content stream fails
     */
    public static void apply(PDDocument doc, WatermarkConfig config) throws IOException {
        if (config == null) return;

        for (int i = 0; i < doc.getNumberOfPages(); i++) {
            PDPage page = doc.getPage(i);
            PDRectangle mediaBox = page.getMediaBox();

            // Watermark behind content uses PREPEND mode, above uses APPEND
            PDPageContentStream.AppendMode mode = switch (config.getLayer()) {
                case BEHIND_CONTENT -> PDPageContentStream.AppendMode.PREPEND;
                case ABOVE_CONTENT -> PDPageContentStream.AppendMode.APPEND;
            };

            try (PDPageContentStream cs = new PDPageContentStream(doc, page, mode, true, true)) {
                // Set opacity
                PDExtendedGraphicsState gState = new PDExtendedGraphicsState();
                gState.setNonStrokingAlphaConstant(config.getOpacity());
                gState.setStrokingAlphaConstant(config.getOpacity());
                cs.setGraphicsStateParameters(gState);

                if (config.isTextBased()) {
                    renderTextWatermark(cs, config, mediaBox);
                } else if (config.isImageBased()) {
                    renderImageWatermark(cs, doc, config, mediaBox);
                }
            }
        }
    }

    private static void renderTextWatermark(PDPageContentStream cs,
                                            WatermarkConfig config,
                                            PDRectangle mediaBox) throws IOException {
        PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        float fontSize = config.getFontSize();
        String text = config.getText();

        float textWidth = font.getStringWidth(text) / 1000f * fontSize;
        float textHeight = fontSize;

        cs.setNonStrokingColor(config.getColor());

        if (config.getPosition() == WatermarkPosition.TILE) {
            renderTiledText(cs, font, fontSize, text, textWidth, textHeight, config.getRotation(), mediaBox);
        } else {
            float[] pos = resolvePosition(config.getPosition(), mediaBox, textWidth, textHeight);
            float cx = pos[0];
            float cy = pos[1];

            cs.beginText();
            cs.setFont(font, fontSize);
            Matrix matrix = Matrix.getRotateInstance(
                    Math.toRadians(config.getRotation()), cx, cy);
            cs.setTextMatrix(matrix);
            cs.showText(text);
            cs.endText();
        }
    }

    private static void renderTiledText(PDPageContentStream cs,
                                        PDFont font, float fontSize,
                                        String text,
                                        float textWidth, float textHeight,
                                        float rotation,
                                        PDRectangle mediaBox) throws IOException {
        float spacingX = textWidth + 80;
        float spacingY = textHeight + 120;

        for (float y = -mediaBox.getHeight(); y < mediaBox.getHeight() * 2; y += spacingY) {
            for (float x = -mediaBox.getWidth(); x < mediaBox.getWidth() * 2; x += spacingX) {
                cs.beginText();
                cs.setFont(font, fontSize);
                Matrix matrix = Matrix.getRotateInstance(Math.toRadians(rotation), x, y);
                cs.setTextMatrix(matrix);
                cs.showText(text);
                cs.endText();
            }
        }
    }

    private static void renderImageWatermark(PDPageContentStream cs,
                                             PDDocument doc,
                                             WatermarkConfig config,
                                             PDRectangle mediaBox) throws IOException {
        PDImageXObject image;
        if (config.getImagePath() != null) {
            byte[] bytes = Files.readAllBytes(config.getImagePath());
            image = PDImageXObject.createFromByteArray(doc, bytes, "watermark");
        } else {
            image = PDImageXObject.createFromByteArray(doc, config.getImageBytes(), "watermark");
        }

        float imgW = image.getWidth();
        float imgH = image.getHeight();

        if (config.getPosition() == WatermarkPosition.TILE) {
            float spacingX = imgW + 60;
            float spacingY = imgH + 60;
            for (float y = 0; y < mediaBox.getHeight(); y += spacingY) {
                for (float x = 0; x < mediaBox.getWidth(); x += spacingX) {
                    cs.drawImage(image, x, y, imgW, imgH);
                }
            }
        } else {
            float[] pos = resolvePosition(config.getPosition(), mediaBox, imgW, imgH);
            cs.drawImage(image, pos[0], pos[1], imgW, imgH);
        }
    }

    private static float[] resolvePosition(WatermarkPosition position,
                                           PDRectangle mediaBox,
                                           float objWidth, float objHeight) {
        float pageW = mediaBox.getWidth();
        float pageH = mediaBox.getHeight();

        return switch (position) {
            case CENTER -> new float[]{(pageW - objWidth) / 2f, (pageH - objHeight) / 2f};
            case TOP_LEFT -> new float[]{20, pageH - objHeight - 20};
            case TOP_RIGHT -> new float[]{pageW - objWidth - 20, pageH - objHeight - 20};
            case BOTTOM_LEFT -> new float[]{20, 20};
            case BOTTOM_RIGHT -> new float[]{pageW - objWidth - 20, 20};
            case TILE -> new float[]{0, 0}; // handled separately
        };
    }
}
