package com.demcha.compose.document.templates.coverletter.presets;

import com.demcha.compose.document.style.DocumentColor;
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
 * Same right-aligned header, same body typography (Helvetica 10 pt,
 * V1 secondary blue), same compact spacing rhythm. The cover letter
 * itself is a single-column letter format — header on top, greeting,
 * body paragraphs separated by paragraph spacing, closing.</p>
 *
 * <p>To customise: copy the body of {@link #create(BusinessTheme)}
 * into your own class and tweak any of the styles or spacing tokens.</p>
 */
public final class ModernProfessionalLetter {

    /** Stable template identifier. */
    public static final String ID = "modern-professional-letter";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Modern Professional Letter";

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

        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(10.0)
                .color(DocumentColor.rgb(44, 62, 80))       // V1 primary slate
                .build();

        return CoverLetterBuilder.builder()
                .id(ID)
                .displayName(DISPLAY_NAME)
                .header(Header.rightAligned(theme, spacing))
                .layout(LetterFormat.layout()
                        .moduleGap(spacing.moduleGap()))
                .bodyStyle(bodyStyle)
                .spacing(spacing)
                .build();
    }
}
