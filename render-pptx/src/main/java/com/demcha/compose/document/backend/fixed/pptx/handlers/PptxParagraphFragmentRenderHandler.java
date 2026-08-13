package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.backend.fixed.pptx.PptxFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.PptxRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.*;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.InlineBackground;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.engine.text.bidi.ArabicShaper;
import com.demcha.compose.engine.text.bidi.BidiMirroring;
import com.demcha.compose.font.FontLibrary;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.xslf.usermodel.XSLFShapeContainer;
import org.apache.poi.xslf.usermodel.*;

import java.awt.Color;
import java.awt.geom.Rectangle2D;

/**
 * Renders each pre-wrapped paragraph line into an absolute, non-wrapping text
 * frame and paints inline chips and graphics at their measured span boxes.
 *
 * @since 2.1.0
 */
public final class PptxParagraphFragmentRenderHandler
        implements PptxFragmentRenderHandler<ParagraphFragmentPayload> {

    /** Creates the paragraph renderer. */
    public PptxParagraphFragmentRenderHandler() {
    }

    @Override
    public Class<ParagraphFragmentPayload> payloadType() {
        return ParagraphFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       ParagraphFragmentPayload payload,
                       PptxRenderEnvironment environment) {
        FontLibrary fonts = environment.fonts();
        double innerX = fragment.x() + payload.padding().left();
        double innerWidth = Math.max(0.0, fragment.width() - payload.padding().horizontal());
        double cursorTop = fragment.y() + fragment.height() - payload.padding().top();

        for (ParagraphLine line : payload.lines()) {
            double baselineY = cursorTop - line.lineHeight() + line.baselineOffsetFromBottom();
            if (payload.verticalAlign() != TextVerticalAlign.DEFAULT) {
                baselineY += verticalSeatShift(line, fonts, payload.verticalAlign());
            }
            double lineX = ParagraphLineGeometry.lineStartX(
                    payload.align(), innerX, innerWidth, line.width());
            renderLine(fragment.pageIndex(), line, lineX, baselineY, cursorTop, payload, fonts, environment);
            cursorTop -= line.lineHeight() + payload.lineGap();
        }
    }

    private static void renderLine(int pageIndex,
                                   ParagraphLine line,
                                   double lineX,
                                   double baselineY,
                                   double lineTop,
                                   ParagraphFragmentPayload payload,
                                   FontLibrary fonts,
                                   PptxRenderEnvironment environment) {
        if (line.spans().isEmpty()) {
            return;
        }
        XSLFShapeContainer surface = environment.surface(pageIndex);
        // PowerPoint seats one shared baseline per paragraph using its largest
        // run, so one shared frame is only safe while every plain run agrees on
        // font size; chips, inline graphics, and mixed sizes go through
        // per-span absolute frames whose tops already encode their own ascent.
        // A right-to-left line also goes per-span. The shared frame appends its runs and
        // lets PowerPoint flow them, which puts the words back in reading order and
        // undoes the ordering the layout resolved; an absolute frame per span pins each
        // one where the page put it.
        boolean usesAbsoluteSpans = mixesFontSizes(line)
                || !line.visualOrder().isEmpty()
                || line.spans().stream().anyMatch(span ->
                !(span instanceof ParagraphTextSpan textSpan)
                        || textSpan.background() != null
                        || textSpan.rightToLeft());
        XSLFTextParagraph paragraph = null;
        if (!usesAbsoluteSpans) {
            double top = environment.canvasHeight() - baselineY
                    - sharedBoxViewerAscent(line, environment);
            XSLFTextBox lineBox = PptxTextFrames.newTextBox(surface,
                    new Rectangle2D.Double(lineX, top,
                            Math.max(PptxTextFrames.FRAME_EPSILON, line.width() + PptxTextFrames.FRAME_EPSILON),
                            Math.max(PptxTextFrames.FRAME_EPSILON, line.textLineHeight())));
            PptxTextFrames.setShapeName(lineBox, "GraphCompose Text Line");
            paragraph = PptxTextFrames.preparedParagraph(lineBox);
            paragraph.setLineSpacing(100.0);
            paragraph.setSpaceBefore(0.0);
            paragraph.setSpaceAfter(0.0);
        }

        double cursorX = lineX;
        // Drawn in visual order, matching the PDF backend, so the two describe the same
        // page; for a left-to-right line this is the source order it always was.
        for (ParagraphSpan span : line.spansInVisualOrder()) {
            if (span instanceof ParagraphTextSpan textSpan) {
                String text = sanitized(fonts, textSpan);
                if (textSpan.background() == null) {
                    if (paragraph == null) {
                        renderTextSpan(surface, textSpan, text, cursorX, baselineY, fonts,
                                environment.canvasHeight(), environment);
                    } else {
                        PptxTextFrames.addRun(paragraph, text, textSpan.textStyle(), environment);
                    }
                } else {
                    renderChip(surface, textSpan, text, cursorX, baselineY, line, fonts, environment);
                }
            } else if (span instanceof ParagraphImageSpan imageSpan) {
                renderImage(surface, imageSpan, cursorX, baselineY, line, environment);
            } else if (span instanceof ParagraphShapeSpan shapeSpan) {
                renderShape(surface, shapeSpan, cursorX, baselineY, line, environment.canvasHeight());
            } else if (span instanceof ParagraphSvgSpan svgSpan) {
                renderSvg(surface, svgSpan, cursorX, baselineY, line, environment);
            }
            deferLinkOverlay(pageIndex, spanLinkTarget(span), span, cursorX, span.width(),
                    lineTop, baselineY, line, environment);
            cursorX += span.width();
        }
        // Paragraph-level links get one hotspot per line, tight to the measured
        // line box — the same per-line emission the PDF backend uses.
        deferLinkOverlay(pageIndex, payload.linkTarget(), null, lineX, line.width(),
                lineTop, baselineY, line, environment);
    }

    /**
     * Returns the viewer ascent that will seat the shared frame's first
     * baseline: PowerPoint uses the tallest run, so the frame top compensates
     * with the largest viewer ascent among the line's plain runs.
     */
    private static double sharedBoxViewerAscent(ParagraphLine line,
                                                PptxRenderEnvironment environment) {
        double ascent = 0;
        for (ParagraphSpan span : line.spans()) {
            if (span instanceof ParagraphTextSpan textSpan && textSpan.background() == null) {
                ascent = Math.max(ascent,
                        environment.viewerAscent(textSpan.textStyle(), line.textAscent()));
            }
        }
        return ascent > 0 ? ascent : line.textAscent();
    }

    /**
     * Returns whether the line's plain text runs disagree on font size — the
     * case a single shared PowerPoint frame cannot seat on one baseline.
     */
    private static boolean mixesFontSizes(ParagraphLine line) {
        double size = Double.NaN;
        for (ParagraphSpan span : line.spans()) {
            if (span instanceof ParagraphTextSpan textSpan && textSpan.background() == null) {
                double spanSize = textSpan.textStyle().size();
                if (Double.isNaN(size)) {
                    size = spanSize;
                } else if (spanSize != size) {
                    return true;
                }
            }
        }
        return false;
    }

    private static com.demcha.compose.document.node.DocumentLinkTarget spanLinkTarget(ParagraphSpan span) {
        if (span instanceof ParagraphTextSpan textSpan) {
            return textSpan.linkTarget();
        }
        if (span instanceof ParagraphImageSpan imageSpan) {
            return imageSpan.linkTarget();
        }
        if (span instanceof ParagraphShapeSpan shapeSpan) {
            return shapeSpan.linkTarget();
        }
        if (span instanceof ParagraphSvgSpan svgSpan) {
            return svgSpan.linkTarget();
        }
        return null;
    }

    private static void renderTextSpan(XSLFShapeContainer surface,
                                       ParagraphTextSpan span,
                                       String text,
                                       double cursorX,
                                       double baselineY,
                                       FontLibrary fonts,
                                       double canvasHeight,
                                       PptxRenderEnvironment environment) {
        PdfFont.VerticalMetrics metrics = verticalMetrics(fonts, span);
        double top = canvasHeight - baselineY
                - environment.viewerAscent(span.textStyle(), metrics.ascent());
        XSLFTextBox textBox = PptxTextFrames.newTextBox(surface, new Rectangle2D.Double(
                cursorX, top, Math.max(PptxTextFrames.FRAME_EPSILON, span.width() + PptxTextFrames.FRAME_EPSILON),
                Math.max(PptxTextFrames.FRAME_EPSILON, metrics.lineHeight())));
        PptxTextFrames.setShapeName(textBox, "GraphCompose Inline Text Span");
        PptxTextFrames.addRun(PptxTextFrames.preparedParagraph(textBox, span.rightToLeft()),
                text, span.textStyle(), environment);
    }

    private static void renderChip(XSLFShapeContainer surface,
                                   ParagraphTextSpan span,
                                   String text,
                                   double cursorX,
                                   double baselineY,
                                   ParagraphLine line,
                                   FontLibrary fonts,
                                   PptxRenderEnvironment environment) {
        if (text.isEmpty()) {
            return;
        }
        InlineBackground background = span.background();
        DocumentInsets padding = background.padding();
        double chipBottom = baselineY - line.baselineOffsetFromBottom() - padding.bottom();
        double chipHeight = line.textLineHeight() + padding.vertical();
        double canvasHeight = environment.canvasHeight();
        Rectangle2D chipAnchor = new Rectangle2D.Double(
                cursorX, canvasHeight - chipBottom - chipHeight, span.width(), chipHeight);
        XSLFAutoShape chip = surface.createAutoShape();
        chip.setShapeType(background.cornerRadius() > 0 ? ShapeType.ROUND_RECT : ShapeType.RECT);
        chip.setAnchor(chipAnchor);
        if (background.cornerRadius() > 0) {
            PptxShapeStyle.applyRoundRectAdjust(chip, background.cornerRadius(), span.width(), chipHeight);
        }
        chip.setFillColor(background.fill().color());
        chip.setLineColor(null);

        PdfFont.VerticalMetrics metrics = verticalMetrics(fonts, span);
        double textTop = canvasHeight - baselineY
                - environment.viewerAscent(span.textStyle(), metrics.ascent());
        double textWidth = Math.max(PptxTextFrames.FRAME_EPSILON, span.width() - padding.horizontal());
        XSLFTextBox textBox = PptxTextFrames.newTextBox(surface, new Rectangle2D.Double(
                cursorX + padding.left(), textTop, textWidth,
                Math.max(PptxTextFrames.FRAME_EPSILON, metrics.lineHeight())));
        PptxTextFrames.setShapeName(textBox, "GraphCompose Inline Chip Text");
        PptxTextFrames.addRun(PptxTextFrames.preparedParagraph(textBox, span.rightToLeft()),
                text, span.textStyle(), environment);
    }

    /**
     * Records one clickable span or line rectangle for deferred emission:
     * hotspots land on the slide after every fragment is painted, so they sit
     * above all content (the PDF annotation contract) and internal targets can
     * resolve forward references. Inline-graphic spans get their
     * baseline-aligned box, everything else the full line box — the PDF
     * backend's span-rectangle rules.
     */
    private static void deferLinkOverlay(int pageIndex,
                                         com.demcha.compose.document.node.DocumentLinkTarget target,
                                         ParagraphSpan span,
                                         double x,
                                         double width,
                                         double lineTop,
                                         double baselineY,
                                         ParagraphLine line,
                                         PptxRenderEnvironment environment) {
        if (target == null || width <= 0) {
            return;
        }
        double canvasHeight = environment.canvasHeight();
        InlineImageAlignment alignment = null;
        double graphicHeight = 0;
        double baselineOffset = 0;
        if (span instanceof ParagraphImageSpan imageSpan) {
            alignment = imageSpan.alignment();
            graphicHeight = imageSpan.height();
            baselineOffset = imageSpan.baselineOffset();
        } else if (span instanceof ParagraphShapeSpan shapeSpan) {
            alignment = shapeSpan.alignment();
            graphicHeight = shapeSpan.height();
            baselineOffset = shapeSpan.baselineOffset();
        } else if (span instanceof ParagraphSvgSpan svgSpan) {
            alignment = svgSpan.alignment();
            graphicHeight = svgSpan.height();
            baselineOffset = svgSpan.baselineOffset();
        }
        Rectangle2D.Double rectangle;
        if (graphicHeight > 0) {
            double bottom = inlineBottom(graphicHeight, alignment, baselineOffset, baselineY, line);
            rectangle = new Rectangle2D.Double(
                    x, canvasHeight - bottom - graphicHeight, width, graphicHeight);
        } else {
            rectangle = new Rectangle2D.Double(
                    x, canvasHeight - lineTop, width, line.lineHeight());
        }
        environment.deferLink(pageIndex, rectangle, target);
    }

    private static void renderImage(XSLFShapeContainer surface,
                                    ParagraphImageSpan span,
                                    double cursorX,
                                    double baselineY,
                                    ParagraphLine line,
                                    PptxRenderEnvironment environment) {
        double bottom = inlineBottom(span.height(), span.alignment(), span.baselineOffset(), baselineY, line);
        XSLFPictureShape picture = surface.createPicture(environment.resolvePicture(span.imageData()));
        picture.setAnchor(new Rectangle2D.Double(cursorX,
                environment.canvasHeight() - bottom - span.height(), span.width(), span.height()));
    }

    private static void renderShape(XSLFShapeContainer surface,
                                    ParagraphShapeSpan span,
                                    double cursorX,
                                    double baselineY,
                                    ParagraphLine line,
                                    double canvasHeight) {
        if (span.width() <= 0 || span.height() <= 0) {
            return;
        }
        double bottom = inlineBottom(span.height(), span.alignment(), span.baselineOffset(), baselineY, line);
        for (ResolvedShapeLayer layer : span.layers()) {
            double width = layer.outline().width();
            double height = layer.outline().height();
            Rectangle2D box = new Rectangle2D.Double(
                    cursorX + (span.width() - width) / 2.0,
                    canvasHeight - bottom - (span.height() + height) / 2.0,
                    width, height);
            PptxInlineGeometry.drawOutline(surface, layer.outline(), box, layer.fillColor(), layer.stroke());
        }
    }

    private static void renderSvg(XSLFShapeContainer surface,
                                  ParagraphSvgSpan span,
                                  double cursorX,
                                  double baselineY,
                                  ParagraphLine line,
                                  PptxRenderEnvironment environment) {
        if (span.width() <= 0 || span.height() <= 0) {
            return;
        }
        double bottom = inlineBottom(span.height(), span.alignment(), span.baselineOffset(), baselineY, line);
        Rectangle2D box = new Rectangle2D.Double(cursorX,
                environment.canvasHeight() - bottom - span.height(), span.width(), span.height());
        if (PptxInlineSvgRasterizer.requiresRaster(span)) {
            PptxCapabilityNotes.inlineSvgRasterized();
            if (span.layers().stream().anyMatch(layer ->
                    layer.fillPaint() != null || layer.strokePaint() != null)) {
                PptxCapabilityNotes.gradientApproximated();
            }
            XSLFPictureShape picture = surface.createPicture(
                    environment.resolvePicture(PptxInlineSvgRasterizer.rasterize(span)));
            picture.setAnchor(box);
            return;
        }
        for (ResolvedSvgLayer layer : span.layers()) {
            if (layer.fillPaint() != null || layer.strokePaint() != null) {
                PptxCapabilityNotes.gradientApproximated();
            }
            Color fill = layer.fillPaint() == null
                    ? layer.fillColor() : layer.fillPaint().primaryColor().color();
            com.demcha.compose.engine.components.content.shape.Stroke stroke = layer.stroke();
            if (stroke != null && layer.strokePaint() != null) {
                stroke = new com.demcha.compose.engine.components.content.shape.Stroke(
                        layer.strokePaint().primaryColor().color(), stroke.width());
            }
            PptxInlineGeometry.drawPath(surface,
                    PptxInlineGeometry.path(layer.segments(), box), fill, stroke);
        }
    }

    private static double inlineBottom(double height,
                                       InlineImageAlignment alignment,
                                       double baselineOffset,
                                       double baselineY,
                                       ParagraphLine line) {
        double lineBottom = baselineY - line.baselineOffsetFromBottom();
        double base = switch (alignment == null ? InlineImageAlignment.CENTER : alignment) {
            case BASELINE -> baselineY;
            case CENTER -> lineBottom + (line.lineHeight() - height) / 2.0;
            case TEXT_TOP -> baselineY + line.textAscent() - height;
            case TEXT_BOTTOM -> lineBottom;
        };
        return base + baselineOffset;
    }

    private static String sanitized(FontLibrary fonts, ParagraphTextSpan span) {
        PdfFont font = fonts.getFont(span.textStyle().fontName(), PdfFont.class).orElseThrow();
        // The span carries shaped Arabic because measurement measures what the PDF
        // draws. PowerPoint shapes Arabic itself, so it gets the base letters back —
        // handing it the forms would freeze this shaper's choices into a file a user
        // may search or copy from. Frame widths stay measured on the form advances,
        // an approximation PowerPoint's own shaping may differ from by a hair.
        // The joining controls go back with the letters. They are what the author wrote
        // to say which letters may connect, and PowerPoint's own shaper is exactly the
        // reader they were written for; the render sanitizing drops them, because a PDF
        // has no glyph for a zero-width control, and dropping them here would hand
        // PowerPoint a word it joins straight back up.
        // Paired punctuation is mirrored here rather than left to PowerPoint. Declaring
        // the frame's direction gets a neutral placed on the correct side — the em-dash
        // of a mixed line moved to where it belongs the moment that was written — but
        // PowerPoint does not go on to mirror the character itself, so a bracket closing
        // a right-to-left line kept facing the way it was typed. Measured, on a slide it
        // drew as ")AUTO resolves it (" where the same document as a PDF reads
        // "(AUTO resolves it)". The cost is that a copy out of the slide carries the
        // mirrored bracket rather than the typed one; a line nobody can read is worse.
        // A chip is exempt, because it is the one span whose flag does not describe all
        // of it: a chip is a single rounded fill, so the wrapper cannot split it at a
        // level boundary and hands it its first character's level whole. Its interior may
        // sit at the opposite level, where UAX #9 L4 mirrors nothing. Swapping such a span
        // whole inverts punctuation the algorithm leaves alone — a chip reading
        // "(a > b)" after a Hebrew word would be stored as ")a < b(", the comparison the
        // wrong way round in the only copy of the text the file has.
        String logical = span.rightToLeft() && span.background() == null
                ? BidiMirroring.mirror(span.text())
                : span.text();
        return font.sanitizeForTextExport(span.textStyle(),
                ArabicShaper.toBaseLetters(logical));
    }

    private static PdfFont.VerticalMetrics verticalMetrics(FontLibrary fonts,
                                                           ParagraphTextSpan span) {
        PdfFont font = fonts.getFont(span.textStyle().fontName(), PdfFont.class).orElseThrow();
        return font.verticalMetrics(span.textStyle());
    }

    private static double verticalSeatShift(ParagraphLine line,
                                            FontLibrary fonts,
                                            TextVerticalAlign align) {
        for (ParagraphSpan span : line.spans()) {
            if (span instanceof ParagraphTextSpan textSpan) {
                PdfFont font = fonts.getFont(textSpan.textStyle().fontName(), PdfFont.class).orElse(null);
                if (font == null) {
                    return 0;
                }
                double capHeight = font.getCapHeight(textSpan.textStyle());
                double ascent = line.textAscent();
                double descent = line.baselineOffsetFromBottom();
                double leading = Math.max(0, line.textLineHeight() - ascent - descent);
                double capTopToBoxTop = ascent + leading - capHeight;
                return switch (align) {
                    case TOP -> capTopToBoxTop;
                    case CENTER -> (capTopToBoxTop - descent) / 2.0;
                    case BOTTOM -> -descent;
                    case DEFAULT -> 0;
                };
            }
        }
        return 0;
    }
}
