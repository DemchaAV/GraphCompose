package com.demcha.compose.document.templates.coverletter.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
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
import com.demcha.compose.document.templates.widgets.CardWidget;
import com.demcha.compose.font.FontName;

import java.util.Locale;
import java.util.Objects;

/**
 * v2 cover-letter pair for the {@code Panel} CV preset.
 *
 * <p>Carries the CV's signature <strong>pale-teal header card</strong>
 * into the letter: a full-width rounded card (thin teal stroke) holding
 * the centred UPPERCASE Poppins name, job title, centred meta line, and
 * a centred link row with teal accent links — the same header card as
 * {@link com.demcha.compose.document.templates.cv.v2.presets.Panel}.
 * Below it, a single-column letter body via the shared
 * {@link LetterBody}. Card shell + body palette come from
 * {@link CvTheme#panel()}.</p>
 *
 * <p>The two masthead colours (deep-navy header text, teal accent) are
 * mirrored from the CV, where they are preset-local. The header card is
 * pinned to the full content width with a zero-height spacer
 * ({@link #widthAnchor}) so it spans the page rather than shrinking to
 * fit the name — the same trick the CV uses to keep its panels aligned.</p>
 */
public final class PanelLetter {

    /** Stable template identifier. */
    public static final String ID = "panel-letter";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Panel Letter";

    /** Recommended page margin (in points) — generous business-letter feel. */
    public static final double RECOMMENDED_MARGIN = 48.0;

    /** Deep navy masthead text. Mirrors the Panel CV's preset-local token. */
    private static final DocumentColor HEADER_TEXT = DocumentColor.rgb(20, 44, 66);

    /** Teal accent for header links. Mirrors the Panel CV's preset-local token. */
    private static final DocumentColor ACCENT = DocumentColor.rgb(0, 128, 128);

    /** Thin card stroke — single value keeps the card outline crisp. Mirrors the CV. */
    private static final double PANEL_STROKE_THICKNESS = 0.45;

    private PanelLetter() {
    }

    /**
     * Builds the letter with its Panel theme.
     */
    public static DocumentTemplate<CoverLetterDocument> create() {
        return create(CvTheme.panel());
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

            double innerWidth = document.canvas().innerWidth();
            double cardPadding = theme.spacing().bannerInnerPadding();
            double cardContentWidth = innerWidth - 2 * cardPadding;

            PageFlowBuilder flow = document.dsl()
                    .pageFlow()
                    .name("CoverLetterV2PanelRoot")
                    .spacing(theme.spacing().pageFlowSpacing());

            addHeader(flow, doc.identity(), cardContentWidth);

            flow.addSection("CoverLetterV2PanelBody", host ->
                    LetterBody.render(host, doc, theme));

            flow.build();
        }

        private void addHeader(PageFlowBuilder flow, CvIdentity identity,
                               double anchorWidth) {
            CardWidget.render(flow, "CoverLetterV2PanelHeader", headerStyle(),
                    card -> {
                        widthAnchor(card, anchorWidth);
                        card.addParagraph(paragraph -> paragraph
                                .text(identity.name().full().toUpperCase(Locale.ROOT))
                                .textStyle(nameStyle())
                                .align(TextAlign.CENTER)
                                .margin(DocumentInsets.zero()));
                        if (!identity.jobTitle().isBlank()) {
                            card.addParagraph(paragraph -> paragraph
                                    .text(identity.jobTitle())
                                    .textStyle(headerBodyStyle())
                                    .align(TextAlign.CENTER)
                                    .margin(DocumentInsets.zero()));
                        }
                        String contact = TextOrnaments.joinPipe(identity.contact().address(),
                                identity.contact().phone());
                        if (!contact.isBlank()) {
                            card.addParagraph(paragraph -> paragraph
                                    .text(contact)
                                    .textStyle(headerMetaStyle())
                                    .align(TextAlign.CENTER)
                                    .margin(DocumentInsets.zero()));
                        }
                        addLinkRow(card, identity);
                    });
        }

        private void addLinkRow(SectionBuilder section, CvIdentity identity) {
            boolean hasEmail = !identity.contact().email().isBlank();
            boolean hasLinks = !identity.links().isEmpty();
            if (!hasEmail && !hasLinks) {
                return;
            }
            DocumentTextStyle bodyStyle = headerMetaStyle();
            DocumentTextStyle linkStyle = headerLinkStyle();
            section.addParagraph(paragraph -> paragraph
                    .textStyle(bodyStyle)
                    .align(TextAlign.CENTER)
                    .margin(DocumentInsets.zero())
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

        private void widthAnchor(SectionBuilder card, double width) {
            card.spacer(width, 0.0);
        }

        private CardWidget.Style headerStyle() {
            return CardWidget.Style.builder()
                    .spacing(4)
                    .padding(DocumentInsets.of(theme.spacing().bannerInnerPadding()))
                    .fillColor(theme.palette().banner())
                    .stroke(DocumentStroke.of(theme.palette().rule(),
                            PANEL_STROKE_THICKNESS))
                    .cornerRadius(theme.spacing().bannerCornerRadius())
                    .build();
        }

        private DocumentTextStyle nameStyle() {
            return CvTextStyles.of(FontName.POPPINS,
                    theme.typography().sizeHeadline(),
                    DocumentTextDecoration.BOLD, HEADER_TEXT);
        }

        private DocumentTextStyle headerBodyStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeBody(),
                    DocumentTextDecoration.DEFAULT, theme.palette().ink());
        }

        private DocumentTextStyle headerMetaStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.DEFAULT, theme.palette().ink());
        }

        private DocumentTextStyle headerLinkStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.UNDERLINE, ACCENT);
        }
    }
}
