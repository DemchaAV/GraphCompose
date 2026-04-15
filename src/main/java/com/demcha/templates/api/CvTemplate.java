package com.demcha.templates.api;

import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.templates.data.MainPageCV;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.nio.file.Path;

/**
 * Compose-first contract for reusable CV templates.
 * <p>
 * Implementations should describe the document against a backend-neutral
 * {@link DocumentComposer}. The deprecated {@code render(...)} overloads remain
 * as convenience adapters for callers that still want PDFBox output directly.
 * </p>
 *
 * @deprecated Use {@link com.demcha.compose.document.templates.api.CvTemplate}
 *             with {@link com.demcha.compose.document.api.DocumentSession}
 *             instead.
 */
@Deprecated(forRemoval = false)
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
     * Composes this template into the provided document composer.
     *
     * @param originalCv  The original CV data (contains personal info like phone,
     *                    address).
     * @param rewrittenCv The rewritten CV data (contains optimized content).
     */
    void compose(DocumentComposer composer, MainPageCV originalCv, MainPageCvDTO rewrittenCv);

    /**
     * Convenience PDF adapter for callers that still want a {@link PDDocument}.
     * Prefer {@link #compose(DocumentComposer, MainPageCV, MainPageCvDTO)} for
     * new integrations.
     */
    @Deprecated(forRemoval = false)
    PDDocument render(MainPageCV originalCv, MainPageCvDTO rewrittenCv);

    /**
     * Convenience PDF adapter for callers that still want a {@link PDDocument}.
     * Prefer {@link #compose(DocumentComposer, MainPageCV, MainPageCvDTO)} for
     * new integrations.
     */
    @Deprecated(forRemoval = false)
    PDDocument render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, boolean guideLines);

    /**
     * Convenience PDF adapter for callers that still want file output written by
     * the template itself. Prefer
     * {@link #compose(DocumentComposer, MainPageCV, MainPageCvDTO)} for new
     * integrations.
     *
     * @param originalCv  The original CV data (contains personal info like phone, address).
     * @param rewrittenCv The rewritten CV data (contains optimized content).
     * @param path        The file path where the generated PDF should be saved.
     */
    @Deprecated(forRemoval = false)
    void render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, Path path);

    /**
     * Convenience PDF adapter for callers that still want file output written by
     * the template itself. Prefer
     * {@link #compose(DocumentComposer, MainPageCV, MainPageCvDTO)} for new
     * integrations.
     */
    @Deprecated(forRemoval = false)
    void render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, Path path, boolean guideLines);
}
