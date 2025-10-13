package com.demcha.Templatese;

import com.demcha.ConfigLoader;
import com.demcha.Templatese.data.ModuleSummary;
import com.demcha.Templatese.data.ModuleYml;
import com.demcha.components.CanvasObject;
import com.demcha.components.components_builders.*;
import com.demcha.components.content.link.Email;
import com.demcha.components.content.link.LinkUrl;
import com.demcha.components.content.text.TextDecoration;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.Anchor;
import com.demcha.components.style.ComponentColor;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;
import com.demcha.system.pdf_systems.PdfLayoutSystem;
import com.demcha.system.pdf_systems.PdfFileManagerSystem;
import com.demcha.system.pdf_systems.PdfRenderingSystemECS;
import lombok.AllArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@AllArgsConstructor
public class Model {
    private EntityManager entityManager;

    public Entity name(String name) {
        Entity nameEntity = new TextBuilder(entityManager)
                .textWithAutoSize(name)
                .anchor(Anchor.topRight())
                .margin(Margin.bottom(5))
                .textStyle(TextStyle.builder()
                        .size(30)
                        .color(new Color(44, 62, 80))
                        .font(TextStyle.HELVETICA_BOLD)
                        .build())
                .build();
        return nameEntity;
    }

    public Entity info(String info) {
        Entity infoEntity = new TextBuilder(entityManager)
                .textWithAutoSize(info)
                .anchor(Anchor.center())
                .textStyle(TextStyle.builder()
                        .size(12)
                        .color(ComponentColor.MODULE_LINE_TEXT)
                        .font(TextStyle.HELVETICA)
                        .build())

                .build();
        return infoEntity;
    }

    public Entity infoPanel(java.util.List<Entity> entities) {
        var heigh = entities.stream()
                .map(entity -> entity.getComponent(ContentSize.class).orElseThrow())
                .map(size -> size.height())
                .max(Double::compareTo).get();
        ComponentColor componentColor = entities.get(0).getComponent(ComponentColor.class).orElse(new ComponentColor(ComponentColor.MODULE_LINE_TEXT));

        var links = new HContainerBuilder(entityManager,Align.right(5))
                .anchor(Anchor.topRight());


        for (int i = 0; i < entities.size(); i++) {
            if (i < entities.size() && i != 0) {
                var separator = new RectangleBuilder(entityManager)
                        .size(new ContentSize(1, heigh))
                        .fillColor(componentColor)
                        .margin(0, 2, 0, 2)
                        .anchor(Anchor.center())
                        .build();
                links.addChild(separator);
            }
            links.addChild(entities.get(i));
        }
        return links.build();
    }

    public <T extends LinkUrl> Entity link(T link, String displayText) {
        var linkEntity = new LinkBuilder(entityManager)
                .linkUrl(link)
                .anchor(Anchor.centerRight())
                .displayText(new DisplayUrlTextBuilder(entityManager)
                        .textWithAutoSize(displayText)
                        .textStyle(TextStyle.builder()
                                .size(12)
                                .color(ComponentColor.ROYAL_BLUE)
                                .font(TextStyle.HELVETICA_OBLIQUE)
                                .build())
                )
                .build();
        return linkEntity;
    }

    private Entity moduleName(String moduleName) {
        Entity moduleNameEntity = new TextBuilder(entityManager)
                .textWithAutoSize(moduleName)
                .anchor(Anchor.topLeft())
                .margin(new Margin(5, 5, 5, 10))
                .textStyle(TextStyle.builder()
                        .size(18.4)
                        .color(new Color(41, 128, 185))
                        .font(TextStyle.HELVETICA_BOLD)
                        .build())
                .build();
        return moduleNameEntity;
    }

    public ModuleBuilder moduleBuilder(String moduleName, PDPage page) {
        var moduleHeader = new ModuleBuilder(entityManager,Align.middle(5), page)
                .margin(Margin.of(20))
                .anchor(Anchor.topRight());
        if (moduleName != null) {
            moduleHeader.addChild(moduleName("Professional Summary"));
        }

        return moduleHeader;
    }

    public ModuleBuilder moduleBuilder(String moduleName, InnerBoxSize innerBoxSize) {
        var moduleHeader = new ModuleBuilder(entityManager,Align.middle(5), innerBoxSize)
                .margin(Margin.of(5))
                .anchor(Anchor.topLeft());
        if (moduleName != null) {
            moduleHeader.addChild(moduleName(moduleName));
        }

        return moduleHeader;
    }

