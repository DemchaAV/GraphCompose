package com.demcha.compose.document.templates.api;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.data.coverletter.CoverLetterDocumentSpec;

/**
 * Canonical compose contract for reusable cover-letter templates.
 *
 * <p><b>Responsibility:</b> define one reusable cover-letter scene that writes
 * semantic blocks into a caller-owned {@link DocumentSession}. Implementations
 * are usually immutable value objects configured by a theme.</p>
 *
 * @deprecated since 1.9.0; removed in 2.0. No type implements this interface —
 *             the layered cover-letter presets implement the generic
 *             {@link DocumentTemplate} seam
 *             ({@code DocumentTemplate<CoverLetterDocumentSpec>}) instead.
 *             Implement {@code DocumentTemplate<CoverLetterDocumentSpec>}.
 */
@Deprecated(since = "1.9.0", forRemoval = true)
public interface CoverLetterTemplate {

    /**
     * Stable public template identifier.
     *
     * @return unique template id used by registries and integrations
     */
    String getTemplateId();

    /**
     * Human-readable display name.
     *
     * @return template display name
     */
    String getTemplateName();

    /**
     * Optional human-readable description.
     *
     * @return template description, or an empty string when omitted
     */
    default String getDescription() {
        return "";
    }

    /**
     * Composes a cover letter into a live document session.
     *
     * @param document active mutable document session receiving template nodes
     * @param spec     cover-letter document spec
     * @throws NullPointerException if an implementation requires non-null inputs
     */
    void compose(DocumentSession document, CoverLetterDocumentSpec spec);
}
