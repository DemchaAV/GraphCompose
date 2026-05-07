package com.demcha.compose.document.templates.cv.spec;

import java.util.List;
import java.util.Objects;

/**
 * Top-of-document identity block for a CV in Templates v2.
 *
 * <p>This is the v2 replacement for the legacy mutable
 * {@code com.demcha.compose.document.templates.data.common.Header}.
 * It is an immutable record carrying the contact and link fields a
 * preset's {@code Header} component renders. Optional string fields
 * may be empty (rendered as absent rows); {@code links} may be empty
 * but never null after construction.</p>
 *
 * @param name     document subject's name (required, non-blank)
 * @param jobTitle optional job title rendered as a subline under the
 *                 name (e.g. "Backend Java Developer"); empty string
 *                 when absent
 * @param address  optional address line; empty string when absent
 * @param phone    optional phone number; empty string when absent
 * @param email    optional email address; empty string when absent
 * @param links    ordered list of {@link Link} entries (typically
 *                 LinkedIn, GitHub); never null, may be empty
 */
public record CvHeader(
        String name,
        String jobTitle,
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
    public CvHeader {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        jobTitle = jobTitle == null ? "" : jobTitle;
        address = address == null ? "" : address;
        phone = phone == null ? "" : phone;
        email = email == null ? "" : email;
        links = links == null ? List.of() : List.copyOf(links);
    }

    /**
     * Returns a fluent builder seeded with empty fields.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the contact items (address, phone) in source order,
     * skipping blank fields. Useful for preset headers that join the
     * items with a {@code " | "} separator.
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
     * Returns the link labels in source order: email (if present)
     * followed by each {@link Link#label()}. Useful for preset headers
     * that join the labels with a {@code " | "} separator.
     *
     * @return ordered list of link labels
     */
    public List<String> linkLabels() {
        java.util.List<String> labels = new java.util.ArrayList<>(1 + links.size());
        if (!email.isBlank()) {
            labels.add(email);
        }
        for (Link link : links) {
            labels.add(link.label());
        }
        return List.copyOf(labels);
    }

    /**
     * Named hyperlink in a CV header (e.g. LinkedIn, GitHub).
     *
     * @param label visible label (e.g. {@code "LinkedIn"})
     * @param url   target URL (may be empty if rendering only the label)
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
     * Mutable builder for {@link CvHeader}.
     */
    public static final class Builder {
        private String name;
        private String jobTitle = "";
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
         * Sets the optional job title (e.g. "Backend Java Developer")
         * rendered under the subject's name by presets that surface it.
         *
         * @param value job title; null treated as empty
         * @return this builder
         */
        public Builder jobTitle(String value) {
            this.jobTitle = value == null ? "" : value;
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
         * Appends a labelled link (e.g. LinkedIn, GitHub) to the header.
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
         * Builds an immutable {@link CvHeader}.
         *
         * @return new CV header
         */
        public CvHeader build() {
            return new CvHeader(name, jobTitle, address, phone, email, links);
        }
    }
}
