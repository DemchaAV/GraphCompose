package com.demcha.compose.document.templates.data.invoice;

import java.util.List;
import java.util.Objects;

/**
 * The billed-to block of a structured invoice.
 *
 * <p>The block owns its heading, and everything under the recipient's name
 * is an authored line — department, street, city, country — so the caller
 * decides how many lines the address takes and in what order.</p>
 *
 * @param heading    the block heading (e.g. {@code "BILLED TO"})
 * @param name       the recipient's name, set apart from the lines below
 * @param lines      the address lines under the name, in order
 * @param emailLabel the label printed before the email address
 * @param email      the recipient's email address
 */
public record InvoiceRecipient(
        String heading,
        String name,
        List<String> lines,
        String emailLabel,
        String email) {

    /**
     * Normalizes optional fields and freezes the address lines.
     */
    public InvoiceRecipient {
        heading = Objects.requireNonNullElse(heading, "");
        name = Objects.requireNonNullElse(name, "");
        lines = List.copyOf(Objects.requireNonNullElse(lines, List.of()));
        emailLabel = Objects.requireNonNullElse(emailLabel, "");
        email = Objects.requireNonNullElse(email, "");
    }
}
