package com.demcha.compose.document.templates.data.invoice;

import java.util.Objects;

/**
 * The page-foot line of a structured invoice: who issued it and under
 * which registration.
 *
 * @param legalName        the registered business name
 * @param registrationText the registration text as printed
 *                         (e.g. {@code "ABN 12 345 678 901"})
 */
public record InvoiceFooterLine(String legalName, String registrationText) {

    /**
     * Normalizes optional fields to empty strings.
     */
    public InvoiceFooterLine {
        legalName = Objects.requireNonNullElse(legalName, "");
        registrationText = Objects.requireNonNullElse(registrationText, "");
    }
}
