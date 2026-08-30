package com.demcha.compose.document.templates.data.invoice;

import java.util.Objects;

/**
 * Display-oriented input for structured invoice documents.
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
 * @param summary      what the invoice covers, and for which period
 * @param serviceLines the line-items table
 * @param totals       the totals stack and its total band
 * @param payment      where to send the money, and by when
 * @param notes        the closing notes and query channels
 * @param footer       the page-foot line
 * @param currencyCode the ISO currency code the figures are stated in
 */
public record StructuredInvoiceData(
        InvoiceBrand brand,
        InvoiceContactBlock supplier,
        InvoiceMasthead masthead,
        InvoiceRecipient billTo,
        InvoiceSummaryBlock summary,
        InvoiceServiceLines serviceLines,
        InvoiceTotalsBlock totals,
        InvoicePaymentBlock payment,
        InvoiceNotesBlock notes,
        InvoiceFooterLine footer,
        String currencyCode) {

    /**
     * Normalizes absent components to their empty forms.
     */
    public StructuredInvoiceData {
        brand = brand == null ? new InvoiceBrand(null, null, null, null) : brand;
        supplier = supplier == null
                ? new InvoiceContactBlock(null, null, null, null, null, null, null) : supplier;
        masthead = masthead == null ? new InvoiceMasthead(null, null) : masthead;
        billTo = billTo == null ? new InvoiceRecipient(null, null, null, null, null) : billTo;
        summary = summary == null ? new InvoiceSummaryBlock(null, null, null) : summary;
        serviceLines = serviceLines == null
                ? new InvoiceServiceLines(null, null) : serviceLines;
        totals = totals == null ? new InvoiceTotalsBlock(null, null, null) : totals;
        payment = payment == null
                ? new InvoicePaymentBlock(null, null, null, null) : payment;
        notes = notes == null ? new InvoiceNotesBlock(null, null, null, null) : notes;
        footer = footer == null ? new InvoiceFooterLine(null, null) : footer;
        currencyCode = Objects.requireNonNullElse(currencyCode, "");
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
        private InvoiceSummaryBlock summary;
        private InvoiceServiceLines serviceLines;
        private InvoiceTotalsBlock totals;
        private InvoicePaymentBlock payment;
        private InvoiceNotesBlock notes;
        private InvoiceFooterLine footer;
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
         * Sets the page-foot line.
         *
         * @param footer footer line
         * @return this builder
         */
        public Builder footer(InvoiceFooterLine footer) {
            this.footer = footer;
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
            return new StructuredInvoiceData(brand, supplier, masthead, billTo, summary,
                    serviceLines, totals, payment, notes, footer, currencyCode);
        }
    }
}
