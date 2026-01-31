package com.demcha.Templatese.cv_templates;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.loyaut_core.core.PdfComposer;
import com.demcha.Templatese.CvTheme;
import com.demcha.Templatese.TemplateBuilder;
import com.demcha.Templatese.data.MainPageCV;
import com.demcha.Templatese.templates_utils.ConfigLoader;
import com.demcha.compose.loyaut_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.loyaut_core.components.components_builders.Canvas;
import com.demcha.compose.loyaut_core.components.components_builders.ModuleBuilder;
import com.demcha.compose.loyaut_core.components.content.link.Email;
import com.demcha.compose.loyaut_core.components.content.link.LinkUrl;
import com.demcha.compose.loyaut_core.components.core.Entity;
import com.demcha.compose.loyaut_core.components.layout.Align;
import com.demcha.compose.loyaut_core.components.layout.Anchor;
import com.demcha.compose.loyaut_core.components.style.Margin;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class TemplateCV_1 implements Template {

        private static final String OUTPUT_FILE = "ExpleoGroup_CV_ArtemDemchyshyn.pdf";
        private static final String DATA_FILE = "cv_data.json";

        public static void main(String[] args) throws Exception {
                new TemplateCV_1().process();
        }

        @Override
        public void process() {
                boolean guideLines = true;

                Path target = Paths.get(OUTPUT_FILE);

                try (PdfComposer composer = GraphCompose.pdf(target)
                                .pageSize(PDRectangle.A4)
                                .margin(15, 10, 15, 15)
                                .markdown(true)
                                .guideLines(guideLines)
                                .create()) {

                        Canvas canvas = composer.canvas();
                        String whitespace = "  ";
                        BlockIndentStrategy indentStrategy = BlockIndentStrategy.FROM_SECOND_LINE;

                        TemplateBuilder cv = composer.componentBuilder().template(CvTheme.defaultTheme());
                        MainPageCV data = ConfigLoader.loadConfigWithEnv(DATA_FILE, MainPageCV.class, false);

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

                        Entity mainVBoxContainer = cv.moduleBuilder(canvas)
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
                                .margin(cv.theme().modulMargin())
                                .build();
        }
}
