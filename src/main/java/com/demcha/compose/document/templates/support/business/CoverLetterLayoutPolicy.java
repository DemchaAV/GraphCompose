package com.demcha.compose.document.templates.support.business;

import com.demcha.compose.document.templates.support.common.TemplateLayoutPolicy;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.engine.components.style.Margin;

import java.util.Objects;

/**
 * Cover-letter rhythm tokens kept outside the composer for predictable spacing.
 */
record CoverLetterLayoutPolicy(
        TemplateLayoutPolicy rhythm,
        Margin headerNameMargin,
        Margin headerTrailingMargin,
        Margin closingMargin,
        String bodyFirstLineIndent
) {
    CoverLetterLayoutPolicy {
        rhythm = Objects.requireNonNull(rhythm, "rhythm");
        headerNameMargin = headerNameMargin == null ? Margin.zero() : headerNameMargin;
        headerTrailingMargin = headerTrailingMargin == null ? Margin.zero() : headerTrailingMargin;
        closingMargin = closingMargin == null ? Margin.zero() : closingMargin;
        bodyFirstLineIndent = bodyFirstLineIndent == null ? "" : bodyFirstLineIndent;
    }

    static CoverLetterLayoutPolicy standard(CvTheme theme) {
        TemplateLayoutPolicy rhythm = TemplateLayoutPolicy.standardCv(theme);
        return new CoverLetterLayoutPolicy(
                rhythm,
                rhythm.margin(0, 10, 5, 0),
                rhythm.margin(0, 10, 0, 0),
                rhythm.margin(20, 20, 0, 0),
                "  ");
    }
}
