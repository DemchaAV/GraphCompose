package com.demcha.templates.builtins;

import com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.layout_core.components.components_builders.BlockTextBuilder;
import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.layout_core.components.components_builders.ElementBuilder;
import com.demcha.compose.layout_core.components.components_builders.HContainerBuilder;
import com.demcha.compose.layout_core.components.components_builders.ModuleBuilder;
import com.demcha.compose.layout_core.components.components_builders.TableCellStyle;
import com.demcha.compose.layout_core.components.components_builders.TableColumnSpec;
import com.demcha.compose.layout_core.components.components_builders.TextBuilder;
import com.demcha.compose.layout_core.components.components_builders.VContainerBuilder;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.content.text.TextDecoration;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.Canvas;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.templates.CvTheme;
import com.demcha.templates.api.MainPageCvDTO;
import com.demcha.templates.data.MainPageCV;
import com.demcha.templates.data.ModuleSummary;
import com.demcha.templates.data.ModuleYml;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Backend-neutral scene builder for the Editorial Blue CV template.
 */
final class EditorialBlueCvSceneBuilder {
    private static final String ROOT_NAME = "EditorialBlueRoot";
    private static final String HEADER_NAME = "EditorialBlueHeader";
    private static final String PROFILE_NAME = "EditorialBlueProfile";
    private static final String EXPERIENCE_NAME = "EditorialBlueExperience";
    private static final String PROJECTS_NAME = "EditorialBlueProjects";
    private static final String EDUCATION_NAME = "EditorialBlueEducation";
    private static final String SKILLS_NAME = "EditorialBlueSkills";
    private static final String FOOTER_NAME = "EditorialBlueFooter";

    private static final double ROOT_SPACING = 7;
    private static final double HEADER_SPACING = 2;
    private static final double SECTION_SPACING = 3;
    private static final double ENTRY_SPACING = 1.8;
    private static final double BODY_LINE_SPACING = 1.8;
    private static final double DETAIL_INDENT = 14;
    private static final double HEADER_NAME_MAX_WIDTH = 440;
    private static final double HEADER_HEADLINE_MAX_WIDTH = 470;
    private static final double HEADER_META_MAX_WIDTH = 410;
    private static final double RULE_HEIGHT = 6;
    private static final double SECTION_RULE_STROKE = 1.25;
    private static final double HEADER_RULE_STROKE = 1.5;
    private static final double FOOTER_RULE_STROKE = 0.8;
    private static final double DATE_COLUMN_WIDTH = 126;
    private static final double HEADER_ROW_GAP = 8;
    private static final int SKILL_COLUMNS = 4;
    private static final int MAX_GRID_SKILLS = 12;

    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile(
            "^\\*\\*(.+?)\\*\\*,\\s*(.+?)\\s*\\|\\s*\\*(.+?)\\*\\s*[\\u2013-]\\s*(.+)$");
    private static final Pattern PROJECT_PATTERN = Pattern.compile(
            "^\\*\\*(.+?)\\*\\*\\s*(?:\\*\\((.+?)\\)\\*)?\\s*[\\u2013-]\\s*(.+)$");
    private static final Pattern EDUCATION_PATTERN = Pattern.compile(
            "^\\*\\*(.+?)\\*\\*\\s*[\\u2013-]\\s*(.+?)(?:\\s*\\|\\s*(.+?))?(?:\\s+-\\s+(.+))?$");
    private static final Pattern PAREN_PATTERN = Pattern.compile("\\([^)]*\\)");

    private final CvTheme theme;

