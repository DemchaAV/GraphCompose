package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.components.CvTextStyles;
import com.demcha.compose.document.templates.cv.v2.components.EntryCompactRenderer;
import com.demcha.compose.document.templates.cv.v2.components.ParagraphRenderer;
import com.demcha.compose.document.templates.cv.v2.components.ProjectRenderer;
import com.demcha.compose.document.templates.cv.v2.components.RowRenderer;
import com.demcha.compose.document.templates.cv.v2.components.SectionLookup;
import com.demcha.compose.document.templates.cv.v2.components.SkillsRenderer;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.data.CvEntry;
import com.demcha.compose.document.templates.cv.v2.data.CvRow;
import com.demcha.compose.document.templates.cv.v2.data.CvSection;
import com.demcha.compose.document.templates.cv.v2.data.EntriesSection;
import com.demcha.compose.document.templates.cv.v2.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.v2.data.RowStyle;
import com.demcha.compose.document.templates.cv.v2.data.RowsSection;
import com.demcha.compose.document.templates.cv.v2.data.SkillsSection;
import com.demcha.compose.document.templates.cv.v2.data.Slot;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.ContactLine;
import com.demcha.compose.document.templates.cv.v2.widgets.FlowSectionHeader;
import com.demcha.compose.document.templates.cv.v2.widgets.Headline;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * v2 port of the legacy "Blue Banner" CV preset.
 *
 * <p>Visual signature: centred PT-Serif spaced-caps name, compact
 * Lato contact row, full-width blue section banners sandwiched
 * between thin dark-blue rules, and dense white body blocks.</p>
 *
 * <p>Most of the preset is ordinary widget composition. The body
 * renderer is deliberately preset-local because Blue Banner differs
 * from the shared {@code EntryRenderer}: entry titles are uppercase,
 * dates are bold, subtitles are regular ink, and project rows render
 * without bullets.</p>
 */
public final class BlueBanner {

    /** Stable template identifier. */
    public static final String ID = "blue-banner";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Blue Banner";

    /** Recommended page margin (in points). */
    public static final double RECOMMENDED_MARGIN = 28.0;

    private static final double BANNER_RULE_WIDTH = 500.0;
    private static final double BANNER_RULE_HORIZONTAL_INSET = 18.0;

    private static final DocumentColor BANNER_TEXT =
            DocumentColor.rgb(22, 32, 48);

    private static final List<String> SUMMARY_KEYS =
            List.of("summary", "professional summary", "profile");
    private static final List<String> EXPERIENCE_KEYS =
            List.of("experience", "professional experience", "employment", "work");
    private static final List<String> EDUCATION_KEYS =
            List.of("education", "certifications");
    private static final List<String> SKILL_KEYS =
            List.of("technical skills", "skills");
    private static final List<String> ADDITIONAL_KEYS =
            List.of("additional information", "additional");

    private BlueBanner() {
    }

    /**
     * Builds the preset with the Blue Banner theme.
     */
    public static DocumentTemplate<CvDocument> create() {
        return create(CvTheme.blueBanner());
    }

