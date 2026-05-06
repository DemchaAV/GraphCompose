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
 * Templates v2 "Boxed Sections" CV preset.
 *
 * <p>Editorial CV with section headings in a soft grey banner. The
 * legacy preset rendered each heading inside a light fill rectangle;
 * the v2 port keeps the typography and palette but defers the banner
 * background to a follow-up that adds Banner-decoration support to
 * Module rendering. Until then, the colour and weight of the heading
 * carry the boxed-sections personality.</p>
 *
 * <p>Visual signature ported from the legacy
 * {@code BoxedSectionsCvTemplateComposer}:</p>
 *
 * <ul>
 *   <li>INK    = #222222 (dark body text)</li>
 *   <li>BANNER = #DCE2E6 (light grey banner — applied as heading
 *       background in a future iteration)</li>
 *   <li>PT Serif family on the legacy preset → Times Roman in v2
 *       (font registration helper for v2 presets is a follow-up)</li>
 * </ul>
 */
public final class BoxedSections {

    /** Stable template identifier. */
    public static final String ID = "boxed-sections";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Boxed Sections";

    private BoxedSections() {
    }

    /**
     * Builds a fresh {@code Boxed Sections} template.
     *
     * @param theme active business theme
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.comfortable();

        DocumentTextStyle headingStyle = DocumentTextStyle.builder()
                .fontName(FontName.TIMES_BOLD)
                .size(13.5)
                .decoration(DocumentTextDecoration.BOLD)
                .color(DocumentColor.rgb(34, 34, 34))       // INK
                .build();

        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.TIMES_ROMAN)
                .size(10.5)
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
