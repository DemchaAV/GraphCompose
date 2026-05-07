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
 * Templates v2 "Modern Professional Letter" cover-letter preset.
 *
 * <p>Visual pair of
 * {@link com.demcha.compose.document.templates.cv.presets.ModernProfessional}.
 * Same right-aligned header (slate-blue name, royal-blue underlined
 * contact links), same Helvetica body type. The cover letter itself
 * is a single-column letter — header on top, greeting, body
 * paragraphs separated by paragraph spacing, closing.</p>
 */
public final class ModernProfessionalLetter {

    /** Stable template identifier. */
    public static final String ID = "modern-professional-letter";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Modern Professional Letter";

    /** V1 {@code CvTheme} primary slate-blue used by the display name. */
    private static final DocumentColor NAME_COLOR = DocumentColor.rgb(44, 62, 80);

    /** V1 link accent (royal blue) used by the contact link row. */
    private static final DocumentColor LINK_COLOR = DocumentColor.rgb(65, 105, 225);

    private ModernProfessionalLetter() {
    }

    /**
     * Builds a fresh cover-letter template paired with the
     * Modern Professional CV style.
     *
     * @param theme active business theme
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CoverLetterSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.compact();

        DocumentTextStyle nameStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(28.0)
                .decoration(DocumentTextDecoration.BOLD)
                .color(NAME_COLOR)
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
                .color(LINK_COLOR)
                .build();

        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(10.0)
                .color(theme.text().body().color())
                .build();

        return CoverLetterBuilder.builder()
                .id(ID)
                .displayName(DISPLAY_NAME)
                .header(Header.rightAligned(theme, spacing)
                        .withNameStyle(nameStyle)
                        .withContactStyle(contactStyle)
                        .withLinkStyle(linkStyle))
                .layout(LetterFormat.layout()
                        .moduleGap(spacing.moduleGap()))
                .bodyStyle(bodyStyle)
                .spacing(spacing)
                .build();
    }
}
