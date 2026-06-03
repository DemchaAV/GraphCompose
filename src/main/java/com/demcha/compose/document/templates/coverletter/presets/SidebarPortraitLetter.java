package com.demcha.compose.document.templates.coverletter.presets;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.components.Header;
import com.demcha.compose.document.templates.coverletter.builder.CoverLetterBuilder;
import com.demcha.compose.document.templates.coverletter.layouts.LetterFormat;
import com.demcha.compose.document.templates.coverletter.spec.CoverLetterSpec;
import com.demcha.compose.document.templates.themes.Spacing;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.font.FontName;

/**
 * Templates v2 cover-letter pair for {@code SidebarPortrait} CV preset.
 *
 * <p>Crimson Text serif headline + Lato body in the restrained grey
 * palette of
 * {@link com.demcha.compose.document.templates.cv.presets.SidebarPortrait}.
 * The cover letter is a simple single-column letter — the CV's
 * portrait sidebar is intentionally not replicated.</p>
 *
 * @deprecated Superseded by the layered <code>…v2…</code> surface (the current
 *             standard). Kept for backward compatibility; scheduled for removal
 *             in a future major. See {@code docs/templates/v2-layered/} and
 *             {@link com.demcha.compose.document.templates.coverletter.v2.presets.SidebarPortraitLetter}.
 */
@Deprecated(since = "1.7.0", forRemoval = true)
public final class SidebarPortraitLetter {

    /** Stable template identifier. */
    public static final String ID = "sidebar-portrait-letter";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Sidebar Portrait Letter";

    private static final DocumentColor INK = DocumentColor.rgb(34, 34, 34);
    private static final DocumentColor SOFT = DocumentColor.rgb(85, 85, 85);
    private static final DocumentColor ACCENT = DocumentColor.rgb(106, 106, 106);

    private SidebarPortraitLetter() {
    }

    /**
     * Builds the cover-letter template paired with the {@code SidebarPortrait} CV preset.
     *
     * @param theme the active theme supplying palette, typography, and spacing
     * @return a {@code DocumentTemplate} for the "Sidebar Portrait Letter"
     */
    public static DocumentTemplate<CoverLetterSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.comfortable();

        DocumentTextStyle nameStyle = DocumentTextStyle.builder()
                .fontName(FontName.CRIMSON_TEXT)
                .size(28.0)
                .decoration(DocumentTextDecoration.BOLD)
                .color(INK)
                .build();
        DocumentTextStyle contactStyle = DocumentTextStyle.builder()
                .fontName(FontName.LATO)
                .size(8.3)
                .color(SOFT)
                .build();
        DocumentTextStyle linkStyle = DocumentTextStyle.builder()
                .fontName(FontName.LATO)
                .size(8.3)
                .decoration(DocumentTextDecoration.UNDERLINE)
                .color(ACCENT)
                .build();
        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.LATO)
                .size(10.0)
                .color(INK)
                .build();

        return CoverLetterBuilder.builder()
                .id(ID).displayName(DISPLAY_NAME)
                .header(Header.rightAligned(theme, spacing)
                        .withNameStyle(nameStyle)
                        .withContactStyle(contactStyle)
                        .withLinkStyle(linkStyle))
                .layout(LetterFormat.layout().moduleGap(spacing.moduleGap()))
                .bodyStyle(bodyStyle).spacing(spacing).build();
    }
}
