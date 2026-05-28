package com.demcha.compose.document.templates.coverletter.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.v2.components.LetterBody;
import com.demcha.compose.document.templates.coverletter.v2.data.CoverLetterDocument;
import com.demcha.compose.document.templates.cv.v2.components.CvTextStyles;
import com.demcha.compose.document.templates.cv.v2.components.TextOrnaments;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.data.CvLink;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.Headline;
import com.demcha.compose.font.FontName;

import java.util.Objects;

/**
 * v2 cover-letter pair for the {@code Executive} CV preset.
 *
 * <p>Renders the <strong>identical masthead</strong> as
 * {@link com.demcha.compose.document.templates.cv.v2.presets.Executive}
 * — UPPERCASE name in deep slate, a {@code address | phone} meta line, a
 * bronze-underlined link row, and a thin full-width muted rule — then a
 * single-column letter body (greeting, paragraphs, closing) via the
 * shared {@link LetterBody}. Both documents read all colour, font, and
 * spacing from {@link CvTheme#executive()}, so a writer's CV and cover
 * letter ship as one matched set.</p>
 *
 * <p>The masthead block is preset-local inline DSL mirroring the CV's,
 * because the CV's header is itself preset-local (V1 splits meta and
 * links across two rows — no shared v2 contact widget has that exact
 * shape today). When a second brand needs the same header shape, this
 * block should be promoted to a shared {@code coverletter/v2/widgets}
 * letterhead widget the CV preset can also adopt.</p>
 */
public final class ExecutiveLetter {

    /** Stable template identifier. */
    public static final String ID = "executive-letter";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Executive Letter";

    /** Recommended page margin (in points) — generous business-letter feel. */
    public static final double RECOMMENDED_MARGIN = 48.0;

    /**
     * Deeper slate of the Executive masthead. Mirrors the preset-local
     * {@code PRIMARY_NAME} token of the Executive CV (the theme's
     * {@code palette().ink()} is the lighter body slate); kept local
     * here for the same reason it is local there — no other brand
     * shares it.
     */
    private static final DocumentColor PRIMARY_NAME =
            DocumentColor.rgb(24, 35, 51);

    /**
     * Warm bronze of the Executive contact links. Mirrors the
     * preset-local {@code ACCENT} token of the Executive CV.
     */
    private static final DocumentColor ACCENT =
            DocumentColor.rgb(172, 112, 55);

    private ExecutiveLetter() {
    }

    /**
     * Builds the letter with its Executive theme.
     */
    public static DocumentTemplate<CoverLetterDocument> create() {
        return create(CvTheme.executive());
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
                    .name("CoverLetterV2ExecutiveRoot")
                    .spacing(theme.spacing().pageFlowSpacing());

            addHeader(flow, doc.identity(), width);

            flow.addSection("CoverLetterV2ExecutiveBody", host ->
                    LetterBody.render(host, doc, theme));

            flow.build();
        }

        private void addHeader(PageFlowBuilder flow, CvIdentity identity,
                               double width) {
            flow.addSection("CoverLetterV2ExecutiveHeader", section -> {
                section.spacing(2)
                        .padding(DocumentInsets.zero());
                Headline.uppercaseLeftAligned(section, identity.name(), theme,
                        nameStyle());
                String meta = TextOrnaments.joinPipe(identity.contact().address(),
                        identity.contact().phone());
                if (!meta.isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(meta)
                            .textStyle(metaStyle())
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.top(2)));
                }
                addLinkRow(section, identity);
                section.addLine(line -> line
                        .name("CoverLetterV2ExecutiveHeaderRule")
                        .horizontal(width)
                        .color(theme.palette().rule())
                        .thickness(theme.spacing().accentRuleWidth())
                        .margin(DocumentInsets.top(5)));
            });
        }

        private void addLinkRow(SectionBuilder section, CvIdentity identity) {
            boolean hasEmail = !identity.contact().email().isBlank();
            boolean hasLinks = !identity.links().isEmpty();
            if (!hasEmail && !hasLinks) {
                return;
            }
            DocumentTextStyle bodyStyle = linkRowBodyStyle();
            DocumentTextStyle linkStyle = linkRowLinkStyle();
            section.addParagraph(paragraph -> paragraph
                    .textStyle(bodyStyle)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(1))
                    .rich(rich -> {
                        boolean first = true;
                        String email = identity.contact().email();
                        if (!email.isBlank()) {
                            rich.with(email, linkStyle,
                                    new DocumentLinkOptions("mailto:" + email));
                            first = false;
                        }
                        for (CvLink link : identity.links()) {
                            if (link.label().isBlank()) {
                                continue;
                            }
                            if (!first) {
                                rich.style(" | ", bodyStyle);
                            }
                            first = false;
                            if (link.url().isBlank()) {
                                rich.style(link.label(), bodyStyle);
                            } else {
                                rich.with(link.label(), linkStyle,
                                        new DocumentLinkOptions(link.url()));
                            }
                        }
                    }));
        }

        private DocumentTextStyle nameStyle() {
            return CvTextStyles.of(FontName.POPPINS,
                    theme.typography().sizeHeadline(),
                    DocumentTextDecoration.BOLD,
                    PRIMARY_NAME);
        }

        private DocumentTextStyle metaStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }

        private DocumentTextStyle linkRowBodyStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeBody(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }

        private DocumentTextStyle linkRowLinkStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeBody(),
                    DocumentTextDecoration.UNDERLINE,
                    ACCENT);
        }
    }
}
