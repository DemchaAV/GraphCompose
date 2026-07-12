package com.demcha.compose.engine.measurement;

import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.font.Font;
import com.demcha.compose.engine.font.FontLineMetrics;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Default measurement system backed by a document font library and a concrete
 * font implementation class supplied by the backend runtime.
 *
 * <p>Line metrics resolve polymorphically through the {@link Font} contract
 * ({@link Font#lineMetrics(TextStyle)} plus {@link Font#measurementCacheKey(TextStyle)}),
 * so every backend font — not only the PDF font — gets first-class metrics and
 * can opt into the process-wide cache without this shared class being modified
 * or special-cased per backend.</p>
 */
public final class FontLibraryTextMeasurementSystem implements TextMeasurementSystem {
    private static final int GLOBAL_LINE_METRICS_CACHE_LIMIT = 50_000;
    private static final int SESSION_TEXT_WIDTH_CACHE_LIMIT = 10_000;
    private static final ConcurrentMap<GlobalStyleKey, LineMetrics> GLOBAL_LINE_METRICS_CACHE = new ConcurrentHashMap<>();

    private final FontLibrary fonts;
    private final Class<? extends Font<?>> fontClass;
    private final Map<TextStyle, Font<?>> fontCache = new HashMap<>();
    private final Map<TextStyle, LineMetrics> lineMetricsCache = new HashMap<>();
    private final Map<TextStyle, Map<String, Double>> textWidthCache = new HashMap<>();
    private final Map<TextStyle, GlobalStyleKey> globalStyleKeyCache = new HashMap<>();

    public FontLibraryTextMeasurementSystem(FontLibrary fonts, Class<? extends Font<?>> fontClass) {
        this.fonts = Objects.requireNonNull(fonts, "fonts");
        this.fontClass = Objects.requireNonNull(fontClass, "fontClass");
    }

    @Override
    public ContentSize measure(TextStyle style, String text) {
        TextStyle safeStyle = style == null ? TextStyle.DEFAULT_STYLE : style;
        return new ContentSize(
                textWidth(safeStyle, text),
                lineMetrics(safeStyle).lineHeight()
        );
    }

    @Override
    public double textWidth(TextStyle style, String text) {
        TextStyle safeStyle = style == null ? TextStyle.DEFAULT_STYLE : style;
        String safeText = text == null ? "" : text;
        Map<String, Double> widthsByText = textWidthCache.computeIfAbsent(safeStyle, key -> new HashMap<>());
        Double cachedWidth = widthsByText.get(safeText);
        if (cachedWidth != null) {
            return cachedWidth;
        }
        Font<?> font = resolveFont(safeStyle);
        double width = resolveTextWidth(font, safeStyle, safeText);
        if (widthsByText.size() >= SESSION_TEXT_WIDTH_CACHE_LIMIT) {
            widthsByText.clear();
        }
        widthsByText.put(safeText, width);
        return width;
    }

    @Override
    public LineMetrics lineMetrics(TextStyle style) {
        TextStyle safeStyle = style == null ? TextStyle.DEFAULT_STYLE : style;
        return lineMetricsCache.computeIfAbsent(safeStyle, this::resolveLineMetrics);
    }

    private LineMetrics resolveLineMetrics(TextStyle style) {
        Font<?> font = resolveFont(style);
        String cacheKey = font.measurementCacheKey(style);
        if (cacheKey == null) {
            // Backend opted out of the process-wide cache; the per-session
            // lineMetricsCache (via lineMetrics(...)) still memoizes per style.
            return toLineMetrics(font.lineMetrics(style));
        }
        GlobalStyleKey key = globalStyleKey(style, cacheKey);
        LineMetrics cached = GLOBAL_LINE_METRICS_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        LineMetrics resolved = toLineMetrics(font.lineMetrics(style));
        cacheGlobalLineMetrics(key, resolved);
        return resolved;
    }

    private static LineMetrics toLineMetrics(FontLineMetrics metrics) {
        return new LineMetrics(metrics.ascent(), metrics.descent(), metrics.leading());
    }

    private double resolveTextWidth(Font<?> font, TextStyle style, String text) {
        return font.getTextWidth(style, text);
    }

    private Font<?> resolveFont(TextStyle style) {
        TextStyle safeStyle = style == null ? TextStyle.DEFAULT_STYLE : style;
        return fontCache.computeIfAbsent(safeStyle, key -> fonts.getFont(key.fontName(), fontClass)
                .orElseThrow(() -> new IllegalStateException("Font not found for style: " + key.fontName())));
    }

    private GlobalStyleKey globalStyleKey(TextStyle style, String cacheKey) {
        // Namespace the process-wide cache by backend font type: distinct backends
        // may return the same measurementCacheKey (e.g. both key on "Helvetica")
        // for different metrics, so without fontClass they would collide in the
        // shared static cache.
        return globalStyleKeyCache.computeIfAbsent(style,
                key -> new GlobalStyleKey(fontClass.getName(), cacheKey, key.size(), key.decoration()));
    }

    private static void cacheGlobalLineMetrics(GlobalStyleKey key, LineMetrics metrics) {
        // Safety cap on the process-wide line-metrics cache. Distinct styles are
        // few in real use (a handful of font/size/decoration combos); this only
        // guards a pathological style explosion. Stop inserting once full instead
        // of clear()-ing: the old full flush wiped every hot entry under
        // concurrent rendering (a thundering-herd recompute), so keeping the
        // existing entries is strictly better. This runs on a cache miss only,
        // never on the per-measurement get() path.
        if (GLOBAL_LINE_METRICS_CACHE.size() < GLOBAL_LINE_METRICS_CACHE_LIMIT) {
            GLOBAL_LINE_METRICS_CACHE.putIfAbsent(key, metrics);
        }
    }

    @Override
    public void clearCaches() {
        fontCache.clear();
        lineMetricsCache.clear();
        textWidthCache.clear();
        globalStyleKeyCache.clear();
    }

    int sessionTextWidthCacheSize() {
        int total = 0;
        for (Map<String, Double> widthsByText : textWidthCache.values()) {
            total += widthsByText.size();
        }
        return total;
    }

    private record GlobalStyleKey(String fontType, String fontKey, double size, TextDecoration decoration) {
    }

}
