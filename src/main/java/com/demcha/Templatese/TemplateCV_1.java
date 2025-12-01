package com.demcha.Templatese;

import com.demcha.Templatese.data.MainPageCV;
import com.demcha.Templatese.data.ModuleSummary;
import com.demcha.Templatese.data.ModuleYml;
import com.demcha.Templatese.templates_utils.ConfigLoader;
import com.demcha.components.ModulesContainer;
import com.demcha.components.components_builders.Canvas;
import com.demcha.components.components_builders.ModuleBuilder;
import com.demcha.components.content.link.Email;
import com.demcha.components.content.link.LinkUrl;
import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.Anchor;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.style.Margin;
import com.demcha.core.EntityManager;
import com.demcha.system.LayoutSystemImpl;
import com.demcha.system.implemented_systems.pdf_systems.PdfCanvas;
import com.demcha.system.implemented_systems.pdf_systems.PdfFileManagerSystem;
import com.demcha.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class TemplateCV_1 {
    public static void main(String[] args) {
        Path target = Paths.get("ExpleoGroup_CV_ArtemDemchyshyn.pdf");

        EntityManager entityManager = new EntityManager();
        entityManager.setGuideLines(false);
        PDDocument doc = new PDDocument();
        Canvas canvasPdf = new PdfCanvas(PDRectangle.A4, 0.0f, 0.0f);
        canvasPdf.addMargin(Margin.of(10));


        entityManager.addSystem(new LayoutSystemImpl(canvasPdf));
        entityManager.addSystem(new PdfRenderingSystemECS(doc, canvasPdf));
        entityManager.addSystem(new PdfFileManagerSystem(target, doc));

        ModelBuilder cv = new ModelBuilder(entityManager);
        String pathFolder = "/ai_content/";
//        MainPageCV data = ConfigLoader.loadConfigWithEnv("cv_data_frelance.yml", MainPageCV.class, false);
        MainPageCV data = ConfigLoader.loadConfigWithEnv("cv_data.yml", MainPageCV.class, false);
//        MainPageCV data = ConfigLoader.loadConfigWithEnv("ExpleoGroup.yml", MainPageCV.class, false);
        var number = data.getHeder().getPhoneNumber();
        var address = data.getHeder().getAddress();
        var email = data.getHeder().getEmail();
        var linkedIn = data.getHeder().getLinkedIn();
        var gitHub = data.getHeder().getGitHub();
        ModuleSummary summary = data.getModuleSummary();
        ModuleYml technicalSkills = data.getTechnicalSkills();
        ModuleYml educationCertifications = data.getEducationCertifications();
        // Эти три — были добавлены в MainPageCV ранее
        ModuleYml projects = data.getProjects();
        ModuleYml professionalExperience = data.getProfessionalExperience();
        ModuleYml additional = data.getAdditional();


        var canvas = new ModulesContainer(entityManager, canvasPdf);
        canvas.addComponent(new EntityName("ModulesContainer"));


        Entity artemDemchyshyn = cv.name(data.getHeder().getName());

        Entity infoPanel = cv.infoPanel(List.of(cv.info(address), cv.info(number)), null, null);

        var linksPanel = cv.infoPanel(List.of(
                        cv.link(
                                new Email(email.getTo(),
                                        email.getSubject(),
                                        email.getBody()), email.getDisplayText())
                        , cv.link(new LinkUrl(linkedIn.getLinkUrl().getUrl()), linkedIn.getDisplayText())
                        , cv.link(new LinkUrl(gitHub.getLinkUrl().getUrl()), gitHub.getDisplayText())
                ), null, null
        );



        Entity moduleHeader = new ModuleBuilder(entityManager, Align.middle(5), canvasPdf)
                .entityName("ModuleHeader")
                .margin(Margin.of(10))
                .anchor(Anchor.topRight())
                .addChild(artemDemchyshyn)
                .addChild(infoPanel)
                .addChild(linksPanel)
                .build();

        // 7) Professional Summary
        float textBlockWidth = (float) canvasPdf.innerWidth();
        Entity moduleProfessionalSummary = cv.moduleBuilder(summary.getModuleName(), canvasPdf)
                .entityName("ModuleProfessionalSummary")
                .addChild(cv.blockText(summary.getBlockSummary(), textBlockWidth))
                .margin(Margin.top(6))
                .build();

        // 8) Technical Skills
        Entity moduleTechnicalSkills = cv.moduleBuilder(technicalSkills.getName(), canvasPdf)
                .entityName("ModuleTechnicalSkills")
                .addChild(cv.blockText(technicalSkills.getModulePoints(), textBlockWidth, "• "))
                .margin(Margin.top(6))
                .build();

        // 9) Education & Certifications
        Entity moduleEducationCertifications = cv.moduleBuilder(educationCertifications.getName(), canvasPdf)
                .entityName("moduleEducationCertifications")
                .addChild(cv.blockText(educationCertifications.getModulePoints(), textBlockWidth, null))
                .margin(Margin.top(6))
                .build();

        // 10) Projects (как modulePoints)
        Entity moduleProjects = cv.moduleBuilder(projects.getName(), canvasPdf)
                .entityName("ModuleProjects")
                .addChild(cv.blockText(projects.getModulePoints(), textBlockWidth, null))
                .margin(Margin.top(6))
                .build();

        // 11) Professional Experience (как modulePoints)
        Entity moduleProfessionalExperience = cv.moduleBuilder(professionalExperience.getName(),canvasPdf)
                .entityName("ModuleProfessionalExperience")
                .addChild(cv.blockText(professionalExperience.getModulePoints(), textBlockWidth, null))
                .margin(Margin.top(6))
                .build();

        // 12) Additional (как blockText)
        Entity moduleAdditional = cv.moduleBuilder(additional.getName(), canvasPdf)
                .entityName("ModuleAdditional")
                .addChild(cv.blockText(additional.getModulePoints(), textBlockWidth, null))
                .margin(Margin.top(6))
                .build();

        // 13) Главная вертикальная колонка
        Entity mainVBoxContainer = cv.moduleBuilder(canvasPdf)
                .entityName("MainVBoxContainer")
                .addChild(moduleHeader)
                .addChild(moduleProfessionalSummary)
                .addChild(moduleTechnicalSkills)
                .addChild(moduleEducationCertifications)
                .addChild(moduleProjects)
                .addChild(moduleProfessionalExperience)
                .addChild(moduleAdditional)
                .build();

        // 14) Рендер
        canvas.addModule(mainVBoxContainer).build();


        entityManager.processSystems();

//        entityManager.printEntities();

    }
}

