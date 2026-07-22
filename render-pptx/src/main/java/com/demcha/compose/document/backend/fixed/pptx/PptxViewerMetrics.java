package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.font.FontFamilyDefinition;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.font.FontRenderContext;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads the ascent the viewer's font engine will use for an embedded family,
 * per facet, from the actual font program. PowerPoint anchors a top-aligned
 * text frame's first baseline at {@code frameTop + viewerAscent}, so the
 * backend needs the viewer's ascent — not the PDF descriptor's — to seat
 * frames such that baselines land on the engine-measured coordinates.
 */
final class PptxViewerMetrics {

    private static final FontRenderContext FONT_RENDER_CONTEXT =
            new FontRenderContext(null, true, true);
    private static final float FONT_METRICS_SIZE = 1000.0f;

    private PptxViewerMetrics() {
    }

    record ViewerFontMetrics(double regularAscent,
                             double boldAscent,
                             double italicAscent,
                             double boldItalicAscent) {
        double ascent(boolean bold, boolean italic) {
            if (bold && italic) {
                return boldItalicAscent;
            }
            if (bold) {
                return boldAscent;
            }
            if (italic) {
                return italicAscent;
            }
            return regularAscent;
        }
    }

    /**
     * Resolves the viewer ascent for one style, in points — the single seat
     * formula shared by the render environment and the geometry test mirror,
     * so the two cannot drift line-by-line.
     */
    static double ascentPoints(com.demcha.compose.engine.components.content.text.TextStyle style,
                               com.demcha.compose.engine.render.pdf.PdfFont font,
                               ViewerFontMetrics custom) {
        double pdfRatio = font.verticalMetrics(style).ascent() / style.size();
        double ratio = custom == null
                ? com.demcha.compose.document.backend.fixed.pptx.handlers.PptxFontMapping
                        .viewerAscentRatio(style.fontName(), pdfRatio)
                : custom.ascent(
                        com.demcha.compose.document.backend.fixed.pptx.handlers.PptxFontMapping.isBold(style),
                        com.demcha.compose.document.backend.fixed.pptx.handlers.PptxFontMapping.isItalic(style));
        return ratio * style.size();
    }

    static ViewerFontMetrics load(FontFamilyDefinition family,
                                  FontFamilyDefinition.FontSourceSet sources) {
        return new ViewerFontMetrics(
                ascentRatio(family, sources.regular()),
                ascentRatio(family, sources.bold()),
                ascentRatio(family, sources.italic()),
                ascentRatio(family, sources.boldItalic()));
    }

    private static double ascentRatio(FontFamilyDefinition family,
                                      FontFamilyDefinition.FontBinarySource source) {
        try (InputStream stream = source.openStream()) {
            Font font = Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont(FONT_METRICS_SIZE);
            return font.getLineMetrics("Hg", FONT_RENDER_CONTEXT).getAscent() / FONT_METRICS_SIZE;
        } catch (IOException | FontFormatException exception) {
            throw new IllegalStateException(
                    "Unable to read PPTX font metrics for " + family.name().name()
                            + " from " + source.description(), exception);
        }
    }
}
