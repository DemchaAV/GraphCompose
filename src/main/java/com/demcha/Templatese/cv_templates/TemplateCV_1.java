package com.demcha.Templatese.cv_templates;

import com.demcha.Templatese.CvTheme;
import com.demcha.Templatese.ModelBuilder;
import com.demcha.Templatese.data.MainPageCV;
import com.demcha.Templatese.templates_utils.ConfigLoader;
import com.demcha.loyaut_core.components.ModulesContainer;
import com.demcha.loyaut_core.components.components_builders.Canvas;
import com.demcha.loyaut_core.components.components_builders.ModuleBuilder;
import com.demcha.loyaut_core.components.content.link.Email;
import com.demcha.loyaut_core.components.content.link.LinkUrl;
import com.demcha.loyaut_core.components.core.Entity;
import com.demcha.loyaut_core.components.core.EntityName;
import com.demcha.loyaut_core.components.layout.Align;
import com.demcha.loyaut_core.components.layout.Anchor;
import com.demcha.loyaut_core.components.style.Margin;
import com.demcha.loyaut_core.components.style.Padding;
import com.demcha.loyaut_core.core.EntityManager;
import com.demcha.loyaut_core.system.LayoutSystem;
import com.demcha.loyaut_core.system.implemented_systems.pdf_systems.PdfCanvas;
import com.demcha.loyaut_core.system.implemented_systems.pdf_systems.PdfFileManagerSystem;
import com.demcha.loyaut_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class TemplateCV_1 implements Template {

    private static final String OUTPUT_FILE = "ExpleoGroup_CV_ArtemDemchyshyn.pdf";
    private static final String DATA_FILE = "cv_data.json";

    public static void main(String[] args) {
        new TemplateCV_1().process();
    }

    @Override
    public void process() {
        EntityManager entityManager = new EntityManager(true);
        entityManager.setGuideLines(false);
        
        PDDocument doc = new PDDocument();
        Canvas canvasPdf = new PdfCanvas(PDRectangle.A4, 0.0f, 0.0f);
        canvasPdf.addMargin(new Margin(15, 10, 15, 15));

        setupSystems(entityManager, doc, canvasPdf);
        String whitespace = "";

        ModelBuilder cv = new ModelBuilder(entityManager, CvTheme.defaultTheme());
        MainPageCV data = ConfigLoader.loadConfigWithEnv(DATA_FILE, MainPageCV.class, false);

        float textBlockWidth = (float) canvasPdf.innerWidth();

        Entity moduleHeader = createHeader(cv, data, canvasPdf);

        Entity moduleProfessionalSummary = createSection(cv, canvasPdf,
                data.getModuleSummary().getModuleName(), "ModuleProfessionalSummary",
                List.of(data.getModuleSummary().getBlockSummary()), textBlockWidth, null);

        Entity moduleTechnicalSkills = createSection(cv, canvasPdf,
                data.getTechnicalSkills().getName(), "ModuleTechnicalSkills",
                data.getTechnicalSkills().getModulePoints(), textBlockWidth, "• ");

        Entity moduleEducationCertifications = createSection(cv, canvasPdf,
                data.getEducationCertifications().getName(), "moduleEducationCertifications",
                data.getEducationCertifications().getModulePoints(), textBlockWidth, whitespace);

        Entity moduleProjects = createSection(cv, canvasPdf, 
                data.getProjects().getName(), "ModuleProjects", 
                data.getProjects().getModulePoints(), textBlockWidth, whitespace);

        Entity moduleProfessionalExperience = createSection(cv, canvasPdf,
                data.getProfessionalExperience().getName(), "ModuleProfessionalExperience",
                data.getProfessionalExperience().getModulePoints(), textBlockWidth, whitespace);

        Entity moduleAdditional = createSection(cv, canvasPdf,
                data.getAdditional().getName(), "ModuleAdditional",
                data.getAdditional().getModulePoints(), textBlockWidth, whitespace);

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

        entityManager.processSystems();
    }

    private void setupSystems(EntityManager entityManager, PDDocument doc, Canvas canvasPdf) {
        Path target = Paths.get(OUTPUT_FILE);
        PdfRenderingSystemECS renderingSystemECS = new PdfRenderingSystemECS(doc, canvasPdf);
        entityManager.getSystems().addSystem(new LayoutSystem<>(canvasPdf, renderingSystemECS));
        entityManager.getSystems().addSystem(renderingSystemECS);
        entityManager.getSystems().addSystem(new PdfFileManagerSystem(target, doc));
    }

    private Entity createHeader(ModelBuilder cv, MainPageCV data, Canvas canvasPdf) {
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
                                        email.getBody()), email.getDisplayText())
                        , cv.link(new LinkUrl(linkedIn.getLinkUrl().getUrl()), linkedIn.getDisplayText())
                        , cv.link(new LinkUrl(gitHub.getLinkUrl().getUrl()), gitHub.getDisplayText())
                ), null, null
        );

        return new ModuleBuilder(cv.entityManager(), Align.middle(5), canvasPdf)
                .entityName("ModuleHeader")
                .margin(new Margin(0,10,10,10))
                .anchor(Anchor.topRight())
                .addChild(artemDemchyshyn)
                .addChild(infoPanel)
                .addChild(linksPanel)
                .build();
    }

    private Entity createSection(ModelBuilder cv, Canvas canvas, String title, String entityName, List<String> content, float width, String bullet) {
        return cv.moduleBuilder(title, canvas)
                .entityName(entityName)
                .addChild(cv.blockText(content, width, bullet))
                .margin(cv.theme().modulMargin())
                .build();
    }
}
