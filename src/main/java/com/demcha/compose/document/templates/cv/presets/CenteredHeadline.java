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
 * Templates v2 "Centered Headline" CV preset.
 *
 * <p>Minimalist CV with letter-spaced typography and thin horizontal
 * rules instead of coloured banners. The legacy preset centred the
 * subject's name and added a "PROFESSIONAL TITLE" sub-headline; the
 * v2 port keeps the typography palette and minimal grey aesthetic.
 * Header alignment will switch to {@code Header.centered(...)} once
 * that style variant ships in Phase B.3 follow-ups.</p>
 *
 * <p>Visual signature ported from the legacy
 * {@code CenteredHeadlineCvTemplateComposer}:</p>
 *
 * <ul>
 *   <li>INK      = #363636 (medium-dark grey body)</li>
 *   <li>HEADLINE = #464646 (slightly darker for headings)</li>
 *   <li>RULE     = #BCBCBC (thin separators — applied via decorations
 *       in a future iteration)</li>
 *   <li>Poppins / Lato on the legacy preset → Helvetica in v2</li>
 * </ul>
 */
public final class CenteredHeadline {

    /** Stable template identifier. */
    public static final String ID = "centered-headline";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Centered Headline";

    private CenteredHeadline() {
    }

    /**
     * Builds a fresh {@code Centered Headline} template.
     *
     * @param theme active business theme
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.comfortable();

        DocumentTextStyle headingStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(13.0)
                .decoration(DocumentTextDecoration.BOLD)
                .color(DocumentColor.rgb(70, 70, 70))       // HEADLINE
                .build();

        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(10.0)
                .color(DocumentColor.rgb(54, 54, 54))       // INK
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
