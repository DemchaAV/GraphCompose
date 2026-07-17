package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.backend.fixed.pptx.PptxFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.PptxRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.*;
import com.demcha.compose.document.node.ExternalLinkTarget;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.InlineBackground;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.font.FontLibrary;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.sl.usermodel.TextShape.TextAutofit;
import org.apache.poi.sl.usermodel.VerticalAlignment;
import org.apache.poi.xslf.usermodel.*;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;

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

    private static final double FRAME_EPSILON = 0.1;

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
            double lineX = switch (payload.align()) {
                case RIGHT -> innerX + innerWidth - line.width();
                case CENTER -> innerX + (innerWidth - line.width()) / 2.0;
                case LEFT -> innerX;
            };
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
        XSLFSlide slide = environment.slide(pageIndex);
        // PowerPoint seats one shared baseline per paragraph using its largest
        // run, so one shared frame is only safe while every plain run agrees on
        // font size; chips, inline graphics, and mixed sizes go through
        // per-span absolute frames whose tops already encode their own ascent.
        boolean usesAbsoluteSpans = mixesFontSizes(line)
                || line.spans().stream().anyMatch(span ->
                !(span instanceof ParagraphTextSpan textSpan) || textSpan.background() != null);
        XSLFTextParagraph paragraph = null;
        if (!usesAbsoluteSpans) {
            double top = environment.canvasHeight() - baselineY
                    - sharedBoxViewerAscent(line, environment);
            XSLFTextBox lineBox = newTextBox(slide,
                    new Rectangle2D.Double(lineX, top,
                            Math.max(FRAME_EPSILON, line.width() + FRAME_EPSILON),
                            Math.max(FRAME_EPSILON, line.textLineHeight())));
            setShapeName(lineBox, "GraphCompose Text Line");
            paragraph = lineBox.getTextParagraphs().get(0);
            removePlaceholderRuns(paragraph);
            paragraph.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.LEFT);
            paragraph.setLineSpacing(100.0);
            paragraph.setSpaceBefore(0.0);
            paragraph.setSpaceAfter(0.0);
        }

        double cursorX = lineX;
        for (ParagraphSpan span : line.spans()) {
            if (span instanceof ParagraphTextSpan textSpan) {
                String text = sanitized(fonts, textSpan);
                if (textSpan.background() == null) {
                    if (paragraph == null) {
                        renderTextSpan(slide, textSpan, text, cursorX, baselineY, fonts,
                                environment.canvasHeight(), environment);
                    } else {
                        addRun(paragraph, text, textSpan.textStyle(), environment);
                    }
                } else {
                    renderChip(slide, textSpan, text, cursorX, baselineY, line, fonts, environment);
                }
            } else if (span instanceof ParagraphImageSpan imageSpan) {
                renderImage(slide, imageSpan, cursorX, baselineY, line, environment);
            } else if (span instanceof ParagraphShapeSpan shapeSpan) {
                renderShape(slide, shapeSpan, cursorX, baselineY, line, environment.canvasHeight());
            } else if (span instanceof ParagraphSvgSpan svgSpan) {
                renderSvg(slide, svgSpan, cursorX, baselineY, line, environment);
            }
            renderExternalLinkOverlay(slide, spanLinkTarget(span), cursorX, span.width(),
                    lineTop, line, environment);
            cursorX += span.width();
        }
        // Paragraph-level links get one hotspot per line, tight to the measured
        // line box — the same per-line emission the PDF backend uses.
        renderExternalLinkOverlay(slide, payload.linkTarget(), lineX, line.width(),
                lineTop, line, environment);
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

    private static XSLFTextBox newTextBox(XSLFSlide slide, Rectangle2D anchor) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(anchor);
        box.setWordWrap(false);
        box.setTextAutofit(TextAutofit.NONE);
        box.setVerticalAlignment(VerticalAlignment.TOP);
        box.setTopInset(0);
        box.setRightInset(0);
        box.setBottomInset(0);
        box.setLeftInset(0);
        return box;
    }

    private static void addRun(XSLFTextParagraph paragraph,
                               String text,
                               TextStyle style,
                               PptxRenderEnvironment environment) {
        if (text.isEmpty()) {
            return;
        }
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(text);
        applyStyle(run, style, environment);
    }

    private static void applyStyle(XSLFTextRun run,
                                   TextStyle style,
                                   PptxRenderEnvironment environment) {
        run.setFontFamily(environment.fontFamily(style.fontName()));
        run.setFontSize(style.size());
        run.setFontColor(style.color() == null ? Color.BLACK : style.color());
        run.setBold(PptxFontMapping.isBold(style));
        run.setItalic(PptxFontMapping.isItalic(style));
        run.setUnderlined(PptxFontMapping.isUnderline(style));
        run.setStrikethrough(PptxFontMapping.isStrikethrough(style));
    }

    private static void renderTextSpan(XSLFSlide slide,
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
        XSLFTextBox textBox = newTextBox(slide, new Rectangle2D.Double(
                cursorX, top, Math.max(FRAME_EPSILON, span.width() + FRAME_EPSILON),
                Math.max(FRAME_EPSILON, metrics.lineHeight())));
        setShapeName(textBox, "GraphCompose Inline Text Span");
        XSLFTextParagraph paragraph = textBox.getTextParagraphs().get(0);
        removePlaceholderRuns(paragraph);
        paragraph.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.LEFT);
        addRun(paragraph, text, span.textStyle(), environment);
    }

    private static void renderChip(XSLFSlide slide,
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
        XSLFAutoShape chip = slide.createAutoShape();
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
        double textWidth = Math.max(FRAME_EPSILON, span.width() - padding.horizontal());
        XSLFTextBox textBox = newTextBox(slide, new Rectangle2D.Double(
                cursorX + padding.left(), textTop, textWidth,
                Math.max(FRAME_EPSILON, metrics.lineHeight())));
        setShapeName(textBox, "GraphCompose Inline Chip Text");
        XSLFTextParagraph paragraph = textBox.getTextParagraphs().get(0);
        removePlaceholderRuns(paragraph);
        paragraph.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.LEFT);
        addRun(paragraph, text, span.textStyle(), environment);
    }

    private static void renderExternalLinkOverlay(XSLFSlide slide,
                                                  com.demcha.compose.document.node.DocumentLinkTarget target,
                                                  double x,
                                                  double width,
                                                  double lineTop,
                                                  ParagraphLine line,
                                                  PptxRenderEnvironment environment) {
        if (!(target instanceof ExternalLinkTarget external) || width <= 0) {
            return;
        }
        XSLFAutoShape hotspot = slide.createAutoShape();
        hotspot.setShapeType(ShapeType.RECT);
        hotspot.setAnchor(new Rectangle2D.Double(x,
                environment.canvasHeight() - lineTop, width, line.lineHeight()));
        // A fully transparent solid fill keeps the whole rectangle clickable
        // in slide-show mode. noFill shapes only hit-test their outline.
        hotspot.setFillColor(new Color(0, 0, 0, 0));
        hotspot.setLineColor(null);
        setShapeName(hotspot, "GraphCompose Text Link Hotspot");
        hotspot.createHyperlink().linkToUrl(external.options().uri());
    }

    private static void removePlaceholderRuns(XSLFTextParagraph paragraph) {
        for (XSLFTextRun run : java.util.List.copyOf(paragraph.getTextRuns())) {
            if (run.getRawText() == null || run.getRawText().isEmpty()) {
                paragraph.removeTextRun(run);
            }
        }
    }

    private static void setShapeName(XSLFSimpleShape shape, String name) {
        ((CTShape) shape.getXmlObject()).getNvSpPr().getCNvPr().setName(name);
    }

    private static void renderImage(XSLFSlide slide,
                                    ParagraphImageSpan span,
                                    double cursorX,
                                    double baselineY,
                                    ParagraphLine line,
                                    PptxRenderEnvironment environment) {
        double bottom = inlineBottom(span.height(), span.alignment(), span.baselineOffset(), baselineY, line);
        XSLFPictureShape picture = slide.createPicture(environment.resolvePicture(span.imageData()));
        picture.setAnchor(new Rectangle2D.Double(cursorX,
                environment.canvasHeight() - bottom - span.height(), span.width(), span.height()));
    }

    private static void renderShape(XSLFSlide slide,
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
            PptxInlineGeometry.drawOutline(slide, layer.outline(), box, layer.fillColor(), layer.stroke());
        }
    }

    private static void renderSvg(XSLFSlide slide,
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
            XSLFPictureShape picture = slide.createPicture(
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
            PptxInlineGeometry.drawPath(slide,
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
        return font.sanitizeForRender(span.textStyle(), span.text());
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
