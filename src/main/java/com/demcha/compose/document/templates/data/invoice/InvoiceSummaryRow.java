package com.demcha.compose.document.templates.data.invoice;

import java.util.Objects;

/**
 * Display-oriented invoice summary row.
 */
public record InvoiceSummaryRow(
        String label,
        String value,
        boolean emphasized) {

    public InvoiceSummaryRow {
        label = Objects.requireNonNullElse(label, "");
        value = Objects.requireNonNullElse(value, "");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String label;
        private String value;
        private boolean emphasized;

        private Builder() {
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder emphasized(boolean emphasized) {
            this.emphasized = emphasized;
            return this;
        }

        public InvoiceSummaryRow build() {
            return new InvoiceSummaryRow(label, value, emphasized);
        }
    }
}
