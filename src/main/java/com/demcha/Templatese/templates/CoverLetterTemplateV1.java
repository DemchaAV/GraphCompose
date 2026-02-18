package com.demcha.Templatese.templates;

import com.demcha.Templatese.CvTheme;
import com.demcha.Templatese.JobDetails;
import com.demcha.Templatese.TemplateBuilder;
import com.demcha.Templatese.data.Header;
import com.demcha.Templatese.template.CoverLetterTemplate;
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
import com.demcha.compose.loyaut_core.components.style.Padding;
import com.demcha.compose.loyaut_core.core.PdfComposer;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.util.List;

@Slf4j
public class CoverLetterTemplateV1 implements CoverLetterTemplate {
    public static void main(String[] args) {

    }

    /**
     * Unique identifier for this template.
     * Used to select template via API (e.g., "modern-professional", "classic",
     * "minimal").
     */
    @Override
    public String getTemplateId() {
        return "";
    }

    /**
     * Human-readable name of the template.
     * Displayed to users in template selection UI.
     */
    @Override
    public String getTemplateName() {
        return "";
    }

    /**
     * Optional description of the template.
     */
    @Override
    public String getDescription() {
        return CoverLetterTemplate.super.getDescription();
    }

    /**
     * Renders a PDF document using this template.
     *
     * @param header
     * @param wroteLetter
     * @param jobDetails
     * @return A PDDocument that can be saved or streamed
     */
    @Override
    public PDDocument render(Header header, String wroteLetter, JobDetails jobDetails) {
        boolean guideLines = false;
        try {
            PdfComposer composer = createpdfComposer(guideLines);
            designLetter(header, wroteLetter, jobDetails, composer);
            return composer.toPDDocument();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CV", e);
        }
    }

    @Override
    public void render(Header header, String wroteLetter, JobDetails jobDetails, Path path) {
        boolean guideLines = false;
        try {
            PdfComposer composer = createpdfComposer(path, guideLines);

            designLetter(header, wroteLetter, jobDetails, composer);

            composer.build();
            log.info("Cover letter saved to {}", path.toAbsolutePath());

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CV", e);
        }
    }

    private void designLetter(Header header, String wroteLetter, JobDetails jobDetails, PdfComposer composer) {
        Canvas canvas = composer.canvas();
        wroteLetter = wroteLetter.replace("${companyName}", jobDetails.company());

        String whitespace = "  ";
        BlockIndentStrategy indentStrategy = BlockIndentStrategy.FIRST_LINE;

        TemplateBuilder cv = composer.componentBuilder().template(CvTheme.defaultTheme());

        float textBlockWidth = (float) composer.canvas().innerWidth();

        Entity moduleHeader = createHeader(cv, header, canvas);


        Entity coverLetter = letterSection(cv, List.of(wroteLetter), textBlockWidth, whitespace, indentStrategy);
        var kingRegards = composer.componentBuilder()
                .blockText(Align.left(CvTheme.courier().spacing()), CvTheme.defaultTheme().bodyTextStyle())
                .size(canvas.innerWidth(), 2)
                .text(List.of("King regards,", header.getName()), CvTheme.defaultTheme().bodyTextStyle(), Padding.zero(), new Margin(20, 20, 0, 0))
                .build();
        kingRegards
                .addComponent(Anchor.topRight());


        cv.moduleBuilder(canvas)
                .entityName("MainVBoxContainer")
                .addChild(moduleHeader)
                .addChild(coverLetter)
                .addChild(kingRegards)
                .build();
    }

    private PdfComposer createpdfComposer(Path path, boolean guideLines) {
        GraphCompose.PdfBuilder composer;
        if (path != null) {
            composer = GraphCompose.pdf(path);
        } else {
            composer = GraphCompose.pdf();
        }

        composer.pageSize(PDRectangle.A4)
                .margin(15, 10, 15, 15)
                .markdown(true)
                .guideLines(guideLines);

        return composer.create();
    }

    private PdfComposer createpdfComposer(boolean guideLines) {
        return createpdfComposer(null, guideLines);
    }

    private Entity createHeader(TemplateBuilder cv, Header header, Canvas canvas) {
        var number = header.getPhoneNumber();
        var address = header.getAddress();
        var email = header.getEmail();

        var linkedIn = header.getLinkedIn();
        var gitHub = header.getGitHub();

        Entity artemDemchyshyn = cv.name(header.getName());

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

    private Entity letterSection(TemplateBuilder cv,
                                 List<String> content, float width, String bullet, BlockIndentStrategy strategy) {
        return cv.blockText(content, width, bullet, strategy);
    }
}
