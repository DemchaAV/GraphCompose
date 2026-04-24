package com.demcha.compose.document.templates.data.invoice;

import java.util.Objects;

/**
 * Display-oriented invoice summary row.
 *
 * @param label row label
 * @param value row value
 * @param emphasized whether the row should be visually emphasized
 */
public record InvoiceSummaryRow(
        String label,
        String value,
        boolean emphasized) {

    /**
     * Normalizes null display fields to empty strings.
     */
    public InvoiceSummaryRow {
        label = Objects.requireNonNullElse(label, "");
        value = Objects.requireNonNullElse(value, "");
    }

    /**
     * Starts a fluent invoice summary row builder.
     *
     * @return summary row builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for invoice summary rows.
     */
    public static final class Builder {
        private String label;
        private String value;
        private boolean emphasized;

        private Builder() {
        }

        /**
         * Sets the row label.
         *
         * @param label row label
         * @return this builder
         */
        public Builder label(String label) {
            this.label = label;
            return this;
        }

        /**
         * Sets the row value.
         *
         * @param value row value
         * @return this builder
         */
        public Builder value(String value) {
            this.value = value;
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
         * Builds an immutable summary row.
         *
         * @return invoice summary row
         */
        public InvoiceSummaryRow build() {
            return new InvoiceSummaryRow(label, value, emphasized);
        }
    }
}
