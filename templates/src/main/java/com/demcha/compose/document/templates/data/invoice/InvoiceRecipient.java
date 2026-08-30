package com.demcha.compose.document.templates.data.invoice;

import java.util.List;
import java.util.Objects;

/**
 * The billed-to block of a structured invoice.
 *
 * <p>The block owns its heading, and the lines under the recipient's name
 * come in two groups because a preset sets them apart: the {@code subline}
 * is the single attention line — a department, a contact, a cost centre —
 * that sits directly under the name, and {@code addressLines} is the
 * address block under that.</p>
 *
 * @param heading      the block heading (e.g. {@code "BILLED TO"})
 * @param name         the recipient's name, set apart from the lines below
 * @param subline      the attention line under the name; empty when the
 *                     recipient has none
 * @param addressLines the address lines, in order
 * @param emailLabel   the label printed before the email address
 * @param email        the recipient's email address
 */
public record InvoiceRecipient(
        String heading,
        String name,
        String subline,
        List<String> addressLines,
        String emailLabel,
        String email) {

    /**
     * Normalizes optional fields and freezes the address lines.
     */
    public InvoiceRecipient {
        heading = Objects.requireNonNullElse(heading, "");
        name = Objects.requireNonNullElse(name, "");
        subline = Objects.requireNonNullElse(subline, "");
        addressLines = List.copyOf(Objects.requireNonNullElse(addressLines, List.of()));
        emailLabel = Objects.requireNonNullElse(emailLabel, "");
        email = Objects.requireNonNullElse(email, "");
    }
}
