package com.demcha.compose.document.templates.support;

import com.demcha.compose.document.model.node.TextAlign;
import com.demcha.compose.document.templates.data.Header;
import com.demcha.compose.document.templates.data.MainPageCV;
import com.demcha.compose.document.templates.data.MainPageCvDTO;
import com.demcha.compose.document.templates.data.ModuleSummary;
import com.demcha.compose.document.templates.data.ModuleYml;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

import java.util.List;
import java.util.Objects;

/**
 * Shared scene composer for the standard CV template.
 *
 * @author Artem Demchyshyn
 */
public final class CvTemplateComposer {
    private static final Padding LEGACY_BLOCK_PADDING = new Padding(0, 5, 0, 20);
    private static final Margin LEGACY_HEADER_RIGHT_MARGIN = new Margin(0, 10, 0, 0);

    private final CvTheme theme;

    public CvTemplateComposer(CvTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    public void compose(TemplateComposeTarget target, MainPageCV originalCv, MainPageCvDTO rewrittenCv) {
        MainPageCV data = rewrittenCv == null ? originalCv : rewrittenCv.merge(originalCv);
        target.startDocument("MainVBoxContainer", theme.spacingModuleName());
        addHeader(target, data.getHeader());
        addSummary(target, data.getModuleSummary());
        addModule(target, "TechnicalSkills", data.getTechnicalSkills());
        addModule(target, "Education", data.getEducationCertifications());
        addModule(target, "Projects", data.getProjects());
        addModule(target, "Experience", data.getProfessionalExperience());
        addModule(target, "Additional", data.getAdditional());
        target.finishDocument();
    }

    private void addHeader(TemplateComposeTarget target, Header header) {
        if (header == null) {
            return;
        }

        target.addParagraph(TemplateSceneSupport.paragraph(
                "ModuleHeaderName",
                Objects.requireNonNullElse(header.getName(), ""),
                theme.nameTextStyle(),
                TextAlign.RIGHT,
                1.0,
                Padding.zero(),
                new Margin(0, 10, Math.max(0.0, theme.spacing() - 3), 0)));

        String info = TemplateSceneSupport.joinNonBlank(" | ",
                header.getAddress(),
                header.getPhoneNumber());
        if (!info.isBlank()) {
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "ModuleHeaderInfo",
                    info,
                    theme.smallBodyTextStyle(),
                    TextAlign.RIGHT,
                    1.0,
                    Padding.zero(),
                    LEGACY_HEADER_RIGHT_MARGIN));
        }

        String links = TemplateSceneSupport.joinNonBlank(" | ",
                header.getEmail() == null ? "" : header.getEmail().getDisplayText(),
                header.getLinkedIn() == null ? "" : header.getLinkedIn().getDisplayText(),
                header.getGitHub() == null ? "" : header.getGitHub().getDisplayText());
        if (!links.isBlank()) {
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "ModuleHeaderLinks",
                    links,
                    theme.linkTextStyle(),
                    TextAlign.RIGHT,
                    1.0,
                    Padding.zero(),
                    LEGACY_HEADER_RIGHT_MARGIN));
        }
    }

    private void addSummary(TemplateComposeTarget target, ModuleSummary summary) {
        if (summary == null) {
            return;
        }
        addModuleTitle(target, "SummaryHeading", summary.getModuleName());
        target.addParagraph(TemplateSceneSupport.blockParagraph(
                "SummaryBody",
                Objects.requireNonNullElse(summary.getBlockSummary(), ""),
                theme.bodyTextStyle(),
                TextAlign.LEFT,
                theme.spacing(),
                "    ",
                BlockIndentStrategy.FIRST_LINE,
                LEGACY_BLOCK_PADDING,
                Margin.zero()));
    }

    private void addModule(TemplateComposeTarget target, String prefix, ModuleYml module) {
        if (module == null || module.getModulePoints() == null || module.getModulePoints().isEmpty()) {
            return;
        }
        List<String> points = TemplateSceneSupport.sanitizeLines(module.getModulePoints());
        if (points.isEmpty()) {
            return;
        }
        if ("TechnicalSkills".equals(prefix)) {
            List<String> normalizedPoints = normalizeTechnicalSkillPoints(points);
            if (normalizedPoints.isEmpty()) {
                return;
            }
            addModuleTitle(target, prefix + "Heading", module.getName());
            target.addParagraph(TemplateSceneSupport.blockParagraph(
                    prefix + "Body",
                    String.join("\n", normalizedPoints),
                    theme.bodyTextStyle(),
                    TextAlign.LEFT,
                    theme.spacing(),
                    "\u2022 ",
                    BlockIndentStrategy.ALL_LINES,
                    LEGACY_BLOCK_PADDING,
                    Margin.zero()));
            return;
        }
        addModuleTitle(target, prefix + "Heading", module.getName());
        target.addParagraph(TemplateSceneSupport.blockParagraph(
                prefix + "Body",
                String.join("\n", points),
                theme.bodyTextStyle(),
                TextAlign.LEFT,
                theme.spacing(),
                "  ",
                BlockIndentStrategy.FROM_SECOND_LINE,
                LEGACY_BLOCK_PADDING,
                Margin.zero()));
    }

    private static List<String> normalizeTechnicalSkillPoints(List<String> points) {
        return points.stream()
                .map(CvTemplateComposer::stripLeadingListMarker)
                .filter(point -> !point.isBlank())
                .toList();
    }

    private static String stripLeadingListMarker(String value) {
        String normalized = Objects.requireNonNullElse(value, "").trim();
        if (normalized.startsWith("\u2022")) {
            return normalized.substring(1).trim();
        }
        if (normalized.startsWith("- ")) {
            return normalized.substring(2).trim();
        }
        if (normalized.startsWith("* ") && !normalized.startsWith("**")) {
            return normalized.substring(2).trim();
        }
        return normalized;
    }

    private void addModuleTitle(TemplateComposeTarget target, String name, String title) {
        target.addParagraph(TemplateSceneSupport.paragraph(
                name,
                Objects.requireNonNullElse(title, ""),
                theme.sectionHeaderTextStyle(),
                TextAlign.LEFT,
                1.0,
                Padding.zero(),
                Margin.of(5)));
    }
}
