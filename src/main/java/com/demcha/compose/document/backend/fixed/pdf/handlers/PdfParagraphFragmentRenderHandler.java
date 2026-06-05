package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphShapeSpan;
import com.demcha.compose.document.layout.payloads.ResolvedShapeLayer;
import com.demcha.compose.document.layout.payloads.ParagraphImageSpan;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.layout.payloads.ParagraphSpan;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.engine.render.pdf.PdfFont;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
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
            double cursorTop = contentTop;
            for (int lineIndex = 0; lineIndex < payload.lines().size(); lineIndex++) {
                ParagraphLine line = payload.lines().get(lineIndex);
                double lineTop = cursorTop;
                double resolvedLineHeight = line.lineHeight();
                double baselineY = lineTop - resolvedLineHeight + line.baselineOffsetFromBottom();
                if (payload.verticalAlign() != TextVerticalAlign.DEFAULT) {
                    baselineY += verticalSeatShift(line, fonts, payload.verticalAlign());
                }
                double lineX = switch (payload.align()) {
                    case RIGHT -> innerX + innerWidth - line.width();
                    case CENTER -> innerX + (innerWidth - line.width()) / 2.0;
                    case LEFT -> innerX;
                };

                renderLine(stream, fonts, line, lineX, baselineY, environment);

                cursorTop = lineTop - resolvedLineHeight - payload.lineGap();
            }
        } finally {
            stream.restoreGraphicsState();
        }
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

    private void renderLine(PDPageContentStream stream,
                            FontLibrary fonts,
                            ParagraphLine line,
                            double lineX,
                            double baselineY,
                            PdfRenderEnvironment environment) throws IOException {
        List<ParagraphSpan> spans = line.spans();
        if (spans.isEmpty()) {
            return;
        }

        boolean inTextBlock = false;
        double cursorX = lineX;
        try {
            for (ParagraphSpan span : spans) {
                if (span instanceof ParagraphTextSpan textSpan) {
                    PdfFont font = fonts.getFont(textSpan.textStyle().fontName(), PdfFont.class).orElseThrow();
                    // Font-aware sanitization keeps width measurement
                    // (PdfFont.getTextWidth) and the bytes emitted here
                    // in lockstep. PdfFont.sanitizeForRender substitutes
                    // any code point the resolved font cannot encode
                    // with '?', preventing PDFBox from throwing on
                    // arrows / bullets / emoji / unsupported unicode.
                    String text = font.sanitizeForRender(textSpan.textStyle(), textSpan.text());
                    if (text.isEmpty()) {
                        cursorX += textSpan.width();
                        continue;
                    }
                    if (!inTextBlock) {
                        stream.beginText();
                        stream.newLineAtOffset((float) cursorX, (float) baselineY);
                        inTextBlock = true;
                    }
                    stream.setFont(font.fontType(textSpan.textStyle().decoration()), (float) textSpan.textStyle().size());
                    stream.setNonStrokingColor(textSpan.textStyle().color());
                    stream.showText(text);
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
                    PDImageXObject image = environment.resolveImage(imageSpan.imageData());
                    stream.drawImage(image,
                            (float) cursorX,
                            (float) imageBottom,
                            (float) imageSpan.width(),
                            (float) imageSpan.height());
                    cursorX += imageSpan.width();
                } else if (span instanceof ParagraphShapeSpan shapeSpan) {
                    if (inTextBlock) {
                        stream.endText();
                        inTextBlock = false;
                    }
                    renderShape(stream, shapeSpan, cursorX, baselineY,
                            line.textAscent(), line.baselineOffsetFromBottom(), line.lineHeight());
                    cursorX += shapeSpan.width();
                }
            }
        } finally {
            if (inTextBlock) {
                stream.endText();
            }
        }
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

    private static void renderShape(PDPageContentStream stream,
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
            PdfShapeGeometry.fillAndStrokePath(stream, layer.fillColor(), layer.stroke(), s -> {
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
                } else {
                    throw new IllegalStateException("Unknown inline outline: " + outline);
                }
            });
        }
    }

}
