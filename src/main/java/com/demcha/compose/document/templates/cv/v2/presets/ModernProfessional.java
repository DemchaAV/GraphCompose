package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.components.SectionDispatcher;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.data.CvSection;
import com.demcha.compose.document.templates.cv.v2.data.Slot;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.ContactLine;
import com.demcha.compose.document.templates.cv.v2.widgets.Headline;
import com.demcha.compose.document.templates.cv.v2.widgets.SectionHeader;
import com.demcha.compose.font.FontName;

import java.util.List;
import java.util.Objects;

/**
 * v2 port of the canonical "Modern Professional" CV preset.
 *
 * <p>Visual signature ported from the legacy v1 preset:</p>
 * <ul>
 *   <li>Right-aligned <strong>big slate-blue display name</strong>
 *       (no spaced caps, no centring) at the top.</li>
 *   <li>Right-aligned pipe-separated contact + link row beneath.</li>
 *   <li>Large <strong>bright-blue bold section titles</strong> —
 *       flat, left-aligned, no banner panel.</li>
 *   <li>Body text in Helvetica 10pt — denser than Boxed Sections.</li>
 *   <li>Single-page-friendly proportions on A4 with 18pt margins.</li>
 * </ul>
 *
 * <p><strong>Why some colours live inside this preset and not in
 * {@link CvTheme}:</strong> the slate-blue display name and the
 * bright-blue accent for section titles are unique to this preset —
 * no other v2 preset shares them today. Putting them in
 * {@link com.demcha.compose.document.templates.cv.v2.theme.CvPalette}
 * would pollute the palette with single-use fields. When (or if) a
 * second preset reaches for the same colours, extract them to
 * {@code CvPalette} and update both presets.</p>
 *
 * <p><strong>Architectural lesson learned in Phase 2:</strong>
 * single-column presets that don't fit the boxed-banner visual
 * (e.g. flat titles, underlined titles, coloured titles) currently
 * inline their own {@code renderSectionTitle} helper. Once 3+ presets
 * share this need, factor out a {@code SectionTitleRenderer}
 * component with style variants. Until then, the per-preset inline
 * helper keeps each preset readable end-to-end.</p>
 */
public final class ModernProfessional {

    /** Stable template identifier. */
    public static final String ID = "modern-professional";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Modern Professional";

    /** Recommended page margin (in points) — matches the legacy v1 preset. */
    public static final double RECOMMENDED_MARGIN = 18.0;

    /** Slate-blue used by the display name. Preset-specific. */
    private static final DocumentColor NAME_COLOR = DocumentColor.rgb(44, 62, 80);

    /** Bright-blue used by section titles. Preset-specific. */
    private static final DocumentColor SECTION_TITLE_COLOR =
            DocumentColor.rgb(41, 128, 185);

    /** Royal-blue used by contact links. Preset-specific. */
    private static final DocumentColor LINK_COLOR = DocumentColor.rgb(65, 105, 225);

    private ModernProfessional() {
    }

    /**
     * Builds the preset with the Modern Professional theme
     * ({@link CvTheme#modernProfessional()}).
     *
     * @return ready-to-use template
     */
    public static DocumentTemplate<CvDocument> create() {
        return create(CvTheme.modernProfessional());
    }

    /**
     * Builds the preset with a caller-supplied theme. Allows
     * variations on the Modern Professional theme (different
     * typography scale, custom spacing) without forking this class.
     *
     * @param theme active theme
     * @return ready-to-use template
     */
    public static DocumentTemplate<CvDocument> create(CvTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new Template(theme);
    }

    private static final class Template implements DocumentTemplate<CvDocument> {

        private final CvTheme theme;

        Template(CvTheme theme) {
            this.theme = theme;
        }

        @Override
        public String id() {
            return ID;
        }

        @Override
        public String displayName() {
            return DISPLAY_NAME;
        }

        @Override
        public void compose(DocumentSession document, CvDocument doc) {
            Objects.requireNonNull(document, "document");
            Objects.requireNonNull(doc, "doc");

            // Preset-specific styles — built once, fed to widgets.
            // These cannot live in the theme because their colours are
            // unique to this preset (no other preset uses slate-blue
            // for the name or royal-blue for links).
            DocumentTextStyle nameStyle = DocumentTextStyle.builder()
                    .fontName(FontName.HELVETICA_BOLD)
                    .size(theme.typography().sizeHeadline())
                    .decoration(DocumentTextDecoration.BOLD)
                    .color(NAME_COLOR)
                    .build();
            DocumentTextStyle contactBodyStyle = DocumentTextStyle.builder()
                    .fontName(FontName.HELVETICA)
                    .size(theme.typography().sizeContact())
                    .color(theme.palette().ink())
                    .build();
            DocumentTextStyle contactLinkStyle = DocumentTextStyle.builder()
                    .fontName(FontName.HELVETICA)
                    .size(theme.typography().sizeContact())
                    .decoration(DocumentTextDecoration.UNDERLINE)
                    .color(LINK_COLOR)
                    .build();
            DocumentTextStyle contactSeparatorStyle = DocumentTextStyle.builder()
                    .fontName(FontName.HELVETICA)
                    .size(theme.typography().sizeContact())
                    .color(theme.palette().rule())
                    .build();

            PageFlowBuilder pageFlow = document.dsl()
                    .pageFlow()
                    .name("CvV2ModernRoot")
                    .spacing(theme.spacing().pageFlowSpacing())
                    .addSection("Header", section ->
                            Headline.rightAligned(section, doc.identity().name(),
                                    theme, nameStyle))
                    .addSection("Contact", section -> {
                        section.accentBottom(theme.palette().rule(),
                                theme.spacing().accentRuleWidth());
                        ContactLine.twoRowRightAligned(section, doc.identity(),
                                theme, contactBodyStyle, contactLinkStyle,
                                contactSeparatorStyle);
                    });

            // Single-column preset — only MAIN slot.
            List<CvSection> sections = doc.sectionsIn(Slot.MAIN);
            for (int i = 0; i < sections.size(); i++) {
                final CvSection sec = sections.get(i);
                final int idx = i;
                pageFlow.addSection("Title_" + idx, host ->
                        SectionHeader.flat(host, sec.title(), SECTION_TITLE_COLOR, theme));
                pageFlow.addSection("Body_" + idx, host ->
                        SectionDispatcher.renderBody(host, sec, theme));
            }

            pageFlow.build();
        }
    }
}
