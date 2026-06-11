package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.*;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.components.*;
import com.demcha.compose.document.templates.cv.v2.data.*;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.ContactLine;
import com.demcha.compose.document.templates.cv.v2.widgets.Headline;
import com.demcha.compose.document.templates.cv.v2.widgets.SectionHeader;
import com.demcha.compose.document.templates.cv.v2.widgets.SectionModule;
import com.demcha.compose.document.templates.widgets.CardWidget;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * v2 port of the "Compact Mono" CV preset.
 *
 * <p>Visual signature: a dark command-bar header, IBM Plex Mono
 * identity/section labels, a pale left rail for compact skills,
 * education, and additional information, plus same-width white cards
 * in the right column for profile, experience, and selected
 * projects.</p>
 *
 * <p>The rail/card composition is preset-local: it decides which
 * semantic v2 sections belong in each column and how aggressively to
 * compact them. Reusable pieces still go through v2 widgets:
 * {@link Headline}, {@link ContactLine}, and
 * {@link SectionHeader#tickLabel}.</p>
 */
public final class CompactMono {

    /**
     * Stable template identifier.
     */
    public static final String ID = "compact-mono";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Compact Mono";

    /**
     * Recommended page margin (in points).
     */
    public static final double RECOMMENDED_MARGIN = 20.0;

    private static final DocumentColor PAPER =
            DocumentColor.rgb(248, 250, 252);
    private static final DocumentColor HEADER =
            DocumentColor.rgb(18, 24, 32);
    private static final DocumentColor HEADER_SOFT =
            DocumentColor.rgb(192, 207, 219);
    private static final DocumentColor LINK_CYAN =
            DocumentColor.rgb(108, 213, 222);
    private static final DocumentColor SEPARATOR_GRAY =
            DocumentColor.rgb(102, 117, 132);
    private static final DocumentColor ACCENT =
            DocumentColor.rgb(0, 126, 151);
    private static final DocumentColor CARD_FILL =
            DocumentColor.rgb(255, 255, 255);

    private static final List<String> SUMMARY_KEYS =
            List.of("summary", "professional summary", "profile");
    private static final List<String> SKILL_KEYS =
            List.of("technical skills", "skills", "core skills");
    private static final List<String> EDUCATION_KEYS =
            List.of("education", "certifications");
    private static final List<String> EXPERIENCE_KEYS =
            List.of("experience", "professional experience", "employment", "work");
    private static final List<String> PROJECT_KEYS =
            List.of("projects", "project", "selected projects");
    private static final List<String> ADDITIONAL_KEYS =
            List.of("additional information", "additional");

    private CompactMono() {
    }

    /**
     * Builds the preset with its Compact Mono theme.
     *
     * @return ready-to-use template
     */
    public static DocumentTemplate<CvDocument> create() {
        return create(CvTheme.compactMono());
    }

    /**
     * Builds the preset with a caller-supplied theme. Use this for
     * controlled typography/spacing/palette variants without changing
     * the shipped preset.
     *
     * @param theme active theme
     * @return ready-to-use template
     */
    public static DocumentTemplate<CvDocument> create(CvTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new Template(theme);
    }

    private record Template(CvTheme theme) implements DocumentTemplate<CvDocument> {

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

                List<CvSection> sections = doc.sectionsIn(Slot.MAIN);
                PageFlowBuilder flow = document.dsl()
                        .pageFlow()
                        .name("CvV2CompactMonoRoot")
                        .spacing(theme.spacing().pageFlowSpacing())
                        .fillColor(PAPER);

                addHeader(flow, doc, document.canvas().innerWidth());
                flow.addRow("CvV2CompactMonoBody", row -> row
                        .spacing(14)
                        .weights(0.78, 1.62)
                        .addSection("Rail", rail -> addRail(rail, sections))
                        .addSection("Main", main -> addMain(main, sections)));
                flow.build();
            }

            private void addHeader(PageFlowBuilder flow, CvDocument doc,
                                   double width) {
                flow.addSection("CvV2CompactMonoHeader", section -> {
                    section.spacing(4)
                            .padding(new DocumentInsets(13, 16, 14, 16))
                            .fillColor(HEADER)
                            .cornerRadius(3);
                    section.addSection("Name", name ->
                            Headline.uppercaseLeftAligned(name,
                                    doc.identity().name(), theme,
                                    headerNameStyle()));
                    section.addSection("Contact", contact ->
                            ContactLine.leftAligned(contact, doc.identity(), theme,
                                    headerMetaStyle(), headerLinkStyle(),
                                    headerSeparatorStyle()));
                    section.addLine(line -> line
                            .name("CvV2CompactMonoHeaderWidthRule")
                            .horizontal(Math.max(0, width - 32))
                            .color(HEADER)
                            .thickness(0.1)
                            .margin(DocumentInsets.zero()));
                });
            }

            private void addRail(SectionBuilder rail, List<CvSection> sections) {
                rail.spacing(8)
                        .padding(new DocumentInsets(11, 11, 13, 11))
                        .fillColor(theme.palette().banner())
                        .accentLeft(ACCENT, 3.0);
                addRailSkills(rail, SectionLookup.firstMatching(sections, SKILL_KEYS));
                addRailEducation(rail, SectionLookup.firstMatching(sections, EDUCATION_KEYS));
                addRailAdditional(rail, SectionLookup.firstMatching(sections, ADDITIONAL_KEYS));
            }

            private void addMain(SectionBuilder main, List<CvSection> sections) {
                main.spacing(8);
                addProfile(main, SectionLookup.firstMatching(sections, SUMMARY_KEYS));
                addExperience(main, SectionLookup.firstMatching(sections, EXPERIENCE_KEYS));
                addProjects(main, SectionLookup.firstMatching(sections, PROJECT_KEYS));
            }

            private void addRailSkills(SectionBuilder parent, CvSection section) {
                if (!(section instanceof SkillsSection skills)
                    || skills.groups().isEmpty()) {
                    return;
                }

                SectionModule.tick(parent, "CvV2CompactMonoSkills", "Skills",
                        theme, ACCENT, 22, moduleLabelStyle(), host -> {
                            for (SkillGroup group : skills.groups()) {
                                addSkillLine(host, group);
                            }
                        });
            }

            private void addRailEducation(SectionBuilder parent, CvSection section) {
                if (!(section instanceof EntriesSection education)
                    || education.entries().isEmpty()) {
                    return;
                }

                SectionModule.tick(parent, "CvV2CompactMonoEducation", "Education",
                        theme, ACCENT, 22, moduleLabelStyle(), host -> {
                            for (CvEntry entry : education.entries()) {
                                EntryCompactRenderer.slashSubtitleDate(host, entry,
                                        bodyBoldStyle(7.25),
                                        bodyStyle(7.15, theme.palette().ink()),
                                        bodyStyle(7.0, theme.palette().muted()),
                                        1.02, DocumentInsets.bottom(2.0));
                            }
                        });
            }

            private void addRailAdditional(SectionBuilder parent, CvSection section) {
                if (!(section instanceof RowsSection rows) || rows.rows().isEmpty()) {
                    return;
                }

                SectionModule.tick(parent, "CvV2CompactMonoAdditional", "Additional",
                        theme, ACCENT, 22, moduleLabelStyle(), host -> {
                            for (CvRow row : rows.rows()) {
                                addRailLabelValue(host, row.label(), row.body());
                            }
                        });
            }

            private void addProfile(SectionBuilder parent, CvSection section) {
                if (!(section instanceof ParagraphSection profile)
                    || profile.body().isBlank()) {
                    return;
                }
                addCard(parent, "CvV2CompactMonoProfile", "Profile", card ->
                        RichParagraphRenderer.render(card, profile.body(),
                                bodyStyle(8.05, theme.palette().ink()),
                                1.18, DocumentInsets.zero()));
            }

            private void addExperience(SectionBuilder parent, CvSection section) {
                if (!(section instanceof EntriesSection entries)
                    || entries.entries().isEmpty()) {
                    return;
                }
                addCard(parent, "CvV2CompactMonoExperience", "Experience", card -> {
                    for (CvEntry entry : entries.entries()) {
                        addExperienceEntry(card, entry);
                    }
                });
            }

            private void addProjects(SectionBuilder parent, CvSection section) {
                if (!(section instanceof RowsSection projects)
                    || projects.rows().isEmpty()) {
                    return;
                }
                addCard(parent, "CvV2CompactMonoProjects", "Selected Projects", card -> {
                    for (CvRow row : projects.rows()) {
                        addProject(card, row);
                    }
                });
            }

            private void addCard(SectionBuilder parent, String name, String title,
                                 Consumer<SectionBuilder> content) {
                CardWidget.render(parent, name, CardWidget.Style.builder()
                        .spacing(4)
                        .padding(new DocumentInsets(9, 10, 10, 11))
                        .fillColor(CARD_FILL)
                        .stroke(DocumentStroke.of(theme.palette().rule(), 0.35))
                        .cornerRadius(3)
                        .build(), card -> {
                    moduleLabel(card, title, 24);
                    content.accept(card);
                });
            }

            private void moduleLabel(SectionBuilder host, String title, double tickWidth) {
                SectionHeader.tickLabel(host, title, theme, ACCENT, tickWidth,
                        moduleLabelStyle());
            }

            private void addRailLabelValue(SectionBuilder host, String label,
                                           String value) {
                LabelValueRenderer.render(host, label, value, bodyBoldStyle(7.2),
                        bodyStyle(7.2, theme.palette().muted()), 1.05,
                        DocumentInsets.bottom(2.0));
            }

            private void addSkillLine(SectionBuilder host, SkillGroup group) {
                SkillLineRenderer.limitedInline(host, group, 4,
                        bodyBoldStyle(7.55),
                        bodyStyle(7.45, theme.palette().ink()),
                        0.98, DocumentInsets.bottom(2.0), " ");
            }

            private void addExperienceEntry(SectionBuilder host, CvEntry entry) {
                EntryCompactRenderer.titleSubtitleDateBody(host, entry,
                        bodyBoldStyle(8.0),
                        bodyStyle(8.0, theme.palette().ink()),
                        bodyStyle(7.75, theme.palette().muted()),
                        bodyStyle(7.95, theme.palette().ink()),
                        ", ", " | ", 1.08,
                        DocumentInsets.bottom(1.2),
                        DocumentInsets.bottom(theme.spacing().entrySeparation()),
                        1.14);
            }

            private void addProject(SectionBuilder host, CvRow row) {
                ProjectRenderer.inline(host, row, bodyBoldStyle(8.0),
                        italicStyle(7.75, theme.palette().muted()),
                        bodyStyle(7.95, theme.palette().ink()),
                        1.12, DocumentInsets.bottom(3.0));
            }

            private DocumentTextStyle headerNameStyle() {
                return CvTextStyles.of(theme.typography().headlineFont(),
                        theme.typography().sizeHeadline(),
                        DocumentTextDecoration.BOLD,
                        DocumentColor.rgb(255, 255, 255));
            }

            private DocumentTextStyle headerMetaStyle() {
                return CvTextStyles.of(theme.typography().bodyFont(),
                        theme.typography().sizeContact(),
                        DocumentTextDecoration.DEFAULT,
                        HEADER_SOFT);
            }

            private DocumentTextStyle headerLinkStyle() {
                return CvTextStyles.of(theme.typography().bodyFont(),
                        theme.typography().sizeContact(),
                        DocumentTextDecoration.UNDERLINE,
                        LINK_CYAN);
            }

            private DocumentTextStyle headerSeparatorStyle() {
                return CvTextStyles.of(theme.typography().bodyFont(),
                        theme.typography().sizeContact(),
                        DocumentTextDecoration.DEFAULT,
                        SEPARATOR_GRAY);
            }

            private DocumentTextStyle moduleLabelStyle() {
                return CvTextStyles.of(theme.typography().headlineFont(),
                        theme.typography().sizeBanner(),
                        DocumentTextDecoration.BOLD,
                        ACCENT);
            }

            private DocumentTextStyle bodyStyle(double size, DocumentColor color) {
                return CvTextStyles.of(theme.typography().bodyFont(), size,
                        DocumentTextDecoration.DEFAULT, color);
            }

            private DocumentTextStyle bodyBoldStyle(double size) {
                return CvTextStyles.of(theme.typography().bodyFont(), size,
                        DocumentTextDecoration.BOLD, theme.palette().ink());
            }

            private DocumentTextStyle italicStyle(double size, DocumentColor color) {
                return CvTextStyles.of(theme.typography().bodyFont(), size,
                        DocumentTextDecoration.ITALIC, color);
            }
        }

}