    EditorialBlueCvSceneBuilder(CvTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    void compose(DocumentComposer composer, MainPageCV originalCv, MainPageCvDTO rewrittenCv) {
        designDocument(composer, rewrittenCv.merge(originalCv));
    }

    private void designDocument(DocumentComposer composer, MainPageCV data) {
        Canvas canvas = composer.canvas();
        ComponentBuilder cb = composer.componentBuilder();
        double width = canvas.innerWidth();

        VContainerBuilder root = cb.vContainer(Align.left(ROOT_SPACING))
                .entityName(ROOT_NAME)
                .size(width, 0)
                .anchor(Anchor.topLeft());

        root.addChild(createHeader(cb, data, width));
        root.addChild(createProfileSection(cb, data.getModuleSummary(), width));
        root.addChild(createExperienceSection(cb, data.getProfessionalExperience(), width));

        Entity projectsSection = createProjectsSection(cb, data.getProjects(), width);
        if (projectsSection != null) {
            root.addChild(projectsSection);
        }

        Entity educationSection = createEducationSection(cb, data.getEducationCertifications(), width);
        if (educationSection != null) {
            root.addChild(educationSection);
        }

        Entity skillsSection = createSkillsSection(cb, data.getTechnicalSkills(), width);
        if (skillsSection != null) {
            root.addChild(skillsSection);
        }

        root.addChild(createFooter(cb, width));
        root.build();
    }

    private Entity createHeader(ComponentBuilder cb, MainPageCV data, double width) {
        ModuleBuilder header = cb.moduleBuilder(Align.middle(HEADER_SPACING))
                .entityName(HEADER_NAME)
                .anchor(Anchor.topLeft())
                .margin(Margin.bottom(2));

        header.addChild(createCenteredParagraph(
                cb,
                List.of(safe(data.getHeader().getName()).toUpperCase(Locale.ROOT)),
                Math.min(width, HEADER_NAME_MAX_WIDTH),
                nameStyle(),
                Margin.zero(),
                "EditorialBlueName"));

        String headline = extractHeadline(data.getModuleSummary());
        if (!headline.isBlank()) {
            header.addChild(createCenteredParagraph(
                    cb,
                    List.of(headline),
                    Math.min(width, HEADER_HEADLINE_MAX_WIDTH),
                    headlineStyle(),
                    Margin.zero(),
                    "EditorialBlueHeadline"));
        }

        String metaLine = joinNonBlank(" - ",
                safe(data.getHeader().getPhoneNumber()),
                safe(data.getHeader().getEmail().getDisplayText()),
                safe(data.getHeader().getAddress()));
        if (!metaLine.isBlank()) {
            header.addChild(createCenteredParagraph(
                    cb,
                    List.of(metaLine),
                    Math.min(width, HEADER_META_MAX_WIDTH),
                    metaStyle(),
                    Margin.top(1),
                    "EditorialBlueMeta"));
        }

        header.addChild(createRule(cb, width, accentColor(), HEADER_RULE_STROKE, Margin.top(6)));
        return header.build();
    }

    private Entity createProfileSection(ComponentBuilder cb, ModuleSummary summary, double width) {
        ModuleBuilder section = sectionContainer(cb, PROFILE_NAME, width, "PROFESSIONAL PROFILE");
        section.addChild(createParagraph(
                cb,
                List.of(stripMarkdown(summary.getBlockSummary())),
                width,
                Margin.top(2)));
        return section.build();
    }

    private Entity createExperienceSection(ComponentBuilder cb, ModuleYml experience, double width) {
        ModuleBuilder section = sectionContainer(cb, EXPERIENCE_NAME, width, "EMPLOYMENT HISTORY");
        for (ExperienceEntry entry : parseExperienceEntries(experience.getModulePoints())) {
            section.addChild(createExperienceEntry(cb, entry, width));
        }
        return section.build();
    }

    private Entity createProjectsSection(ComponentBuilder cb, ModuleYml projects, double width) {
        if (projects == null || projects.getModulePoints().isEmpty()) {
            return null;
        }

        ModuleBuilder section = sectionContainer(cb, PROJECTS_NAME, width, "PROJECTS");
        List<ProjectEntry> entries = parseProjectEntries(projects.getModulePoints());
        int limit = Math.min(2, entries.size());

        for (int index = 0; index < limit; index++) {
            section.addChild(createProjectEntry(cb, entries.get(index), width));
        }

        return section.build();
    }

    private Entity createEducationSection(ComponentBuilder cb, ModuleYml education, double width) {
        if (education == null || education.getModulePoints().isEmpty()) {
            return null;
        }

        ModuleBuilder section = sectionContainer(cb, EDUCATION_NAME, width, "EDUCATION");
        for (EducationEntry entry : parseEducationEntries(education.getModulePoints())) {
            section.addChild(createEducationEntry(cb, entry, width));
        }
        return section.build();
    }

    private Entity createSkillsSection(ComponentBuilder cb, ModuleYml skillsModule, double width) {
        List<String> skills = extractSkillTokens(skillsModule);
        if (skills.isEmpty()) {
            return null;
        }

        ModuleBuilder section = sectionContainer(cb, SKILLS_NAME, width, "KEY SKILLS");
        section.addChild(createSkillsTable(cb, skills, width));
        return section.build();
    }

    private Entity createFooter(ComponentBuilder cb, double width) {
        ModuleBuilder footer = cb.moduleBuilder(Align.middle(2))
                .entityName(FOOTER_NAME)
                .anchor(Anchor.topLeft())
                .margin(Margin.top(2));

        footer.addChild(createRule(cb, width, mutedBorderColor(), FOOTER_RULE_STROKE, Margin.zero()));
        footer.addChild(createSingleLineText(
                cb,
                "References available upon request.",
                footerStyle(),
                Anchor.topCenter(),
                Margin.top(3)));

        return footer.build();
    }

    private ModuleBuilder sectionContainer(ComponentBuilder cb, String entityName, double width, String title) {
        ModuleBuilder section = cb.moduleBuilder(Align.left(SECTION_SPACING))
                .entityName(entityName)
                .anchor(Anchor.topLeft())
                .margin(Margin.bottom(2));
        section.addChild(createSectionHeader(cb, title, width));
        return section;
    }

    private Entity createSectionHeader(ComponentBuilder cb, String title, double width) {
        VContainerBuilder header = cb.vContainer(Align.left(1))
                .size(width, 0)
                .anchor(Anchor.topLeft());
        header.addChild(createSingleLineText(cb, title, sectionHeaderStyle(), Anchor.topLeft(), Margin.zero()));
        header.addChild(createRule(cb, width, accentColor(), SECTION_RULE_STROKE, Margin.zero()));
        return header.build();
    }

    private Entity createExperienceEntry(ComponentBuilder cb, ExperienceEntry entry, double width) {
        VContainerBuilder container = cb.vContainer(Align.left(ENTRY_SPACING))
                .size(width, 0)
                .anchor(Anchor.topLeft())
                .margin(Margin.bottom(4));

        container.addChild(createHeaderRow(
                cb,
                entry.role(),
                roleStyle(),
                entry.dateRange(),
                dateStyle(),
                width));

        if (!entry.company().isBlank()) {
            container.addChild(createSingleLineText(cb, entry.company(), companyStyle(), Anchor.topLeft(), Margin.zero()));
        }

        if (!entry.details().isEmpty()) {
            container.addChild(createBulletParagraph(
                    cb,
                    entry.details(),
                    width - DETAIL_INDENT,
                    new Margin(0, 0, 0, DETAIL_INDENT)));
        }

        return container.build();
    }

    private Entity createProjectEntry(ComponentBuilder cb, ProjectEntry entry, double width) {
        VContainerBuilder container = cb.vContainer(Align.left(1.6))
                .size(width, 0)
                .anchor(Anchor.topLeft())
                .margin(Margin.bottom(4));

        container.addChild(createSingleLineText(cb, entry.title(), educationTitleStyle(), Anchor.topLeft(), Margin.zero()));

        if (!entry.stack().isBlank()) {
            container.addChild(createSingleLineText(cb, entry.stack(), companyStyle(), Anchor.topLeft(), Margin.zero()));
        }

        container.addChild(createParagraph(cb, List.of(entry.description()), width, Margin.zero()));
        return container.build();
    }

    private Entity createEducationEntry(ComponentBuilder cb, EducationEntry entry, double width) {
        VContainerBuilder container = cb.vContainer(Align.left(1.2))
                .size(width, 0)
                .anchor(Anchor.topLeft())
                .margin(Margin.bottom(3));

        container.addChild(createHeaderRow(
                cb,
                entry.title(),
                educationTitleStyle(),
                entry.dateRange(),
                dateStyle(),
                width));

        if (!entry.organization().isBlank()) {
            container.addChild(createSingleLineText(cb, entry.organization(), companyStyle(), Anchor.topLeft(), Margin.zero()));
        }

        return container.build();
    }

    private Entity createSkillsTable(ComponentBuilder cb, List<String> skills, double width) {
        double columnWidth = width / SKILL_COLUMNS;
        List<String> paddedSkills = new ArrayList<>(skills);
        while (paddedSkills.size() % SKILL_COLUMNS != 0) {
            paddedSkills.add("");
        }

        TextStyle skillStyle = skillStyle();
        var table = cb.table()
                .entityName(SKILLS_NAME + "Table")
                .anchor(Anchor.topLeft())
                .columns(
                        TableColumnSpec.fixed(columnWidth),
                        TableColumnSpec.fixed(columnWidth),
                        TableColumnSpec.fixed(columnWidth),
                        TableColumnSpec.fixed(columnWidth))
                .width(width)
                .defaultCellStyle(TableCellStyle.builder()
                        .padding(new Padding(5, 8, 5, 8))
                        .fillColor(skillFillColor())
                        .stroke(new Stroke(mutedBorderColor(), 1.1))
                        .textStyle(skillStyle)
                        .textAnchor(Anchor.centerLeft())
                        .build());

        for (int index = 0; index < paddedSkills.size(); index += SKILL_COLUMNS) {
            table.row(
                    decorateSkill(paddedSkills.get(index)),
                    decorateSkill(paddedSkills.get(index + 1)),
                    decorateSkill(paddedSkills.get(index + 2)),
                    decorateSkill(paddedSkills.get(index + 3)));
        }

        return table.build();
    }

    private Entity createHeaderRow(ComponentBuilder cb,
                                   String leftText,
                                   TextStyle leftStyle,
                                   String rightText,
                                   TextStyle rightStyle,
                                   double width) {
        Entity left = createSingleLineText(cb, leftText, leftStyle, Anchor.topLeft(), Margin.zero());
        if (safe(rightText).isBlank()) {
            return left;
        }

        Entity right = createSingleLineText(cb, rightText, rightStyle, Anchor.topRight(), Margin.zero());
        double leftColumnWidth = Math.max(80.0, width - DATE_COLUMN_WIDTH - HEADER_ROW_GAP);

        HContainerBuilder row = cb.hContainer(Align.left(0))
                .size(width, 0)
                .anchor(Anchor.topLeft());

        row.addChild(createAlignedCell(cb, left, leftColumnWidth, Anchor.topLeft()));
        row.addChild(createSpacer(cb, HEADER_ROW_GAP, 1));
        row.addChild(createAlignedCell(cb, right, DATE_COLUMN_WIDTH, Anchor.topRight()));
        return row.build();
    }

    private Entity createAlignedCell(ComponentBuilder cb, Entity child, double width, Anchor childAnchor) {
        VContainerBuilder cell = cb.vContainer(Align.left(0))
                .size(width, 0)
                .anchor(Anchor.topLeft());

        child.addComponent(childAnchor);
        cell.addChild(child);
        return cell.build();
    }

    private Entity createRule(ComponentBuilder cb, double width, Color color, double strokeWidth, Margin margin) {
        return cb.line()
                .horizontal()
                .size(width, RULE_HEIGHT)
                .padding(Padding.of(1))
                .stroke(new Stroke(color, strokeWidth))
                .anchor(Anchor.topLeft())
                .margin(margin)
                .build();
    }

    private Entity createSpacer(ComponentBuilder cb, double width, double height) {
        ElementBuilder spacer = cb.element()
                .size(width, height)
                .anchor(Anchor.topLeft());
        return spacer.build();
    }

    private Entity createSingleLineText(ComponentBuilder cb, String text, TextStyle style, Anchor anchor, Margin margin) {
        TextBuilder builder = cb.text()
                .textWithAutoSize(safe(text))
                .textStyle(style)
                .anchor(anchor)
                .margin(margin);
        return builder.build();
    }

    private Entity createParagraph(ComponentBuilder cb, List<String> paragraphs, double width, Margin margin) {
        List<String> sanitized = paragraphs.stream()
                .map(EditorialBlueCvSceneBuilder::stripMarkdown)
                .filter(value -> !value.isBlank())
                .toList();

        BlockTextBuilder builder = cb.blockText(Align.left(BODY_LINE_SPACING), bodyStyle())
                .size(width, 2)
                .strategy(BlockIndentStrategy.FIRST_LINE)
                .anchor(Anchor.topLeft())
                .margin(margin)
                .padding(Padding.zero())
                .text(sanitized, bodyStyle(), Padding.zero(), Margin.zero());
        return builder.build();
    }

    private Entity createCenteredParagraph(ComponentBuilder cb,
                                           List<String> paragraphs,
                                           double width,
                                           TextStyle style,
                                           Margin margin,
                                           String entityName) {
        List<String> sanitized = paragraphs.stream()
                .map(EditorialBlueCvSceneBuilder::stripMarkdown)
                .filter(value -> !value.isBlank())
                .toList();

        BlockTextBuilder builder = cb.blockText(Align.middle(1.2), style)
                .entityName(entityName)
                .size(width, 2)
                .strategy(BlockIndentStrategy.FIRST_LINE)
                .anchor(Anchor.topCenter())
                .margin(margin)
                .padding(Padding.zero())
                .text(sanitized, style, Padding.zero(), Margin.zero());
        return builder.build();
    }

    private Entity createBulletParagraph(ComponentBuilder cb, List<String> items, double width, Margin margin) {
        List<String> sanitized = items.stream()
                .map(EditorialBlueCvSceneBuilder::stripMarkdown)
                .filter(item -> item != null && !item.isBlank())
                .toList();

        BlockTextBuilder builder = cb.blockText(Align.left(BODY_LINE_SPACING), bodyStyle())
                .size(width, 2)
                .strategy(BlockIndentStrategy.FROM_SECOND_LINE)
                .bulletOffset("•")
                .anchor(Anchor.topLeft())
                .margin(margin)
                .padding(Padding.zero())
                .text(sanitized, bodyStyle(), Padding.zero(), Margin.zero());
        return builder.build();
    }

    private List<ExperienceEntry> parseExperienceEntries(List<String> items) {
        List<ExperienceEntry> result = new ArrayList<>();
        for (String item : items) {
            Matcher matcher = EXPERIENCE_PATTERN.matcher(safe(item));
            if (matcher.matches()) {
                result.add(new ExperienceEntry(
                        stripMarkdown(matcher.group(1)),
                        stripMarkdown(matcher.group(2)),
                        stripMarkdown(matcher.group(3)),
                        splitDetails(matcher.group(4))));
            } else if (!safe(item).isBlank()) {
                result.add(new ExperienceEntry("Experience", "", "", List.of(stripMarkdown(item))));
            }
        }
        return result;
    }

    private List<ProjectEntry> parseProjectEntries(List<String> items) {
        List<ProjectEntry> result = new ArrayList<>();
        for (String item : items) {
            Matcher matcher = PROJECT_PATTERN.matcher(safe(item));
            if (matcher.matches()) {
                result.add(new ProjectEntry(
                        stripMarkdown(matcher.group(1)),
                        stripMarkdown(safe(matcher.group(2))),
                        compactProjectDescription(matcher.group(3))));
            } else if (!safe(item).isBlank()) {
                result.add(new ProjectEntry("Project", "", stripMarkdown(item)));
            }
        }
        return result;
    }

    private List<EducationEntry> parseEducationEntries(List<String> items) {
        List<EducationEntry> result = new ArrayList<>();
        for (String item : items) {
            Matcher matcher = EDUCATION_PATTERN.matcher(safe(item));
            if (matcher.matches()) {
                result.add(new EducationEntry(
                        stripMarkdown(matcher.group(1)),
                        stripMarkdown(safe(matcher.group(2))),
                        stripMarkdown(safe(matcher.group(3))),
                        stripMarkdown(safe(matcher.group(4)))));
            } else if (!safe(item).isBlank()) {
                result.add(new EducationEntry(stripMarkdown(item), "", "", ""));
            }
        }
        return result;
    }

    private List<String> extractSkillTokens(ModuleYml technicalSkills) {
        if (technicalSkills == null) {
            return List.of();
        }

        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String line : technicalSkills.getModulePoints()) {
            if (line == null || line.isBlank()) {
                continue;
            }

            String clean = stripMarkdown(line);
            int colonIndex = clean.indexOf(':');
            String values = colonIndex >= 0 ? clean.substring(colonIndex + 1) : clean;

            for (String rawToken : values.split(",")) {
                String token = normalizeSkillToken(rawToken);
                if (!token.isBlank()) {
                    tokens.add(token);
                }
                if (tokens.size() >= MAX_GRID_SKILLS) {
                    return List.copyOf(tokens);
                }
            }
        }
        return List.copyOf(tokens);
    }

