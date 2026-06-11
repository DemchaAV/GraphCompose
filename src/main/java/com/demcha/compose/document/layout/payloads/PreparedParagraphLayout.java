package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.layout.PreparedNodeLayout;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;

import java.util.List;

/**
 * Prepared layout payload attached to {@code ParagraphNode} prepared
 * nodes. Carries the wrapped logical and visual lines, line metrics, and
 * cumulative measurements computed during the prepare pass; consumed by
 * the paragraph definition's split + emit paths.
 *
 * @param logicalLines   original paragraph lines before wrapping
 * @param visualLines    wrapped lines that the renderer paints
 * @param lineMetrics    font line metrics for the dominant text style
 * @param baselineOffset baseline offset in points
 * @param lineHeight     resolved line height
 * @param lineGap        extra spacing between wrapped lines
 * @param maxLineWidth   widest measured line width
 * @param totalHeight    cumulative paragraph height
 * @param emitBookmark   whether the paragraph should emit a bookmark
 */
public record PreparedParagraphLayout(
        List<String> logicalLines,
        List<ParagraphLine> visualLines,
        TextMeasurementSystem.LineMetrics lineMetrics,
        double baselineOffset,
        double lineHeight,
        double lineGap,
        double maxLineWidth,
        double totalHeight,
        boolean emitBookmark
) implements PreparedNodeLayout {
}
