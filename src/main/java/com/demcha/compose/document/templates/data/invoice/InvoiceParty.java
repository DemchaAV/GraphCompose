package com.demcha.compose.document.templates.data.invoice;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

/**
 * Display-oriented invoice party details.
 *
 * @param name party/company name
 * @param addressLines address lines in display order
 * @param email optional email address
 * @param phone optional phone number
 * @param taxId optional tax or company identifier
 */
public record InvoiceParty(
        String name,
        List<String> addressLines,
        String email,
        String phone,
        String taxId) {

    /**
     * Normalizes null party fields and address lists.
     */
    public InvoiceParty {
        name = Objects.requireNonNullElse(name, "");
        addressLines = List.copyOf(Objects.requireNonNullElse(addressLines, List.of()));
        email = Objects.requireNonNullElse(email, "");
        phone = Objects.requireNonNullElse(phone, "");
        taxId = Objects.requireNonNullElse(taxId, "");
    }

    /**
     * Starts a fluent invoice party builder.
     *
     * @return party builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for invoice party contact details.
     */
    public static final class Builder {
        private String name;
        private final List<String> addressLines = new ArrayList<>();
        private String email;
        private String phone;
        private String taxId;

        private Builder() {
        }

        /**
         * Sets the party or company name.
         *
         * @param name party name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Replaces all address lines.
         *
         * @param addressLines address lines in display order
         * @return this builder
         */
        public Builder addressLines(List<String> addressLines) {
            this.addressLines.clear();
            if (addressLines != null) {
                this.addressLines.addAll(addressLines);
            }
            return this;
        }

        /**
         * Replaces all address lines.
         *
         * @param addressLines address lines in display order
         * @return this builder
         */
        public Builder addressLines(String... addressLines) {
            this.addressLines.clear();
            if (addressLines != null) {
                this.addressLines.addAll(List.of(addressLines));
            }
            return this;
        }

        /**
         * Appends one address line.
         *
         * @param addressLine address line
         * @return this builder
         */
        public Builder addAddressLine(String addressLine) {
            this.addressLines.add(addressLine);
            return this;
        }

        /**
         * Sets the party email address.
         *
         * @param email email address
         * @return this builder
         */
        public Builder email(String email) {
            this.email = email;
            return this;
        }

        /**
         * Sets the party phone number.
         *
         * @param phone phone number
         * @return this builder
         */
        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        /**
         * Sets the tax or company identifier.
         *
         * @param taxId tax identifier
         * @return this builder
         */
        public Builder taxId(String taxId) {
            this.taxId = taxId;
            return this;
        }

        /**
         * Builds an immutable party payload.
         *
         * @return invoice party
         */
        public InvoiceParty build() {
            return new InvoiceParty(name, addressLines, email, phone, taxId);
        }
    }
}
