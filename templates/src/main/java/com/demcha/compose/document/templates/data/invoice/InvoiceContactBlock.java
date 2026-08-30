package com.demcha.compose.document.templates.data.invoice;

import java.util.List;
import java.util.Objects;

/**
 * The sender's address and contact channels on a structured invoice.
 *
 * <p>Distinct from {@link InvoiceParty}, the narrative model's party
 * record: this block carries a website channel and a labelled business
 * registration (ABN, VAT or company number — the label is content because
 * it differs by jurisdiction), which a structured invoice prints in its
 * contact band and its footer.</p>
 *
 * @param legalName          the registered business name
 * @param addressLines       the address lines, in order
 * @param phone              the phone channel
 * @param email              the email channel
 * @param website            the website channel
 * @param registrationLabel  the label of the registration number
 *                           (e.g. {@code "ABN"})
 * @param registrationNumber the registration number itself
 */
public record InvoiceContactBlock(
        String legalName,
        List<String> addressLines,
        String phone,
        String email,
        String website,
        String registrationLabel,
        String registrationNumber) {

    /**
     * Normalizes optional fields and freezes the address lines.
     */
    public InvoiceContactBlock {
        legalName = Objects.requireNonNullElse(legalName, "");
        addressLines = List.copyOf(Objects.requireNonNullElse(addressLines, List.of()));
        phone = Objects.requireNonNullElse(phone, "");
        email = Objects.requireNonNullElse(email, "");
        website = Objects.requireNonNullElse(website, "");
        registrationLabel = Objects.requireNonNullElse(registrationLabel, "");
        registrationNumber = Objects.requireNonNullElse(registrationNumber, "");
    }
}
