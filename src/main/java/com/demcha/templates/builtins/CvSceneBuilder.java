package com.demcha.templates.builtins;

import com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.layout_core.components.content.link.Email;
import com.demcha.compose.layout_core.components.content.link.LinkUrl;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.core.Canvas;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.templates.CvTheme;
import com.demcha.templates.TemplateBuilder;
import com.demcha.templates.api.MainPageCvDTO;
import com.demcha.templates.data.MainPageCV;

import java.util.List;
import java.util.Objects;

/**
 * Backend-neutral scene builder for the built-in CV V1 template.
 */
final class CvSceneBuilder {
    private static final String MAIN_CONTAINER_NAME = "MainVBoxContainer";
    private static final String HEADER_ENTITY_NAME = "ModuleHeader";
    private static final String DEFAULT_BULLET_OFFSET = "  ";
    private static final String SUMMARY_BULLET_OFFSET = "    ";
    private static final String SKILLS_BULLET = "• ";

    private final CvTheme theme;

    CvSceneBuilder(CvTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    void compose(DocumentComposer composer, MainPageCV originalCv, MainPageCvDTO rewrittenCv) {
        MainPageCV data = rewrittenCv.merge(originalCv);
        designDocument(composer, data);
    }

    private void designDocument(DocumentComposer composer, MainPageCV data) {
        Canvas canvas = composer.canvas();
        TemplateBuilder cv = TemplateBuilder.from(composer.componentBuilder(), theme);
        float textBlockWidth = (float) canvas.innerWidth();

        Entity moduleHeader = createHeader(cv, data, canvas);
        Entity moduleProfessionalSummary = createSection(
                cv,
                canvas,
                data.getModuleSummary().getModuleName(),
                "ModuleProfessionalSummary",
                List.of(data.getModuleSummary().getBlockSummary()),
                textBlockWidth,
                SUMMARY_BULLET_OFFSET,
                BlockIndentStrategy.FIRST_LINE);
        Entity moduleTechnicalSkills = createSection(
                cv,
                canvas,
                data.getTechnicalSkills().getName(),
                "ModuleTechnicalSkills",
                data.getTechnicalSkills().getModulePoints(),
                textBlockWidth,
                SKILLS_BULLET,
                BlockIndentStrategy.ALL_LINES);
        Entity moduleEducationCertifications = createSection(
                cv,
                canvas,
                data.getEducationCertifications().getName(),
                "moduleEducationCertifications",
                data.getEducationCertifications().getModulePoints(),
                textBlockWidth,
                DEFAULT_BULLET_OFFSET,
                BlockIndentStrategy.FROM_SECOND_LINE);
        Entity moduleProjects = createSection(
                cv,
                canvas,
                data.getProjects().getName(),
                "ModuleProjects",
                data.getProjects().getModulePoints(),
                textBlockWidth,
                DEFAULT_BULLET_OFFSET,
                BlockIndentStrategy.FROM_SECOND_LINE);
        Entity moduleProfessionalExperience = createSection(
                cv,
                canvas,
                data.getProfessionalExperience().getName(),
                "ModuleProfessionalExperience",
                data.getProfessionalExperience().getModulePoints(),
                textBlockWidth,
                DEFAULT_BULLET_OFFSET,
                BlockIndentStrategy.FROM_SECOND_LINE);
        Entity moduleAdditional = createSection(
                cv,
                canvas,
                data.getAdditional().getName(),
                "ModuleAdditional",
                data.getAdditional().getModulePoints(),
                textBlockWidth,
                DEFAULT_BULLET_OFFSET,
                BlockIndentStrategy.FROM_SECOND_LINE);

        cv.pageFlow(canvas)
                .entityName(MAIN_CONTAINER_NAME)
                .addChild(moduleHeader)
                .addChild(moduleProfessionalSummary)
                .addChild(moduleTechnicalSkills)
                .addChild(moduleEducationCertifications)
                .addChild(moduleProjects)
                .addChild(moduleProfessionalExperience)
                .addChild(moduleAdditional)
                .build();
    }

    private Entity createHeader(TemplateBuilder cv, MainPageCV data, Canvas canvas) {
        var number = data.getHeader().getPhoneNumber();
        var address = data.getHeader().getAddress();
        var email = data.getHeader().getEmail();
        var linkedIn = data.getHeader().getLinkedIn();
        var gitHub = data.getHeader().getGitHub();

        Entity artemDemchyshyn = cv.name(data.getHeader().getName());

        Entity infoPanel = cv.infoPanel(List.of(cv.info(address), cv.info(number)), null, null);

        var linksPanel = cv.infoPanel(List.of(
                        cv.link(
                                new Email(email.getTo(),
                                        email.getSubject(),
                                        email.getBody()),
                                email.getDisplayText()),
                        cv.link(new LinkUrl(linkedIn.getLinkUrl().getUrl()), linkedIn.getDisplayText()),
                        cv.link(new LinkUrl(gitHub.getLinkUrl().getUrl()), gitHub.getDisplayText())), null,
                null);

        return cv.componentBuilder()
                .moduleBuilder(Align.middle(5), canvas)
                .entityName(HEADER_ENTITY_NAME)
                .margin(new Margin(0, 10, 5, 0))
                .anchor(Anchor.topRight())
                .addChild(artemDemchyshyn)
                .addChild(infoPanel)
                .addChild(linksPanel)
                .build();
    }

    private Entity createSection(TemplateBuilder cv,
                                 Canvas canvas,
                                 String title,
                                 String entityName,
                                 List<String> content,
                                 float width,
                                 String bullet,
                                 BlockIndentStrategy strategy) {
        return cv.moduleBuilder(title, canvas)
                .entityName(entityName)
                .addChild(cv.blockText(content, width, bullet, strategy))
                .margin(cv.theme().moduleMargin())
                .build();
    }
}
