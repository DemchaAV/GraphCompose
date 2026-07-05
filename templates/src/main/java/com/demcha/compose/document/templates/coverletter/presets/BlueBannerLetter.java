package com.demcha.compose.document.templates.coverletter.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.components.LetterBody;
import com.demcha.compose.document.templates.coverletter.data.CoverLetterDocument;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.core.identity.ContactLine;
import com.demcha.compose.document.templates.core.identity.Headline;

import java.util.Objects;

/**
 * v2 cover-letter pair for the {@code BlueBanner} CV preset.
 *
 * <p>Renders the <strong>identical masthead</strong> as
 * {@link com.demcha.compose.document.templates.cv.presets.BlueBanner}
 * — a centred PT-Serif spaced-caps name over a compact centred contact
 * row — then a single-column letter body via the shared
 * {@link LetterBody}. Both documents read everything from
 * {@link BrandTheme#blueBanner()}.</p>
 *
 * <p>The CV's signature blue banners decorate <em>section</em> titles,
 * which a letter has none of, so the brand identity here is carried by
 * the theme: the compact PT-Serif headline scale and the dark-blue rule
 * tone of the contact separators / links. No preset-local colour is
 * needed.</p>
 */
public final class BlueBannerLetter {

    /**
     * Stable template identifier.
     */
    public static final String ID = "blue-banner-letter";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Blue Banner Letter";

    /**
     * Recommended page margin (in points) — generous business-letter feel.
     */
    public static final double RECOMMENDED_MARGIN = 48.0;

    private BlueBannerLetter() {
    }

    /**
     * Builds the letter with its Blue Banner theme.
     *
     * @return a {@code DocumentTemplate} for the "Blue Banner Letter"
     */
    public static DocumentTemplate<CoverLetterDocument> create() {
        return create(BrandTheme.blueBanner());
    }

    /**
     * Builds the letter with a caller-supplied theme (share the paired
     * CV's theme instance for a guaranteed visual match).
     *
     * @param theme the active theme supplying palette, typography, and spacing
     * @return a {@code DocumentTemplate} for the "Blue Banner Letter"
     */
    public static DocumentTemplate<CoverLetterDocument> create(BrandTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new Template(theme);
    }

    private record Template(BrandTheme theme) implements DocumentTemplate<CoverLetterDocument> {

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
                                Headline.spacedCentered(section, doc.identity().name().full(), theme))
                        .addSection("CoverLetterV2BlueBannerContact", section ->
                                ContactLine.centered(section, doc.identity(), theme));

                flow.addSection("CoverLetterV2BlueBannerBody", host ->
                        LetterBody.render(host, doc, theme));

                flow.build();
            }
        }
}