    private String normalizeSkillToken(String rawToken) {
        String token = PAREN_PATTERN.matcher(safe(rawToken)).replaceAll("").trim();
        if (token.isBlank()) {
            return "";
        }

        token = token.replace("Swagger/OpenAPI", "OpenAPI");
        token = token.replace("REST design", "REST API Design");
        token = token.replace("Multithreading/Concurrency", "Concurrency");
        token = token.replace("Git/GitHub", "Git & GitHub");
        token = token.replace("Spring Data JPA", "Spring Data JPA");
        token = token.replace("Docker Compose", "Docker");

        if (token.length() > 24) {
            token = token.replace("stateless authentication", "Authentication");
            token = token.replace("role-based authorization", "Authorization");
            token = token.replace("request validation", "Validation");
        }

        return token.length() > 28 ? "" : token;
    }

    private String extractHeadline(ModuleSummary summary) {
        Matcher matcher = BOLD_PATTERN.matcher(summary.getBlockSummary());
        if (matcher.find()) {
            return stripMarkdown(matcher.group(1));
        }
        String plain = stripMarkdown(summary.getBlockSummary());
        int period = plain.indexOf('.');
        return period > 0 ? plain.substring(0, period) : plain;
    }

    private String compactProjectDescription(String rawDescription) {
        String normalized = stripMarkdown(rawDescription).trim();
        int stackIndex = normalized.indexOf("Stack:");
        if (stackIndex >= 0) {
            normalized = normalized.substring(0, stackIndex).trim();
        }

        int lastSentenceEnd = normalized.indexOf(". ", Math.min(40, normalized.length()));
        if (lastSentenceEnd > 0) {
            normalized = normalized.substring(0, lastSentenceEnd + 1).trim();
        }

        return normalized;
    }

