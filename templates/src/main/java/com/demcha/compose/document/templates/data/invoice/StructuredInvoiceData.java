package com.demcha.compose.document.templates.data.invoice;

import java.util.Objects;

/**
 * Structured input for business invoice documents.
 *
 * <p>The display model ({@link InvoiceData}) carries an invoice as
 * pre-formatted strings: one address block per party, line items whose
 * quantity and money are already rendered, and a flat list of summary
 * rows. This model is the structured business invoice: a brand lockup with
 * the sender's own logo, labelled masthead metadata, a contact block with
 * a business registration, priced service lines carrying
 * {@link java.math.BigDecimal} figures and the unit they are counted in, a
 * totals stack with its own total band, bank payment fields, and a footer
 * line. Each block owns its heading and labels, so the wording is content
 * rather than a preset choice.</p>
 *
 * <p>Both models stay: a preset consumes the one whose shape it renders.
 * Every component normalizes {@code null} to its empty form, so a partial
 * document composes without null checks in preset code. The section
 * records construct positionally; this builder is where a document is
 * assembled.</p>
 *
 * @param brand        the sender's brand lockup
 * @param supplier     the sender's address and contact channels
 * @param masthead     the document title and its metadata rows
 * @param billTo       the billed-to block
 * @param shipTo       the shipped-to block, for the designs that print a
 *                     delivery address beside the billing one; empty when
 *                     the two are the same or the design shows only one
 * @param summary      what the invoice covers, and for which period
 * @param serviceLines the line-items table
 * @param totals       the totals stack and its total band
 * @param payment      where to send the money, and by when
 * @param notes        the closing notes and query channels
 * @param currencyCode the ISO currency code the figures are stated in
 */
