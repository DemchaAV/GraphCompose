package com.demcha.compose.document.output;

import com.demcha.compose.document.style.DocumentColor;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.nio.file.Path;

/**
 * Backend-neutral watermark applied to every rendered page.
 *
 * <p>A watermark may be text-based or image-based. Backends that cannot
 * render watermarks (for example purely tabular exports) simply ignore the
 * value.</p>
 *
 * @author Artem Demchyshyn
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DocumentWatermark {
    private final String text;

    @Builder.Default
    private final float fontSize = 72f;

    @Builder.Default
    private final float rotation = 45f;

    @Builder.Default
    private final DocumentColor color = DocumentColor.LIGHT_GRAY;

    private final Path imagePath;
    private final byte[] imageBytes;

    @Builder.Default
    private final float opacity = 0.15f;

    @Builder.Default
    private final DocumentWatermarkLayer layer = DocumentWatermarkLayer.BEHIND_CONTENT;

    @Builder.Default
    private final DocumentWatermarkPosition position = DocumentWatermarkPosition.CENTER;

    private DocumentWatermark() {
        this.text = null;
        this.fontSize = 72f;
        this.rotation = 45f;
        this.color = DocumentColor.LIGHT_GRAY;
        this.imagePath = null;
        this.imageBytes = null;
        this.opacity = 0.15f;
        this.layer = DocumentWatermarkLayer.BEHIND_CONTENT;
        this.position = DocumentWatermarkPosition.CENTER;
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
