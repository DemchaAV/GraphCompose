package com.demcha.compose.document.templates.data.invoice;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

/**
 * Display-oriented invoice party details.
 */
public record InvoiceParty(
        String name,
        List<String> addressLines,
        String email,
        String phone,
        String taxId) {

    public InvoiceParty {
        name = Objects.requireNonNullElse(name, "");
        addressLines = List.copyOf(Objects.requireNonNullElse(addressLines, List.of()));
        email = Objects.requireNonNullElse(email, "");
        phone = Objects.requireNonNullElse(phone, "");
        taxId = Objects.requireNonNullElse(taxId, "");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private final List<String> addressLines = new ArrayList<>();
        private String email;
        private String phone;
        private String taxId;

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

        public Builder taxId(String taxId) {
            this.taxId = taxId;
            return this;
        }

        public InvoiceParty build() {
            return new InvoiceParty(name, addressLines, email, phone, taxId);
        }
    }
}
