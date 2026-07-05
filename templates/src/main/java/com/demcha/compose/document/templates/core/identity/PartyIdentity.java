package com.demcha.compose.document.templates.core.identity;

import java.util.List;

/**
 * Family-neutral identity contract rendered by the shared header widgets
 * ({@link Headline}, {@link ContactLine}, {@link Masthead}). A CV supplies a
 * person; an invoice or proposal supplies an organisation — both implement
 * this interface, so the same widgets draw either masthead without knowing the
 * family's concrete data model.
 *
 * @since 2.0.0
 */
public interface PartyIdentity {

    /**
     * Full display name — a person's full name for a CV, a company name for an
     * invoice or proposal.
     *
     * @return the display name, never {@code null}
     */
    String displayName();

    /**
     * Secondary line shown under the name — a job title for a CV, a strapline
     * for an organisation. May be empty when the family has no tagline.
     *
     * @return the tagline, never {@code null} (empty when absent)
     */
    String tagline();

    /**
     * Contact block (phone / email / address).
     *
     * @return the contact details, never {@code null}
     */
    Contact contact();

    /**
     * Labelled links (portfolio, social, web).
     *
     * @return the links in display order, never {@code null}
     */
    List<Link> links();
}
