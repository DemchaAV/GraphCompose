package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterZone;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfMetadataOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfProtectionOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkLayer;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkPosition;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.document.output.DocumentProtection;
import com.demcha.compose.document.output.DocumentWatermark;
import com.demcha.compose.document.output.DocumentWatermarkLayer;
import com.demcha.compose.document.output.DocumentWatermarkPosition;

/**
 * Translates backend-neutral document output options into PDF-specific
 * configuration consumed by {@link PdfFixedLayoutBackend}.
 *
 * <p>Translation is one-way: canonical {@code DocumentXxx} types come in,
 * PDF {@code PdfXxxOptions} values come out. The translator never mutates the
 * canonical inputs.</p>
 *
 * @author Artem Demchyshyn
 */
public final class PdfOutputOptionsTranslator {

    private PdfOutputOptionsTranslator() {
    }

    /**
     * Translates canonical document metadata into the PDF backend value.
     *
     * @param metadata canonical metadata, may be {@code null}
     * @return PDF metadata options or {@code null} when input is {@code null}
     */
    public static PdfMetadataOptions toPdf(DocumentMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        return PdfMetadataOptions.builder()
                .title(metadata.getTitle())
                .author(metadata.getAuthor())
                .subject(metadata.getSubject())
                .keywords(metadata.getKeywords())
                .creator(metadata.getCreator())
                .producer(metadata.getProducer())
                .build();
    }

    /**
     * Translates a canonical watermark into the PDF backend value.
     *
     * @param watermark canonical watermark, may be {@code null}
     * @return PDF watermark options or {@code null} when input is {@code null}
     */
    public static PdfWatermarkOptions toPdf(DocumentWatermark watermark) {
        if (watermark == null) {
            return null;
        }
        return PdfWatermarkOptions.builder()
                .text(watermark.getText())
                .fontSize(watermark.getFontSize())
                .rotation(watermark.getRotation())
                .color(watermark.getColor() == null ? null : watermark.getColor().color())
                .imagePath(watermark.getImagePath())
                .imageBytes(watermark.getImageBytes())
                .opacity(watermark.getOpacity())
                .layer(toPdf(watermark.getLayer()))
                .position(toPdf(watermark.getPosition()))
                .build();
    }

    /**
     * Translates canonical document protection into the PDF backend value.
     *
     * @param protection canonical protection, may be {@code null}
     * @return PDF protection options or {@code null} when input is {@code null}
     */
    public static PdfProtectionOptions toPdf(DocumentProtection protection) {
        if (protection == null) {
            return null;
        }
        return PdfProtectionOptions.builder()
                .userPassword(protection.getUserPassword())
                .ownerPassword(protection.getOwnerPassword())
                .canPrint(protection.isCanPrint())
                .canCopyContent(protection.isCanCopyContent())
                .canModify(protection.isCanModify())
                .canFillForms(protection.isCanFillForms())
                .canExtractForAccessibility(protection.isCanExtractForAccessibility())
                .canAssemble(protection.isCanAssemble())
                .canPrintHighQuality(protection.isCanPrintHighQuality())
                .keyLength(protection.getKeyLength())
                .build();
    }

    /**
     * Translates a canonical header/footer entry into the PDF backend value.
     *
     * @param entry canonical header/footer, may be {@code null}
     * @return PDF header/footer options or {@code null} when input is {@code null}
     */
    public static PdfHeaderFooterOptions toPdf(DocumentHeaderFooter entry) {
        if (entry == null) {
            return null;
        }
        return PdfHeaderFooterOptions.builder()
                .zone(toPdf(entry.getZone()))
                .height(entry.getHeight())
                .leftText(entry.getLeftText())
                .centerText(entry.getCenterText())
                .rightText(entry.getRightText())
                .fontSize(entry.getFontSize())
                .textColor(entry.getTextColor() == null ? null : entry.getTextColor().color())
                .showSeparator(entry.isShowSeparator())
                .separatorColor(entry.getSeparatorColor() == null ? null : entry.getSeparatorColor().color())
                .separatorThickness(entry.getSeparatorThickness())
                .build();
    }

    private static PdfHeaderFooterZone toPdf(DocumentHeaderFooterZone zone) {
        if (zone == null) {
            return PdfHeaderFooterZone.HEADER;
        }
        return switch (zone) {
            case HEADER -> PdfHeaderFooterZone.HEADER;
            case FOOTER -> PdfHeaderFooterZone.FOOTER;
        };
    }

    private static PdfWatermarkLayer toPdf(DocumentWatermarkLayer layer) {
        if (layer == null) {
            return PdfWatermarkLayer.BEHIND_CONTENT;
        }
        return switch (layer) {
            case BEHIND_CONTENT -> PdfWatermarkLayer.BEHIND_CONTENT;
            case ABOVE_CONTENT -> PdfWatermarkLayer.ABOVE_CONTENT;
        };
    }

    private static PdfWatermarkPosition toPdf(DocumentWatermarkPosition position) {
        if (position == null) {
            return PdfWatermarkPosition.CENTER;
        }
        return switch (position) {
            case CENTER -> PdfWatermarkPosition.CENTER;
            case TOP_LEFT -> PdfWatermarkPosition.TOP_LEFT;
            case TOP_RIGHT -> PdfWatermarkPosition.TOP_RIGHT;
            case BOTTOM_LEFT -> PdfWatermarkPosition.BOTTOM_LEFT;
            case BOTTOM_RIGHT -> PdfWatermarkPosition.BOTTOM_RIGHT;
            case TILE -> PdfWatermarkPosition.TILE;
        };
    }
}
