package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.font_library.FontLibrary;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfFont;
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
        PdfFont font = fonts.getFont(payload.textStyle().fontName(), PdfFont.class).orElseThrow();
        double innerX = fragment.x() + payload.padding().left();
        double innerWidth = Math.max(0.0, fragment.width() - payload.padding().horizontal());
        double contentTop = fragment.y() + fragment.height() - payload.padding().top();
        PDPageContentStream stream = environment.pageSurface(fragment.pageIndex());

        stream.saveGraphicsState();
        try {
            stream.setFont(font.fontType(payload.textStyle().decoration()), (float) payload.textStyle().size());
            stream.setNonStrokingColor(payload.textStyle().color());

            for (int lineIndex = 0; lineIndex < payload.lines().size(); lineIndex++) {
                BuiltInNodeDefinitions.ParagraphLine line = payload.lines().get(lineIndex);
                String text = sanitize(line.text());
                if (text.isEmpty()) {
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
                stream.showText(text);
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
