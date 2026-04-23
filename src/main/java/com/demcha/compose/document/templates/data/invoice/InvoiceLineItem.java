package com.demcha.compose.document.templates.data.invoice;

import java.util.Objects;

/**
 * Display-oriented invoice line item.
 */
public record InvoiceLineItem(
        String description,
        String details,
        String quantity,
        String unitPrice,
        String amount) {

    public InvoiceLineItem {
        description = Objects.requireNonNullElse(description, "");
        details = Objects.requireNonNullElse(details, "");
        quantity = Objects.requireNonNullElse(quantity, "");
        unitPrice = Objects.requireNonNullElse(unitPrice, "");
        amount = Objects.requireNonNullElse(amount, "");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String description;
        private String details;
        private String quantity;
        private String unitPrice;
        private String amount;

        private Builder() {
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder details(String details) {
            this.details = details;
            return this;
        }

        public Builder quantity(String quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder unitPrice(String unitPrice) {
            this.unitPrice = unitPrice;
            return this;
        }

        public Builder amount(String amount) {
            this.amount = amount;
            return this;
        }

        public InvoiceLineItem build() {
            return new InvoiceLineItem(description, details, quantity, unitPrice, amount);
        }
    }
}
