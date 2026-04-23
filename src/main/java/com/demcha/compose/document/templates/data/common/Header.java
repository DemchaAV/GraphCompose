package com.demcha.compose.document.templates.data.common;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
/**
 * Mutable top-of-document identity block used by canonical job-application templates.
 *
 * <p><b>Pipeline role:</b> supplies the contact and profile fields consumed by
 * shared template composers when they build the visible header region.</p>
 *
 * <p><b>Mutability:</b> mutable Lombok-backed bean. <b>Thread-safety:</b>
 * not thread-safe.</p>
 */
public class Header {
    private String name;
    private String address;
    private String phoneNumber;
    private EmailYaml email;
    private LinkYml gitHub;
    private LinkYml linkedIn;

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String address;
        private String phoneNumber;
        private EmailYaml email;
        private LinkYml gitHub;
        private LinkYml linkedIn;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder email(EmailYaml email) {
            this.email = email;
            return this;
        }

        public Builder email(String to, String displayText) {
            this.email = EmailYaml.mailto(to, displayText);
            return this;
        }

        public Builder gitHub(LinkYml gitHub) {
            this.gitHub = gitHub;
            return this;
        }

        public Builder gitHub(String url, String displayText) {
            this.gitHub = LinkYml.of(url, displayText);
            return this;
        }

        public Builder linkedIn(LinkYml linkedIn) {
            this.linkedIn = linkedIn;
            return this;
        }

        public Builder linkedIn(String url, String displayText) {
            this.linkedIn = LinkYml.of(url, displayText);
            return this;
        }

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
