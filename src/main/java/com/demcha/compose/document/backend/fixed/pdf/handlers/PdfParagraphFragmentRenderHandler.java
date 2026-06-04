package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphShapeSpan;
import com.demcha.compose.document.layout.payloads.ParagraphImageSpan;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.layout.payloads.ParagraphSpan;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.document.node.InlineImageAlignment;
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
        float x = (float) cursorX;
        float y = (float) bottom;
        float w = (float) width;
        float h = (float) height;
        ShapeOutline outline = span.outline();
        PdfShapeGeometry.fillAndStrokePath(stream, span.fillColor(), span.stroke(), s -> {
            if (outline instanceof ShapeOutline.Ellipse) {
                PdfEllipseFragmentRenderHandler.drawEllipse(s, x, y, w, h);
            } else if (outline instanceof ShapeOutline.Rectangle) {
                s.addRect(x, y, w, h);
            } else if (outline instanceof ShapeOutline.RoundedRectangle r) {
                float radius = (float) Math.min(r.cornerRadius(), Math.min(w, h) / 2.0f);
                PdfShapeFragmentRenderHandler.drawRoundedRectangle(s, x, y, w, h, radius, radius, radius, radius);
            } else if (outline instanceof ShapeOutline.Polygon p) {
                PdfShapeGeometry.addPolygonPath(s, x, y, w, h, p.points());
            }
        });
    }

}