    /**
     * Builds the preset with a caller-supplied theme. The caller can
     * adjust palette, typography, spacing, or separator tokens without
     * changing the page-flow composition.
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

            DocumentTextStyle bannerTitleStyle = CvTextStyles.of(
                    theme.typography().bodyFont(),
                    theme.typography().sizeBanner(),
                    DocumentTextDecoration.BOLD,
                    BANNER_TEXT);

            PageFlowBuilder pageFlow = document.dsl()
                    .pageFlow()
                    .name("CvV2BlueBannerRoot")
                    .spacing(theme.spacing().pageFlowSpacing())
                    .addSection("Header", section ->
                            Headline.spacedCentered(section,
                                    doc.identity().name(), theme))
                    .addSection("Contact", section ->
                            ContactLine.centered(section, doc.identity(), theme));

            List<CvSection> sections = orderedSections(doc);
            for (int i = 0; i < sections.size(); i++) {
                final CvSection sec = sections.get(i);
                final int idx = i;
                FlowSectionHeader.banner(pageFlow, "BlueBannerTitle_" + idx,
                        sec.title(), BANNER_RULE_WIDTH, theme, bannerTitleStyle,
                        new DocumentInsets(3, BANNER_RULE_HORIZONTAL_INSET,
                                1, BANNER_RULE_HORIZONTAL_INSET),
                        new DocumentInsets(1, BANNER_RULE_HORIZONTAL_INSET,
                                1, BANNER_RULE_HORIZONTAL_INSET));
                pageFlow.addSection("BlueBannerBody_" + idx, host ->
                        renderBody(host, sec, theme));
            }

            pageFlow.build();
        }
    }

    private static void renderBody(SectionBuilder host,
                                   CvSection section,
                                   CvTheme theme) {
        host.spacing(theme.spacing().sectionBodySpacing())
                .padding(theme.spacing().sectionBodyPadding());

        if (section instanceof ParagraphSection p) {
            ParagraphRenderer.render(host, p.body(), theme);
        } else if (section instanceof SkillsSection s) {
            SkillsRenderer.render(host, s, theme);
        } else if (section instanceof RowsSection r) {
            renderRows(host, r, theme);
        } else if (section instanceof EntriesSection e) {
            for (CvEntry entry : e.entries()) {
                renderEntry(host, entry, theme);
            }
        } else {
            throw new IllegalStateException(
                    "Unknown CvSection subtype: " + section.getClass().getName());
        }
    }

    private static void renderRows(SectionBuilder host,
                                   RowsSection section,
                                   CvTheme theme) {
        if (section.style() == RowStyle.BULLETED_STACKED) {
            for (CvRow row : section.rows()) {
                renderPlainProjectRow(host, row, theme);
            }
            return;
        }
        for (CvRow row : section.rows()) {
            RowRenderer.render(host, row, section.style(), theme);
        }
    }

    private static void renderPlainProjectRow(SectionBuilder host,
                                              CvRow row,
                                              CvTheme theme) {
        ProjectRenderer.plainInline(host, row, theme.entryTitleStyle(),
                theme.bodyStyle(), theme.typography().bodyLineSpacing(),
                DocumentInsets.top((float) theme.spacing().paragraphMarginTop()),
                " - ");
    }

    private static void renderEntry(SectionBuilder section,
                                    CvEntry entry,
                                    CvTheme theme) {
        DocumentTextStyle titleStyle = theme.entryTitleStyle();
        DocumentTextStyle dateStyle = CvTextStyles.of(theme.typography().bodyFont(),
                theme.typography().sizeEntryDate(),
                DocumentTextDecoration.BOLD,
                theme.palette().ink());
        DocumentTextStyle subtitleStyle = CvTextStyles.of(theme.typography().bodyFont(),
                theme.typography().sizeEntrySubtitle(),
                DocumentTextDecoration.DEFAULT,
                theme.palette().ink());

        EntryCompactRenderer.twoColumnTitleDateBody(section, entry,
                "BlueBannerEntryHeader", titleStyle, dateStyle, subtitleStyle,
                theme.bodyStyle(), theme.spacing().entryHeaderRowSpacing(),
                theme.spacing().entryTitleWeight(),
                theme.spacing().entryDateWeight(), DocumentInsets.zero(),
                DocumentInsets.top((float) theme.spacing().paragraphMarginTop()),
                theme.typography().bodyLineSpacing(), true);
    }

    private static List<CvSection> orderedSections(CvDocument doc) {
        List<CvSection> sections = doc.sectionsIn(Slot.MAIN);
        List<CvSection> ordered = new ArrayList<>();
        addIfPresent(ordered, SectionLookup.firstMatching(sections, SUMMARY_KEYS));
        addIfPresent(ordered, SectionLookup.firstMatching(sections, EXPERIENCE_KEYS));
        addIfPresent(ordered, SectionLookup.firstMatching(sections, EDUCATION_KEYS));
        addIfPresent(ordered, SectionLookup.firstMatching(sections, SKILL_KEYS));
        addIfPresent(ordered, SectionLookup.firstMatching(sections, ADDITIONAL_KEYS));
        for (CvSection section : sections) {
            addIfPresent(ordered, section);
        }
        return List.copyOf(ordered);
    }

    private static void addIfPresent(List<CvSection> sections, CvSection section) {
        if (section != null && !sections.contains(section)) {
            sections.add(section);
        }
    }
}
