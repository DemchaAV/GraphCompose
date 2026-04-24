package com.demcha.compose.engine.measurement;

import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.font.Font;
import com.demcha.compose.engine.render.pdf.PdfFont;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Default measurement system backed by a document font library and a concrete
 * font implementation class supplied by the backend runtime.
 */
public final class FontLibraryTextMeasurementSystem implements TextMeasurementSystem {
    private static final int GLOBAL_CACHE_LIMIT = 50_000;
    private static final ConcurrentMap<GlobalPdfStyleKey, ConcurrentMap<String, Double>> GLOBAL_PDF_TEXT_WIDTH_CACHE =
            new ConcurrentHashMap<>();
    private static final ConcurrentMap<GlobalPdfStyleKey, LineMetrics> GLOBAL_PDF_LINE_METRICS_CACHE = new ConcurrentHashMap<>();

    private final FontLibrary fonts;
    private final Class<? extends Font<?>> fontClass;
    private final Map<TextStyle, Font<?>> fontCache = new HashMap<>();
    private final Map<TextStyle, LineMetrics> lineMetricsCache = new HashMap<>();
    private final Map<TextStyle, Map<String, Double>> textWidthCache = new HashMap<>();
    private final Map<TextStyle, GlobalPdfStyleKey> globalPdfStyleKeyCache = new HashMap<>();

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
        if (font instanceof PdfFont pdfFont) {
            GlobalPdfStyleKey cacheKey = globalPdfStyleKey(pdfFont, style);
            LineMetrics cached = GLOBAL_PDF_LINE_METRICS_CACHE.get(cacheKey);
            if (cached != null) {
                return cached;
            }
            var metrics = pdfFont.verticalMetrics(style);
            LineMetrics resolved = new LineMetrics(metrics.ascent(), metrics.descent(), metrics.leading());
            cacheGlobalLineMetrics(cacheKey, resolved);
            return resolved;
        }

        double lineHeight = Math.max(0.0, font.getLineHeight(style));
        return new LineMetrics(lineHeight, 0.0, 0.0);
    }

    private double resolveTextWidth(Font<?> font, TextStyle style, String text) {
        if (font instanceof PdfFont pdfFont) {
            GlobalPdfStyleKey styleKey = globalPdfStyleKey(pdfFont, style);
            ConcurrentMap<String, Double> widthsByText = GLOBAL_PDF_TEXT_WIDTH_CACHE.get(styleKey);
            if (widthsByText != null) {
                Double cached = widthsByText.get(text);
                if (cached != null) {
                    return cached;
                }
            }
            double width = pdfFont.getTextWidth(style, text);
            cacheGlobalTextWidth(styleKey, text, width);
            return width;
        }
        return font.getTextWidth(style, text);
    }

    private Font<?> resolveFont(TextStyle style) {
        TextStyle safeStyle = style == null ? TextStyle.DEFAULT_STYLE : style;
        return fontCache.computeIfAbsent(safeStyle, key -> fonts.getFont(key.fontName(), fontClass)
                .orElseThrow(() -> new IllegalStateException("Font not found for style: " + key.fontName())));
    }

    private GlobalPdfStyleKey globalPdfStyleKey(PdfFont font, TextStyle style) {
        return globalPdfStyleKeyCache.computeIfAbsent(style, key -> GlobalPdfStyleKey.from(font, key));
    }

    private static void cacheGlobalTextWidth(GlobalPdfStyleKey key, String text, double width) {
        ConcurrentMap<String, Double> widthsByText = GLOBAL_PDF_TEXT_WIDTH_CACHE.computeIfAbsent(
                key,
                ignored -> new ConcurrentHashMap<>());
        if (widthsByText.size() > GLOBAL_CACHE_LIMIT) {
            widthsByText.clear();
        }
        widthsByText.putIfAbsent(text, width);
    }

    private static void cacheGlobalLineMetrics(GlobalPdfStyleKey key, LineMetrics metrics) {
        if (GLOBAL_PDF_LINE_METRICS_CACHE.size() > GLOBAL_CACHE_LIMIT) {
            GLOBAL_PDF_LINE_METRICS_CACHE.clear();
        }
        GLOBAL_PDF_LINE_METRICS_CACHE.putIfAbsent(key, metrics);
    }

    private record GlobalPdfStyleKey(String fontKey, double size, TextDecoration decoration) {
        private static GlobalPdfStyleKey from(PdfFont font, TextStyle style) {
            return new GlobalPdfStyleKey(font.measurementCacheKey(style), style.size(), style.decoration());
        }
    }

}
