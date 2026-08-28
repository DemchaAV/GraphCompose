package com.demcha.compose.engine.render.pdf.helpers;

import com.demcha.compose.engine.components.content.header_footer.HeaderFooterConfig;
import com.demcha.compose.engine.components.content.header_footer.HeaderFooterZone;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.render.pdf.GlyphFallbackLogger;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.font.FontLibrary;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.util.List;

/**
 * PDF-specific helper that renders repeating headers and footers on every page.
 *
 * <p>This is a post-processing step called after all content has been rendered
 * and pagination is complete. It draws text into reserved zones at the top
 * (header) and bottom (footer) of each page.</p>
 *
 * @author Artem Demchyshyn
 */
@Slf4j
public final class PdfHeaderFooterRenderer {

    private PdfHeaderFooterRenderer() {
        // static helper
    }

    /**
     * Applies the given header/footer configurations to every page.
     *
     * @param doc     the target PDF document
     * @param configs list of header/footer configurations
     * @param fonts   font library used to resolve each zone's font family
     * @param marginLeft  left margin of the main canvas
     * @param marginRight right margin of the main canvas
     * @throws IOException if writing to the content stream fails
     */
    public static void apply(PDDocument doc,
                             List<HeaderFooterConfig> configs,
                             FontLibrary fonts,
                             float marginLeft,
                             float marginRight) throws IOException {
        if (doc == null) return;
        apply(doc, configs, fonts, marginLeft, marginRight, 0, doc.getNumberOfPages());
    }

    /**
     * Applies the given header/footer configurations to one contiguous window of
     * pages, numbering them relative to that window.
     *
     * <p>This is the multi-section entry point: each section of a combined
     * document gets its own header/footer with section-local {@code {page}} /
     * {@code {pages}} tokens. {@code currentPage} runs {@code 1..sectionPageCount}
     * within the window (so {@link HeaderFooterConfig#appliesTo} and the page
     * counter restart per section) and {@code totalPages} is the section's own
     * page count rather than the whole document's.</p>
     *
     * @param doc             the target PDF document
     * @param configs         list of header/footer configurations
     * @param fonts           font library used to resolve each zone's font family
     * @param marginLeft      left margin of the section canvas
     * @param marginRight     right margin of the section canvas
     * @param basePageOffset  zero-based index of the window's first physical page
     * @param sectionPageCount number of pages in the window
     * @throws IOException if writing to the content stream fails
     */
    public static void apply(PDDocument doc,
                             List<HeaderFooterConfig> configs,
                             FontLibrary fonts,
                             float marginLeft,
                             float marginRight,
                             int basePageOffset,
                             int sectionPageCount) throws IOException {
        if (configs == null || configs.isEmpty()) return;

        for (int local = 0; local < sectionPageCount; local++) {
            int physical = basePageOffset + local;
            PDPage page = doc.getPage(physical);
            PDRectangle mediaBox = page.getMediaBox();

            try (PDPageContentStream cs = new PDPageContentStream(
                    doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                for (HeaderFooterConfig config : configs) {
                    if (!config.appliesTo(local + 1)) {
                        continue;
                    }
                    renderZone(cs, config, fonts, mediaBox, local + 1, sectionPageCount, marginLeft, marginRight);
                }
            }
        }
    }

    private static void renderZone(PDPageContentStream cs,
                                   HeaderFooterConfig config,
                                   FontLibrary fonts,
                                   PDRectangle mediaBox,
                                   int currentPage,
                                   int totalPages,
                                   float marginLeft,
                                   float marginRight) throws IOException {
        PDFont font = resolveFont(fonts, config);
        float fontSize = config.getFontSize();
        float pageWidth = mediaBox.getWidth();
        float usableWidth = pageWidth - marginLeft - marginRight;

        // Determine Y position
        float baseY;
        if (config.getZone() == HeaderFooterZone.HEADER) {
            baseY = mediaBox.getHeight() - config.getHeight() + fontSize / 2f;
        } else {
            baseY = config.getHeight() - fontSize;
        }

        cs.setNonStrokingColor(config.getTextColor());

        // Zone text frequently includes page-number glyphs, copyright marks,
        // or user branding outside the chosen family's coverage. Sanitise each
        // zone through GlyphFallbackLogger so unsupported code points become
        // '?' instead of crashing the whole document render — the same policy
        // the body text runs under, since the engine has no fallback chain.
        String leftText = GlyphFallbackLogger.sanitize(font,
                config.resolveTokens(config.getLeftText(), currentPage, totalPages));
        if (!leftText.isEmpty()) {
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(marginLeft, baseY);
            cs.showText(leftText);
            cs.endText();
        }

        String centerText = GlyphFallbackLogger.sanitize(font,
                config.resolveTokens(config.getCenterText(), currentPage, totalPages));
        if (!centerText.isEmpty()) {
            float textWidth = font.getStringWidth(centerText) / 1000f * fontSize;
            float centerX = marginLeft + (usableWidth - textWidth) / 2f;
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(centerX, baseY);
            cs.showText(centerText);
            cs.endText();
        }

        String rightText = GlyphFallbackLogger.sanitize(font,
                config.resolveTokens(config.getRightText(), currentPage, totalPages));
        if (!rightText.isEmpty()) {
            float textWidth = font.getStringWidth(rightText) / 1000f * fontSize;
            float rightX = pageWidth - marginRight - textWidth;
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(rightX, baseY);
            cs.showText(rightText);
            cs.endText();
        }

        // Separator line
        if (config.isShowSeparator()) {
            float separatorY;
            if (config.getZone() == HeaderFooterZone.HEADER) {
                separatorY = mediaBox.getHeight() - config.getHeight();
            } else {
                separatorY = config.getHeight();
            }

            cs.setStrokingColor(config.getSeparatorColor());
            cs.setLineWidth(config.getSeparatorThickness());
            cs.moveTo(marginLeft, separatorY);
            cs.lineTo(pageWidth - marginRight, separatorY);
            cs.stroke();
        }
    }

    /**
     * Resolves the zone's font family through the document's own font library, so
     * a header or footer draws in the same face as the body asked for rather than
     * in a font of this renderer's choosing.
     *
     * <p>Falls back to standard-14 Helvetica when the library cannot supply the
     * family — a zone is chrome, and failing to draw it would cost the caller the
     * whole document over a font name.</p>
     */
    private static PDFont resolveFont(FontLibrary fonts, HeaderFooterConfig config) {
        TextStyle style = config.textStyle();
        if (fonts != null) {
            PdfFont resolved = fonts.getFont(style.fontName(), PdfFont.class).orElse(null);
            if (resolved != null) {
                return resolved.fontType(style.decoration());
            }
        }
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }
}
