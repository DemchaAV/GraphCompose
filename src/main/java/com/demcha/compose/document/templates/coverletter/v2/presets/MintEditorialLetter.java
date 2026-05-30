package com.demcha.compose.document.templates.coverletter.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.v2.components.LetterBody;
import com.demcha.compose.document.templates.coverletter.v2.data.CoverLetterDocument;
import com.demcha.compose.document.templates.cv.v2.components.CvTextStyles;
import com.demcha.compose.document.templates.cv.v2.components.TextOrnaments;
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
 *
 * <p>The same {@link Options} colour knobs as the paired
 * {@link com.demcha.compose.document.templates.cv.v2.presets.MintEditorial}
 * preset recolour the letter masthead (accent, rule, name, optional header
 * band), with identical defaults so the matched set stays in sync.</p>
 */
public final class MintEditorialLetter {

    /** Stable template identifier. */
    public static final String ID = "mint-editorial-letter";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Mint Editorial Letter";

    /** Recommended symmetric page margin (in points). Matches the CV preset. */
    public static final double RECOMMENDED_MARGIN = 48.0;

    // Banded-masthead canvas geometry — mirrors the CV preset so the matched
    // set's masthead is identical, and reproduces the letter's own DEFAULT
    // (bandless) masthead positions so the banded render only adds a fill
    // behind an otherwise-unchanged masthead.

    /** Canvas flow footprint (points) — matches the CV masthead footprint. */
    private static final double MASTHEAD_CANVAS_HEIGHT = 143.76;

    /** Canvas-local y (points) of the masthead name — matches the default top. */
    private static final double MASTHEAD_NAME_Y = 48.0;

    /** Canvas-local y (points) of the masthead tagline — matches the default top. */
    private static final double MASTHEAD_TAGLINE_Y = 87.4;

    /** Canvas-local y (points) of the masthead rule — matches the default top. */
    private static final double MASTHEAD_RULE_Y = 123.76;

    private MintEditorialLetter() {
    }

    /** Builds the letter with its Mint Editorial theme and default colours. */
    public static DocumentTemplate<CoverLetterDocument> create() {
        return create(CvTheme.mintEditorial(), Options.defaults());
    }

    /**
     * Builds the letter with a caller-supplied theme and default colours
     * (share the paired CV's theme instance for a guaranteed visual match).
     */
    public static DocumentTemplate<CoverLetterDocument> create(CvTheme theme) {
        return create(theme, Options.defaults());
    }

    /** Builds the letter with its Mint Editorial theme and explicit colours. */
    public static DocumentTemplate<CoverLetterDocument> create(Options options) {
        return create(CvTheme.mintEditorial(), options);
    }

    /**
     * Builds the letter with a caller-supplied theme and explicit colour
     * {@link Options}. Pass the same {@code Options} as the paired CV preset
     * so the recoloured masthead matches.
     */
    public static DocumentTemplate<CoverLetterDocument> create(CvTheme theme,
                                                               Options options) {
        Objects.requireNonNull(theme, "theme");
        Objects.requireNonNull(options, "options");
        return new Template(theme, options);
    }

    /**
     * Mint Editorial letter masthead colour knobs — same shape and defaults as
     * {@code MintEditorial.Options}. Every {@code null} field reproduces the
     * stock render.
     *
     * @param accentColor     mint accent for the tagline; {@code null} →
     *                        {@code theme.palette().banner()}
     * @param ruleColor       masthead rule colour; {@code null} → the resolved
     *                        {@code accentColor}
     * @param nameColor       masthead name colour; {@code null} →
     *                        {@code theme.palette().ink()}
     * @param headerBandColor optional full-width band behind the masthead;
     *                        {@code null} → no band
     */
    public record Options(DocumentColor accentColor,
                          DocumentColor ruleColor,
                          DocumentColor nameColor,
                          DocumentColor headerBandColor) {

        public static Options defaults() {
            return new Options(null, null, null, null);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private DocumentColor accentColor;
            private DocumentColor ruleColor;
            private DocumentColor nameColor;
            private DocumentColor headerBandColor;

            private Builder() {
            }

            public Builder accentColor(DocumentColor value) {
                this.accentColor = value;
                return this;
            }

            public Builder ruleColor(DocumentColor value) {
                this.ruleColor = value;
                return this;
            }

            public Builder nameColor(DocumentColor value) {
                this.nameColor = value;
                return this;
            }

            public Builder headerBandColor(DocumentColor value) {
                this.headerBandColor = value;
                return this;
            }

            public Options build() {
                return new Options(accentColor, ruleColor, nameColor,
                        headerBandColor);
            }
        }
    }

    private static final class Template implements DocumentTemplate<CoverLetterDocument> {

        private final CvTheme theme;
        private final DocumentColor accent;
        private final DocumentColor ruleColor;
        private final DocumentColor nameColor;
        private final DocumentColor headerBandColor;

