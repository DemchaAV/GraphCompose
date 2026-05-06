package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.components.Header;
import com.demcha.compose.document.templates.components.Module;
import com.demcha.compose.document.templates.cv.builder.CvBuilder;
import com.demcha.compose.document.templates.cv.layouts.SingleColumn;
import com.demcha.compose.document.templates.cv.spec.CvSpec;
import com.demcha.compose.document.templates.themes.Spacing;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.font.FontName;

/**
 * Templates v2 "Blue Banner" CV preset.
 *
 * <p>Bold editorial CV with full-width blue banners behind each
 * section heading. The legacy preset rendered each heading inside a
 * coloured rectangle the width of the page; the v2 port retains the
 * banner-blue colour signature on heading text but defers the
 * full-width fill to a future Banner-decoration integration. Until
 * then, the saturated banner blue carries the Blue-Banner identity.</p>
 *
 * <p>Visual signature ported from the legacy
 * {@code BlueBannerCvTemplateComposer}:</p>
 *
 * <ul>
 *   <li>BANNER_BG  = #7092BE (medium blue — heading colour in v2,
 *       full-width fill in a follow-up)</li>
 *   <li>BANNER_RULE = #3A5276 (darker blue — for the rule below
 *       contact lines)</li>
 *   <li>PT Serif family on the legacy preset → Times Roman in v2</li>
 * </ul>
 */
public final class BlueBanner {

    /** Stable template identifier. */
    public static final String ID = "blue-banner";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Blue Banner";

    private BlueBanner() {
    }

    /**
     * Builds a fresh {@code Blue Banner} template.
     *
     * @param theme active business theme
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.comfortable();

        DocumentTextStyle headingStyle = DocumentTextStyle.builder()
                .fontName(FontName.TIMES_BOLD)
                .size(13.0)
                .decoration(DocumentTextDecoration.BOLD)
                .color(DocumentColor.rgb(112, 146, 190))    // BANNER_BG
                .build();

        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.TIMES_ROMAN)
                .size(10.0)
                .color(DocumentColor.rgb(22, 32, 48))       // dark navy ink
                .build();

        Module.Style moduleStyle = new Module.Style(
                headingStyle,
                bodyStyle,
                spacing.sectionTitleAbove(),
                spacing.sectionTitleBelow());

        return CvBuilder.builder()
                .id(ID)
                .displayName(DISPLAY_NAME)
                .theme(theme)
                .spacing(spacing)
                .layout(SingleColumn.layout()
                        .moduleGap(spacing.moduleGap()))
                .header(Header.rightAligned(theme, spacing))
                .moduleStyle(moduleStyle)
                .place(SingleColumn.MAIN,
                        "Professional Summary",
                        "Technical Skills",
                        "Education & Certifications",
                        "Projects",
                        "Professional Experience",
                        "Additional Information")
                .build();
    }
}
