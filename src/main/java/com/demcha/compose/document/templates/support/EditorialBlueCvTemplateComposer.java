package com.demcha.compose.document.templates.support;

import com.demcha.compose.document.model.node.TextAlign;
import com.demcha.compose.document.templates.data.MainPageCV;
import com.demcha.compose.document.templates.data.MainPageCvDTO;
import com.demcha.compose.document.templates.data.ModuleSummary;
import com.demcha.compose.document.templates.data.ModuleYml;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.layout_core.components.components_builders.TableCellSpec;
import com.demcha.compose.layout_core.components.components_builders.TableCellStyle;
import com.demcha.compose.layout_core.components.components_builders.TableColumnSpec;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.content.text.TextDecoration;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared scene composer for the editorial blue CV template.
 */
public final class EditorialBlueCvTemplateComposer {
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

    public EditorialBlueCvTemplateComposer(CvTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    public void compose(TemplateComposeTarget target, MainPageCV originalCv, MainPageCvDTO rewrittenCv) {
        MainPageCV data = rewrittenCv == null ? originalCv : rewrittenCv.merge(originalCv);
        target.startDocument("EditorialBlueRoot", 7);
        addHeader(target, data);
        addProfile(target, data.getModuleSummary());
        addExperience(target, data.getProfessionalExperience());
        addProjects(target, data.getProjects());
        addEducation(target, data.getEducationCertifications());
        addSkills(target, data.getTechnicalSkills());
        addFooter(target);
        target.finishDocument();
    }

    private void addHeader(TemplateComposeTarget target, MainPageCV data) {
        String name = data.getHeader() == null ? "" : Objects.requireNonNullElse(data.getHeader().getName(), "");
        target.addParagraph(TemplateSceneSupport.paragraph(
                "EditorialBlueName",
                name.toUpperCase(Locale.ROOT),
                nameStyle(),
                TextAlign.CENTER,
                1.0,
                Padding.zero(),
                Margin.zero()));

        String headline = extractHeadline(data.getModuleSummary());
        if (!headline.isBlank()) {
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "EditorialBlueHeadline",
                    headline,
                    headlineStyle(),
                    TextAlign.CENTER,
                    1.0,
                    Padding.zero(),
                    Margin.zero()));
        }

        if (data.getHeader() != null) {
            String meta = TemplateSceneSupport.joinNonBlank(" - ",
                    data.getHeader().getPhoneNumber(),
                    data.getHeader().getEmail() == null ? "" : data.getHeader().getEmail().getDisplayText(),
                    data.getHeader().getAddress());
            if (!meta.isBlank()) {
                target.addParagraph(TemplateSceneSupport.paragraph(
                        "EditorialBlueMeta",
                        meta,
                        metaStyle(),
                        TextAlign.CENTER,
                        1.0,
                        Padding.zero(),
                        Margin.top(1)));
            }
        }

