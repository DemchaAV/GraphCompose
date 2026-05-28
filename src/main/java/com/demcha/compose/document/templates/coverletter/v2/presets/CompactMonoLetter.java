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
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.ContactLine;
import com.demcha.compose.document.templates.cv.v2.widgets.Headline;

import java.util.Objects;

/**
 * v2 cover-letter pair for the {@code CompactMono} CV preset.
 *
 * <p>Carries the CV's signature <strong>dark command-bar header</strong>
 * into the letter: a near-black rounded band holding the UPPERCASE
 * left-aligned name over a left-aligned contact line with cyan links and
 * grey separators — the same header as
 * {@link com.demcha.compose.document.templates.cv.v2.presets.CompactMono}.
 * Below it, a single-column letter body via the shared
 * {@link LetterBody}. Body palette / typography come from
 * {@link CvTheme#compactMono()}.</p>
 *
 * <p>The four command-bar colours are mirrored from the CV, where they
 * are preset-local. A near-invisible width rule (band-coloured, 0.1pt)
 * pins the band to the full content width — the same trick the CV uses
 * so the dark bar spans the page instead of shrinking to the name.</p>
 */
public final class CompactMonoLetter {

    /** Stable template identifier. */
    public static final String ID = "compact-mono-letter";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Compact Mono Letter";

    /** Recommended page margin (in points) — generous business-letter feel. */
    public static final double RECOMMENDED_MARGIN = 48.0;

    /** Near-black command-bar fill. Mirrors the CompactMono CV token. */
    private static final DocumentColor HEADER = DocumentColor.rgb(18, 24, 32);

    /** Contact metadata over the dark band. Mirrors the CV token. */
    private static final DocumentColor HEADER_SOFT = DocumentColor.rgb(192, 207, 219);

    /** Cyan contact-link colour over the band. Mirrors the CV token. */
    private static final DocumentColor LINK_CYAN = DocumentColor.rgb(108, 213, 222);

    /** Contact separator colour over the band. Mirrors the CV token. */
    private static final DocumentColor SEPARATOR_GRAY = DocumentColor.rgb(102, 117, 132);

    private CompactMonoLetter() {
    }

    /**
     * Builds the letter with its Compact Mono theme.
     */
    public static DocumentTemplate<CoverLetterDocument> create() {
        return create(CvTheme.compactMono());
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

            double width = document.canvas().innerWidth();
            PageFlowBuilder flow = document.dsl()
                    .pageFlow()
                    .name("CoverLetterV2CompactMonoRoot")
                    .spacing(theme.spacing().pageFlowSpacing());

            addHeader(flow, doc.identity(), width);

            flow.addSection("CoverLetterV2CompactMonoBody", host ->
                    LetterBody.render(host, doc, theme));

            flow.build();
        }

        private void addHeader(PageFlowBuilder flow, CvIdentity identity,
                               double width) {
            flow.addSection("CoverLetterV2CompactMonoHeader", section -> {
                section.spacing(4)
                        .padding(new DocumentInsets(13, 16, 14, 16))
                        .fillColor(HEADER)
                        .cornerRadius(3);
                section.addSection("Name", name ->
                        Headline.uppercaseLeftAligned(name, identity.name(),
                                theme, headerNameStyle()));
                section.addSection("Contact", contact ->
                        ContactLine.leftAligned(contact, identity, theme,
                                headerMetaStyle(), headerLinkStyle(),
                                headerSeparatorStyle()));
                section.addLine(line -> line
                        .name("CoverLetterV2CompactMonoHeaderWidthRule")
                        .horizontal(Math.max(0, width - 32))
                        .color(HEADER)
                        .thickness(0.1)
                        .margin(DocumentInsets.zero()));
            });
        }

        private DocumentTextStyle headerNameStyle() {
            return CvTextStyles.of(theme.typography().headlineFont(),
                    theme.typography().sizeHeadline(),
                    DocumentTextDecoration.BOLD, DocumentColor.WHITE);
        }

        private DocumentTextStyle headerMetaStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.DEFAULT, HEADER_SOFT);
        }

        private DocumentTextStyle headerLinkStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.UNDERLINE, LINK_CYAN);
        }

        private DocumentTextStyle headerSeparatorStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.DEFAULT, SEPARATOR_GRAY);
        }
    }
}
