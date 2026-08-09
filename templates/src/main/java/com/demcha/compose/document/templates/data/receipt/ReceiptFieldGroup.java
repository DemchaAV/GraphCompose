package com.demcha.compose.document.templates.data.receipt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A titled block of {@link ReceiptField} rows — {@code Transfer details},
 * {@code Beneficiary details}, {@code Fees and charges}.
 *
 * <p>A receipt is a sequence of these plus one amount; giving the group its
 * own record is what lets a caller add a block the template has never heard
 * of without touching the preset.</p>
 *
 * @param title  group heading
 * @param fields rows inside the group
 */
public record ReceiptFieldGroup(String title, List<ReceiptField> fields) {

    /**
     * Normalizes the title and freezes the row list.
     */
    public ReceiptFieldGroup {
        title = Objects.requireNonNullElse(title, "");
        fields = List.copyOf(Objects.requireNonNullElse(fields, List.of()));
    }

    /**
     * Starts a fluent group builder.
     *
     * @param title group heading
     * @return group builder
     */
    public static Builder builder(String title) {
        return new Builder(title);
    }

    /**
     * Fluent builder for a titled field group.
     */
    public static final class Builder {
        private final String title;
        private final List<ReceiptField> fields = new ArrayList<>();

        private Builder(String title) {
            this.title = title;
        }

        /**
         * Appends a plain row.
         *
         * @param label field caption
         * @param value field value
         * @return this builder
         */
        public Builder field(String label, String value) {
            fields.add(ReceiptField.of(label, value));
            return this;
        }

        /**
         * Appends a row whose value renders in the emphasised weight.
         *
         * @param label field caption
         * @param value field value
         * @return this builder
         */
        public Builder emphasized(String label, String value) {
            fields.add(ReceiptField.emphasized(label, value));
            return this;
        }

        /**
         * Appends a prepared row.
         *
         * @param field field row
         * @return this builder
         */
        public Builder add(ReceiptField field) {
            fields.add(Objects.requireNonNull(field, "field"));
            return this;
        }

        /**
         * Builds the immutable group.
         *
         * @return field group
         */
        public ReceiptFieldGroup build() {
            return new ReceiptFieldGroup(title, fields);
        }
    }
}
