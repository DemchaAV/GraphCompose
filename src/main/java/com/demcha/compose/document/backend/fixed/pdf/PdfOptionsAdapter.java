package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfBarcodeOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfBarcodeType;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterZone;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfMetadataOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfProtectionOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkLayer;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkPosition;
import com.demcha.compose.layout_core.components.content.barcode.BarcodeData;
import com.demcha.compose.layout_core.components.content.barcode.BarcodeType;
import com.demcha.compose.layout_core.components.content.header_footer.HeaderFooterConfig;
import com.demcha.compose.layout_core.components.content.header_footer.HeaderFooterZone;
import com.demcha.compose.layout_core.components.content.metadata.DocumentMetadata;
import com.demcha.compose.layout_core.components.content.protection.PdfProtectionConfig;
import com.demcha.compose.layout_core.components.content.watermark.WatermarkConfig;
import com.demcha.compose.layout_core.components.content.watermark.WatermarkLayer;
import com.demcha.compose.layout_core.components.content.watermark.WatermarkPosition;

/**
 * Internal adapters from canonical {@code document.*} PDF options to the
 * existing production helper types in the internal PDF backend.
 */
final class PdfOptionsAdapter {
    private PdfOptionsAdapter() {
    }

    static DocumentMetadata toEngine(PdfMetadataOptions options) {
        if (options == null) {
            return null;
        }
        return DocumentMetadata.builder()
                .title(options.getTitle())
                .author(options.getAuthor())
                .subject(options.getSubject())
                .keywords(options.getKeywords())
                .creator(options.getCreator())
                .producer(options.getProducer())
                .build();
    }

    static WatermarkConfig toEngine(PdfWatermarkOptions options) {
        if (options == null) {
            return null;
        }
        return WatermarkConfig.builder()
                .text(options.getText())
                .fontSize(options.getFontSize())
                .rotation(options.getRotation())
                .color(options.getColor())
                .imagePath(options.getImagePath())
                .imageBytes(options.getImageBytes())
                .opacity(options.getOpacity())
                .layer(map(options.getLayer()))
                .position(map(options.getPosition()))
                .build();
    }

    static PdfProtectionConfig toEngine(PdfProtectionOptions options) {
        if (options == null) {
            return null;
        }
        return PdfProtectionConfig.builder()
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

    static HeaderFooterConfig toEngine(PdfHeaderFooterOptions options) {
        if (options == null) {
            return null;
        }
        return HeaderFooterConfig.builder()
                .zone(map(options.getZone()))
                .height(options.getHeight())
                .leftText(options.getLeftText())
                .centerText(options.getCenterText())
                .rightText(options.getRightText())
                .fontSize(options.getFontSize())
                .textColor(options.getTextColor())
                .showSeparator(options.isShowSeparator())
                .separatorColor(options.getSeparatorColor())
                .separatorThickness(options.getSeparatorThickness())
                .build();
    }

    static BarcodeData toEngine(PdfBarcodeOptions options) {
        if (options == null) {
            return null;
        }
        return BarcodeData.of(
                options.getContent(),
                map(options.getType()),
                options.getForeground(),
                options.getBackground(),
                options.getQuietZoneMargin());
    }

    private static WatermarkLayer map(PdfWatermarkLayer layer) {
        return layer == PdfWatermarkLayer.ABOVE_CONTENT
                ? WatermarkLayer.ABOVE_CONTENT
                : WatermarkLayer.BEHIND_CONTENT;
    }

    private static WatermarkPosition map(PdfWatermarkPosition position) {
        return switch (position) {
            case TOP_LEFT -> WatermarkPosition.TOP_LEFT;
            case TOP_RIGHT -> WatermarkPosition.TOP_RIGHT;
            case BOTTOM_LEFT -> WatermarkPosition.BOTTOM_LEFT;
            case BOTTOM_RIGHT -> WatermarkPosition.BOTTOM_RIGHT;
            case TILE -> WatermarkPosition.TILE;
            case CENTER -> WatermarkPosition.CENTER;
        };
    }

    private static HeaderFooterZone map(PdfHeaderFooterZone zone) {
        return zone == PdfHeaderFooterZone.FOOTER ? HeaderFooterZone.FOOTER : HeaderFooterZone.HEADER;
    }

    private static BarcodeType map(PdfBarcodeType type) {
        return switch (type) {
            case CODE_128 -> BarcodeType.CODE_128;
            case CODE_39 -> BarcodeType.CODE_39;
            case EAN_13 -> BarcodeType.EAN_13;
            case EAN_8 -> BarcodeType.EAN_8;
            case UPC_A -> BarcodeType.UPC_A;
            case PDF_417 -> BarcodeType.PDF_417;
            case DATA_MATRIX -> BarcodeType.DATA_MATRIX;
            case QR_CODE -> BarcodeType.QR_CODE;
        };
    }
}
