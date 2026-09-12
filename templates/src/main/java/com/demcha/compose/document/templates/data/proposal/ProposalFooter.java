package com.demcha.compose.document.templates.data.proposal;

import java.util.List;
import java.util.Objects;

/**
 * The issuer's own identity, as the foot of a proposal states it.
 *
 * <p>Distinct from {@link ProposalBrand#footerName()}, which is one name: a
 * sales proposal's foot carries the legal entity, its registered address, the
 * channels a reader can follow, and — where the jurisdiction or the deal asks
 * for it — a confidentiality line. A design that prints only the name leaves the
 * rest blank.</p>
 *
 * @param name            the legal entity the proposal is issued by
 * @param addressLines    its registered address, in the order it is printed
 * @param contacts        the channels the foot lists, as the sheet prints them —
 *                        a site, a number, an address — in order
 * @param confidentiality the confidentiality or validity line, when the design
 *                        carries one; blank otherwise
 */
public record ProposalFooter(String name, List<String> addressLines,
                             List<String> contacts, String confidentiality) {

    /**
     * Normalizes optional fields.
     */
    public ProposalFooter {
        name = Objects.requireNonNullElse(name, "");
        addressLines = List.copyOf(Objects.requireNonNullElse(addressLines, List.of()));
        contacts = List.copyOf(Objects.requireNonNullElse(contacts, List.of()));
        confidentiality = Objects.requireNonNullElse(confidentiality, "");
    }

    /**
     * Whether there is anything to draw.
     *
     * @return {@code true} when the foot states any of its four parts
     */
    public boolean isPresent() {
        return !name.isBlank() || !addressLines.isEmpty()
                || !contacts.isEmpty() || !confidentiality.isBlank();
    }
}
