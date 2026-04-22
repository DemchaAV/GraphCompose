package com.demcha.compose.document.templates.support.common;

import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

import java.util.Objects;

/**
 * Shared rhythm tokens for canonical template scene composers.
 *
 * <p><b>Pipeline role:</b> keeps module gaps, body text spacing, list item
 * spacing, and reusable cell padding in one support-level object so built-in
 * templates do not hide layout rules in scattered local constants.</p>
 *
 * @param rootSpacing spacing between top-level blocks in the root flow
 * @param sectionMargin margin applied to section/module headings
 * @param subsectionMargin margin applied to nested section blocks
 * @param blockMargin margin applied to regular body blocks when a template
 *                    needs an explicit top gap
 * @param bodyLineSpacing extra spacing between wrapped body text lines
 * @param bodyItemSpacing extra spacing between list items
 * @param tableLineSpacing extra spacing between multiple text lines inside
 *                         table cells
 * @param markerlessContinuationIndent prefix used for wrapped continuation
 *                                     lines in markerless rows
 * @param bodyPadding default body block padding
 * @param compactCellPadding reusable padding for compact table cells
 * @param contentCellPadding reusable padding for content-heavy table cells
 * @author Artem Demchyshyn
 */
public record TemplateLayoutPolicy(
        double rootSpacing,
        Margin sectionMargin,
        Margin subsectionMargin,
        Margin blockMargin,
        double bodyLineSpacing,
        double bodyItemSpacing,
        double tableLineSpacing,
        String markerlessContinuationIndent,
        Padding bodyPadding,
        Padding compactCellPadding,
        Padding contentCellPadding
) {
    /**
     * Creates a normalized layout policy.
     */
    public TemplateLayoutPolicy {
        validateSpacing(rootSpacing, "rootSpacing");
        validateSpacing(bodyLineSpacing, "bodyLineSpacing");
        validateSpacing(bodyItemSpacing, "bodyItemSpacing");
        validateSpacing(tableLineSpacing, "tableLineSpacing");
        sectionMargin = sectionMargin == null ? Margin.zero() : sectionMargin;
        subsectionMargin = subsectionMargin == null ? Margin.zero() : subsectionMargin;
        blockMargin = blockMargin == null ? Margin.zero() : blockMargin;
        markerlessContinuationIndent = markerlessContinuationIndent == null ? "" : markerlessContinuationIndent;
        bodyPadding = bodyPadding == null ? Padding.zero() : bodyPadding;
        compactCellPadding = compactCellPadding == null ? Padding.zero() : compactCellPadding;
        contentCellPadding = contentCellPadding == null ? Padding.zero() : contentCellPadding;
    }

    /**
     * Returns the historical rhythm used by the standard CV template.
     *
     * @param theme CV visual theme whose spacing tokens are reused
     * @return layout policy for the standard CV
     */
    public static TemplateLayoutPolicy standardCv(CvTheme theme) {
        CvTheme safeTheme = Objects.requireNonNull(theme, "theme");
        return new TemplateLayoutPolicy(
                safeTheme.spacingModuleName(),
                Margin.of(5),
                Margin.zero(),
                Margin.zero(),
                safeTheme.spacing(),
                safeTheme.spacing(),
                0.0,
                "  ",
                new Padding(0, 5, 0, 20),
                new Padding(2, 0, 2, 0),
                new Padding(7, 8, 7, 8));
    }

    /**
     * Returns the compact business rhythm used by the executive CV template.
     *
     * @return layout policy for executive CV documents
     */
    public static TemplateLayoutPolicy executiveCv() {
        return new TemplateLayoutPolicy(
                0.0,
                Margin.top(7),
                Margin.top(2),
                Margin.top(2),
                1.8,
                1.4,
                0.0,
                "  ",
                new Padding(0, 0, 1, 9),
                new Padding(2, 0, 2, 0),
                new Padding(7, 8, 7, 8));
    }

    /**
     * Returns the default rhythm shared by built-in business documents.
     *
     * @return layout policy for invoice/proposal style templates
     */
    public static TemplateLayoutPolicy businessDocument() {
        return new TemplateLayoutPolicy(
                10.0,
                Margin.top(6),
                Margin.top(4),
                Margin.top(3),
                2.0,
                2.0,
                1.2,
                "  ",
                Padding.zero(),
                new Padding(2, 0, 2, 0),
                new Padding(7, 8, 7, 8));
    }

    /**
     * Creates a top-only margin after validating the spacing value.
     *
     * @param value top margin value
     * @return top-only margin
     */
    public Margin top(double value) {
        validateSpacing(value, "value");
        return Margin.top(value);
    }

    private static void validateSpacing(double value, String label) {
        if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(label + " must be finite and non-negative: " + value);
        }
    }
}
