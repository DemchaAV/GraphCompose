package com.demcha.Templatese;

import com.demcha.ConfigLoader;
import com.demcha.Templatese.data.ModuleSummary;
import com.demcha.Templatese.data.ModuleYml;
import com.demcha.components.ModuleContainer;
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
import com.demcha.system.pdf_systems.PdfFileManagerSystem;
import com.demcha.system.pdf_systems.PdfRenderingSystemECS;
import com.demcha.utils.page_brecker.PdfCanvas;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class test {
    public static void main(String[] args) {
        Path target = Paths.get("new_test_file.pdf");

        EntityManager entityManager = new EntityManager();
        entityManager.setGuideLines(true);
        PDDocument doc = new PDDocument();
        Canvas canvasSize = new PdfCanvas(PDRectangle.A4, 0.0f, 0.0f);
        canvasSize.addMargin(Margin.of(10));


        entityManager.addSystem(new LayoutSystemImpl(canvasSize));
        entityManager.addSystem(new PdfRenderingSystemECS(doc, canvasSize));
        entityManager.addSystem(new PdfFileManagerSystem(target, doc));

        Model cv = new Model(entityManager);
        CvData data = ConfigLoader.loadConfigWithEnv("cv_data.yml", CvData.class, false);
        var number = data.getHeder().getPhoneNumber();
        var address = data.getHeder().getAddress();
        var email = data.getHeder().getEmail();
        var linkedIn = data.getHeder().getLinkedIn();
        var gitHub = data.getHeder().getGitHub();
        ModuleSummary summary = data.getModuleSummary();
        ModuleYml technicalSkills = data.getTechnicalSkills();
        ModuleYml educationCertifications = data.getEducationCertifications();
        // Эти три — были добавлены в CvData ранее
        ModuleYml projects = data.getProjects();
        ModuleYml professionalExperience = data.getProfessionalExperience();
        ModuleYml additional = data.getAdditional();


        var canvas = new ModuleContainer(entityManager, canvasSize);


        Entity artemDemchyshyn = cv.name(data.getHeder().getName());

        Entity infoPanel = cv.infoPanel(List.of(cv.info(address), cv.info(number)));

        var linksPanel = cv.infoPanel(List.of(
                        cv.link(
                                new Email(email.getTo(),
                                        email.getSubject(),
                                        email.getBody()), email.getDisplayText())
                        , cv.link(new LinkUrl(linkedIn.getLinkUrl().getUrl()), linkedIn.getDisplayText())
                        , cv.link(new LinkUrl(gitHub.getLinkUrl().getUrl()), gitHub.getDisplayText())
                )
        );


        Entity moduleHeader = new ModuleBuilder(entityManager, Align.middle(5), canvas.innerBoxSize())
                .margin(Margin.of(10))
                .anchor(Anchor.topRight())
                .addChild(artemDemchyshyn)
                .addChild(infoPanel)
                .addChild(linksPanel)
                .build();

        // 7) Professional Summary
        Entity moduleProfessionalSummary = cv.moduleBuilder(summary.getModuleName(), canvas.innerBoxSize())
                .addChild(cv.blockText(summary.getBlockSummary(), canvas.innerBoxSize().width()))
                .margin(Margin.top(6))
                .build();

        // 8) Technical Skills
        Entity moduleTechnicalSkills = cv.moduleBuilder(technicalSkills.getName(), canvas.innerBoxSize())
                .addChild(cv.blockText(technicalSkills.getModulePoints(), canvas.innerBoxSize().width(), "• "))
                .margin(Margin.top(6))
                .build();

        // 9) Education & Certifications
        Entity moduleEducationCertifications = cv.moduleBuilder(educationCertifications.getName(), canvas.innerBoxSize())
                .addChild(cv.blockText(educationCertifications.getModulePoints(), canvas.innerBoxSize().width(), null))
                .margin(Margin.top(6))
                .build();

        // 10) Projects (как modulePoints)
        Entity moduleProjects = cv.moduleBuilder(projects.getName(), canvas.innerBoxSize())
                .addChild(cv.blockText(projects.getModulePoints(), canvas.innerBoxSize().width(), null))
                .margin(Margin.top(6))
                .build();

        // 11) Professional Experience (как modulePoints)
        Entity moduleProfessionalExperience = cv.moduleBuilder(professionalExperience.getName(), canvas.innerBoxSize())
                .addChild(cv.blockText(professionalExperience.getModulePoints(), canvas.innerBoxSize().width(), null))
                .margin(Margin.top(6))
                .build();

        // 12) Additional (как blockText)
        Entity moduleAdditional = cv.moduleBuilder(additional.getName(), canvas.innerBoxSize())
                .addChild(cv.blockText(additional.getModulePoints(), canvas.innerBoxSize().width(), null))
                .margin(Margin.top(6))
                .build();

        // 13) Главная вертикальная колонка
        InnerBoxSize inner = canvas.innerBoxSize();
        Entity vBox = cv.moduleBuilder(inner)
                .anchor(Anchor.bottomCenter())
                .addChild(moduleHeader)
                .addChild(moduleProfessionalSummary)
                .addChild(moduleTechnicalSkills)
                .addChild(moduleEducationCertifications)
                .addChild(moduleProjects)
                .addChild(moduleProfessionalExperience)
                .addChild(moduleAdditional)
                .build();

        // 14) Рендер
        canvas.addModule(vBox).build();


        entityManager.processSystems();
        System.out.printf("Page number: %s ", doc.getNumberOfPages());
        entityManager.getEntities().forEach(
                (u, e) -> {
                    System.out.println(e.name());
                    Placement placement = e.getComponent(Placement.class).orElseThrow();
                    System.out.println(placement);


                }

        );
    }
}

