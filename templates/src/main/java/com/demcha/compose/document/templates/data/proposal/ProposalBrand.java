package com.demcha.compose.document.templates.data.proposal;

import java.util.Objects;

/**
 * The sender's brand marks for a structured proposal: the logo monogram,
 * the two wordmark lines, the tracked document label, and the footer line.
 *
 * <p>These are display strings, not identity data — the narrative model's
 * {@link ProposalParty} carries the sender's address and contact details;
 * this record carries only what the page chrome draws.</p>
 *
 * @param monogram      the one- or two-letter logo mark
 * @param nameLine1     first wordmark line
 * @param nameLine2     second wordmark line
 * @param documentLabel the document label, set in tracked capitals
 * @param website       the website line shown in the footer
 * @param footerName    the brand name shown in the footer
 */
public record ProposalBrand(
        String monogram,
        String nameLine1,
        String nameLine2,
        String documentLabel,
        String website,
        String footerName) {

    /**
     * Normalizes optional fields to empty strings.
     */
    public ProposalBrand {
        monogram = Objects.requireNonNullElse(monogram, "");
        nameLine1 = Objects.requireNonNullElse(nameLine1, "");
        nameLine2 = Objects.requireNonNullElse(nameLine2, "");
        documentLabel = Objects.requireNonNullElse(documentLabel, "");
        website = Objects.requireNonNullElse(website, "");
        footerName = Objects.requireNonNullElse(footerName, "");
    }
}
