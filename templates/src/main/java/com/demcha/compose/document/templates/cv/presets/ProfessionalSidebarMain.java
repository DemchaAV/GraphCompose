package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.core.text.MarkdownInline;
import com.demcha.compose.document.templates.cv.components.SectionLookup;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.ACCENT_PRIMARY;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.BODY_FONT;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.BODY_LEADING;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.DISPLAY_FONT;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.DIVIDER_TO_ENTRY;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.DIVIDER_TO_PROJECT;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.ENTRY_HEAD_TO_META;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.ENTRY_META_TO_HIGHLIGHTS;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.ENTRY_TO_DIVIDER;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EXPERIENCE_HEADING_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EXPERIENCE_TO_PROJECTS;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.HIGHLIGHT_ITEM_GAP;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.IDENTITY_ACCENT_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.IDENTITY_TO_PROFILE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.MAIN_CONTENT_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.MAIN_HEADING_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.MAIN_PAD_LEFT;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.MAIN_PAD_RIGHT;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.MAIN_PAD_TOP;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.NAME_SIZE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.NAME_TO_ROLE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.NAME_TRACKING_EM;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.PROFESSIONAL_TITLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.PROFILE_TO_EXPERIENCE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.PROJECTS_TO_REFERENCES;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.PROJECT_HEADING_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.PROJECT_HEAD_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.PROJECT_TO_DIVIDER;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.REFERENCES_HEADING_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.REFERENCES_SIZE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.ROLE_TO_RULE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.ROLE_TRACKING_EM;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.RULE_MUTED;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.SECTION_ACCENT_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.TEXT_MUTED;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.TEXT_PRIMARY;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.body;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.metaItalic;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.style;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.tracked;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarWidgets.mainDivider;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarWidgets.mainHeading;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarWidgets.spacer;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarWidgets.titleDateBand;

/**
 * The white right column: the name and role over the identity rule, then the
 * profile, the roles held, the projects and the references note.
 */
final class ProfessionalSidebarMain {

    private ProfessionalSidebarMain() {
    }

    static void compose(SectionBuilder section,
                        CvIdentity identity,
                        ParagraphSection profile,
                        EntriesSection experience,
                        EntriesSection projects,
                        ParagraphSection references) {
        section.spacing(0);
        section.padding(new DocumentInsets(MAIN_PAD_TOP, MAIN_PAD_RIGHT, 0, MAIN_PAD_LEFT));
        section.addSection("Identity", host -> renderIdentity(host, identity));
        if (SectionLookup.hasContent(profile)) {
            section.addSection("Profile", host -> renderProfile(host, profile));
        }
        if (SectionLookup.hasContent(experience)) {
            section.addSection("Experience", host -> renderExperience(host, experience));
        }
        if (SectionLookup.hasContent(projects)) {
            section.addSection("Projects", host -> renderProjects(host, projects));
        }
        if (SectionLookup.hasContent(references)) {
            section.addSection("References", host -> renderReferences(host, references));
        }
    }

    /**
     * The column as blocks that move to another page whole, for a CV longer
     * than the sheet: the identity, the profile, each role, each project and
     * the references. A heading rides on the first entry it heads, and the
     * hairline between two entries is the second one's lead, so it is left
     * out when that entry opens a page.
     */
    static List<ColumnPages.Block> blocks(CvIdentity identity,
                                          ParagraphSection profile,
                                          EntriesSection experience,
                                          EntriesSection projects,
                                          ParagraphSection references) {
        List<ColumnPages.Block> blocks = new ArrayList<>();
        blocks.add(ColumnPages.Block.of("Identity",
                column -> column.addSection("Identity", host -> renderIdentity(host, identity))));
        if (SectionLookup.hasContent(profile)) {
            blocks.add(ColumnPages.Block.of("Profile",
                    column -> column.addSection("Profile", host -> renderProfile(host, profile))));
        }
        if (SectionLookup.hasContent(experience)) {
            for (int i = 0; i < experience.entries().size(); i++) {
                blocks.add(roleBlock(experience, i));
            }
        }
        if (SectionLookup.hasContent(projects)) {
            for (int i = 0; i < projects.entries().size(); i++) {
                blocks.add(projectBlock(projects, i));
            }
        }
        if (SectionLookup.hasContent(references)) {
            blocks.add(ColumnPages.Block.of("References",
                    column -> column.addSection("References",
                            host -> renderReferences(host, references))));
        }
        return blocks;
    }

