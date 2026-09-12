package com.demcha.compose.document.templates.data.proposal;

import java.util.List;
import java.util.Objects;

/**
 * The organisation a proposal is addressed to.
 *
 * <p>Distinct from {@link ProposalMetaLine#preparedFor()}, which is one line of
 * a header strip: this is the addressed block a sales proposal opens with — a
 * caption, the company, and its address — and a design that prints it needs all
 * three separately rather than as one string.</p>
 *
 * @param label        the caption above the block, as the sheet prints it
 *                     (e.g. {@code "PREPARED FOR"}); blank when the design draws
 *                     the block without one
 * @param name         the addressed organisation
 * @param addressLines the address, in the order it is printed
 */
public record ProposalRecipient(String label, String name, List<String> addressLines) {

    /**
     * Normalizes optional fields.
     */
    public ProposalRecipient {
        label = Objects.requireNonNullElse(label, "");
        name = Objects.requireNonNullElse(name, "");
        addressLines = List.copyOf(Objects.requireNonNullElse(addressLines, List.of()));
    }

    /**
     * Whether there is anything to draw.
     *
     * @return {@code true} when the block names an organisation or an address
     */
    public boolean isPresent() {
        return !name.isBlank() || !addressLines.isEmpty();
    }
}
