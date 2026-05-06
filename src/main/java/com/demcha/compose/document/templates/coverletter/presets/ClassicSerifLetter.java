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
 * Templates v2 cover-letter pair for {@code ClassicSerif} CV preset.
 * Times Roman serif body in warm dark ink with comfortable spacing.
 */
public final class ClassicSerifLetter {

    /** Stable template identifier. */
    public static final String ID = "classic-serif-letter";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Classic Serif Letter";

    private ClassicSerifLetter() {
    }

    /**
     * Builds a fresh cover-letter template paired with the
     * Classic Serif CV style.
     *
     * @param theme active business theme
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CoverLetterSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.comfortable();
        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.TIMES_ROMAN)
                .size(10.5)
                .color(DocumentColor.rgb(45, 43, 40))
                .build();
        return CoverLetterBuilder.builder()
                .id(ID).displayName(DISPLAY_NAME)
                .header(Header.rightAligned(theme, spacing))
                .layout(LetterFormat.layout().moduleGap(spacing.moduleGap()))
                .bodyStyle(bodyStyle).spacing(spacing).build();
    }
}
