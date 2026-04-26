package com.demcha.compose.document.templates.api;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;

/**
 * Canonical compose contract for reusable CV templates.
 *
 * <p>Implementations author content directly against a live
 * {@link DocumentSession}, which means callers can inspect layout snapshots,
 * export through semantic backends, or render to PDF through the canonical
 * document API pipeline.</p>
 *
 * <p><b>Responsibility:</b> provide one stable, reusable CV scene definition.
 * Implementations are typically immutable and reusable across sessions, but the
 * contract itself does not require thread-safety.</p>
 *
 * <pre>{@code
 * CvTemplate template = new CvTemplateV1();
 * CvDocumentSpec cv = CvDocumentSpec.builder()
 *         .header(header)
 *         .summary("Backend engineer building reliable document systems.")
 *         .technicalSkills("Java 21", "Spring Boot", "GraphCompose")
 *         .rows("Projects", "GraphCompose - Canonical document engine.")
 *         .build();
 *
 * try (DocumentSession document = GraphCompose.document(Path.of("cv.pdf")).create()) {
 *     template.compose(document, cv);
 *     document.buildPdf();
 * }
 * }</pre>
 */
public interface CvTemplate {

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
     * Composes the template into an existing semantic document session.
     *
     * @param document active mutable document session receiving template nodes
     * @param documentSpec header plus ordered content modules
     * @throws NullPointerException if an implementation requires non-null inputs
     */
    void compose(DocumentSession document, CvDocumentSpec documentSpec);

}
