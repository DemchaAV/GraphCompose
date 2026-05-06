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
 * Templates v2 "Classic Serif" CV preset.
 *
 * <p>Editorial-leaning CV with serif typography and warm bronze
 * accents. Times Roman throughout for a formal print-newspaper feel,
 * and a comfortable spacing rhythm so the long-form serif body has
 * room to breathe.</p>
 *
 * <p>Visual signature ported from the legacy
 * {@code ClassicSerifCvTemplateComposer}:</p>
 *
 * <ul>
 *   <li>INK    = #2D2B28 (warm dark, easier on print than pure black)</li>
 *   <li>ACCENT = #7E5D34 (warm bronze for section headings)</li>
 *   <li>Times Roman family — serif throughout</li>
 * </ul>
 *
 * <p>To customise: copy the body of {@link #create(BusinessTheme)} into
 * your own class and tweak any of the styles or spacing tokens.</p>
 */
public final class ClassicSerif {

    /** Stable template identifier. */
    public static final String ID = "classic-serif";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Classic Serif";

    private ClassicSerif() {
        // utility class — not instantiable
    }

    /**
     * Builds a fresh {@code Classic Serif} template configured for the
     * given business theme.
     *
     * @param theme active business theme (palette + typography are
     *              overridden by the preset's serif-specific tokens)
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.comfortable();

        DocumentTextStyle headingStyle = DocumentTextStyle.builder()
                .fontName(FontName.TIMES_BOLD)
                .size(15.0)
                .decoration(DocumentTextDecoration.BOLD)
                .color(DocumentColor.rgb(126, 93, 52))      // ACCENT bronze
                .build();

        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.TIMES_ROMAN)
                .size(10.5)
                .color(DocumentColor.rgb(45, 43, 40))       // INK warm dark
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
