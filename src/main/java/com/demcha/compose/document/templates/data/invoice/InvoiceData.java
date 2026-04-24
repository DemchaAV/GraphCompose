package com.demcha.compose.document.templates.data.invoice;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Display-oriented invoice document input.
 *
 * @param title display title
 * @param invoiceNumber invoice number shown in the header
 * @param issueDate invoice issue date
 * @param dueDate payment due date
 * @param reference optional customer/project reference
 * @param status optional invoice status label
 * @param fromParty sender/supplier party
 * @param billToParty recipient/customer party
 * @param lineItems invoice line items
 * @param summaryRows subtotal/tax/total rows
 * @param notes invoice notes
 * @param paymentTerms payment term rows
 * @param footerNote footer note rendered at the bottom
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

    /**
     * Normalizes optional invoice fields and freezes collection inputs.
     */
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

    /**
     * Starts a fluent invoice data builder.
     *
     * @return invoice data builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for complete invoice content.
     */
    public static final class Builder {
        private String title;
        private String invoiceNumber;
        private String issueDate;
        private String dueDate;
        private String reference;
        private String status;
        private InvoiceParty fromParty;
        private InvoiceParty billToParty;
        private final List<InvoiceLineItem> lineItems = new ArrayList<>();
        private final List<InvoiceSummaryRow> summaryRows = new ArrayList<>();
        private final List<String> notes = new ArrayList<>();
        private final List<String> paymentTerms = new ArrayList<>();
        private String footerNote;

        private Builder() {
        }

        /**
         * Sets the invoice title.
         *
         * @param title display title
         * @return this builder
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the invoice number.
         *
         * @param invoiceNumber invoice number
         * @return this builder
         */
        public Builder invoiceNumber(String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
            return this;
        }

        /**
         * Sets the issue date.
         *
         * @param issueDate issue date text
         * @return this builder
         */
        public Builder issueDate(String issueDate) {
            this.issueDate = issueDate;
            return this;
        }

        /**
         * Sets the due date.
         *
         * @param dueDate due date text
         * @return this builder
         */
        public Builder dueDate(String dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        /**
         * Sets an optional reference.
         *
         * @param reference reference text
         * @return this builder
         */
        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        /**
         * Sets an optional status label.
         *
         * @param status status label
         * @return this builder
         */
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the sender party.
         *
         * @param fromParty sender party
         * @return this builder
         */
        public Builder fromParty(InvoiceParty fromParty) {
            this.fromParty = fromParty;
            return this;
        }

        /**
         * Builds and sets the sender party.
         *
         * @param spec party builder callback
         * @return this builder
         */
        public Builder fromParty(Consumer<InvoiceParty.Builder> spec) {
            InvoiceParty.Builder builder = InvoiceParty.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return fromParty(builder.build());
        }

        /**
         * Sets the recipient party.
         *
         * @param billToParty recipient party
         * @return this builder
         */
        public Builder billToParty(InvoiceParty billToParty) {
            this.billToParty = billToParty;
            return this;
        }

        /**
         * Builds and sets the recipient party.
         *
         * @param spec party builder callback
         * @return this builder
         */
        public Builder billToParty(Consumer<InvoiceParty.Builder> spec) {
            InvoiceParty.Builder builder = InvoiceParty.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return billToParty(builder.build());
        }

        /**
         * Replaces all line items.
         *
         * @param lineItems invoice line items
         * @return this builder
         */
        public Builder lineItems(List<InvoiceLineItem> lineItems) {
            this.lineItems.clear();
            if (lineItems != null) {
                this.lineItems.addAll(lineItems);
            }
            return this;
        }

        /**
         * Appends a line item.
         *
         * @param lineItem invoice line item
         * @return this builder
         */
        public Builder addLineItem(InvoiceLineItem lineItem) {
            this.lineItems.add(lineItem);
            return this;
        }

        /**
         * Builds and appends a line item.
         *
         * @param spec line item builder callback
         * @return this builder
         */
        public Builder lineItem(Consumer<InvoiceLineItem.Builder> spec) {
            InvoiceLineItem.Builder builder = InvoiceLineItem.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return addLineItem(builder.build());
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
            return addLineItem(new InvoiceLineItem(description, details, quantity, unitPrice, amount));
        }

        /**
         * Replaces all summary rows.
         *
         * @param summaryRows summary rows
         * @return this builder
         */
        public Builder summaryRows(List<InvoiceSummaryRow> summaryRows) {
            this.summaryRows.clear();
            if (summaryRows != null) {
                this.summaryRows.addAll(summaryRows);
            }
            return this;
        }

        /**
         * Appends a summary row.
         *
         * @param summaryRow summary row
         * @return this builder
         */
        public Builder addSummaryRow(InvoiceSummaryRow summaryRow) {
            this.summaryRows.add(summaryRow);
            return this;
        }

        /**
         * Builds and appends a summary row.
         *
         * @param spec summary row builder callback
         * @return this builder
         */
        public Builder summaryRow(Consumer<InvoiceSummaryRow.Builder> spec) {
            InvoiceSummaryRow.Builder builder = InvoiceSummaryRow.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return addSummaryRow(builder.build());
        }

        /**
         * Appends a normal summary row.
         *
         * @param label row label
         * @param value row value
         * @return this builder
         */
        public Builder summaryRow(String label, String value) {
            return addSummaryRow(new InvoiceSummaryRow(label, value, false));
        }

        /**
         * Appends an emphasized total row.
         *
         * @param label total label
         * @param value total value
         * @return this builder
         */
        public Builder totalRow(String label, String value) {
            return addSummaryRow(new InvoiceSummaryRow(label, value, true));
        }

        /**
         * Replaces all note rows.
         *
         * @param notes note rows
         * @return this builder
         */
        public Builder notes(List<String> notes) {
            this.notes.clear();
            if (notes != null) {
                this.notes.addAll(notes);
            }
            return this;
        }

        /**
         * Appends a note row.
         *
         * @param note note text
         * @return this builder
         */
        public Builder note(String note) {
            this.notes.add(note);
            return this;
        }

        /**
         * Replaces all payment term rows.
         *
         * @param paymentTerms payment term rows
         * @return this builder
         */
        public Builder paymentTerms(List<String> paymentTerms) {
            this.paymentTerms.clear();
            if (paymentTerms != null) {
                this.paymentTerms.addAll(paymentTerms);
            }
            return this;
        }

        /**
         * Appends a payment term row.
         *
         * @param paymentTerm payment term text
         * @return this builder
         */
        public Builder paymentTerm(String paymentTerm) {
            this.paymentTerms.add(paymentTerm);
            return this;
        }

        /**
         * Sets the footer note.
         *
         * @param footerNote footer note text
         * @return this builder
         */
        public Builder footerNote(String footerNote) {
            this.footerNote = footerNote;
            return this;
        }

        /**
         * Builds immutable invoice data.
         *
         * @return invoice data
         */
        public InvoiceData build() {
            return new InvoiceData(
                    title,
                    invoiceNumber,
                    issueDate,
                    dueDate,
                    reference,
                    status,
                    fromParty,
                    billToParty,
                    lineItems,
                    summaryRows,
                    notes,
                    paymentTerms,
                    footerNote);
        }
    }
}
