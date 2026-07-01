package com.demcha.compose.document.templates.coverletter.presets;

import com.demcha.compose.document.templates.core.identity.Link;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.components.LetterBody;
import com.demcha.compose.document.templates.coverletter.data.CoverLetterDocument;
import com.demcha.compose.document.templates.core.text.TextStyles;
import com.demcha.compose.document.templates.cv.v2.components.SectionLookup;
import com.demcha.compose.document.templates.core.text.TextOrnaments;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.core.identity.SvgGlyph;

import java.util.*;

/**
 * v2 cover-letter pair for the {@code TimelineMinimal} CV preset.
 *
 * <p>Reproduces the CV's masthead: a left spaced-caps Barlow-Condensed
 * name + UPPERCASE role line, balanced by a right-aligned contact stack
 * where each line ends with its recolorable SVG glyph icon (LinkedIn / GitHub /
 * location / phone / email), all under a thin full-width rule — the same
 * header as
 * {@link com.demcha.compose.document.templates.cv.v2.presets.TimelineMinimal}.
 * Below it, a single-column letter body via the shared {@link LetterBody}.
 * Palette / typography come from {@link BrandTheme#timelineMinimal()}; the
 * CV's three-column timeline axis is a body element and is not part of
 * the letter.</p>
 *
 * <p>The contact icons reuse the CV's icon set
 * ({@code /templates/cv/timeline-minimal/icons/}) and its text-glyph
 * fallback, so no new assets are introduced.</p>
 */
public final class TimelineMinimalLetter {

    /**
     * Stable template identifier.
     */
    public static final String ID = "timeline-minimal-letter";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Timeline Minimal Letter";

    /**
     * Recommended page margin (in points) — generous business-letter feel.
     */
    public static final double RECOMMENDED_MARGIN = 48.0;

    /**
     * Letter body size. The Timeline Minimal CV theme uses a 7.8pt body
     * tuned for its dense three-column layout — too small for a
     * single-column letter, so the prose is rendered a touch larger here.
     */
    private static final double LETTER_BODY_SIZE = 9.0;

    private static final double CONTACT_ICON_SIZE = 10.5;
    private static final double CONTACT_ICON_BASELINE_OFFSET = -1.35;
    private static final String CONTACT_ICON_ROOT =
            "/templates/cv/timeline-minimal/icons/";
    private static final DocumentColor ICON_COLOR = DocumentColor.rgb(58, 58, 58);

    private TimelineMinimalLetter() {
    }

    /**
     * Builds the letter with its Timeline Minimal theme.
     *
     * @return a {@code DocumentTemplate} for the "Timeline Minimal Letter"
     */
    public static DocumentTemplate<CoverLetterDocument> create() {
        return create(BrandTheme.timelineMinimal());
    }

