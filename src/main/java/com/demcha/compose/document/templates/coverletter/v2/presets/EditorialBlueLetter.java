package com.demcha.compose.document.templates.coverletter.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.v2.components.LetterBody;
import com.demcha.compose.document.templates.coverletter.v2.data.CoverLetterDocument;
import com.demcha.compose.document.templates.cv.v2.components.CvTextStyles;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.Masthead;
import com.demcha.compose.font.FontName;

import java.util.Objects;

/**
 * v2 cover-letter pair for the {@code EditorialBlue} CV preset.
 *
 * <p>Renders the <strong>identical masthead</strong> as
 * {@link com.demcha.compose.document.templates.cv.v2.presets.EditorialBlue}
 * — a centred navy Helvetica name (with the job-title subtitle), centred
 * contact metadata, and blue underlined profile links, via the shared
 * {@link Masthead#centered} widget — then a single-column letter body
 * via the shared {@link LetterBody}. Both documents read their palette /
 * typography from {@link CvTheme#editorialBlue()}.</p>
 *
 * <p>Only the navy {@code NAME_COLOR} is mirrored from the CV (its
 * preset-local token); everything else flows through {@code Masthead}
 * and the theme.</p>
 */
public final class EditorialBlueLetter {

    /** Stable template identifier. */
    public static final String ID = "editorial-blue-letter";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Editorial Blue Letter";

    /** Recommended page margin (in points) — generous business-letter feel. */
    public static final double RECOMMENDED_MARGIN = 48.0;

    /** Navy display name. Mirrors the EditorialBlue CV's preset-local token. */
    private static final DocumentColor NAME_COLOR = DocumentColor.rgb(18, 31, 72);

    private EditorialBlueLetter() {
    }

    /**
     * Builds the letter with its Editorial Blue theme.
     *
     * @return a {@code DocumentTemplate} for the "Editorial Blue Letter"
     */
    public static DocumentTemplate<CoverLetterDocument> create() {
        return create(CvTheme.editorialBlue());
    }

    /**
     * Builds the letter with a caller-supplied theme (share the paired
     * CV's theme instance for a guaranteed visual match).
     *
     * @param theme the active theme supplying palette, typography, and spacing
     * @return a {@code DocumentTemplate} for the "Editorial Blue Letter"
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
                    .name("CoverLetterV2EditorialBlueRoot")
                    .spacing(theme.spacing().pageFlowSpacing());

            flow.addSection("CoverLetterV2EditorialBlueHeader", section ->
                    Masthead.centered(section, doc.identity(), theme,
                            mastheadStyle()));

            flow.addSection("CoverLetterV2EditorialBlueBody", host ->
                    LetterBody.render(host, doc, theme));

            flow.build();
        }

        private Masthead.Style mastheadStyle() {
            DocumentTextStyle nameStyle = CvTextStyles.of(FontName.HELVETICA_BOLD,
                    theme.typography().sizeHeadline(),
                    DocumentTextDecoration.BOLD, NAME_COLOR);
            DocumentTextStyle titleStyle = CvTextStyles.of(FontName.HELVETICA,
                    10.0, DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
            DocumentTextStyle linkStyle = CvTextStyles.of(FontName.HELVETICA,
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.UNDERLINE,
                    theme.palette().rule());
            return Masthead.Style.builder()
                    .nameStyle(nameStyle)
                    .titleStyle(titleStyle)
                    .metaStyle(theme.contactStyle())
                    .linkStyle(linkStyle)
                    .separatorStyle(theme.contactStyle())
                    .lineMargin(DocumentInsets.top(1))
                    .build();
        }
    }
}
