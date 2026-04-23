package com.demcha.compose.document.templates.api;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.data.common.Header;
import com.demcha.compose.document.templates.data.coverletter.JobDetails;

/**
 * Canonical compose contract for reusable cover-letter templates.
 *
 * <p><b>Responsibility:</b> define one reusable cover-letter scene that writes
 * semantic blocks into a caller-owned {@link DocumentSession}. Implementations
 * are usually immutable value objects configured by a theme.</p>
 *
 * <pre>{@code
 * CoverLetterTemplate template = new CoverLetterTemplateV1();
 * Header header = Header.builder()
 *         .name("Artem Demchyshyn")
 *         .email("artem@demo.dev", "artem@demo.dev")
 *         .linkedIn("https://linkedin.com/in/graphcompose", "LinkedIn")
 *         .build();
 * JobDetails job = JobDetails.builder()
 *         .company("Northwind Systems")
 *         .title("Platform Engineer")
 *         .build();
 *
 * try (DocumentSession document = GraphCompose.document(Path.of("cover-letter.pdf")).create()) {
 *     template.compose(document, header, letterBody, job);
 *     document.buildPdf();
 * }
 * }</pre>
 */
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
     * @param header contact and profile header data
     * @param wroteLetter cover-letter body text or template text
     * @param jobDetails job metadata used for personalization
     * @throws NullPointerException if an implementation requires non-null inputs
     */
    void compose(DocumentSession document, Header header, String wroteLetter, JobDetails jobDetails);
}
