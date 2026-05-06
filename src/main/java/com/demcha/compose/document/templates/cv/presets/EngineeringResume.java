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
 * Templates v2 "Engineering Resume" CV preset.
 *
 * <p>Tech-focused CV with deep navy headings and an emerald-green
 * accent. Was {@code TechLeadCvTemplate} in the legacy surface;
 * renamed for accuracy — the preset suits any engineering role, not
 * just team-lead positions.</p>
 *
 * <p>Visual signature ported from the legacy
 * {@code TechLeadCvTemplateComposer}:</p>
 *
 * <ul>
 *   <li>NAVY (heading)  = #0D202F (deep navy)</li>
 *   <li>INK  (body)     = #202A37 (cool dark grey)</li>
 *   <li>GREEN (accent)  = #1B9168 (emerald — used for inline
 *       emphasis, here applied via Module.Style heading colour
 *       fallback only)</li>
 *   <li>Helvetica family — clean sans throughout</li>
 * </ul>
 */
public final class EngineeringResume {

    /** Stable template identifier. */
    public static final String ID = "engineering-resume";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Engineering Resume";

    private EngineeringResume() {
    }

    /**
     * Builds a fresh {@code Engineering Resume} template.
     *
     * @param theme active business theme
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.compact();

        DocumentTextStyle headingStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(15.5)
                .decoration(DocumentTextDecoration.BOLD)
                .color(DocumentColor.rgb(13, 32, 47))       // NAVY
                .build();

        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(10.0)
                .color(DocumentColor.rgb(32, 42, 55))       // INK cool dark
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
