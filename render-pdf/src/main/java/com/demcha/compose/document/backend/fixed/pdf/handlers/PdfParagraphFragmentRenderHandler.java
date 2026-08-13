package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.*;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.InlineBackground;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.engine.text.bidi.BidiMirroring;
import com.demcha.compose.engine.text.bidi.BidiText;
import com.demcha.compose.engine.text.bidi.BidiVisualOrder;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.font.FontLibrary;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders wrapped paragraph fragments emitted by the semantic layout compiler.
 *
 * <p>Lines may carry both text spans and inline image spans. The handler
 * walks per-line heights, opens a {@code BT/ET} block for runs of text spans
 * and switches to {@code drawImage} for image spans without losing the
 * shared baseline.</p>
 */
public final class PdfParagraphFragmentRenderHandler
        implements PdfFragmentRenderHandler<ParagraphFragmentPayload> {

    /**
     * Creates the paragraph fragment renderer.
     */
    public PdfParagraphFragmentRenderHandler() {
    }

    /**
     * Baseline correction that seats a line by its cap band within the line box,
     * used for the non-default {@link TextVerticalAlign} modes. Derived purely
     * from font metrics — no magic offset — so it scales with font size:
     *
     * <ul>
     *   <li>{@code TOP} — raise the cap top to the line-box top
     *       ({@code ascent + leading - capHeight}).</li>
     *   <li>{@code CENTER} — centre the cap band {@code [baseline, baseline + capHeight]}
     *       on the line-box middle (the midpoint of {@code TOP} and {@code BOTTOM}).</li>
     *   <li>{@code BOTTOM} — lower the baseline to the line-box bottom
     *       ({@code -descent}); descenders extend below the box.</li>
     * </ul>
     *
     * <p>The cap height is read from the line's first text span; an image-only
     * line is left untouched.</p>
     *
     * @return points to add to the baseline Y (positive raises the text)
     */
    private static double verticalSeatShift(ParagraphLine line, FontLibrary fonts, TextVerticalAlign align) {
        for (ParagraphSpan span : line.spans()) {
            if (span instanceof ParagraphTextSpan textSpan) {
                PdfFont font = fonts.getFont(textSpan.textStyle().fontName(), PdfFont.class).orElse(null);
                if (font == null) {
                    return 0.0;
                }
                double capHeight = font.getCapHeight(textSpan.textStyle());
                double ascent = line.textAscent();
                double descent = line.baselineOffsetFromBottom();
                double leading = Math.max(0.0, line.textLineHeight() - ascent - descent);
                double capTopToBoxTop = ascent + leading - capHeight;
                return switch (align) {
                    case TOP -> capTopToBoxTop;
                    case CENTER -> (capTopToBoxTop - descent) / 2.0;
                    case BOTTOM -> -descent;
                    case DEFAULT -> 0.0;
                };
            }
        }
        return 0.0;
    }

    private static double resolveImageBottom(ParagraphImageSpan imageSpan,
                                             double baselineY,
                                             double textAscent,
                                             double baselineOffsetFromBottom,
                                             double lineHeight) {
        return resolveInlineGraphicBottom(
                imageSpan.height(),
                imageSpan.alignment(),
                imageSpan.baselineOffset(),
                baselineY,
                textAscent,
                baselineOffsetFromBottom,
                lineHeight);
    }

    /**
     * Resolves the PDF-space bottom edge of an inline graphic (image or
     * ellipse) for the given vertical alignment. Shared by both span kinds so
     * dots and icons sit identically next to text.
     */
    private static double resolveInlineGraphicBottom(double graphicHeight,
                                                     InlineImageAlignment alignment,
                                                     double baselineOffset,
                                                     double baselineY,
                                                     double textAscent,
                                                     double baselineOffsetFromBottom,
                                                     double lineHeight) {
        double lineBottom = baselineY - baselineOffsetFromBottom;
        double base = switch (alignment == null ? InlineImageAlignment.CENTER : alignment) {
            case BASELINE -> baselineY;
            // Visually centers the graphic inside the resolved line box
            // (lineBottom + lineHeight/2). This matches how readers expect
            // icons or dots next to text to sit, regardless of ascender height.
            case CENTER -> lineBottom + (lineHeight - graphicHeight) / 2.0;
            case TEXT_TOP -> baselineY + textAscent - graphicHeight;
            case TEXT_BOTTOM -> lineBottom;
        };
        return base + baselineOffset;
    }

    /**
     * Draws a highlight "chip": a rounded fill behind the span's glyphs, then the
     * glyphs in their own text block offset right by the chip's left padding. The
     * fill band is the line's text box expanded by vertical padding, so the chip
     * overflows the line box like a browser highlight without enlarging it.
     */
    private static boolean renderChip(PDPageContentStream stream,
                                   PdfRenderEnvironment environment,
                                   FontLibrary fonts,
                                   ParagraphTextSpan span,
                                   double cursorX,
                                   double baselineY,
                                   ParagraphLine line,
                                   TextRenderState textState) throws IOException {
        PdfFont font = fonts.getFont(span.textStyle().fontName(), PdfFont.class).orElseThrow();
        // A chip is one unsplittable span, so it is the one span that can reach this
        // point carrying both directions' levels — the wrapper splits everything else
        // at level boundaries. Reversing it whole inverted its interior: a chip reading
        // "(a > b)" after a Hebrew word drew as "(b < a)", the comparison flipped. The
        // level-aware transform reverses and mirrors only what UAX #9 moves; for a
        // single-level chip it is the same reverse-and-mirror the plain path does.
        // Sanitize-then-resolve, as in renderLine, so a degraded ligature keeps its
        // letter order.
        String sanitizedLogical = font.sanitizeForRender(span.textStyle(), span.text());
        String text = sanitizedLogical;
        String written = null;
        if (span.rightToLeft()) {
            text = BidiVisualOrder.visualize(sanitizedLogical);
            written = PdfActualText.writtenTextOf(span);
            environment.markReorderedText();
        }
        if (text.isEmpty()) {
            return false;                                       // nothing to paint — no glyph-less fill or mark
        }
        InlineBackground background = span.background();
        DocumentInsets pad = background.padding();
        float chipWidth = (float) span.width();                 // glyphs + left + right padding
        float chipBottom = (float) (baselineY - line.baselineOffsetFromBottom() - pad.bottom());
        float chipHeight = (float) (line.textLineHeight() + pad.vertical());
        Color fill = background.fill() == null ? null : background.fill().color();
        if (fill != null && chipWidth > 0 && chipHeight > 0) {
            float radius = (float) Math.min(background.cornerRadius(), Math.min(chipWidth, chipHeight) / 2.0f);
            PdfShapeGeometry.fillAndStrokePath(stream, environment, fill, null, s ->
                    PdfShapeFragmentRenderHandler.drawRoundedRectangle(
                            s, (float) cursorX, chipBottom, chipWidth, chipHeight, radius, radius, radius, radius));
        }
        stream.beginText();
        stream.newLineAtOffset((float) (cursorX + pad.left()), (float) baselineY);
        textState.invalidate();
        textState.applyFont(stream, font.fontType(span.textStyle().decoration()), (float) span.textStyle().size());
        textState.applyColor(stream, span.textStyle().color());
        if (written != null) {
            stream.beginMarkedContent(PdfActualText.tag(), PdfActualText.properties(written));
        }
        stream.showText(text);
        if (written != null) {
            stream.endMarkedContent();
        }
        stream.endText();
        return true;
    }

    private static void renderShape(PDPageContentStream stream,
                                    PdfRenderEnvironment environment,
                                    ParagraphShapeSpan span,
                                    double cursorX,
                                    double baselineY,
                                    double textAscent,
                                    double baselineOffsetFromBottom,
                                    double lineHeight) throws IOException {
        double width = span.width();
        double height = span.height();
        if (width <= 0 || height <= 0) {
            return;
        }
        double bottom = resolveInlineGraphicBottom(
                height,
                span.alignment(),
                span.baselineOffset(),
                baselineY,
                textAscent,
                baselineOffsetFromBottom,
                lineHeight);
        for (ResolvedShapeLayer layer : span.layers()) {
            ShapeOutline outline = layer.outline();
            float lw = (float) outline.width();
            float lh = (float) outline.height();
            // Each layer is centred within the run's bounding box, so a smaller
            // checkmark sits inside its larger checkbox frame.
            float lx = (float) (cursorX + (width - outline.width()) / 2.0);
            float ly = (float) (bottom + (height - outline.height()) / 2.0);
            PdfShapeGeometry.fillAndStrokePath(stream, environment, layer.fillColor(), layer.stroke(), s -> {
                if (outline instanceof ShapeOutline.Ellipse) {
                    PdfEllipseFragmentRenderHandler.drawEllipse(s, lx, ly, lw, lh);
                } else if (outline instanceof ShapeOutline.Rectangle) {
                    s.addRect(lx, ly, lw, lh);
                } else if (outline instanceof ShapeOutline.RoundedRectangle r) {
                    float radius = (float) Math.min(r.cornerRadius(), Math.min(lw, lh) / 2.0f);
                    PdfShapeFragmentRenderHandler.drawRoundedRectangle(s, lx, ly, lw, lh, radius, radius, radius, radius);
                } else if (outline instanceof ShapeOutline.RoundedRectanglePerCorner rp) {
                    float maxRadius = Math.min(lw, lh) / 2.0f;
                    DocumentCornerRadius c = rp.corners();
                    PdfShapeFragmentRenderHandler.drawRoundedRectangle(s, lx, ly, lw, lh,
                            (float) Math.min(c.topLeft(), maxRadius),
                            (float) Math.min(c.topRight(), maxRadius),
                            (float) Math.min(c.bottomRight(), maxRadius),
                            (float) Math.min(c.bottomLeft(), maxRadius));
                } else if (outline instanceof ShapeOutline.Polygon p) {
                    PdfShapeGeometry.addPolygonPath(s, lx, ly, lw, lh, p.points());
                } else if (outline instanceof ShapeOutline.Path path) {
                    PdfShapeGeometry.addPathSegments(s, lx, ly, lw, lh, path.segments());
                } else {
                    throw new IllegalStateException("Unknown inline outline: " + outline);
                }
            });
        }
    }

    /**
     * Draws an inline SVG-icon span: each resolved vector layer is painted on
     * the baseline-seated box through the shared {@link PdfPathPainter}, so the
     * inline glyph matches the block path fragment for fragment (flat colours,
     * gradients, dashes alike).
     */
    private static void renderSvg(PDPageContentStream stream,
                                  ParagraphSvgSpan span,
                                  PdfRenderEnvironment environment,
                                  int pageIndex,
                                  double cursorX,
                                  double baselineY,
                                  double textAscent,
                                  double baselineOffsetFromBottom,
                                  double lineHeight) throws IOException {
        double width = span.width();
        double height = span.height();
        if (width <= 0 || height <= 0) {
            return;
        }
        double bottom = resolveInlineGraphicBottom(
                height,
                span.alignment(),
                span.baselineOffset(),
                baselineY,
                textAscent,
                baselineOffsetFromBottom,
                lineHeight);
        // Clip to the glyph box (the SVG viewBox). Real-world icon art — notably
        // Noto's working files — parks off-canvas geometry outside the viewBox
        // (a browser clips it away); without this it would bleed into adjacent
        // glyphs, e.g. :package: smearing duplicate boxes across its neighbours.
        stream.saveGraphicsState();
        try {
            stream.addRect((float) cursorX, (float) bottom, (float) width, (float) height);
            stream.clip();
            for (ResolvedSvgLayer layer : span.layers()) {
                PdfPathPainter.paintPath(stream, environment, pageIndex,
                        (float) cursorX, (float) bottom, (float) width, (float) height,
                        layer.segments(), layer.fillColor(), layer.fillPaint(),
                        layer.stroke(), layer.strokePaint(),
                        layer.dashPattern(), layer.lineCap(), layer.lineJoin(), layer.clip());
            }
        } finally {
            stream.restoreGraphicsState();
        }
    }

    @Override
    public Class<ParagraphFragmentPayload> payloadType() {
        return ParagraphFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       ParagraphFragmentPayload payload,
                       PdfRenderEnvironment environment) throws IOException {
        FontLibrary fonts = environment.fonts();
        double innerX = fragment.x() + payload.padding().left();
        double innerWidth = Math.max(0.0, fragment.width() - payload.padding().horizontal());
        double contentTop = fragment.y() + fragment.height() - payload.padding().top();
        PDPageContentStream stream = environment.pageSurface(fragment.pageIndex());

        stream.saveGraphicsState();
        try {
            // Font and non-stroking colour persist across BT/ET within this one
            // q...Q block, so track the last-written pair and re-emit Tf/rg only
            // when a span actually changes them — a single-style paragraph then
            // emits one setFont + one setNonStrokingColor instead of one per span.
            TextRenderState textState = new TextRenderState(environment);
            double cursorTop = contentTop;
            for (int lineIndex = 0; lineIndex < payload.lines().size(); lineIndex++) {
                ParagraphLine line = payload.lines().get(lineIndex);
                double lineTop = cursorTop;
                double resolvedLineHeight = line.lineHeight();
                double baselineY = lineTop - resolvedLineHeight + line.baselineOffsetFromBottom();
                if (payload.verticalAlign() != TextVerticalAlign.DEFAULT) {
                    baselineY += verticalSeatShift(line, fonts, payload.verticalAlign());
                }
                double lineX = ParagraphLineGeometry.lineStartX(
                    payload.align(), innerX, innerWidth, line.width());

                renderLine(stream, fonts, line, lineX, baselineY, environment, textState, fragment.pageIndex());

                cursorTop = lineTop - resolvedLineHeight - payload.lineGap();
            }
        } finally {
            stream.restoreGraphicsState();
        }
    }

    private void renderLine(PDPageContentStream stream,
                            FontLibrary fonts,
                            ParagraphLine line,
                            double lineX,
                            double baselineY,
                            PdfRenderEnvironment environment,
                            TextRenderState textState,
                            int pageIndex) throws IOException {
        // Drawn in visual order, which is the source order for every left-to-right
        // line. The cursor advances over this same sequence, so a run of text spans
        // still leaves the pen exactly where the next one starts and the implicit
        // text position stays correct without reopening a text block per span.
        List<ParagraphSpan> spans = line.spansInVisualOrder();
        if (spans.isEmpty()) {
            return;
        }

        boolean inTextBlock = false;
        double cursorX = lineX;
        // Underline/strikethrough marks are path fills, illegal inside a
        // BT/ET block — collect them while the glyphs stream out and paint
        // them after the text. Lazily allocated: most lines carry none.
        List<PdfTextDecorations.Segment> decorations = null;
        try {
            for (ParagraphSpan span : spans) {
                if (span instanceof ParagraphTextSpan textSpan) {
                    if (textSpan.background() != null) {
                        // A chip span paints a rounded fill plus its glyphs in an
                        // isolated text block (offset by the left padding), so it
                        // closes any open run first and is a fragment boundary.
                        if (inTextBlock) {
                            stream.endText();
                            inTextBlock = false;
                        }
                        textState.resetAlpha(stream);
                        boolean chipPainted = renderChip(stream, environment, fonts, textSpan, cursorX, baselineY, line, textState);
                        textState.invalidate();
                        if (chipPainted && PdfTextDecorations.drawsMark(textSpan.textStyle().decoration())) {
                            DocumentInsets pad = textSpan.background().padding();
                            decorations = addDecoration(decorations, new PdfTextDecorations.Segment(
                                    cursorX + pad.left(), baselineY,
                                    textSpan.width() - pad.horizontal(),
                                    textSpan.textStyle().size(), textSpan.textStyle().color(),
                                    textSpan.textStyle().decoration()));
                        }
                        cursorX += textSpan.width();
                        continue;
                    }
                    PdfFont font = fonts.getFont(textSpan.textStyle().fontName(), PdfFont.class).orElseThrow();
                    // Font-aware sanitization keeps width measurement
                    // (PdfFont.getTextWidth) and the bytes emitted here
                    // in lockstep. PdfFont.sanitizeForRender substitutes
                    // any code point the resolved font cannot encode
                    // with '?', preventing PDFBox from throwing on
                    // arrows / bullets / emoji / unsupported unicode.
                    // A right-to-left run is stored in logical order, but showText emits
                    // characters in the order it is given them — so it is reversed here,
                    // by grapheme cluster. The span keeps its logical text for the
                    // semantic backends; the PDF content stream, and therefore plain
                    // text extraction and copy-paste, carries the visual order. Undoing
                    // that would take ActualText marked content — a known trade-off,
                    // recorded in the changelog rather than hidden here.
                    // Mirroring happens here and not in the span, for the same reason
                    // reversal does: the span's logical text is what the semantic
                    // backends read. PowerPoint does NOT apply UAX #9 L4 — measured on a
                    // slide, a bracket closing a right-to-left line kept facing the way
                    // it was typed even with the frame's direction declared — so the PPTX
                    // backend mirrors at its own seam too.
                    // Sanitizing runs BEFORE the reversal: the glyph seam may degrade a
                    // lam-alef ligature into its two letters, and those come out in
                    // logical order — appended into an already-reversed string they
                    // would land swapped on the page. Reversing last keeps every
                    // substitution on the correct side.
                    String sanitizedLogical = font.sanitizeForRender(textSpan.textStyle(),
                            textSpan.rightToLeft()
                                    ? BidiMirroring.mirror(textSpan.text())
                                    : textSpan.text());
                    String text = sanitizedLogical;
                    String written = null;
                    if (textSpan.rightToLeft()) {
                        text = BidiText.reverseForDisplay(sanitizedLogical);
                        // The glyphs go out backwards, because that is what drawing a
                        // right-to-left run means. The section states what they say —
                        // one run, not the line, so every neighbouring left-to-right
                        // run stays ordinary glyphs a reader keeps whole.
                        written = PdfActualText.writtenTextOf(textSpan);
                        environment.markReorderedText();
                    }
                    if (text.isEmpty()) {
                        cursorX += textSpan.width();
                        continue;
                    }
                    if (!inTextBlock) {
                        stream.beginText();
                        stream.newLineAtOffset((float) cursorX, (float) baselineY);
                        inTextBlock = true;
                    }
                    textState.applyFont(stream,
                            font.fontType(textSpan.textStyle().decoration()),
                            (float) textSpan.textStyle().size());
                    textState.applyColor(stream, textSpan.textStyle().color());
                    if (written != null) {
                        stream.beginMarkedContent(PdfActualText.tag(),
                                PdfActualText.properties(written));
                    }
                    stream.showText(text);
                    if (written != null) {
                        stream.endMarkedContent();
                    }
                    if (PdfTextDecorations.drawsMark(textSpan.textStyle().decoration())) {
                        decorations = addDecoration(decorations, new PdfTextDecorations.Segment(
                                cursorX, baselineY, textSpan.width(),
                                textSpan.textStyle().size(), textSpan.textStyle().color(),
                                textSpan.textStyle().decoration()));
                    }
                    cursorX += textSpan.width();
                } else if (span instanceof ParagraphImageSpan imageSpan) {
                    if (inTextBlock) {
                        stream.endText();
                        inTextBlock = false;
                    }
                    double imageBottom = resolveImageBottom(
                            imageSpan,
                            baselineY,
                            line.textAscent(),
                            line.baselineOffsetFromBottom(),
                            line.lineHeight());
                    textState.resetAlpha(stream);
                    PDImageXObject image = environment.resolveImage(imageSpan.imageData());
                    stream.drawImage(image,
                            (float) cursorX,
                            (float) imageBottom,
                            (float) imageSpan.width(),
                            (float) imageSpan.height());
                    // An inline graphic runs its own graphics-state save/restore and
                    // colour ops; drop the tracked font/colour so the next text span
                    // re-emits them rather than trusting persistence across it.
                    textState.invalidate();
                    cursorX += imageSpan.width();
                } else if (span instanceof ParagraphShapeSpan shapeSpan) {
                    if (inTextBlock) {
                        stream.endText();
                        inTextBlock = false;
                    }
                    textState.resetAlpha(stream);
                    renderShape(stream, environment, shapeSpan, cursorX, baselineY,
                            line.textAscent(), line.baselineOffsetFromBottom(), line.lineHeight());
                    textState.invalidate();
                    cursorX += shapeSpan.width();
                } else if (span instanceof ParagraphSvgSpan svgSpan) {
                    if (inTextBlock) {
                        stream.endText();
                        inTextBlock = false;
                    }
                    textState.resetAlpha(stream);
                    renderSvg(stream, svgSpan, environment, pageIndex, cursorX, baselineY,
                            line.textAscent(), line.baselineOffsetFromBottom(), line.lineHeight());
                    textState.invalidate();
                    cursorX += svgSpan.width();
                }
            }
        } finally {
            if (inTextBlock) {
                stream.endText();
            }
        }
        if (decorations != null) {
            // Marks inherit the ambient alpha into their q..Q, so restore
            // opacity first; each mark then applies its own colour's alpha.
            textState.resetAlpha(stream);
            PdfTextDecorations.draw(environment, stream, decorations);
        }
    }

    private static List<PdfTextDecorations.Segment> addDecoration(
            List<PdfTextDecorations.Segment> decorations,
            PdfTextDecorations.Segment segment) {
        List<PdfTextDecorations.Segment> target =
                decorations == null ? new ArrayList<>() : decorations;
        target.add(segment);
        return target;
    }

    /**
     * Tracks the font/size and non-stroking colour last written to the content
     * stream within one paragraph's {@code q...Q} block, so the handler emits a
     * {@code Tf}/{@code rg} operator only when a span actually changes them. The
     * common single-style paragraph then carries one of each instead of one per
     * span. {@link #invalidate()} forces a re-emit after anything that may disturb
     * the persisted text state (inline images, shapes).
     */
    private static final class TextRenderState {
        private final PdfRenderEnvironment environment;
        private PDFont font;
        private float size = Float.NaN;
        private Color color;
        // A fresh q block inherits the page default of fully opaque; every
        // nested draw (chips, inline graphics, decoration marks) runs in its
        // own q..Q, so the alpha WE set is what survives — invalidate() must
        // not reset it.
        private float alpha = 1f;

        TextRenderState(PdfRenderEnvironment environment) {
            this.environment = environment;
        }

        void applyFont(PDPageContentStream stream, PDFont newFont, float newSize) throws IOException {
            if (newFont != font || newSize != size) {
                stream.setFont(newFont, newSize);
                font = newFont;
                size = newSize;
            }
        }

        void applyColor(PDPageContentStream stream, Color newColor) throws IOException {
            if (!newColor.equals(color)) {
                float newAlpha = newColor.getAlpha() / 255f;
                if (newAlpha != alpha) {
                    // setNonStrokingColor drops the alpha channel, so a
                    // translucent run carries it as a graphics-state constant
                    // (the gs operator is legal inside a text object).
                    PdfAlphaSupport.setFillAlpha(environment, stream, newAlpha);
                    alpha = newAlpha;
                }
                stream.setNonStrokingColor(newColor);
                color = newColor;
            }
        }

        /**
         * Restores full opacity before a nested draw. The {@code gs} alpha
         * survives {@code ET}, and the nested q..Q blocks (chips, inline
         * graphics, decoration marks) inherit it — their own alpha helpers
         * no-op for opaque colours, so without this reset an opaque nested
         * draw after a translucent run would render translucent.
         */
        void resetAlpha(PDPageContentStream stream) throws IOException {
            if (alpha != 1f) {
                PdfAlphaSupport.setFillAlpha(environment, stream, 1f);
                alpha = 1f;
                // The next translucent run must re-emit its gs even when its
                // colour is unchanged.
                color = null;
            }
        }

        void invalidate() {
            font = null;
            size = Float.NaN;
            color = null;
        }
    }

}
