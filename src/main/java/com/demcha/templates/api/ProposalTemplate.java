package com.demcha.templates.api;

import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.templates.data.ProposalData;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.nio.file.Path;

/**
 * Compose-first contract for reusable proposal templates.
 * <p>
 * Implementations should compose scene structure through
 * {@link DocumentComposer}. The deprecated {@code render(...)} overloads remain
 * as PDF convenience adapters.
 * </p>
 *
 * @deprecated Use {@link com.demcha.compose.document.templates.api.ProposalTemplate}
 *             instead.
 */
@Deprecated(forRemoval = false)
public interface ProposalTemplate {

    String getTemplateId();

    String getTemplateName();

    default String getDescription() {
        return "";
    }

    /**
     * Composes this template into the provided document composer.
     */
    void compose(DocumentComposer composer, ProposalData data);

    /**
     * Convenience PDF adapter for callers that still want a {@link PDDocument}.
     * Prefer {@link #compose(DocumentComposer, ProposalData)} for new
     * integrations.
     */
    @Deprecated(forRemoval = false)
    PDDocument render(ProposalData data);

    /**
     * Convenience PDF adapter for callers that still want a {@link PDDocument}.
     * Prefer {@link #compose(DocumentComposer, ProposalData)} for new
     * integrations.
     */
    @Deprecated(forRemoval = false)
    PDDocument render(ProposalData data, boolean guideLines);

    /**
     * Convenience PDF adapter for callers that still want file output written by
     * the template itself. Prefer
     * {@link #compose(DocumentComposer, ProposalData)} for new integrations.
     */
    @Deprecated(forRemoval = false)
    void render(ProposalData data, Path path);

    /**
     * Convenience PDF adapter for callers that still want file output written by
     * the template itself. Prefer
     * {@link #compose(DocumentComposer, ProposalData)} for new integrations.
     */
    @Deprecated(forRemoval = false)
    void render(ProposalData data, Path path, boolean guideLines);
}
