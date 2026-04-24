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

        /**
         * Sets the invoice title.
         *
         * @param title display title
         * @return this builder
         */
        public Builder title(String title) {
            invoice.title(title);
            return this;
        }

        /**
         * Sets the invoice number.
         *
         * @param invoiceNumber invoice number
         * @return this builder
         */
        public Builder invoiceNumber(String invoiceNumber) {
            invoice.invoiceNumber(invoiceNumber);
            return this;
        }

        /**
         * Sets the issue date.
         *
         * @param issueDate issue date text
         * @return this builder
         */
        public Builder issueDate(String issueDate) {
            invoice.issueDate(issueDate);
            return this;
        }

        /**
         * Sets the due date.
         *
         * @param dueDate due date text
         * @return this builder
         */
        public Builder dueDate(String dueDate) {
            invoice.dueDate(dueDate);
            return this;
        }

        /**
         * Sets the customer or project reference.
         *
         * @param reference reference text
         * @return this builder
         */
        public Builder reference(String reference) {
            invoice.reference(reference);
            return this;
        }

        /**
         * Sets the invoice status label.
         *
         * @param status status label
         * @return this builder
         */
        public Builder status(String status) {
            invoice.status(status);
            return this;
        }

        /**
         * Sets the sender party.
         *
         * @param fromParty sender party
         * @return this builder
         */
        public Builder fromParty(InvoiceParty fromParty) {
            invoice.fromParty(fromParty);
            return this;
        }

        /**
         * Builds and sets the sender party.
         *
         * @param spec party builder callback
         * @return this builder
         */
        public Builder fromParty(Consumer<InvoiceParty.Builder> spec) {
            invoice.fromParty(spec);
            return this;
        }

        /**
         * Sets the recipient party.
         *
         * @param billToParty recipient party
         * @return this builder
         */
        public Builder billToParty(InvoiceParty billToParty) {
            invoice.billToParty(billToParty);
            return this;
        }

        /**
         * Builds and sets the recipient party.
         *
         * @param spec party builder callback
         * @return this builder
         */
        public Builder billToParty(Consumer<InvoiceParty.Builder> spec) {
            invoice.billToParty(spec);
            return this;
        }

        /**
         * Replaces all line items.
         *
         * @param lineItems invoice line items
         * @return this builder
         */
        public Builder lineItems(List<InvoiceLineItem> lineItems) {
            invoice.lineItems(lineItems);
            return this;
        }

        /**
         * Appends a line item.
         *
         * @param lineItem invoice line item
         * @return this builder
         */
        public Builder addLineItem(InvoiceLineItem lineItem) {
            invoice.addLineItem(Objects.requireNonNull(lineItem, "lineItem"));
            return this;
        }

        /**
         * Builds and appends a line item.
         *
         * @param spec line item builder callback
         * @return this builder
         */
        public Builder lineItem(Consumer<InvoiceLineItem.Builder> spec) {
            invoice.lineItem(spec);
            return this;
        }

        /**
         * Appends a line item from display values.
         *
         * @param description item description
         * @param details optional details
         * @param quantity quantity text
         * @param unitPrice unit price text
         * @param amount amount text
         * @return this builder
         */
        public Builder lineItem(String description, String details, String quantity, String unitPrice, String amount) {
            invoice.lineItem(description, details, quantity, unitPrice, amount);
            return this;
        }

        /**
         * Replaces all summary rows.
         *
         * @param summaryRows summary rows
         * @return this builder
         */
        public Builder summaryRows(List<InvoiceSummaryRow> summaryRows) {
            invoice.summaryRows(summaryRows);
            return this;
        }

        /**
         * Appends a summary row.
         *
         * @param summaryRow summary row
         * @return this builder
         */
        public Builder addSummaryRow(InvoiceSummaryRow summaryRow) {
            invoice.addSummaryRow(Objects.requireNonNull(summaryRow, "summaryRow"));
            return this;
        }

        /**
         * Builds and appends a summary row.
         *
         * @param spec summary row builder callback
         * @return this builder
         */
        public Builder summaryRow(Consumer<InvoiceSummaryRow.Builder> spec) {
            invoice.summaryRow(spec);
            return this;
        }

        /**
         * Appends a normal summary row.
         *
         * @param label row label
         * @param value row value
         * @return this builder
         */
        public Builder summaryRow(String label, String value) {
            invoice.summaryRow(label, value);
            return this;
        }

        /**
         * Appends an emphasized total row.
         *
         * @param label total label
         * @param value total value
         * @return this builder
         */
        public Builder totalRow(String label, String value) {
            invoice.totalRow(label, value);
            return this;
        }

        /**
         * Replaces all notes.
         *
         * @param notes note rows
         * @return this builder
         */
        public Builder notes(List<String> notes) {
            invoice.notes(notes);
            return this;
        }

        /**
         * Appends a note.
         *
         * @param note note text
         * @return this builder
         */
        public Builder note(String note) {
            invoice.note(note);
            return this;
        }

        /**
         * Replaces all payment terms.
         *
         * @param paymentTerms payment term rows
         * @return this builder
         */
        public Builder paymentTerms(List<String> paymentTerms) {
            invoice.paymentTerms(paymentTerms);
            return this;
        }

        /**
         * Appends a payment term.
         *
         * @param paymentTerm payment term text
         * @return this builder
         */
        public Builder paymentTerm(String paymentTerm) {
            invoice.paymentTerm(paymentTerm);
            return this;
        }

        /**
         * Sets the footer note.
         *
         * @param footerNote footer note text
         * @return this builder
         */
        public Builder footerNote(String footerNote) {
            invoice.footerNote(footerNote);
            return this;
        }

        /**
         * Builds the invoice document spec.
         *
         * @return invoice document spec
         */
        public InvoiceDocumentSpec build() {
            return new InvoiceDocumentSpec(invoice.build());
        }
    }
}
