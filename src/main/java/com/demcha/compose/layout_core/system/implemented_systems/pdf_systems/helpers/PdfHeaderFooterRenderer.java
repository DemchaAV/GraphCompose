package com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.helpers;

import com.demcha.compose.layout_core.components.content.header_footer.HeaderFooterConfig;
import com.demcha.compose.layout_core.components.content.header_footer.HeaderFooterZone;
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
     * @param marginLeft  left margin of the main canvas
     * @param marginRight right margin of the main canvas
     * @throws IOException if writing to the content stream fails
     */
    public static void apply(PDDocument doc,
                             List<HeaderFooterConfig> configs,
                             float marginLeft,
                             float marginRight) throws IOException {
        if (configs == null || configs.isEmpty()) return;

        int totalPages = doc.getNumberOfPages();

        for (int i = 0; i < totalPages; i++) {
            PDPage page = doc.getPage(i);
            PDRectangle mediaBox = page.getMediaBox();

            try (PDPageContentStream cs = new PDPageContentStream(
                    doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                for (HeaderFooterConfig config : configs) {
                    renderZone(cs, config, mediaBox, i + 1, totalPages, marginLeft, marginRight);
                }
            }
        }
    }

    private static void renderZone(PDPageContentStream cs,
                                   HeaderFooterConfig config,
                                   PDRectangle mediaBox,
                                   int currentPage,
                                   int totalPages,
                                   float marginLeft,
                                   float marginRight) throws IOException {
        PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
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

        // Left text
        String leftText = HeaderFooterConfig.resolvePlaceholders(config.getLeftText(), currentPage, totalPages);
        if (leftText != null && !leftText.isEmpty()) {
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(marginLeft, baseY);
            cs.showText(leftText);
            cs.endText();
        }

        // Center text
        String centerText = HeaderFooterConfig.resolvePlaceholders(config.getCenterText(), currentPage, totalPages);
        if (centerText != null && !centerText.isEmpty()) {
            float textWidth = font.getStringWidth(centerText) / 1000f * fontSize;
            float centerX = marginLeft + (usableWidth - textWidth) / 2f;
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(centerX, baseY);
            cs.showText(centerText);
            cs.endText();
        }

        // Right text
        String rightText = HeaderFooterConfig.resolvePlaceholders(config.getRightText(), currentPage, totalPages);
        if (rightText != null && !rightText.isEmpty()) {
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
}
