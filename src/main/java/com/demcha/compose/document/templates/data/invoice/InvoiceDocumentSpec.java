package com.demcha.compose.document.templates.data.invoice;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Public compose-first invoice input.
 *
 * <p><b>Authoring role:</b> gives callers one document-level object to pass to
 * invoice templates while keeping the familiar invoice-domain builder methods:
 * parties, line items, notes, payment terms, and totals.</p>
 *
 * @param invoice normalized invoice content rendered by invoice templates
 * @author Artem Demchyshyn
 */
public record InvoiceDocumentSpec(InvoiceData invoice) {

    /**
     * Creates a normalized invoice document spec.
     */
    public InvoiceDocumentSpec {
        invoice = invoice == null ? InvoiceData.builder().build() : invoice;
    }

    /**
     * Wraps existing invoice data in the document-level spec expected by
     * canonical templates.
     *
     * @param invoice invoice data
     * @return document spec
     */
    public static InvoiceDocumentSpec from(InvoiceData invoice) {
        return new InvoiceDocumentSpec(invoice);
    }

    /**
     * Starts a fluent invoice document builder.
     *
     * @return document builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for invoice document specs.
     */
    public static final class Builder {
        private final InvoiceData.Builder invoice = InvoiceData.builder();

        private Builder() {
        }

        public Builder title(String title) {
            invoice.title(title);
            return this;
        }

        public Builder invoiceNumber(String invoiceNumber) {
            invoice.invoiceNumber(invoiceNumber);
            return this;
        }

        public Builder issueDate(String issueDate) {
            invoice.issueDate(issueDate);
            return this;
        }

        public Builder dueDate(String dueDate) {
            invoice.dueDate(dueDate);
            return this;
        }

        public Builder reference(String reference) {
            invoice.reference(reference);
            return this;
        }

        public Builder status(String status) {
            invoice.status(status);
            return this;
        }

        public Builder fromParty(InvoiceParty fromParty) {
            invoice.fromParty(fromParty);
            return this;
        }

        public Builder fromParty(Consumer<InvoiceParty.Builder> spec) {
            invoice.fromParty(spec);
            return this;
        }

        public Builder billToParty(InvoiceParty billToParty) {
            invoice.billToParty(billToParty);
            return this;
        }

        public Builder billToParty(Consumer<InvoiceParty.Builder> spec) {
            invoice.billToParty(spec);
            return this;
        }

        public Builder lineItems(List<InvoiceLineItem> lineItems) {
            invoice.lineItems(lineItems);
            return this;
        }

        public Builder addLineItem(InvoiceLineItem lineItem) {
            invoice.addLineItem(Objects.requireNonNull(lineItem, "lineItem"));
            return this;
        }

        public Builder lineItem(Consumer<InvoiceLineItem.Builder> spec) {
            invoice.lineItem(spec);
            return this;
        }

        public Builder lineItem(String description, String details, String quantity, String unitPrice, String amount) {
            invoice.lineItem(description, details, quantity, unitPrice, amount);
            return this;
        }

        public Builder summaryRows(List<InvoiceSummaryRow> summaryRows) {
            invoice.summaryRows(summaryRows);
            return this;
        }

        public Builder addSummaryRow(InvoiceSummaryRow summaryRow) {
            invoice.addSummaryRow(Objects.requireNonNull(summaryRow, "summaryRow"));
            return this;
        }

        public Builder summaryRow(Consumer<InvoiceSummaryRow.Builder> spec) {
            invoice.summaryRow(spec);
            return this;
        }

        public Builder summaryRow(String label, String value) {
            invoice.summaryRow(label, value);
            return this;
        }

        public Builder totalRow(String label, String value) {
            invoice.totalRow(label, value);
            return this;
        }

        public Builder notes(List<String> notes) {
            invoice.notes(notes);
            return this;
        }

        public Builder note(String note) {
            invoice.note(note);
            return this;
        }

        public Builder paymentTerms(List<String> paymentTerms) {
            invoice.paymentTerms(paymentTerms);
            return this;
        }

        public Builder paymentTerm(String paymentTerm) {
            invoice.paymentTerm(paymentTerm);
            return this;
        }

        public Builder footerNote(String footerNote) {
            invoice.footerNote(footerNote);
            return this;
        }

        public InvoiceDocumentSpec build() {
            return new InvoiceDocumentSpec(invoice.build());
        }
    }
}
