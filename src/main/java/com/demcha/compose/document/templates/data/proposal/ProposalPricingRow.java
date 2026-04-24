package com.demcha.compose.document.templates.data.proposal;

import java.util.Objects;

/**
 * Display-oriented proposal pricing row.
 *
 * @param label row label
 * @param description row description
 * @param amount display amount
 * @param emphasized whether the row should be visually emphasized
 */
public record ProposalPricingRow(
        String label,
        String description,
        String amount,
        boolean emphasized) {

    /**
     * Normalizes null display fields to empty strings.
     */
    public ProposalPricingRow {
        label = Objects.requireNonNullElse(label, "");
        description = Objects.requireNonNullElse(description, "");
        amount = Objects.requireNonNullElse(amount, "");
    }

    /**
     * Starts a fluent proposal pricing row builder.
     *
     * @return pricing row builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for proposal pricing rows.
     */
    public static final class Builder {
        private String label;
        private String description;
        private String amount;
        private boolean emphasized;

        private Builder() {
        }

        /**
         * Sets the pricing row label.
         *
         * @param label row label
         * @return this builder
         */
        public Builder label(String label) {
            this.label = label;
            return this;
        }

        /**
         * Sets the pricing row description.
         *
         * @param description row description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
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
         * Marks the row as visually emphasized.
         *
         * @param emphasized whether the row is emphasized
         * @return this builder
         */
        public Builder emphasized(boolean emphasized) {
            this.emphasized = emphasized;
            return this;
        }

        /**
         * Builds immutable pricing row data.
         *
         * @return proposal pricing row
         */
        public ProposalPricingRow build() {
            return new ProposalPricingRow(label, description, amount, emphasized);
        }
    }
}
