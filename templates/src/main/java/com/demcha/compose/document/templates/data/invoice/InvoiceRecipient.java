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
 * @param registrationLabel  the label a printed registration is named by
 *                           (e.g. {@code "VAT Number"}); blank when absent
 * @param registrationNumber the registration itself, printed under the
 *                           address rather than under the name; blank when
 *                           absent
 */
public record InvoiceRecipient(
        String heading,
        String name,
        String subline,
        List<String> addressLines,
        String emailLabel,
        String email,
        String registrationLabel,
        String registrationNumber) {

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
        registrationLabel = Objects.requireNonNullElse(registrationLabel, "");
        registrationNumber = Objects.requireNonNullElse(registrationNumber, "");
    }

    /**
     * Backward-compatible constructor for callers that predate the printed
     * registration. The recipient simply carries none.
     *
     * @param heading      the block heading
     * @param name         the recipient's name
     * @param subline      the attention line under the name
     * @param addressLines the address lines, in order
     * @param emailLabel   the label printed before the email address
     * @param email        the recipient's email address
     */
    public InvoiceRecipient(String heading, String name, String subline,
                            List<String> addressLines, String emailLabel, String email) {
        this(heading, name, subline, addressLines, emailLabel, email, "", "");
    }

    /**
     * Whether this recipient prints a registration under its address.
     *
     * @return {@code true} when the number is set
     */
    public boolean hasRegistration() {
        return !registrationNumber.isBlank();
    }
}
