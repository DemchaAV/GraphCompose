package com.demcha.templates.builtins;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.templates.CvTheme;
import com.demcha.templates.JobDetails;
import com.demcha.templates.api.CoverLetterTemplate;
import com.demcha.templates.data.Header;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;

@Slf4j
public class CoverLetterTemplateV1 implements CoverLetterTemplate {
    private final CoverLetterSceneBuilder sceneBuilder;

    public CoverLetterTemplateV1() {
        this.sceneBuilder = new CoverLetterSceneBuilder(CvTheme.defaultTheme(), CvTheme.courier());
    }

    /**
     * Preferred backend-neutral template contract.
     */
    @Override
    public void compose(DocumentComposer composer, Header header, String wroteLetter, JobDetails jobDetails) {
        sceneBuilder.compose(composer, header, wroteLetter, jobDetails);
    }

    /**
     * Deprecated compatibility adapter for direct PDFBox document output.
     */
    @Deprecated(forRemoval = false)
    public PDDocument render(Header header, String wroteLetter, JobDetails jobDetails, boolean guideLines) {
        try {
            PdfComposer composer = createPdfComposer(guideLines);
            compose(composer, header, wroteLetter, jobDetails);
            return composer.toPDDocument();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CV", e);
        }
    }

    /**
     * Deprecated compatibility adapter for direct PDFBox document output.
     */
    @Deprecated(forRemoval = false)
    @Override
    public PDDocument render(Header header, String wroteLetter, JobDetails jobDetails) {
        return render(header, wroteLetter, jobDetails, false);
    }

    /**
     * Deprecated compatibility adapter for direct PDF file writing.
     */
    @Deprecated(forRemoval = false)
    public void render(Header header, String wroteLetter, JobDetails jobDetails, Path path, boolean guideLines) {
        try (PdfComposer composer = createPdfComposer(path, guideLines)) {
            compose(composer, header, wroteLetter, jobDetails);
            composer.build();
            log.info("Cover letter saved to {}", path.toAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CV", e);
        }
    }

    /**
     * Deprecated compatibility adapter for direct PDF file writing.
     */
    @Deprecated(forRemoval = false)
    @Override
    public void render(Header header, String wroteLetter, JobDetails jobDetails, Path path) {
        render(header, wroteLetter, jobDetails, path, false);
    }

    private PdfComposer createPdfComposer(Path path, boolean guideLines) {
        GraphCompose.PdfBuilder composer = path != null ? GraphCompose.pdf(path) : GraphCompose.pdf();
        return composer.pageSize(PDRectangle.A4)
                .margin(15, 10, 15, 15)
                .markdown(true)
                .guideLines(guideLines)
                .create();
    }

    private PdfComposer createPdfComposer(boolean guideLines) {
        return createPdfComposer(null, guideLines);
    }

    @Override
    public String getTemplateId() {
        return "cover-letter-v1";
    }

    @Override
    public String getTemplateName() {
        return "Cover Letter V1";
    }

    @Override
    public String getDescription() {
        return "A cover letter template with header details, contact links, and a single-column letter body.";
    }
}
