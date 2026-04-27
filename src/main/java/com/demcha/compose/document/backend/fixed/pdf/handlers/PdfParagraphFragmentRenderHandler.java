package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.engine.text.TextControlSanitizer;
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
        implements PdfFragmentRenderHandler<BuiltInNodeDefinitions.ParagraphFragmentPayload> {

    /**
     * Creates the paragraph fragment renderer.
     */
    public PdfParagraphFragmentRenderHandler() {
    }

    @Override
    public Class<BuiltInNodeDefinitions.ParagraphFragmentPayload> payloadType() {
        return BuiltInNodeDefinitions.ParagraphFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       BuiltInNodeDefinitions.ParagraphFragmentPayload payload,
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
                BuiltInNodeDefinitions.ParagraphLine line = payload.lines().get(lineIndex);
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
                            BuiltInNodeDefinitions.ParagraphLine line,
                            double lineX,
                            double baselineY,
                            PdfRenderEnvironment environment) throws IOException {
        List<BuiltInNodeDefinitions.ParagraphSpan> spans = line.spans();
        if (spans.isEmpty()) {
            return;
        }

        boolean inTextBlock = false;
        double cursorX = lineX;
        try {
            for (BuiltInNodeDefinitions.ParagraphSpan span : spans) {
                if (span instanceof BuiltInNodeDefinitions.ParagraphTextSpan textSpan) {
                    String text = sanitize(textSpan.text());
                    if (text.isEmpty()) {
                        cursorX += textSpan.width();
                        continue;
                    }
                    if (!inTextBlock) {
                        stream.beginText();
                        stream.newLineAtOffset((float) cursorX, (float) baselineY);
                        inTextBlock = true;
                    }
                    PdfFont font = fonts.getFont(textSpan.textStyle().fontName(), PdfFont.class).orElseThrow();
                    stream.setFont(font.fontType(textSpan.textStyle().decoration()), (float) textSpan.textStyle().size());
                    stream.setNonStrokingColor(textSpan.textStyle().color());
                    stream.showText(text);
                    cursorX += textSpan.width();
                } else if (span instanceof BuiltInNodeDefinitions.ParagraphImageSpan imageSpan) {
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
                }
            }
        } finally {
            if (inTextBlock) {
                stream.endText();
            }
        }
    }

    private static double resolveImageBottom(BuiltInNodeDefinitions.ParagraphImageSpan imageSpan,
                                             double baselineY,
                                             double textAscent,
                                             double baselineOffsetFromBottom,
                                             double lineHeight) {
        double imageHeight = imageSpan.height();
        double lineBottom = baselineY - baselineOffsetFromBottom;
        double base = switch (imageSpan.alignment() == null ? InlineImageAlignment.CENTER : imageSpan.alignment()) {
            case BASELINE -> baselineY;
            // Visually centers the image inside the resolved line box
            // (lineBottom + lineHeight/2). This matches how readers expect
            // icons next to text to sit, regardless of text ascender height.
            case CENTER -> lineBottom + (lineHeight - imageHeight) / 2.0;
            case TEXT_TOP -> baselineY + textAscent - imageHeight;
            case TEXT_BOTTOM -> lineBottom;
        };
        return base + imageSpan.baselineOffset();
    }

    private String sanitize(String text) {
        return TextControlSanitizer.remove(text);
    }
}
