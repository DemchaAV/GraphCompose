package com.demcha.templates.data;

import java.util.List;
import java.util.Objects;

/**
 * Display-oriented invoice document input.
 */
public record InvoiceData(
        String title,
        String invoiceNumber,
        String issueDate,
        String dueDate,
        String reference,
        String status,
        InvoiceParty fromParty,
        InvoiceParty billToParty,
        List<InvoiceLineItem> lineItems,
        List<InvoiceSummaryRow> summaryRows,
        List<String> notes,
        List<String> paymentTerms,
        String footerNote) {

    public InvoiceData {
        title = Objects.requireNonNullElse(title, "Invoice");
        invoiceNumber = Objects.requireNonNullElse(invoiceNumber, "");
        issueDate = Objects.requireNonNullElse(issueDate, "");
        dueDate = Objects.requireNonNullElse(dueDate, "");
        reference = Objects.requireNonNullElse(reference, "");
        status = Objects.requireNonNullElse(status, "");
        lineItems = List.copyOf(Objects.requireNonNullElse(lineItems, List.of()));
        summaryRows = List.copyOf(Objects.requireNonNullElse(summaryRows, List.of()));
        notes = List.copyOf(Objects.requireNonNullElse(notes, List.of()));
        paymentTerms = List.copyOf(Objects.requireNonNullElse(paymentTerms, List.of()));
        footerNote = Objects.requireNonNullElse(footerNote, "");
    }
}
