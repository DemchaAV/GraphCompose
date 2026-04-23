package com.demcha.compose.document.templates.support.business;

import com.demcha.compose.document.templates.support.common.TemplateLayoutPolicy;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.layout_core.components.style.Margin;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessDocumentLayoutPolicyTest {

    @Test
    void shouldCentralizeInvoiceAndProposalBusinessGeometryTokens() {
        BusinessDocumentLayoutPolicy policy = BusinessDocumentLayoutPolicy.standard();

        assertThat(policy.rhythm()).isEqualTo(TemplateLayoutPolicy.businessDocument());
        assertThat(policy.columnGap()).isEqualTo(18.0);
        assertThat(policy.mainDividerThickness()).isEqualTo(1.2);
        assertThat(policy.proposalSummaryDividerThickness()).isEqualTo(1.1);
        assertThat(policy.sectionDividerThickness()).isEqualTo(1.0);
        assertThat(policy.tableBorderThickness()).isEqualTo(1.3);
        assertThat(policy.invoiceSummaryWidth()).isEqualTo(206.0);
        assertThat(policy.proposalSummaryRuleWidth()).isEqualTo(170.0);
        assertThat(policy.proposalSectionRuleWidth()).isEqualTo(132.0);
    }

    @Test
    void shouldProvideReusableBusinessLayoutHelpers() {
        BusinessDocumentLayoutPolicy policy = BusinessDocumentLayoutPolicy.standard();

        assertThat(policy.twoColumnWidth(500)).isEqualTo(241.0);
        assertThat(policy.leftWidthForReservedRight(500, 220, 188)).isEqualTo(312.0);
        assertThat(policy.rightWidth(500, 312)).isEqualTo(170.0);
        assertThat(policy.boundedRuleWidth(100, 132)).isEqualTo(100.0);
        assertThat(policy.moduleBodyGap(Margin.top(4))).isEqualTo(new Margin(14, 0, 0, 0));
    }

    @Test
    void shouldCentralizeCoverLetterRhythmTokens() {
        CoverLetterLayoutPolicy policy = CoverLetterLayoutPolicy.standard(CvTheme.defaultTheme());

        assertThat(policy.rhythm()).isEqualTo(TemplateLayoutPolicy.standardCv(CvTheme.defaultTheme()));
        assertThat(policy.headerNameMargin()).isEqualTo(new Margin(0, 10, 5, 0));
        assertThat(policy.headerTrailingMargin()).isEqualTo(new Margin(0, 10, 0, 0));
        assertThat(policy.closingMargin()).isEqualTo(new Margin(20, 20, 0, 0));
        assertThat(policy.bodyFirstLineIndent()).isEqualTo("  ");
    }
}