    private List<String> splitDetails(String rawDetails) {
        String normalized = stripMarkdown(rawDetails);
        List<String> details = new ArrayList<>();
        for (String part : normalized.split("\\s*;\\s*")) {
            if (!part.isBlank()) {
                details.add(part.trim());
            }
        }
        if (details.isEmpty() && !normalized.isBlank()) {
            details.add(normalized.trim());
        }
        return details;
    }

    private String decorateSkill(String skill) {
        return skill.isBlank() ? "" : "• " + skill;
    }

    private static String stripMarkdown(String value) {
        return safe(value)
                .replace("**", "")
                .replace("*", "")
                .replace("`", "")
                .replace("_", "");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private String joinNonBlank(String delimiter, String... values) {
        List<String> nonBlank = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                nonBlank.add(value.trim());
            }
        }
        return String.join(delimiter, nonBlank);
    }

    private TextStyle nameStyle() {
        return TextStyle.builder()
                .fontName(theme.headerFont())
                .size(21.2)
                .decoration(TextDecoration.BOLD)
                .color(primaryTextColor())
                .build();
    }

    private TextStyle headlineStyle() {
        return TextStyle.builder()
                .fontName(theme.bodyFont())
                .size(11.5)
                .decoration(TextDecoration.DEFAULT)
                .color(primaryTextColor())
                .build();
    }

    private TextStyle metaStyle() {
        return TextStyle.builder()
                .fontName(theme.bodyFont())
                .size(9.1)
                .decoration(TextDecoration.DEFAULT)
                .color(bodyTextColor())
                .build();
    }

    private TextStyle sectionHeaderStyle() {
        return TextStyle.builder()
                .fontName(theme.headerFont())
                .size(10.6)
                .decoration(TextDecoration.BOLD)
                .color(accentColor())
                .build();
    }

    private TextStyle roleStyle() {
        return TextStyle.builder()
                .fontName(theme.headerFont())
                .size(11.4)
                .decoration(TextDecoration.BOLD)
                .color(primaryTextColor())
                .build();
    }

    private TextStyle educationTitleStyle() {
        return TextStyle.builder()
                .fontName(theme.headerFont())
                .size(10.4)
                .decoration(TextDecoration.BOLD)
                .color(primaryTextColor())
                .build();
    }

    private TextStyle companyStyle() {
        return TextStyle.builder()
                .fontName(theme.bodyFont())
                .size(9.6)
                .decoration(TextDecoration.ITALIC)
                .color(bodyTextColor())
                .build();
    }

    private TextStyle dateStyle() {
        return TextStyle.builder()
                .fontName(theme.bodyFont())
                .size(9.2)
                .decoration(TextDecoration.DEFAULT)
                .color(bodyTextColor())
                .build();
    }

    private TextStyle bodyStyle() {
        return TextStyle.builder()
                .fontName(theme.bodyFont())
                .size(9.6)
                .decoration(TextDecoration.DEFAULT)
                .color(bodyTextColor())
                .build();
    }

    private TextStyle skillStyle() {
        return TextStyle.builder()
                .fontName(theme.bodyFont())
                .size(9.2)
                .decoration(TextDecoration.DEFAULT)
                .color(primaryTextColor())
                .build();
    }

    private TextStyle footerStyle() {
        return TextStyle.builder()
                .fontName(theme.bodyFont())
                .size(9.0)
                .decoration(TextDecoration.ITALIC)
                .color(bodyTextColor())
                .build();
    }

    private Color primaryTextColor() {
        return new Color(18, 31, 72);
    }

    private Color bodyTextColor() {
        return new Color(60, 72, 106);
    }

    private Color accentColor() {
        return new Color(86, 136, 255);
    }

    private Color mutedBorderColor() {
        return new Color(174, 190, 219);
    }

    private Color skillFillColor() {
        return new Color(245, 247, 252);
    }

    private record ExperienceEntry(String role, String company, String dateRange, List<String> details) {
    }

    private record ProjectEntry(String title, String stack, String description) {
    }

    private record EducationEntry(String title, String organization, String dateRange, String note) {
    }
}
