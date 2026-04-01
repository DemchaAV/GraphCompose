package com.demcha.templates.builtins;

import com.demcha.templates.CvTheme;
import com.demcha.templates.JobDetails;
import com.demcha.templates.TemplateBuilder;
import com.demcha.templates.data.Header;
import com.demcha.templates.api.CoverLetterTemplate;
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
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.compose.layout_core.core.PdfComposer;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.util.List;

@Slf4j
public class CoverLetterTemplateV1 implements CoverLetterTemplate {
    private static final String MAIN_CONTAINER_NAME = "MainVBoxContainer";
    private static final String HEADER_ENTITY_NAME = "ModuleHeader";
    private static final String DEFAULT_BULLET_OFFSET = "  ";
    private static final String KIND_REGARDS = "Kind regards,";

    /**
     * Unique identifier for this template.
     * Used to select template via API (e.g., "modern-professional", "classic",
     * "minimal").
     */
    @Override
    public String getTemplateId() {
        return "cover-letter-v1";
    }

    /**
     * Human-readable name of the template.
     * Displayed to users in template selection UI.
     */
    @Override
    public String getTemplateName() {
        return "Cover Letter V1";
    }

    /**
     * Optional description of the template.
     */
    @Override
    public String getDescription() {
        return "A cover letter template with header details, contact links, and a single-column letter body.";
    }

    public PDDocument render(Header header, String wroteLetter, JobDetails jobDetails, boolean guideLines) {
        try {
            PdfComposer composer = createPdfComposer(guideLines);
            designLetter(header, wroteLetter, jobDetails, composer);
            return composer.toPDDocument();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CV", e);
        }
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
        return render(header, wroteLetter, jobDetails, false);
    }

    public void render(Header header, String wroteLetter, JobDetails jobDetails, Path path, boolean guideLines) {
        try (PdfComposer composer = createPdfComposer(path, guideLines)) {

            designLetter(header, wroteLetter, jobDetails, composer);

            composer.build();
            log.info("Cover letter saved to {}", path.toAbsolutePath());

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CV", e);
        }
    }

    @Override
    public void render(Header header, String wroteLetter, JobDetails jobDetails, Path path) {
        render(header, wroteLetter, jobDetails, path, false);
    }

    private void designLetter(Header header, String wroteLetter, JobDetails jobDetails, DocumentComposer composer) {
        Canvas canvas = composer.canvas();
        TemplateBuilder cv = TemplateBuilder.from(composer.componentBuilder(), CvTheme.defaultTheme());
        Entity moduleHeader = createHeader(cv, header, canvas);
        Entity coverLetter = createLetterSection(cv, wroteLetter, jobDetails, canvas);
        Entity kindRegards = createClosingSignature(composer, header, canvas);

        cv.pageFlow(canvas)
                .entityName(MAIN_CONTAINER_NAME)
                .addChild(moduleHeader)
                .addChild(coverLetter)
                .addChild(kindRegards)
                .build();
    }

    private PdfComposer createPdfComposer(Path path, boolean guideLines) {
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

    private PdfComposer createPdfComposer(boolean guideLines) {
        return createPdfComposer(null, guideLines);
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

        return cv.componentBuilder()
                .moduleBuilder(Align.middle(5), canvas)
                .entityName(HEADER_ENTITY_NAME)
                .margin(new Margin(0, 10, 10, 10))
                .anchor(Anchor.topRight())
                .addChild(artemDemchyshyn)
                .addChild(infoPanel)
                .addChild(linksPanel)
                .build();
    }

    private Entity createLetterSection(TemplateBuilder cv, String wroteLetter, JobDetails jobDetails, Canvas canvas) {
        String resolvedLetter = wroteLetter.replace("${companyName}", jobDetails.company());
        return cv.moduleBuilder(canvas)
                .entityName("CoverLetterBody")
                .addChild(cv.blockText(
                        List.of(resolvedLetter),
                        (float) canvas.innerWidth(),
                        DEFAULT_BULLET_OFFSET,
                        BlockIndentStrategy.FIRST_LINE))
                .build();
    }

    private Entity createClosingSignature(DocumentComposer composer, Header header, Canvas canvas) {
        Entity kindRegards = composer.componentBuilder()
                .blockText(Align.left(CvTheme.courier().spacing()), CvTheme.defaultTheme().bodyTextStyle())
                .size(canvas.innerWidth(), 2)
                .text(
                        List.of(KIND_REGARDS, header.getName()),
                        CvTheme.defaultTheme().bodyTextStyle(),
                        Padding.zero(),
                        new Margin(20, 20, 0, 0))
                .build();
        kindRegards.addComponent(Anchor.topRight());
        return composer.componentBuilder()
                .moduleBuilder(Align.middle(CvTheme.defaultTheme().spacing()), canvas)
                .entityName("CoverLetterClosingSignature")
                .addChild(kindRegards)
                .build();
    }
}

