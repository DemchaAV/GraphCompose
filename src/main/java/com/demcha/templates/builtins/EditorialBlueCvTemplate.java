package com.demcha.templates.builtins;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.font_library.FontName;
import com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.layout_core.components.components_builders.BlockTextBuilder;
import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.layout_core.components.components_builders.ElementBuilder;
import com.demcha.compose.layout_core.components.components_builders.HContainerBuilder;
import com.demcha.compose.layout_core.components.components_builders.ModuleBuilder;
import com.demcha.compose.layout_core.components.components_builders.RectangleBuilder;
import com.demcha.compose.layout_core.components.components_builders.TextBuilder;
import com.demcha.compose.layout_core.components.components_builders.VContainerBuilder;
import com.demcha.compose.layout_core.components.content.text.TextDecoration;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.Canvas;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.templates.CvTheme;
import com.demcha.templates.api.CvTemplate;
import com.demcha.templates.api.MainPageCvDTO;
import com.demcha.templates.data.MainPageCV;
import com.demcha.templates.data.ModuleSummary;
import com.demcha.templates.data.ModuleYml;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.awt.Color;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class EditorialBlueCvTemplate implements CvTemplate {
    private static final String ROOT_NAME = "EditorialBlueRoot";
    private static final String HEADER_NAME = "EditorialBlueHeader";
    private static final String SUMMARY_NAME = "EditorialBlueSummary";
    private static final String EXPERIENCE_NAME = "EditorialBlueExperience";
    private static final String PROJECTS_NAME = "EditorialBlueProjects";
    private static final String EDUCATION_NAME = "EditorialBlueEducation";
    private static final String SKILLS_NAME = "EditorialBlueSkills";
    private static final String ADDITIONAL_NAME = "EditorialBlueAdditional";

    private static final float PAGE_MARGIN = 24f;
    private static final double ROOT_SPACING = 10;
    private static final double HEADER_SPACING = 4;
    private static final double SECTION_SPACING = 4;
    private static final double ENTRY_SPACING = 3;
    private static final double BLOCK_LINE_SPACING = 2;
    private static final double DIVIDER_HEIGHT = 1.6;
    private static final double MUTED_DIVIDER_HEIGHT = 0.8;
    private static final double SKILL_ROW_HEIGHT = 28;
    private static final int SKILL_COLUMNS = 4;
    private static final int MAX_GRID_SKILLS = 12;

    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile(
            "^\\*\\*(.+?)\\*\\*,\\s*(.+?)\\s*\\|\\s*\\*(.+?)\\*\\s*[\\u2013-]\\s*(.+)$");
    private static final Pattern PROJECT_PATTERN = Pattern.compile(
            "^\\*\\*(.+?)\\*\\*\\s*(?:\\*\\((.+?)\\)\\*)?\\s*[\\u2013-]\\s*(.+)$");
    private static final Pattern PAREN_PATTERN = Pattern.compile("\\([^)]*\\)");

    private final CvTheme theme;

    public EditorialBlueCvTemplate() {
        this(null);
    }

    public EditorialBlueCvTemplate(CvTheme theme) {
        this.theme = Objects.requireNonNullElseGet(theme, EditorialBlueCvTemplate::defaultTheme);
    }

    @Override
    public PDDocument render(MainPageCV originalCv, MainPageCvDTO rewrittenCv) {
        return render(originalCv, rewrittenCv, false);
    }

    @Override
    public PDDocument render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, boolean guideLines) {
        MainPageCV data = rewrittenCv.merge(originalCv);

        try {
            PdfComposer composer = createComposer(null, guideLines);
            designDocument(composer, data);
            return composer.toPDDocument();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate editorial CV", e);
        }
    }

    @Override
    public void render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, Path path) {
        render(originalCv, rewrittenCv, path, false);
    }

    @Override
    public void render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, Path path, boolean guideLines) {
        MainPageCV data = rewrittenCv.merge(originalCv);

        try (PdfComposer composer = createComposer(path, guideLines)) {
            designDocument(composer, data);
            composer.build();
            log.info("File has been saved to {}", path.toAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate editorial CV", e);
        }
    }

    @Override
    public String getTemplateId() {
        return "editorial-blue";
    }

    @Override
    public String getTemplateName() {
        return "Editorial Blue";
    }

    @Override
    public String getDescription() {
        return "A single-column editorial CV with centered header, blue section rules, and compact skills grid.";
    }

    private PdfComposer createComposer(Path path, boolean guideLines) {
        GraphCompose.PdfBuilder builder = path != null ? GraphCompose.pdf(path) : GraphCompose.pdf();
        return builder.pageSize(PDRectangle.A4)
                .margin(PAGE_MARGIN, PAGE_MARGIN, PAGE_MARGIN, PAGE_MARGIN)
                .markdown(true)
                .guideLines(guideLines)
                .create();
    }

    private void designDocument(PdfComposer composer, MainPageCV data) {
        Canvas canvas = composer.canvas();
        ComponentBuilder cb = composer.componentBuilder();
        EntityManager entityManager = cb.entityManager();
        double width = canvas.innerWidth();

        VContainerBuilder root = new VContainerBuilder(entityManager, Align.left(ROOT_SPACING))
                .entityName(ROOT_NAME)
                .size(width, 0)
                .anchor(Anchor.topLeft());

        root.addChild(createHeader(cb, data, width));
        root.addChild(createSummarySection(cb, data.getModuleSummary(), width));
        root.addChild(createExperienceSection(cb, data.getProfessionalExperience(), width));

        Entity projectsSection = createProjectsSection(cb, data.getProjects(), width);
        if (projectsSection != null) {
            root.addChild(projectsSection);
        }

        root.addChild(createEducationSection(cb, data.getEducationCertifications(), width));
        root.addChild(createSkillsSection(cb, data.getTechnicalSkills(), width));

        Entity additionalSection = createAdditionalSection(cb, data.getAdditional(), width);
        if (additionalSection != null) {
            root.addChild(additionalSection);
        }

        root.build();
    }

    private Entity createHeader(ComponentBuilder cb, MainPageCV data, double width) {
        String headline = extractHeadline(data.getModuleSummary());
        String contactLine = String.join(" | ",
                safe(data.getHeader().getPhoneNumber()),
                safe(data.getHeader().getEmail().getDisplayText()),
                safe(data.getHeader().getAddress()));
        String linksLine = String.join(" | ",
                safe(data.getHeader().getLinkedIn().getDisplayText()),
                safe(data.getHeader().getGitHub().getDisplayText()));

        VContainerBuilder header = cb.vContainer(Align.middle(HEADER_SPACING))
                .entityName(HEADER_NAME)
                .size(width, 0)
                .anchor(Anchor.topLeft())
                .margin(Margin.bottom(6));

        header.addChild(createSingleLineText(cb, data.getHeader().getName(), nameStyle(), Anchor.topCenter(), Margin.bottom(2)));
        header.addChild(createSingleLineText(cb, headline, headlineStyle(), Anchor.topCenter(), Margin.zero()));
        header.addChild(createSingleLineText(cb, contactLine, metaStyle(), Anchor.topCenter(), Margin.top(2)));
        header.addChild(createSingleLineText(cb, linksLine, linksStyle(), Anchor.topCenter(), Margin.zero()));
        header.addChild(createDivider(cb, width, accentColor(), DIVIDER_HEIGHT, Margin.top(8)));

        return header.build();
    }

    private Entity createSummarySection(ComponentBuilder cb, ModuleSummary summary, double width) {
        VContainerBuilder section = sectionContainer(cb, SUMMARY_NAME, width, "PROFESSIONAL PROFILE");
        section.addChild(createParagraph(cb, List.of(summary.getBlockSummary()), width, Margin.top(2)));
        return section.build();
    }

    private Entity createExperienceSection(ComponentBuilder cb, ModuleYml experience, double width) {
        VContainerBuilder section = sectionContainer(cb, EXPERIENCE_NAME, width, "EMPLOYMENT HISTORY");
        for (ExperienceEntry entry : parseExperienceEntries(experience.getModulePoints())) {
            section.addChild(createExperienceEntry(cb, entry, width));
        }
        return section.build();
    }

    private Entity createProjectsSection(ComponentBuilder cb, ModuleYml projects, double width) {
        if (projects == null || projects.getModulePoints().isEmpty()) {
            return null;
        }

        VContainerBuilder section = sectionContainer(cb, PROJECTS_NAME, width, "SELECTED PROJECTS");
        int limit = Math.min(2, projects.getModulePoints().size());
        for (int i = 0; i < limit; i++) {
            section.addChild(createProjectEntry(cb, parseProjectEntry(projects.getModulePoints().get(i)), width));
        }
        return section.build();
    }

    private Entity createEducationSection(ComponentBuilder cb, ModuleYml education, double width) {
        VContainerBuilder section = sectionContainer(cb, EDUCATION_NAME, width, "EDUCATION");
        section.addChild(createBulletParagraph(cb, education.getModulePoints(), width, Margin.top(2)));
        return section.build();
    }

    private Entity createSkillsSection(ComponentBuilder cb, ModuleYml skillsModule, double width) {
        VContainerBuilder section = sectionContainer(cb, SKILLS_NAME, width, "KEY SKILLS");
        List<String> skills = extractSkillTokens(skillsModule);
        if (skills.isEmpty()) {
            return section.build();
        }

        double separatorWidth = 1;
        double totalSeparatorWidth = separatorWidth * (SKILL_COLUMNS - 1);
        double cellWidth = (width - totalSeparatorWidth) / SKILL_COLUMNS;

        section.addChild(createDivider(cb, width, mutedBorderColor(), MUTED_DIVIDER_HEIGHT, Margin.top(2)));

        for (int index = 0; index < skills.size(); index += SKILL_COLUMNS) {
            List<String> row = skills.subList(index, Math.min(index + SKILL_COLUMNS, skills.size()));
            section.addChild(createSkillsRow(cb, row, width, cellWidth, separatorWidth));
            section.addChild(createDivider(cb, width, mutedBorderColor(), MUTED_DIVIDER_HEIGHT, Margin.zero()));
        }
        return section.build();
    }

    private Entity createAdditionalSection(ComponentBuilder cb, ModuleYml additional, double width) {
        if (additional == null || additional.getModulePoints().isEmpty()) {
            return null;
        }

        VContainerBuilder section = sectionContainer(cb, ADDITIONAL_NAME, width, "ADDITIONAL INFORMATION");
        section.addChild(createBulletParagraph(cb, additional.getModulePoints(), width, Margin.top(2)));
        return section.build();
    }

    private VContainerBuilder sectionContainer(ComponentBuilder cb, String entityName, double width, String title) {
        VContainerBuilder section = cb.vContainer(Align.left(SECTION_SPACING))
                .entityName(entityName)
                .size(width, 0)
                .anchor(Anchor.topLeft())
                .margin(Margin.bottom(4));
        section.addChild(createSectionHeader(cb, title, width));
        return section;
    }

    private Entity createSectionHeader(ComponentBuilder cb, String title, double width) {
        VContainerBuilder header = cb.vContainer(Align.left(2))
                .size(width, 0)
                .anchor(Anchor.topLeft());
        header.addChild(createSingleLineText(cb, title, sectionHeaderStyle(), Anchor.topLeft(), Margin.zero()));
        header.addChild(createDivider(cb, width, accentColor(), DIVIDER_HEIGHT, Margin.zero()));
        return header.build();
    }

    private Entity createExperienceEntry(ComponentBuilder cb, ExperienceEntry entry, double width) {
        VContainerBuilder container = cb.vContainer(Align.left(ENTRY_SPACING))
                .size(width, 0)
                .anchor(Anchor.topLeft())
                .margin(Margin.bottom(6));

        Entity role = createSingleLineText(cb, entry.role(), roleStyle(), Anchor.topLeft(), Margin.zero());
        Entity dates = createSingleLineText(cb, entry.dateRange(), dateStyle(), Anchor.topLeft(), Margin.zero());
        container.addChild(createBalancedRow(cb, dates, role, width));
        container.addChild(createSingleLineText(cb, entry.company(), companyStyle(), Anchor.topLeft(), Margin.zero()));
        container.addChild(createBulletParagraph(cb, entry.details(), width, Margin.zero()));

        return container.build();
    }

    private Entity createProjectEntry(ComponentBuilder cb, ProjectEntry entry, double width) {
        VContainerBuilder container = cb.vContainer(Align.left(ENTRY_SPACING))
                .size(width, 0)
                .anchor(Anchor.topLeft())
                .margin(Margin.bottom(6));

        container.addChild(createSingleLineText(cb, entry.title(), roleStyle(), Anchor.topLeft(), Margin.zero()));
        if (!entry.stack().isBlank()) {
            container.addChild(createSingleLineText(cb, entry.stack(), companyStyle(), Anchor.topLeft(), Margin.zero()));
        }
        container.addChild(createParagraph(cb, List.of(entry.description()), width, Margin.zero()));

        return container.build();
    }

    private Entity createSkillsRow(ComponentBuilder cb, List<String> skills, double width, double cellWidth, double separatorWidth) {
        HContainerBuilder row = cb.hContainer(Align.left(0))
                .size(width, SKILL_ROW_HEIGHT)
                .anchor(Anchor.topLeft());

        for (int index = 0; index < SKILL_COLUMNS; index++) {
            String skill = index < skills.size() ? skills.get(index) : "";
            row.addChild(createSkillCell(cb, skill, cellWidth));
            if (index < SKILL_COLUMNS - 1) {
                row.addChild(createDivider(cb, separatorWidth, mutedBorderColor(), SKILL_ROW_HEIGHT, Margin.zero()));
            }
        }
        return row.build();
    }

    private Entity createSkillCell(ComponentBuilder cb, String skill, double cellWidth) {
        VContainerBuilder cell = cb.vContainer(Align.left(0))
                .size(cellWidth, SKILL_ROW_HEIGHT)
                .anchor(Anchor.topLeft())
                .padding(Padding.of(4));

        if (!skill.isBlank()) {
            cell.addChild(createSingleLineText(cb, skill, skillStyle(), Anchor.centerLeft(), Margin.zero()));
        } else {
            cell.addChild(createSpacer(cb, cellWidth, 1));
        }
        return cell.build();
    }

    private Entity createBalancedRow(ComponentBuilder cb, Entity left, Entity right, double width) {
        double leftWidth = left.getComponent(ContentSize.class).map(ContentSize::width).orElse(0.0);
        double rightWidth = right.getComponent(ContentSize.class).map(ContentSize::width).orElse(0.0);
        double spacerWidth = Math.max(8.0, width - leftWidth - rightWidth);

        HContainerBuilder row = cb.hContainer(Align.left(0))
                .size(width, 0)
                .anchor(Anchor.topLeft());
        row.addChild(left);
        row.addChild(createSpacer(cb, spacerWidth, 1));
        row.addChild(right);
        return row.build();
    }

    private Entity createDivider(ComponentBuilder cb, double width, Color color, double height, Margin margin) {
        RectangleBuilder line = cb.rectangle()
                .size(width, height)
                .fillColor(color)
                .anchor(Anchor.topLeft())
                .margin(margin);
        return line.build();
    }

    private Entity createSpacer(ComponentBuilder cb, double width, double height) {
        ElementBuilder spacer = cb.element()
                .size(width, height)
                .anchor(Anchor.topLeft());
        return spacer.build();
    }

    private Entity createSingleLineText(ComponentBuilder cb, String text, TextStyle style, Anchor anchor, Margin margin) {
        TextBuilder builder = cb.text()
                .textWithAutoSize(text)
                .textStyle(style)
                .anchor(anchor)
                .margin(margin);
        return builder.build();
    }

    private Entity createParagraph(ComponentBuilder cb, List<String> paragraphs, double width, Margin margin) {
        BlockTextBuilder builder = cb.blockText(Align.left(BLOCK_LINE_SPACING), bodyStyle())
                .size(width, 2)
                .strategy(BlockIndentStrategy.FIRST_LINE)
                .anchor(Anchor.topLeft())
                .margin(margin)
                .padding(Padding.zero())
                .text(paragraphs, bodyStyle(), Padding.zero(), Margin.zero());
        return builder.build();
    }

    private Entity createBulletParagraph(ComponentBuilder cb, List<String> items, double width, Margin margin) {
        List<String> sanitized = items.stream()
                .filter(item -> item != null && !item.isBlank())
                .toList();

        BlockTextBuilder builder = cb.blockText(Align.left(BLOCK_LINE_SPACING), bodyStyle())
                .size(width, 2)
                .strategy(BlockIndentStrategy.FROM_SECOND_LINE)
                .bulletOffset("-")
                .anchor(Anchor.topLeft())
                .margin(margin)
                .padding(Padding.zero())
                .text(sanitized, bodyStyle(), Padding.zero(), Margin.zero());
        return builder.build();
    }

    private List<ExperienceEntry> parseExperienceEntries(List<String> items) {
        List<ExperienceEntry> result = new ArrayList<>();
        for (String item : items) {
            Matcher matcher = EXPERIENCE_PATTERN.matcher(item);
            if (matcher.matches()) {
                result.add(new ExperienceEntry(
                        stripMarkdown(matcher.group(1)),
                        stripMarkdown(matcher.group(2)),
                        stripMarkdown(matcher.group(3)),
                        splitDetails(matcher.group(4))));
            } else {
                result.add(new ExperienceEntry("Experience", "", "", List.of(stripMarkdown(item))));
            }
        }
        return result;
    }

    private ProjectEntry parseProjectEntry(String item) {
        Matcher matcher = PROJECT_PATTERN.matcher(item);
        if (!matcher.matches()) {
            return new ProjectEntry("Project", "", stripMarkdown(item));
        }

        String title = stripMarkdown(matcher.group(1));
        String stack = stripMarkdown(safe(matcher.group(2)));
        String description = stripMarkdown(matcher.group(3));
        return new ProjectEntry(title, stack, description);
    }

    private List<String> extractSkillTokens(ModuleYml technicalSkills) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String line : technicalSkills.getModulePoints()) {
            if (line == null || line.isBlank()) {
                continue;
            }

            String clean = stripMarkdown(line);
            int colonIndex = clean.indexOf(':');
            String values = colonIndex >= 0 ? clean.substring(colonIndex + 1) : clean;

            for (String rawToken : values.split(",")) {
                String token = PAREN_PATTERN.matcher(rawToken).replaceAll("").trim();
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

    private String extractHeadline(ModuleSummary summary) {
        Matcher matcher = BOLD_PATTERN.matcher(summary.getBlockSummary());
        if (matcher.find()) {
            return stripMarkdown(matcher.group(1));
        }
        String plain = stripMarkdown(summary.getBlockSummary());
        int period = plain.indexOf('.');
        return period > 0 ? plain.substring(0, period) : plain;
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

    private String sectionTitle(String value) {
        return safe(value).toUpperCase();
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
                .size(28)
                .decoration(TextDecoration.BOLD)
                .color(primaryTextColor())
                .build();
    }

    private TextStyle headlineStyle() {
        return TextStyle.builder()
                .fontName(theme.bodyFont())
                .size(12)
                .decoration(TextDecoration.DEFAULT)
                .color(primaryTextColor())
                .build();
    }

    private TextStyle metaStyle() {
        return TextStyle.builder()
                .fontName(theme.bodyFont())
                .size(10)
                .decoration(TextDecoration.DEFAULT)
                .color(bodyTextColor())
                .build();
    }

    private TextStyle linksStyle() {
        return TextStyle.builder()
                .fontName(theme.bodyFont())
                .size(9.5)
                .decoration(TextDecoration.DEFAULT)
                .color(accentColor())
                .build();
    }

    private TextStyle sectionHeaderStyle() {
        return TextStyle.builder()
                .fontName(theme.headerFont())
                .size(12)
                .decoration(TextDecoration.BOLD)
                .color(accentColor())
                .build();
    }

    private TextStyle roleStyle() {
        return TextStyle.builder()
                .fontName(theme.headerFont())
                .size(11.5)
                .decoration(TextDecoration.BOLD)
                .color(primaryTextColor())
                .build();
    }

    private TextStyle companyStyle() {
        return TextStyle.builder()
                .fontName(theme.bodyFont())
                .size(10)
                .decoration(TextDecoration.ITALIC)
                .color(bodyTextColor())
                .build();
    }

    private TextStyle dateStyle() {
        return TextStyle.builder()
                .fontName(theme.bodyFont())
                .size(10)
                .decoration(TextDecoration.DEFAULT)
                .color(bodyTextColor())
                .build();
    }

    private TextStyle bodyStyle() {
        return TextStyle.builder()
                .fontName(theme.bodyFont())
                .size(10)
                .decoration(TextDecoration.DEFAULT)
                .color(bodyTextColor())
                .build();
    }

    private TextStyle skillStyle() {
        return TextStyle.builder()
                .fontName(theme.bodyFont())
                .size(9.5)
                .decoration(TextDecoration.DEFAULT)
                .color(primaryTextColor())
                .build();
    }

    private Color primaryTextColor() {
        return new Color(18, 31, 72);
    }

    private Color bodyTextColor() {
        return theme.bodyColor();
    }

    private Color accentColor() {
        return new Color(62, 122, 255);
    }

    private Color mutedBorderColor() {
        return new Color(195, 205, 230);
    }

    private static CvTheme defaultTheme() {
        return new CvTheme(
                new Color(18, 31, 72),
                new Color(62, 122, 255),
                new Color(55, 71, 102),
                new Color(62, 122, 255),
                FontName.HELVETICA,
                FontName.HELVETICA,
                28,
                12,
                10,
                4,
                Margin.top(4),
                0);
    }

    private record ExperienceEntry(String role, String company, String dateRange, List<String> details) {
    }

    private record ProjectEntry(String title, String stack, String description) {
    }
}