        target.addDivider(TemplateSceneSupport.divider(
                "EditorialBlueHeaderRule",
                target.pageWidth(),
                1.5,
                accentColor(),
                Margin.top(6)));
    }

    private void addProfile(TemplateComposeTarget target, ModuleSummary summary) {
        if (summary == null) {
            return;
        }
        sectionHeader(target, "EditorialBlueProfile", "PROFESSIONAL PROFILE", Margin.top(3));
        target.addParagraph(TemplateSceneSupport.paragraph(
                "EditorialBlueProfileBody",
                stripMarkdown(summary.getBlockSummary()),
                bodyStyle(),
                TextAlign.LEFT,
                2.0,
                Padding.zero(),
                Margin.top(3)));
    }

    private void addExperience(TemplateComposeTarget target, ModuleYml experience) {
        if (experience == null || experience.getModulePoints().isEmpty()) {
            return;
        }
        sectionHeader(target, "EditorialBlueExperience", "EMPLOYMENT HISTORY", Margin.top(4));
        int index = 0;
        for (ExperienceEntry entry : parseExperienceEntries(experience.getModulePoints())) {
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "EditorialBlueExperienceTitle_" + index,
                    TemplateSceneSupport.joinNonBlank(" | ", entry.role(), entry.company(), entry.dateRange()),
                    roleStyle(),
                    TextAlign.LEFT,
                    1.0,
                    Padding.zero(),
                    Margin.top(index == 0 ? 3 : 4)));
            if (!entry.details().isEmpty()) {
                target.addParagraph(TemplateSceneSupport.paragraph(
                        "EditorialBlueExperienceBody_" + index,
                        TemplateSceneSupport.bulletText(entry.details()),
                        bodyStyle(),
                        TextAlign.LEFT,
                        2.0,
                        Padding.zero(),
                        Margin.top(2)));
            }
            index++;
        }
    }

    private void addProjects(TemplateComposeTarget target, ModuleYml projects) {
        if (projects == null || projects.getModulePoints().isEmpty()) {
            return;
        }
        sectionHeader(target, "EditorialBlueProjects", "PROJECTS", Margin.top(4));
        int index = 0;
        for (ProjectEntry entry : parseProjectEntries(projects.getModulePoints()).stream().limit(2).toList()) {
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "EditorialBlueProjectTitle_" + index,
                    TemplateSceneSupport.joinNonBlank(" | ", entry.title(), entry.stack()),
                    educationTitleStyle(),
                    TextAlign.LEFT,
                    1.0,
                    Padding.zero(),
                    Margin.top(index == 0 ? 3 : 4)));
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "EditorialBlueProjectBody_" + index,
                    entry.description(),
                    bodyStyle(),
                    TextAlign.LEFT,
                    2.0,
                    Padding.zero(),
                    Margin.top(1)));
            index++;
        }
    }

    private void addEducation(TemplateComposeTarget target, ModuleYml education) {
        if (education == null || education.getModulePoints().isEmpty()) {
            return;
        }
        sectionHeader(target, "EditorialBlueEducation", "EDUCATION", Margin.top(4));
        int index = 0;
        for (EducationEntry entry : parseEducationEntries(education.getModulePoints())) {
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "EditorialBlueEducationTitle_" + index,
                    TemplateSceneSupport.joinNonBlank(" | ", entry.title(), entry.organization(), entry.dateRange(), entry.note()),
                    educationTitleStyle(),
                    TextAlign.LEFT,
                    1.0,
                    Padding.zero(),
                    Margin.top(index == 0 ? 3 : 3)));
            index++;
        }
    }

    private void addSkills(TemplateComposeTarget target, ModuleYml skillsModule) {
        List<String> skills = extractSkillTokens(skillsModule);
        if (skills.isEmpty()) {
            return;
        }
        sectionHeader(target, "EditorialBlueSkills", "KEY SKILLS", Margin.top(4));
        target.addTable(skillsTable(target, skills));
    }

    private void addFooter(TemplateComposeTarget target) {
        target.addDivider(TemplateSceneSupport.divider(
                "EditorialBlueFooterRule",
                target.pageWidth(),
                0.8,
                mutedBorderColor(),
                Margin.top(4)));
        target.addParagraph(TemplateSceneSupport.paragraph(
                "EditorialBlueFooter",
                "References available upon request.",
                footerStyle(),
                TextAlign.CENTER,
                1.0,
                Padding.zero(),
                Margin.top(3)));
    }

    private void sectionHeader(TemplateComposeTarget target, String prefix, String title, Margin margin) {
        TemplateSceneSupport.addSectionHeader(
                target,
                prefix,
                title,
                sectionHeaderStyle(),
                target.pageWidth(),
                accentColor(),
                1.25,
                margin);
    }

    private TemplateTableSpec skillsTable(TemplateComposeTarget target, List<String> skills) {
        double columnWidth = target.pageWidth() / SKILL_COLUMNS;
        List<String> padded = new ArrayList<>(skills);
        while (padded.size() % SKILL_COLUMNS != 0) {
            padded.add("");
        }
        List<List<TableCellSpec>> rows = new ArrayList<>();
        for (int index = 0; index < padded.size(); index += SKILL_COLUMNS) {
            rows.add(List.of(
                    TableCellSpec.text(decorateSkill(padded.get(index))),
                    TableCellSpec.text(decorateSkill(padded.get(index + 1))),
                    TableCellSpec.text(decorateSkill(padded.get(index + 2))),
                    TableCellSpec.text(decorateSkill(padded.get(index + 3)))));
        }
        TableCellStyle defaultStyle = TableCellStyle.builder()
                .padding(new Padding(5, 8, 5, 8))
                .fillColor(skillFillColor())
                .stroke(new Stroke(mutedBorderColor(), 1.1))
                .textStyle(skillStyle())
                .textAnchor(Anchor.centerLeft())
                .build();
        return new TemplateTableSpec(
                "EditorialBlueSkillsTable",
                List.of(
                        TableColumnSpec.fixed(columnWidth),
                        TableColumnSpec.fixed(columnWidth),
                        TableColumnSpec.fixed(columnWidth),
                        TableColumnSpec.fixed(columnWidth)),
                rows,
                defaultStyle,
                java.util.Map.of(),
                java.util.Map.of(),
                target.pageWidth(),
                Padding.zero(),
                Margin.top(3));
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
        return token.length() > 28 ? "" : token;
    }

    private String extractHeadline(ModuleSummary summary) {
        if (summary == null) {
            return "";
        }
        Matcher matcher = BOLD_PATTERN.matcher(safe(summary.getBlockSummary()));
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
