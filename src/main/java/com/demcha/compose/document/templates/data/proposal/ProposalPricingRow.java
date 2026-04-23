package com.demcha.compose.document.templates.data.proposal;

import java.util.Objects;

/**
 * Display-oriented proposal pricing row.
 */
public record ProposalPricingRow(
        String label,
        String description,
        String amount,
        boolean emphasized) {

    public ProposalPricingRow {
        label = Objects.requireNonNullElse(label, "");
        description = Objects.requireNonNullElse(description, "");
        amount = Objects.requireNonNullElse(amount, "");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String label;
        private String description;
        private String amount;
        private boolean emphasized;

        private Builder() {
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder amount(String amount) {
            this.amount = amount;
            return this;
        }

        public Builder emphasized(boolean emphasized) {
            this.emphasized = emphasized;
            return this;
        }

        public ProposalPricingRow build() {
            return new ProposalPricingRow(label, description, amount, emphasized);
        }
    }
}
