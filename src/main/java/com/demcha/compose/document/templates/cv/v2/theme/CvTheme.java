package com.demcha.compose.document.templates.cv.v2.theme;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import java.util.Objects;

/**
 * Aggregate cosmetic theme — palette + typography + spacing — passed
 * to every component renderer in {@code cv/v2/components}.
 *
 * <p>This is the <strong>only</strong> place a CV preset reads colour,
 * font, size, or spacing values from. Renderers never inline literal
 * RGB tuples, font names, or magic numbers.</p>
 *
 * <p>To define a new visual flavour: add a static factory here
 * returning a fresh {@code CvTheme} with custom sub-records. The
 * existing preset code keeps working — only the theme handed to
 * {@code BoxedSections.create(theme)} changes.</p>
 *
 * @param palette    colour tokens
 * @param typography font + size scale
 * @param spacing    paddings / margins / weights
 */
public record CvTheme(CvPalette palette,
                      CvTypography typography,
                      CvSpacing spacing) {

    public CvTheme {
        Objects.requireNonNull(palette, "palette");
        Objects.requireNonNull(typography, "typography");
        Objects.requireNonNull(spacing, "spacing");
    }

    // -- canonical factories ---------------------------------------------

    /**
     * The "Boxed Sections" classic look — PT-Serif, near-black ink,
     * pale-grey section banners. Visual signature of the original
     * {@code cv-boxed-sections.pdf} reference output.
     */
    public static CvTheme boxedClassic() {
        return new CvTheme(
                CvPalette.classic(),
                CvTypography.classic(),
                CvSpacing.classic());
    }

    // -- pre-built text-style helpers ------------------------------------
    // Renderers ask the theme for an already-composed DocumentTextStyle
    // instead of re-assembling font + size + decoration + colour every
    // call site. This is the only "computed" code in the theme — every
    // value reads from the underlying records.

    public DocumentTextStyle headlineStyle() {
        return style(typography.headlineFont(), typography.sizeHeadline(),
                DocumentTextDecoration.DEFAULT, palette.ink());
    }

    public DocumentTextStyle bannerStyle() {
        return style(typography.headlineFont(), typography.sizeBanner(),
                DocumentTextDecoration.BOLD, palette.ink());
    }

    public DocumentTextStyle contactStyle() {
        return style(typography.bodyFont(), typography.sizeContact(),
                DocumentTextDecoration.DEFAULT, palette.ink());
    }

    public DocumentTextStyle contactSeparatorStyle() {
        return style(typography.bodyFont(), typography.sizeContact(),
                DocumentTextDecoration.DEFAULT, palette.rule());
    }

    public DocumentTextStyle bodyStyle() {
        return style(typography.bodyFont(), typography.sizeBody(),
                DocumentTextDecoration.DEFAULT, palette.ink());
    }

    public DocumentTextStyle bodyBoldStyle() {
        return style(typography.bodyFont(), typography.sizeBody(),
                DocumentTextDecoration.BOLD, palette.ink());
    }

    public DocumentTextStyle entryTitleStyle() {
        return style(typography.bodyFont(), typography.sizeEntryTitle(),
                DocumentTextDecoration.BOLD, palette.ink());
    }

    public DocumentTextStyle entryDateStyle() {
        return style(typography.bodyFont(), typography.sizeEntryDate(),
                DocumentTextDecoration.DEFAULT, palette.ink());
    }

    public DocumentTextStyle entrySubtitleStyle() {
        return style(typography.bodyFont(), typography.sizeEntrySubtitle(),
                DocumentTextDecoration.ITALIC, palette.muted());
    }

    private static DocumentTextStyle style(FontName font, double size,
                                           DocumentTextDecoration decoration,
                                           DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(font)
                .size(size)
                .decoration(decoration)
                .color(color)
                .build();
    }
}
