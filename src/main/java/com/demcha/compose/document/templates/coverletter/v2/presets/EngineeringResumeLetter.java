package com.demcha.compose.document.templates.coverletter.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.*;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.v2.components.LetterBody;
import com.demcha.compose.document.templates.coverletter.v2.data.CoverLetterDocument;
import com.demcha.compose.document.templates.cv.v2.components.CvTextStyles;
import com.demcha.compose.document.templates.cv.v2.components.MarkdownInline;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.data.CvLink;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * v2 cover-letter pair for the {@code EngineeringResume} CV preset.
 *
 * <p>Carries the CV's signature <strong>full-width navy command
 * header</strong> into the letter: a deep-navy band (rounded top, green
 * accent strip beneath) holding the UPPERCASE name + role subtitle on
 * the left and a right-aligned contact stack with cyan-green underlined
 * links — the same masthead as
 * {@link com.demcha.compose.document.templates.cv.v2.presets.EngineeringResume}.
 * Below the band, a single-column letter body via the shared
 * {@link LetterBody}. Body palette / typography come from
 * {@link CvTheme#engineeringResume()}.</p>
 *
 * <p>The five navy-header colours are mirrored from the CV, where they
 * are preset-local (the theme only covers body ink / muted / rule /
 * profile-band fill — no other brand shares this navy command look).</p>
 */
public final class EngineeringResumeLetter {

    /**
     * Stable template identifier.
     */
    public static final String ID = "engineering-resume-letter";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Engineering Resume Letter";

    /**
     * Recommended page margin (in points) — generous business-letter feel.
     */
    public static final double RECOMMENDED_MARGIN = 48.0;

    /**
     * Deep navy command-header fill. Mirrors the EngineeringResume CV token.
     */
    private static final DocumentColor NAVY = DocumentColor.rgb(13, 32, 47);

    /**
     * Green accent strip beneath the header. Mirrors the CV token.
     */
    private static final DocumentColor GREEN = DocumentColor.rgb(27, 145, 104);

    /**
     * Role-subtitle colour under the name. Mirrors the CV token.
     */
    private static final DocumentColor SUBTITLE_COLOR = DocumentColor.rgb(190, 209, 219);

    /**
     * Contact metadata colour over the navy header. Mirrors the CV token.
     */
    private static final DocumentColor CONTACT_META = DocumentColor.rgb(196, 211, 220);

    /**
     * Cyan-green contact-link colour over the navy header. Mirrors the CV token.
     */
    private static final DocumentColor CONTACT_LINK = DocumentColor.rgb(78, 207, 161);

    private EngineeringResumeLetter() {
    }

    /**
     * Builds the letter with its Engineering Resume theme.
     *
     * @return a {@code DocumentTemplate} for the "Engineering Resume Letter"
     */
    public static DocumentTemplate<CoverLetterDocument> create() {
        return create(CvTheme.engineeringResume());
    }

    /**
     * Builds the letter with a caller-supplied theme (share the paired
     * CV's theme instance for a guaranteed visual match).
     *
     * @param theme the active theme supplying palette, typography, and spacing
     * @return a {@code DocumentTemplate} for the "Engineering Resume Letter"
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
                        .name("CoverLetterV2EngineeringResumeRoot")
                        .spacing(theme.spacing().pageFlowSpacing());

                addHeader(flow, doc.identity());

                flow.addSection("CoverLetterV2EngineeringResumeBody", host ->
                        LetterBody.render(host, doc, theme));

                flow.build();
            }

            private void addHeader(PageFlowBuilder flow, CvIdentity identity) {
                flow.addSection("CoverLetterV2EngineeringResumeHeader", section -> section
                        .spacing(5)
                        .padding(new DocumentInsets(13, 15, 13, 15))
                        .fillColor(NAVY)
                        .cornerRadius(DocumentCornerRadius.top(
                                theme.spacing().bannerCornerRadius()))
                        .accentBottom(GREEN, theme.spacing().accentRuleWidth())
                        .addRow("CoverLetterV2EngineeringResumeHeaderRow", row -> row
                                .spacing(12)
                                .weights(1.15, 0.85)
                                .addSection("CoverLetterV2EngineeringResumeIdentity",
                                        block -> addIdentityBlock(block, identity))
                                .addSection("CoverLetterV2EngineeringResumeContact",
                                        contact -> addContactStack(contact, identity))));
            }

            private void addIdentityBlock(SectionBuilder block, CvIdentity identity) {
                block.padding(DocumentInsets.zero())
                        .spacing(3)
                        .addParagraph(paragraph -> paragraph
                                .text(identity.name().full().toUpperCase(Locale.ROOT))
                                .textStyle(nameStyle())
                                .autoSize(theme.typography().sizeHeadline(), 19.0)
                                .margin(DocumentInsets.zero()));
                String subtitle = headerSubtitleText(identity);
                if (!subtitle.isBlank()) {
                    block.addParagraph(paragraph -> paragraph
                            .text(subtitle)
                            .textStyle(subtitleStyle())
                            .margin(DocumentInsets.zero()));
                }
            }

            private void addContactStack(SectionBuilder section, CvIdentity identity) {
                section.spacing(2).padding(DocumentInsets.zero());
                DocumentTextStyle meta = contactMetaStyle();
                DocumentTextStyle link = contactLinkStyle();
                for (ContactPart part : contactParts(identity)) {
                    section.addParagraph(paragraph -> paragraph
                            .text(part.text())
                            .textStyle(part.linkOptions() == null ? meta : link)
                            .link(part.linkOptions())
                            .align(TextAlign.RIGHT)
                            .margin(DocumentInsets.zero()));
                }
            }

            private DocumentTextStyle nameStyle() {
                return CvTextStyles.of(theme.typography().headlineFont(),
                        theme.typography().sizeHeadline(),
                        DocumentTextDecoration.BOLD, DocumentColor.WHITE);
            }

            private DocumentTextStyle subtitleStyle() {
                return CvTextStyles.of(theme.typography().bodyFont(), 7.6,
                        DocumentTextDecoration.BOLD, SUBTITLE_COLOR);
            }

            private DocumentTextStyle contactMetaStyle() {
                return CvTextStyles.of(theme.typography().bodyFont(),
                        theme.typography().sizeContact(),
                        DocumentTextDecoration.DEFAULT, CONTACT_META);
            }

            private DocumentTextStyle contactLinkStyle() {
                return CvTextStyles.of(theme.typography().bodyFont(),
                        theme.typography().sizeContact(),
                        DocumentTextDecoration.UNDERLINE, CONTACT_LINK);
            }

            private static String headerSubtitleText(CvIdentity identity) {
                String jobTitle = identity.jobTitle();
                if (jobTitle == null || jobTitle.isBlank()) {
                    return "";
                }
                return MarkdownInline.plainText(jobTitle).toUpperCase(Locale.ROOT);
            }

            private static List<ContactPart> contactParts(CvIdentity identity) {
                List<ContactPart> parts = new ArrayList<>();
                addPart(parts, identity.contact().address(), null);
                addPart(parts, identity.contact().phone(), null);
                String email = identity.contact().email();
                if (!email.isBlank()) {
                    addPart(parts, email, new DocumentLinkOptions("mailto:" + email));
                }
                for (CvLink link : identity.links()) {
                    addPart(parts, link.label(), link.url().isBlank()
                            ? null
                            : new DocumentLinkOptions(link.url().trim()));
                }
                return List.copyOf(parts);
            }

            private static void addPart(List<ContactPart> parts, String text,
                                        DocumentLinkOptions linkOptions) {
                if (text != null && !text.isBlank()) {
                    parts.add(new ContactPart(text.trim(), linkOptions));
                }
            }

            private record ContactPart(String text, DocumentLinkOptions linkOptions) {
            }
        }
}
