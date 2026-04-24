package com.demcha.compose.document.templates.data.invoice;

import java.util.Objects;

/**
 * Display-oriented invoice line item.
 *
 * @param description item description
 * @param details optional supporting details
 * @param quantity display quantity
 * @param unitPrice display unit price
 * @param amount display amount
 */
public record InvoiceLineItem(
        String description,
        String details,
        String quantity,
        String unitPrice,
        String amount) {

    /**
     * Normalizes null display fields to empty strings.
     */
    public InvoiceLineItem {
        description = Objects.requireNonNullElse(description, "");
        details = Objects.requireNonNullElse(details, "");
        quantity = Objects.requireNonNullElse(quantity, "");
        unitPrice = Objects.requireNonNullElse(unitPrice, "");
        amount = Objects.requireNonNullElse(amount, "");
    }

    /**
     * Starts a fluent invoice line item builder.
     *
     * @return line item builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for invoice line items.
     */
    public static final class Builder {
        private String description;
        private String details;
        private String quantity;
        private String unitPrice;
        private String amount;

        private Builder() {
        }

        /**
         * Sets the line item description.
         *
         * @param description item description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets optional supporting details.
         *
         * @param details detail text
         * @return this builder
         */
        public Builder details(String details) {
            this.details = details;
            return this;
        }

        /**
         * Sets the display quantity.
         *
         * @param quantity quantity text
         * @return this builder
         */
        public Builder quantity(String quantity) {
            this.quantity = quantity;
            return this;
        }

        /**
         * Sets the display unit price.
         *
         * @param unitPrice unit price text
         * @return this builder
         */
        public Builder unitPrice(String unitPrice) {
            this.unitPrice = unitPrice;
            return this;
        }

        /**
         * Sets the display amount.
         *
         * @param amount amount text
         * @return this builder
         */
        public Builder amount(String amount) {
            this.amount = amount;
            return this;
        }

        /**
         * Builds an immutable line item.
         *
         * @return invoice line item
         */
        public InvoiceLineItem build() {
            return new InvoiceLineItem(description, details, quantity, unitPrice, amount);
        }
    }
}
