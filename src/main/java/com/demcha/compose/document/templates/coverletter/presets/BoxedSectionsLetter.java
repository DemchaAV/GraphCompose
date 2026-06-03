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
 * Templates v2 cover-letter pair for {@code BoxedSections} CV preset.
 *
 * <p>PT Serif throughout, dark grey ink — matches
 * {@link com.demcha.compose.document.templates.cv.presets.BoxedSections}.</p>
 *
 * @deprecated Superseded by the layered <code>…v2…</code> surface (the current
 *             standard). Kept for backward compatibility; scheduled for removal
 *             in a future major. See {@code docs/templates/v2-layered/} and
 *             {@link com.demcha.compose.document.templates.coverletter.v2.presets.BoxedSectionsLetter}.
 */
@Deprecated(since = "1.7.0", forRemoval = true)
public final class BoxedSectionsLetter {

    /** Stable template identifier. */
    public static final String ID = "boxed-sections-letter";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Boxed Sections Letter";

    private static final DocumentColor INK = DocumentColor.rgb(34, 34, 34);
    private static final DocumentColor MUTED = DocumentColor.rgb(120, 120, 120);
    private static final DocumentColor RULE = DocumentColor.rgb(170, 170, 170);

    private BoxedSectionsLetter() {
    }

    /**
     * Builds the cover-letter template paired with the {@code BoxedSections} CV preset.
     *
     * @param theme the active theme supplying palette, typography, and spacing
     * @return a {@code DocumentTemplate} for the "Boxed Sections Letter"
     */
    public static DocumentTemplate<CoverLetterSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.comfortable();

        DocumentTextStyle nameStyle = DocumentTextStyle.builder()
                .fontName(FontName.PT_SERIF)
                .size(22.0)
                .decoration(DocumentTextDecoration.DEFAULT)
                .color(INK)
                .build();
        DocumentTextStyle contactStyle = DocumentTextStyle.builder()
                .fontName(FontName.PT_SERIF)
                .size(8.5)
                .color(MUTED)
                .build();
        DocumentTextStyle linkStyle = DocumentTextStyle.builder()
                .fontName(FontName.PT_SERIF)
                .size(8.5)
                .decoration(DocumentTextDecoration.UNDERLINE)
                .color(RULE)
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
