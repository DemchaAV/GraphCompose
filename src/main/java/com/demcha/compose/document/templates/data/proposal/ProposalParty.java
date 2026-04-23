package com.demcha.compose.document.templates.data.proposal;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

/**
 * Display-oriented proposal party details.
 */
public record ProposalParty(
        String name,
        List<String> addressLines,
        String email,
        String phone,
        String website) {

    public ProposalParty {
        name = Objects.requireNonNullElse(name, "");
        addressLines = List.copyOf(Objects.requireNonNullElse(addressLines, List.of()));
        email = Objects.requireNonNullElse(email, "");
        phone = Objects.requireNonNullElse(phone, "");
        website = Objects.requireNonNullElse(website, "");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private final List<String> addressLines = new ArrayList<>();
        private String email;
        private String phone;
        private String website;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder addressLines(List<String> addressLines) {
            this.addressLines.clear();
            if (addressLines != null) {
                this.addressLines.addAll(addressLines);
            }
            return this;
        }

        public Builder addressLines(String... addressLines) {
            this.addressLines.clear();
            if (addressLines != null) {
                this.addressLines.addAll(List.of(addressLines));
            }
            return this;
        }

        public Builder addAddressLine(String addressLine) {
            this.addressLines.add(addressLine);
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder website(String website) {
            this.website = website;
            return this;
        }

        public ProposalParty build() {
            return new ProposalParty(name, addressLines, email, phone, website);
        }
    }
}
