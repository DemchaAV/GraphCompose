package com.demcha.compose.document.templates.data.common;

import lombok.Data;

/**
 * Mutable top-of-document identity block used by canonical job-application templates.
 *
 * <p><b>Pipeline role:</b> supplies the contact and profile fields consumed by
 * shared template composers when they build the visible header region.</p>
 *
 * <p><b>Mutability:</b> mutable Lombok-backed bean. <b>Thread-safety:</b>
 * not thread-safe.</p>
 */
@Data
public class Header {
    private String name;
    private String address;
    private String phoneNumber;
    private EmailYaml email;
    private LinkYml gitHub;
    private LinkYml linkedIn;

    /**
     * Creates an empty mutable header payload.
     */
    public Header() {
    }

    /**
     * Starts a fluent header builder.
     *
     * @return header builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for header contact data.
     */
    public static final class Builder {
        private String name;
        private String address;
        private String phoneNumber;
        private EmailYaml email;
        private LinkYml gitHub;
        private LinkYml linkedIn;

        private Builder() {
        }

        /**
         * Sets the display name.
         *
         * @param name display name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the address/location line.
         *
         * @param address address or location text
         * @return this builder
         */
        public Builder address(String address) {
            this.address = address;
            return this;
        }

        /**
         * Sets the phone number.
         *
         * @param phoneNumber phone number text
         * @return this builder
         */
        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        /**
         * Sets email contact metadata.
         *
         * @param email email payload
         * @return this builder
         */
        public Builder email(EmailYaml email) {
            this.email = email;
            return this;
        }

        /**
         * Creates and sets email contact metadata.
         *
         * @param to target email address
         * @param displayText visible link text
         * @return this builder
         */
        public Builder email(String to, String displayText) {
            this.email = EmailYaml.mailto(to, displayText);
            return this;
        }

        /**
         * Sets GitHub link metadata.
         *
         * @param gitHub GitHub link payload
         * @return this builder
         */
        public Builder gitHub(LinkYml gitHub) {
            this.gitHub = gitHub;
            return this;
        }

        /**
         * Creates and sets GitHub link metadata.
         *
         * @param url GitHub URL
         * @param displayText visible link text
         * @return this builder
         */
        public Builder gitHub(String url, String displayText) {
            this.gitHub = LinkYml.of(url, displayText);
            return this;
        }

        /**
         * Sets LinkedIn link metadata.
         *
         * @param linkedIn LinkedIn link payload
         * @return this builder
         */
        public Builder linkedIn(LinkYml linkedIn) {
            this.linkedIn = linkedIn;
            return this;
        }

        /**
         * Creates and sets LinkedIn link metadata.
         *
         * @param url LinkedIn URL
         * @param displayText visible link text
         * @return this builder
         */
        public Builder linkedIn(String url, String displayText) {
            this.linkedIn = LinkYml.of(url, displayText);
            return this;
        }

        /**
         * Builds the mutable header payload.
         *
         * @return header payload
         */
        public Header build() {
            Header header = new Header();
            header.setName(name);
            header.setAddress(address);
            header.setPhoneNumber(phoneNumber);
            header.setEmail(email);
            header.setGitHub(gitHub);
            header.setLinkedIn(linkedIn);
            return header;
        }
    }
}
