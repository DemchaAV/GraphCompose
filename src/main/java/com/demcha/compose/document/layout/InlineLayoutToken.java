package com.demcha.compose.document.layout;

import com.demcha.compose.document.layout.payloads.*;
import com.demcha.compose.document.node.*;
import com.demcha.compose.document.style.DocumentDashPattern;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.InlineBackground;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.engine.components.content.ImageData;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.demcha.compose.document.layout.DocumentNodeAdapters.toImageData;
import static com.demcha.compose.document.layout.DocumentNodeAdapters.toStroke;

/**
 * The inline layout tokens produced while wrapping a paragraph — one per glyph
 * run, image, shape, or SVG placed on a line. Pulled out of {@code TextFlowSupport}
 * so the token model is a focused, self-contained unit; the wrapping loops that
 * build and consume these live alongside it in the same package.
 *
 * @author Artem Demchyshyn
 */
sealed interface InlineLayoutToken
        permits InlineTextToken, InlineImageToken, InlineShapeToken, InlineSvgToken {
    double width();

    /**
     * Width used for line-wrap accounting. Equals {@link #width()} except for
     * a highlight chip token, which reserves its outer horizontal padding here
     * so wrapping accounts for the chip's full advance.
     */
    default double wrapWidth() {
        return width();
    }
}

record InlineTextToken(
        String text,
        TextStyle textStyle,
        DocumentLinkTarget linkTarget,
        double width,
        InlineBackground background,
        Object highlightGroup,
        double leadPad,
        double trailPad
) implements InlineLayoutToken {
    InlineTextToken {
        text = text == null ? "" : text;
        textStyle = textStyle == null ? TextStyle.DEFAULT_STYLE : textStyle;
    }

    @Override
    public double wrapWidth() {
        return width + leadPad + trailPad;
    }

    static InlineTextToken of(String text,
                              TextStyle style,
                              DocumentLinkTarget linkTarget,
                              TextMeasurementSystem measurement) {
        String safeText = text == null ? "" : text;
        TextStyle safeStyle = style == null ? TextStyle.DEFAULT_STYLE : style;
        double width = safeText.isEmpty() ? 0.0 : measurement.textWidth(safeStyle, safeText);
        return new InlineTextToken(safeText, safeStyle, linkTarget, width, null, null, 0.0, 0.0);
    }

    static InlineTextToken ofHighlight(String text,
                                       TextStyle style,
                                       DocumentLinkTarget linkTarget,
                                       InlineBackground background,
                                       Object highlightGroup,
                                       double leadPad,
                                       double trailPad,
                                       TextMeasurementSystem measurement) {
        String safeText = text == null ? "" : text;
        TextStyle safeStyle = style == null ? TextStyle.DEFAULT_STYLE : style;
        double width = safeText.isEmpty() ? 0.0 : measurement.textWidth(safeStyle, safeText);
        return new InlineTextToken(safeText, safeStyle, linkTarget, width,
                background, highlightGroup, leadPad, trailPad);
    }
}

record InlineImageToken(
        ImageData imageData,
        double width,
        double height,
        InlineImageAlignment alignment,
        double baselineOffset,
        DocumentLinkTarget linkTarget
) implements InlineLayoutToken {
    InlineImageToken {
        Objects.requireNonNull(imageData, "imageData");
        alignment = alignment == null ? InlineImageAlignment.CENTER : alignment;
    }

    static InlineImageToken of(InlineImageRun run) {
        return new InlineImageToken(
                toImageData(run.imageData()),
                run.width(),
                run.height(),
                run.alignment(),
                run.baselineOffset(),
                run.linkTarget());
    }
}

