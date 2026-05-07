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
 * Templates v2 cover-letter pair for {@code MonogramSidebar} CV preset.
 *
 * <p>Crimson Text headline + Lato body, deep slate ink with muted gold
 * accent — matches
 * {@link com.demcha.compose.document.templates.cv.presets.MonogramSidebar}.
 * The cover letter is a simple single-column letter — the CV's
 * monogram sidebar is intentionally not replicated.</p>
 */
public final class MonogramSidebarLetter {

    /** Stable template identifier. */
    public static final String ID = "monogram-sidebar-letter";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Monogram Sidebar Letter";

    private static final DocumentColor INK = DocumentColor.rgb(37, 45, 58);
    private static final DocumentColor SOFT = DocumentColor.rgb(112, 119, 125);
    private static final DocumentColor ACCENT = DocumentColor.rgb(158, 146, 104);

    private MonogramSidebarLetter() {
    }

    public static DocumentTemplate<CoverLetterSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.comfortable();

        DocumentTextStyle nameStyle = DocumentTextStyle.builder()
                .fontName(FontName.CRIMSON_TEXT)
                .size(26.0)
                .decoration(DocumentTextDecoration.DEFAULT)
                .color(INK)
                .build();
        DocumentTextStyle contactStyle = DocumentTextStyle.builder()
                .fontName(FontName.LATO)
                .size(7.4)
                .color(SOFT)
                .build();
        DocumentTextStyle linkStyle = DocumentTextStyle.builder()
                .fontName(FontName.LATO)
                .size(7.4)
                .decoration(DocumentTextDecoration.UNDERLINE)
                .color(ACCENT)
                .build();
        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.LATO)
                .size(9.5)
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