public record StructuredInvoiceData(
        InvoiceBrand brand,
        InvoiceContactBlock supplier,
        InvoiceMasthead masthead,
        InvoiceRecipient billTo,
        InvoiceRecipient shipTo,
        InvoiceSummaryBlock summary,
        InvoiceServiceLines serviceLines,
        InvoiceTotalsBlock totals,
        InvoicePaymentBlock payment,
        InvoiceNotesBlock notes,
        String currencyCode) {

    /**
     * Normalizes absent components to their empty forms.
     */
    public StructuredInvoiceData {
        brand = brand == null ? new InvoiceBrand(null, null, null, null) : brand;
        supplier = supplier == null
                ? new InvoiceContactBlock(null, null, null, null, null, null, null) : supplier;
        masthead = masthead == null ? new InvoiceMasthead(null, null) : masthead;
        billTo = billTo == null
                ? new InvoiceRecipient(null, null, null, null, null, null) : billTo;
        shipTo = shipTo == null
                ? new InvoiceRecipient(null, null, null, null, null, null) : shipTo;
        summary = summary == null ? new InvoiceSummaryBlock(null, null, null) : summary;
        serviceLines = serviceLines == null
                ? new InvoiceServiceLines(null, null) : serviceLines;
        totals = totals == null ? new InvoiceTotalsBlock(null, null, null) : totals;
        payment = payment == null
                ? new InvoicePaymentBlock(null, null, null, null, null) : payment;
        notes = notes == null ? new InvoiceNotesBlock(null, null, null, null) : notes;
        currencyCode = Objects.requireNonNullElse(currencyCode, "");
    }

    /**
     * Backward-compatible constructor for callers that predate the
     * shipped-to block.
     *
     * @param brand        the sender's brand lockup
     * @param supplier     the sender's address and contact channels
     * @param masthead     the document title and its metadata rows
     * @param billTo       the billed-to block
     * @param summary      what the invoice covers, and for which period
     * @param serviceLines the line-items table
     * @param totals       the totals stack and its total band
     * @param payment      where to send the money, and by when
     * @param notes        the closing notes and query channels
     * @param currencyCode the ISO currency code the figures are stated in
     */
    public StructuredInvoiceData(InvoiceBrand brand, InvoiceContactBlock supplier,
                                 InvoiceMasthead masthead, InvoiceRecipient billTo,
                                 InvoiceSummaryBlock summary,
                                 InvoiceServiceLines serviceLines,
                                 InvoiceTotalsBlock totals, InvoicePaymentBlock payment,
                                 InvoiceNotesBlock notes, String currencyCode) {
        this(brand, supplier, masthead, billTo, null, summary, serviceLines,
                totals, payment, notes, currencyCode);
    }

    /**
     * Starts a fluent structured invoice data builder.
     *
     * @return structured invoice data builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for complete structured invoice content.
     */
    public static final class Builder {
        private InvoiceBrand brand;
        private InvoiceContactBlock supplier;
        private InvoiceMasthead masthead;
        private InvoiceRecipient billTo;
        private InvoiceRecipient shipTo;
        private InvoiceSummaryBlock summary;
        private InvoiceServiceLines serviceLines;
        private InvoiceTotalsBlock totals;
        private InvoicePaymentBlock payment;
        private InvoiceNotesBlock notes;
        private String currencyCode;

        private Builder() {
        }

        /**
         * Sets the sender's brand lockup.
         *
         * @param brand brand lockup
         * @return this builder
         */
        public Builder brand(InvoiceBrand brand) {
            this.brand = brand;
            return this;
        }

        /**
         * Sets the sender's address and contact channels.
         *
         * @param supplier contact block
         * @return this builder
         */
        public Builder supplier(InvoiceContactBlock supplier) {
            this.supplier = supplier;
            return this;
        }

        /**
         * Sets the document title and its metadata rows.
         *
         * @param masthead masthead
         * @return this builder
         */
        public Builder masthead(InvoiceMasthead masthead) {
            this.masthead = masthead;
            return this;
        }

        /**
         * Sets the billed-to block.
         *
         * @param billTo recipient block
         * @return this builder
         */
        public Builder billTo(InvoiceRecipient billTo) {
            this.billTo = billTo;
            return this;
        }

        /**
         * Sets the shipped-to block, for the designs that print a delivery
         * address beside the billing one.
         *
         * @param shipTo recipient block
         * @return this builder
         */
        public Builder shipTo(InvoiceRecipient shipTo) {
            this.shipTo = shipTo;
            return this;
        }

        /**
         * Sets what the invoice covers, and for which period.
         *
         * @param summary summary block
         * @return this builder
         */
        public Builder summary(InvoiceSummaryBlock summary) {
            this.summary = summary;
            return this;
        }

        /**
         * Sets the line-items table.
         *
         * @param serviceLines service lines
         * @return this builder
         */
        public Builder serviceLines(InvoiceServiceLines serviceLines) {
            this.serviceLines = serviceLines;
            return this;
        }

        /**
         * Sets the totals stack and its total band.
         *
         * @param totals totals block
         * @return this builder
         */
        public Builder totals(InvoiceTotalsBlock totals) {
            this.totals = totals;
            return this;
        }

        /**
         * Sets where the money goes, and by when.
         *
         * @param payment payment block
         * @return this builder
         */
        public Builder payment(InvoicePaymentBlock payment) {
            this.payment = payment;
            return this;
        }

        /**
         * Sets the closing notes and query channels.
         *
         * @param notes notes block
         * @return this builder
         */
        public Builder notes(InvoiceNotesBlock notes) {
            this.notes = notes;
            return this;
        }

        /**
         * Sets the ISO currency code the figures are stated in.
         *
         * @param currencyCode currency code
         * @return this builder
         */
        public Builder currencyCode(String currencyCode) {
            this.currencyCode = currencyCode;
            return this;
        }

        /**
         * Builds the normalized structured invoice data.
         *
         * @return structured invoice data
         */
        public StructuredInvoiceData build() {
            return new StructuredInvoiceData(brand, supplier, masthead, billTo, shipTo,
                    summary, serviceLines, totals, payment, notes, currencyCode);
        }
    }
}
