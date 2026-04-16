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
 */
public final class CvTemplateComposer {
    private final CvTheme theme;

    public CvTemplateComposer(CvTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    public void compose(TemplateComposeTarget target, MainPageCV originalCv, MainPageCvDTO rewrittenCv) {
        MainPageCV data = rewrittenCv == null ? originalCv : rewrittenCv.merge(originalCv);
        target.startDocument("MainVBoxContainer", theme.spacing());
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
                TextAlign.CENTER,
                1.0,
                Padding.zero(),
                Margin.bottom((float) Math.max(0.0, theme.spacing() - 3))));

        String info = TemplateSceneSupport.joinNonBlank(" | ",
                header.getAddress(),
                header.getPhoneNumber());
        if (!info.isBlank()) {
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "ModuleHeaderInfo",
                    info,
                    theme.smallBodyTextStyle(),
                    TextAlign.CENTER,
                    1.0,
                    Padding.zero(),
                    Margin.zero()));
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
                    TextAlign.CENTER,
                    1.0,
                    Padding.zero(),
                    Margin.zero()));
        }
    }

    private void addSummary(TemplateComposeTarget target, ModuleSummary summary) {
        if (summary == null) {
            return;
        }
        TemplateSceneSupport.addSectionHeader(
                target,
                "Summary",
                summary.getModuleName(),
                theme.sectionHeaderTextStyle(),
                Math.min(target.pageWidth(), 140),
                theme.accentColor(),
                1.0,
                theme.moduleMargin());
        target.addParagraph(TemplateSceneSupport.blockParagraph(
                "SummaryBody",
                TemplateSceneSupport.stripBasicMarkdown(Objects.requireNonNullElse(summary.getBlockSummary(), "")),
                theme.bodyTextStyle(),
                TextAlign.LEFT,
                2.0,
                "    ",
                BlockIndentStrategy.FIRST_LINE,
                Padding.zero(),
                Margin.top(3)));
    }

    private void addModule(TemplateComposeTarget target, String prefix, ModuleYml module) {
        if (module == null || module.getModulePoints() == null || module.getModulePoints().isEmpty()) {
            return;
        }
        TemplateSceneSupport.addSectionHeader(
                target,
                prefix,
                module.getName(),
                theme.sectionHeaderTextStyle(),
                Math.min(target.pageWidth(), 140),
                theme.accentColor(),
                1.0,
                theme.moduleMargin());
        List<String> points = TemplateSceneSupport.sanitizeLines(module.getModulePoints());
        for (int index = 0; index < points.size(); index++) {
            String point = points.get(index);
            String bulletOffset = "TechnicalSkills".equals(prefix) ? "• " : "  ";
            BlockIndentStrategy indentStrategy = "TechnicalSkills".equals(prefix)
                    ? BlockIndentStrategy.ALL_LINES
                    : BlockIndentStrategy.FROM_SECOND_LINE;
            target.addParagraph(TemplateSceneSupport.blockParagraph(
                    prefix + "Body_" + index,
                    point,
                    theme.bodyTextStyle(),
                    TextAlign.LEFT,
                    2.0,
                    bulletOffset,
                    indentStrategy,
                    Padding.zero(),
                    Margin.top(index == 0 ? 3 : 1)));
        }
    }
}
