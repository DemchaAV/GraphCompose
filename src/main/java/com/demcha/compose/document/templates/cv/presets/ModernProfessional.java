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
 *
 * @deprecated Superseded by the layered <code>…v2…</code> surface (the current
 *             standard). Kept for backward compatibility; scheduled for removal
 *             in a future major. See {@code docs/templates/v2-layered/} and
 *             {@link com.demcha.compose.document.templates.cv.v2.presets.ModernProfessional}.
 */
@Deprecated(since = "1.7.0", forRemoval = true)
public final class ModernProfessional {

    /** Stable template identifier. */
    public static final String ID = "modern-professional";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Modern Professional";

    /**
     * Recommended page margin (in points) for this preset. Matches the
     * legacy {@code CvFileExample} margin so the v2 render keeps the
     * V1 visual proportions on A4.
     */
    public static final double RECOMMENDED_MARGIN = 18.0;

    /** V1 {@code CvTheme} primary slate-blue used by the display name. */
    private static final DocumentColor V1_NAME_COLOR = DocumentColor.rgb(44, 62, 80);

    /** V1 {@code CvTheme} secondary bright-blue used by section headings. */
    private static final DocumentColor V1_SECTION_COLOR = DocumentColor.rgb(41, 128, 185);

    /** V1 link accent (royal blue) used by the contact link row. */
    private static final DocumentColor V1_LINK_COLOR = DocumentColor.rgb(65, 105, 225);

    private ModernProfessional() {
        // utility class — not instantiable
    }

    /**
     * Builds a fresh {@code Modern Professional} template configured
     * for the given business theme.
     *
     * <p>Visual signature mirrors the legacy {@code CvTemplateV1}:
     * slate-blue display name, bright-blue section headings, and
     * royal-blue underlined contact links. Overrides the active theme's
     * text scale here so the preset reads the same on any
     * {@link BusinessTheme} variant.</p>
     *
     * @param theme active business theme (palette + typography)
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.compact();

        DocumentTextStyle nameStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(28.0)
                .decoration(DocumentTextDecoration.BOLD)
                .color(V1_NAME_COLOR)
                .build();

        DocumentTextStyle contactStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(9.0)
                .color(theme.text().body().color())
                .build();

        DocumentTextStyle linkStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(10.0)
                .decoration(DocumentTextDecoration.UNDERLINE)
                .color(V1_LINK_COLOR)
                .build();

        DocumentTextStyle headingStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(17.4)
                .decoration(DocumentTextDecoration.BOLD)
                .color(V1_SECTION_COLOR)
                .build();

        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(10.0)
                .color(theme.text().body().color())
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
                .header(Header.rightAligned(theme, spacing)
                        .withNameStyle(nameStyle)
                        .withContactStyle(contactStyle)
                        .withLinkStyle(linkStyle))
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
