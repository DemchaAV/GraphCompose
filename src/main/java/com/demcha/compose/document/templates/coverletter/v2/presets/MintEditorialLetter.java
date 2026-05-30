package com.demcha.compose.document.templates.coverletter.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.v2.components.LetterBody;
import com.demcha.compose.document.templates.coverletter.v2.data.CoverLetterDocument;
import com.demcha.compose.document.templates.cv.v2.components.CvTextStyles;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.Headline;
import com.demcha.compose.document.templates.cv.v2.widgets.Subheadline;

import java.util.Objects;

/**
 * v2 cover-letter pair for the {@code MintEditorial} CV preset.
 *
 * <p>Reproduces the CV's signature masthead 1:1 — a centred spaced-caps
 * Poppins name, a centred soft-mint accent tagline, and a full-width 6pt
 * mint accent rule — then a single-column letter body via the shared
 * {@link LetterBody}. Palette / typography / spacing come from the
 * <strong>same</strong> {@link CvTheme#mintEditorial()} the CV uses, and the
 * mint accent is read from {@code theme.palette().banner()} exactly as in the
 * CV, so the CV and the letter read as one matched set. The CV's two-column
 * sidebar grids are a CV-body concern and are intentionally not part of the
 * letter.</p>
 */
public final class MintEditorialLetter {

    /** Stable template identifier. */
    public static final String ID = "mint-editorial-letter";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Mint Editorial Letter";

    /** Recommended symmetric page margin (in points). Matches the CV preset. */
    public static final double RECOMMENDED_MARGIN = 48.0;

    private MintEditorialLetter() {
    }

    /** Builds the letter with its Mint Editorial theme. */
    public static DocumentTemplate<CoverLetterDocument> create() {
        return create(CvTheme.mintEditorial());
    }

    /**
     * Builds the letter with a caller-supplied theme (share the paired CV's
     * theme instance for a guaranteed visual match).
     */
    public static DocumentTemplate<CoverLetterDocument> create(CvTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new Template(theme);
    }

    private static final class Template implements DocumentTemplate<CoverLetterDocument> {

        private final CvTheme theme;
        private final DocumentColor accent;

        Template(CvTheme theme) {
            this.theme = theme;
            // Same accent source as the paired CV preset — the palette
            // banner slot carries the mint accent.
            this.accent = theme.palette().banner();
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
        public void compose(DocumentSession document, CoverLetterDocument doc) {
            Objects.requireNonNull(document, "document");
            Objects.requireNonNull(doc, "doc");

            double innerWidth = document.canvas().innerWidth();
            double pageWidth = document.canvas().width();
            // Full-page-width masthead rule (see MintEditorial): bleed past
            // the page margins so the mint rule spans the whole page.
            double ruleBleed = (pageWidth - innerWidth) / 2.0;
            PageFlowBuilder flow = document.dsl()
                    .pageFlow()
                    .name("CoverLetterV2MintEditorialRoot")
                    .spacing(theme.spacing().pageFlowSpacing());

            flow.addSection("CoverLetterV2MintEditorialHeader",
                    section -> addMasthead(section, doc.identity()));
            flow.addLine(line -> line
                    .name("CoverLetterV2MintEditorialHeaderRule")
                    .horizontal(pageWidth)
                    .color(accent)
                    .thickness(theme.spacing().accentRuleWidth())
                    .margin(new DocumentInsets(8, -ruleBleed, 14, -ruleBleed)));

            flow.addSection("CoverLetterV2MintEditorialBody", host ->
                    LetterBody.render(host, doc, theme));

            flow.build();
        }

        /**
         * Centred spaced-caps name + mint accent tagline — the identical
         * masthead the {@code MintEditorial} CV preset renders, so the
         * matched set never forks the header treatment.
         */
        private void addMasthead(SectionBuilder section, CvIdentity identity) {
            Headline.spacedCentered(section, identity.name(), theme);
            String jobTitle = identity.jobTitle();
            if (jobTitle != null && !jobTitle.isBlank()) {
                Subheadline.centeredSpacedCaps(section, jobTitle, taglineStyle());
            }
        }

        private DocumentTextStyle taglineStyle() {
            return CvTextStyles.of(theme.typography().headlineFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.BOLD, accent);
        }
    }
}
