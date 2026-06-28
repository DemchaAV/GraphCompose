package com.demcha.compose.document.templates.api;

import com.demcha.compose.document.api.DocumentSession;

/**
 * Generic compose-first contract for all canonical document templates.
 *
 * <p>A {@code DocumentTemplate<S>} renders its domain-specific specification
 * {@code S} (CV, invoice, proposal, cover letter, etc.) into an active
 * {@link DocumentSession}. The interface is intentionally minimal — just
 * identity (for registry lookup) and one composition seam.</p>
 *
 * <p>Implementation contract:</p>
 * <ul>
 *   <li>Implementations are plain factory-style classes — a static
 *       {@code create(theme)} method returns a configured
 *       {@code DocumentTemplate<S>}. The theme type is the family's own:
 *       the layered cv / cover-letter presets take a {@code BrandTheme};
 *       the layered invoice / proposal presets take a
 *       {@code BusinessTheme}.</li>
 *   <li>Implementations are stateless after construction — composing the
 *       same spec twice produces the same output.</li>
 *   <li>Implementations do not call {@code session.buildPdf()}; the caller
 *       owns session lifecycle. They only place semantic nodes via the
 *       session DSL.</li>
 * </ul>
 *
 * @param <S> the spec type rendered by this template (e.g. {@code CvDocument},
 *            {@code InvoiceSpec}, {@code ProposalSpec})
 */
public interface DocumentTemplate<S> {

    /**
     * Returns the stable identifier used for registry lookup, for example
     * {@code "modern-professional"} or {@code "nordic-clean"}.
     *
     * <p>The id is part of the public contract: stored configuration files
     * and CLI tools may reference templates by id. Renaming an id is a
     * breaking change.</p>
     *
     * @return non-blank stable identifier
     */
    String id();

    /**
     * Returns the human-readable display name for the template, for example
     * {@code "Modern Professional"} or {@code "Nordic Clean"}.
     *
     * <p>Display names are not part of the stable contract — they may be
     * adjusted between versions for clarity or localisation.</p>
     *
     * @return non-blank display name
     */
    String displayName();

    /**
     * Renders the given specification into the supplied document session.
     *
     * <p>Implementations must:</p>
     * <ul>
     *   <li>Add semantic nodes via the session DSL ({@code session.addSection(...)},
     *       {@code session.pageFlow(...)}, etc.) — never via direct backend calls.</li>
     *   <li>Honour the session's currently configured theme, spacing tokens,
     *       and page size.</li>
     *   <li>Leave the session open: rendering or finalisation
     *       ({@code buildPdf}, {@code writePdf}) is the caller's responsibility.</li>
     * </ul>
     *
     * @param session active document session to compose into
     * @param spec    domain-specific data populating the template
     * @throws NullPointerException     if {@code session} or {@code spec} is null
     * @throws IllegalArgumentException if a required slot or module name
     *                                  declared by the template cannot be resolved against
     *                                  {@code spec}
     */
    void compose(DocumentSession session, S spec);
}
