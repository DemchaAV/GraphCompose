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

    /**
     * A face this reader cannot read, named so the caller can report it.
     *
     * <p>An {@link IOException}, like the refusals {@link PptxEmbeddedFont#wrap}
     * raises, because it means the same thing to the caller: a face whose program
     * could not be read, which costs the deck the font rather than the render.</p>
     */
    static final class UnreadableFaceException extends IOException {

        private static final long serialVersionUID = 1L;

        private final String source;

        private UnreadableFaceException(String family, String source, Throwable cause) {
            super("Unable to read PPTX font metrics for " + family + " from " + source, cause);
            this.source = source;
        }

        /** The face's source, as it describes itself. */
        String source() {
            return source;
        }
    }

    /**
     * Reads the viewer ascent of every face of a family.
     *
     * <p>An independent read of the same font programs the embedder takes, through
     * {@link Font#createFont} rather than fontbox, and the two do not accept the same
     * files: fontbox's header parse skips a table whose directory entry runs past the
     * end of the file and reads on, while {@code createFont} rejects the font outright
     * for such an entry. A font truncated below its last table used to embed on the
     * first read and then have no readable metrics, which cost the whole deck. So this
     * throws a checked refusal the caller degrades the family on, and the caller reads
     * the metrics <em>before</em> handing any program to the presentation.</p>
     *
     * @throws UnreadableFaceException naming the first face that cannot be read
     */
    static ViewerFontMetrics load(FontFamilyDefinition family,
                                  FontFamilyDefinition.FontSourceSet sources)
            throws UnreadableFaceException {
        return new ViewerFontMetrics(
                ascentRatio(family, sources.regular()),
                ascentRatio(family, sources.bold()),
                ascentRatio(family, sources.italic()),
                ascentRatio(family, sources.boldItalic()));
    }

    private static double ascentRatio(FontFamilyDefinition family,
                                      FontFamilyDefinition.FontBinarySource source)
            throws UnreadableFaceException {
        // Named inside the same try, so that asking the source what it is cannot throw out
        // of the failure path itself.
        String description = "<unnamed>";
        try {
            description = source.description();
            try (InputStream stream = source.openStream()) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, stream)
                        .deriveFont(FONT_METRICS_SIZE);
                return font.getLineMetrics("Hg", FONT_RENDER_CONTEXT).getAscent()
                        / FONT_METRICS_SIZE;
            }
        } catch (IOException | FontFormatException | RuntimeException exception) {
            // Unchecked as well as checked, deliberately and without a case of its own:
            // a refusal from this reader is to cost the deck the font, and the one thing it
            // must not cost is the export. The only refusal seen from it is
            // FontFormatException — 1500 damaged copies of a bundled face raised nothing
            // else — so no test reaches the other arms; they are here because the failure
            // this catch is closing is "the export is lost", not "the font is unusual".
            throw new UnreadableFaceException(family.name().name(), description, exception);
        }
    }
}
