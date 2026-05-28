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
 * Templates v2 cover-letter pair for {@code NordicClean} CV preset.
 *
 * <p>Same INK / ACCENT palette and Barlow + Lato typography as the
 * {@link com.demcha.compose.document.templates.cv.presets.NordicClean}
 * CV. Single-column letter format — header on top, greeting, body
 * paragraphs, closing.</p>
 *
 * @deprecated Superseded by the layered <code>…v2…</code> surface (the current
 *             standard). Kept for backward compatibility; scheduled for removal
 *             in a future major. See {@code docs/templates/v2-layered/} and
 *             {@link com.demcha.compose.document.templates.coverletter.v2.presets.NordicCleanLetter}.
 */
@Deprecated(since = "1.7.0", forRemoval = true)
public final class NordicCleanLetter {

    /** Stable template identifier. */
    public static final String ID = "nordic-clean-letter";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Nordic Clean Letter";

    private static final DocumentColor INK = DocumentColor.rgb(18, 39, 52);
    private static final DocumentColor MUTED = DocumentColor.rgb(82, 104, 116);
    private static final DocumentColor ACCENT = DocumentColor.rgb(28, 128, 135);

    private NordicCleanLetter() {
    }

    /**
     * Builds a fresh cover-letter template paired with the
     * Nordic Clean CV style.
     *
     * @param theme active business theme
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CoverLetterSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.comfortable();

        DocumentTextStyle nameStyle = DocumentTextStyle.builder()
                .fontName(FontName.BARLOW)
                .size(24.0)
                .decoration(DocumentTextDecoration.BOLD)
                .color(INK)
                .build();
        DocumentTextStyle contactStyle = DocumentTextStyle.builder()
                .fontName(FontName.LATO)
                .size(8.5)
                .color(MUTED)
                .build();
        DocumentTextStyle linkStyle = DocumentTextStyle.builder()
                .fontName(FontName.LATO)
                .size(9.0)
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
