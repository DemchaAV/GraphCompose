package com.demcha.Templatese.template;

import com.demcha.Templatese.data.MainPageCV;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.nio.file.Path;

/**
 * Interface for CV PDF templates.
 * Implement this interface to create reusable PDF templates that can be
 * selected by users.
 */
public interface CvTemplate {

    /**
     * Unique identifier for this template.
     * Used to select template via API (e.g., "modern-professional", "classic",
     * "minimal").
     *
     * @return The unique template ID.
     */
    String getTemplateId();

    /**
     * Human-readable name of the template.
     * Displayed to users in template selection UI.
     *
     * @return The display name of the template.
     */
    String getTemplateName();

    /**
     * Optional description of the template.
     *
     * @return A description of the template, or an empty string if none provided.
     */
    default String getDescription() {
        return "";
    }

    /**
     * Renders a PDF document using this template.
     *
     * @param originalCv  The original CV data (contains personal info like phone,
     *                    address).
     * @param rewrittenCv The rewritten CV data (contains optimized content).
     * @return A PDDocument that can be saved or streamed.
     */
    PDDocument render(MainPageCV originalCv, MainPageCvDTO rewrittenCv);

    PDDocument render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, boolean guideLines);

    /**
     * Renders a PDF document using this template and saves it to the specified path.
     *
     * @param originalCv  The original CV data (contains personal info like phone, address).
     * @param rewrittenCv The rewritten CV data (contains optimized content).
     * @param path        The file path where the generated PDF should be saved.
     */
    void render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, Path path);

    void render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, Path path, boolean guideLines);
}
