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
 * Templates v2 "Editorial Blue" CV preset.
 *
 * <p>Lighter, editorial sibling of {@link BlueBanner} — softer blue
 * accents and thinner heading weight, with a soft fill behind the
 * section block. The legacy preset rendered headings on a soft blue
 * fill; the v2 port keeps the editorial-blue palette and defers the
 * fill to a future Banner-decoration integration.</p>
 *
 * <p>Visual signature ported from the legacy
 * {@code EditorialBlueCvTemplateComposer}:</p>
 *
 * <ul>
 *   <li>ACCENT      = #AEBEDB (soft editorial blue — heading colour)</li>
 *   <li>SOFT_FILL   = #F5F7FC (very light blue page accent — applied
 *       via decorations in a follow-up)</li>
 *   <li>Body INK kept dark for legibility on the soft-fill backdrop</li>
 *   <li>Helvetica family — clean sans</li>
 * </ul>
 */
public final class EditorialBlue {

    /** Stable template identifier. */
    public static final String ID = "editorial-blue";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Editorial Blue";

    private EditorialBlue() {
    }

    /**
     * Builds a fresh {@code Editorial Blue} template.
     *
     * @param theme active business theme
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.comfortable();

        DocumentTextStyle headingStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(13.5)
                .decoration(DocumentTextDecoration.BOLD)
                .color(DocumentColor.rgb(86, 114, 158))     // medium editorial blue
                .build();

        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(10.0)
                .color(DocumentColor.rgb(40, 50, 70))       // dark navy ink
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
