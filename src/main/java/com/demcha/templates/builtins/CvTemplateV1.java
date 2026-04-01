package com.demcha.templates.builtins;

import com.demcha.templates.CvTheme;
import com.demcha.templates.TemplateBuilder;
import com.demcha.templates.data.MainPageCV;
import com.demcha.templates.api.CvTemplate;
import com.demcha.templates.api.MainPageCvDTO;
import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.layout_core.core.Canvas;
import com.demcha.compose.layout_core.components.components_builders.ModuleBuilder;
import com.demcha.compose.layout_core.components.content.link.Email;
import com.demcha.compose.layout_core.components.content.link.LinkUrl;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.compose.layout_core.core.DocumentComposer;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.util.List;

@Slf4j
public class CvTemplateV1 implements CvTemplate {
    private static final String MAIN_CONTAINER_NAME = "MainVBoxContainer";
    private static final String HEADER_ENTITY_NAME = "ModuleHeader";
    private static final String DEFAULT_BULLET_OFFSET = "  ";
    private static final String SUMMARY_BULLET_OFFSET = "    ";
    private static final String SKILLS_BULLET = "• ";

    private final CvTheme theme;

    public CvTemplateV1() {
        this(CvTheme.defaultTheme());
    }

    public CvTemplateV1(CvTheme theme) {
        this.theme = theme == null ? CvTheme.defaultTheme() : theme;
    }

    /**
     * Renders a PDF document using this template.
     *
     * @param originalCv  The original CV data (contains personal info like phone,
     *                    address)
     * @param guideLines on or off guide lines on rendering document
     * @param rewrittenCv The rewritten CV data (contains optimized content)
     * @return A PDDocument that can be saved or streamed
     */
    @Override
    public PDDocument render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, boolean guideLines) {
        MainPageCV data = rewrittenCv.merge(originalCv);

        try {
            PdfComposer composer = createPdfComposer(null, guideLines);
            designDocument(composer, data);
            return composer.toPDDocument();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CV", e);
        }
    }

    @Override
    public PDDocument render(MainPageCV originalCv, MainPageCvDTO rewrittenCv) {
        return render(originalCv, rewrittenCv, false);
    }

    @Override
    public void render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, Path path, boolean guideLines) {
        MainPageCV data = rewrittenCv.merge(originalCv);

        try (PdfComposer composer = createPdfComposer(path, guideLines)) {
            designDocument(composer, data);
            composer.build();
            log.info("File has been saved to {}", path.toAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CV", e);
        }
    }

    @Override
    public void render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, Path path) {
        render(originalCv, rewrittenCv, path, false);
    }

    private PdfComposer createPdfComposer(Path path, boolean guideLines) {
        GraphCompose.PdfBuilder builder = path != null ? GraphCompose.pdf(path) : GraphCompose.pdf();
        return builder.pageSize(PDRectangle.A4)
                .margin(15, 10, 15, 15)
                .markdown(true)
                .guideLines(guideLines)
                .create();
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

