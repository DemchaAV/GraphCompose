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
 * Templates v2 "Panel" CV preset.
 *
 * <p>Two-column editorial CV with a soft turquoise sidebar and a
 * teal accent. Was {@code ProductLeaderCvTemplate} in the legacy
 * surface; renamed for accuracy — the preset is a generic two-column
 * panel layout suitable for any product / strategy / leadership
 * role, not just product leads.</p>
 *
 * <p>Visual signature ported from the legacy
 * {@code PanelCvTemplateComposer} default palette:</p>
 *
 * <ul>
 *   <li>SIDEBAR_BG = #E7F6F4 (soft turquoise — applied as full-column
 *       fill in a follow-up Banner-decoration integration)</li>
 *   <li>INK       = #142C42 (deep navy ink for body)</li>
 *   <li>ACCENT    = #008080 (teal — heading colour)</li>
 *   <li>Helvetica family</li>
 * </ul>
 *
 * <p>Slot mapping: Summary, Experience, Projects on the main column;
 * Education, Skills, Languages on the sidebar.</p>
 */
public final class Panel {

    /** Stable template identifier. */
    public static final String ID = "panel";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Panel";

    private Panel() {
    }

    /**
     * Builds a fresh {@code Panel} template.
     *
     * @param theme active business theme
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.compact();

        DocumentTextStyle headingStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(13.0)
                .decoration(DocumentTextDecoration.BOLD)
                .color(DocumentColor.rgb(0, 128, 128))      // ACCENT teal
                .build();

        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(10.0)
                .color(DocumentColor.rgb(20, 44, 66))       // INK deep navy
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
