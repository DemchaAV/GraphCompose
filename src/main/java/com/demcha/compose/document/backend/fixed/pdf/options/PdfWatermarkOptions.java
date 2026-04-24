package com.demcha.compose.document.backend.fixed.pdf.options;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.awt.Color;
import java.nio.file.Path;

/**
 * Canonical watermark configuration applied to every rendered PDF page.
 *
 * <p>A watermark may be text-based or image-based. The canonical document API
 * uses this type instead of exposing backend watermark config classes in public
 * signatures.</p>
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PdfWatermarkOptions {
    private final String text;

    @Builder.Default
    private final float fontSize = 72f;

    @Builder.Default
    private final float rotation = 45f;

    @Builder.Default
    private final Color color = Color.LIGHT_GRAY;

    private final Path imagePath;
    private final byte[] imageBytes;

    @Builder.Default
    private final float opacity = 0.15f;

    @Builder.Default
    private final PdfWatermarkLayer layer = PdfWatermarkLayer.BEHIND_CONTENT;

    @Builder.Default
    private final PdfWatermarkPosition position = PdfWatermarkPosition.CENTER;

    private PdfWatermarkOptions() {
        this.text = null;
        this.fontSize = 72f;
        this.rotation = 45f;
        this.color = Color.LIGHT_GRAY;
        this.imagePath = null;
        this.imageBytes = null;
        this.opacity = 0.15f;
        this.layer = PdfWatermarkLayer.BEHIND_CONTENT;
        this.position = PdfWatermarkPosition.CENTER;
    }

    /**
     * Returns {@code true} when the watermark is configured to render text.
     *
     * @return {@code true} for text-based watermark instances
     */
    public boolean isTextBased() {
        return text != null && !text.isBlank();
    }

    /**
     * Returns {@code true} when the watermark is configured to render an image.
     *
     * @return {@code true} for image-based watermark instances
     */
    public boolean isImageBased() {
        return imagePath != null || (imageBytes != null && imageBytes.length > 0);
    }
}