    /**
     * Builds the letter with a caller-supplied theme (share the paired
     * CV's theme instance for a guaranteed visual match).
     *
     * @param theme the active theme supplying palette, typography, and spacing
     * @return a {@code DocumentTemplate} for the "Timeline Minimal Letter"
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

                double width = document.canvas().innerWidth();
                PageFlowBuilder flow = document.dsl()
                        .pageFlow()
                        .name("CoverLetterV2TimelineMinimalRoot")
                        .spacing(theme.spacing().pageFlowSpacing())
                        .addRow("CoverLetterV2TimelineMinimalHeader", row -> row
                                .spacing(3)
                                .weights(1.00, 0.61)
                                .addSection("CoverLetterV2TimelineMinimalName",
                                        section -> addNameBlock(section, doc.identity()))
                                .addSection("CoverLetterV2TimelineMinimalContact",
                                        section -> addContact(section, doc.identity())))
                        .addLine(line -> line
                                .name("CoverLetterV2TimelineMinimalHeaderRule")
                                .horizontal(width)
                                .color(theme.palette().rule())
                                .thickness(theme.spacing().accentRuleWidth())
                                .margin(DocumentInsets.zero()));

                flow.addSection("CoverLetterV2TimelineMinimalBody", host ->
                        LetterBody.render(host, doc, theme, LETTER_BODY_SIZE));

                flow.build();
            }

            private void addNameBlock(SectionBuilder section, CvIdentity identity) {
                section.spacing(4)
                        .addParagraph(paragraph -> paragraph
                                .text(TextOrnaments.spacedUpper(identity.name().full()))
                                .textStyle(nameStyle())
                                .margin(DocumentInsets.zero()));
                String jobTitle = identity.jobTitle();
                if (!jobTitle.isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(jobTitle.toUpperCase(Locale.ROOT))
                            .textStyle(jobTitleStyle())
                            .margin(DocumentInsets.zero()));
                }
            }

            private void addContact(SectionBuilder section, CvIdentity identity) {
                section.spacing(3);
                DocumentTextStyle textStyle = contactTextStyle();
                DocumentTextStyle fallbackIconStyle = fallbackIconStyle();
                for (ContactItem item : contactItems(identity)) {
                    section.addParagraph(paragraph -> paragraph
                            .textStyle(textStyle)
                            .align(TextAlign.RIGHT)
                            .link(item.linkOptions())
                            .margin(DocumentInsets.zero())
                            .rich(rich -> {
                                rich.style(item.text(), textStyle);
                                rich.plain("  ");
                                if (item.iconFile() != null) {
                                    rich.shape(glyph(item.iconFile()).outline(CONTACT_ICON_SIZE),
                                            ICON_COLOR, null,
                                            InlineImageAlignment.CENTER,
                                            CONTACT_ICON_BASELINE_OFFSET,
                                            item.linkOptions());
                                } else {
                                    rich.style(item.fallbackIcon(), fallbackIconStyle);
                                }
                            }));
                }
            }

            private List<ContactItem> contactItems(CvIdentity identity) {
                if (identity == null) {
                    return List.of();
                }
                List<ContactItem> items = new ArrayList<>();
                addContactItem(items, "LOC", "location.svg",
                        identity.contact().address(), null);
                addContactItem(items, "TEL", "phone.svg",
                        identity.contact().phone(), null);
                String email = identity.contact().email();
                if (!email.isBlank()) {
                    addContactItem(items, "@", "email.svg", email,
                            new DocumentLinkOptions("mailto:" + email));
                }
                for (Link link : identity.links()) {
                    String label = link.label();
                    if (label.isBlank()) {
                        continue;
                    }
                    String url = link.url();
                    addContactItem(items, pickFallbackIcon(label),
                            pickIconFile(label), label,
                            url.isBlank() ? null : new DocumentLinkOptions(url.trim()));
                }
                return List.copyOf(items);
            }

            private SvgGlyph glyph(String iconFile) {
                return SvgGlyph.fromResource(CONTACT_ICON_ROOT + iconFile);
            }

            private DocumentTextStyle nameStyle() {
                return TextStyles.of(theme.typography().headlineFont(),
                        theme.typography().sizeHeadline(),
                        DocumentTextDecoration.DEFAULT, theme.palette().ink());
            }

            private DocumentTextStyle jobTitleStyle() {
                return TextStyles.of(theme.typography().headlineFont(), 9.5,
                        DocumentTextDecoration.BOLD, theme.palette().ink());
            }

            private DocumentTextStyle contactTextStyle() {
                return TextStyles.of(theme.typography().bodyFont(),
                        theme.typography().sizeContact(),
                        DocumentTextDecoration.BOLD, theme.palette().muted());
            }

            private DocumentTextStyle fallbackIconStyle() {
                return TextStyles.of(theme.typography().headlineFont(), 8.0,
                        DocumentTextDecoration.BOLD, theme.palette().muted());
            }
        }

    private static void addContactItem(List<ContactItem> items,
                                       String fallbackIcon, String iconFile,
                                       String text, DocumentLinkOptions linkOptions) {
        if (text != null && !text.isBlank()) {
            items.add(new ContactItem(fallbackIcon, iconFile, text, linkOptions));
        }
    }

    private static String pickIconFile(String label) {
        String normalized = SectionLookup.normalize(label);
        if (normalized.contains("linkedin")) {
            return "linkedin.svg";
        }
        if (normalized.contains("github")) {
            return "github.svg";
        }
        if (normalized.contains("dribbble")) {
            return "dribbble.svg";
        }
        if (normalized.contains("google")) {
            return "google.svg";
        }
        return null;
    }

    private static String pickFallbackIcon(String label) {
        String normalized = SectionLookup.normalize(label);
        if (normalized.contains("linkedin")) {
            return "in";
        }
        if (normalized.contains("github")) {
            return "GH";
        }
        return "@";
    }

    private record ContactItem(String fallbackIcon, String iconFile,
                               String text, DocumentLinkOptions linkOptions) {
    }
}
