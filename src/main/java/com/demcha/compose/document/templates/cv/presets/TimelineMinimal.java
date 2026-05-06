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
 * Templates v2 "Timeline Minimal" CV preset.
 *
 * <p>Stripped-back grey-scale CV with a focus on chronology and
 * whitespace. The original used Google Fonts {@code Barlow Condensed}
 * and {@code Lato}; the v2 port falls back to Helvetica until the
 * project ships its own font registration helper for Templates v2
 * presets.</p>
 *
 * <p>Visual signature ported from the legacy
 * {@code TimelineMinimalCvTemplateComposer}:</p>
 *
 * <ul>
 *   <li>INK  = #4A4A4A (medium grey for body and headings)</li>
 *   <li>SOFT = #7A7A7A (lighter grey for muted text)</li>
 *   <li>Helvetica family — clean sans, no colour accent</li>
 *   <li>Comfortable spacing rhythm to read airy</li>
 * </ul>
 */
public final class TimelineMinimal {

    /** Stable template identifier. */
    public static final String ID = "timeline-minimal";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Timeline Minimal";

    private TimelineMinimal() {
    }

    /**
     * Builds a fresh {@code Timeline Minimal} template.
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
                .color(DocumentColor.rgb(74, 74, 74))       // INK medium grey
                .build();

        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(10.0)
                .color(DocumentColor.rgb(74, 74, 74))       // same INK
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
