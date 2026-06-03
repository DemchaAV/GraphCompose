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
 * Templates v2 cover-letter pair for {@code EditorialBlue} CV preset.
 *
 * <p>Helvetica throughout, deep navy ink with bright editorial blue
 * accent — matches
 * {@link com.demcha.compose.document.templates.cv.presets.EditorialBlue}.</p>
 *
 * @deprecated Superseded by the layered <code>…v2…</code> surface (the current
 *             standard). Kept for backward compatibility; scheduled for removal
 *             in a future major. See {@code docs/templates/v2-layered/} and
 *             {@link com.demcha.compose.document.templates.coverletter.v2.presets.EditorialBlueLetter}.
 */
@Deprecated(since = "1.7.0", forRemoval = true)
public final class EditorialBlueLetter {

    /** Stable template identifier. */
    public static final String ID = "editorial-blue-letter";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Editorial Blue Letter";

    private static final DocumentColor INK = DocumentColor.rgb(18, 31, 72);
    private static final DocumentColor BODY = DocumentColor.rgb(60, 72, 106);
    private static final DocumentColor ACCENT = DocumentColor.rgb(86, 136, 255);

    private EditorialBlueLetter() {
    }

    /**
     * Builds the cover-letter template paired with the {@code EditorialBlue} CV preset.
     *
     * @param theme the active theme supplying palette, typography, and spacing
     * @return a {@code DocumentTemplate} for the "Editorial Blue Letter"
     */
    public static DocumentTemplate<CoverLetterSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.comfortable();

        DocumentTextStyle nameStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(22.0)
                .decoration(DocumentTextDecoration.BOLD)
                .color(INK)
                .build();
        DocumentTextStyle contactStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(9.0)
                .color(BODY)
                .build();
        DocumentTextStyle linkStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(9.0)
                .decoration(DocumentTextDecoration.UNDERLINE)
                .color(ACCENT)
                .build();
        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(10.0)
                .color(BODY)
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
