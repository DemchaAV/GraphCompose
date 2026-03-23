package com.demcha.Templatese.templates;

import com.demcha.Templatese.CvTheme;
import com.demcha.Templatese.TemplateBuilder;
import com.demcha.Templatese.data.MainPageCV;
import com.demcha.Templatese.template.CvTemplate;
import com.demcha.Templatese.template.MainPageCvDTO;
import com.demcha.compose.GraphCompose;
import com.demcha.compose.loyaut_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.loyaut_core.components.components_builders.Canvas;
import com.demcha.compose.loyaut_core.components.components_builders.ModuleBuilder;
import com.demcha.compose.loyaut_core.components.content.link.Email;
import com.demcha.compose.loyaut_core.components.content.link.LinkUrl;
import com.demcha.compose.loyaut_core.components.core.Entity;
import com.demcha.compose.loyaut_core.components.layout.Align;
import com.demcha.compose.loyaut_core.components.layout.Anchor;
import com.demcha.compose.loyaut_core.components.style.Margin;
import com.demcha.compose.loyaut_core.core.PdfComposer;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.util.List;

@Slf4j
public class Template_CV1 implements CvTemplate {

    /**
     * Renders a PDF document using this template.
     *
     * @param originalCv  The original CV data (contains personal info like phone,
     *                    address)
     * @param rewrittenCv The rewritten CV data (contains optimized content)
     * @return A PDDocument that can be saved or streamed
     */
    @Override
    public PDDocument render(MainPageCV originalCv, MainPageCvDTO rewrittenCv) {
        MainPageCV data = rewrittenCv.merge(originalCv);

        boolean guideLines = false;

        try {
            // Do NOT use try-with-resources here!
            // The PDDocument must remain open for the caller to stream/save it.
            // The caller (StreamingResponseBody) is responsible for closing it.
            PdfComposer composer = GraphCompose.pdf()
                    .pageSize(PDRectangle.A4)
                    .margin(15, 10, 15, 15)
                    .markdown(true)
                    .guideLines(guideLines)
                    .create();

            Canvas canvas = composer.canvas();
            String whitespace = "  ";
            BlockIndentStrategy indentStrategy = BlockIndentStrategy.FROM_SECOND_LINE;

            TemplateBuilder cv = TemplateBuilder.from(composer.componentBuilder(), CvTheme.defaultTheme());

            float textBlockWidth = (float) canvas.innerWidth();

            Entity moduleHeader = createHeader(cv, data, canvas);

            Entity moduleProfessionalSummary = createSection(cv, canvas,
                    data.getModuleSummary().getModuleName(), "ModuleProfessionalSummary",
                    List.of(data.getModuleSummary().getBlockSummary()), textBlockWidth, "    ",
                    BlockIndentStrategy.FIRST_LINE);

            Entity moduleTechnicalSkills = createSection(cv, canvas,
                    data.getTechnicalSkills().getName(), "ModuleTechnicalSkills",
                    data.getTechnicalSkills().getModulePoints(), textBlockWidth, "• ",
                    BlockIndentStrategy.ALL_LINES);

            Entity moduleEducationCertifications = createSection(cv, canvas,
                    data.getEducationCertifications().getName(), "moduleEducationCertifications",
                    data.getEducationCertifications().getModulePoints(), textBlockWidth,
                    whitespace, indentStrategy);

            Entity moduleProjects = createSection(cv, canvas,
                    data.getProjects().getName(), "ModuleProjects",
                    data.getProjects().getModulePoints(), textBlockWidth, whitespace,
                    indentStrategy);

            Entity moduleProfessionalExperience = createSection(cv, canvas,
                    data.getProfessionalExperience().getName(), "ModuleProfessionalExperience",
                    data.getProfessionalExperience().getModulePoints(), textBlockWidth,
                    whitespace, indentStrategy);

            Entity moduleAdditional = createSection(cv, canvas,
                    data.getAdditional().getName(), "ModuleAdditional",
                    data.getAdditional().getModulePoints(), textBlockWidth, whitespace,
                    indentStrategy);

            cv.moduleBuilder(canvas)
                    .entityName("MainVBoxContainer")
                    .addChild(moduleHeader)
                    .addChild(moduleProfessionalSummary)
                    .addChild(moduleTechnicalSkills)
                    .addChild(moduleEducationCertifications)
                    .addChild(moduleProjects)
                    .addChild(moduleProfessionalExperience)
                    .addChild(moduleAdditional)
                    .build();

            return composer.toPDDocument();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CV", e);
        }
    }

