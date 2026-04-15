package com.demcha.templates.builtins;

import com.demcha.compose.document.templates.support.CoverLetterTemplateComposer;
import com.demcha.compose.document.templates.support.LegacyComposerTemplateComposeTarget;
import com.demcha.compose.document.templates.support.LegacyTemplateMappers;
import com.demcha.compose.document.templates.support.SessionTemplateComposeTarget;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.templates.CvTheme;
import com.demcha.templates.JobDetails;
import com.demcha.templates.api.CoverLetterTemplate;
import com.demcha.templates.data.Header;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;

/**
 * Deprecated bridge to the canonical {@code document.templates} cover-letter template.
 */
@Deprecated(forRemoval = false)
public class CoverLetterTemplateV1 extends PdfTemplateAdapterSupport implements CoverLetterTemplate {
    private final CoverLetterTemplateComposer composer;

    public CoverLetterTemplateV1() {
        this.composer = new CoverLetterTemplateComposer(
                LegacyTemplateMappers.toCanonical(CvTheme.defaultTheme()),
                LegacyTemplateMappers.toCanonical(CvTheme.courier()));
    }

    @Override
    public void compose(DocumentComposer composer, Header header, String wroteLetter, JobDetails jobDetails) {
        this.composer.compose(
                new LegacyComposerTemplateComposeTarget(composer),
                LegacyTemplateMappers.toCanonical(header),
                wroteLetter,
                LegacyTemplateMappers.toCanonical(jobDetails));
    }

    @Deprecated(forRemoval = false)
    public PDDocument render(Header header, String wroteLetter, JobDetails jobDetails, boolean guideLines) {
        return renderToDocumentSession(
                guideLines,
                "Failed to generate cover letter",
                PDRectangle.A4,
                15,
                10,
                15,
                15,
                session -> composer.compose(
                        new SessionTemplateComposeTarget(session),
                        LegacyTemplateMappers.toCanonical(header),
                        wroteLetter,
                        LegacyTemplateMappers.toCanonical(jobDetails)));
    }

    @Deprecated(forRemoval = false)
    @Override
    public PDDocument render(Header header, String wroteLetter, JobDetails jobDetails) {
        return render(header, wroteLetter, jobDetails, false);
    }

    @Deprecated(forRemoval = false)
    public void render(Header header, String wroteLetter, JobDetails jobDetails, Path path, boolean guideLines) {
        renderToFileSession(
                path,
                guideLines,
                "Failed to generate cover letter",
                "Cover letter saved to {}",
                PDRectangle.A4,
                15,
                10,
                15,
                15,
                session -> composer.compose(
                        new SessionTemplateComposeTarget(session),
                        LegacyTemplateMappers.toCanonical(header),
                        wroteLetter,
                        LegacyTemplateMappers.toCanonical(jobDetails)));
    }

    @Deprecated(forRemoval = false)
    @Override
    public void render(Header header, String wroteLetter, JobDetails jobDetails, Path path) {
        render(header, wroteLetter, jobDetails, path, false);
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
