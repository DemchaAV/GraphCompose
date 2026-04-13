package com.demcha.templates.builtins;

import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.templates.CvTheme;
import com.demcha.templates.JobDetails;
import com.demcha.templates.api.CoverLetterTemplate;
import com.demcha.templates.data.Header;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;

public class CoverLetterTemplateV1 extends PdfTemplateAdapterSupport implements CoverLetterTemplate {
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
        return renderToDocument(
                guideLines,
                "Failed to generate cover letter",
                this::createPdfComposer,
                composer -> compose(composer, header, wroteLetter, jobDetails));
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
        renderToFile(
                path,
                guideLines,
                "Failed to generate cover letter",
                "Cover letter saved to {}",
                this::createPdfComposer,
                composer -> compose(composer, header, wroteLetter, jobDetails));
    }

    /**
     * Deprecated compatibility adapter for direct PDF file writing.
     */
    @Deprecated(forRemoval = false)
    @Override
    public void render(Header header, String wroteLetter, JobDetails jobDetails, Path path) {
        render(header, wroteLetter, jobDetails, path, false);
    }

    private com.demcha.compose.layout_core.core.PdfComposer createPdfComposer(Path path, boolean guideLines) {
        return createPdfComposer(path, guideLines, PDRectangle.A4, 15, 10, 15, 15);
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
