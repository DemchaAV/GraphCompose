package com.demcha.compose.engine.font;

import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.geometry.ContentSize;

public interface Font<T> {


    T defaultFont();

    T bold();

    T italic();

    T boldItalic();

    T underline();

    T strikethrough();

    default T fontType(TextDecoration textDecoration) {
        if (textDecoration == null) {
            return defaultFont();
        }
        return switch (textDecoration) {
            case BOLD -> bold();
            case ITALIC -> italic();
            case UNDERLINE -> underline();
            case BOLD_ITALIC -> boldItalic();
            case STRIKETHROUGH -> strikethrough();
            default -> defaultFont();
        };
    }

    double getTextWidth(TextStyle style, String text);

    double getTextWidthNoSanitize(TextStyle style, String text);


    double getLineHeight(TextStyle style);

    double getTextHeight(TextStyle style);

    double getCapHeight(TextStyle style);

    /**
     * Resolves backend-neutral vertical metrics (ascent, descent, leading) for the
     * supplied style.
     *
     * <p>The shared text-measurement system calls this polymorphically, so a
     * backend supplies first-class line metrics by overriding this method rather
     * than being special-cased in shared measurement code. The default derives a
     * degraded metric from {@link #getLineHeight} with zero descent and leading;
     * backends with real font metrics (ascent/descent/leading) should override.</p>
     *
     * @param style the resolved text style
     * @return vertical metrics in document units
     */
    default FontLineMetrics lineMetrics(TextStyle style) {
        return new FontLineMetrics(Math.max(0.0, getLineHeight(style)), 0.0, 0.0);
    }

    /**
     * Returns a process-stable key identifying this font's metrics for the supplied
     * style, or {@code null} to opt out of the shared process-wide line-metrics
     * cache (per-session caching still applies).
     *
     * <p>Backends whose {@link #lineMetrics} computation is expensive — e.g. it
     * reads font-descriptor tables — should return a stable non-null key so
     * identical styles resolve once per process across sessions and threads. Cheap
     * or stub backends can leave the default and skip the global cache.</p>
     *
     * @param style the resolved text style
     * @return a stable cache key, or {@code null} to skip the global cache
     */
    default String measurementCacheKey(TextStyle style) {
        return null;
    }

    double scale(double size);

    /**
     * @param text           the text to fit
     * @param style          the starting style
     * @param availableWidth the width to fit within
     * @return a re-sized style
     * @deprecated Unused and incorrect: the only real implementation re-measures
     *     with the unchanged {@code style}, so the loop never converges and the
     *     result is always the minimum size. Canonical auto-size is resolved by the
     *     layout compiler ({@code TextFlowSupport.resolveAutoSizeTextStyle}); this
     *     method has no callers and is kept only for binary compatibility,
     *     scheduled for removal in the next major.
     */
    @Deprecated
    TextStyle adjustFontSizeToFit(String text, TextStyle style, double availableWidth);

    ContentSize getTightBounds(String text, TextStyle style);
}