    /**
     * One page of the column: the padding it carries on every page, then the
     * blocks the plan put there.
     */
    static void composePage(SectionBuilder section, List<ColumnPages.Block> blocks,
                            List<Integer> page) {
        section.spacing(0);
        section.padding(new DocumentInsets(MAIN_PAD_TOP, MAIN_PAD_RIGHT, 0, MAIN_PAD_LEFT));
        ColumnPages.compose(section, blocks, page);
    }

    private static ColumnPages.Block roleBlock(EntriesSection experience, int index) {
        List<CvEntry> entries = experience.entries();
        String name = "Experience_" + index;
        return new ColumnPages.Block(name,
                index == 0 ? null : column -> mainDivider(column, ENTRY_TO_DIVIDER, DIVIDER_TO_ENTRY),
                column -> column.addSection(name, host -> {
                    host.spacing(0);
                    if (index == 0) {
                        mainHeading(host, experience.title(), EXPERIENCE_HEADING_TO_BODY);
                    }
                    renderRole(host, entries.get(index), index);
                    if (index == entries.size() - 1) {
                        spacer(host, EXPERIENCE_TO_PROJECTS);
                    }
                }));
    }

    private static ColumnPages.Block projectBlock(EntriesSection projects, int index) {
        List<CvEntry> entries = projects.entries();
        String name = "Projects_" + index;
        return new ColumnPages.Block(name,
                index == 0 ? null : column -> mainDivider(column, PROJECT_TO_DIVIDER, DIVIDER_TO_PROJECT),
                column -> column.addSection(name, host -> {
                    host.spacing(0);
                    if (index == 0) {
                        mainHeading(host, projects.title(), PROJECT_HEADING_TO_BODY);
                    }
                    renderProject(host, entries.get(index), index);
                    if (index == entries.size() - 1) {
                        spacer(host, PROJECTS_TO_REFERENCES);
                    }
                }));
    }

    // -- identity --------------------------------------------------------

    /**
     * The name in tracked condensed capitals, the role under it, and the
     * rule that closes the block: a full-width hairline whose first stretch
     * is the accent.
     */
    private static void renderIdentity(SectionBuilder section, CvIdentity identity) {
        DocumentTextStyle nameStyle = style(DISPLAY_FONT, NAME_SIZE, TEXT_PRIMARY,
                DocumentTextDecoration.BOLD);
        ParagraphBuilder name = new ParagraphBuilder()
                .name("DisplayName")
                .textStyle(nameStyle)
                .align(TextAlign.LEFT);
        tracked(name, identity.name().full(), nameStyle, NAME_TRACKING_EM);
        section.add(name.margin(new DocumentInsets(0, 0, NAME_TO_ROLE, 0)).build());

        DocumentTextStyle roleStyle = style(BODY_FONT, PROFESSIONAL_TITLE_SIZE, TEXT_MUTED,
                DocumentTextDecoration.DEFAULT);
        ParagraphBuilder role = new ParagraphBuilder()
                .name("ProfessionalTitle")
                .textStyle(roleStyle)
                .align(TextAlign.LEFT);
        tracked(role, identity.jobTitle(), roleStyle, ROLE_TRACKING_EM);
        section.add(role.margin(new DocumentInsets(0, 0, ROLE_TO_RULE, 0)).build());

        section.add(identityRule());
        spacer(section, IDENTITY_TO_PROFILE);
    }

    private static DocumentNode identityRule() {
        DocumentNode accent = new ShapeBuilder()
                .name("IdentityAccentSegment")
                .size(IDENTITY_ACCENT_WIDTH, SECTION_ACCENT_HEIGHT)
                .fillColor(ACCENT_PRIMARY)
                .build();
        return new ShapeContainerBuilder()
                .name("IdentityRule")
                .rectangle(MAIN_CONTENT_WIDTH, SECTION_ACCENT_HEIGHT)
                .fillColor(RULE_MUTED)
                .centerLeft(accent)
                .build();
    }

