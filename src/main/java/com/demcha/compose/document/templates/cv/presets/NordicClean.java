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
 * Templates v2 "Nordic Clean" CV preset.
 *
 * <p>Calm modern CV with restrained teal accents and generous
 * whitespace. Uses a comfortable spacing rhythm (rather than the tight
 * compact one) to lean into the Nordic-design "lots of breathing room"
 * aesthetic; the heading colour is a muted teal rather than the
 * stronger blue used by Modern Professional.</p>
 *
 * <p>Visual signature ported from the legacy
 * {@code NordicCleanCvTemplateComposer}:</p>
 *
 * <ul>
 *   <li>INK   = #122734 (dark teal-leaning ink for body text)</li>
 *   <li>ACCENT = #1C8087 (muted teal for section headings)</li>
 *   <li>Helvetica family throughout — sans-serif minimalism</li>
 * </ul>
 *
 * <p>To customise: copy the body of {@link #create(BusinessTheme)} into
 * your own class and tweak any of the styles or spacing tokens.</p>
 */
public final class NordicClean {

    /** Stable template identifier. */
    public static final String ID = "nordic-clean";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Nordic Clean";

    private NordicClean() {
        // utility class — not instantiable
    }

    /**
     * Builds a fresh {@code Nordic Clean} template configured for the
     * given business theme.
     *
     * @param theme active business theme (palette is overridden by the
     *              preset's Nordic-specific tokens; typography is
     *              overridden to the V1 Helvetica baseline)
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.comfortable();

        DocumentTextStyle headingStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(15.0)
                .decoration(DocumentTextDecoration.BOLD)
                .color(DocumentColor.rgb(28, 128, 135))     // ACCENT teal
                .build();

        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(10.0)
                .color(DocumentColor.rgb(18, 39, 52))       // INK
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
