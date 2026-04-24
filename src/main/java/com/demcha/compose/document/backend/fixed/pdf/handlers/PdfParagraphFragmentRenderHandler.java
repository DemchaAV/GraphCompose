package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.engine.render.pdf.PdfFont;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

/**
 * Renders wrapped paragraph fragments emitted by the semantic layout compiler.
 */
public final class PdfParagraphFragmentRenderHandler
        implements PdfFragmentRenderHandler<BuiltInNodeDefinitions.ParagraphFragmentPayload> {

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
            for (int lineIndex = 0; lineIndex < payload.lines().size(); lineIndex++) {
                BuiltInNodeDefinitions.ParagraphLine line = payload.lines().get(lineIndex);
                if (sanitize(line.text()).isEmpty()) {
                    continue;
                }

                double lineTop = contentTop - lineIndex * (payload.lineHeight() + payload.lineGap());
                double baselineY = lineTop - payload.lineHeight() + payload.baselineOffset();
                double lineX = switch (payload.align()) {
                    case RIGHT -> innerX + innerWidth - line.width();
                    case CENTER -> innerX + (innerWidth - line.width()) / 2.0;
                    case LEFT -> innerX;
                };

                stream.beginText();
                stream.newLineAtOffset((float) lineX, (float) baselineY);
                for (BuiltInNodeDefinitions.ParagraphSpan span : line.spans()) {
                    String text = sanitize(span.text());
                    if (text.isEmpty()) {
                        continue;
                    }
                    PdfFont font = fonts.getFont(span.textStyle().fontName(), PdfFont.class).orElseThrow();
                    stream.setFont(font.fontType(span.textStyle().decoration()), (float) span.textStyle().size());
                    stream.setNonStrokingColor(span.textStyle().color());
                    stream.showText(text);
                }
                stream.endText();
            }
        } finally {
            stream.restoreGraphicsState();
        }
    }

    private String sanitize(String text) {
        return text == null ? "" : text.replaceAll("\\p{C}", "");
    }
}
