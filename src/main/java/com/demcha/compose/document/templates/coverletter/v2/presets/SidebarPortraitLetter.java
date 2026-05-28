package com.demcha.compose.document.templates.coverletter.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.v2.components.LetterBody;
import com.demcha.compose.document.templates.coverletter.v2.data.CoverLetterDocument;
import com.demcha.compose.document.templates.cv.v2.components.CvTextStyles;
import com.demcha.compose.document.templates.cv.v2.components.TextOrnaments;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.ContactLine;

import java.util.Objects;

/**
 * v2 cover-letter pair for the {@code SidebarPortrait} CV preset.
 *
 * <p>The CV's identity treatment is a beige "hero strip" carrying the
 * centred serif name + spaced-caps role sub-line. The letter keeps that
 * centred name treatment but <strong>drops the beige fill</strong> — a
 * coloured box read as out of place on a single-column letter — leaving a
 * clean centred letterhead, followed by a centred contact line and a
 * single-column letter body via the shared {@link LetterBody}. The CV's
 * circular portrait, icon contact stack, and pale sidebar column (painted
 * via {@code pageBackgrounds}) are sidebar-only and are intentionally
 * dropped for the single-column letter. Palette / typography come from
 * {@link CvTheme#sidebarPortrait()}.</p>
 */
public final class SidebarPortraitLetter {

    /** Stable template identifier. */
    public static final String ID = "sidebar-portrait-letter";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Sidebar Portrait Letter";

    /** Recommended page margin (in points) — generous business-letter feel. */
    public static final double RECOMMENDED_MARGIN = 48.0;

    private SidebarPortraitLetter() {
    }

    /**
     * Builds the letter with its Sidebar Portrait theme.
     */
    public static DocumentTemplate<CoverLetterDocument> create() {
        return create(CvTheme.sidebarPortrait());
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

            PageFlowBuilder flow = document.dsl()
                    .pageFlow()
                    .name("CoverLetterV2SidebarPortraitRoot")
                    .spacing(theme.spacing().pageFlowSpacing());

            addHeroBand(flow, doc.identity());
            addContact(flow, doc.identity());

            flow.addSection("CoverLetterV2SidebarPortraitBody", host ->
                    LetterBody.render(host, doc, theme));

            flow.build();
        }

        private void addHeroBand(PageFlowBuilder flow, CvIdentity identity) {
            String displayName = identity.name().full();
            String jobTitle = identity.jobTitle();
            String subline = jobTitle == null || jobTitle.isBlank()
                    ? ""
                    : TextOrnaments.spacedUpper(jobTitle);
            flow.addSection("CoverLetterV2SidebarPortraitHero", hero -> {
                // No fill: the CV's beige hero band reads as a coloured box
                // on a single-column letter, which clashed with the concept,
                // so the name treatment is kept but the background dropped.
                hero.padding(new DocumentInsets(19, 34, 17, 34))
                        .spacing(3)
                        .addParagraph(paragraph -> paragraph
                                .text(displayName)
                                .textStyle(nameStyle())
                                .align(TextAlign.CENTER)
                                .lineSpacing(1.0)
                                .margin(DocumentInsets.zero()));
                if (!subline.isBlank()) {
                    hero.addParagraph(paragraph -> paragraph
                            .text(subline)
                            .textStyle(subtitleStyle())
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.zero()));
                }
            });
        }

        private void addContact(PageFlowBuilder flow, CvIdentity identity) {
            flow.addSection("CoverLetterV2SidebarPortraitContact", section ->
                    ContactLine.centered(section, identity, theme,
                            contactStyle(), contactLinkStyle(), contactStyle()));
        }

        private DocumentTextStyle nameStyle() {
            return CvTextStyles.of(theme.typography().headlineFont(),
                    theme.typography().sizeHeadline(),
                    DocumentTextDecoration.BOLD, theme.palette().ink());
        }

        private DocumentTextStyle subtitleStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeEntryDate(),
                    DocumentTextDecoration.DEFAULT, theme.palette().ink());
        }

        private DocumentTextStyle contactStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.DEFAULT, theme.palette().ink());
        }

        private DocumentTextStyle contactLinkStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.UNDERLINE, theme.palette().muted());
        }
    }
}
