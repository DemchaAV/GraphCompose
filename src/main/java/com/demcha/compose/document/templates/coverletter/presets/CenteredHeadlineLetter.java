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
 * Templates v2 cover-letter pair for {@code CenteredHeadline} CV preset.
 * Helvetica body in medium-dark grey with comfortable spacing.
 */
public final class CenteredHeadlineLetter {

    /** Stable template identifier. */
    public static final String ID = "centered-headline-letter";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Centered Headline Letter";

    private CenteredHeadlineLetter() {
    }

    /**
     * Builds a fresh cover-letter template paired with the
     * Centered Headline CV style.
     *
     * @param theme active business theme
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CoverLetterSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.comfortable();
        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(10.0)
                .color(DocumentColor.rgb(54, 54, 54))
                .build();
        return CoverLetterBuilder.builder()
                .id(ID).displayName(DISPLAY_NAME)
                .header(Header.rightAligned(theme, spacing))
                .layout(LetterFormat.layout().moduleGap(spacing.moduleGap()))
                .bodyStyle(bodyStyle).spacing(spacing).build();
    }
}
