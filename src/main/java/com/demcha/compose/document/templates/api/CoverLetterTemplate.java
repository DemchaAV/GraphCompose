package com.demcha.compose.document.templates.api;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.data.Header;
import com.demcha.compose.document.templates.data.JobDetails;

/**
 * Canonical compose contract for reusable cover-letter templates.
 */
public interface CoverLetterTemplate {

    String getTemplateId();

    String getTemplateName();

    default String getDescription() {
        return "";
    }

    /**
     * Composes a cover letter into a live document session.
     *
     * @param document active mutable document session receiving template nodes
     * @param header contact and profile header data
     * @param wroteLetter cover-letter body text or template text
     * @param jobDetails job metadata used for personalization
     */
    void compose(DocumentSession document, Header header, String wroteLetter, JobDetails jobDetails);
}