    @Override
    public void render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, Path path) {
        MainPageCV data = rewrittenCv.merge(originalCv);

        boolean guideLines = false;

        try (PdfComposer composer = GraphCompose.pdf(path)
                    .pageSize(PDRectangle.A4)
                    .margin(15, 10, 15, 15)
                    .markdown(true)
                    .guideLines(guideLines)
                    .create()) {

            Canvas canvas = composer.canvas();
            String whitespace = "  ";
            BlockIndentStrategy indentStrategy = BlockIndentStrategy.FROM_SECOND_LINE;

            TemplateBuilder cv = TemplateBuilder.from(composer.componentBuilder(), CvTheme.defaultTheme());

            float textBlockWidth = (float) canvas.innerWidth();

            Entity moduleHeader = createHeader(cv, data, canvas);

            Entity moduleProfessionalSummary = createSection(cv, canvas,
                    data.getModuleSummary().getModuleName(), "ModuleProfessionalSummary",
                    List.of(data.getModuleSummary().getBlockSummary()), textBlockWidth, "    ",
                    BlockIndentStrategy.FIRST_LINE);

            Entity moduleTechnicalSkills = createSection(cv, canvas,
                    data.getTechnicalSkills().getName(), "ModuleTechnicalSkills",
                    data.getTechnicalSkills().getModulePoints(), textBlockWidth, "• ",
                    BlockIndentStrategy.ALL_LINES);

            Entity moduleEducationCertifications = createSection(cv, canvas,
                    data.getEducationCertifications().getName(), "moduleEducationCertifications",
                    data.getEducationCertifications().getModulePoints(), textBlockWidth,
                    whitespace, indentStrategy);

            Entity moduleProjects = createSection(cv, canvas,
                    data.getProjects().getName(), "ModuleProjects",
                    data.getProjects().getModulePoints(), textBlockWidth, whitespace,
                    indentStrategy);

            Entity moduleProfessionalExperience = createSection(cv, canvas,
                    data.getProfessionalExperience().getName(), "ModuleProfessionalExperience",
                    data.getProfessionalExperience().getModulePoints(), textBlockWidth,
                    whitespace, indentStrategy);

            Entity moduleAdditional = createSection(cv, canvas,
                    data.getAdditional().getName(), "ModuleAdditional",
                    data.getAdditional().getModulePoints(), textBlockWidth, whitespace,
                    indentStrategy);

            cv.moduleBuilder(canvas)
                    .entityName("MainVBoxContainer")
                    .addChild(moduleHeader)
                    .addChild(moduleProfessionalSummary)
                    .addChild(moduleTechnicalSkills)
                    .addChild(moduleEducationCertifications)
                    .addChild(moduleProjects)
                    .addChild(moduleProfessionalExperience)
                    .addChild(moduleAdditional)
                    .build();

            composer.build();
            log.info("File has been saved to {}", path.toAbsolutePath());

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CV", e);
        }
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

        return new ModuleBuilder(cv.entityManager(), Align.middle(5), canvas)
                .entityName("ModuleHeader")
                .margin(new Margin(0, 10, 10, 10))
                .anchor(Anchor.topRight())
                .addChild(artemDemchyshyn)
                .addChild(infoPanel)
                .addChild(linksPanel)
                .build();
    }

    private Entity createSection(TemplateBuilder cv, Canvas canvas, String title, String entityName,
                                 List<String> content, float width, String bullet, BlockIndentStrategy strategy) {
        return cv.moduleBuilder(title, canvas)
                .entityName(entityName)
                .addChild(cv.blockText(content, width, bullet, strategy))
                .margin(cv.theme().moduleMargin())
                .build();
    }

    /**
     * Unique identifier for this template.
     * Used to select template via API (e.g., "modern-professional", "classic",
     * "minimal").
     */
    @Override
    public String getTemplateId() {
        return "modern-professional";
    }

    /**
     * Human-readable name of the template.
     * Displayed to users in template selection UI.
     */
    @Override
    public String getTemplateName() {
        return "Modern Professional";
    }

    @Override
    public String getDescription() {
        return "A clean, professional template with centered header and well-organized sections.";
    }

}
