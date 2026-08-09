package com.demcha.compose.document.templates.data.receipt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One side of a transfer — who sent the money, or who received it.
 *
 * <p>The account rows are {@link ReceiptField}s rather than named accessors
 * because what identifies an account is regional: a UK payment shows a sort
 * code and an account number, a SEPA payment an IBAN and a BIC, a domestic US
 * one a routing number. A named field per scheme would leave every template
 * rendering blanks for the schemes it was not written for.</p>
 *
 * @param name         account holder or beneficiary name
 * @param addressLines optional postal address, one entry per line
 * @param fields       account identification rows (sort code, IBAN, …)
 */
public record ReceiptParty(String name, List<String> addressLines, List<ReceiptField> fields) {

    /**
     * Normalizes the name and freezes both lists.
     */
    public ReceiptParty {
        name = Objects.requireNonNullElse(name, "");
        addressLines = List.copyOf(Objects.requireNonNullElse(addressLines, List.of()));
        fields = List.copyOf(Objects.requireNonNullElse(fields, List.of()));
    }

    /**
     * Starts a fluent party builder.
     *
     * @return party builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for one side of a transfer.
     */
    public static final class Builder {
        private String name;
        private final List<String> addressLines = new ArrayList<>();
        private final List<ReceiptField> fields = new ArrayList<>();

        private Builder() {
        }

        /**
         * Sets the account holder or beneficiary name.
         *
         * @param name display name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Appends address lines.
         *
         * @param lines address lines, one per rendered line
         * @return this builder
         */
        public Builder addressLines(String... lines) {
            if (lines != null) {
                for (String line : lines) {
                    if (line != null && !line.isBlank()) {
                        addressLines.add(line);
                    }
                }
            }
            return this;
        }

        /**
         * Appends an account identification row.
         *
         * @param label field caption (for example {@code Sort code})
         * @param value field value
         * @return this builder
         */
        public Builder field(String label, String value) {
            fields.add(ReceiptField.of(label, value));
            return this;
        }

        /**
         * Builds the immutable party.
         *
         * @return receipt party
         */
        public ReceiptParty build() {
            return new ReceiptParty(name, addressLines, fields);
        }
    }
}
