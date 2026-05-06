package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.components.Header;
import com.demcha.compose.document.templates.components.Module;
import com.demcha.compose.document.templates.cv.builder.CvBuilder;
import com.demcha.compose.document.templates.cv.layouts.SingleColumn;
import com.demcha.compose.document.templates.cv.spec.CvSpec;
import com.demcha.compose.document.templates.themes.Spacing;
import com.demcha.compose.document.theme.BusinessTheme;

/**
 * Templates v2 "Modern Professional" CV preset.
 *
 * <p>Single-column layout with right-aligned header — the canonical
 * starting-point preset for Templates v2. Uses {@link SingleColumn}
 * layout, {@link Header#rightAligned} header style, and
 * {@link Module#headingFlat} module style, all backed by
 * {@link Spacing#compact}.</p>
 *
 * <p>To build a custom preset, copy the body of
 * {@link #create(BusinessTheme)} into your own class and tweak the
 * builder calls — slot placement, module style, spacing tokens, or
 * which layout / header style is used. No subclassing is required.</p>
 *
 * <p>The preset declares six standard slot placements in
 * {@link SingleColumn#MAIN}: {@code "Professional Summary"},
 * {@code "Technical Skills"}, {@code "Education & Certifications"},
 * {@code "Projects"}, {@code "Professional Experience"}, and
 * {@code "Additional Information"}. The accompanying
 * {@link CvSpec} must declare modules with these names; alternative
 * orderings are achieved by copying the preset and changing the
 * {@code .place(...)} calls.</p>
 */
public final class ModernProfessional {

    /** Stable template identifier. */
    public static final String ID = "modern-professional";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Modern Professional";

    private ModernProfessional() {
        // utility class — not instantiable
    }

    /**
     * Builds a fresh {@code Modern Professional} template configured
     * for the given business theme.
     *
     * @param theme active business theme (palette + typography)
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.compact();

        return CvBuilder.builder()
                .id(ID)
                .displayName(DISPLAY_NAME)
                .theme(theme)
                .spacing(spacing)
                .layout(SingleColumn.layout()
                        .moduleGap(spacing.moduleGap()))
                .header(Header.rightAligned(theme, spacing))
                .moduleStyle(Module.headingFlat(theme)
                        .marginAbove(spacing.sectionTitleAbove())
                        .marginBelow(spacing.sectionTitleBelow()))
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
