package com.demcha.compose.document.api;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterZone;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfProtectionOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkLayer;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkPosition;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentProtection;
import com.demcha.compose.document.output.DocumentWatermark;
import com.demcha.compose.document.output.DocumentWatermarkLayer;
import com.demcha.compose.document.output.DocumentWatermarkPosition;
import com.demcha.compose.document.style.DocumentColor;

/**
 * Internal helper that converts the PDF-specific output option values into the
 * canonical, backend-neutral types stored on {@link DocumentSession}.
 *
 * <p>Keeping the helper inside {@code com.demcha.compose.document.api} keeps the
 * compatibility shim close to the session that actually consumes both shapes.
 * This class is intentionally package-private.</p>
 */
final class PdfOutputOptionsToCanonical {

    private PdfOutputOptionsToCanonical() {
    }

    static DocumentWatermark toCanonical(PdfWatermarkOptions options) {
        if (options == null) {
            return null;
        }
        return DocumentWatermark.builder()
                .text(options.getText())
                .fontSize(options.getFontSize())
                .rotation(options.getRotation())
                .color(options.getColor() == null ? null : DocumentColor.of(options.getColor()))
                .imagePath(options.getImagePath())
                .imageBytes(options.getImageBytes())
                .opacity(options.getOpacity())
                .layer(toCanonical(options.getLayer()))
                .position(toCanonical(options.getPosition()))
                .build();
    }

    static DocumentProtection toCanonical(PdfProtectionOptions options) {
        if (options == null) {
            return null;
        }
        return DocumentProtection.builder()
                .userPassword(options.getUserPassword())
                .ownerPassword(options.getOwnerPassword())
                .canPrint(options.isCanPrint())
                .canCopyContent(options.isCanCopyContent())
                .canModify(options.isCanModify())
                .canFillForms(options.isCanFillForms())
                .canExtractForAccessibility(options.isCanExtractForAccessibility())
                .canAssemble(options.isCanAssemble())
                .canPrintHighQuality(options.isCanPrintHighQuality())
                .keyLength(options.getKeyLength())
                .build();
    }

    static DocumentHeaderFooter toCanonical(PdfHeaderFooterOptions options) {
        if (options == null) {
            return null;
        }
        return DocumentHeaderFooter.builder()
                .zone(toCanonical(options.getZone()))
                .height(options.getHeight())
                .leftText(options.getLeftText())
                .centerText(options.getCenterText())
                .rightText(options.getRightText())
                .fontSize(options.getFontSize())
                .textColor(options.getTextColor() == null ? null : DocumentColor.of(options.getTextColor()))
                .showSeparator(options.isShowSeparator())
                .separatorColor(options.getSeparatorColor() == null ? null : DocumentColor.of(options.getSeparatorColor()))
                .separatorThickness(options.getSeparatorThickness())
                .build();
    }

    private static DocumentHeaderFooterZone toCanonical(PdfHeaderFooterZone zone) {
        if (zone == null) {
            return DocumentHeaderFooterZone.HEADER;
        }
        return switch (zone) {
            case HEADER -> DocumentHeaderFooterZone.HEADER;
            case FOOTER -> DocumentHeaderFooterZone.FOOTER;
        };
    }

    private static DocumentWatermarkLayer toCanonical(PdfWatermarkLayer layer) {
        if (layer == null) {
            return DocumentWatermarkLayer.BEHIND_CONTENT;
        }
        return switch (layer) {
            case BEHIND_CONTENT -> DocumentWatermarkLayer.BEHIND_CONTENT;
            case ABOVE_CONTENT -> DocumentWatermarkLayer.ABOVE_CONTENT;
        };
    }

    private static DocumentWatermarkPosition toCanonical(PdfWatermarkPosition position) {
        if (position == null) {
            return DocumentWatermarkPosition.CENTER;
        }
        return switch (position) {
            case CENTER -> DocumentWatermarkPosition.CENTER;
            case TOP_LEFT -> DocumentWatermarkPosition.TOP_LEFT;
            case TOP_RIGHT -> DocumentWatermarkPosition.TOP_RIGHT;
            case BOTTOM_LEFT -> DocumentWatermarkPosition.BOTTOM_LEFT;
            case BOTTOM_RIGHT -> DocumentWatermarkPosition.BOTTOM_RIGHT;
            case TILE -> DocumentWatermarkPosition.TILE;
        };
    }
}
