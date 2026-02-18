package com.demcha.Templatese.template;

import com.demcha.Templatese.JobDetails;
import com.demcha.Templatese.data.Header;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.nio.file.Path;

public interface CoverLetterTemplate {

    /**
     * Unique identifier for this template.
     * Used to select template via API (e.g., "modern-professional", "classic",
     * "minimal").
     */
    String getTemplateId();

    /**
     * Human-readable name of the template.
     * Displayed to users in template selection UI.
     */
    String getTemplateName();

    /**
     * Optional description of the template.
     */
    default String getDescription() {
        return "";
    }

    /**
     * Renders a PDF document using this template.
     *
     * @param header
     * @param wroteLetter
     * @return A PDDocument that can be saved or streamed
     */
    PDDocument render(Header header, String wroteLetter, JobDetails jobDetails);

    void render(Header header, String wroteLetter, JobDetails jobDetails, Path path);
}
