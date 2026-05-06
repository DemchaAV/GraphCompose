package com.demcha.compose.document.templates.coverletter.spec;

import java.util.List;
import java.util.Objects;

/**
 * Top-of-document identity block for a cover letter in Templates v2.
 *
 * <p>Mirrors {@code CvHeader} structurally because the user-facing
 * concept (name, address, phone, email, links) is identical. The
 * domain types are kept separate so the cover-letter and CV
 * surfaces can evolve independently if needed.</p>
 *
 * @param name    document subject's name (required, non-blank)
 * @param address optional address line; empty string when absent
 * @param phone   optional phone number; empty string when absent
 * @param email   optional email address; empty string when absent
 * @param links   ordered list of {@link Link} entries (typically
 *                LinkedIn, GitHub); never null after construction
 */
public record CoverLetterHeader(
        String name,
        String address,
        String phone,
        String email,
        List<Link> links) {

    /**
     * Compact constructor that normalises null strings to empty and
     * defensively copies the link list.
     *
     * @throws NullPointerException     if {@code name} is null
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public CoverLetterHeader {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        address = address == null ? "" : address;
        phone = phone == null ? "" : phone;
        email = email == null ? "" : email;
        links = links == null ? List.of() : List.copyOf(links);
    }

    /**
     * Returns the contact items (address, phone) in source order,
     * skipping blank fields.
     *
     * @return ordered list of non-blank contact strings
     */
    public List<String> contactItems() {
        java.util.List<String> items = new java.util.ArrayList<>(2);
        if (!address.isBlank()) {
            items.add(address);
        }
        if (!phone.isBlank()) {
            items.add(phone);
        }
        return List.copyOf(items);
    }

    /**
     * Returns a fresh builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Named hyperlink in a cover letter header (e.g. LinkedIn).
     *
     * @param label visible label (must not be blank)
     * @param url   target URL (may be empty for non-clickable label)
     */
    public record Link(String label, String url) {

        /**
         * Compact constructor that rejects null fields.
         *
         * @throws NullPointerException     if {@code label} or {@code url} is null
         * @throws IllegalArgumentException if {@code label} is blank
         */
        public Link {
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(url, "url");
            if (label.isBlank()) {
                throw new IllegalArgumentException("label must not be blank");
            }
        }
    }

    /**
     * Mutable builder for {@link CoverLetterHeader}.
     */
    public static final class Builder {
        private String name;
        private String address = "";
        private String phone = "";
        private String email = "";
        private final java.util.List<Link> links = new java.util.ArrayList<>();

        private Builder() {
        }

        /**
         * Sets the document subject's name.
         *
         * @param value non-blank name
         * @return this builder
         */
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the optional address line.
         *
         * @param value address; null treated as empty
         * @return this builder
         */
        public Builder address(String value) {
            this.address = value == null ? "" : value;
            return this;
        }

        /**
         * Sets the optional phone number.
         *
         * @param value phone; null treated as empty
         * @return this builder
         */
        public Builder phone(String value) {
            this.phone = value == null ? "" : value;
            return this;
        }

        /**
         * Sets the optional email address.
         *
         * @param value email; null treated as empty
         * @return this builder
         */
        public Builder email(String value) {
            this.email = value == null ? "" : value;
            return this;
        }

        /**
         * Appends a labelled link.
         *
         * @param label non-blank link label
         * @param url   non-null target URL (may be empty)
         * @return this builder
         */
        public Builder link(String label, String url) {
            this.links.add(new Link(label, url));
            return this;
        }

        /**
         * Builds an immutable {@link CoverLetterHeader}.
         *
         * @return new cover letter header
         */
        public CoverLetterHeader build() {
            return new CoverLetterHeader(name, address, phone, email, links);
        }
    }
}
