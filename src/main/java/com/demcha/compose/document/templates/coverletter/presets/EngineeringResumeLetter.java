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
 * Templates v2 cover-letter pair for {@code EngineeringResume} CV preset.
 *
 * <p>Navy primary, green accent, Barlow headline + Lato body — matches
 * {@link com.demcha.compose.document.templates.cv.presets.EngineeringResume}.</p>
 */
public final class EngineeringResumeLetter {

    /** Stable template identifier. */
    public static final String ID = "engineering-resume-letter";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Engineering Resume Letter";

    private static final DocumentColor NAVY = DocumentColor.rgb(13, 32, 47);
    private static final DocumentColor INK = DocumentColor.rgb(32, 42, 55);
    private static final DocumentColor MUTED = DocumentColor.rgb(91, 105, 119);
    private static final DocumentColor GREEN = DocumentColor.rgb(27, 145, 104);

    private EngineeringResumeLetter() {
    }

    public static DocumentTemplate<CoverLetterSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.compact();

        DocumentTextStyle nameStyle = DocumentTextStyle.builder()
                .fontName(FontName.BARLOW)
                .size(24.0)
                .decoration(DocumentTextDecoration.BOLD)
                .color(NAVY)
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
                .color(GREEN)
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
