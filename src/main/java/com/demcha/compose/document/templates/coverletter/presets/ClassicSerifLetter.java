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
 * Templates v2 cover-letter pair for {@code ClassicSerif} CV preset.
 *
 * <p>PT Serif throughout with the bronze accent and warm INK palette
 * of {@link com.demcha.compose.document.templates.cv.presets.ClassicSerif}.</p>
 */
public final class ClassicSerifLetter {

    /** Stable template identifier. */
    public static final String ID = "classic-serif-letter";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Classic Serif Letter";

    private static final DocumentColor INK = DocumentColor.rgb(45, 43, 40);
    private static final DocumentColor MUTED = DocumentColor.rgb(105, 101, 94);
    private static final DocumentColor ACCENT = DocumentColor.rgb(126, 93, 52);

    private ClassicSerifLetter() {
    }

    public static DocumentTemplate<CoverLetterSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.comfortable();

        DocumentTextStyle nameStyle = DocumentTextStyle.builder()
                .fontName(FontName.PT_SERIF)
                .size(24.0)
                .decoration(DocumentTextDecoration.DEFAULT)
                .color(INK)
                .build();
        DocumentTextStyle contactStyle = DocumentTextStyle.builder()
                .fontName(FontName.PT_SERIF)
                .size(8.7)
                .color(MUTED)
                .build();
        DocumentTextStyle linkStyle = DocumentTextStyle.builder()
                .fontName(FontName.PT_SERIF)
                .size(8.7)
                .decoration(DocumentTextDecoration.UNDERLINE)
                .color(ACCENT)
                .build();
        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.PT_SERIF)
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
