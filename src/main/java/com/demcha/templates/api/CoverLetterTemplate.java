package com.demcha.templates.api;

import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.templates.JobDetails;
import com.demcha.templates.data.Header;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.nio.file.Path;

/**
 * Compose-first contract for reusable cover-letter templates.
 * <p>
 * Implementations should assemble document structure through
 * {@link DocumentComposer}. The deprecated {@code render(...)} overloads remain
 * as PDF adapter conveniences for compatibility.
 * </p>
 */
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
     * Composes this template into the provided document composer.
     *
     * @param header
     * @param wroteLetter
     * @param jobDetails
     */
    void compose(DocumentComposer composer, Header header, String wroteLetter, JobDetails jobDetails);

    /**
     * Convenience PDF adapter for callers that still want a {@link PDDocument}.
     * Prefer {@link #compose(DocumentComposer, Header, String, JobDetails)} for
     * new integrations.
     */
    @Deprecated(forRemoval = false)
    PDDocument render(Header header, String wroteLetter, JobDetails jobDetails);

    /**
     * Convenience PDF adapter for callers that still want file output written by
     * the template itself. Prefer
     * {@link #compose(DocumentComposer, Header, String, JobDetails)} for new
     * integrations.
     */
    @Deprecated(forRemoval = false)
    void render(Header header, String wroteLetter, JobDetails jobDetails, Path path);
}
