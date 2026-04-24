package com.demcha.compose.document.templates.data.proposal;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

/**
 * Display-oriented proposal party details.
 *
 * @param name party/company name
 * @param addressLines address lines in display order
 * @param email optional email address
 * @param phone optional phone number
 * @param website optional website URL
 */
public record ProposalParty(
        String name,
        List<String> addressLines,
        String email,
        String phone,
        String website) {

    /**
     * Normalizes null party fields and address lists.
     */
    public ProposalParty {
        name = Objects.requireNonNullElse(name, "");
        addressLines = List.copyOf(Objects.requireNonNullElse(addressLines, List.of()));
        email = Objects.requireNonNullElse(email, "");
        phone = Objects.requireNonNullElse(phone, "");
        website = Objects.requireNonNullElse(website, "");
    }

    /**
     * Starts a fluent proposal party builder.
     *
     * @return party builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for proposal party details.
     */
    public static final class Builder {
        private String name;
        private final List<String> addressLines = new ArrayList<>();
        private String email;
        private String phone;
        private String website;

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
         * Sets the party website URL.
         *
         * @param website website URL
         * @return this builder
         */
        public Builder website(String website) {
            this.website = website;
            return this;
        }

        /**
         * Builds immutable proposal party data.
         *
         * @return proposal party
         */
        public ProposalParty build() {
            return new ProposalParty(name, addressLines, email, phone, website);
        }
    }
}
