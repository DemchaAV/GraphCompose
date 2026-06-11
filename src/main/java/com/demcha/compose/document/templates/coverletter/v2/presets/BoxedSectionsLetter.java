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
 * v2 cover-letter pair for the {@code BoxedSections} CV preset.
 *
 * <p>Renders the <strong>identical masthead</strong> as
 * {@link com.demcha.compose.document.templates.cv.v2.presets.BoxedSections}
 * — a centred letter-spaced PT-Serif name with a thin rule beneath it,
 * then a centred pipe-separated contact line with its own rule beneath
 * — then a single-column letter body via the shared {@link LetterBody}.
 * Both documents read everything from {@link CvTheme#boxedClassic()}.</p>
 *
 * <p>The header is composed entirely from shared widgets
 * ({@link Headline#spacedCentered} + {@link ContactLine#centered}) at
 * the theme's default styles, so this preset carries no preset-local
 * colour — the cleanest possible matched-set letter.</p>
 */
public final class BoxedSectionsLetter {

    /**
     * Stable template identifier.
     */
    public static final String ID = "boxed-sections-letter";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Boxed Sections Letter";

    /**
     * Recommended page margin (in points) — generous business-letter feel.
     */
    public static final double RECOMMENDED_MARGIN = 48.0;

    private BoxedSectionsLetter() {
    }

    /**
     * Builds the letter with its Boxed Sections theme.
     *
     * @return a {@code DocumentTemplate} for the "Boxed Sections Letter"
     */
    public static DocumentTemplate<CoverLetterDocument> create() {
        return create(CvTheme.boxedClassic());
    }

    /**
     * Builds the letter with a caller-supplied theme (share the paired
     * CV's theme instance for a guaranteed visual match).
     *
     * @param theme the active theme supplying palette, typography, and spacing
     * @return a {@code DocumentTemplate} for the "Boxed Sections Letter"
     */
    public static DocumentTemplate<CoverLetterDocument> create(CvTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new Template(theme);
    }

    private record Template(CvTheme theme) implements DocumentTemplate<CoverLetterDocument> {

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
                        .name("CoverLetterV2BoxedRoot")
                        .spacing(theme.spacing().pageFlowSpacing())
                        .addSection("CoverLetterV2BoxedHeadline", section -> {
                            section.accentBottom(theme.palette().rule(),
                                    theme.spacing().accentRuleWidth());
                            Headline.spacedCentered(section, doc.identity().name(), theme);
                        })
                        .addSection("CoverLetterV2BoxedContact", section -> {
                            section.accentBottom(theme.palette().rule(),
                                    theme.spacing().accentRuleWidth());
                            ContactLine.centered(section, doc.identity(), theme);
                        });

                flow.addSection("CoverLetterV2BoxedBody", host ->
                        LetterBody.render(host, doc, theme));

                flow.build();
            }
        }
}
