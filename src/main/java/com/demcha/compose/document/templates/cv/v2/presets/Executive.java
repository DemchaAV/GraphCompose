package com.demcha.compose.document.templates.cv.v2.presets;

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
import com.demcha.compose.document.templates.cv.v2.components.CvTextStyles;
import com.demcha.compose.document.templates.cv.v2.components.SectionDispatcher;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.data.CvLink;
import com.demcha.compose.document.templates.cv.v2.data.CvSection;
import com.demcha.compose.document.templates.cv.v2.data.Slot;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.Headline;
import com.demcha.compose.document.templates.cv.v2.widgets.SectionHeader;
import com.demcha.compose.font.FontName;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * v2 port of the legacy "Executive" CV preset.
 *
 * <p>Polished business CV with restrained slate typography, a compact
 * left-aligned header (UPPERCASE name in deep slate, meta line, link
 * row, full-width muted rule below), and warm bronze module headings
 * over a single-column body. Visual signature ported from the legacy
 * {@code ExecutiveSlateCvTemplate}: Poppins for headings, Lato for
 * body, slate primary, bronze accent.</p>
 *
 * <p>The preset stays a thin orchestrator — the header block is
 * preset-local inline DSL because V1 splits meta and links across two
 * rows (no v2 contact widget has that exact shape today), while
 * everything below the header reuses {@link SectionHeader#flat} for
 * the bronze module titles and {@link SectionDispatcher#renderBody}
 * for the body of every {@code CvSection} subtype.</p>
 */
public final class Executive {

    /** Stable template identifier. */
    public static final String ID = "executive";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Executive";

    /** Recommended page margin (in points) — generous for an executive feel. */
    public static final double RECOMMENDED_MARGIN = 28.0;

    /**
     * Deeper slate used by the V1 Executive masthead. The theme's
     * {@code palette().ink()} is the body-text slate; this is a
     * preset-local fifth token because no other v2 preset shares it.
     */
    private static final DocumentColor PRIMARY_NAME =
            DocumentColor.rgb(24, 35, 51);

    /**
     * Warm bronze used by the V1 Executive module headings and the
     * underlined contact links. Preset-local sixth token.
     */
    private static final DocumentColor ACCENT =
            DocumentColor.rgb(172, 112, 55);

    private Executive() {
    }

    /**
     * Builds the preset with its Executive theme.
     */
    public static DocumentTemplate<CvDocument> create() {
        return create(CvTheme.executive());
    }

    /**
     * Builds the preset with a caller-supplied theme.
     */
    public static DocumentTemplate<CvDocument> create(CvTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new Template(theme);
    }

    private static final class Template implements DocumentTemplate<CvDocument> {

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
        public void compose(DocumentSession document, CvDocument doc) {
            Objects.requireNonNull(document, "document");
            Objects.requireNonNull(doc, "doc");

            double width = document.canvas().innerWidth();
            PageFlowBuilder flow = document.dsl()
                    .pageFlow()
                    .name("CvV2ExecutiveRoot")
                    .spacing(theme.spacing().pageFlowSpacing());

            addHeader(flow, doc.identity(), width);

            List<CvSection> sections = doc.sectionsIn(Slot.MAIN);
            for (int i = 0; i < sections.size(); i++) {
                CvSection sec = sections.get(i);
                int idx = i;
                flow.addSection("CvV2ExecutiveTitle_" + idx, host ->
                        SectionHeader.flat(host,
                                sec.title().toUpperCase(Locale.ROOT),
                                ACCENT, theme));
                flow.addSection("CvV2ExecutiveBody_" + idx, host ->
                        SectionDispatcher.renderBody(host, sec, theme));
            }

            flow.build();
        }

        private void addHeader(PageFlowBuilder flow, CvIdentity identity,
                               double width) {
            flow.addSection("CvV2ExecutiveHeader", section -> {
                section.spacing(2)
                        .padding(DocumentInsets.zero());
                Headline.uppercaseLeftAligned(section, identity.name(), theme,
                        nameStyle());
                String meta = joinPipe(identity.contact().address(),
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
                        .name("CvV2ExecutiveHeaderRule")
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

        private static String joinPipe(String... parts) {
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (part == null || part.isBlank()) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(" | ");
                }
                sb.append(part.trim());
            }
            return sb.toString();
        }
    }
}
