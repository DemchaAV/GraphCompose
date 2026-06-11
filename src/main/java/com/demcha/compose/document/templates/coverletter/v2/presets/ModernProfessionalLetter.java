package com.demcha.compose.document.templates.coverletter.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.v2.components.LetterBody;
import com.demcha.compose.document.templates.coverletter.v2.data.CoverLetterDocument;
import com.demcha.compose.document.templates.cv.v2.components.CvTextStyles;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.ContactLine;
import com.demcha.compose.document.templates.cv.v2.widgets.Headline;
import com.demcha.compose.font.FontName;

import java.util.Objects;

/**
 * v2 cover-letter pair for the {@code ModernProfessional} CV preset.
 *
 * <p>Renders the <strong>identical masthead</strong> as
 * {@link com.demcha.compose.document.templates.cv.v2.presets.ModernProfessional}
 * — a right-aligned slate-blue Helvetica display name over a two-row,
 * right-aligned contact stack with royal-blue underlined links and a
 * bottom accent rule — then a single-column letter body via the shared
 * {@link LetterBody}. Both documents read their scale and palette from
 * {@link CvTheme#modernProfessional()}.</p>
 *
 * <p>Unlike Executive, the header is composed almost entirely from
 * shared widgets ({@link Headline#rightAligned} +
 * {@link ContactLine#twoRowRightAligned}); only the three preset-local
 * colours (slate-blue name, royal-blue links) are mirrored from the CV,
 * which keeps them preset-local there because no other brand shares
 * them.</p>
 */
public final class ModernProfessionalLetter {

    /**
     * Stable template identifier.
     */
    public static final String ID = "modern-professional-letter";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Modern Professional Letter";

    /**
     * Recommended page margin (in points) — generous business-letter feel.
     */
    public static final double RECOMMENDED_MARGIN = 48.0;

    /**
     * Slate-blue display name. Mirrors the ModernProfessional CV's preset-local token.
     */
    private static final DocumentColor NAME_COLOR = DocumentColor.rgb(44, 62, 80);

    /**
     * Royal-blue contact links. Mirrors the ModernProfessional CV's preset-local token.
     */
    private static final DocumentColor LINK_COLOR = DocumentColor.rgb(65, 105, 225);

    private ModernProfessionalLetter() {
    }

    /**
     * Builds the letter with its Modern Professional theme.
     *
     * @return a {@code DocumentTemplate} for the "Modern Professional Letter"
     */
    public static DocumentTemplate<CoverLetterDocument> create() {
        return create(CvTheme.modernProfessional());
    }

    /**
     * Builds the letter with a caller-supplied theme (share the paired
     * CV's theme instance for a guaranteed visual match).
     *
     * @param theme the active theme supplying palette, typography, and spacing
     * @return a {@code DocumentTemplate} for the "Modern Professional Letter"
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

                DocumentTextStyle nameStyle = CvTextStyles.of(FontName.HELVETICA_BOLD,
                        theme.typography().sizeHeadline(),
                        DocumentTextDecoration.BOLD, NAME_COLOR);
                DocumentTextStyle contactBodyStyle = CvTextStyles.of(FontName.HELVETICA,
                        theme.typography().sizeContact(),
                        DocumentTextDecoration.DEFAULT, theme.palette().ink());
                DocumentTextStyle contactLinkStyle = CvTextStyles.of(FontName.HELVETICA,
                        theme.typography().sizeContact(),
                        DocumentTextDecoration.UNDERLINE, LINK_COLOR);
                DocumentTextStyle contactSeparatorStyle = CvTextStyles.of(FontName.HELVETICA,
                        theme.typography().sizeContact(),
                        DocumentTextDecoration.DEFAULT, theme.palette().rule());

                PageFlowBuilder flow = document.dsl()
                        .pageFlow()
                        .name("CoverLetterV2ModernRoot")
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

                flow.addSection("CoverLetterV2ModernBody", host ->
                        LetterBody.render(host, doc, theme));

                flow.build();
            }
        }
}
