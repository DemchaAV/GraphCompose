package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.components.Header;
import com.demcha.compose.document.templates.components.Module;
import com.demcha.compose.document.templates.cv.builder.CvBuilder;
import com.demcha.compose.document.templates.cv.layouts.TwoColumnSidebar;
import com.demcha.compose.document.templates.cv.spec.CvSpec;
import com.demcha.compose.document.templates.themes.Spacing;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.font.FontName;

/**
 * Templates v2 "Sidebar Portrait" CV preset.
 *
 * <p>Two-column CV with a warm-grey sidebar designed to accommodate a
 * portrait photo placeholder (legacy preset used Crimson Text serif
 * to deepen the editorial feel). The v2 port keeps the typographic
 * palette and grey accents; the photo placeholder slot will land in
 * a follow-up that adds image-block support to Templates v2 specs.</p>
 *
 * <p>Visual signature ported from the legacy
 * {@code SidebarPortraitCvTemplateComposer}:</p>
 *
 * <ul>
 *   <li>INK         = #222222 (dark grey body text)</li>
 *   <li>ACCENT      = #6A6A6A (medium grey heading)</li>
 *   <li>SIDEBAR_BG  = #F1F0ED (warm cream sidebar — applied as
 *       column fill in a Banner-decoration follow-up)</li>
 *   <li>Crimson Text serif on the legacy preset → Times Roman in v2</li>
 * </ul>
 */
public final class SidebarPortrait {

    /** Stable template identifier. */
    public static final String ID = "sidebar-portrait";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Sidebar Portrait";

    private SidebarPortrait() {
    }

    /**
     * Builds a fresh {@code Sidebar Portrait} template.
     *
     * @param theme active business theme
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.compact();

        DocumentTextStyle headingStyle = DocumentTextStyle.builder()
                .fontName(FontName.TIMES_BOLD)
                .size(12.5)
                .decoration(DocumentTextDecoration.BOLD)
                .color(DocumentColor.rgb(106, 106, 106))    // ACCENT medium grey
                .build();

        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.TIMES_ROMAN)
                .size(10.0)
                .color(DocumentColor.rgb(34, 34, 34))       // INK
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
                .layout(TwoColumnSidebar.layout()
                        .mainWeight(0.62)
                        .sidebarWeight(0.38)
                        .columnGap(18.0)
                        .moduleGap(spacing.moduleGap()))
                .header(Header.rightAligned(theme, spacing))
                .moduleStyle(moduleStyle)
                .place(TwoColumnSidebar.MAIN,
                        "Professional Summary",
                        "Professional Experience",
                        "Projects")
                .place(TwoColumnSidebar.SIDEBAR,
                        "Education & Certifications",
                        "Technical Skills",
                        "Additional Information")
                .build();
    }
}
