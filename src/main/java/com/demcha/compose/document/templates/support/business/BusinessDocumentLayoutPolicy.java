package com.demcha.compose.document.templates.support.business;

import com.demcha.compose.document.templates.support.common.TemplateLayoutPolicy;
import com.demcha.compose.layout_core.components.style.Margin;

import java.util.Objects;

/**
 * Business-template geometry tokens shared by invoice and proposal composers.
 */
record BusinessDocumentLayoutPolicy(
        TemplateLayoutPolicy rhythm,
        double columnGap,
        double mainDividerThickness,
        double proposalSummaryDividerThickness,
        double sectionDividerThickness,
        double subtleDividerThickness,
        double tableBorderThickness,
        double invoiceHeaderReservedWidth,
        double invoiceSummaryWidth,
        double invoiceItemsRuleWidth,
        double invoiceNotesSummaryTopGap,
        double proposalHeaderReservedWidth,
        double proposalSummaryRuleWidth,
        double proposalSectionRuleWidth
) {
    BusinessDocumentLayoutPolicy {
        rhythm = Objects.requireNonNull(rhythm, "rhythm");
        validate(columnGap, "columnGap");
        validate(mainDividerThickness, "mainDividerThickness");
        validate(proposalSummaryDividerThickness, "proposalSummaryDividerThickness");
        validate(sectionDividerThickness, "sectionDividerThickness");
        validate(subtleDividerThickness, "subtleDividerThickness");
        validate(tableBorderThickness, "tableBorderThickness");
        validate(invoiceHeaderReservedWidth, "invoiceHeaderReservedWidth");
        validate(invoiceSummaryWidth, "invoiceSummaryWidth");
        validate(invoiceItemsRuleWidth, "invoiceItemsRuleWidth");
        validate(invoiceNotesSummaryTopGap, "invoiceNotesSummaryTopGap");
        validate(proposalHeaderReservedWidth, "proposalHeaderReservedWidth");
        validate(proposalSummaryRuleWidth, "proposalSummaryRuleWidth");
        validate(proposalSectionRuleWidth, "proposalSectionRuleWidth");
    }

    static BusinessDocumentLayoutPolicy standard() {
        return new BusinessDocumentLayoutPolicy(
                TemplateLayoutPolicy.businessDocument(),
                18.0,
                1.2,
                1.1,
                1.0,
                1.0,
                1.3,
                188.0,
                206.0,
                128.0,
                8.0,
                212.0,
                170.0,
                132.0);
    }

    double boundedRuleWidth(double pageWidth, double preferredWidth) {
        return Math.min(pageWidth, preferredWidth);
    }

    double twoColumnWidth(double pageWidth) {
        return (pageWidth - columnGap) / 2.0;
    }

    double leftWidthForReservedRight(double pageWidth, double minimumLeftWidth, double reservedRightWidth) {
        return Math.max(minimumLeftWidth, pageWidth - reservedRightWidth);
    }

    double rightWidth(double pageWidth, double leftWidth) {
        return pageWidth - leftWidth - columnGap;
    }

    Margin notesSummaryMargin() {
        return rhythm.top(invoiceNotesSummaryTopGap);
    }

    Margin moduleBodyGap(Margin margin) {
        return rhythm.withRootSpacingTop(margin);
    }

    private static void validate(double value, String label) {
        if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(label + " must be finite and non-negative: " + value);
        }
    }
}