        Template(CvTheme theme, Options options) {
            this.theme = theme;
            // Same accent source + Options defaults as the paired CV preset.
            this.accent = options.accentColor() != null
                    ? options.accentColor()
                    : theme.palette().banner();
            this.ruleColor = options.ruleColor() != null
                    ? options.ruleColor()
                    : this.accent;
            this.nameColor = options.nameColor() != null
                    ? options.nameColor()
                    : theme.palette().ink();
            this.headerBandColor = options.headerBandColor();
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
                    section -> addMasthead(section, doc.identity(), pageWidth,
                            ruleBleed));
            if (headerBandColor == null) {
                // Stock masthead rule. In banded mode the rule is drawn flush at
                // the band's bottom edge inside the header canvas, so the
                // separate flow rule is suppressed (no doubled line).
                flow.addLine(line -> line
                        .name("CoverLetterV2MintEditorialHeaderRule")
                        .horizontal(pageWidth)
                        .color(ruleColor)
                        .thickness(theme.spacing().accentRuleWidth())
                        .margin(new DocumentInsets(8, -ruleBleed, 14, -ruleBleed)));
            }

            flow.addSection("CoverLetterV2MintEditorialBody", host ->
                    LetterBody.render(host, doc, theme));

            flow.build();
        }

        /**
         * Centred spaced-caps name + mint accent tagline — the identical
         * masthead the {@code MintEditorial} CV preset renders, so the matched
         * set never forks the header treatment. With default colours and no
         * band the output is byte-identical to the pre-Options render.
         */
        private void addMasthead(SectionBuilder section, CvIdentity identity,
                                 double pageWidth, double ruleBleed) {
            if (headerBandColor != null) {
                addBandedMasthead(section, identity, pageWidth, ruleBleed);
            } else {
                addPlainMasthead(section, identity);
            }
        }

        private void addPlainMasthead(SectionBuilder section, CvIdentity identity) {
            // Style-override variant so only the name colour can change.
            Headline.render(section, identity.name(), theme,
                    TextAlign.CENTER, true, mastheadNameStyle());
            String jobTitle = identity.jobTitle();
            if (jobTitle != null && !jobTitle.isBlank()) {
                Subheadline.centeredSpacedCaps(section, jobTitle, taglineStyle());
            }
        }

        /**
         * Banded masthead: the whole masthead zone is one
         * {@code CanvasLayerNode} (controlled absolute placement), mirroring the
         * paired CV preset. The band fills the canvas (page top edge → rule),
         * the canvas is bled to the page edges via negative margins, and the
         * rule sits flush at the band's bottom edge. The canvas reserves only
         * {@value #MASTHEAD_CANVAS_HEIGHT}pt of flow, so the band adds no extra
         * flow height. See {@code MintEditorial.addBandedHeader} for the full
         * rationale (including why the band fills the canvas rather than
         * overflowing a child upward).
         */
        private void addBandedMasthead(SectionBuilder section,
                                       CvIdentity identity, double pageWidth,
                                       double ruleBleed) {
            double canvasH = MASTHEAD_CANVAS_HEIGHT;
            double ruleThickness = theme.spacing().accentRuleWidth();
            double bandHeight = MASTHEAD_RULE_Y + ruleThickness;

            DocumentNode band = new ShapeBuilder()
                    .name("CoverLetterV2MintEditorialHeaderBand")
                    .size(pageWidth, bandHeight)
                    .fillColor(headerBandColor)
                    .build();
            DocumentNode rule = new ShapeBuilder()
                    .name("CoverLetterV2MintEditorialHeaderRule")
                    .size(pageWidth, ruleThickness)
                    .fillColor(ruleColor)
                    .build();
            ParagraphNode name = new ParagraphBuilder()
                    .name("CoverLetterV2MintEditorialHeaderName")
                    .text(TextOrnaments.spacedUpper(identity.name().full()))
                    .textStyle(mastheadNameStyle())
                    .align(TextAlign.CENTER)
                    .build();
            String jobTitle = identity.jobTitle();
            ParagraphNode tagline = jobTitle != null && !jobTitle.isBlank()
                    ? new ParagraphBuilder()
                            .name("CoverLetterV2MintEditorialHeaderTagline")
                            .text(TextOrnaments.spacedUpper(jobTitle))
                            .textStyle(taglineStyle())
                            .align(TextAlign.CENTER)
                            .build()
                    : null;

            section.addCanvas(pageWidth, canvasH, canvas -> {
                canvas.name("CoverLetterV2MintEditorialHeaderCanvas")
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .margin(new DocumentInsets(-ruleBleed, -ruleBleed, 0,
                                -ruleBleed))
                        .position(band, 0.0, 0.0)
                        .position(rule, 0.0, MASTHEAD_RULE_Y)
                        .position(name, 0.0, MASTHEAD_NAME_Y);
                if (tagline != null) {
                    canvas.position(tagline, 0.0, MASTHEAD_TAGLINE_Y);
                }
            });
        }

        private DocumentTextStyle mastheadNameStyle() {
            return CvTextStyles.of(theme.typography().headlineFont(),
                    theme.typography().sizeHeadline(),
                    DocumentTextDecoration.DEFAULT, nameColor);
        }

        private DocumentTextStyle taglineStyle() {
            return CvTextStyles.of(theme.typography().headlineFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.BOLD, accent);
        }
    }
}
