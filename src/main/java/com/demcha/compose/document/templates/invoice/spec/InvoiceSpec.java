package com.demcha.compose.document.templates.invoice.spec;

import java.util.List;
import java.util.Objects;

/**
 * User-facing data record for an invoice in Templates v2.
 *
 * <p>Simplified compared to the legacy {@code InvoiceData} —
 * captures the essentials (header info, parties, line items, summary
 * rows, footer notes) without the cinematic-presentation flags. Each
 * field is immutable and validates at construction.</p>
 *
 * @param invoiceNumber stable invoice identifier (required)
 * @param issueDate     human-readable issue date (required)
 * @param dueDate       human-readable due date (may be empty)
 * @param fromParty     billing party (required)
 * @param billToParty   recipient party (required)
 * @param lineItems     ordered line items (must not be empty)
 * @param summaryRows   subtotal / tax / total rows in source order
 * @param notes         optional footer notes
 */
public record InvoiceSpec(
        String invoiceNumber,
        String issueDate,
        String dueDate,
        Party fromParty,
        Party billToParty,
        List<LineItem> lineItems,
        List<SummaryRow> summaryRows,
        List<String> notes) {

    /**
     * Compact constructor that defensively copies lists and
     * normalises null strings.
     *
     * @throws NullPointerException     if a required field is null
     * @throws IllegalArgumentException if invoiceNumber, issueDate, or
     *                                  lineItems are blank/empty
     */
    public InvoiceSpec {
        Objects.requireNonNull(invoiceNumber, "invoiceNumber");
        Objects.requireNonNull(issueDate, "issueDate");
        Objects.requireNonNull(fromParty, "fromParty");
        Objects.requireNonNull(billToParty, "billToParty");
        Objects.requireNonNull(lineItems, "lineItems");
        if (invoiceNumber.isBlank()) {
            throw new IllegalArgumentException("invoiceNumber must not be blank");
        }
        if (issueDate.isBlank()) {
            throw new IllegalArgumentException("issueDate must not be blank");
        }
        if (lineItems.isEmpty()) {
            throw new IllegalArgumentException("lineItems must not be empty");
        }
        dueDate = dueDate == null ? "" : dueDate;
        lineItems = List.copyOf(lineItems);
        summaryRows = summaryRows == null ? List.of() : List.copyOf(summaryRows);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    /**
     * One billing party (sender or recipient).
     *
     * @param name         legal / display name
     * @param addressLines address lines in source order
     * @param email        contact email
     * @param phone        contact phone
     * @param taxId        tax identifier (may be empty)
     */
    public record Party(String name, List<String> addressLines,
                        String email, String phone, String taxId) {

        /**
         * Compact constructor that normalises null strings and
         * defensively copies the address list.
         *
         * @throws NullPointerException     if {@code name} is null
         * @throws IllegalArgumentException if {@code name} is blank
         */
        public Party {
            Objects.requireNonNull(name, "name");
            if (name.isBlank()) {
                throw new IllegalArgumentException("name must not be blank");
            }
            addressLines = addressLines == null ? List.of() : List.copyOf(addressLines);
            email = email == null ? "" : email;
            phone = phone == null ? "" : phone;
            taxId = taxId == null ? "" : taxId;
        }
    }

    /**
     * One line item in the invoice table.
     *
     * @param description short description column
     * @param details     extended description column (may be empty)
     * @param quantity    numeric quantity column
     * @param unitPrice   per-unit price column
     * @param amount      total amount column
     */
    public record LineItem(String description, String details,
                           String quantity, String unitPrice, String amount) {

        /**
         * Compact constructor that normalises null strings.
         */
        public LineItem {
            description = description == null ? "" : description;
            details = details == null ? "" : details;
            quantity = quantity == null ? "" : quantity;
            unitPrice = unitPrice == null ? "" : unitPrice;
            amount = amount == null ? "" : amount;
        }
    }

    /**
     * One summary row (subtotal, tax, total, etc.) appearing beneath
     * the line item table.
     *
     * @param label  row label
     * @param value  row value
     * @param isTotal whether this row is the headline total (rendered
     *                emphasised by the preset)
     */
    public record SummaryRow(String label, String value, boolean isTotal) {

        /**
         * Compact constructor that normalises null strings.
         */
        public SummaryRow {
            label = label == null ? "" : label;
            value = value == null ? "" : value;
        }

        /**
         * Convenience factory for a non-total row.
         *
         * @param label row label
         * @param value row value
         * @return summary row with isTotal = false
         */
        public static SummaryRow of(String label, String value) {
            return new SummaryRow(label, value, false);
        }

        /**
         * Convenience factory for the headline total row.
         *
         * @param label row label
         * @param value row value
         * @return summary row with isTotal = true
         */
        public static SummaryRow total(String label, String value) {
            return new SummaryRow(label, value, true);
        }
    }

    /**
     * Returns a fluent builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Mutable builder for {@link InvoiceSpec}.
     */
    public static final class Builder {
        private String invoiceNumber;
        private String issueDate;
        private String dueDate = "";
        private Party fromParty;
        private Party billToParty;
        private final java.util.List<LineItem> lineItems = new java.util.ArrayList<>();
        private final java.util.List<SummaryRow> summaryRows = new java.util.ArrayList<>();
        private final java.util.List<String> notes = new java.util.ArrayList<>();

        private Builder() {
        }

        /** @param value invoice number @return this builder */
        public Builder invoiceNumber(String value) { this.invoiceNumber = value; return this; }
        /** @param value issue date @return this builder */
        public Builder issueDate(String value) { this.issueDate = value; return this; }
        /** @param value due date @return this builder */
        public Builder dueDate(String value) { this.dueDate = value == null ? "" : value; return this; }
        /** @param value sender party @return this builder */
        public Builder fromParty(Party value) { this.fromParty = value; return this; }
        /** @param value recipient party @return this builder */
        public Builder billToParty(Party value) { this.billToParty = value; return this; }
        /** @param item line item @return this builder */
        public Builder lineItem(LineItem item) { this.lineItems.add(item); return this; }
        /** @param row summary row @return this builder */
        public Builder summaryRow(SummaryRow row) { this.summaryRows.add(row); return this; }
        /** @param note footer note @return this builder */
        public Builder note(String note) { if (note != null) this.notes.add(note); return this; }

        /**
         * Builds an immutable {@link InvoiceSpec}.
         *
         * @return new invoice spec
         */
        public InvoiceSpec build() {
            return new InvoiceSpec(invoiceNumber, issueDate, dueDate,
                    fromParty, billToParty, lineItems, summaryRows, notes);
        }
    }
}
