package com.demcha.compose.layout_core.components.content.watermark;

import com.demcha.compose.layout_core.components.core.Component;
import lombok.Builder;
import lombok.Getter;

import java.awt.Color;
import java.nio.file.Path;

/**
 * Configuration for a document-wide watermark.
 *
 * <p>A watermark is either text-based or image-based. Text watermarks
 * support rotation, opacity, color, and font size. Image watermarks
 * support opacity and positioning.</p>
 *
 * <p>This is a document-level config attached to canonical PDF rendering,
 * not an ECS entity. The renderer applies it to every page after (or before)
 * content rendering depending on the {@link WatermarkLayer}.</p>
 *
 * @author Artem Demchyshyn
 */
@Getter
@Builder
public final class WatermarkConfig implements Component {

    // ---- Text watermark fields ----
    /** The text to render as a watermark (null if image-based). */
    private final String text;

    /** Font size for text watermarks (in points). */
    @Builder.Default
    private final float fontSize = 72f;

    /** Rotation angle in degrees for text watermarks. */
    @Builder.Default
    private final float rotation = 45f;

    /** Text/image color. */
    @Builder.Default
    private final Color color = Color.LIGHT_GRAY;

    // ---- Image watermark fields ----
    /** Path to a watermark image (null if text-based). */
    private final Path imagePath;

    /** Raw image bytes for the watermark (alternative to {@code imagePath}). */
    private final byte[] imageBytes;

    // ---- Common fields ----
    /** Opacity (alpha) from 0.0 (invisible) to 1.0 (fully opaque). */
    @Builder.Default
    private final float opacity = 0.15f;

    /** Z-layer for the watermark relative to content. */
    @Builder.Default
    private final WatermarkLayer layer = WatermarkLayer.BEHIND_CONTENT;

    /** Position on the page. */
    @Builder.Default
    private final WatermarkPosition position = WatermarkPosition.CENTER;

    /**
     * Returns {@code true} if this config describes a text watermark.
     */
    public boolean isTextBased() {
        return text != null && !text.isEmpty();
    }

    /**
     * Returns {@code true} if this config describes an image watermark.
     */
    public boolean isImageBased() {
        return imagePath != null || (imageBytes != null && imageBytes.length > 0);
    }
}