    public ModuleBuilder moduleBuilder(String moduleName, InnerBoxSize innerBoxSize, List<String> modulePoints) {
        var moduleHeader = new ModuleBuilder(entityManager,Align.middle(5), innerBoxSize)
                .margin(Margin.of(5))
                .anchor(Anchor.topLeft());
        if (moduleName != null) {
            moduleHeader.addChild(moduleName(moduleName));
        }
        var vbox = new VContainerBuilder(entityManager,Align.middle(5))
                .size(new ContentSize(innerBoxSize.innerW(), 50));
        for (int i = 0; i < modulePoints.size(); i++) {
            vbox.addChild(
                    blockTextBuilder(modulePoints.get(i), innerBoxSize.innerW())
                            .anchor(Anchor.left())
                            .build()
            );
        }
        moduleHeader.addChild(vbox.build());

        return moduleHeader;
    }

    public ModuleBuilder moduleBuilder(PDPage page) {
        return moduleBuilder(null, page);
    }

    public ModuleBuilder moduleBuilder(InnerBoxSize innerBoxSize) {
        return moduleBuilder(null, innerBoxSize);
    }

    public Entity blockText(String text) {
        return blockText(text, 500);

    }

    public Entity blockText(String text, double width) {
        return blockTextBuilder(text, width)
                .anchor(Anchor.center())
                .build();

    }

    public Entity blockText(List<String> text, double width, String bulletOffset) {
        Padding padding = null;
        Margin margin = null;

        TextStyle stye = TextStyle.builder()
                .size(12)
                .color(ComponentColor.TITLE)
                .font(new PDType1Font(Standard14Fonts.FontName.HELVETICA))
                .decoration(TextDecoration.UNDERLINE)
                .build();

        var blockText = new BlockTextBuilder(entityManager,Align.left(5))
                .size(width, 2)
                .padding(0, 5, 0, 25)
                .text(text, stye, padding, margin, bulletOffset)
                .anchor(Anchor.center())
                .build();


        return blockText;

    }


    private BlockTextBuilder blockTextBuilder(String text, double width) {
        TextBuilder textBuilder = new TextBuilder(entityManager)
                .textWithAutoSize(text)
                .textStyle(TextStyle.builder()
                        .size(12)
                        .color(ComponentColor.TITLE)
                        .font(new PDType1Font(Standard14Fonts.FontName.HELVETICA))
                        .decoration(TextDecoration.UNDERLINE)
                        .build());


        var blockText = new BlockTextBuilder(entityManager,Align.left(5))
                .size(width, 2)
                .padding(0, 5, 0, 25)
                .text(textBuilder);

        return blockText;

    }
}


class test {
    public static void main(String[] args) {
        Path target = Paths.get("new_test_file.pdf");

        EntityManager entityManager = new EntityManager();
        entityManager.setGuideLines(false);
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage(PDRectangle.A4));


        entityManager.addSystem(new PdfLayoutSystem(doc.getPage(0)));
        entityManager.addSystem(new PdfRenderingSystemECS(doc));
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


        var canvas = new CanvasObject(entityManager, doc.getPage(0))
                .padding(Padding.of(10));


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


        Entity moduleHeader = new ModuleBuilder(entityManager,Align.middle(5), canvas.innerBoxSize())
                .margin(Margin.of(10))
                .anchor(Anchor.topRight())
                .addChild(artemDemchyshyn)
                .addChild(infoPanel)
                .addChild(linksPanel)
                .build();

        // 7) Professional Summary
        Entity moduleProfessionalSummary = cv.moduleBuilder(summary.getModuleName(), canvas.innerBoxSize())
                .addChild(cv.blockText(summary.getBlockSummary(), canvas.innerBoxSize().innerW()))
                .margin(Margin.top(6))
                .build();

        // 8) Technical Skills
        Entity moduleTechnicalSkills = cv.moduleBuilder(technicalSkills.getName(), canvas.innerBoxSize())
                .addChild(cv.blockText(technicalSkills.getModulePoints(), canvas.innerBoxSize().innerW(), "• "))
                .margin(Margin.top(6))
                .build();

        // 9) Education & Certifications
        Entity moduleEducationCertifications = cv.moduleBuilder(educationCertifications.getName(), canvas.innerBoxSize())
                .addChild(cv.blockText(educationCertifications.getModulePoints(), canvas.innerBoxSize().innerW(), null))
                .margin(Margin.top(6))
                .build();

        // 10) Projects (как modulePoints)
        Entity moduleProjects = cv.moduleBuilder(projects.getName(), canvas.innerBoxSize())
                .addChild(cv.blockText(projects.getModulePoints(), canvas.innerBoxSize().innerW(), null))
                .margin(Margin.top(6))
                .build();

        // 11) Professional Experience (как modulePoints)
        Entity moduleProfessionalExperience = cv.moduleBuilder(professionalExperience.getName(), canvas.innerBoxSize())
                .addChild(cv.blockText(professionalExperience.getModulePoints(), canvas.innerBoxSize().innerW(), null))
                .margin(Margin.top(6))
                .build();

        // 12) Additional (как blockText)
        Entity moduleAdditional = cv.moduleBuilder(additional.getName(), canvas.innerBoxSize())
                .addChild(cv.blockText(additional.getModulePoints(), canvas.innerBoxSize().innerW(), null))
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
    }
}
