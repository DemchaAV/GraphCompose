package com.demcha.compose.document.templates.cv.v2.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Top-of-document identity block — name, optional professional title,
 * required contact triple, and an optional ordered list of labelled
 * outbound links.
 *
 * <p>Required pieces have their own types ({@link CvName},
 * {@link CvContact}); optional links are accumulated through the
 * builder's {@code link(...)} method — added when the author wants
 * them, simply omitted otherwise.</p>
 *
 * @param name    structured name with first / last required and an
 *                optional middle component
 * @param jobTitle optional professional title rendered by presets
 *                 that have a subtitle line; blank when absent
 * @param contact  phone / email / address — all three required
 * @param links    ordered list of optional outbound links; never null
 */
public record CvIdentity(CvName name, String jobTitle,
                         CvContact contact, List<CvLink> links) {

    /**
     * Validates that {@code name} and {@code contact} are non-null,
     * trims a null {@code jobTitle} to empty, and defensively copies
     * {@code links} (null becomes an empty list).
     */
    public CvIdentity {
        Objects.requireNonNull(name, "name");
        jobTitle = jobTitle == null ? "" : jobTitle.trim();
        Objects.requireNonNull(contact, "contact");
        links = links == null ? List.of() : List.copyOf(links);
    }

    /**
     * Backward-compatible constructor for callers that predate the
     * optional job-title field. The title simply stays blank.
     *
     * @param name    structured name with first / last required and an
     *                optional middle component
     * @param contact phone / email / address — all three required
     * @param links   ordered list of optional outbound links; never null
     */
    public CvIdentity(CvName name, CvContact contact, List<CvLink> links) {
        this(name, "", contact, links);
    }

    /**
     * Creates a fluent builder for {@code CvIdentity}.
     *
     * @return new fluent builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Mutable builder for {@link CvIdentity}.
     */
    public static final class Builder {
        private CvName name;
        private String jobTitle = "";
        private CvContact contact;
        private final List<CvLink> links = new ArrayList<>();

        private Builder() {
        }

        /**
         * Sets the structured name.
         *
         * @param value the name to use
         * @return this builder for chaining
         */
        public Builder name(CvName value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the name from first and last parts.
         *
         * @param first first name (required, non-blank)
         * @param last  family name (required, non-blank)
         * @return this builder for chaining
         */
        public Builder name(String first, String last) {
            this.name = CvName.of(first, last);
            return this;
        }

        /**
         * Sets the name from first, middle, and last parts.
         *
         * @param first  first name (required, non-blank)
         * @param middle optional middle name; may be empty
         * @param last   family name (required, non-blank)
         * @return this builder for chaining
         */
        public Builder name(String first, String middle, String last) {
            this.name = new CvName(first, middle, last);
            return this;
        }

        /**
         * Sets the optional professional title shown only by presets
         * with a dedicated subtitle/header line.
         *
         * @param value the professional title; null becomes empty
         * @return this builder for chaining
         */
        public Builder jobTitle(String value) {
            this.jobTitle = value == null ? "" : value;
            return this;
        }

        /**
         * Sets the contact block.
         *
         * @param value the contact block to use
         * @return this builder for chaining
         */
        public Builder contact(CvContact value) {
            this.contact = value;
            return this;
        }

        /**
         * Sets the contact block from its three required fields.
         *
         * @param phone   non-blank phone number
         * @param email   non-blank email address
         * @param address non-blank location / postal address line
         * @return this builder for chaining
         */
        public Builder contact(String phone, String email, String address) {
            this.contact = new CvContact(phone, email, address);
            return this;
        }

        /**
         * Appends one outbound link.
         *
         * @param link the link to append (non-null)
         * @return this builder for chaining
         */
        public Builder link(CvLink link) {
            this.links.add(Objects.requireNonNull(link, "link"));
            return this;
        }

        /**
         * Appends one outbound link from its label and target.
         *
         * @param label visible link text (required, non-blank)
         * @param url   click target (required, non-blank)
         * @return this builder for chaining
         */
        public Builder link(String label, String url) {
            this.links.add(new CvLink(label, url));
            return this;
        }

        /**
         * Builds the immutable {@link CvIdentity}.
         *
         * @return the assembled identity block
         */
        public CvIdentity build() {
            return new CvIdentity(name, jobTitle, contact, links);
        }
    }
}
