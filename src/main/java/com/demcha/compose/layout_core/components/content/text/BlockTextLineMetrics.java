package com.demcha.compose.layout_core.components.content.text;

import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.LayoutSystem;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfFont;
import com.demcha.compose.layout_core.system.interfaces.Font;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves per-line vertical metrics for block text so layout can react to
 * mixed markdown styles such as headings inside one block.
 */
public final class BlockTextLineMetrics {

    private BlockTextLineMetrics() {
    }

    public static LineMetrics resolveStyleMetrics(EntityManager entityManager, TextStyle style) {
        TextStyle safeStyle = style == null ? TextStyle.DEFAULT_STYLE : style;
        @SuppressWarnings("unchecked")
        Class<? extends Font<?>> fontClass = (Class<? extends Font<?>>) entityManager.getSystems()
                .getSystem(LayoutSystem.class)
                .orElseThrow(() -> new IllegalStateException("LayoutSystem is required to resolve text metrics"))
                .getRenderingSystem()
                .fontClazz();

        Font<?> font = (Font<?>) entityManager.getFonts()
                .getFont(safeStyle.fontName(), fontClass)
                .orElseThrow(() -> new IllegalStateException("Font is not registered: " + safeStyle.fontName()));

        if (font instanceof PdfFont pdfFont) {
            PdfFont.VerticalMetrics metrics = pdfFont.verticalMetrics(safeStyle);
            return new LineMetrics(metrics.ascent(), metrics.descent(), metrics.leading());
        }

        double lineHeight = Math.max(0.0, font.getLineHeight(safeStyle));
        return new LineMetrics(lineHeight, 0.0, 0.0);
    }

    public static LineMetrics resolveLineMetrics(EntityManager entityManager, LineTextData line, TextStyle fallbackStyle) {
        if (line == null || line.bodies().isEmpty()) {
            return resolveStyleMetrics(entityManager, fallbackStyle);
        }

        double ascent = 0.0;
        double descent = 0.0;
        double leading = 0.0;
        boolean hasMetrics = false;

        for (TextDataBody body : line.bodies()) {
            if (body == null) {
                continue;
            }

            TextStyle style = body.textStyle() == null ? fallbackStyle : body.textStyle();
            if (style == null) {
                continue;
            }

            LineMetrics metrics = resolveStyleMetrics(entityManager, style);
            ascent = Math.max(ascent, metrics.ascent());
            descent = Math.max(descent, metrics.descent());
            leading = Math.max(leading, metrics.leading());
            hasMetrics = true;
        }

        if (!hasMetrics) {
            return resolveStyleMetrics(entityManager, fallbackStyle);
        }

        return new LineMetrics(ascent, descent, leading);
    }

    public static List<LineMetrics> resolveLineMetrics(EntityManager entityManager,
                                                       List<LineTextData> lines,
                                                       TextStyle fallbackStyle) {
        List<LineMetrics> result = new ArrayList<>(lines.size());
        for (LineTextData line : lines) {
            result.add(resolveLineMetrics(entityManager, line, fallbackStyle));
        }
        return result;
    }

    public static double interLineGap(LineMetrics previous,
                                      LineMetrics next,
                                      LineMetrics base,
                                      double spacing) {
        return spacing + previous.outerGap(base) / 2.0 + next.outerGap(base) / 2.0;
    }

    public record LineMetrics(double ascent, double descent, double leading) {
        public double lineHeight() {
            return ascent + descent + leading;
        }

        public double baselineOffsetFromBottom() {
            return descent;
        }

        /**
         * Larger markdown lines such as headings get a small outer gap so they
         * visually breathe before and after neighboring body lines.
         */
        public double outerGap(LineMetrics base) {
            if (base == null) {
                return 0.0;
            }
            return Math.max(0.0, lineHeight() - base.lineHeight()) / 2.0;
        }
    }
}
