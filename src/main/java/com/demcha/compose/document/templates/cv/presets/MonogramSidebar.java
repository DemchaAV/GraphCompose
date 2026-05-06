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
 * Templates v2 "Monogram Sidebar" CV preset.
 *
 * <p>Two-column CV with a navy / pale-teal palette and a warm gold
 * accent. The legacy preset rendered a circular monogram of the
 * subject's initials in the sidebar; the v2 port keeps the colour
 * signature and column rhythm, and defers the monogram circle to a
 * follow-up that adds shape-decoration support to Header / Sidebar
 * components.</p>
 *
 * <p>Visual signature ported from the legacy
 * {@code MonogramSidebarCvTemplateComposer}:</p>
 *
 * <ul>
 *   <li>INK         = #252D3A (dark navy ink for body)</li>
 *   <li>ACCENT      = #9E9268 (warm gold — heading colour)</li>
 *   <li>MONOGRAM_RING = #363E4A (deep navy monogram outline —
 *       deferred)</li>
 *   <li>SIDEBAR_BG  = #E2EBEB (light teal-grey sidebar fill —
 *       deferred to Banner-decoration)</li>
 *   <li>Helvetica family</li>
 * </ul>
 */
public final class MonogramSidebar {

    /** Stable template identifier. */
    public static final String ID = "monogram-sidebar";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Monogram Sidebar";

    private MonogramSidebar() {
    }

    /**
     * Builds a fresh {@code Monogram Sidebar} template.
     *
     * @param theme active business theme
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.compact();

        DocumentTextStyle headingStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(12.5)
                .decoration(DocumentTextDecoration.BOLD)
                .color(DocumentColor.rgb(158, 146, 104))    // ACCENT warm gold
                .build();

        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(10.0)
                .color(DocumentColor.rgb(37, 45, 58))       // INK navy
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
                        .mainWeight(0.65)
                        .sidebarWeight(0.35)
                        .columnGap(20.0)
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
