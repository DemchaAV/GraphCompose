package com.demcha.compose.document.templates.coverletter.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.components.LetterBody;
import com.demcha.compose.document.templates.coverletter.data.CoverLetterDocument;
import com.demcha.compose.document.templates.core.text.TextStyles;
import com.demcha.compose.document.templates.core.text.MarkdownInline;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.core.identity.ContactLine;
import com.demcha.compose.document.templates.core.identity.Headline;

import java.util.Locale;
import java.util.Objects;

/**
 * v2 cover-letter pair for the {@code NordicClean} CV preset.
 *
 * <p>Reproduces the CV's signature header: a left-aligned UPPERCASE
 * Barlow name with a short <strong>teal accent bar</strong> beneath it
 * and an UPPERCASE role sub-line, balanced by a right-aligned stacked
 * contact list with teal links — the same masthead as
 * {@link com.demcha.compose.document.templates.cv.v2.presets.NordicClean}.
 * Below it, a single-column letter body via the shared
 * {@link LetterBody}. Body palette / typography come from
 * {@link BrandTheme#nordicClean()}; the CV's tinted profile band is a
 * CV-body element and is intentionally not part of the letter.</p>
 *
 * <p>The teal {@code ACCENT} is mirrored from the CV's default accent
 * (the CV exposes it via an Options knob; the letter uses the default).</p>
 */
public final class NordicCleanLetter {

    /**
     * Stable template identifier.
     */
    public static final String ID = "nordic-clean-letter";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Nordic Clean Letter";

    /**
     * Recommended page margin (in points) — generous business-letter feel.
     */
    public static final double RECOMMENDED_MARGIN = 48.0;

    /**
     * Teal accent bar + link colour. Mirrors the NordicClean CV default accent.
     */
    private static final DocumentColor ACCENT = DocumentColor.rgb(28, 128, 135);

    private NordicCleanLetter() {
    }

    /**
     * Builds the letter with its Nordic Clean theme.
     *
     * @return a {@code DocumentTemplate} for the "Nordic Clean Letter"
     */
    public static DocumentTemplate<CoverLetterDocument> create() {
        return create(BrandTheme.nordicClean());
    }

    /**
     * Builds the letter with a caller-supplied theme (share the paired
     * CV's theme instance for a guaranteed visual match).
     *
     * @param theme the active theme supplying palette, typography, and spacing
     * @return a {@code DocumentTemplate} for the "Nordic Clean Letter"
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
                        .name("CoverLetterV2NordicCleanRoot")
                        .spacing(theme.spacing().pageFlowSpacing());

                addHeader(flow, doc.identity());

                flow.addSection("CoverLetterV2NordicCleanBody", host ->
                        LetterBody.render(host, doc, theme));

                flow.build();
            }

            private void addHeader(PageFlowBuilder flow, CvIdentity identity) {
                flow.addRow("CoverLetterV2NordicCleanHeader", row -> row
                        .spacing(14)
                        .weights(1.2, 0.8)
                        .addSection("Identity", id -> {
                            id.spacing(3).padding(new DocumentInsets(1, 0, 2, 0));
                            Headline.uppercaseLeftAligned(id, identity.name().full(), theme,
                                    headlineStyle());
                            id.addShape(shape -> shape
                                    .name("CoverLetterV2NordicCleanNameAccent")
                                    .size(64, 2.6)
                                    .fillColor(ACCENT)
                                    .cornerRadius(1.3)
                                    .margin(DocumentInsets.zero()));
                            if (!identity.jobTitle().isBlank()) {
                                id.addParagraph(paragraph -> paragraph
                                        .text(MarkdownInline.plainText(identity.jobTitle())
                                                .toUpperCase(Locale.ROOT))
                                        .textStyle(TextStyles.of(
                                                theme.typography().bodyFont(), 7.7,
                                                DocumentTextDecoration.BOLD,
                                                theme.palette().muted()))
                                        .margin(DocumentInsets.zero()));
                            }
                        })
                        .addSection("Contact", contact ->
                                ContactLine.rightAlignedStacked(contact, identity,
                                        theme, contactMetaStyle(), contactLinkStyle())));
            }

            private DocumentTextStyle headlineStyle() {
                return TextStyles.of(theme.typography().headlineFont(),
                        theme.typography().sizeHeadline(),
                        DocumentTextDecoration.BOLD, theme.palette().ink());
            }

            private DocumentTextStyle contactMetaStyle() {
                return TextStyles.of(theme.typography().bodyFont(),
                        theme.typography().sizeContact(),
                        DocumentTextDecoration.DEFAULT, theme.palette().muted());
            }

            private DocumentTextStyle contactLinkStyle() {
                return TextStyles.of(theme.typography().bodyFont(),
                        theme.typography().sizeContact(),
                        DocumentTextDecoration.UNDERLINE, ACCENT);
            }
        }
}
