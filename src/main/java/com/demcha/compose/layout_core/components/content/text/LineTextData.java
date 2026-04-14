package com.demcha.compose.layout_core.components.content.text;

import com.demcha.compose.layout_core.system.interfaces.Font;
import com.demcha.compose.layout_core.system.interfaces.TextMeasurementSystem;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable line payload plus per-pass placement for one block-text line.
 *
 * <p>The text bodies and cached measurement data are resolved against the active
 * document-level {@link TextMeasurementSystem} during block-text build and then
 * carried forward unchanged through layout and page-break copies. Only
 * placement coordinates are expected to vary between passes.</p>
 *
 * <p>This keeps the line payload backend-neutral at the engine level while still
 * making repeated layout passes stable. A different backend/composer should
 * materialize its own entity tree and therefore its own line caches.</p>
 *
 * <p>The {@code @Data} annotation from Lombok automatically generates getters
 * for all fields, setters for non-final fields, a constructor for final fields,
 * {@code equals()}, {@code hashCode()}, and {@code toString()} methods.</p>
 */
@Data
@Accessors(fluent = true)

public final class LineTextData {
    /**
     * The actual text content of the line. This field is final and immutable.
     */
    private final List<TextDataBody> bodies = new ArrayList<>();
    private final int page;
    private final double lineWidth;
    private final TextMeasurementSystem.LineMetrics lineMetrics;
    private final double baselineOffset;
    private double x;
    private double y;



    private LineTextData(int page) {
        this(page, Double.NaN, null, Double.NaN);
    }

    private LineTextData(int page,
                         double lineWidth,
                         TextMeasurementSystem.LineMetrics lineMetrics,
                         double baselineOffset) {
        this.page = page;
        this.lineWidth = lineWidth;
        this.lineMetrics = lineMetrics;
        this.baselineOffset = baselineOffset;
    }

    public LineTextData(List<TextDataBody> bodies, int page) {
        this(bodies, page, Double.NaN, null, Double.NaN);
    }

    public LineTextData(List<TextDataBody> bodies,
                        int page,
                        double lineWidth,
                        TextMeasurementSystem.LineMetrics lineMetrics,
                        double baselineOffset) {
        this(page, lineWidth, lineMetrics, baselineOffset);
        this.bodies.addAll(bodies);
    }

    public LineTextData(LineTextData ltd, double x, double y, int page) {
        this(ltd.bodies(), page, ltd.lineWidth(), ltd.lineMetrics(), ltd.baselineOffset());
        this.x = x;
        this.y = y;
    }

    public static LineTextData createWithoutMarkdown(String text, TextStyle style, int page) {
        var ltd = new LineTextData(page);
        ltd.bodies.add(new TextDataBody(text, style));
        return ltd;
    }

    /**
     * Returns whether this line already carries a builder-time cached width.
     */
    public boolean hasCachedLineWidth() {
        return !Double.isNaN(lineWidth);
    }

    /**
     * Returns whether this line already carries a resolved mixed-style line metrics payload.
     */
    public boolean hasCachedLineMetrics() {
        return lineMetrics != null;
    }

    /**
     * Returns whether this line already carries a cached baseline offset from the line bottom.
     */
    public boolean hasCachedBaselineOffset() {
        return !Double.isNaN(baselineOffset);
    }


    public <T extends Font<?>> double width(TextDataBody textDataBody, T font) {
        return  font.getTextWidth(textDataBody.textStyle(),textDataBody.text());
    }

    public <T extends Font<?>> double  width(T font) {
        return bodies.stream()
                .mapToDouble((textDataBody) -> width(textDataBody, font))
                .sum();
    }

    /**
     * Returns the cached width when available and otherwise measures the line on demand.
     *
     * <p>The fallback exists for manually constructed lines that bypass the normal
     * {@code BlockTextBuilder} path.</p>
     */
    public double width(TextMeasurementSystem measurementSystem, TextStyle fallbackStyle) {
        if (hasCachedLineWidth()) {
            return lineWidth;
        }
        return bodies.stream()
                .mapToDouble(body -> {
                    TextStyle style = body.textStyle() == null ? fallbackStyle : body.textStyle();
                    return measurementSystem.textWidth(style, body.text());
                })
                .sum();
    }


}
