package com.demcha.compose.layout_core.system.implemented_systems.pdf_systems;

import com.demcha.compose.layout_core.components.content.ImageData;
import com.demcha.compose.layout_core.components.content.ImageMetadata;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
final class PdfImageCache {
    private static final double SCALED_VARIANT_THRESHOLD = 0.5d;
    private static final String DRAW_MODE = "draw";

    private final PDDocument document;
    private final Map<String, PDImageXObject> originalImages = new HashMap<>();
    private final Map<ScaledImageKey, PDImageXObject> scaledVariants = new HashMap<>();

    PdfImageCache(PDDocument document) {
        this.document = document;
    }

    PDImageXObject getOrCreateOriginal(ImageData imageData) throws IOException {
        PDImageXObject cached = originalImages.get(imageData.getFingerprint());
        if (cached != null) {
            return cached;
        }

        PDImageXObject created = PDImageXObject.createFromByteArray(
                document,
                imageData.getBytes(),
                imageData.getFingerprint()
        );
        originalImages.put(imageData.getFingerprint(), created);
        return created;
    }

    PDImageXObject getOrCreateBestFit(ImageData imageData, double targetWidth, double targetHeight) throws IOException {
        ScaledImageKey variantKey = toVariantKey(imageData, targetWidth, targetHeight);
        if (variantKey == null || !shouldUseScaledVariant(imageData.getMetadata(), variantKey)) {
            return getOrCreateOriginal(imageData);
        }

        PDImageXObject cached = scaledVariants.get(variantKey);
        if (cached != null) {
            return cached;
        }

        byte[] scaledBytes = scaleImageBytes(imageData, variantKey.targetWidthPx(), variantKey.targetHeightPx());
        PDImageXObject created = PDImageXObject.createFromByteArray(
                document,
                scaledBytes,
                variantKey.cacheName()
        );
        scaledVariants.put(variantKey, created);
        return created;
    }

    PdfRenderingSystemECS.ImageCacheStats stats() {
        return new PdfRenderingSystemECS.ImageCacheStats(originalImages.size(), scaledVariants.size());
    }

    private boolean shouldUseScaledVariant(ImageMetadata metadata, ScaledImageKey key) {
        return key.targetWidthPx() > 0
               && key.targetHeightPx() > 0
               && key.targetWidthPx() <= metadata.width() * SCALED_VARIANT_THRESHOLD
               && key.targetHeightPx() <= metadata.height() * SCALED_VARIANT_THRESHOLD;
    }

    private ScaledImageKey toVariantKey(ImageData imageData, double targetWidth, double targetHeight) {
        int roundedWidth = Math.max(1, (int) Math.round(targetWidth));
        int roundedHeight = Math.max(1, (int) Math.round(targetHeight));
        if (Double.isNaN(targetWidth) || Double.isInfinite(targetWidth) || Double.isNaN(targetHeight) || Double.isInfinite(targetHeight)) {
            return null;
        }
        return new ScaledImageKey(imageData.getFingerprint(), roundedWidth, roundedHeight, DRAW_MODE);
    }

    private byte[] scaleImageBytes(ImageData imageData, int targetWidth, int targetHeight) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData.getBytes())) {
            BufferedImage source = ImageIO.read(inputStream);
            if (source == null) {
                throw new IllegalStateException("Unable to decode raster image for scaled variant");
            }

            int imageType = source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
            BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, imageType);
            Graphics2D graphics = scaled.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
            } finally {
                graphics.dispose();
            }

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                ImageIO.write(scaled, "png", outputStream);
                return outputStream.toByteArray();
            }
        }
    }

    private record ScaledImageKey(String fingerprint, int targetWidthPx, int targetHeightPx, String mode) {
        String cacheName() {
            return fingerprint + "-" + targetWidthPx + "x" + targetHeightPx + "-" + mode;
        }
    }
}
