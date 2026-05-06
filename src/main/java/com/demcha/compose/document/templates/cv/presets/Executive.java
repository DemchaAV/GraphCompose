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
 * Templates v2 "Executive" CV preset.
 *
 * <p>Formal, serif-driven CV with airy spacing and a slate-blue accent.
 * Suited for senior leadership / consulting roles where a print-
 * newspaper feel reads as gravitas. Uses the airy spacing rhythm so
 * each section gets generous breathing room.</p>
 *
 * <p>Visual signature ported from the legacy
 * {@code ExecutiveSlateCvTemplateComposer} (which delegated heading
 * colour to {@code CvTheme.timesRoman()}'s primary slate blue):</p>
 *
 * <ul>
 *   <li>Heading colour = #2C3E50 (slate blue, primary token of V1
 *       Times Roman theme)</li>
 *   <li>Body INK = #2D2B28 (warm dark, easier on print)</li>
 *   <li>Times Roman family — formal serif throughout</li>
 * </ul>
 */
public final class Executive {

    /** Stable template identifier. */
    public static final String ID = "executive";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Executive";

    private Executive() {
    }

    /**
     * Builds a fresh {@code Executive} template.
     *
     * @param theme active business theme
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.airy();

        DocumentTextStyle headingStyle = DocumentTextStyle.builder()
                .fontName(FontName.TIMES_BOLD)
                .size(16.0)
                .decoration(DocumentTextDecoration.BOLD)
                .color(DocumentColor.rgb(44, 62, 80))       // slate blue
                .build();

        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.TIMES_ROMAN)
                .size(10.5)
                .color(DocumentColor.rgb(45, 43, 40))       // warm dark
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