    // -- profile ---------------------------------------------------------

    private static void renderProfile(SectionBuilder section, ParagraphSection profile) {
        mainHeading(section, profile.title(), MAIN_HEADING_TO_BODY);
        section.addParagraph(p -> p
                .name("ProfileText")
                .text(profile.body())
                .textStyle(body())
                .lineSpacing(BODY_LEADING)
                .margin(DocumentInsets.zero()));
        spacer(section, PROFILE_TO_EXPERIENCE);
    }

    // -- experience ------------------------------------------------------

    /**
     * The roles held, each a title-and-dates band over the employer line and
     * a bulleted list of what the role delivered, separated by hairlines.
     */
    private static void renderExperience(SectionBuilder section, EntriesSection experience) {
        mainHeading(section, experience.title(), EXPERIENCE_HEADING_TO_BODY);
        List<CvEntry> entries = experience.entries();
        for (int i = 0; i < entries.size(); i++) {
            renderRole(section, entries.get(i), i);
            if (i + 1 < entries.size()) {
                mainDivider(section, ENTRY_TO_DIVIDER, DIVIDER_TO_ENTRY);
            }
        }
        spacer(section, EXPERIENCE_TO_PROJECTS);
    }

    private static void renderRole(SectionBuilder section, CvEntry entry, int index) {
        titleDateBand(section, "ExperienceHead_" + index, entry.title(), entry.date());
        section.addParagraph(p -> p
                .name("ExperienceMeta_" + index)
                .text(entry.subtitle())
                .textStyle(metaItalic())
                .margin(new DocumentInsets(
                        ENTRY_HEAD_TO_META, 0, ENTRY_META_TO_HIGHLIGHTS, 0)));
        List<String> highlights = lines(entry.body());
        if (!highlights.isEmpty()) {
            section.addList(list -> list
                    .name("ExperienceHighlights_" + index)
                    .items(highlights)
                    .bullet()
                    .textStyle(body())
                    .lineSpacing(BODY_LEADING)
                    .itemSpacing(HIGHLIGHT_ITEM_GAP)
                    .margin(DocumentInsets.zero()));
        }
    }

    // -- projects --------------------------------------------------------

    private static void renderProjects(SectionBuilder section, EntriesSection projects) {
        mainHeading(section, projects.title(), PROJECT_HEADING_TO_BODY);
        List<CvEntry> entries = projects.entries();
        for (int i = 0; i < entries.size(); i++) {
            renderProject(section, entries.get(i), i);
            if (i + 1 < entries.size()) {
                mainDivider(section, PROJECT_TO_DIVIDER, DIVIDER_TO_PROJECT);
            }
        }
        spacer(section, PROJECTS_TO_REFERENCES);
    }

    private static void renderProject(SectionBuilder section, CvEntry project, int index) {
        titleDateBand(section, "ProjectHead_" + index, project.title(), project.date());
        spacer(section, PROJECT_HEAD_TO_BODY);
        section.addParagraph(p -> p
                .name("ProjectDescription_" + index)
                .text(project.body())
                .textStyle(body())
                .lineSpacing(BODY_LEADING)
                .margin(DocumentInsets.zero()));
    }

    // -- references ------------------------------------------------------

    private static void renderReferences(SectionBuilder section, ParagraphSection references) {
        mainHeading(section, references.title(), REFERENCES_HEADING_TO_BODY);
        section.addParagraph(p -> p
                .name("ReferencesNote")
                .text(references.body())
                .textStyle(style(BODY_FONT, REFERENCES_SIZE, TEXT_MUTED,
                        DocumentTextDecoration.DEFAULT))
                .margin(DocumentInsets.zero()));
    }

    /**
     * One bullet per non-blank line of an entry body — the family's way of
     * carrying a list in a field the model types as a single string.
     */
    private static List<String> lines(String body) {
        List<String> out = new ArrayList<>();
        for (String line : body.split("\\R")) {
            String clean = MarkdownInline.plainText(line).trim();
            if (!clean.isBlank()) {
                out.add(clean);
            }
        }
        return out;
    }
}
