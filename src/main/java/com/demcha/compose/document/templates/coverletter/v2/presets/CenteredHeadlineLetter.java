package com.demcha.compose.document.templates.coverletter.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.v2.components.LetterBody;
import com.demcha.compose.document.templates.coverletter.v2.data.CoverLetterDocument;
import com.demcha.compose.document.templates.cv.v2.components.CvTextStyles;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.ContactLine;
import com.demcha.compose.document.templates.cv.v2.widgets.Headline;
import com.demcha.compose.document.templates.cv.v2.widgets.Subheadline;

import java.util.Objects;

/**
 * v2 cover-letter pair for the {@code CenteredHeadline} CV preset.
 *
 * <p>Renders the <strong>identical masthead</strong> as
 * {@link com.demcha.compose.document.templates.cv.v2.presets.CenteredHeadline}
 * — a centred letter-spaced Poppins name, a small spaced-caps
 * subheadline, and a centred contact line framed by thin full-width
 * rules — then a single-column letter body via the shared
 * {@link LetterBody}. Both documents read everything from
 * {@link CvTheme#centeredHeadline()}.</p>
 *
 * <p>The subheadline uses the real {@link CvIdentity#jobTitle()} (the
 * CV preset still shows a hard-coded placeholder pending its own
 * jobTitle wiring); on a letter the writer's actual title reads more
 * naturally and stays a faithful match to the CV's <em>design</em>. The
 * rule that the CV places below the contact is dropped here because the
 * shared {@code LetterBody} already supplies the gap to the greeting.</p>
 */
public final class CenteredHeadlineLetter {

    /**
     * Stable template identifier.
     */
    public static final String ID = "centered-headline-letter";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Centered Headline Letter";

    /**
     * Recommended page margin (in points) — generous business-letter feel.
     */
    public static final double RECOMMENDED_MARGIN = 48.0;

    private CenteredHeadlineLetter() {
    }

    /**
     * Builds the letter with its Centered Headline theme.
     *
     * @return a {@code DocumentTemplate} for the "Centered Headline Letter"
     */
    public static DocumentTemplate<CoverLetterDocument> create() {
        return create(CvTheme.centeredHeadline());
    }

    /**
     * Builds the letter with a caller-supplied theme (share the paired
     * CV's theme instance for a guaranteed visual match).
     *
     * @param theme the active theme supplying palette, typography, and spacing
     * @return a {@code DocumentTemplate} for the "Centered Headline Letter"
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

                double width = document.canvas().innerWidth();
                CvIdentity identity = doc.identity();

                PageFlowBuilder flow = document.dsl()
                        .pageFlow()
                        .name("CoverLetterV2CenteredHeadlineRoot")
                        .spacing(theme.spacing().pageFlowSpacing())
                        .addSection("CoverLetterV2CenteredHeadlineHeadline", section -> {
                            Headline.spacedCentered(section, identity.name(), theme);
                            if (!identity.jobTitle().isBlank()) {
                                Subheadline.centeredSpacedCaps(section,
                                        identity.jobTitle(), subheadlineStyle());
                            }
                        })
                        .addLine(line -> rule(line, "CoverLetterV2CenteredHeadlineRule",
                                width, 7, 0))
                        .addSection("CoverLetterV2CenteredHeadlineContact", section ->
                                ContactLine.centered(section, identity, theme))
                        .addLine(line -> rule(line, "CoverLetterV2CenteredContactRule",
                                width, 0, 0));

                flow.addSection("CoverLetterV2CenteredHeadlineBody", host ->
                        LetterBody.render(host, doc, theme));

                flow.build();
            }

            private DocumentTextStyle subheadlineStyle() {
                return CvTextStyles.of(theme.typography().headlineFont(), 8.6,
                        DocumentTextDecoration.DEFAULT, theme.palette().muted());
            }

            private void rule(LineBuilder line, String name, double width,
                              double top, double bottom) {
                line.name(name)
                        .horizontal(width)
                        .color(theme.palette().rule())
                        .thickness(theme.spacing().accentRuleWidth())
                        .margin(new DocumentInsets(top, 0, bottom, 0));
            }
        }
}