record InlineShapeToken(
        List<ResolvedShapeLayer> layers,
        double width,
        double height,
        InlineImageAlignment alignment,
        double baselineOffset,
        DocumentLinkTarget linkTarget
) implements InlineLayoutToken {
    InlineShapeToken {
        alignment = alignment == null ? InlineImageAlignment.CENTER : alignment;
    }

    static InlineShapeToken of(InlineShapeRun run) {
        List<ResolvedShapeLayer> resolved = new ArrayList<>(run.layers().size());
        for (ShapeLayer layer : run.layers()) {
            resolved.add(new ResolvedShapeLayer(
                    layer.outline(),
                    layer.fill() == null ? null : layer.fill().color(),
                    toStroke(layer.stroke())));
        }
        return new InlineShapeToken(
                List.copyOf(resolved),
                run.width(),
                run.height(),
                run.alignment(),
                run.baselineOffset(),
                run.linkTarget());
    }
}

record InlineSvgToken(
        List<ResolvedSvgLayer> layers,
        double width,
        double height,
        InlineImageAlignment alignment,
        double baselineOffset,
        DocumentLinkTarget linkTarget
) implements InlineLayoutToken {
    InlineSvgToken {
        alignment = alignment == null ? InlineImageAlignment.CENTER : alignment;
    }

    static InlineSvgToken of(InlineSvgRun run) {
        // Lower each SVG layer to an engine-ready span. Geometry (and the clip
        // region) stay normalized to the unit box and scale at render; the
        // stroke width and dash lengths are in SVG user units, so scale them to
        // points here (scale = target width / source frame width) — the same
        // arithmetic SvgIcon.node(double) does, but carrying the clip through.
        SvgIcon icon = run.icon();
        double scale = run.width() / icon.sourceWidth();
        List<ResolvedSvgLayer> resolved = new ArrayList<>(icon.layers().size());
        for (SvgIcon.Layer layer : icon.layers()) {
            resolved.add(toResolvedSvgLayer(layer, scale));
        }
        return new InlineSvgToken(
                List.copyOf(resolved),
                run.width(),
                run.height(),
                run.alignment(),
                run.baselineOffset(),
                run.linkTarget());
    }

    /**
     * Lowers an {@link SvgIcon.Layer} to an engine-ready {@link ResolvedSvgLayer}.
     * Mirrors the paint normalization in {@code PathDefinition} (solid paints
     * collapse to flat colours; only true gradients travel as
     * {@link DocumentPaint}), scales the stroke/dash to points by {@code scale},
     * and carries the optional clip region.
     */
    static ResolvedSvgLayer toResolvedSvgLayer(SvgIcon.Layer layer, double scale) {
        Color fill;
        DocumentPaint fillGradient = null;
        if (layer.fillPaint() instanceof DocumentPaint.Solid solid) {
            fill = solid.color().color();
        } else if (layer.fillPaint() != null) {
            fillGradient = layer.fillPaint();
            fill = null;
        } else {
            fill = layer.fill() == null ? null : layer.fill().color();
        }
        DocumentStroke stroke = layer.stroke() == null ? null
                : DocumentStroke.of(layer.stroke().color(), layer.stroke().width() * scale);
        DocumentPaint strokeGradient = null;
        if (layer.strokePaint() instanceof DocumentPaint.Solid solid && stroke != null) {
            stroke = DocumentStroke.of(solid.color(), stroke.width());
        } else if (layer.strokePaint() != null && !(layer.strokePaint() instanceof DocumentPaint.Solid)) {
            strokeGradient = layer.strokePaint();
        }
        DocumentDashPattern dash = null;
        if (!layer.dashArray().isEmpty()) {
            double[] scaled = new double[layer.dashArray().size()];
            for (int i = 0; i < scaled.length; i++) {
                scaled[i] = layer.dashArray().get(i) * scale;
            }
            dash = DocumentDashPattern.of(scaled);
        }
        return new ResolvedSvgLayer(
                layer.geometry().segments(),
                fill,
                fillGradient,
                toStroke(stroke),
                strokeGradient,
                dash,
                layer.lineCap(),
                layer.lineJoin(),
                layer.clip() == null ? null : layer.clip().segments());
    }
}
