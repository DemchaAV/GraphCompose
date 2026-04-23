package com.demcha.compose.document.templates.data.invoice;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.function.Consumer;

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

    public static Builder builder() {
        return new Builder();
    }

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

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder invoiceNumber(String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
            return this;
        }

        public Builder issueDate(String issueDate) {
            this.issueDate = issueDate;
            return this;
        }

        public Builder dueDate(String dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder fromParty(InvoiceParty fromParty) {
            this.fromParty = fromParty;
            return this;
        }

        public Builder fromParty(Consumer<InvoiceParty.Builder> spec) {
            InvoiceParty.Builder builder = InvoiceParty.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return fromParty(builder.build());
        }

        public Builder billToParty(InvoiceParty billToParty) {
            this.billToParty = billToParty;
            return this;
        }

        public Builder billToParty(Consumer<InvoiceParty.Builder> spec) {
            InvoiceParty.Builder builder = InvoiceParty.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return billToParty(builder.build());
        }

        public Builder lineItems(List<InvoiceLineItem> lineItems) {
            this.lineItems.clear();
            if (lineItems != null) {
                this.lineItems.addAll(lineItems);
            }
            return this;
        }

        public Builder addLineItem(InvoiceLineItem lineItem) {
            this.lineItems.add(lineItem);
            return this;
        }

        public Builder lineItem(Consumer<InvoiceLineItem.Builder> spec) {
            InvoiceLineItem.Builder builder = InvoiceLineItem.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return addLineItem(builder.build());
        }

        public Builder lineItem(String description, String details, String quantity, String unitPrice, String amount) {
            return addLineItem(new InvoiceLineItem(description, details, quantity, unitPrice, amount));
        }

        public Builder summaryRows(List<InvoiceSummaryRow> summaryRows) {
            this.summaryRows.clear();
            if (summaryRows != null) {
                this.summaryRows.addAll(summaryRows);
            }
            return this;
        }

        public Builder addSummaryRow(InvoiceSummaryRow summaryRow) {
            this.summaryRows.add(summaryRow);
            return this;
        }

        public Builder summaryRow(Consumer<InvoiceSummaryRow.Builder> spec) {
            InvoiceSummaryRow.Builder builder = InvoiceSummaryRow.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return addSummaryRow(builder.build());
        }

        public Builder summaryRow(String label, String value) {
            return addSummaryRow(new InvoiceSummaryRow(label, value, false));
        }

        public Builder totalRow(String label, String value) {
            return addSummaryRow(new InvoiceSummaryRow(label, value, true));
        }

        public Builder notes(List<String> notes) {
            this.notes.clear();
            if (notes != null) {
                this.notes.addAll(notes);
            }
            return this;
        }

        public Builder note(String note) {
            this.notes.add(note);
            return this;
        }

        public Builder paymentTerms(List<String> paymentTerms) {
            this.paymentTerms.clear();
            if (paymentTerms != null) {
                this.paymentTerms.addAll(paymentTerms);
            }
            return this;
        }

        public Builder paymentTerm(String paymentTerm) {
            this.paymentTerms.add(paymentTerm);
            return this;
        }

        public Builder footerNote(String footerNote) {
            this.footerNote = footerNote;
            return this;
        }

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
