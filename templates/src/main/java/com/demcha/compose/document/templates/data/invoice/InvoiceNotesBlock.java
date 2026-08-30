package com.demcha.compose.document.templates.data.invoice;

import java.util.List;
import java.util.Objects;

/**
 * The closing notes of a structured invoice, with the channels to raise a
 * query on.
 *
 * @param heading      the block heading (e.g. {@code "NOTES"})
 * @param paragraphs   the note paragraphs, in order
 * @param contactEmail the email address a query goes to
 * @param contactPhone the phone number a query goes to
 */
public record InvoiceNotesBlock(
        String heading,
        List<String> paragraphs,
        String contactEmail,
        String contactPhone) {

    /**
     * Normalizes optional fields and freezes the paragraph list.
     */
    public InvoiceNotesBlock {
        heading = Objects.requireNonNullElse(heading, "");
        paragraphs = List.copyOf(Objects.requireNonNullElse(paragraphs, List.of()));
        contactEmail = Objects.requireNonNullElse(contactEmail, "");
        contactPhone = Objects.requireNonNullElse(contactPhone, "");
    }
}
