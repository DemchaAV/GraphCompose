package com.demcha.compose.document.templates.coverletter.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.v2.components.LetterBody;
import com.demcha.compose.document.templates.coverletter.v2.data.CoverLetterDocument;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.ContactLine;
import com.demcha.compose.document.templates.cv.v2.widgets.Headline;

import java.util.Objects;

/**
 * v2 cover-letter pair for the {@code BlueBanner} CV preset.
 *
 * <p>Renders the <strong>identical masthead</strong> as
 * {@link com.demcha.compose.document.templates.cv.v2.presets.BlueBanner}
 * — a centred PT-Serif spaced-caps name over a compact centred contact
 * row — then a single-column letter body via the shared
 * {@link LetterBody}. Both documents read everything from
 * {@link CvTheme#blueBanner()}.</p>
 *
 * <p>The CV's signature blue banners decorate <em>section</em> titles,
 * which a letter has none of, so the brand identity here is carried by
 * the theme: the compact PT-Serif headline scale and the dark-blue rule
 * tone of the contact separators / links. No preset-local colour is
 * needed.</p>
 */
public final class BlueBannerLetter {

    /** Stable template identifier. */
    public static final String ID = "blue-banner-letter";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Blue Banner Letter";

    /** Recommended page margin (in points) — generous business-letter feel. */
    public static final double RECOMMENDED_MARGIN = 48.0;

    private BlueBannerLetter() {
    }

    /**
     * Builds the letter with its Blue Banner theme.
     */
    public static DocumentTemplate<CoverLetterDocument> create() {
        return create(CvTheme.blueBanner());
    }

    /**
     * Builds the letter with a caller-supplied theme (share the paired
     * CV's theme instance for a guaranteed visual match).
     */
    public static DocumentTemplate<CoverLetterDocument> create(CvTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new Template(theme);
    }

    private static final class Template implements DocumentTemplate<CoverLetterDocument> {

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
        public void compose(DocumentSession document, CoverLetterDocument doc) {
            Objects.requireNonNull(document, "document");
            Objects.requireNonNull(doc, "doc");

            PageFlowBuilder flow = document.dsl()
                    .pageFlow()
                    .name("CoverLetterV2BlueBannerRoot")
                    .spacing(theme.spacing().pageFlowSpacing())
                    .addSection("CoverLetterV2BlueBannerHeader", section ->
                            Headline.spacedCentered(section, doc.identity().name(), theme))
                    .addSection("CoverLetterV2BlueBannerContact", section ->
                            ContactLine.centered(section, doc.identity(), theme));

            flow.addSection("CoverLetterV2BlueBannerBody", host ->
                    LetterBody.render(host, doc, theme));

            flow.build();
        }
    }
}
