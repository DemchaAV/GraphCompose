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
 * Templates v2 cover-letter pair for {@code CompactMono} CV preset.
 *
 * <p>IBM Plex Mono headline + Lato body, dark INK ink with the
 * teal-blue ACCENT used by
 * {@link com.demcha.compose.document.templates.cv.presets.CompactMono}.</p>
 */
public final class CompactMonoLetter {

    /** Stable template identifier. */
    public static final String ID = "compact-mono-letter";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Compact Mono Letter";

    private static final DocumentColor INK = DocumentColor.rgb(28, 34, 42);
    private static final DocumentColor MUTED = DocumentColor.rgb(84, 96, 112);
    private static final DocumentColor ACCENT = DocumentColor.rgb(0, 126, 151);

    private CompactMonoLetter() {
    }

    public static DocumentTemplate<CoverLetterSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.compact();

        DocumentTextStyle nameStyle = DocumentTextStyle.builder()
                .fontName(FontName.IBM_PLEX_MONO)
                .size(20.0)
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
