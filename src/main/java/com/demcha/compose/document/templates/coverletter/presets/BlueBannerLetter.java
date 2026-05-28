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
 * Templates v2 cover-letter pair for {@code BlueBanner} CV preset.
 *
 * <p>PT Serif headline + Lato body, dark INK with the soft-blue
 * BANNER_BG used by
 * {@link com.demcha.compose.document.templates.cv.presets.BlueBanner}
 * for accent runs.</p>
 *
 * @deprecated Superseded by the layered <code>…v2…</code> surface (the current
 *             standard). Kept for backward compatibility; scheduled for removal
 *             in a future major. See {@code docs/templates/v2-layered/} and
 *             {@link com.demcha.compose.document.templates.coverletter.v2.presets.BlueBannerLetter}.
 */
@Deprecated(since = "1.7.0", forRemoval = true)
public final class BlueBannerLetter {

    /** Stable template identifier. */
    public static final String ID = "blue-banner-letter";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Blue Banner Letter";

    private static final DocumentColor INK = DocumentColor.rgb(20, 25, 35);
    private static final DocumentColor SOFT = DocumentColor.rgb(85, 85, 85);
    private static final DocumentColor BANNER_BG = DocumentColor.rgb(112, 146, 190);

    private BlueBannerLetter() {
    }

    public static DocumentTemplate<CoverLetterSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.comfortable();

        DocumentTextStyle nameStyle = DocumentTextStyle.builder()
                .fontName(FontName.PT_SERIF)
                .size(22.0)
                .decoration(DocumentTextDecoration.DEFAULT)
                .color(INK)
                .build();
        DocumentTextStyle contactStyle = DocumentTextStyle.builder()
                .fontName(FontName.LATO)
                .size(7.5)
                .color(SOFT)
                .build();
        DocumentTextStyle linkStyle = DocumentTextStyle.builder()
                .fontName(FontName.LATO)
                .size(8.0)
                .decoration(DocumentTextDecoration.UNDERLINE)
                .color(BANNER_BG)
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
